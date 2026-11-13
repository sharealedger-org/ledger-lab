package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.file.{Files, Path, Paths}

import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source

// ── Tests for Step ttl (APA Technical Transform Layer) ──────────────────────────────────────
//
//  Architecture contract being tested:
//    - applyAPA discovers monthly APA expenditure CSVs under an "FY YYYY Expenditure" folder,
//      resolves AGENCY_NAME / FUND_NAME / OBJECT_NAME / PROGRAM_NAME to numeric keys via the
//      DPB reference tables (aRef*.txt), and writes one SortedJEFY{yr}q{n}exp.csv per quarter.
//    - loadApaReferenceData loads the four reference tables and returns an ApaReferenceData
//      with the expected map sizes.
//    - Each source row produces exactly 2 SJE rows (debit + credit) that sum to zero.
//    - Rows with invalid amounts are skipped; name-unresolved rows still produce SJE rows
//      (with key "0") — the reference miss is counted in unresolved_ref, not dropped.
//    - Revenue files (AGENCY_NAME / AMOUNT / FUND_NAME / LOCALITY / SOURCE_TYPE / SOURCE /
//      DEPOSIT_DATE) are routed to the revenue path.
//    - RESULT step=ttl and RESOURCE step=ttl log lines are emitted.

class ApaTtlSpec extends AnyFunSuite {

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

  // ── Reference table fixture: minimal aRef*.txt files ───────────────────────────
  // Creates the four reference tables as real CSV files under refDir.
  private def writeRefTables(refDir: Path): Unit = {
    // Agency: AGY_AGENCY_CODE, AGY_AGENCY_KEY, AGY_AGENCY_NAME
    Files.write(refDir.resolve("aRefAgencyCSV.txt"),
      "AGY_AGENCY_CODE,AGY_AGENCY_KEY,AGY_AGENCY_NAME\n204,237,The College of William and Mary in Virginia\n207,238,University of Virginia-Academic Division\n".getBytes("UTF-8"))
    // Fund: FNDDTL_FUND_DETAIL_KEY, FNDDTL_FUND_DETAIL_CODE, FNDDTL_FUND_DETAIL_NAME, ...
    Files.write(refDir.resolve("aRefFundCSV.txt"),
      "FNDDTL_FUND_DETAIL_KEY,FNDDTL_FUND_DETAIL_CODE,FNDDTL_FUND_DETAIL_NAME,FND_FUND_CODE,FND_FUND_NAME\n1552,0100,HIGHER EDUCATION OPERATING,01,GENERAL\n1553,0200,AUXILIARY ENTERPRISE,02,SPECIAL REVENUE\n".getBytes("UTF-8"))
    // Object: OBJ_OBJECT_KEY, 2-DIGIT OBJECT CODE, 2-DIGIT OBJECT NAME, 4-DIGIT OBJECT CODE, 4-DIGIT OBJECT NAME
    Files.write(refDir.resolve("aRefObjectCSV.txt"),
      "OBJ_OBJECT_KEY,2-DIGIT OBJECT CODE,2-DIGIT OBJECT NAME,4-DIGIT OBJECT CODE,4-DIGIT OBJECT NAME\n56,12,CONTRACTUAL SERVICES,1260,LAUNDRY & LINEN SERVICES\n57,13,SUPPLIES AND MATERIALS,1310,PRINTING SERVICES\n".getBytes("UTF-8"))
    // Program: SPRG_SUB_PROGRAM_KEY, ..., SPRG_SUB_PROGRAM_NAME
    Files.write(refDir.resolve("aRefProgramCSV.txt"),
      "SPRG_SUB_PROGRAM_KEY,PFCN_FUNCTION_CODE,PFCN_FUNCTION_NAME,PRG_PROGRAM_CODE,PRG_PROGRAM_NAME,SPRG_SUB_PROGRAM_CODE,,SPRG_SUB_PROGRAM_NAME\n2701,1,EDUCATION,201,INSTRUCTION,20101,,CUSTODIAL SERVICE\n2702,1,EDUCATION,201,INSTRUCTION,20102,,COMPUTING SUPPORT\n".getBytes("UTF-8"))
    // Source: SRC_SOURCE_KEY, SRCCLS_SOURCE_CLASS_CODE, ..., SRC_SOURCE_NAME
    Files.write(refDir.resolve("aRefSourceCSV.txt"),
      "SRC_SOURCE_KEY,SRCCLS_SOURCE_CLASS_CODE,SRCCLS_SOURCE_CLASS_NAME,SRC_SOURCE_CODE,,SRC_SOURCE_NAME\n801,1,TAXES,1001,,LOCAL FUND TAXES\n802,2,FEES,1002,,LOCAL FUND INSTITUTIONAL REVENUE\n".getBytes("UTF-8"))
  }

  // ── APA expenditure file fixture ────────────────────────────────────────────
  private val expHeader =
    "\"AGENCY_NAME\",\"AMOUNT\",\"FUND_NAME\",\"LOCALITY\",\"OBJECT_NAME\",\"PROGRAM_NAME\",\"VENDOR_NAME\",\"VOUCHER_NUMBER\",\"VOUCHER_DATE\""

  // Row in Q1 (Jul 2002): agency=The College of William and Mary in Virginia
  private val expRowJul =
    "\"The College of William and Mary in Virginia\",45.32,\"HIGHER EDUCATION OPERATING\",\"VIRGINIA\",\"LAUNDRY & LINEN SERVICES\",\"CUSTODIAL SERVICE\",\"SERVITEX INC\",\"001526A\",07-08-02"

  // Row in Q4 (Apr 2003)
  private val expRowApr =
    "\"The College of William and Mary in Virginia\",1195.22,\"HIGHER EDUCATION OPERATING\",\"VIRGINIA\",\"PRINTING SERVICES\",\"COMPUTING SUPPORT\",\"PRINTING CO\",\"6003040\",04-30-03"

  private def writeExpFile(fyDir: Path, month: String, rows: String*): Path = {
    val csvFile = fyDir.resolve(s"Expenditures $month 2003.csv")
    val content = (expHeader +: rows).mkString("\n") + "\n"
    Files.write(csvFile, content.getBytes("UTF-8"))
    csvFile
  }

  // ── APA revenue file fixture ─────────────────────────────────────────────────
  private val revHeader =
    "\"AGENCY_NAME\",\"AMOUNT\",\"FUND_NAME\",\"LOCALITY\",\"SOURCE_TYPE\",\"SOURCE\",\"DEPOSIT_DATE\""

  private val revRowJul =
    "\"University of Virginia - College at Wise\",240,\"HIGHER EDUCATION LOCAL FUNDS UNRESTRICTED\",\"VIRGINIA\",\"TAXES\",\"LOCAL FUND TAXES\",07-31-02"

  // ── Test 1: reference table loading ─────────────────────────────────────────
  test("loadApaReferenceData loads four reference tables with correct entry counts") {
    withTempDirectory("ttl-ref-") { tempDir =>
      writeRefTables(tempDir)
      val ref = standardizeAndSort.loadApaReferenceData(tempDir.toString)
      assert(ref.agencyByName.size == 2,  s"Expected 2 agency entries; got ${ref.agencyByName.size}")
      assert(ref.fundByName.size == 2,    s"Expected 2 fund entries; got ${ref.fundByName.size}")
      assert(ref.objectByName.size == 2,  s"Expected 2 object entries; got ${ref.objectByName.size}")
      assert(ref.programByName.size == 2, s"Expected 2 program entries; got ${ref.programByName.size}")
      assert(ref.sourceByName.size == 2,  s"Expected 2 source entries; got ${ref.sourceByName.size}")
    }
  }

  // ── Test 2: applyAPA discovers Q1 expenditure file and produces 2 SJE rows per source row ──
  test("Step ttl: applyAPA produces 2 SJE rows per source row with 39 columns") {
    withTempDirectory("ttl-cols-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val fyDir  = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val refDir = Files.createDirectory(tempDir.resolve("data"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)
      writeExpFile(fyDir, "Jul 2002", expRowJul)

      standardizeAndSort.applyAPA(Map(
        "inPath"  -> inDir.toString,
        "refPath" -> refDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "type"    -> "E"
      ))

      val q1File = outDir.resolve("SortedJEFY03q1exp.csv").toFile
      assert(q1File.exists(), "SortedJEFY03q1exp.csv should be created")
      val src  = Source.fromFile(q1File, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      assert(rows.length == 2, s"Expected 2 SJE rows (1 source × 2), got ${rows.length}")
      assert(rows.forall(_.split(",", -1).length == 39),
        s"Every row must have 39 columns; got ${rows.map(_.split(",", -1).length).distinct}")
    }
  }

  // ── Test 3: debit + credit amounts cancel ───────────────────────────────────
  test("Step ttl: debit and credit amounts cancel within each journal group") {
    withTempDirectory("ttl-balance-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val fyDir  = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val refDir = Files.createDirectory(tempDir.resolve("data"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)
      writeExpFile(fyDir, "Jul 2002", expRowJul)

      standardizeAndSort.applyAPA(Map(
        "inPath"  -> inDir.toString,
        "refPath" -> refDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "type"    -> "E"
      ))

      val src  = Source.fromFile(outDir.resolve("SortedJEFY03q1exp.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      assert(rows.length == 2)
      val amounts = rows.map(r => BigDecimal(r.split(",", -1)(25)))
      assert(amounts.sum == BigDecimal(0), s"Debit + credit must sum to 0; got ${amounts.sum}")
    }
  }

  // ── Test 4: agency name resolved to key (col 12 = transLegalEntityID) ───────
  test("Step ttl: AGENCY_NAME is resolved to AGY_AGENCY_KEY in transLegalEntityID (col 12)") {
    withTempDirectory("ttl-agencykey-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val fyDir  = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val refDir = Files.createDirectory(tempDir.resolve("data"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)
      writeExpFile(fyDir, "Jul 2002", expRowJul)

      standardizeAndSort.applyAPA(Map(
        "inPath"  -> inDir.toString,
        "refPath" -> refDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "type"    -> "E"
      ))

      val src  = Source.fromFile(outDir.resolve("SortedJEFY03q1exp.csv").toFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      // The debit row (lineId=1) carries agency at col 12
      val debitRow = rows.find(_.split(",", -1)(4) == "1").get
      val agencyKey = debitRow.split(",", -1)(12)
      assert(agencyKey == "237",
        s"transLegalEntityID should be '237' (AGY_AGENCY_KEY for William and Mary); got '$agencyKey'")
    }
  }

  // ── Test 5: files routed to correct quarter ──────────────────────────────────
  test("Step ttl: monthly files are routed to correct fiscal quarters") {
    withTempDirectory("ttl-quarters-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val fyDir  = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val refDir = Files.createDirectory(tempDir.resolve("data"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)
      // Q1 file (Jul 2002) and Q4 file (Apr 2003)
      writeExpFile(fyDir, "Jul 2002", expRowJul)
      writeExpFile(fyDir, "Apr 2003", expRowApr)

      standardizeAndSort.applyAPA(Map(
        "inPath"  -> inDir.toString,
        "refPath" -> refDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "type"    -> "E"
      ))

      val q1File = outDir.resolve("SortedJEFY03q1exp.csv").toFile
      val q4File = outDir.resolve("SortedJEFY03q4exp.csv").toFile
      assert(q1File.exists(), "Q1 output file should be created")
      assert(q4File.exists(), "Q4 output file should be created")

      val q1Rows = { val s = Source.fromFile(q1File, "UTF-8"); try s.getLines().filter(_.trim.nonEmpty).toVector finally s.close() }
      val q4Rows = { val s = Source.fromFile(q4File, "UTF-8"); try s.getLines().filter(_.trim.nonEmpty).toVector finally s.close() }
      assert(q1Rows.length == 2, s"Q1 should have 2 rows; got ${q1Rows.length}")
      assert(q4Rows.length == 2, s"Q4 should have 2 rows; got ${q4Rows.length}")

      // Q2 and Q3 have no source files — output should not be created
      assert(!outDir.resolve("SortedJEFY03q2exp.csv").toFile.exists(), "Q2 should not be created (no source)")
      assert(!outDir.resolve("SortedJEFY03q3exp.csv").toFile.exists(), "Q3 should not be created (no source)")
    }
  }

  // ── Test 6: revenue files routed to rev output ──────────────────────────────
  test("Step ttl: revenue files produce SortedJEFY03rev.csv with correct schema") {
    withTempDirectory("ttl-revenue-") { tempDir =>
      val inDir   = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val revFyDir = Files.createDirectory(inDir.resolve("FY 2003 Revenue"))
      val refDir  = Files.createDirectory(tempDir.resolve("data"))
      val outDir  = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)

      // Write one revenue month file
      val revFile = revFyDir.resolve("Revenues Jul 2002.csv")
      Files.write(revFile, (revHeader + "\n" + revRowJul + "\n").getBytes("UTF-8"))

      standardizeAndSort.applyAPA(Map(
        "inPath"  -> inDir.toString,
        "refPath" -> refDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "type"    -> "R"
      ))

      val revOut = outDir.resolve("SortedJEFY03rev.csv").toFile
      assert(revOut.exists(), "SortedJEFY03rev.csv should be created")
      val src  = Source.fromFile(revOut, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      assert(rows.length == 2, s"Expected 2 revenue SJE rows; got ${rows.length}")
      assert(rows.forall(_.split(",", -1).length == 39), "Revenue rows must have 39 columns")
      val amounts = rows.map(r => BigDecimal(r.split(",", -1)(25)))
      assert(amounts.sum == BigDecimal(0), s"Revenue debit+credit must sum to 0; got ${amounts.sum}")
    }
  }

  test("combined monthly TTL preserves revenue and expenditure through posting") {
    withTempDirectory("ttl-both-post-") { tempDir =>
      val inDir = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val expDir = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val revDir = Files.createDirectory(inDir.resolve("FY 2003 Revenue"))
      val refs = Files.createDirectory(tempDir.resolve("refs"))
      val out = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refs)
      writeExpFile(expDir, "Jul", expRowJul)
      Files.write(revDir.resolve("Revenues Jul 2002.csv"),
        (revHeader + "\n" + revRowJul.replace("HIGHER EDUCATION LOCAL FUNDS UNRESTRICTED", "HIGHER EDUCATION OPERATING") + "\n").getBytes("UTF-8"))
      standardizeAndSort.applyAPA(Map("inPath" -> inDir.toString,
        "refPath" -> refs.toString, "outPath" -> out.toString,
        "year" -> "03", "type" -> "B", "periodGranularity" -> "monthly"))
      def rows(file: File): Vector[Array[String]] = {
        val source = Source.fromFile(file, "UTF-8")
        try source.getLines().filter(_.nonEmpty).map(_.split(",", -1)).toVector
        finally source.close()
      }
      val partitions = out.toFile.listFiles().filter(_.getName.startsWith("SortedJE"))
      val sjes = partitions.toVector.flatMap(rows)
      assert(sjes.size == 4)
      assert(sjes.groupBy(_(3)).values.forall(_.map(r => BigDecimal(r(25))).sum == 0))
      assert(sjes.filter(_(16).startsWith("REV")).map(r => BigDecimal(r(25))).sum == BigDecimal(240))
      val receipt = sjes.find(_(16) == "REV801").get
      assert(receipt(13) == "1552")
      assert(receipt(24) == "07-31-02")
      assert(sjes.filter(_(16).startsWith("EXP")).map(r => BigDecimal(r(25))).sum == BigDecimal("45.32"))
      post(out.toString + "/", out.toString + "/", "03")
      val ledger = rows(out.resolve("LDGR2003.csv").toFile).tail
      assert(ledger.map(r => BigDecimal(r(19))).sum == 0)
      assert(ledger.filter(_(12).startsWith("REV")).map(r => BigDecimal(r(19))).sum == BigDecimal(240))
      assert(ledger.filter(_(12).startsWith("EXP")).map(r => BigDecimal(r(19))).sum == BigDecimal("45.32"))
    }
  }

  // ── Test 7: RESULT and RESOURCE log lines emitted ──────────────────────────
  test("Step ttl: RESULT step=ttl and RESOURCE step=ttl are emitted to stdout") {
    withTempDirectory("ttl-metrics-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val fyDir  = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val refDir = Files.createDirectory(tempDir.resolve("data"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)
      writeExpFile(fyDir, "Jul 2002", expRowJul)

      val output = new java.io.ByteArrayOutputStream()
      Console.withOut(new java.io.PrintStream(output)) {
        standardizeAndSort.applyAPA(Map(
          "inPath"  -> inDir.toString,
          "refPath" -> refDir.toString,
          "outPath" -> outDir.toString,
          "year"    -> "2003",
          "type"    -> "E"
        ))
      }
      val log = output.toString("UTF-8")
      assert(log.contains("RESULT step=ttl"), s"Expected 'RESULT step=ttl' in output")
      assert(log.contains("RESOURCE step=ttl"), s"Expected 'RESOURCE step=ttl' in output")
    }
  }

  // ── Test 8: unresolved reference names still produce rows (key="0") ─────────
  test("Step ttl: source rows with unknown agency name still produce SJE rows with key '0'") {
    withTempDirectory("ttl-unresolved-") { tempDir =>
      val inDir  = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val fyDir  = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val refDir = Files.createDirectory(tempDir.resolve("data"))
      val outDir = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)

      // Row with an agency name NOT in the reference table
      val unknownAgencyRow =
        "\"Unknown New Agency\",100.00,\"HIGHER EDUCATION OPERATING\",\"VIRGINIA\"," +
        "\"LAUNDRY & LINEN SERVICES\",\"CUSTODIAL SERVICE\",\"SOME VENDOR\",\"VCH001\",07-15-02"
      writeExpFile(fyDir, "Jul 2002", unknownAgencyRow)

      standardizeAndSort.applyAPA(Map(
        "inPath"  -> inDir.toString,
        "refPath" -> refDir.toString,
        "outPath" -> outDir.toString,
        "year"    -> "2003",
        "type"    -> "E"
      ))

      val q1File = outDir.resolve("SortedJEFY03q1exp.csv").toFile
      assert(q1File.exists(), "Output should still be created for unresolved rows")
      val src  = Source.fromFile(q1File, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      assert(rows.length == 2, s"Unresolved row should still produce 2 SJE rows; got ${rows.length}")
      // transLegalEntityID (col 12) should be "0" for unresolved agency
      val agencyKeys = rows.map(_.split(",", -1)(12))
      assert(agencyKeys.forall(_ == "0"), s"Agency key should be '0' for unresolved name; got $agencyKeys")
    }
  }


  // ── Test 9: two-digit --year must select the 4-digit fiscal-year folder ─────
  // "--year 03" arrives as 3. Matching the bare digit also selects "FY 2013 Expenditure".
  test("Step ttl: year '03' reads FY 2003 and not FY 2013 (folders share the digit 3)") {
    withTempDirectory("ttl-yearfolder-") { tempDir =>
      val inDir   = Files.createDirectory(tempDir.resolve("VARawFiles"))
      val fy2003  = Files.createDirectory(inDir.resolve("FY 2003 Expenditure"))
      val fy2013  = Files.createDirectory(inDir.resolve("FY 2013 Expenditure"))
      val refDir  = Files.createDirectory(tempDir.resolve("data"))
      val outDir  = Files.createDirectory(tempDir.resolve("out"))
      writeRefTables(refDir)
      writeExpFile(fy2003, "Jul 2002", expRowJul)                       // 1 source row
      writeExpFile(fy2013, "Jul 2012", expRowJul, expRowJul, expRowJul) // 3 source rows

      val output = new java.io.ByteArrayOutputStream()
      Console.withOut(output) {
        standardizeAndSort.applyAPA(Map(
          "inPath"            -> inDir.toString,
          "refPath"           -> refDir.toString,
          "outPath"           -> outDir.toString,
          "year"              -> "03",
          "type"              -> "E",
          "periodGranularity" -> "monthly"
        ))
      }

      val outFile = outDir.resolve("SortedJEFY03_0003_07_exp.csv").toFile
      assert(outFile.exists(), "monthly output for Jul should be created")
      val src  = Source.fromFile(outFile, "UTF-8")
      val rows = try src.getLines().filter(_.trim.nonEmpty).toVector finally src.close()
      assert(rows.length == 2, s"FY 2003 has 1 source row (2 SJE rows); got ${rows.length} (FY 2013 has 3 source rows)")

      val log = output.toString("UTF-8")
      assert(log.contains("Source folder (Expenditure FY2003): " + fy2003.toAbsolutePath), s"log should name the FY 2003 folder:\n$log")
      assert(log.contains("data_lines=1 source=1 unresolved_ref=0 blank=0"), s"per-file reconciliation line missing:\n$log")
    }
  }
}
