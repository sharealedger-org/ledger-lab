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
   *  31  transDirVsOffsetFlg    — "D" (debit) or "N" (credit offset)
   */
  private def makeJERow(
    vendorId:   String,
    agency:     String,
    fund:       String,
    nomAcct:    String,
    fiscalYear: String,
    amount:     String,
    dirFlag:    String
  ): String = {
    val cols = Array.fill(39)("0")
    cols(0)  = vendorId    // transIPID
    cols(1)  = "0"         // transContractID
    cols(2)  = "0"         // transCommitmentID
    cols(3)  = "ID001"     // transJrnlID
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
    cols(14) = "OBJ001"    // transProjectID
    cols(15) = "0"         // transProductID
    cols(16) = nomAcct     // transNominalAccountID
    cols(17) = "0"         // transAltAccountID
    cols(18) = "USD"       // transCurrencyCodeSourceID
    cols(19) = "TXN"       // transCurrencyTypeCodeSourceID
    cols(20) = "USD"       // transCurrencyCodeTargetID
    cols(21) = "BASE-LE"   // transCurrencyTypeCodeTargetID
    cols(22) = fiscalYear  // transFiscalPeriod
    cols(23) = "2014/03/01" // transAcctDate
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

  private def writeSortedJE(dir: Path, year: String, rows: Seq[String]): File = {
    val shortYr = year.takeRight(2)
    val jeFile = dir.resolve(s"SortedJEFY${shortYr}q1exp.csv").toFile
    val lines  = rows.mkString("\n")
    java.nio.file.Files.write(jeFile.toPath, lines.getBytes("UTF-8"))
    jeFile
  }

  // ── GLPost tests ─────────────────────────────────────────────────────────────

  test("GLPost writes one balance row per distinct account key") {
    withTempDirectory("glpost-account-keys-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      // Two transactions to the same account, one to a different account
      val rows = Seq(
        makeJERow("V001", "AGENCY_A", "FUND01", "EXP001", "2014", "100.00", "D"),
        makeJERow("V001", "AGENCY_A", "FUND01", "EXP001", "2014", "-100.00", "N"),  // credit pair — excluded
        makeJERow("V002", "AGENCY_A", "FUND01", "EXP001", "2014", "200.00", "D"),
        makeJERow("V002", "AGENCY_A", "FUND01", "EXP001", "2014", "-200.00", "N"),
        makeJERow("V001", "AGENCY_B", "FUND01", "EXP002", "2014", "50.00", "D"),
        makeJERow("V001", "AGENCY_B", "FUND01", "EXP002", "2014", "-50.00", "N")
      )
      writeSortedJE(inDir, "2014", rows)

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val glFile = outDir.resolve("GL2014.csv").toFile
      assert(glFile.exists(), "GL2014.csv should be created")

      val src = Source.fromFile(glFile, "UTF-8")
      val lines = try src.getLines().toVector finally src.close()
      val dataRows = lines.drop(1)  // skip header

      // Two distinct account keys: (AGENCY_A, EXP001, FUND01) and (AGENCY_B, EXP002, FUND01)
      assert(dataRows.length == 2, s"Expected 2 GL balance rows, got ${dataRows.length}: $dataRows")
    }
  }

  test("GLPost accumulates amounts correctly for the same account key") {
    withTempDirectory("glpost-amounts-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      // Two debit rows for the same account: 100.00 + 200.00 = 300.00
      val rows = Seq(
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "100.00", "D"),
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "-100.00", "N"),
        makeJERow("V002", "AGY", "FND", "EXP001", "2014", "200.00", "D"),
        makeJERow("V002", "AGY", "FND", "EXP001", "2014", "-200.00", "N")
      )
      writeSortedJE(inDir, "2014", rows)

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val glFile = outDir.resolve("GL2014.csv").toFile
      val src    = Source.fromFile(glFile, "UTF-8")
      val lines  = try src.getLines().toVector finally src.close()
      val dataRow = lines.drop(1).head
      val totalAmount = dataRow.split(",").last.trim.toDouble

      assert(Math.abs(totalAmount - 300.0) < 0.01, s"Expected 300.00, got $totalAmount")
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

      val glFile = outDir.resolve("GL2014.csv").toFile
      val src    = Source.fromFile(glFile, "UTF-8")
      val lines  = try src.getLines().toVector finally src.close()
      val dataRows = lines.drop(1)

      assert(dataRows.length == 1, s"Three vendors, same account — expected 1 GL row, got ${dataRows.length}")
      val totalAmount = dataRows.head.split(",").last.trim.toDouble
      assert(Math.abs(totalAmount - 60.0) < 0.01, s"Expected 60.00, got $totalAmount")
    }
  }

  test("GLPost one-read rule: merged file plus its 2m inputs is read once (control total is not doubled)") {
    withTempDirectory("glpost-oneread-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows = Seq(
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "100.00", "D"),
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "-100.00", "N"),
        makeJERow("V002", "AGY", "FND", "EXP001", "2014", "200.00", "D"),
        makeJERow("V002", "AGY", "FND", "EXP001", "2014", "-200.00", "N")
      )
      // Step 2m inputs and their merged output hold the same rows.
      writeSortedJE(inDir, "2014", rows)                                   // SortedJEFY14q1exp.csv
      java.nio.file.Files.write(inDir.resolve("SortedJEFY14merged.csv"), rows.mkString("\n").getBytes("UTF-8"))

      val baos = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(baos, true, "UTF-8")) {
        GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      }

      val src   = Source.fromFile(outDir.resolve("GL2014.csv").toFile, "UTF-8")
      val lines = try src.getLines().toVector finally src.close()
      val total = lines.drop(1).head.split(",").last.trim.toDouble
      assert(Math.abs(total - 300.0) < 0.01, s"Expected 300.00 (read once), got $total (600.00 would mean a double read)")
      assert(baos.toString("UTF-8").contains("records_read=4 "), s"Expected records_read=4; got:\n${baos.toString("UTF-8")}")
    }
  }

  test("GLPost emits RESULT and RESOURCE log lines") {
    withTempDirectory("glpost-metrics-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows = Seq(
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "100.00", "D"),
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "-100.00", "N")
      )
      writeSortedJE(inDir, "2014", rows)

      // Capture stdout to verify RESULT/RESOURCE lines are emitted
      // Scala's println uses Console.out, so we use Console.withOut to intercept it
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
    }
  }

  // ── PermutationGenerator tests ────────────────────────────────────────────────

  test("PermutationGenerator produces more rows than GLPost for same input") {
    withTempDirectory("permgen-more-rows-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      // Three vendors posting to the same account → GL = 1 row, PERM = 3 rows
      val rows = Seq(
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "10.00", "D"),
        makeJERow("V002", "AGY", "FND", "EXP001", "2014", "20.00", "D"),
        makeJERow("V003", "AGY", "FND", "EXP001", "2014", "30.00", "D")
      )
      writeSortedJE(inDir, "2014", rows)

      GLPost(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      PermutationGenerator(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val glSrc   = Source.fromFile(outDir.resolve("GL2014.csv").toFile, "UTF-8")
      val glRows  = try glSrc.getLines().toVector.drop(1) finally glSrc.close()

      val permSrc  = Source.fromFile(outDir.resolve("PERM_TBL2014.csv").toFile, "UTF-8")
      val permRows = try permSrc.getLines().toVector.drop(1) finally permSrc.close()

      assert(glRows.length  == 1, s"GL should have 1 row, got ${glRows.length}")
      assert(permRows.length == 3, s"Perm table should have 3 rows (one per vendor), got ${permRows.length}")
      assert(permRows.length > glRows.length, "Permutation table must have more rows than GL when multiple vendors share an account")
    }
  }

  test("PermutationGenerator includes vendor_id in each row") {
    withTempDirectory("permgen-vendor-id-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows = Seq(
        makeJERow("VENDOR001", "AGY", "FND", "EXP001", "2014", "50.00", "D"),
        makeJERow("VENDOR002", "AGY", "FND", "EXP001", "2014", "75.00", "D")
      )
      writeSortedJE(inDir, "2014", rows)

      PermutationGenerator(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))

      val permFile = outDir.resolve("PERM_TBL2014.csv").toFile
      assert(permFile.exists(), "PERM_TBL2014.csv should be created")

      val src   = Source.fromFile(permFile, "UTF-8")
      val lines = try src.getLines().toVector finally src.close()

      // Header should have vendor_id column
      assert(lines.head.contains("vendor_id"), "Header should contain vendor_id column")
      // Data rows should contain vendor IDs
      val dataRows = lines.drop(1)
      assert(dataRows.exists(_.contains("VENDOR001")), "Row for VENDOR001 should be present")
      assert(dataRows.exists(_.contains("VENDOR002")), "Row for VENDOR002 should be present")
    }
  }

  test("PermutationGenerator emits RESULT and RESOURCE log lines") {
    withTempDirectory("permgen-metrics-") { tmpDir =>
      val inDir  = Files.createDirectory(tmpDir.resolve("input"))
      val outDir = Files.createDirectory(tmpDir.resolve("output"))

      val rows = Seq(
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "100.00", "D"),
        makeJERow("V001", "AGY", "FND", "EXP001", "2014", "-100.00", "N")
      )
      writeSortedJE(inDir, "2014", rows)

      val baos = new java.io.ByteArrayOutputStream()
      val ps   = new java.io.PrintStream(baos, true, "UTF-8")
      Console.withOut(ps) {
        PermutationGenerator(Map("inPath" -> inDir.toString, "outPath" -> outDir.toString, "year" -> "2014"))
      }
      val output = baos.toString("UTF-8")

      assert(output.contains("RESULT step=21 year=2014"),   "Should emit RESULT log line")
      assert(output.contains("RESOURCE step=21 year=2014"), "Should emit RESOURCE log line")
    }
  }
}
