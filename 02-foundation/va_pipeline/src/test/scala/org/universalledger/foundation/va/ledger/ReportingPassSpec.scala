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
    CKBViews(params ++ Map("outPath" -> out.toString, "spillRows" -> buffer.toString,
      "aggregateKeys" -> params.getOrElse("aggregateKeys", buffer.toString), "mergeFanIn" -> fanIn.toString))
    ReportingReduce(Map("outPath" -> out.toString, "mergeFanIn" -> fanIn.toString))
    out
  }
  test("canonical quotes remain literal through actual posting and indexed reporting") {
    val root=Files.createTempDirectory(Paths.get("../../session-run.tmp"),"literal-quotes-")
    val input=Files.createDirectories(root.resolve("input"))
    val quoted="\"VENDOR\" unusual quoted description"
    val commitment="\"PO\"literal"
    val rows=pair("0000000000","0014/07","17.25","EXP1","EXPENSE_PAYMENT").map { line=>
      val r=line.split(",",-1);r(2)=commitment;r(8)=quoted;r.mkString(",")
    }
    write(input.resolve("SortedJEFY14merged.csv"),SortEngine.sortChunk(rows,spec))
    post(input.toString+"/",input.toString+"/","14")
    val out=run(root,Map("inPaths"->input.toString,"reportProfile"->"expanded","packetVendors"->"0000000000"),"report")
    assert(table(out.resolve("reconciliation.csv")).forall(_("status")=="PASS"))
    val packets=table(out.resolve("analysis_packets.csv"))
    def field(r:Map[String,String],index:Int):String={
      val p=CSVParser.parse(r("canonical_record_csv"),CSVFormat.DEFAULT)
      try p.getRecords.get(0).get(index) finally p.close()
    }
    assert(packets.exists(r=>r("kind")=="movement" && field(r,8)==quoted))
    assert(packets.exists(r=>r("kind")=="balance" && field(r,2)==commitment))
    assert(table(out.resolve("input_manifest.csv")).filter(r=>Set("movement","balance")(r("kind"))).map(_("rows").toLong).sum==4)
  }
  test("real posting output reconciles; features and effective dimensions remain aligned") {
    withFixture { (dir, params) =>
      val out = run(dir, params, "report")
      assert(table(out.resolve("reconciliation.csv")).forall(_("status") == "PASS"))
      val combined = table(out.resolve("aim_combined.csv"))
      val a = combined.find(r => r("vendor_id") == "0000000001" && r("fiscal_year") == "2014").get
      assert(a("vendor_name") == "Acme, Old")
      assert(a("name_status") == "matched_asof")
      assert(a("currency") == "USD")
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
      manifest.foreach { r =>
        val bytes = Files.readAllBytes(new File(r("path")).toPath)
        val expected = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).map(b => f"${b & 255}%02x").mkString
        assert(r("sha256") == expected)
      }
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
  test("feature bounds, reference bounds and duplicate inputs fail closed; disorder is indexed") {
    withFixture { (dir, params) =>
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("cap").toString, "maxFeatureKeys" -> "2")) }
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("refcap").toString, "maxReferenceBytes" -> "1")) }
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("indexcap").toString, "maxIndexedBlocks" -> "1")) }
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("recordcap").toString, "maxRecordBytes" -> "10")) }
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> dir.resolve("dup").toString, "inPaths" -> (params("inPaths") + "," + params("inPaths")))) }
      val f = dir.resolve("inputs/SortedJEFY14merged.csv")
      val src = scala.io.Source.fromFile(f.toFile); val lines = try src.getLines().toVector finally src.close()
      write(f, lines.reverse)
      val reordered = run(dir, params, "disorder")
      assert(table(reordered.resolve("reconciliation.csv")).forall(_("status") == "PASS"))
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
  test("literal variable-width IDs retain identity across arbitrary physical order and dimension joins") {
    withFixture { (dir, params) =>
      val input = Files.createDirectory(dir.resolve("literal-input"))
      val rows = pair("c12464", "0014/07", "7", "EXP1", "PAYMENT") ++
        pair("c1246", "0014/07", "11", "EXP1", "PAYMENT") ++
        pair("999999999999", "0014/07", "13", "EXP1", "PAYMENT")
      write(input.resolve("SortedJEFY14merged.csv"), SortEngine.sortChunk(rows, spec))
      post(input.toString + "/", input.toString + "/", "14")
      write(dir.resolve("literal-vendors.csv"), Seq("instID,instHolderName", "c12464,Simulated Four", "c1246,Simulated Six"))
      val out = run(dir, params ++ Map("inPaths" -> input.toString, "vendorMaster" -> dir.resolve("literal-vendors.csv").toString), "literal")
      val combined = table(out.resolve("aim_combined.csv"))
      assert(combined.find(_("vendor_id") == "c1246").get("vendor_name") == "Simulated Six")
      assert(combined.find(_("vendor_id") == "c12464").get("vendor_name") == "Simulated Four")
      assert(combined.find(_("vendor_id") == "999999999999").get("attributed") == "false")
      assert(table(out.resolve("reconciliation.csv")).forall(_("status") == "PASS"))
    }
  }
  test("source currencies cannot mix merely because their currency types match") {
    withFixture { (dir, params) =>
      val input = Files.createDirectory(dir.resolve("currency-input"))
      val usd = pair("0000000001", "0014/07", "10", "EXP1", "PAYMENT")
      val eur = pair("0000000001", "0014/07", "20", "EXP1", "PAYMENT").map { line =>
        val r = line.split(",", -1); r(18) = "EUR"; r(20) = "EUR"; r.mkString(",")
      }
      write(input.resolve("SortedJEFY14merged.csv"), SortEngine.sortChunk(usd ++ eur, spec))
      post(input.toString + "/", input.toString + "/", "14")
      val error = intercept[IllegalArgumentException] {
        CKBViews(params ++ Map("inPaths" -> input.toString, "outPath" -> dir.resolve("mixed-currency").toString))
      }
      assert(error.getMessage.contains("mixes currencies"))
    }
  }
  test("a legitimate and an unattributed group exceed the old row guard with bounded aggregation") {
    withFixture { (dir, params) =>
      val input = Files.createDirectory(dir.resolve("large-input"))
      val p = new java.io.PrintWriter(input.resolve("SortedJEFY14merged.csv").toFile, "UTF-8")
      try Seq("0000000000", "SIM_VENDOR_0001").foreach { id =>
        val sorted = SortEngine.sortChunk(pair(id, "0014/07", "1.25", "EXP1", "PAYMENT"), spec)
        sorted.foreach { row =>
          (1 to 125001).foreach { n =>
            val r = row.split(",", -1); r(3) = s"simulated-$id-$n"; p.println(r.mkString(","))
          }
        }
      } finally p.close()
      post(input.toString + "/", input.toString + "/", "14")
      val out = run(dir, params + ("inPaths" -> input.toString) + ("aggregateKeys" -> "1"), "large-group")
      assert(table(out.resolve("reconciliation.csv")).forall(_("status") == "PASS"))
      val feature = table(out.resolve("aim_combined.csv")).find(_("vendor_id") == "SIM_VENDOR_0001").get
      assert(BigDecimal(feature("debit_amount")) == BigDecimal("156251.25"))
      val metrics = table(out.resolve("extract_metrics.csv")).map(r => r("metric") -> r("value")).toMap
      assert(metrics("peak_vendor_rows").toLong > 250000)
      assert(metrics("aggregate_capacity_keys") == "1")
    }
  }
  test("synthetic date cuts and master dimensions conserve actual posted inputs with bounded spill equivalence") {
    withFixture { (dir, params) =>
      val input = Files.createDirectory(dir.resolve("dated-input"))
      def dated(amount: String, day: String): Seq[String] = pair("0000000001", "2014/06", amount, "EXP1", "PAYMENT").map { line =>
        val r = line.split(",", -1); r(24) = day; r.mkString(",")
      }
      val rows = dated("100", "2014/06/22") ++ dated("25", "06-23-14") ++ dated("-10", "2014-06-24") ++
        pair("0000000002", "2013/07", "40", "EXP1", "PAYMENT") ++ pair("0000000000", "2014/06", "300", "REV1", "REVENUE_RECEIPT")
      write(input.resolve("SortedJEFY14merged.csv"), SortEngine.sortChunk(rows, spec))
      post(input.toString + "/", input.toString + "/", "14")
      write(dir.resolve("assignments.csv"), Seq("vendor_id,template_instrument_id", "0000000001,SIM_I000001", "0000000002,SIM_I000002"))
      write(dir.resolve("enriched.csv"), Seq("instID,instHolderName,instEffectDate,instEffectEndDate,instSizeTier,instCounterpartySector", "0000000001,Simulated Acme,2000-01-01,9999-99-99,LARGE,COMMERCIAL"))
      val p = params ++ Map("inPaths" -> input.toString, "vendorMaster" -> dir.resolve("enriched.csv").toString,
        "syntheticHistories" -> "../../data/synthetic/reclass_history_v1", "syntheticRunDate" -> "2015-07-01", "syntheticAsOf" -> "2014-06-30", "syntheticAssignments" -> dir.resolve("assignments.csv").toString)
      val small = run(dir, p + ("syntheticAggregateKeys" -> "1"), "dated-small", 1, 2)
      val large = run(dir, p + ("syntheticAggregateKeys" -> "1000"), "dated-large", 1000, 16)
      Seq("vendor_temporal_cuts.csv", "vendor_attribute_totals.csv", "synthetic_conservation.csv", "synthetic_vendor_coverage.csv", "vendor_temporal_summary.csv", "vendor_attribute_summary.csv").foreach { name =>
        assert(java.util.Arrays.equals(Files.readAllBytes(small.resolve(name)), Files.readAllBytes(large.resolve(name))), name)
      }
      assert(table(small.resolve("synthetic_validation.csv")).size == 20)
      assert(table(small.resolve("synthetic_validation.csv")).forall(_("status") == "PASS"))
      assert(table(small.resolve("synthetic_conservation.csv")).forall(_("status") == "PASS"))
      val cuts = table(small.resolve("vendor_temporal_cuts.csv")).filter(r => r("vendor_id") == "0000000001" && r("side") == "movement" && r("direction") == "D")
      def sum(rs: Vector[Map[String, String]]) = rs.map(r => BigDecimal(r("amount"))).sum
      val switch = cuts.filter(_("date_mode") == "EVENT_DATE")
      assert(sum(switch.filter(_("synthetic_hierarchy_path").contains("SIM_S01"))) == 100)
      assert(sum(switch.filter(_("synthetic_hierarchy_path").contains("SIM_S02"))) == 15)
      Seq("AS_OF", "RUN_DATE").foreach { mode =>
        val recast = cuts.filter(_("date_mode") == mode)
        assert(sum(recast) == 115 && recast.forall(_("synthetic_hierarchy_path").contains("SIM_S02")))
      }
      val coverage = table(small.resolve("synthetic_vendor_coverage.csv"))
      assert(coverage.find(_("vendor_id") == "0000000000").get("template_instrument_id").isEmpty)
      assert(coverage.forall(_("identity_rekeyed") == "false"))
      val attributes = table(small.resolve("vendor_attribute_totals.csv"))
      assert(attributes.exists(r => r("vendor_id") == "0000000001" && r("attribute") == "instSizeTier" && r("attribute_value") == "LARGE"))
      assert(attributes.exists(r => r("vendor_id") == "0000000002" && r("attribute_status") == "missing_vendor"))
      val m = table(small.resolve("synthetic_metrics.csv")).map(r => r("metric") -> r("value")).toMap
      assert(m("financial_rows").toLong == rows.size + table(input.resolve("LDGR2014.csv")).size)
      assert(m("peak_partial_keys") == "1")
    }
  }
  test("synthetic reporting rejects incorrect history expectations instead of publishing") {
    withFixture { (dir, params) =>
      val pack = Files.createDirectory(dir.resolve("bad-pack"))
      val source = new File("../../data/synthetic/reclass_history_v1")
      source.listFiles().filter(_.isFile).foreach(f => Files.copy(f.toPath, pack.resolve(f.getName)))
      val file = pack.resolve("lookup_cases.csv")
      val text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
      Files.write(file, text.replace("REF-agency-SIM_A405-v2", "WRONG_VERSION").getBytes(StandardCharsets.UTF_8))
      val out = dir.resolve("bad-synthetic")
      intercept[IllegalArgumentException] { CKBViews(params ++ Map("outPath" -> out.toString, "syntheticHistories" -> pack.toString, "syntheticRunDate" -> "2015-07-01")) }
      assert(!Files.exists(out.resolve("EXTRACT_COMPLETE")))
    }
  }
  test("export connected fixture for separate-process orchestration when requested") {
    sys.env.get("CKB_REPORT_FIXTURE_EXPORT").foreach { path =>
      val root = Paths.get(path); require(!Files.exists(root)); Files.createDirectories(root)
      fixture(root)
    }
  }
}
