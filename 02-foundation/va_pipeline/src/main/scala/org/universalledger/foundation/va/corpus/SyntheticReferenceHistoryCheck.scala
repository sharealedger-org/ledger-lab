package org.universalledger.foundation.va.corpus

import java.io.File
import java.time.LocalDate
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport}
import scala.collection.mutable

/** Bounded synthetic input validation/oracle checks only, not a measured lookup engine. */
object SyntheticReferenceHistoryCheck {
  import SyntheticReferenceHistory._
  type Row = Map[String, String]
  def read(file: File, expected: Vector[String]): Vector[Row] = {
    require(file.isFile && file.length() <= 16L * 1024 * 1024, s"Bounded fixture file required: $file")
    val in = new ReportingSupport.Input(file, "synthetic_fixture", true)
    try {
      require(in.header == expected, s"Fixture header mismatch: $file")
      val rows = mutable.ArrayBuffer[Row]()
      var next = in.next()
      while (next.nonEmpty) {
        require(rows.size < 100000, "Fixture row allowance exceeded")
        val r = next.get; require(r.size == expected.size, s"Fixture shape: $file:${in.count}")
        rows += expected.zip(r).toMap; next = in.next()
      }
      rows.toVector
    } finally in.close()
  }
  def validate(out: File, hierarchyOverride: Option[File] = None): Unit = {
    val paths = schemas.map(s => if (s.name == "hierarchy_history") hierarchyOverride.getOrElse(new File(out, s.name + ".csv")) else new File(out, s.name + ".csv"))
    require(paths.map(_.length()).sum <= 32L * 1024 * 1024, "Fixture byte allowance exceeded")
    val tables = schemas.zip(paths).map { case (s, file) => s.name -> read(file, s.header) }.toMap
    require(tables.values.map(_.size).sum <= 100000, "Total fixture row allowance exceeded")
    def key(s: Schema, r: Row): String = s.keys.map(r).mkString("|")
    val versionIds = mutable.HashSet[String]()
    schemas.foreach { s =>
      val seen = mutable.HashSet[(String, String)]()
      tables(s.name).foreach { r =>
        require(r("synthetic") == "true", "Synthetic fixture classification missing")
        require(s.keys.forall(k => r(k).nonEmpty && !r(k).contains("|")), "Invalid fixture business key")
        LocalDate.parse(r("effective_start"))
        require(seen.add(key(s, r) -> r("effective_start")), s"duplicate start: ${s.name}")
        require(versionIds.add(r("version_id")), "Duplicate version identity")
      }
    }
    val indices = schemas.map(s => s.name -> tables(s.name).groupBy(r => key(s, r)).map {
      case (k, rs) => k -> rs.sortBy(_("effective_start"))
    }).toMap
    def selected(table: String, businessKey: String, day: String): Option[Row] = {
      LocalDate.parse(day)
      indices(table).getOrElse(businessKey, Vector.empty).filter(_("effective_start") <= day).lastOption
    }
    def nodeExists(t: String, k: String, day: String): Boolean = selected("reference_history", s"$t|$k", day).nonEmpty
    val edges = tables("hierarchy_history")
    val boundaries = (edges ++ tables("reference_history")).map(_("effective_start")).distinct.sorted
    require(boundaries.size <= 128, "Synthetic date-boundary allowance exceeded")
    val forests = mutable.HashMap[(String, String), Map[String, Row]]()
    def walk(hierarchy: String, start: String, day: String): String = {
      val at = forests.getOrElseUpdate((hierarchy, day), {
        require(forests.size < 256, "Synthetic hierarchy/date allowance exceeded")
        edges.filter(r => r("hierarchy_id") == hierarchy && r("effective_start") <= day)
          .groupBy(r => r("child_type") + ":" + r("child_key")).map { case (k, rs) => k -> rs.maxBy(_("effective_start")) }
      })
      val visited = mutable.HashSet[String](); val path = mutable.ArrayBuffer[String]()
      var node = start; var done = false
      while (!done) {
        require(visited.add(node), s"dated hierarchy cycle: $hierarchy $day $node")
        val parts = node.split(":", 2); require(parts.length == 2 && nodeExists(parts(0), parts(1), day), s"dated hierarchy orphan: $node")
        path += node
        at.get(node) match {
          case Some(r) => node = r("parent_type") + ":" + r("parent_key")
          case None => require(parts(0) == "root", s"Unrooted hierarchy node: $node"); done = true
        }
      }
      path.mkString(">")
    }
    boundaries.foreach { day =>
      edges.filter(_("effective_start") <= day).groupBy(r => (r("hierarchy_id"), r("child_type"), r("child_key"))).keys.foreach {
        case (h, t, k) => walk(h, s"$t:$k", day)
      }
    }
    tables("sal_history").foreach { r =>
      val day = r("effective_start")
      require(nodeExists("instrument", r("instrument_id"), day) && nodeExists("agency", r("agency_key"), day), "SAL orphan")
      val parent = selected("hierarchy_history", "reporting|instrument|" + r("instrument_id"), day)
      require(parent.exists(p => p("parent_type") == "agency" && p("parent_key") == r("agency_key")), "SAL/hierarchy agency mismatch")
    }
    tables("car_history").foreach { r =>
      val day = r("effective_start")
      require(selected("sal_history", r("instrument_id"), day).nonEmpty && selected("party_history", r("party_id"), day).nonEmpty, "CAR orphan")
    }
    tables("alias_history").foreach { r =>
      require(nodeExists(r("reference_type"), r("target_key"), r("effective_start")) && nodeExists("agency", r("context_key"), r("effective_start")), "Alias orphan")
    }
    val changes = read(new File(out, "change_events.csv"), Vector("change_id", "effective_date", "recorded_date", "scenario", "inspiration_id", "synthetic"))
    require(changes.map(_("change_id")).distinct.size == changes.size)
    val changeMap = changes.map(r => r("change_id") -> r).toMap
    changes.foreach { r => LocalDate.parse(r("effective_date")); LocalDate.parse(r("recorded_date")); require(r("synthetic") == "true") }
    tables.values.flatten.filter(_("change_id") != "BASELINE").foreach { r =>
      require(changeMap.contains(r("change_id")) && changeMap(r("change_id"))("effective_date") == r("effective_start"), "Missing/inconsistent change lineage")
    }
    val cases = read(new File(out, "lookup_cases.csv"), Vector("case_id", "table", "business_key", "date_mode", "as_of_date", "event_date", "run_date", "value_field", "expected_version_id", "expected_value"))
    cases.foreach { c =>
      val day = c("date_mode") match {
        case "AS_OF" => c("as_of_date")
        case "RUN_DATE" => c("run_date")
        case "EVENT_DATE" => c("event_date")
        case other => sys.error(s"Unknown date mode $other")
      }
      val found = selected(c("table"), c("business_key"), day)
      require(found.map(_("version_id")).getOrElse("NOT_FOUND") == c("expected_version_id"), s"Lookup version mismatch: ${c("case_id")}")
      require(found.map(_(c("value_field"))).getOrElse("") == c("expected_value"), s"Lookup value mismatch: ${c("case_id")}")
    }
    val pathsCases = read(new File(out, "hierarchy_cases.csv"), Vector("case_id", "hierarchy_id", "child_type", "child_key", "lookup_date", "expected_path"))
    pathsCases.foreach { c =>
      require(walk(c("hierarchy_id"), c("child_type") + ":" + c("child_key"), c("lookup_date")) == c("expected_path"), s"Hierarchy path mismatch: ${c("case_id")}")
    }
    val quiet = read(new File(out, "activity_context.csv"), Vector("instrument_id", "last_business_event_date", "reference_change_id", "new_activity_on_change_date", "synthetic"))
    quiet.foreach { r =>
      require(r("synthetic") == "true" && r("new_activity_on_change_date") == "false")
      require(selected("sal_history", r("instrument_id"), r("last_business_event_date")).nonEmpty)
      require(LocalDate.parse(r("last_business_event_date")).isBefore(LocalDate.parse(changeMap(r("reference_change_id"))("effective_date"))))
    }
    schemas.foreach { s =>
      val order = tables(s.name).map(r => key(s, r).replace('|', '\u0000') + "\u0000" + r("effective_start"))
      require(order == order.sorted, s"Unsorted fixture table ${s.name}")
    }
    println(s"PASS synthetic_reference_history rows=${tables.values.map(_.size).sum} lookup_cases=${cases.size} hierarchy_cases=${pathsCases.size}")
  }
  def main(args: Array[String]): Unit = {
    val p = LedgerApp.parseArgs(args)
    validate(new File(p("in")), p.get("hierarchyOverride").map(new File(_)))
  }
}
