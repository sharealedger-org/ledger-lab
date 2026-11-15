package org.universalledger.foundation.va.corpus

import java.io.{File, PrintWriter}
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport}
import scala.collection.mutable

/** Post-run evidence inspection; no financial producer or accounting rules. */
object ConnectedViewsCheck {
  type Row = Map[String, String]
  def scan(file: File)(f: Row => Unit): Long = {
    val in = new ReportingSupport.Input(file, "verification", true)
    try {
      var next = in.next()
      while (next.nonEmpty) { require(next.get.size == in.header.size, s"Schema width $file:${in.count}"); f(in.header.zip(next.get).toMap); next = in.next() }
      in.count
    } finally in.close()
  }
  def inspect(report: File): Unit = {
    require(new File(report, "EXTRACT_COMPLETE").isFile && new File(report, "REPORT_COMPLETE").isFile, "Incomplete report")
    Vector("reconciliation.csv", "synthetic_validation.csv", "synthetic_conservation.csv").foreach { name =>
      val count = scan(new File(report, name))(r => require(r("status") == "PASS", s"Failed $name: $r"))
      require(count > 0); println(s"PASS $name rows=$count")
    }
    var manifestRows = 0L
    scan(new File(report, "input_manifest.csv")) { r => if (Set("movement", "balance", "opening")(r("kind"))) manifestRows += r("rows").toLong }
    val metrics = mutable.HashMap[String, String]()
    scan(new File(report, "synthetic_metrics.csv"))(r => metrics(r("metric")) = r("value"))
    require(metrics("financial_rows").toLong == manifestRows)
    var coveredRows = 0L; var attributed = 0L; var unattributed = 0L; var vendors = 0L
    scan(new File(report, "synthetic_vendor_coverage.csv")) { r =>
      require(r("identity_rekeyed") == "false")
      val n = r("financial_rows").toLong; coveredRows += n; vendors += 1
      if (r("attribution") == "unattributed") { require(r("template_instrument_id").isEmpty); unattributed += n }
      else { require(r("template_instrument_id").nonEmpty); attributed += n }
    }
    require(coveredRows == manifestRows)
    println(s"PASS financial_rows=$manifestRows vendors=$vendors attributed_input_rows=$attributed unattributed_input_rows=$unattributed")
    Vector("vendor_temporal_summary.csv", "vendor_attribute_summary.csv").foreach { name =>
      val n = scan(new File(report, name)) { r => BigDecimal(r("amount")); require(r("rows").toLong >= 0) }
      require(n > 0); println(s"PASS $name rows=$n")
    }
  }
  def fixture(root: File): Unit = {
    val report = new File(root, "report"); inspect(report)
    def journal(year: String): Vector[Row] = {
      val in = new ReportingSupport.Input(new File(root, s"output/FY$year/SortedJEFY${year}merged.csv"), "movement", false)
      val out = mutable.ArrayBuffer[Row]()
      try {
        var next = in.next()
        while (next.nonEmpty) {
          val r = next.get; require(r.size == 39)
          out += Map("vendor" -> r(0), "journal" -> r(3), "event" -> r(5), "account" -> r(16), "amount" -> r(25), "direction" -> r(31)); next = in.next()
        }
      } finally in.close()
      out.toVector
    }
    val a = journal("14"); val b = journal("15")
    require(a.size == 12 && b.size == 4)
    Vector(a, b).foreach(rows => require(rows.groupBy(_("journal")).values.forall(_.map(r => BigDecimal(r("amount"))).sum == 0)))
    require(a.filter(r => r("event") == "EXPENSE_PAYMENT" && r("direction") == "D").map(r => BigDecimal(r("amount"))).sum == 175)
    require(b.filter(r => r("event") == "COMMITMENT" && r("direction") == "D").map(r => BigDecimal(r("amount"))).sum == 50)
    val cuts = mutable.ArrayBuffer[Row]()
    scan(new File(report, "vendor_temporal_cuts.csv"))(cuts += _)
    val movement = cuts.filter(r => r("vendor_id") == "c1246" && r("side") == "movement" && r("event") == "EXPENSE_PAYMENT" && r("direction") == "D")
    def amount(rows: Seq[Row]): BigDecimal = rows.map(r => BigDecimal(r("amount"))).sum
    val sw = movement.filter(_("date_mode") == "EVENT_DATE")
    require(amount(sw.filter(_("synthetic_hierarchy_path").contains("SIM_S01"))) == 100)
    require(amount(sw.filter(_("synthetic_hierarchy_path").contains("SIM_S02"))) == 15)
    val recast = movement.filter(_("date_mode") == "AS_OF")
    require(amount(recast) == 115 && recast.forall(_("synthetic_hierarchy_path").contains("SIM_S02")))
    val quiet = cuts.filter(r => r("vendor_id") == "c12464" && r("side") == "movement" && r("date_mode") == "RUN_DATE")
    require(quiet.nonEmpty && quiet.forall(_("synthetic_hierarchy_path").contains("SIM_A793")))
    val master = mutable.ArrayBuffer[Row]()
    scan(new File(report, "vendor_attribute_totals.csv"))(master += _)
    require(master.exists(r => r("vendor_id") == "c1246" && r("fiscal_year") == "2014" && r("attribute") == "instSizeTier" && r("attribute_value") == "SMALL"))
    require(master.exists(r => r("vendor_id") == "c1246" && r("fiscal_year") == "2015" && r("attribute") == "instSizeTier" && r("attribute_value") == "LARGE"))
    require(cuts.filter(_("vendor_id") == "0000000000").forall(_("profile_status") == "unattributed"))
    println("PASS connected fixture: known financial totals, switch/recast/quiet hierarchy, dated master dimensions and zero attribution")
  }
  def main(args: Array[String]): Unit = {
    val p = LedgerApp.parseArgs(args)
    if (p.getOrElse("fixture", "false") == "true") fixture(new File(p("root"))) else inspect(new File(p("report")))
  }
}
