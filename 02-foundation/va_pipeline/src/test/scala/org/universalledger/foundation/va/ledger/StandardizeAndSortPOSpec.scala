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

  // ── Test 8: Step 2m merges payment + PO files in sort order ─────────────────
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
