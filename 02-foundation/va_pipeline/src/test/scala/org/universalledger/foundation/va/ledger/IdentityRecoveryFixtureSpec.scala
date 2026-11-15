package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.charset.StandardCharsets
import org.apache.commons.csv.{CSVFormat, CSVParser}
import org.scalatest.funsuite.AnyFunSuite
import scala.collection.JavaConverters._

/** Independent expected controls for the committed, explicitly simulated input snapshot. */
class IdentityRecoveryFixtureSpec extends AnyFunSuite {
  private def rows(file: File): Vector[Vector[String]] = {
    val p = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.DEFAULT)
    try p.iterator.asScala.map(_.iterator.asScala.toVector).toVector finally p.close()
  }
  test("connected identity recovery fixture retains money, IDs, ARE lineage and reporting coverage") {
    val root = sys.env.getOrElse("CKB_IDENTITY_RECOVERY_OUTPUT", cancel("Run the connected fixture orchestrator and supply CKB_IDENTITY_RECOVERY_OUTPUT"))
    val journal03 = rows(new File(root, "output/FY03/SortedJEFY03merged.csv"))
    val journal04 = rows(new File(root, "output/FY04/SortedJEFY04merged.csv"))
    assert(journal03.size == 18 && journal04.size == 2)
    val journals = journal03 ++ journal04
    assert(journals.forall(_.size == 39))
    assert(journals.groupBy(r => (r(3), r(22))).values.forall(_.map(r => BigDecimal(r(25))).sum == 0))
    val expenses = journal03.filter(r => r(5) == "EXPENSE_PAYMENT" && r(31) == "D")
    assert(expenses.size == 6 && expenses.map(r => BigDecimal(r(25))).sum == 160)
    assert(expenses.forall(r => r(12) == "101" && r(13) == "201" && r(16) == "EXP301"))
    assert(expenses.filter(_(0) == "c1246").map(r => BigDecimal(r(25))).sum == 75)
    assert(expenses.filter(_(0) == "c12464").map(r => BigDecimal(r(25))).sum == 40)
    assert(expenses.filter(_(0) == "0000000000").map(r => BigDecimal(r(25))).sum == 25)
    assert(expenses.filter(_(0) == "SIM_VENDOR_0001").map(r => BigDecimal(r(25))).sum == 20)
    assert(expenses.forall(r => r(29).nonEmpty && r(30).nonEmpty && r(24).nonEmpty))
    assert(journal03.filter(r => r(5) == "REVENUE_RECEIPT" && r(31) == "D").map(r => BigDecimal(r(25))).sum == 200)
    assert(journal03.filter(r => r(5) == "COMMITMENT" && r(31) == "D").map(r => BigDecimal(r(25))).sum == 120)
    Seq("03", "04").foreach { year =>
      val ledger = rows(new File(root, s"output/FY$year/LDGR20$year.csv"))
      assert(ledger.head.reverse.dropWhile(_.isEmpty).reverse == ReportingSupport.ledgerHeader)
      assert(ledger.tail.map(r => BigDecimal(r(19))).sum == 0)
    }
    val report = new File(root, "report")
    assert(new File(report, "REPORT_COMPLETE").isFile)
    val recon = rows(new File(report, "reconciliation.csv"))
    assert(recon.tail.forall(r => r(recon.head.indexOf("status")) == "PASS"))
    val features = rows(new File(report, "aim_combined.csv"))
    val mapped = features.tail.map(features.head.zip(_).toMap)
    val supplier = mapped.find(r => r("vendor_id") == "c1246" && r("fiscal_year") == "2003").get
    assert(supplier("vendor_name") == "SIMULATED SUPPLIER, ONE")
    assert(supplier("eligible_matched_vendor") == "true")
    assert(supplier("signed_direct_po_amount") == "50")
    assert(mapped.find(_("vendor_id") == "0000000000").get("attributed") == "false")
  }
}
