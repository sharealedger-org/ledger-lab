package org.universalledger.foundation.va.corpus

import java.io.File
import scala.collection.mutable

/** Counts parser/lookup differences in already-aggregated audit metadata. */
object IdentityAuditParserSummary {
  import IdentityAudit.{Zero, strict, writer, write, close, csv}
  def category(oldId: String, csvStatus: String, csvId: String, invalid: Boolean): String = {
    if (invalid) "invalid_quote_aware_name"
    else if (csvStatus == "ambiguous") "ambiguous_quote_aware_lookup"
    else if (oldId == Zero && csvStatus == "unique" && csvId != Zero) "missing_to_named_lookup_candidate"
    else if (oldId != Zero && csvStatus != "unique") "legacy_match_absent_under_quote_aware_parse"
    else if (oldId != Zero && csvStatus == "unique" && oldId != csvId) "different_mapped_identity"
    else if (oldId != Zero) "same_mapped_identity"
    else "still_unattributed"
  }
  def main(args: Array[String]): Unit = {
    require(args.length % 2 == 0)
    val p = args.grouped(2).map(a => a(0).stripPrefix("--") -> a(1)).toMap
    val root = new File(p("auditRoot")); val out = new File(p("out")); require(!out.exists()); out.mkdirs()
    require(new File(root, "raw/AUDIT_COMPLETE").isFile)
    val manifest = writer(new File(out, "input_manifest.csv"), Seq("path", "bytes", "physical_lines", "sha256"))
    val input = new IdentityAudit.Read(new File(root, "raw/source_names.csv"), manifest)
    val counts = mutable.HashMap[(String, String), Long]().withDefaultValue(0L)
    val differences = mutable.HashMap[String, Long]().withDefaultValue(0L)
    val top = mutable.PriorityQueue[(Long, Vector[String])]()(Ordering.by[(Long, Vector[String]), Long](_._1).reverse)
    var total = 0L; var enrichedChanges = 0L
    try {
      val h = strict(input.next()).get
      def at(r: Vector[String], key: String): String = r(h.indexOf(key))
      var line = input.next()
      while (line != null) {
        val r = strict(line).get; require(r.size == h.size)
        val family = at(r, "family"); val n = at(r, "source_rows").toLong
        val oldId = at(r, "measured_legacy_id"); val newId = at(r, "csv_master_id")
        val cls = category(oldId, at(r, "csv_master_status"), newId, at(r, "csv_source_name") == "<INVALID_CSV>")
        counts((family, cls)) += n; total += n
        if (at(r, "legacy_source_name") != at(r, "csv_source_name")) differences(family) += n
        if (oldId != at(r, "enriched_legacy_id")) enrichedChanges += n
        if (Set("missing_to_named_lookup_candidate", "legacy_match_absent_under_quote_aware_parse", "different_mapped_identity")(cls)) {
          top.enqueue(n -> Vector(family, at(r, "year"), cls, at(r, "legacy_source_name"), at(r, "csv_source_name"), oldId, newId, n.toString))
          if (top.size > 100) top.dequeue()
        }
        line = input.next()
      }
    } finally { input.close(); close(manifest) }
    var expected = -1L
    IdentityAuditSummary.rows(new File(root, "raw/audit_metrics.csv"))(r => if (r("metric") == "raw_accepted_rows") expected = r("value").toLong)
    require(total == expected, s"Parser metadata counts do not reconcile: $total vs $expected")
    val table = writer(new File(out, "parser_lookup_summary.csv"), Seq("family", "classification", "source_rows"))
    try counts.toSeq.sortBy(_._1).foreach { case ((family, cls), n) => write(table, family, cls, n) } finally close(table)
    val metrics = writer(new File(out, "summary.csv"), Seq("metric", "value"))
    try {
      write(metrics, "accepted_source_rows_checked", total); write(metrics, "enriched_master_changed_lookup_rows", enrichedChanges)
      differences.toSeq.sorted.foreach { case (family, n) => write(metrics, family + "_name_parser_difference_rows", n) }
    } finally close(metrics)
    val examples = writer(new File(out, "largest_lookup_difference_examples.csv"), Seq("family", "year", "classification", "legacy_name", "quote_aware_name", "legacy_id", "quote_aware_id", "source_rows"))
    try top.dequeueAll.sortBy { case (n, r) => (-n, r.mkString("|")) }.foreach { case (_, r) => examples.println(csv(r)) } finally close(examples)
    println(s"Parser/lookup summary reconciled $total accepted source rows; no financial corrections applied")
  }
}
