package org.universalledger.foundation.va.corpus

import java.io.{File, PrintWriter}
import scala.collection.mutable
import scala.io.Source

/** Bounded summary and independent count checks over diagnostic output, not accounting. */
object IdentityAuditSummary {
  import IdentityAudit.{csv, strict, writer, write, close}
  // Exploratory exact-label rules from this audit's largest names; these do not
  // prove the source's payroll/subledger decomposition or a legal party identity.
  val ReviewLabels = Map(
    "JOURNAL VOUCHER" -> "bookkeeping_label_heuristic", "ADJUSTING JOURNAL" -> "bookkeeping_label_heuristic",
    "ADJUSTMENT" -> "bookkeeping_label_heuristic", "PAYROLL INTERFACE" -> "system_or_cost_label_heuristic",
    "STRAIGHT TIME" -> "system_or_cost_label_heuristic", "CIPPS" -> "system_or_cost_label_heuristic",
    "BURDEN COST" -> "system_or_cost_label_heuristic", "BURDEN TRANSACTION" -> "system_or_cost_label_heuristic",
    "LABOR COST" -> "system_or_cost_label_heuristic", "USAGE COST" -> "system_or_cost_label_heuristic",
    "USAGES" -> "system_or_cost_label_heuristic", "UVA MEDICAL CENTER INTERNAL" -> "internal_or_interagency_label_heuristic",
    "INTERAGENCY TRANSACTIONS" -> "internal_or_interagency_label_heuristic", "PROCUREMENT CARD" -> "pooled_payment_label_heuristic",
    "CONTACT APPLICABLE COMMUNITY COLLEGE FOR PAYEE DATA" -> "explicit_missing_detail_label")
  def candidateClass(name: String): String = {
    val n = IdentityAudit.normalize(name)
    if (IdentityAudit.Generic(n)) "exact_generic_label"
    else if (ReviewLabels.contains(n)) ReviewLabels(n)
    else if (!n.exists(_.isLetter)) "numeric_or_punctuation_label"
    else "source_name_candidate_not_legal_identity"
  }
  def rows(f: File)(consume: Map[String, String] => Unit): Unit = {
    val src = Source.fromFile(f, "UTF-8")
    try {
      val it = src.getLines(); require(it.hasNext, s"Empty table: $f")
      val header = strict(it.next()).get
      it.foreach { line => val r = strict(line).get; require(r.size == header.size, s"Summary CSV shape: $f"); consume(header.zip(r).toMap) }
    } finally src.close()
  }
  def main(args: Array[String]): Unit = {
    require(args.length % 2 == 0)
    val p = args.grouped(2).map(a => a(0).stripPrefix("--") -> a(1)).toMap
    val audit = new File(p("auditRoot")); val out = new File(p("out"))
    require(!out.exists()); out.mkdirs()
    Seq("master", "raw", "canonical").foreach(m => require(new File(audit, s"$m/AUDIT_COMPLETE").isFile, s"Incomplete audit phase: $m"))
    val actual = mutable.HashMap[Vector[String], Long]().withDefaultValue(0L)
    val perFile = mutable.HashMap[(String, String), Long]().withDefaultValue(0L)
    rows(new File(audit, "canonical/vendor_year_groups.csv")) { r =>
      val n = r("rows").toLong; perFile((r("year"), r("side"))) += n
      if (r("side") == "movement" && Set("Payment Sys", "Revenue Sys")(r("source_system"))) {
        val family = if (r("source_system") == "Payment Sys") "expense" else "revenue"
        actual(Vector(r("year"), family, if (r("vendor_id") == IdentityAudit.Zero) "unmatched_zero" else "mapped")) += n
      }
    }
    val expected = mutable.HashMap[Vector[String], Long]().withDefaultValue(0L)
    rows(new File(audit, "raw/expected_journal_identity_counts.csv")) { r =>
      expected(Vector(r("year"), r("family"), r("legacy_status"))) += r("expected_sje_rows").toLong
    }
    var mismatches = 0
    val identity = writer(new File(out, "raw_to_journal_identity_controls.csv"), Seq("year", "family", "status", "expected_journal_rows", "actual_journal_rows", "delta", "verdict"))
    try { (expected.keySet ++ actual.keySet).toVector.sortBy(_.mkString("|")).foreach { k =>
      val delta = actual(k) - expected(k); if (delta != 0) mismatches += 1
      identity.println(csv(k ++ Vector(expected(k).toString, actual(k).toString, delta.toString, if (delta == 0) "PASS" else "MISMATCH")))
    }} finally close(identity)
    val canonical = writer(new File(out, "recorded_canonical_count_controls.csv"), Seq("year", "side", "recorded_rows", "audit_rows", "delta", "verdict"))
    try perFile.keys.map(_._1).toVector.distinct.sorted.foreach { year =>
      val recorded = mutable.HashMap[String, Long]().withDefaultValue(0L)
      rows(new File(p("runRoot"), s"FY${year.takeRight(2)}/metrics/process_metrics.csv")) { r =>
        if (r("process_name") == "merge-payment-po") recorded("movement") += r("output_rows").toLong
        if (r("process_name") == "step-3") recorded("balance") += r("output_rows").toLong
      }
      Seq("movement", "balance").foreach { side =>
        val n = perFile((year, side)); val delta = n - recorded(side); if (delta != 0) mismatches += 1
        write(canonical, year, side, recorded(side), n, delta, if (delta == 0) "PASS" else "MISMATCH")
      }
    } finally close(canonical)
    var sourceMismatches = 0
    rows(new File(audit, "raw/source_count_controls.csv"))(r => if (r("status") != "PASS") sourceMismatches += 1)
    val overlap = mutable.HashMap[(Boolean, Boolean, Boolean), (Long, Long, Long)]()
    val shared = writer(new File(out, "shared_literal_payment_po_keys.csv"), Seq("vendor_id", "journal_rows", "posted_rows", "interpretation"))
    var overlapId: String = null; var overlapMovement = 0L; var overlapBalance = 0L
    val sourceSet = mutable.HashSet[String]()
    def finishOverlap(): Unit = if (overlapId != null) {
      val key = (sourceSet("Payment Sys"), sourceSet("Revenue Sys"), sourceSet("PO Sys"))
      val prev = overlap.getOrElse(key, (0L, 0L, 0L))
      overlap(key) = (prev._1 + 1, prev._2 + overlapMovement, prev._3 + overlapBalance)
      if (key._1 && key._3) write(shared, overlapId, overlapMovement, overlapBalance, "literal_key_overlap_not_verified_party_bridge")
    }
    try rows(new File(audit, "canonical/vendor_source_groups.csv")) { r =>
      if (overlapId != r("vendor_id")) {
        finishOverlap(); overlapId = r("vendor_id"); overlapMovement = 0; overlapBalance = 0; sourceSet.clear()
      }
      sourceSet += r("source_system"); require(sourceSet.size <= 64)
      if (r("side") == "movement") overlapMovement += r("rows").toLong else overlapBalance += r("rows").toLong
    } finally { finishOverlap(); close(shared) }
    val overlapOut = writer(new File(out, "literal_source_overlap_summary.csv"), Seq("has_payment", "has_revenue", "has_po", "vendor_keys", "journal_rows", "posted_rows"))
    try overlap.toSeq.sortBy(_._1.toString).foreach { case ((pay, rev, po), (keys, m, b)) => write(overlapOut, pay, rev, po, keys, m, b) } finally close(overlapOut)
    val candidates = mutable.HashMap[(String, String), (Long, Long)]()
    val rules = writer(new File(out, "review_label_rules.csv"), Seq("exact_normalized_label", "classification", "scope"))
    try ReviewLabels.toSeq.sorted.foreach { case (name, cls) => write(rules, name, cls, "exploratory_diagnostic_not_identity_or_payroll_proof") } finally close(rules)
    val candidateTop = mutable.PriorityQueue[(Long, Vector[String])]()(Ordering.by[(Long, Vector[String]), Long](_._1).reverse)
    rows(new File(audit, "raw/global_names.csv")) { r =>
      // Quote-aware expense names have one stable normalized-name/status key.
      // This avoids counting legacy whitespace lookup variants as new parties.
      if (r("interpretation") == "quote_aware" && r("family") == "expense") {
        val cls = if (r("classification") == "invalid_csv") "invalid_csv" else candidateClass(r("normalized_name"))
        val key = (cls, r("status")); val before = candidates.getOrElse(key, (0L, 0L)); val n = r("source_rows").toLong
        candidates(key) = (before._1 + 1, before._2 + n)
        if (r("status") == "missing") {
          candidateTop.enqueue(n -> Vector(r("normalized_name"), cls, r("status"), n.toString))
          if (candidateTop.size > 100) candidateTop.dequeue()
        }
      }
    }
    val candidateOut = writer(new File(out, "candidate_name_summary.csv"), Seq("classification", "master_status", "distinct_normalized_names", "source_rows"))
    try candidates.toSeq.sortBy(_._1).foreach { case ((cls, status), (n, count)) => write(candidateOut, cls, status, n, count) } finally close(candidateOut)
    val candidateTopOut = writer(new File(out, "largest_missing_source_names.csv"), Seq("normalized_name", "classification", "master_status", "source_rows"))
    try candidateTop.dequeueAll.sortBy { case (n, r) => (-n, r.head) }.foreach { case (_, r) => candidateTopOut.println(csv(r)) } finally close(candidateTopOut)
    val top = mutable.PriorityQueue[(Long, Vector[String])]()(Ordering.by[(Long, Vector[String]), Long](_._1).reverse)
    val masterIds = mutable.HashSet[String]()
    rows(new File(audit, "master/master_comparison.csv")) { r => if (r("measured_csv_status") == "unique") masterIds += r("measured_csv_id") }
    val shapes = mutable.HashMap[(String, Int), (Long, Long)]()
    var namespaceResemblances = 0L
    val header = Vector("vendor_id", "movement_rows", "posted_balance_rows", "financial_input_rows", "exceeds_250000", "id_status", "measured_master_literal_match", "padding_candidate_only")
    var padded = 0L; var paddedRows = 0L; var literal = 0L; var literalRows = 0L
    var nonzeroOver = 0L; var largestNonzero = 0L; var zeroRows = 0L; var zeroJournal = 0L; var zeroBalance = 0L
    var placeholderRows = 0L; var placeholderGroups = 0L; var otherOver = 0L; var largestOther = 0L
    var totalRows = 0L; var vendors = 0L
    rows(new File(audit, "canonical/vendor_concentration.csv")) { r =>
      val n = r("financial_input_rows").toLong; totalRows += n; vendors += 1
      val id = r("vendor_id")
      val cPrefix = id.startsWith("c") && id.length > 1 && id.drop(1).forall(_.isDigit)
      val shape = if (r("id_status") == "unattributed_zero") "zero_sentinel" else if (id.forall(_.isDigit)) "numeric" else if (cPrefix) "c_prefixed_numeric" else "other_literal"
      val shapeKey = (shape, id.length); val beforeShape = shapes.getOrElse(shapeKey, (0L, 0L))
      shapes(shapeKey) = (beforeShape._1 + 1, beforeShape._2 + n)
      if (cPrefix && masterIds(IdentityAudit.pad(id.drop(1)))) namespaceResemblances += 1
      val placeholder = CorpusProfile.EvaPo.placeholderKeys(id)
      top.enqueue(n -> (header.map(r) :+ placeholder.toString)); if (top.size > 100) top.dequeue()
      if (placeholder) { placeholderRows += n; placeholderGroups += 1 }
      if (r("padding_candidate_only") == "true") { padded += 1; paddedRows += n }
      if (r("measured_master_literal_match") == "true") { literal += 1; literalRows += n }
      if (r("id_status") == "unattributed_zero") { zeroRows += n; zeroJournal += r("movement_rows").toLong; zeroBalance += r("posted_balance_rows").toLong }
      else {
        if (n > 250000) nonzeroOver += 1; largestNonzero = largestNonzero.max(n)
        if (!placeholder && r("id_status") != "unmatched_surrogate") { if (n > 250000) otherOver += 1; largestOther = largestOther.max(n) }
      }
    }
    val topOut = writer(new File(out, "largest_vendor_groups.csv"), header :+ "known_po_placeholder")
    try top.dequeueAll.sortBy { case (n, r) => (-n, r.head) }.foreach { case (_, r) => topOut.println(csv(r)) } finally close(topOut)
    val shapeOut = writer(new File(out, "id_shape_summary.csv"), Seq("shape", "literal_length", "vendor_keys", "financial_input_rows"))
    try shapes.toSeq.sortBy(_._1).foreach { case ((shape, length), (keys, n)) => write(shapeOut, shape, length, keys, n) } finally close(shapeOut)
    val summary = writer(new File(out, "summary.csv"), Seq("metric", "value"))
    try {
      write(summary, "source_acceptance_mismatches", sourceMismatches); write(summary, "identity_and_canonical_count_mismatches", mismatches)
      write(summary, "vendor_keys", vendors); write(summary, "financial_input_rows", totalRows)
      write(summary, "zero_key_financial_input_rows", zeroRows); write(summary, "zero_key_journal_rows", zeroJournal); write(summary, "zero_key_posted_rows", zeroBalance)
      write(summary, "nonzero_groups_over_250000", nonzeroOver); write(summary, "largest_nonzero_group_rows", largestNonzero)
      write(summary, "known_po_placeholder_groups", placeholderGroups); write(summary, "known_po_placeholder_input_rows", placeholderRows)
      write(summary, "other_observed_groups_over_250000", otherOver); write(summary, "largest_other_observed_group_rows", largestOther)
      write(summary, "literal_master_key_groups", literal); write(summary, "literal_master_key_input_rows", literalRows)
      write(summary, "padding_candidates_not_bridges", padded); write(summary, "padding_candidate_input_rows", paddedRows)
      write(summary, "c_prefix_numeric_resemblances_not_bridges", namespaceResemblances)
      write(summary, "literal_payment_po_shared_keys", overlap.iterator.filter(_._1._1).filter(_._1._3).map(_._2._1).sum)
    } finally close(summary)
    val marker = new PrintWriter(new File(out, if (sourceMismatches + mismatches == 0) "COUNT_CHECKS_PASS" else "COUNT_CHECKS_MISMATCH"), "UTF-8")
    marker.println("Diagnostic row counts only; no accounting, attribution or memory-capability certification."); close(marker)
    println(s"Summary complete; source mismatches=$sourceMismatches; identity/canonical mismatches=$mismatches")
  }
}
