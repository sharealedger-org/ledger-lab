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
  //   col 14 = nigpCode, col 22 = fiscYear, col 24 = transDate, col 25 = amount
  private def makeRow(
    vendorId: String, commitId: String, bizEvent: String,
    agency: String, nigpCode: String, fiscYear: String,
    transDate: String, amount: String
  ): String = {
    // Build a 39-element array with placeholders for unused fields
    val cols = Array.fill(39)(" ")
    cols(0)  = vendorId
    cols(2)  = commitId
    cols(5)  = bizEvent
    cols(12) = agency
    cols(14) = nigpCode
    cols(22) = fiscYear
    cols(24) = transDate
    cols(25) = amount
    cols.mkString(",")
  }

  private def writeSortedJE(dir: Path, yearSuffix: String, rows: String*): Path = {
    val f = dir.resolve(s"SortedJEFY${yearSuffix}q1exp.csv")
    Files.write(f, (rows.mkString("\n") + "\n").getBytes("UTF-8"))
    f
  }

  // Convenience: read all data rows of a CSV (skip header) as arrays of fields
  private def readCsvData(file: File): Vector[Array[String]] = {
    val src = Source.fromFile(file, "UTF-8")
    try src.getLines().drop(1).filter(_.trim.nonEmpty).map(_.split(",", -1)).toVector
    finally src.close()
  }

  // ── Test 1: View A — monthly order volume from COMMITMENT rows ───────────────
  test("CKBViews: View A accumulates COMMITMENT debit rows by fiscal_period × nigp_class_3digit") {
    withTempDirectory("ckbv-viewA-") { tempDir =>
      // Two COMMITMENT debit rows: same nigp3 "081", different fiscal months
      // One COMMITMENT credit row (should be ignored — amount negative)
      val row1 = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "08015", "2003", "2003/09/01", "100.00")
      val row2 = makeRow("v001", "PO2", "COMMITMENT", "AGY1", "08015", "2003", "2003/12/01", "200.00")
      val row3 = makeRow("v001", "PO1", "COMMITMENT", "AGY1", "0000",  "2003", "2003/09/01", "-100.00") // credit offset

      writeSortedJE(tempDir, "03", row1, row2, row3)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val viewA = new File(tempDir.toFile, "view_monthly_order_volume.csv")
      assert(viewA.exists(), "view_monthly_order_volume.csv should be created")

      val rows = readCsvData(viewA)
      // row1 → fiscal_period=2003/09, row2 → 2003/12 — two distinct keys
      assert(rows.length == 2, s"Expected 2 view A rows; got ${rows.length}")

      val byPeriod = rows.map(r => r(0) -> (r(2).toLong, BigDecimal(r(3)))).toMap
      assert(byPeriod("2003/09")._1 == 1L, "2003/09 should have 1 PO line")
      assert(byPeriod("2003/12")._1 == 1L, "2003/12 should have 1 PO line")
      assert(byPeriod("2003/09")._2 == BigDecimal("100.00"), "2003/09 amount should be 100.00")
    }
  }

  // ── Test 2: View B — vendor-year survival present_next_year flag ─────────────
  // Schema: vendor_id(0), vendor_surrogate_key(1), fiscal_year(2), first_order_year(3),
  //         present_next_year(4), total_amount(5), po_count(6)
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

  // ── Test 3: View C — agency-year commodity mix with revision detection ────────
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
      // col 3 = po_count, col 4 = total_committed_amount, col 5 = revision_count
      assert(r(3).toLong == 2L, s"po_count should be 2; got ${r(3)}")
      assert(r(5).toLong == 1L, s"revision_count should be 1 (only -V2 row); got ${r(5)}")
      assert(BigDecimal(r(4)) == BigDecimal("210.00"), s"total should be 210.00; got ${r(4)}")
    }
  }

  // ── Test 4: View D — agency-year payment totals (non-COMMITMENT only) ─────────
  test("CKBViews: View D accumulates non-COMMITMENT debit rows only") {
    withTempDirectory("ckbv-viewD-") { tempDir =>
      // One payment debit, one payment credit (should be ignored), one COMMITMENT (should not appear)
      val payDebit  = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "EXPOBJ1", "2003", "2003/09/01", "500.00")
      val payCredit = makeRow("v001", "0", "EXPENSE_PAYMENT", "AGY1", "0000",    "2003", "2003/09/01", "-500.00")
      val commitRow = makeRow("v002", "PO1", "COMMITMENT",    "AGY1", "08015",   "2003", "2003/09/01", "200.00")

      writeSortedJE(tempDir, "03", payDebit, payCredit, commitRow)

      CKBViews(Map("inPath" -> tempDir.toString, "outPath" -> tempDir.toString, "year" -> "2003"))

      val viewD = new File(tempDir.toFile, "view_agency_year_payment_totals.csv")
      assert(viewD.exists(), "view_agency_year_payment_totals.csv should be created")

      val rows = readCsvData(viewD)
      assert(rows.length == 1, s"Expected 1 row (AGY1/2003); got ${rows.length}")

      val r = rows.head
      // col 2 = total_payment_amount, col 3 = payment_count
      assert(BigDecimal(r(2)) == BigDecimal("500.00"), s"Payment total should be 500.00; got ${r(2)}")
      assert(r(3).toLong == 1L, s"payment_count should be 1; got ${r(3)}")
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
}
