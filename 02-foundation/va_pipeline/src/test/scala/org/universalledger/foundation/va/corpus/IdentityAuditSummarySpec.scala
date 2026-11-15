package org.universalledger.foundation.va.corpus

import java.nio.file.{Files, Path, Paths}
import org.scalatest.funsuite.AnyFunSuite
import scala.collection.JavaConverters._
import scala.io.Source

class IdentityAuditSummarySpec extends AnyFunSuite {
  test("independent count checks distinguish successful counts from an identity mismatch") {
    val base = Paths.get("../../session-run.tmp"); Files.createDirectories(base)
    val root = Files.createTempDirectory(base, "identity-summary-").toAbsolutePath
    def put(rel: String, s: String): Unit = { val p = root.resolve(rel); Files.createDirectories(p.getParent); Files.write(p, s.getBytes("UTF-8")); () }
    def text(p: Path): String = { val s = Source.fromFile(p.toFile, "UTF-8"); try s.mkString finally s.close() }
    try {
      Vector("master", "raw", "canonical").foreach(m => put(s"audit/$m/AUDIT_COMPLETE", "fixture\n"))
      put("audit/master/master_comparison.csv", "measured_csv_status,measured_csv_id\nunique,0000000088\n")
      put("audit/raw/source_count_controls.csv", "year,status\n2003,PASS\n")
      put("audit/raw/global_names.csv", "interpretation,family,normalized_name,classification,status,id,source_rows\nquote_aware,expense,0,meaningful_name_candidate,missing,0000000000,2\nquote_aware,expense,ACME,meaningful_name_candidate,missing,0000000000,1\n")
      put("audit/raw/expected_journal_identity_counts.csv", "year,family,legacy_status,expected_sje_rows\n2003,expense,unmatched_zero,4\n")
      put("audit/canonical/vendor_year_groups.csv", "vendor_id,year,side,source_system,rows\n0000000000,2003,movement,Payment Sys,4\n0000000000,2003,balance,Payment Sys,1\n")
      put("audit/canonical/vendor_source_groups.csv", "vendor_id,side,source_system,rows\n0000000000,movement,Payment Sys,4\n0000000000,balance,Payment Sys,1\n")
      put("audit/canonical/vendor_concentration.csv", "vendor_id,movement_rows,posted_balance_rows,financial_input_rows,exceeds_250000,id_status,measured_master_literal_match,padding_candidate_only\n0000000000,4,1,5,false,unattributed_zero,false,false\n")
      put("run/FY03/metrics/process_metrics.csv", "process_name,output_rows\nmerge-payment-po,4\nstep-3,1\n")
      def run(out: String): Unit = IdentityAuditSummary.main(Array("--auditRoot", root.resolve("audit").toString, "--runRoot", root.resolve("run").toString, "--out", root.resolve(out).toString))
      run("good")
      assert(Files.exists(root.resolve("good/COUNT_CHECKS_PASS")))
      assert(text(root.resolve("good/summary.csv")).contains("zero_key_financial_input_rows,5"))
      assert(text(root.resolve("good/candidate_name_summary.csv")).contains("numeric_or_punctuation_label,missing,1,2"))
      put("audit/raw/expected_journal_identity_counts.csv", "year,family,legacy_status,expected_sje_rows\n2003,expense,unmatched_zero,6\n")
      run("bad")
      assert(Files.exists(root.resolve("bad/COUNT_CHECKS_MISMATCH")))
      assert(text(root.resolve("bad/raw_to_journal_identity_controls.csv")).contains("6,4,-2,MISMATCH"))
    } finally { val s = Files.walk(root); try s.iterator().asScala.toVector.reverse.foreach(Files.deleteIfExists(_)) finally s.close() }
  }
  test("candidate sizing excludes numeric labels and preserves named entities containing digits") {
    assert(IdentityAuditSummary.candidateClass("0") == "numeric_or_punctuation_label")
    assert(IdentityAuditSummary.candidateClass("12345") == "numeric_or_punctuation_label")
    assert(IdentityAuditSummary.candidateClass("123 STOCK INC") == "source_name_candidate_not_legal_identity")
    assert(IdentityAuditSummary.candidateClass("JOURNAL ENTRY") == "exact_generic_label")
  }
  test("lookup counterfactual distinguishes missed matches, conflicting identities and invalid names") {
    import IdentityAuditParserSummary.category
    assert(category(IdentityAudit.Zero, "unique", "0000000001", false) == "missing_to_named_lookup_candidate")
    assert(category("0000000002", "unique", "0000000001", false) == "different_mapped_identity")
    assert(category("0000000002", "missing", IdentityAudit.Zero, false) == "legacy_match_absent_under_quote_aware_parse")
    assert(category(IdentityAudit.Zero, "unique", "0000000001", true) == "invalid_quote_aware_name")
  }
}
