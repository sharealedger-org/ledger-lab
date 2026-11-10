package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.file.{Files, Path, Paths}

import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source

// ── Tests for Step 5v: CKBViews extract-time summarization ──────────────────────────────────────
//
//  Architecture contract being tested:
//    - CKBViews reads SortedJEFY*.csv files (39-field Transaction rows) in a single pass.
//    - It accumulates four bounded aggregation maps keyed on declared grouping keys.
//    - It produces four CSV view files in outPath.
//    - Only debit rows (amount >= 0) are accumulated; credit offset rows are excluded.
//    - COMMITMENT rows feed Views A, B, C.  Non-COMMITMENT rows feed View D.
//    - RESULT/RESOURCE metric lines are emitted.
//    - Memory contract: O(distinct keys) — no ArrayBuffer of all source rows.
//
//  View schemas (enriched 2026-11-04):
//    View A: (fiscal_period, nigp_class_3digit, agency) →
//              po_line_count, total_committed_amount, revision_count
//    View B: (vendor_id, fiscal_year) →
//              vendor_surrogate_key, first_order_year, present_next_year,
//              total_amount, po_count, nigp_class_modal, agency_count, distinct_nigp_classes
//    View C: (agency, fiscal_year, nigp_class_3digit) →
//              po_count, total_committed_amount, revision_count,
//              vendor_count, min_order_amount, max_order_amount
//    View D: (agency, fiscal_year, fund_id, object_code) →
//              total_payment_amount, payment_count
//
//  Fixture: Two synthetic SortedJE rows per journal (1 COMMITMENT debit + 1 COMMITMENT credit)
//           and two synthetic payment rows (1 payment debit + 1 payment credit).

class CKBViewsSpec extends AnyFunSuite {

  private def withTempDirectory(prefix: String)(test: Path => Unit): Unit = {
    val tempRoot = Paths.get("../../session-run.tmp").toAbsolutePath.normalize()
    Files.createDirectories(tempRoot)
    val tempDir = Files.createTempDirectory(tempRoot, prefix)
    try test(tempDir)
    finally deleteRecursively(tempDir)
  }

  private def deleteRecursively(path: Path): Unit = {
    val file = path.toFile
    if (file.isDirectory) Option(file.listFiles()).getOrElse(Array.empty[File]).foreach(child => deleteRecursively(child.toPath))
    Files.deleteIfExists(path)
  }

  // ── Minimal 39-field SJE row builder ────────────────────────────────────────
  // Arguments map to the fields CKBViews reads:
  //   col 0  = vendorId, col 2 = commitId, col 5 = bizEvent, col 12 = agency,
  //   col 13 = fundId,   col 14 = nigpCode, col 22 = fiscYear,
  //   col 24 = transDate, col 25 = amount
  private def makeRow(
    vendorId: String, commitId: String, bizEvent: String,
    agency: String, nigpCode: String, fiscYear: String,
    transDate: String, amount: String,
    fundId: String = "FUND1"
  ): String = {
    // Build a 39-element array with placeholders for unused fields
    val cols = Array.fill(39)(" ")
    cols(0)  = vendorId
    cols(2)  = commitId
    cols(5)  = bizEvent
    cols(12) = agency
    cols(13) = fundId
    cols(14) = nigpCode
    cols(22) = fiscYear
    cols(24) = transDate
    cols(25) = amount
    cols.mkString(",")
  }

  // 5v requires its inputs in engine-key order (SortedJE.sortspec); fixtures are written in that order,
  // as step 2/2p/2m would have produced them.
  private lazy val spec = SortEngine.loadSpec("../sort_specs/SortedJE.sortspec")

  private def inEngineOrder(rows: Seq[String]): Seq[String] =
    rows.map(r => SortEngine.extractKey(r, spec)).sortBy(_.keyStr).map(_.originalRow)

  private def writeSortedJE(dir: Path, yearSuffix: String, rows: String*): Path = {
    val f = dir.resolve(s"SortedJEFY${yearSuffix}q1exp.csv")
    Files.write(f, (inEngineOrder(rows).mkString("\n") + "\n").getBytes("UTF-8"))
    f
  }

  private def writeFile(dir: Path, name: String, rows: Seq[String], sorted: Boolean = true): Path = {
    val f = dir.resolve(name)
    val body = if (sorted) inEngineOrder(rows) else rows
    Files.write(f, (body.mkString("\n") + "\n").getBytes("UTF-8"))
    f
  }

  // Convenience: read all data rows of a CSV (skip header) as arrays of fields
  private def readCsvData(file: File): Vector[Array[String]] = {
    val src = Source.fromFile(file, "UTF-8")
    try src.getLines().drop(1).filter(_.trim.nonEmpty).map(_.split(",", -1)).toVector
    finally src.close()
  }

  // ── Test 1: View A — monthly order volume from COMMITMENT rows ───────────────
  // Schema: fiscal_period(0), nigp_class_3digit(1), agency(2), po_line_count(3),
  //         total_committed_amount(4), revision_count(5)
  test("CKBViews: View A accumulates COMMITMENT debit rows by fiscal_period × nigp_class_3digit × agency") {
    withTempDirectory("ckbv-viewA-") { tempDir =>
      // Two COMMITMENT debit rows: same nigp3 "081", different fiscal months, same agency
      // One COMMITMENT credit row (should be ignored — amount negative)
      val row1 = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00")
      val row2 = makeRow("v001", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/12/01", "200.00")
      val row3 = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "0000",  "2003", "2003/09/01", "-100.00") // credit offset

      writeSortedJE(tempDir, "03", row1, row2, row3)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val viewA = new File(tempDir.toFile, "view_monthly_order_volume.csv")
      assert(viewA.exists(), "view_monthly_order_volume.csv should be created")

      val rows = readCsvData(viewA)
      // row1 → fiscal_period=2003/09, row2 → 2003/12 — two distinct (period, nigp3, agency) keys
      assert(rows.length == 2, s"Expected 2 view A rows; got ${rows.length}")

      // col 0=fiscal_period, col 2=agency, col 3=po_line_count, col 4=total_committed_amount
      val byPeriod = rows.map(r => r(0) -> (r(3).toLong, BigDecimal(r(4)))).toMap
      assert(byPeriod("2003/09")._1 == 1L, "2003/09 should have 1 PO line")
      assert(byPeriod("2003/12")._1 == 1L, "2003/12 should have 1 PO line")
      assert(byPeriod("2003/09")._2 == BigDecimal("100.00"), "2003/09 amount should be 100.00")

      // agency field should be present
      assert(rows.forall(_(2) == "AGY1"), "agency column should be AGY1 for all rows")
    }
  }

  // ── Test 1b: View A — revision_count populated ───────────────────────────────
  test("CKBViews: View A counts revision POs (commitmentID contains -V)") {
    withTempDirectory("ckbv-viewA-rev-") { tempDir =>
      val rowOrig = makeRow("v001", "PO1",       "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00")
      val rowRev  = makeRow("v001", "PO1-V2",    "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "110.00")
      val rowOrig2 = makeRow("v001", "PO2",      "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "90.00")

      writeSortedJE(tempDir, "03", rowOrig, rowRev, rowOrig2)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val rows = readCsvData(new File(tempDir.toFile, "view_monthly_order_volume.csv"))
      assert(rows.length == 1, s"All rows same key; expected 1; got ${rows.length}")
      // col 5 = revision_count
      assert(rows.head(5).toLong == 1L, s"revision_count should be 1; got ${rows.head(5)}")
      // col 3 = po_line_count = 3 (original, revision, another original)
      assert(rows.head(3).toLong == 3L, s"po_line_count should be 3; got ${rows.head(3)}")
    }
  }

  // ── Test 2: View B — vendor-year survival present_next_year flag ─────────────
  // Schema: vendor_id(0), vendor_surrogate_key(1), fiscal_year(2), first_order_year(3),
  //         present_next_year(4), total_amount(5), po_count(6),
  //         nigp_class_modal(7), agency_count(8), distinct_nigp_classes(9)
  test("CKBViews: View B sets present_next_year=1 when vendor appears in a later year") {
    withTempDirectory("ckbv-viewB-") { tempDir =>
      // vendor v001 appears in both 2003 and 2004 → present_next_year=1 for 2003
      // vendor v002 appears only in 2003 → present_next_year=0 for 2003
      val row1  = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "50.00")
      val row2  = makeRow("v001", "PO2", "COMMITMENT", "AGY1", "08015", "2004", "2004/09/01", "75.00")
      val row3  = makeRow("v002", "PO3", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "30.00")

      writeSortedJE(tempDir, "03", row1, row2, row3)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> ""))

      val viewB = new File(tempDir.toFile, "view_vendor_year_survival.csv")
      assert(viewB.exists(), "view_vendor_year_survival.csv should be created")

      val rows = readCsvData(viewB)
      assert(rows.length == 3, s"Expected 3 rows (v001×2003, v001×2004, v002×2003); got ${rows.length}")

      // col 0=vendor_id, col 1=vendor_surrogate_key, col 2=fiscal_year, col 4=present_next_year
      val byKey = rows.map(r => (r(0), r(2)) -> r(4).toInt).toMap
      assert(byKey(("v001", "2003")) == 1, "v001 in 2003 should have present_next_year=1 (appears in 2004)")
      assert(byKey(("v001", "2004")) == 0, "v001 in 2004 should have present_next_year=0 (no later year)")
      assert(byKey(("v002", "2003")) == 0, "v002 in 2003 should have present_next_year=0 (only year)")
    }
  }

  // ── Test 2b: View B — vendor_surrogate_key is deterministic and 8 hex chars ──
  test("CKBViews: View B vendor_surrogate_key is 8-char hex, deterministic, distinct for different vendors") {
    withTempDirectory("ckbv-surrogatekey-") { tempDir =>
      val row1 = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "50.00")
      val row2 = makeRow("v002", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "30.00")

      writeSortedJE(tempDir, "03", row1, row2)
      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> ""))

      val viewB = new File(tempDir.toFile, "view_vendor_year_survival.csv")
      val rows  = readCsvData(viewB)

      // col 1 = vendor_surrogate_key
      val keys = rows.map(_(1))
      keys.foreach { k =>
        assert(k.length == 8,             s"surrogate key should be 8 chars; got '$k'")
        assert(k.matches("[0-9a-f]{8}"),  s"surrogate key should be lowercase hex; got '$k'")
      }
      // same vendor in same row → key matches direct computation
      val v001Row = rows.find(_(0) == "v001").get
      assert(v001Row(1) == CKBViews.vendorSurrogateKey("v001"),
        "surrogate key should match vendorSurrogateKey(\"v001\")")
      // different vendors → different keys
      assert(keys.distinct.length == keys.length, "distinct vendors should produce distinct surrogate keys")
    }
  }

  // ── Test 2c: View B — nigp_class_modal, agency_count, distinct_nigp_classes ──
  test("CKBViews: View B emits nigp_class_modal, agency_count, and distinct_nigp_classes") {
    withTempDirectory("ckbv-viewB-enriched-") { tempDir =>
      // vendor v001 in 2003: three rows — two for class "081", one for class "204", two agencies
      val r1 = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08115", "2003", "2003/09/01", "100.00")
      val r2 = makeRow("v001", "PO2", "COMMITMENT", "AGY1", "08120", "2003", "2003/10/01", "150.00")
      val r3 = makeRow("v001", "PO3", "COMMITMENT", "AGY2", "20401", "2003", "2003/11/01", "200.00")

      writeSortedJE(tempDir, "03", r1, r2, r3)
      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> ""))

      val rows = readCsvData(new File(tempDir.toFile, "view_vendor_year_survival.csv"))
      assert(rows.length == 1, s"Expected 1 row (v001/2003); got ${rows.length}")

      val r = rows.head
      // col 7=nigp_class_modal, col 8=agency_count, col 9=distinct_nigp_classes
      assert(r(7) == "081", s"modal class should be '081' (2 rows vs 1); got '${r(7)}'")
      assert(r(8).toInt == 2, s"agency_count should be 2; got ${r(8)}")
      assert(r(9).toInt == 2, s"distinct_nigp_classes should be 2 (081, 204); got ${r(9)}")
    }
  }

  // ── Test 3: View C — agency-year commodity mix with revision detection ────────
  // Schema: agency(0), fiscal_year(1), nigp_class_3digit(2), po_count(3),
  //         total_committed_amount(4), revision_count(5),
  //         vendor_count(6), min_order_amount(7), max_order_amount(8)
  test("CKBViews: View C counts POs and detects -V revision suffix in commitmentID") {
    withTempDirectory("ckbv-viewC-") { tempDir =>
      // Two rows: one original PO, one revision (commitId contains "-V")
      val rowOrig = makeRow("v001", "A501_0000200268",    "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00")
      val rowRev  = makeRow("v001", "A501_0000200268-V2", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "110.00")

      writeSortedJE(tempDir, "03", rowOrig, rowRev)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val viewC = new File(tempDir.toFile, "view_agency_year_commodity_mix.csv")
      assert(viewC.exists(), "view_agency_year_commodity_mix.csv should be created")

      val rows = readCsvData(viewC)
      assert(rows.length == 1, s"Expected 1 aggregated row (same agency/year/nigp3); got ${rows.length}")

      val r = rows.head
      // col 3=po_count, col 4=total_committed_amount, col 5=revision_count
      assert(r(3).toLong == 2L, s"po_count should be 2; got ${r(3)}")
      assert(r(5).toLong == 1L, s"revision_count should be 1 (only -V2 row); got ${r(5)}")
      assert(BigDecimal(r(4)) == BigDecimal("210.00"), s"total should be 210.00; got ${r(4)}")
    }
  }

  // ── Test 3b: View C — vendor_count, min_order_amount, max_order_amount ────────
  test("CKBViews: View C emits vendor_count, min_order_amount, max_order_amount") {
    withTempDirectory("ckbv-viewC-enriched-") { tempDir =>
      // Two different vendors, same agency-year-class
      val r1 = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "50.00")
      val r2 = makeRow("v002", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/10/01", "200.00")
      val r3 = makeRow("v001", "PO3", "COMMITMENT", "AGY1", "08015", "2003", "2003/11/01", "100.00")

      writeSortedJE(tempDir, "03", r1, r2, r3)
      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val rows = readCsvData(new File(tempDir.toFile, "view_agency_year_commodity_mix.csv"))
      assert(rows.length == 1, s"Expected 1 aggregated row; got ${rows.length}")

      val r = rows.head
      // col 6=vendor_count, col 7=min_order_amount, col 8=max_order_amount
      assert(r(6).toInt == 2, s"vendor_count should be 2 (v001, v002); got ${r(6)}")
      assert(BigDecimal(r(7)) == BigDecimal("50.00"),  s"min should be 50.00; got ${r(7)}")
      assert(BigDecimal(r(8)) == BigDecimal("200.00"), s"max should be 200.00; got ${r(8)}")
    }
  }

  // ── Test 4: View D — agency-year payment totals (non-COMMITMENT only) ─────────
  // Schema: agency(0), fiscal_year(1), fund_id(2), object_code(3),
  //         total_payment_amount(4), payment_count(5)
  test("CKBViews: View D accumulates non-COMMITMENT debit rows only, keyed by fund_id and object_code") {
    withTempDirectory("ckbv-viewD-") { tempDir =>
      // One payment debit, one payment credit (should be ignored), one COMMITMENT (should not appear in D)
      val payDebit  = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "EXPOBJ1", "2003", "2003/09/01", "500.00",
                               fundId = "FUND1")
      val payCredit = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "0000",    "2003", "2003/09/01", "-500.00",
                               fundId = "FUND1")
      val commitRow = makeRow("v002", "PO1", "COMMITMENT",    "AGY1", "08015",   "2003", "2003/09/01", "200.00")

      writeSortedJE(tempDir, "03", payDebit, payCredit, commitRow)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val viewD = new File(tempDir.toFile, "view_agency_year_payment_totals.csv")
      assert(viewD.exists(), "view_agency_year_payment_totals.csv should be created")

      val rows = readCsvData(viewD)
      assert(rows.length == 1, s"Expected 1 row (AGY1/2003/FUND1/EXPOBJ1); got ${rows.length}")

      val r = rows.head
      // col 2=fund_id, col 3=object_code, col 4=total_payment_amount, col 5=payment_count
      assert(r(2) == "FUND1",   s"fund_id should be FUND1; got ${r(2)}")
      assert(r(3) == "EXPOBJ1", s"object_code should be EXPOBJ1; got ${r(3)}")
      assert(BigDecimal(r(4)) == BigDecimal("500.00"), s"Payment total should be 500.00; got ${r(4)}")
      assert(r(5).toLong == 1L, s"payment_count should be 1; got ${r(5)}")
    }
  }

  // ── Test 4b: View D — separate rows for different fund_id × object_code ───────
  test("CKBViews: View D produces separate rows for different fund_id or object_code combinations") {
    withTempDirectory("ckbv-viewD-multikey-") { tempDir =>
      val r1 = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "OBJ1", "2003", "2003/09/01", "100.00", fundId = "F1")
      val r2 = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "OBJ2", "2003", "2003/09/01", "200.00", fundId = "F1")
      val r3 = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "OBJ1", "2003", "2003/09/01", "300.00", fundId = "F2")

      writeSortedJE(tempDir, "03", r1, r2, r3)
      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val rows = readCsvData(new File(tempDir.toFile, "view_agency_year_payment_totals.csv"))
      assert(rows.length == 3, s"Expected 3 rows for 3 distinct (fund,obj) keys; got ${rows.length}")
    }
  }

  // ── Test 5: all four view files are produced and non-empty ───────────────────
  test("CKBViews: all four view output files are created") {
    withTempDirectory("ckbv-allfiles-") { tempDir =>
      // One row that contributes to all four views
      val commitDebit = makeRow("v001", "PO1", "COMMITMENT",    "AGY1", "08015", "2003", "2003/09/01", "100.00")
      val payDebit    = makeRow("v001", "0",   "EXPENSE_PAYMENT","AGY1","EXPOBJ1","2003","2003/09/01", "200.00")

      writeSortedJE(tempDir, "03", commitDebit, payDebit)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val viewFiles = Seq(
        "view_monthly_order_volume.csv",
        "view_vendor_year_survival.csv",
        "view_agency_year_commodity_mix.csv",
        "view_agency_year_payment_totals.csv"
      ).map(n => new File(tempDir.toFile, n))

      viewFiles.foreach { f =>
        assert(f.exists(),    s"${f.getName} should be created")
        assert(f.length() > 0L, s"${f.getName} should be non-empty")
      }
    }
  }

  // ── Test 6: RESULT and RESOURCE metric lines emitted ─────────────────────────
  test("CKBViews: RESULT and RESOURCE metric log lines are emitted") {
    withTempDirectory("ckbv-metrics-") { tempDir =>
      val row = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00")
      writeSortedJE(tempDir, "03", row)

      val output = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(output)) {
        CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))
      }
      val log = output.toString("UTF-8")
      assert(log.contains("RESULT step=5v"), s"Expected 'RESULT step=5v' in output; got:\n$log")
      assert(log.contains("RESOURCE step=5v"), s"Expected 'RESOURCE step=5v' in output; got:\n$log")
    }
  }

  // ── Test 7: deriveFiscalMonth handles both date formats ──────────────────────
  test("CKBViews.deriveFiscalMonth handles YYYY/MM/DD and MM/DD/YYYY formats") {
    assert(CKBViews.deriveFiscalMonth("2003/09/01", "2003") == "2003/09",
      "YYYY/MM/DD should produce YYYY/MM")
    assert(CKBViews.deriveFiscalMonth("09/23/2002", "2003") == "2002/09",
      "MM/DD/YYYY should produce YYYY/MM")
    assert(CKBViews.deriveFiscalMonth("", "2003") == "2003/01",
      "Empty date should fall back to YYYY/01")
    assert(CKBViews.deriveFiscalMonth("unparseable", "2003") == "2003/01",
      "Unparseable date should fall back to YYYY/01")
  }

  // ── Test 8: year filter restricts files processed ───────────────────────────
  test("CKBViews: year parameter filters to matching SortedJE files only") {
    withTempDirectory("ckbv-yearfilter-") { tempDir =>
      // Write two files: FY03 (COMMITMENT) and FY04 (payment).
      // With year=2003, only the FY03 file should be processed.
      val commitRow03 = makeRow("v001", "PO1", "COMMITMENT",    "AGY1","08015","2003","2003/09/01","50.00")
      val payRow04    = makeRow("v002", "0",   "EXPENSE_PAYMENT","AGY2","OBJ1", "2004","2004/09/01","75.00")

      writeSortedJE(tempDir, "03", commitRow03)
      val f04 = tempDir.resolve("SortedJEFY04q1exp.csv")
      Files.write(f04, (payRow04 + "\n").getBytes("UTF-8"))

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      // View D should be empty (FY04 payment file was excluded)
      val viewD = new File(tempDir.toFile, "view_agency_year_payment_totals.csv")
      val rows = readCsvData(viewD)
      assert(rows.isEmpty, s"View D should be empty when year=2003 (FY04 excluded); got ${rows.length} rows")

      // View A should have 1 entry from the FY03 COMMITMENT
      val viewA = new File(tempDir.toFile, "view_monthly_order_volume.csv")
      val rowsA = readCsvData(viewA)
      assert(rowsA.length == 1, s"View A should have 1 row from FY03; got ${rowsA.length}")
    }
  }

  // ── Test 9: Virginia fiscal year used for present_next_year only ────────────
  test("CKBViews.virginiaFiscalYear: YYYY is a fiscal year; YYYY/MM maps July-June; other shapes never count as later") {
    assert(CKBViews.virginiaFiscalYear("2003")    == 2003)
    assert(CKBViews.virginiaFiscalYear("2002/06") == 2002, "June belongs to the fiscal year ending that June")
    assert(CKBViews.virginiaFiscalYear("2002/07") == 2003, "July starts the next fiscal year")
    assert(CKBViews.virginiaFiscalYear("2002/12") == 2003)
    assert(CKBViews.virginiaFiscalYear("")        == -1)
    assert(CKBViews.virginiaFiscalYear("unknown") == -1)
  }

  // ── Test 10: calendar-month periods are compared as fiscal years ────────────
  test("CKBViews: present_next_year compares Virginia fiscal years when column 22 is YYYY/MM") {
    withTempDirectory("ckbv-vafy-") { tempDir =>
      // 2003/05 -> FY2003; 2003/08 -> FY2004; 2003/11 -> FY2004
      val a = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003/05", "2003/05/01", "10.00")
      val b = makeRow("v001", "PO2", "COMMITMENT", "AGY1", "08015", "2003/08", "2003/08/01", "20.00")
      val c = makeRow("v001", "PO3", "COMMITMENT", "AGY1", "08015", "2003/11", "2003/11/01", "30.00")
      writeSortedJE(tempDir, "03", a, b, c)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> ""))

      val byFy = readCsvData(new File(tempDir.toFile, "view_vendor_year_survival.csv")).map(r => r(2) -> r(4).toInt).toMap
      assert(byFy("2003/05") == 1, "FY2003 order, later FY2004 orders exist")
      assert(byFy("2003/08") == 0, "2003/08 and 2003/11 are the same fiscal year (FY2004)")
      assert(byFy("2003/11") == 0)
    }
  }

  // ── Test 11: all years flow through one ordered merge ───────────────────────
  test("CKBViews: inPaths merges every year's merged file in one pass; a vendor is one key group across years") {
    withTempDirectory("ckbv-multi-") { tempDir =>
      val dir03 = Files.createDirectory(tempDir.resolve("FY03"))
      val dir04 = Files.createDirectory(tempDir.resolve("FY04"))
      val out   = tempDir.resolve("views")

      writeFile(dir03, "SortedJEFY03merged.csv", Seq(
        makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00"),
        makeRow("v002", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "30.00")))
      writeFile(dir04, "SortedJEFY04merged.csv", Seq(
        makeRow("v001", "PO3", "COMMITMENT", "AGY1", "08015", "2004", "2004/09/01", "50.00"),
        makeRow("v003", "PO4", "COMMITMENT", "AGY1", "08015", "2004", "2004/09/01", "70.00")))

      val log = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(log)) {
        CKBViews(Map("inPaths" -> s"$dir03,$dir04", "outPath" -> out.toString, "year" -> "2004"))
      }
      assert(log.toString("UTF-8").contains("records_read=4 "), s"Expected records_read=4; got:\n${log.toString("UTF-8")}")
      assert(log.toString("UTF-8").contains("files_read=2 "))

      val b = readCsvData(out.resolve("view_vendor_year_survival.csv").toFile)
      val byKey = b.map(r => (r(0), r(2)) -> r(4).toInt).toMap
      assert(b.length == 4, s"v001x2003, v001x2004, v002x2003, v003x2004; got ${b.length}")
      assert(byKey(("v001", "2003")) == 1, "v001 reappears in FY04")
      assert(byKey(("v001", "2004")) == 0)
      assert(byKey(("v002", "2003")) == 0)
      assert(byKey(("v003", "2004")) == 0)
      // first_order_year is the vendor-year's own earliest period; same vendor, two keys
      assert(b.find(r => r(0) == "v001" && r(2) == "2004").get(3) == "2004")

      // Control total: View A total equals the sum of the four debit rows
      val totalA = readCsvData(out.resolve("view_monthly_order_volume.csv").toFile).map(r => BigDecimal(r(4))).sum
      assert(totalA == BigDecimal("250.00"), s"Expected 250.00; got $totalA")
    }
  }

  // ── Test 12: vendor_count counts a vendor once per key even when it spans files ─
  test("CKBViews: View C vendor_count counts a vendor once across files") {
    withTempDirectory("ckbv-vendorcount-") { tempDir =>
      val dir03 = Files.createDirectory(tempDir.resolve("FY03"))
      val dir04 = Files.createDirectory(tempDir.resolve("FY04"))
      val out   = tempDir.resolve("views")
      // same agency / fiscal_year / class on both sides; v001 appears in both files
      writeFile(dir03, "SortedJEFY03merged.csv", Seq(
        makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "10.00"),
        makeRow("v002", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "20.00")))
      writeFile(dir04, "SortedJEFY04merged.csv", Seq(
        makeRow("v001", "PO3", "COMMITMENT", "AGY1", "08015", "2003", "2003/10/01", "30.00")))

      CKBViews(Map("inPaths" -> s"$dir03,$dir04", "outPath" -> out.toString, "year" -> ""))

      val c = readCsvData(out.resolve("view_agency_year_commodity_mix.csv").toFile)
      assert(c.length == 1)
      assert(c.head(3).toLong == 3L, "po_count 3")
      assert(c.head(6).toLong == 2L, s"vendor_count must be 2 (v001 once, v002 once); got ${c.head(6)}")
    }
  }

  // ── Test 13: one read per row ───────────────────────────────────────────────
  test("CKBViews: a year's merged file is read instead of (not in addition to) its 2m inputs") {
    withTempDirectory("ckbv-oneread-") { tempDir =>
      val rows = Seq(
        makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00"),
        makeRow("v002", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "30.00"))
      writeFile(tempDir, "SortedJEFY03q1PO.csv", rows)       // 2m input
      writeFile(tempDir, "SortedJEFY03merged.csv", rows)     // its merged output

      val log = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(log)) {
        CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))
      }
      assert(log.toString("UTF-8").contains("records_read=2 "), s"double read; got:\n${log.toString("UTF-8")}")
      val a = readCsvData(new File(tempDir.toFile, "view_monthly_order_volume.csv"))
      assert(BigDecimal(a.head(4)) == BigDecimal("130.00"))
    }
  }

  // ── Test 14: order guard stops processing ───────────────────────────────────
  test("CKBViews: an input out of engine-key order stops the step") {
    withTempDirectory("ckbv-orderguard-") { tempDir =>
      val rows = Seq(
        makeRow("v002", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "30.00"),
        makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00"))
      writeFile(tempDir, "SortedJEFY03merged.csv", rows, sorted = false)   // descending: out of order

      val ex = intercept[IllegalStateException] {
        CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))
      }
      assert(ex.getMessage.contains("order guard"), ex.getMessage)
      assert(ex.getMessage.contains("SortedJEFY03merged.csv line 2"), ex.getMessage)
    }
  }

  // ── Test 15: engine key can interleave a vendor with a longer vendor id ─────
  test("CKBViews: a vendor whose key group is split by another vendor stops the step") {
    withTempDirectory("ckbv-splitgroup-") { tempDir =>
      // Engine key = col 0 ++ cols 9..22.  "V1"+"ZZZ" sorts after "V1X"+"AAA", so V1 is split by V1X.
      def withCol9(r: String, v: String): String = { val c = r.split(",", -1); c(9) = v; c.mkString(",") }
      val rows = Seq(
        withCol9(makeRow("V1",  "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "10.00"), "AAA"),
        withCol9(makeRow("V1X", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "20.00"), "AAA"),
        withCol9(makeRow("V1",  "PO3", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "30.00"), "ZZZ"))
      writeFile(tempDir, "SortedJEFY03merged.csv", rows)

      val ex = intercept[IllegalStateException] {
        CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))
      }
      assert(ex.getMessage.contains("key group reappears"), ex.getMessage)
    }
  }

  // ── Test 16: GERS-003 bounded aggregation buffer overflow, spill & reduce ───
  test("CKBViews: GERS-003 bounded View D buffer spills and reduces to exact in-memory result") {
    withTempDirectory("ckbv-gers003-") { tempDir =>
      val inDir   = Files.createDirectory(tempDir.resolve("in"))
      val outSpill = Files.createDirectory(tempDir.resolve("outSpill"))
      val outMem   = Files.createDirectory(tempDir.resolve("outMem"))

      // 6 payment rows across 4 distinct keys, designed to trigger spills with buffer capacity = 2
      val p1 = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "OBJ1", "2003", "2003/09/01", "100.00", fundId = "F1")
      val p2 = makeRow("v002", "0", "EXPENSE_PAYMENT", "AGY1", "OBJ2", "2003", "2003/09/01", "200.00", fundId = "F1")
      val p3 = makeRow("v003", "0", "EXPENSE_PAYMENT", "AGY2", "OBJ1", "2003", "2003/09/01", "300.00", fundId = "F1")
      val p4 = makeRow("v004", "0", "EXPENSE_PAYMENT", "AGY2", "OBJ2", "2003", "2003/09/01", "400.00", fundId = "F2")
      val p5 = makeRow("v005", "0", "EXPENSE_PAYMENT", "AGY1", "OBJ1", "2003", "2003/09/01", "50.00",  fundId = "F1")
      val p6 = makeRow("v006", "0", "EXPENSE_PAYMENT", "AGY2", "OBJ2", "2003", "2003/09/01", "75.00",  fundId = "F2")

      writeFile(inDir, "SortedJEFY03merged.csv", Seq(p1, p2, p3, p4, p5, p6))

      // 1. Run with capacity = 2 (triggers buffer overflow, spills, and k-way reduce)
      val logOutput = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(logOutput)) {
        CKBViews(Map(
          "inPath"              -> inDir.toString,
          "outPath"             -> outSpill.toString,
          "year"                -> "2003",
          "viewDBufferCapacity" -> "2"
        ))
      }
      val log = logOutput.toString("UTF-8")
      assert(log.contains("buffer_overflow=true"), s"Expected buffer_overflow=true; got:\n$log")
      assert(log.contains("partial_aggregate_rows="), s"Expected partial_aggregate_rows in metrics; got:\n$log")

      // Confirm no temporary spill files remain
      val tmpFiles = Option(outSpill.toFile.listFiles()).getOrElse(Array.empty[File]).filter(_.getName.endsWith(".tmp"))
      assert(tmpFiles.isEmpty, s"All temporary spill files should be deleted; found: ${tmpFiles.map(_.getName).mkString(", ")}")

      // 2. Run with capacity = 1000 (completes entirely in-memory)
      CKBViews(Map(
        "inPath"              -> inDir.toString,
        "outPath"             -> outMem.toString,
        "year"                -> "2003",
        "viewDBufferCapacity" -> "1000"
      ))

      // 3. Verify exact row-for-row equality between spilled reduce and in-memory outputs
      val rowsSpill = readCsvData(outSpill.resolve("view_agency_year_payment_totals.csv").toFile)
      val rowsMem   = readCsvData(outMem.resolve("view_agency_year_payment_totals.csv").toFile)

      assert(rowsSpill.length == 4, s"Expected 4 distinct key rows; got ${rowsSpill.length}")
      assert(rowsSpill.length == rowsMem.length, s"Spill count (${rowsSpill.length}) must equal memory count (${rowsMem.length})")

      for (i <- rowsSpill.indices) {
        assert(rowsSpill(i).toSeq == rowsMem(i).toSeq,
          s"Row $i mismatch:\nSpill: ${rowsSpill(i).mkString(",")}\nMem:   ${rowsMem(i).mkString(",")}")
      }

      // Check specific values for AGY1/2003/F1/OBJ1 (100.00 + 50.00 = 150.00, count 2)
      val agy1Obj1 = rowsSpill.find(r => r(0) == "AGY1" && r(2) == "F1" && r(3) == "OBJ1").get
      assert(BigDecimal(agy1Obj1(4)) == BigDecimal("150.00"), s"Amount should be 150.00; got ${agy1Obj1(4)}")
      assert(agy1Obj1(5).toLong == 2L, s"Count should be 2; got ${agy1Obj1(5)}")
    }
  }
}
