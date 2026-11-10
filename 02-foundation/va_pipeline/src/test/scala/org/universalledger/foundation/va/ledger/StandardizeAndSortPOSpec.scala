package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.file.{Files, Path, Paths}

import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source

// ── Tests for Step 2p (PO standardize + sort) and Step 2m (payment + PO merge) ──────────────────
//
//  Architecture contract being tested:
//    - applyPO reads a tab-delimited PO file, emits 2 × 39-field SJE rows per source row,
//      streams them through SortEngine.externalSort (O(chunkSize) memory), and writes
//      SortedJEFY{yr}q{n}PO.csv sorted by the SortedJE.sortspec posting key.
//    - runMerge (step 2m) calls SortEngine.mergeSpills on the payment and PO sorted files
//      and writes SortedJEFY{yr}merged.csv in the same sort order.
//    - The merged file contains both EXPENSE_PAYMENT and COMMITMENT rows interleaved in
//      instrument order — the declared input for post.scala.
//
//  Memory contract: no ArrayBuffer of all rows; both steps are O(chunkSize) / O(k readers).

class StandardizeAndSortPOSpec extends AnyFunSuite {

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

  // ── PO fixture: two rows from VA_opendata_FY2003_small.txt ──────────────────
  private val poHeader =
    "PONUMBER\tAGENCY\tORDEREDDATE\tVENDORCOMMODITYDESC\tQUANTITYORDERED\tPRICE\tVENDORID\tVENDORNAME\tVENDORADDRESS\tVENDORCITY\tVENDORSTATE\tVENDORPOSTALCODE\tVENDORLOC_EMAILADDRESS\tNIGPCODE\tNIGPDESCRIPTION\tUOMCode\tUOMDesc\tVENDORPARTNUMBER\tMANPARTNUMBER"

  // Row 1: vendor c9545, PO A501_0000200268, qty=1, price=33.45 → amount=33.45
  // Row 2: vendor e1,    PO DO36252,         qty=2, price=12.40 → amount=24.80
  private val poRow1 =
    "A501_0000200268\tVirginia Department of Transportation\t09/23/2002\tPEN AND PENCIL\t1\t33.45\tc9545\tJ. JENKINS\tADDR\tCITY\tMD\t00000\temail\t08015\tBadges\tEA\teach\t\t"
  private val poRow2 =
    "DO36252\tCentral Virginia Training Center\t09/23/2002\tSUGAR SUBSTITUTES\t2\t12.40\te1\tVirginia DC\tADDR\tCITY\tVA\t00000\temail\t39382\tSyrups\tCS\tcase\t911297\t"

  private def writePOFile(dir: Path, rows: String*): Path = {
    val poFile = dir.resolve("VA_opendata_FY2003.txt")
    val content = (poHeader +: rows).mkString("\n") + "\n"
    Files.write(poFile, content.getBytes("UTF-8"))
    poFile
  }

  // ── Test 1: applyPO produces 2 × source rows, each with 39 columns ──────────
  test("Step 2p: applyPO produces 2 SJE rows per PO source row, each 39 columns") {
    withTempDirectory("po-2p-cols-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writePOFile(inDir, poRow1, poRow2)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val outFile = outDir.resolve("SortedJEFY03q1PO.csv")
      assert(outFile.toFile.exists(), "Output file SortedJEFY03q1PO.csv should be created")

      val src  = Source.fromFile(outFile.toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      assert(rows.length == 4, s"Expected 4 rows (2 source × 2 SJE lines), got ${rows.length}")
      assert(rows.forall(_.split(",", -1).length == 39),
        s"Every row must have 39 columns; got ${rows.map(_.split(",", -1).length).distinct}")
    }
  }

  // ── Test 2: debit + credit amounts cancel per PO journal group ───────────────
  test("Step 2p: debit and credit amounts cancel within each PO journal group") {
    withTempDirectory("po-2p-balance-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writePOFile(inDir, poRow1)  // single PO row; amount = 33.45

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      assert(rows.length == 2)

      val amounts = rows.map(r => BigDecimal(r.split(",", -1)(25)))
      assert(amounts.sum == BigDecimal(0), s"Debit + credit should sum to 0; got ${amounts.sum}")
    }
  }

  // ── Test 3: transIPID (col 0) is populated from VENDORID (col 6), not "0" ──
  test("Step 2p: transIPID is populated from VENDORID, not hardcoded '0'") {
    withTempDirectory("po-2p-ipid-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writePOFile(inDir, poRow1, poRow2)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      val ipids = rows.map(_.split(",", -1)(0))
      assert(ipids.forall(_ != "0"), s"transIPID must not be '0'; found: ${ipids.distinct}")
      assert(ipids.exists(_ == "c9545"), "transIPID 'c9545' from poRow1 should appear")
      assert(ipids.exists(_ == "e1"),    "transIPID 'e1' from poRow2 should appear")
    }
  }

  // ── Test 4: transCommitmentID (col 2) is set to PONUMBER ─────────────────────
  test("Step 2p: transCommitmentID (col 2) is set to PONUMBER") {
    withTempDirectory("po-2p-commit-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writePOFile(inDir, poRow1)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      val commitIds = rows.map(_.split(",", -1)(2))
      assert(commitIds.forall(_ == "A501_0000200268"),
        s"transCommitmentID must be PONUMBER; got ${commitIds.distinct}")
    }
  }

  // ── Test 5: RESULT and RESOURCE log lines are emitted ─────────────────────
  test("Step 2p: RESULT and RESOURCE metric log lines are emitted to stdout") {
    withTempDirectory("po-2p-metrics-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writePOFile(inDir, poRow1)

      val output = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(output)) {
        standardizeAndSort.applyPO(Map(
          "inPath"  -> inDir.toString,
          "outPath" -> outDir.toString,
          "year"    -> "2003",
          "quarter" -> "1"
        ))
      }
      val log = output.toString("UTF-8")
      assert(log.contains("RESULT step=2p"), s"Expected 'RESULT step=2p' in output; got:\n$log")
      assert(log.contains("RESOURCE step=2p"), s"Expected 'RESOURCE step=2p' in output; got:\n$log")
    }
  }

  // ── Test 6: address-fragment rows are quarantined — no SJE emitted ──────────
  //  A malformed PO row whose PONUMBER field contains a state+ZIP fragment
  //  (e.g. " CT 06511") caused by a multi-line vendor address must be dropped
  //  at ingestion.  The output file must be empty; sourceRows = 0.
  test("Step 2p: address-fragment PONUMBER is quarantined, produces 0 SJE rows") {
    withTempDirectory("po-2p-addrfrag-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      // Simulate a column-shifted row where state+ZIP lands in PONUMBER position.
      // After RFC-4180 reassembly the PONUMBER tab-field contains " CT 06511".
      val malformedRow =
        " CT 06511\tVirginia Department of General Services\t10/08/2002\t" +
        "Internal Standard\t1\t0\tBAD_VENDOR\t" +
        "AccuStandard\t25 Science Park\tNew Haven\tCT\t06511\t\t39382\t\tEA\teach\t\t"
      writePOFile(inDir, malformedRow)

      val errOut = new java.io.ByteArrayOutputStream()
      Console.withErr(new java.io.PrintStream(errOut)) {
        standardizeAndSort.applyPO(Map(
          "inPath"  -> inDir.toString,
          "outPath" -> outDir.toString,
          "year"    -> "2003",
          "quarter" -> "1"
        ))
      }

      val outFile = outDir.resolve("SortedJEFY03q1PO.csv")
      val src  = scala.io.Source.fromFile(outFile.toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      assert(rows.isEmpty, s"Expected 0 SJE rows for quarantined address-fragment row; got ${rows.length}")
      val errMsg = errOut.toString("UTF-8")
      assert(errMsg.contains("QUARANTINE"), s"Expected QUARANTINE log in stderr; got: $errMsg")
    }
  }

  // ── Test 7: address-fragment row is skipped but sibling rows still emit ──────
  test("Step 2p: address-fragment row is skipped; adjacent well-formed rows still emit") {
    withTempDirectory("po-2p-addrfrag-mixed-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      val malformedRow =
        " PA 16823\tVirginia Department of General Services\t10/08/2002\t" +
        "Capillary Ferrules\t1\t0\tBAD_VENDOR\t" +
        "Restek\t110 Benner Circle\tBellefonte\tPA\t16823\t\t08015\t\tEA\teach\t\t"
      writePOFile(inDir, poRow1, malformedRow, poRow2)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = scala.io.Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      // poRow1 + poRow2 produce 4 SJE rows; malformedRow is quarantined → 4 rows total
      assert(rows.length == 4, s"Expected 4 rows (2 well-formed × 2 SJE); got ${rows.length}")
      // Amounts must still sum to 0
      val amounts = rows.map(r => BigDecimal(r.split(",", -1)(25)))
      assert(amounts.sum == BigDecimal(0), s"Amounts should sum to 0; got ${amounts.sum}")
    }
  }

  // ── Test 8: non-numeric price field (column-shift variant) is quarantined ────
  //  Rows where the PONUMBER looks like a valid PO number but cols(5) (PRICE)
  //  contains non-numeric content (e.g. "USD", "03") are column-shifted artifacts
  //  from RFC-4180 multi-line address absorption.  They must be quarantined at
  //  ingestion — no SJE rows emitted.
  test("Step 2p: non-numeric price field is quarantined, produces 0 SJE rows") {
    withTempDirectory("po-2p-badprice-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      // Simulate the column-shift that leaves a non-numeric value in the PRICE
      // column: poNumber is valid, but price contains "USD" (column-shifted text).
      val badPriceRow =
        "DO38861\tVirginia Department of General Services\t10/08/2002\t" +
        "Analyte Standard\t1\tUSD\tBAD_VENDOR\t" +
        "AccuStandard\t25 Science Park\tNew Haven\tCT\t06511\t\t08015\t\tEA\teach\t\t"
      writePOFile(inDir, badPriceRow)

      val errOut = new java.io.ByteArrayOutputStream()
      Console.withErr(new java.io.PrintStream(errOut)) {
        standardizeAndSort.applyPO(Map(
          "inPath"  -> inDir.toString,
          "outPath" -> outDir.toString,
          "year"    -> "2003",
          "quarter" -> "1"
        ))
      }

      val outFile = outDir.resolve("SortedJEFY03q1PO.csv")
      val src  = scala.io.Source.fromFile(outFile.toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      assert(rows.isEmpty, s"Expected 0 SJE rows for quarantined bad-price row; got ${rows.length}")
      val errMsg = errOut.toString("UTF-8")
      assert(errMsg.contains("QUARANTINE"), s"Expected QUARANTINE log in stderr; got: $errMsg")
    }
  }

  // ── Test 9: bad-price row skipped; adjacent well-formed rows still emit ───────
  test("Step 2p: non-numeric price row is skipped; adjacent well-formed rows still emit") {
    withTempDirectory("po-2p-badprice-mixed-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      val badPriceRow =
        "DO38798\tVirginia Department of General Services\t10/08/2002\t" +
        "Capillary Ferrules\t1\tUSD\tBAD_VENDOR\t" +
        "Restek\t110 Benner Circle\tBellefonte\tPA\t16823\t\t08015\t\tEA\teach\t\t"
      writePOFile(inDir, poRow1, badPriceRow, poRow2)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = scala.io.Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      // poRow1 + poRow2 produce 4 SJE rows; badPriceRow is quarantined → 4 rows total
      assert(rows.length == 4, s"Expected 4 rows (2 well-formed × 2 SJE); got ${rows.length}")
      val amounts = rows.map(r => BigDecimal(r.split(",", -1)(25)))
      assert(amounts.sum == BigDecimal(0), s"Amounts should sum to 0; got ${amounts.sum}")
    }
  }

  // ── Test 9a: implausible VENDORID (column shift) is quarantined ──────────────
  //  Observed in FY2003: a price ($158.79), a decimal (.01) or description text landed in
  //  VENDORID from a multi-line record that passed the PONUMBER and PRICE guards. VENDORID is the
  //  leading CKB sort key, so these must not become vendor groups.
  test("Step 2p: implausible VENDORID is quarantined; well-formed neighbours (digits, letters+digits) still emit") {
    withTempDirectory("po-2p-badvendor-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      def row(po: String, vendor: String) =
        s"$po\tAgency\t09/23/2002\tPEN\t1\t10.00\t$vendor\tName\tADDR\tCITY\tVA\t00000\temail\t08015\tdesc\tEA\teach\t\t"
      writePOFile(inDir,
        row("DO1", "c9545"),            // letters + digits  -> kept
        row("DO2", "1229771"),          // digits only       -> kept
        row("DO3", "$158.79"),          // dollar amount     -> quarantined
        row("DO4", ".01"),              // decimal           -> quarantined
        row("DO5", "& conditions of"))  // description text  -> quarantined

      val errOut = new java.io.ByteArrayOutputStream()
      val stdOut = new java.io.ByteArrayOutputStream()
      Console.withErr(new java.io.PrintStream(errOut)) {
        Console.withOut(new java.io.PrintStream(stdOut)) {
          standardizeAndSort.applyPO(Map(
            "inPath"  -> inDir.toString,
            "outPath" -> outDir.toString,
            "year"    -> "2003",
            "quarter" -> "1"
          ))
        }
      }

      val src  = scala.io.Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      assert(rows.length == 4, s"Expected 4 rows (2 kept × 2 SJE); got ${rows.length}")
      assert(rows.map(_.split(",", -1)(0)).distinct.sorted == Vector("1229771", "c9545"))
      assert(rows.map(r => BigDecimal(r.split(",", -1)(25))).sum == BigDecimal(0))
      assert(errOut.toString("UTF-8").split("\n").count(_.contains("QUARANTINE implausible VENDORID")) == 3)
      assert(stdOut.toString("UTF-8").contains("skipped_quarantine=3"),
        s"RESULT line should report skipped_quarantine=3; got:\n${stdOut.toString("UTF-8")}")
    }
  }

  // ── Test 9c: structural reader — inch-mark quotes, embedded tabs, empty-date fragments, bare CR ──────────
  //  Record boundaries come from the ORDEREDDATE anchor and the header width, not from quote parity.
  //  Every rejected record is written to POQuarantineFY{yy}.csv and the run reconciles
  //  physical lines and records (accepted + quarantined = records read).
  test("Step 2p: structural reader accepts inch-mark quotes, quarantines embedded-tab records with a reason code, and reconciles") {
    withTempDirectory("po-2p-reader-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      def q(s: String) = "\"" + s + "\""
      def rec(po: String, date: String, desc: String, vendor: String = "c1") =
        Seq(q(po), q("Agency"), q(date), desc, "1", "10.00", q(vendor), q("Name"), q("ADDR"), q("CITY"),
            q("VA"), q("00000"), q(""), q("08015"), q("d"), q("EA"), q("each"), q(""), q("")).mkString("\t")

      // A fragment before any record start (tail of an address, empty date, empty first field) is an orphan.
      val orphan      = "\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\"\t\"\""
      val inchMark    = rec("DO1", "09/23/2002", q("Tape, Black Gaffer's 2\"\""))   // ends in inch mark: accepted
      val multiLine   = rec("DO2", "09/24/2002", q("Line one\ncontinues here"))      // embedded newline: joined, accepted
      val embeddedTab = rec("DO3", "09/25/2002", q("Part#\tQty\tPrice"))             // 2 extra tabs: FIELD_COUNT
      val bareCr      = rec("DO4", "09/26/2002", q("before\rafter"))                 // CR must not split the SJE row
      writePOFile(inDir, orphan, inchMark, multiLine, embeddedTab, bareCr)

      val stdOut = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(stdOut)) {
        standardizeAndSort.applyPO(Map(
          "inPath"  -> inDir.toString,
          "outPath" -> outDir.toString,
          "year"    -> "2003",
          "quarter" -> "1"
        ))
      }

      val sje = Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try sje.getLines().filter(_.trim.nonEmpty).toVector finally sje.close()
      assert(rows.forall(_.split(",", -1).length == 39), "every SJE row must have 39 columns (bare CR must not split a row)")
      val poNumbers = rows.map(_.split(",", -1)(2)).distinct.sorted
      assert(poNumbers == Vector("DO1", "DO2", "DO4"), s"accepted POs; got $poNumbers")

      val qFile = Source.fromFile(outDir.resolve("POQuarantineFY03.csv").toFile, "UTF-8")
      val qLines = try qFile.getLines().toVector finally qFile.close()
      assert(qLines.head.startsWith("rel_path,record_ordinal,start_line"), s"quarantine header; got ${qLines.head}")
      val data = qLines.tail.filter(_.nonEmpty)
      assert(data.length == 2, s"expected 2 quarantined records (orphan fragment + embedded tab); got ${data.length}: $data")
      assert(data.exists(l => l.contains("DO3") && l.contains(",FIELD_COUNT,")), s"DO3 should be FIELD_COUNT; got $data")
      assert(data.exists(_.contains(",ORPHAN_FRAGMENT,")), s"leading fragment should be ORPHAN_FRAGMENT; got $data")

      val out = stdOut.toString("UTF-8")
      assert(out.contains("records_accepted=3"), s"RESULT line should report records_accepted=3; got:\n$out")
      assert(out.contains("skipped_quarantine=2"), s"RESULT line should report skipped_quarantine=2; got:\n$out")
      assert(out.contains("reader_rejects=2"), s"RESULT line should report reader_rejects=2; got:\n$out")
    }
  }


  // ── Test 9b: record boundary is the date field, not quote parity ─────────────
  //  A description with an unmatched `"` (e.g. `18" x 225 ft.`) used to leave the
  //  quote count odd and absorb every following record.  A description with an
  //  embedded newline must still be joined into one record.
  test("Step 2p: unmatched quote in description does not absorb following records; embedded newline is joined") {
    withTempDirectory("po-2p-boundary-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      val unmatchedQuote =
        "\"DO1\"\t\"Agency A\"\t\"09/23/2002\"\t\"Tape 18\" x 225 ft\"\t1\t10.00\t\"v1\"\t\"Vendor 1\"\t\"ADDR\"\t\"CITY\"\t\"VA\"\t\"00000\"\t\"\"\t\"08015\"\t\"d\"\t\"EA\"\t\"each\"\t\"\"\t\"\""
      val embeddedNewline =
        "\"DO2\"\t\"Agency B\"\t\"09/24/2002\"\t\"Line one\ncontinues here\"\t2\t5.00\t\"v2\"\t\"Vendor 2\"\t\"ADDR\"\t\"CITY\"\t\"VA\"\t\"00000\"\t\"\"\t\"08015\"\t\"d\"\t\"EA\"\t\"each\"\t\"\"\t\"\""
      val lastRow =
        "\"DO3\"\t\"Agency C\"\t\"09/25/2002\"\t\"Pen\"\t3\t1.00\t\"v3\"\t\"Vendor 3\"\t\"ADDR\"\t\"CITY\"\t\"VA\"\t\"00000\"\t\"\"\t\"08015\"\t\"d\"\t\"EA\"\t\"each\"\t\"\"\t\"\""
      writePOFile(inDir, unmatchedQuote, embeddedNewline, lastRow)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = scala.io.Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      assert(rows.length == 6, s"Expected 6 rows (3 records × 2 SJE); got ${rows.length}")
      val poNumbers = rows.map(_.split(",", -1)(2)).distinct.sorted
      assert(poNumbers == Vector("DO1", "DO2", "DO3"), s"Expected DO1,DO2,DO3; got $poNumbers")
    }
  }

  // ── Test 9d: multiline record with embedded tabs is quarantined under FIELD_COUNT ──
  //  In FY2010, continuation lines with embedded tabs previously fooled the reader into
  //  treating the second half as a separate record with empty date, causing column shifts
  //  and ledger imbalance. They must now be joined and quarantined cleanly under FIELD_COUNT.
  test("Step 2p: multiline record with extra embedded tabs is quarantined under FIELD_COUNT") {
    withTempDirectory("po-2p-multiline-tabs-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      val multilineWithExtraTabs =
        "\"EP572828\"\t\"Virginia Commonwealth University\"\t\"06/10/2010\"\t\"79215A669\t\t\t\n" +
        "Regulator Fitting Inlet Nipple, 1/4\" NPT Male, Cga #590, 3.5\" Length\t\t\t\"\t1\t6.16\t\"038599171\"\t\"MCMASTER-CARR SUPPLY CO\"\t\"200 New Canton Way\"\t\"Robbinsville\"\t\"NJ\"\t\"08691\"\t\"ben.schuchardt@mcmaster.com\"\t\"03194\"\t\"Unit Heaters, Steam and Hot Water Types\"\t\"EA\"\t\"each\"\t\"\"\t\"\""
      writePOFile(inDir, multilineWithExtraTabs)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = scala.io.Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      assert(rows.isEmpty, "Malformed multiline row with extra tabs must be quarantined, producing 0 SJE rows")

      val qFile = scala.io.Source.fromFile(outDir.resolve("POQuarantineFY03.csv").toFile, "UTF-8")
      val qLines = try qFile.getLines().toVector finally qFile.close()
      assert(qLines.tail.exists(_.contains(",FIELD_COUNT,")), "Must be quarantined with FIELD_COUNT")
    }
  }

  // ── Test 9e: commas in PONUMBER and descriptions are sanitized to spaces ─────
  test("Step 2p: commas in PONUMBER and descriptions are sanitized to prevent CSV corruption") {
    withTempDirectory("po-2p-commas-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      val poWithCommas =
        "\"PO,123,ABC\"\t\"Agency, Name\"\t\"09/23/2002\"\t\"Item, Description, With, Commas\"\t2\t15.50\t\"038599171\"\t\"Vendor\"\t\"ADDR\"\t\"CITY\"\t\"VA\"\t\"00000\"\t\"\"\t\"08015\"\t\"d\"\t\"EA\"\t\"each\"\t\"\"\t\"\""
      writePOFile(inDir, poWithCommas)

      standardizeAndSort.applyPO(Map(
        "inPath"  -> inDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "quarter" -> "1"
      ))

      val src  = scala.io.Source.fromFile(outDir.resolve("SortedJEFY03q1PO.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      assert(rows.length == 2, s"Expected 2 SJE rows; got ${rows.length}")
      assert(rows.forall(_.split(",", -1).length == 39), s"Every row must have exactly 39 columns; got ${rows.map(_.split(",", -1).length)}")
      assert(rows(0).split(",", -1)(2) == "PO 123 ABC", "Commas in PONUMBER must be sanitized to space")
      val amounts = rows.map(r => BigDecimal(r.split(",", -1)(25)))
      assert(amounts.sum == BigDecimal(0), s"Debit and credit must cancel; sum=${amounts.sum}")
    }
  }

  // ── Test 10: Step 2m merges payment + PO files in sort order ─────────────────
  test("Step 2m: mergeSpills produces a merged file with all rows in sort order") {
    withTempDirectory("po-2m-merge-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("in"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))

      // Build a minimal payment SortedJE file (vendor "z9999") and PO SortedJE (vendor "c9545")
      // After merge, "c9545" rows should precede "z9999" rows (ascending by transIPID col 0).
      val payRow1 = "z9999,0,0,IDpay1,1,EXPENSE_PAYMENT,Payment Sys,Unknown,Test,ACTUALS,FIN,SHRD-3RD-PARTY,AGY1,FUND1,OBJ1,0,EXPOBJ1,0,USD,TXN,USD,BASE-LE,2003,2003/03/01,2003/03/01,100.00, ,0,0, , ,D,N,N, , , , , "
      val payRow2 = "z9999,0,0,IDpay1,2,EXPENSE_PAYMENT,Payment Sys,Unknown,CASH_CLEARING,ACTUALS,FIN,SHRD-3RD-PARTY,AGY1,0000,0000,0,0000,0,USD,TXN,USD,BASE-LE,2003,2003/03/01,2003/03/01,-100.00, ,0,0, , ,D,N,N, , , , , "
      val poRow1s = "c9545,0,A501_0000200268,IDpo1PO,1,COMMITMENT,PO Sys,Unknown,PEN AND PENCIL,ACTUALS,FIN,SHRD-3RD-PARTY,Virginia DOT,0000,08015,0,COMMIT,0,USD,TXN,USD,BASE-LE,2003,2003/03/01,09/23/2002,33.45,EA,33.45,1, , ,D,N,N, , , , , "
      val poRow2s = "c9545,0,A501_0000200268,IDpo1PO,2,COMMITMENT,PO Sys,Unknown,COMMITMENT_OFFSET,ACTUALS,FIN,SHRD-3RD-PARTY,Virginia DOT,0000,0000,0,0000,0,USD,TXN,USD,BASE-LE,2003,2003/03/01,09/23/2002,-33.45,EA,33.45,1, , ,D,N,N, , , , , "

      val payFile     = outDir.resolve("SortedJEFY03q1exp.csv")
      val poSortedFile= outDir.resolve("SortedJEFY03q1PO.csv")
      val mergedFile  = outDir.resolve("SortedJEFY03merged.csv")

      Files.write(payFile,      (payRow1 + "\n" + payRow2 + "\n").getBytes("UTF-8"))
      Files.write(poSortedFile, (poRow1s + "\n" + poRow2s + "\n").getBytes("UTF-8"))

      LedgerApp.dispatch("2m", "", outDir.toString, "", "", "2003",
        Map("outPath" -> outDir.toString, "year" -> "2003",
            "payment" -> payFile.toString, "po" -> poSortedFile.toString))

      assert(mergedFile.toFile.exists(), "Merged output file should be created")

      val src  = Source.fromFile(mergedFile.toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()

      assert(rows.length == 4, s"Merged file should have 4 rows; got ${rows.length}")

      // c9545 < z9999 — PO rows must come first in the merged output
      val ipids = rows.map(_.split(",", -1)(0))
      assert(ipids.head == "c9545", s"First row transIPID should be 'c9545'; got '${ipids.head}'")
      assert(ipids.last == "z9999", s"Last row transIPID should be 'z9999'; got '${ipids.last}'")
    }
  }
}
