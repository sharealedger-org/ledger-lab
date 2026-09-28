#!/usr/bin/env python3
"""Classify observed cost-curve evidence without claiming a global proof.

The experiment runner supplies axis values and run IDs in cost_curve_sweep.csv;
metrics remain in the existing cost_surface.csv accumulator.
"""

import argparse
import csv
import json
import math
import sys
from collections import defaultdict


def read_rows(path):
    with open(path, newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def number(row, key):
    try:
        value = float(row[key])
        return value if math.isfinite(value) else None
    except (KeyError, TypeError, ValueError):
        return None


def classify(points):
    points = sorted(points, key=lambda item: item["axis_value"])
    costs = [item["cost"] for item in points]
    if len(points) < 3:
        return {
            "classification": "inconclusive",
            "reason": "At least three ordered points are needed to assess curvature.",
        }

    minimum_index = min(range(len(costs)), key=costs.__getitem__)
    first_differences = [costs[i + 1] - costs[i] for i in range(len(costs) - 1)]
    second_differences = [
        first_differences[i + 1] - first_differences[i]
        for i in range(len(first_differences) - 1)
    ]
    scale = max(max(abs(cost) for cost in costs), 1.0)
    tolerance = scale * 1e-9
    positive_second = sum(value > tolerance for value in second_differences)
    negative_second = sum(value < -tolerance for value in second_differences)

    if minimum_index == 0:
        classification = "left-boundary-minimum"
    elif minimum_index == len(points) - 1:
        classification = "right-boundary-minimum"
    elif positive_second and not negative_second:
        classification = "interior-u-shaped-evidence"
    elif negative_second and not positive_second:
        classification = "concave-evidence"
    else:
        classification = "mixed-or-noisy"

    return {
        "classification": classification,
        "minimum_point": points[minimum_index]["point_id"],
        "minimum_axis_value": points[minimum_index]["axis_value"],
        "minimum_cost": costs[minimum_index],
        "first_differences": first_differences,
        "second_differences": second_differences,
        "positive_second_differences": positive_second,
        "negative_second_differences": negative_second,
        "reason": (
            "Finite observations support a shape classification only over the sampled axis; "
            "they do not prove a globally U-shaped cost function."
        ),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cost-surface", required=True)
    parser.add_argument("--sweep-log", required=True)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    cost_rows = {row.get("run_id"): row for row in read_rows(args.cost_surface)}
    sweep_rows = read_rows(args.sweep_log)
    groups = defaultdict(list)
    skipped = []

    for sweep in sweep_rows:
        cost_row = cost_rows.get(sweep.get("run_id"))
        axis_value = number(sweep, "axis_value") if sweep.get("axis_value") else None
        cost = number(cost_row, "total_cost_proxy") if cost_row else None
        if axis_value is None or cost is None:
            skipped.append(sweep.get("point_id", "unknown"))
            continue
        groups[sweep.get("curve", "unknown")].append({
            "point_id": sweep.get("point_id", "unknown"),
            "axis_value": axis_value,
            "cost": cost,
            "run_id": sweep.get("run_id"),
        })

    report = {
        "curves": {curve: classify(points) | {"points": sorted(points, key=lambda p: p["axis_value"])}
                   for curve, points in groups.items()},
        "skipped_points": skipped,
        "global_proof_status": "not-proven",
        "global_proof_reason": (
            "A finite sweep can identify an observed interior minimum or boundary behavior, "
            "but proving a global U-shape requires a model of the cost function, domain bounds, "
            "and evidence that the sampled surface is stable under replication."
        ),
    }

    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
        return 0

    for curve, result in report["curves"].items():
        print(f"{curve}: {result['classification']}")
        if "minimum_point" in result:
            print(f"  observed minimum: {result['minimum_point']} at axis={result['minimum_axis_value']} cost={result['minimum_cost']:.2f}")
        print(f"  points: {len(result['points'])}")
    if skipped:
        print(f"Skipped points without matching numeric metrics: {', '.join(skipped)}", file=sys.stderr)
    print("Global proof status: not-proven")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
