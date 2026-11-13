package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import org.scalatest.funsuite.AnyFunSuite
import org.apache.commons.csv.{CSVFormat, CSVParser}
import scala.collection.JavaConverters._

class ReportingPassSpec extends AnyFunSuite {
  import ReportingSupport._
  private val spec = SortEngine.loadSpec("../sort_specs/SortedJE.sortspec")
  private def write(path: Path, lines: Seq[String]): Unit = Files.write(path, (lines.mkString("\n") + "\n").getBytes(StandardCharsets.UTF_8))
  private def table(file: Path): Vector[Map[String, String]] = {
    val parser = CSVParser.parse(file.toFile, StandardCharsets.UTF_8, CSVFormat.DEFAULT)
    try {
      val all = parser.iterator().asScala.map(_.iterator().asScala.toVector).toVector
      all.tail.map(r => all.head.zip(r).toMap)
    } finally parser.close()
  }
  private def pair(v: String, period: String, amount: String, account: String, event: String, contract: String = "0"): Seq[String] = {
    val a = Array.fill(39)("0")
    a(0) = v; a(1) = contract; a(3) = s"$v-$period-$account-$amount"; a(4) = "1"
    a(5) = event; a(6) = "fixture"; a(9) = "ACTUALS"; a(10) = "FIN"; a(11) = "BOOK"
    a(12) = "A1"; a(13) = "F1"; a(14) = "060"; a(16) = account
    a(18) = "USD"; a(19) = "TXN"; a(20) = "USD"; a(21) = "BASE"
    a(22) = period; a(23) = "2013/07/31"; a(24) = "2013/07/31"; a(25) = amount
    a(29) = "fixture-rules-v1"; a(30) = "rule-1"; a(31) = "D"
    val b = a.clone(); b(4) = "2"; b(13) = "0000"; b(14) = "0000"; b(16) = "0000"; b(25) = (-BigDecimal(amount)).toString; b(31) = "O"
    Seq(a.mkString(","), b.mkString(","))
  }
  private def fixture(root: Path): Map[String, String] = {
    val input = Files.createDirectories(root.resolve("inputs"))
    val references = Files.createDirectories(root.resolve("references"))
    val rows14 = pair("0000000001", "0014/07", "100", "EXP1", "PAYMENT") ++
      pair("0000000001", "0014/07", "-10", "EXP1", "PAYMENT") ++
      pair("0000000001", "2013/07", "120", "COMMIT", "COMMITMENT", "C1") ++
      pair("0000000001", "2013/08", "-20", "COMMIT", "COMMITMENT", "C1") ++
      pair("0000000002", "0014/08", "50", "EXP1", "PAYMENT") ++
      pair("0000000000", "0014/07", "300", "REV1", "REVENUE_RECEIPT")
    val rows15 = pair("0000000001", "0015/07", "80", "EXP1", "PAYMENT") ++
      pair("0000000001", "2014/07", "90", "COMMIT", "COMMITMENT", "C1")
    Seq("14" -> rows14, "15" -> rows15).foreach { case (y, rows) =>
      write(input.resolve(s"SortedJEFY${y}merged.csv"), SortEngine.sortChunk(rows, spec))
      post(input.toString + "/", input.toString + "/", y)
    }
    // Merged-component duplicate must not be read again.
    write(input.resolve("SortedJEFY14q1exp.csv"), SortEngine.sortChunk(rows14, spec))
    write(root.resolve("vendors.csv"), Seq("instID,instHolderName,instEffectDate,instEffectEndDate",
      "0000000001,\"Acme, Old\",2000-01-01,2014-06-30", "0000000001,Acme New,2014-07-01,9999-99-99"))
    write(root.resolve("contracts.csv"), Seq("vendor_id,contract_id,effective_from,effective_to,description",
      "0000000001,C1,2000-01-01,2014-06-30,Original", "0000000001,C1,2014-07-01,9999-12-31,Renewed"))
    write(references.resolve("aRefAgencyCSV.txt"), Seq("AGY_AGENCY_CODE,AGY_AGENCY_KEY,AGY_AGENCY_NAME", "001,A1,Agency One"))
    write(references.resolve("aRefFundCSV.txt"), Seq("KEY,CODE,NAME", "F1,001,General Fund"))
    Map("inPaths" -> input.toString, "vendorMaster" -> root.resolve("vendors.csv").toString,
      "contractDimensions" -> root.resolve("contracts.csv").toString, "refPath" -> references.toString,
      "packetVendors" -> "0000000001", "reportProfile" -> "expanded", "balanceBasis" -> "period_movements")
  }
  private def withFixture(f: (Path, Map[String, String]) => Unit): Unit = {
    val dir = Files.createTempDirectory(Paths.get("../../session-run.tmp"), "reporting-")
    try f(dir, fixture(dir)) finally {
      def remove(file: File): Unit = { if (file.isDirectory) file.listFiles().foreach(remove); Files.delete(file.toPath) }
      remove(dir.toFile)
    }
  }
  private def run(dir: Path, params: Map[String, String], name: String, buffer: Int = 2, fanIn: Int = 2): Path = {
    val out = dir.resolve(name)
    CKBViews(params ++ Map("outPath" -> out.toString, "spillRows" -> buffer.toString))
    ReportingReduce(Map("outPath" -> out.toString, "mergeFanIn" -> fanIn.toString))
    out
  }
  test("real posting output reconciles; features and effective dimensions remain aligned") {
    withFixture { (dir, params) =>
      val out = run(dir, params, "report")
      assert(table(out.resolve("reconciliation.csv")).forall(_("status") == "PASS"))
      val combined = table(out.resolve("aim_combined.csv"))
      val a = combined.find(r => r("vendor_id") == "0000000001" && r("fiscal_year") == "2014").get
      assert(a("vendor_name") == "Acme, Old")
      assert(a("name_status") == "matched_asof")
      assert(BigDecimal(a("debit_amount")) == 190)
      assert(a("po_count") == "2" && BigDecimal(a("committed_amount")) == 140) // legacy sign selection
      assert(a("signed_direct_po_count") == "2" && BigDecimal(a("signed_direct_po_amount")) == 100)
      assert(a("eligible_matched_vendor") == "true")
      assert(combined.find(r => r("vendor_id") == "0000000001" && r("fiscal_year") == "2015").get("vendor_name") == "Acme New")
      assert(combined.find(_("vendor_id") == "0000000000").get("eligible_matched_vendor") == "false")
      val cuts = table(out.resolve("cross_vendor_totals.csv"))
      val payment = cuts.find(r => r("side") == "movement" && r("account") == "EXP1" && r("fiscal_year") == "2014").get
      assert(BigDecimal(payment("amount")) == 140 && payment("contributors") == "2")
      assert(table(out.resolve("relationships.csv")).exists(_("agency_description") == "Agency One"))
      assert(table(out.resolve("analysis_packets.csv")).forall(_("vendor_id") == "0000000001"))
      val manifest = table(out.resolve("input_manifest.csv"))
      assert(!manifest.exists(_("path").endsWith("q1exp.csv")))
      assert(manifest.filter(_("kind") == "movement").map(_("rows").toInt).sum == 16)
      assert(manifest.forall(_("sha256").length == 64))
      assert(Files.exists(out.resolve("REPORT_COMPLETE")))
    }
  }
  test("tiny spills and bounded multi-level merge equal a large-buffer execution") {
    withFixture { (dir, params) =>
      val small = run(dir, params, "small", 1, 2)
      val large = run(dir, params, "large", 1000, 16)
      Seq("cross_vendor_totals.csv", "aim_combined.csv", "reconciliation.csv", "relationships.csv").foreach { name =>
        assert(java.util.Arrays.equals(Files.readAllBytes(small.resolve(name)), Files.readAllBytes(large.resolve(name))), name)
      }
      val metrics = table(small.resolve("reduce_metrics.csv")).map(r => r("metric") -> r("value")).toMap
      assert(metrics("merge_passes").toInt >= 2)
    }
  }
  test("a corrupted actual posted balance blocks publication and downstream reduction") {
    withFixture { (dir, params) =>
      val file = dir.resolve("inputs/LDGR2014.csv")
      val lines = scala.io.Source.fromFile(file.toFile); val raw = try lines.getLines().toVector finally lines.close()
      val cols = raw(1).split(",", -1); cols(19) = (BigDecimal(cols(19)) + 1).toString
      write(file, raw.updated(1, cols.mkString(",")))
      val out = dir.resolve("bad")
      intercept[IllegalArgumentException] { CKBViews(params + ("outPath" -> out.toString)) }
      assert(!Files.exists(out.resolve("EXTRACT_COMPLETE")))
      assert(table(out.resolve("reconciliation.csv")).exists(_("status") == "FAIL"))
      intercept[IllegalArgumentException] { ReportingReduce(Map("outPath" -> out.toString)) }
    }
  }
  test("disorder, vendor bounds, reference bounds and duplicate inputs fail closed") {
    withFixture { (dir, params) =>
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("cap").toString, "maxVendorRows" -> "2")) }
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("refcap").toString, "maxReferenceBytes" -> "1")) }
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("dup").toString, "inPaths" -> (params("inPaths") + "," + params("inPaths")))) }
      val f = dir.resolve("inputs/SortedJEFY14merged.csv")
      val src = scala.io.Source.fromFile(f.toFile); val lines = try src.getLines().toVector finally src.close()
      write(f, lines.reverse)
      intercept[IllegalArgumentException] { CKBViews(params + ("outPath" -> dir.resolve("disorder").toString)) }
    }
  }
  test("fiscal-year contract distinguishes payment fiscal labels from calendar PO dates") {
    assert(fiscalYear("0014/07") == 2014)
    assert(fiscalYear("2013/07") == 2014)
    assert(fiscalYear("2014/06") == 2014)
    intercept[IllegalArgumentException] { fiscalYear("0014/13") }
  }
  test("missing balance side, invalid amount and overlapping dimensions remain explicit") {
    withFixture { (dir, params) =>
      val f = dir.resolve("vendors.csv")
      write(f, Seq("instID,instHolderName,instEffectDate,instEffectEndDate", "0000000001,First,2000-01-01,9999-12-31", "0000000001,Second,2000-01-01,9999-12-31"))
      val good = run(dir, params, "ambiguous")
      assert(table(good.resolve("aim_combined.csv")).filter(_("vendor_id") == "0000000001").forall(_("name_status") == "ambiguous"))
      val ledger = dir.resolve("inputs/LDGR2014.csv")
      val src = scala.io.Source.fromFile(ledger.toFile); val lines = try src.getLines().toVector finally src.close()
      write(ledger, lines.take(1) ++ lines.drop(2))
      intercept[IllegalArgumentException] { CKBViews(params + ("outPath" -> dir.resolve("missing-side").toString)) }
      val bad = lines(1).split(",", -1); bad(19) = "not-an-amount"
      write(ledger, lines.updated(1, bad.mkString(",")))
      intercept[NumberFormatException] { CKBViews(params + ("outPath" -> dir.resolve("bad-amount").toString)) }
    }
  }
  test("opening plus movements is a distinct explicit reconciliation basis") {
    withFixture { (dir, params) =>
      val file = dir.resolve("inputs/LDGR2014.csv")
      val src = scala.io.Source.fromFile(file.toFile); val lines = try src.getLines().toVector finally src.close()
      val original = lines(1).split(",", -1)
      val opening = original.clone(); opening(19) = "10"
      val changed = original.clone(); changed(19) = (BigDecimal(changed(19)) + 10).toString
      val openingFile = dir.resolve("opening.csv")
      write(openingFile, Seq(lines.head, opening.mkString(",")))
      write(file, lines.updated(1, changed.mkString(",")))
      val out = run(dir, params ++ Map("openingFiles" -> openingFile.toString, "balanceBasis" -> "opening_plus_movements"), "with-opening")
      assert(table(out.resolve("reconciliation.csv")).forall(_("status") == "PASS"))
    }
  }
  test("export connected fixture for separate-process orchestration when requested") {
    sys.env.get("CKB_REPORT_FIXTURE_EXPORT").foreach { path =>
      val root = Paths.get(path); require(!Files.exists(root)); Files.createDirectories(root)
      fixture(root)
    }
  }
}
