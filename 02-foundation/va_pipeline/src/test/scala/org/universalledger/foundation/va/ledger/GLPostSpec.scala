package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.file.{Files, Path, Paths}

import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source

class GLPostSpec extends AnyFunSuite {

  // ── helpers ──────────────────────────────────────────────────────────────────

  private def withTempDirectory(prefix: String)(test: Path => Unit): Unit = {
    val tempRoot = Paths.get("../../session-run.tmp").toAbsolutePath.normalize()
    Files.createDirectories(tempRoot)
    val tempDir = Files.createTempDirectory(tempRoot, prefix)
    try test(tempDir)
    finally deleteRecursively(tempDir)
  }

  private def deleteRecursively(path: Path): Unit = {
    val file = path.toFile
    if (file.isDirectory)
      Option(file.listFiles()).getOrElse(Array.empty[File]).foreach(child => deleteRecursively(child.toPath))
    Files.deleteIfExists(path)
  }

  /**
   * Build a minimal 39-field SortedJE row.
   *
   * Column layout (Transaction.scala field order, per standardizeAndSort.scala):
   *   0  transIPID              — vendor/instrument ID
   *  12  transLegalEntityID     — agency
   *  13  transCenterID          — fund
   *  14  transProjectID         — object
   *  16  transNominalAccountID  — nominal account
   *  22  transFiscalPeriod      — fiscal year
   *  25  transTransAmount       — amount
   *  31  transDirVsOffsetFlg    — "D" (debit line) or "O" (offset line)
   */
  private def makeJERow(
    vendorId:   String,
    agency:     String,
    fund:       String,
    nomAcct:    String,
    fiscalYear: String,
    amount:     String,
    dirFlag:    String,
    objectCode: String = "OBJ001",
    acctDate:   String = "2014/03/01",
    jrnlId:     String = "ID001"
  ): String = {
    val cols = Array.fill(39)("0")
    cols(0)  = vendorId    // transIPID
    cols(1)  = "0"         // transContractID
    cols(2)  = "0"         // transCommitmentID
    cols(3)  = jrnlId      // transJrnlID
    cols(4)  = "1"         // transJrnlLineID
    cols(5)  = "EXPENSE_PAYMENT"  // transBusinessEventCode
    cols(6)  = "Payment Sys"      // transSourceSystemID
    cols(7)  = "Unknown"          // transOriginalDocID
    cols(8)  = "Test"             // transJrnlDescript
    cols(9)  = "ACTUALS"          // transLedgerID
    cols(10) = "FIN"              // transJrnlType
    cols(11) = "SHRD-3RD-PARTY"   // transBookCodeID
    cols(12) = agency      // transLegalEntityID
    cols(13) = fund        // transCenterID
    cols(14) = objectCode  // transProjectID
    cols(15) = "0"         // transProductID
    cols(16) = nomAcct     // transNominalAccountID
    cols(17) = "0"         // transAltAccountID
    cols(18) = "USD"       // transCurrencyCodeSourceID
    cols(19) = "TXN"       // transCurrencyTypeCodeSourceID
    cols(20) = "USD"       // transCurrencyCodeTargetID
    cols(21) = "BASE-LE"   // transCurrencyTypeCodeTargetID
    cols(22) = fiscalYear  // transFiscalPeriod
    cols(23) = acctDate   // transAcctDate
    cols(24) = "2014/03/01" // transTransDate
    cols(25) = amount      // transTransAmount
    cols(26) = " "
    cols(27) = "0"
    cols(28) = "0"
    cols(29) = " "
    cols(30) = " "
    cols(31) = dirFlag     // transDirVsOffsetFlg
    cols(32) = "N"
    cols(33) = "N"
    cols(34) = " "
    cols(35) = " "
    cols(36) = " "
    cols(37) = " "
    cols(38) = " "
    cols.mkString(",")
  }

  // A balanced pair as standardizeAndSort writes it: expense line ("D"), then offset line at 0000 ("O").
  private def pair(vendor: String, agency: String, fund: String, nominal: String, amount: String,
                   objectCode: String = "OBJ001", acctDate: String = "2014/03/01",
                   jrnlId: String = "ID001"): Seq[String] = {
    val neg = if (amount.startsWith("-")) amount.drop(1) else "-" + amount
    Seq(
      makeJERow(vendor, agency, fund, nominal, "2014", amount, "D", objectCode, acctDate, jrnlId),
      makeJERow(vendor, agency, "0000", "0000", "2014", neg, "O", "0000", acctDate, jrnlId))
  }

  private def readCsv(file: File): (Vector[String], Vector[Map[String, String]]) = {
    val src   = Source.fromFile(file, "UTF-8")
    val lines = try src.getLines().toVector finally src.close()
    val hdr   = lines.head.split(",", -1).toVector
    (hdr, lines.drop(1).map(l => hdr.zip(l.split(",", -1).toVector).toMap))
  }

  private def writeSortedJE(dir: Path, year: String, rows: Seq[String]): File = {
    val shortYr = year.takeRight(2)
    val jeFile = dir.resolve(s"SortedJEFY${shortYr}q1exp.csv").toFile
    val lines  = rows.mkString("\n")
    java.nio.file.Files.write(jeFile.toPath, lines.getBytes("UTF-8"))
    jeFile
  }

  // ── GLPost tests ─────────────────────────────────────────────────────────────

  test("GLPost key is agency, fund, object, nominal, period, dir flag — offset lines are kept") {
    withTempDirectory("glpost-account-keys-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows =
        pair("V001", "AGENCY_A", "FUND01", "EXP001", "100.00") ++
        pair("V002", "AGENCY_A", "FUND01", "EXP001", "200.00") ++
        pair("V001", "AGENCY_B", "FUND01", "EXP002", "50.00")
      writeSortedJE(inDir, "2014", rows)

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val (hdr, data) = readCsv(outDir.resolve("GL2014.csv").toFile)
      assert(hdr == Vector("agency", "nominal_account_id", "fund", "object_id", "fiscal_year", "dir_flag", "total_amount"))
      // Two expense keys plus one offset key per agency.
      assert(data.length == 4, s"Expected 4 GL rows, got ${data.length}: $data")
      assert(data.count(_("nominal_account_id") == "0000") == 2, "Offset lines must be posted at nominal 0000")
      assert(data.filter(_("nominal_account_id") == "0000").forall(_("dir_flag") == "O"), "Offset rows carry flag O")
      assert(data.filter(_("nominal_account_id") != "0000").forall(_("dir_flag") == "D"), "Debit rows carry flag D")
    }
  }

  test("GLPost dir flag is part of the key — same account with D and O stays on two rows") {
    withTempDirectory("glpost-dirflag-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      writeSortedJE(inDir, "2014", Seq(
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "100.00", "D"),
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "-40.00", "O")))

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val (_, data) = readCsv(outDir.resolve("GL2014.csv").toFile)
      assert(data.length == 2, s"D and O on one account must be two GL rows: $data")
      assert(data.map(r => r("dir_flag") -> BigDecimal(r("total_amount"))).toMap ==
        Map("D" -> BigDecimal("100.00"), "O" -> BigDecimal("-40.00")))
    }
  }

  test("GLPost object is part of the key") {
    withTempDirectory("glpost-object-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows =
        pair("V001", "AGY", "FND", "EXP001", "10.00", objectCode = "OBJ1") ++
        pair("V001", "AGY", "FND", "EXP001", "20.00", objectCode = "OBJ2")
      writeSortedJE(inDir, "2014", rows)

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val (_, data) = readCsv(outDir.resolve("GL2014.csv").toFile)
      val exp = data.filter(_("nominal_account_id") == "EXP001")
      assert(exp.map(_("object_id")).toSet == Set("OBJ1", "OBJ2"), s"Two objects must give two rows: $exp")
    }
  }

  test("GLPost accumulates amounts for the same key; offsets net the GL to zero") {
    withTempDirectory("glpost-amounts-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows =
        pair("V001", "AGY", "FND", "EXP001", "100.00") ++
        pair("V002", "AGY", "FND", "EXP001", "200.00")
      writeSortedJE(inDir, "2014", rows)

      val baos = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(baos, true, "UTF-8")) {
        GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      }

      val (_, data) = readCsv(outDir.resolve("GL2014.csv").toFile)
      val exp = data.find(_("nominal_account_id") == "EXP001").get
      val off = data.find(_("nominal_account_id") == "0000").get
      assert(BigDecimal(exp("total_amount")) == BigDecimal("300.00"))
      assert(BigDecimal(off("total_amount")) == BigDecimal("-300.00"))
      assert(baos.toString("UTF-8").contains("net_amount=0"), s"net_amount must be 0:\n${baos.toString("UTF-8")}")
    }
  }

  test("GLPost discards instrument identity — different vendors collapse to same account row") {
    withTempDirectory("glpost-no-vendor-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      // Three different vendors, same account key → should collapse to ONE GL balance row
      val rows = Seq(
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "10.00", "D"),
        makeJERow("V002", "AGY", "FND", "EXP001", "2014", "20.00", "D"),
        makeJERow("V003", "AGY", "FND", "EXP001", "2014", "30.00", "D")
      )
      writeSortedJE(inDir, "2014", rows)

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val (_, data) = readCsv(outDir.resolve("GL2014.csv").toFile)
      assert(data.length == 1, s"Three vendors, same account — expected 1 GL row, got ${data.length}")
      assert(BigDecimal(data.head("total_amount")) == BigDecimal("60.00"))
    }
  }

  test("GLPost one-read rule: merged file plus its 2m inputs is read once (control total is not doubled)") {
    withTempDirectory("glpost-oneread-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows =
        pair("V001", "AGY", "FND", "EXP001", "100.00") ++
        pair("V002", "AGY", "FND", "EXP001", "200.00")
      // Step 2m inputs and their merged output hold the same rows.
      writeSortedJE(inDir, "2014", rows)                                   // SortedJEFY14q1exp.csv
      java.nio.file.Files.write(inDir.resolve("SortedJEFY14merged.csv"), rows.mkString("\n").getBytes("UTF-8"))

      val baos = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(baos, true, "UTF-8")) {
        GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      }

      val (_, data) = readCsv(outDir.resolve("GL2014.csv").toFile)
      val total = BigDecimal(data.find(_("nominal_account_id") == "EXP001").get("total_amount"))
      assert(total == BigDecimal("300.00"), s"Expected 300.00 (read once), got $total (600.00 would mean a double read)")
      assert(baos.toString("UTF-8").contains("records_read=4 "), s"Expected records_read=4; got:\n${baos.toString("UTF-8")}")
    }
  }

  test("GLPost emits RESULT and RESOURCE log lines") {
    withTempDirectory("glpost-metrics-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      writeSortedJE(inDir, "2014", pair("V001", "AGY", "FND", "EXP001", "100.00"))

      val baos = new java.io.ByteArrayOutputStream()
      val ps   = new java.io.PrintStream(baos, true, "UTF-8")
      Console.withOut(ps) {
        GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      }
      val output = baos.toString("UTF-8")

      assert(output.contains("RESULT step=20 year=2014"),   "Should emit RESULT log line")
      assert(output.contains("RESOURCE step=20 year=2014"), "Should emit RESOURCE log line")
      assert(output.contains("records_read="),              "RESULT should contain records_read")
      assert(output.contains("records_written="),           "RESULT should contain records_written")
      assert(output.contains("net_amount="),                "RESULT should contain net_amount")
    }
  }

  // ── PermutationGenerator tests ────────────────────────────────────────────────

  private def runPerm(rows: Seq[String], prefix: String)
      : (Vector[String], Vector[Map[String, String]], String) = {
    var result: (Vector[String], Vector[Map[String, String]], String) = null
    withTempDirectory(prefix) { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))
      writeSortedJE(inDir, "2014", rows)
      val baos = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(baos, true, "UTF-8")) {
        PermutationGenerator(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      }
      val (hdr, data) = readCsv(outDir.resolve("PERM_TBL2014.csv").toFile)
      result = (hdr, data, baos.toString("UTF-8"))
    }
    result
  }

  test("PermutationGenerator writes one row per GL key, same keys as GLPost") {
    withTempDirectory("permgen-keys-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))
      val rows =
        pair("V001", "AGY", "FND", "EXP001", "10.00") ++
        pair("V002", "AGY", "FND", "EXP001", "20.00") ++
        pair("V003", "AGY", "FND", "EXP002", "30.00", objectCode = "OBJ2")
      writeSortedJE(inDir, "2014", rows)

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      PermutationGenerator(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val keyCols = Seq("agency", "nominal_account_id", "fund", "object_id", "fiscal_year", "dir_flag")
      val (_, gl)   = readCsv(outDir.resolve("GL2014.csv").toFile)
      val (_, perm) = readCsv(outDir.resolve("PERM_TBL2014.csv").toFile)
      assert(perm.map(r => keyCols.map(r)).toSet == gl.map(r => keyCols.map(r)).toSet)
      assert(perm.length == gl.length)
    }
  }

  test("PermutationGenerator capacity is the product of distinct values; constants contribute 1") {
    // One GL key: 3 vendors on 2 dates.  Journal IDs differ and amounts differ (both excluded).
    val rows = Seq(
      makeJERow("V001", "AGY", "FND", "EXP001", "2014", "10.00", "D", acctDate = "2014/03/01", jrnlId = "ID1"),
      makeJERow("V002", "AGY", "FND", "EXP001", "2014", "20.00", "D", acctDate = "2014/03/01", jrnlId = "ID2"),
      makeJERow("V003", "AGY", "FND", "EXP001", "2014", "30.00", "D", acctDate = "2014/06/01", jrnlId = "ID3"),
      makeJERow("V001", "AGY", "FND", "EXP001", "2014", "40.00", "D", acctDate = "2014/06/01", jrnlId = "ID4")
    )
    val (hdr, data, log) = runPerm(rows, "permgen-capacity-")

    assert(data.length == 1)
    val r = data.head
    assert(r("row_count") == "4")
    assert(r("d_transIPID") == "3")
    assert(r("d_transAcctDate") == "2")
    assert(r("capacity") == "6", s"3 vendors x 2 dates = 6, got ${r("capacity")}")
    assert(!hdr.contains("d_transJrnlID"),     "journal ID is event identity, not a dimension")
    assert(!hdr.contains("d_transTransAmount"), "amount is a measure, not a dimension")
    assert(!hdr.contains("d_transLedgerID"),   "constant columns are not listed")
    assert(log.contains("capacity=6 "), s"RESULT must carry capacity:\n$log")
    assert(log.contains("vendor_rows=3 "), s"RESULT must carry vendor_rows:\n$log")
  }

  test("PermutationGenerator sums capacity over GL keys") {
    val rows =
      Seq(makeJERow("V001", "AGY", "FND", "EXP001", "2014", "10.00", "D"),
          makeJERow("V002", "AGY", "FND", "EXP001", "2014", "20.00", "D")) ++   // key 1: 2 vendors
      Seq(makeJERow("V009", "AGY", "FND", "EXP002", "2014", "5.00", "D"))        // key 2: 1 vendor
    val (_, data, log) = runPerm(rows, "permgen-sum-")
    assert(data.map(_("capacity").toInt).sorted == Vector(1, 2))
    assert(log.contains("capacity=3 "), log)
    assert(log.contains("records_written=2 "), log)
  }

  // Overwrite one column of a 39-field row.
  private def withCol(row: String, idx: Int, value: String): String = {
    val cols = row.split(",", -1); cols(idx) = value; cols.mkString(",")
  }

  test("PermutationGenerator drops a dimension that another dimension determines, and reports CAR dims") {
    // One GL key.  Business event (col 5) is fixed per vendor, so vendor -> event: event is CAR and adds
    // nothing to the product.  Dates vary within each vendor, so date is independent of vendor.
    def row(v: String, evt: String, d: String, j: String) =
      withCol(makeJERow(v, "AGY", "FND", "EXP001", "2014", "10.00", "D", acctDate = d, jrnlId = j), 5, evt)
    val rows = Seq(
      row("V001", "EVT_A", "2014/03/01", "ID1"), row("V001", "EVT_A", "2014/06/01", "ID2"),
      row("V002", "EVT_B", "2014/03/01", "ID3"), row("V002", "EVT_B", "2014/06/01", "ID4"))
    val (_, data, log) = runPerm(rows, "permgen-fd-")

    assert(data.head("capacity") == "4", s"2 vendors x 2 dates = 4 (event dropped), got ${data.head("capacity")}")
    assert(log.contains("capacity=4 "), log)
    assert(log.contains("capacity_independent=8 "), s"independent bound is 2x2x2=8:\n$log")
    assert(log.contains("transBusinessEventCode<-transIPID"), s"event must be reported as dropped:\n$log")
    val carDims = "car_dims=([^ ]*)".r.findFirstMatchIn(log).get.group(1).split(";").toSet
    assert(carDims.contains("transBusinessEventCode"), s"event is vendor-determined, so CAR: $carDims")
    assert(carDims.contains("transContractID"), s"contract is CAR by definition: $carDims")
    assert(!carDims.contains("transAcctDate"), s"date varies within a vendor, so it is not CAR: $carDims")
  }

  test("PermutationSummary sums per-year summaries as BigInt") {
    withTempDirectory("permgen-21s-") { tmpDir =>
      val big = "99999999999999999999"   // > Long.MaxValue
      for (yy <- Seq("03", "04")) {
        java.nio.file.Files.write(tmpDir.resolve(s"PERM_SUMMARY20$yy.csv"),
          s"fiscal_year,gl_keys,vendor_rows,capacity,capacity_independent\n20$yy,5,7,$big,$big\n".getBytes("UTF-8"))
      }
      val baos = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(baos, true, "UTF-8")) {
        PermutationSummary(Map("outPath" -> tmpDir.toString, "years" -> "03,04,05"))
      }
      val log = baos.toString("UTF-8")
      assert(log.contains("gl_keys=10 vendor_rows=14 capacity=199999999999999999998 "), log)
      assert(log.contains("years_missing=05"), log)
    }
  }

  test("PermutationGenerator emits RESULT and RESOURCE log lines") {
    val (_, _, log) = runPerm(pair("V001", "AGY", "FND", "EXP001", "100.00"), "permgen-metrics-")
    assert(log.contains("RESULT step=21 year=2014"),   "Should emit RESULT log line")
    assert(log.contains("RESOURCE step=21 year=2014"), "Should emit RESOURCE log line")
    assert(log.contains("records_read=2 "), log)
  }
}
