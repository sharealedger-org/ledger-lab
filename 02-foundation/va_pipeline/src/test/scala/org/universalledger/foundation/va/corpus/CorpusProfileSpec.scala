package org.universalledger.foundation.va.corpus

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.JavaConverters._

class CorpusProfileSpec extends AnyFunSuite {
  import CorpusProfile._

  private def withTempDirectory(test: Path => Unit): Unit = {
    val tempRoot = Paths.get("../../session-run.tmp").toAbsolutePath.normalize()
    Files.createDirectories(tempRoot)
    val dir = Files.createTempDirectory(tempRoot, "corpus-")
    try test(dir)
    finally {
      val st = Files.walk(dir)
      try st.iterator().asScala.toList.reverse.foreach(p => Files.deleteIfExists(p)) finally st.close()
    }
  }

  private val expHeader =
    "\"AGENCY_NAME\",\"AMOUNT\",\"FUND_NAME\",\"LOCALITY\",\"OBJECT_NAME\",\"PROGRAM_NAME\",\"VENDOR_NAME\",\"VOUCHER_NUMBER\",\"VOUCHER_DATE\""

  private def expRow(amount: String, vendor: String, date: String = "04-30-03", voucher: String = "0012345") =
    s"""\"Agency A\",$amount,\"GENERAL\",\"VIRGINIA\",\"OBJ\",\"PRG\",$vendor,\"$voucher\",$date"""

  private def write(p: Path, content: String): Unit = {
    Files.createDirectories(p.getParent)
    Files.write(p, content.getBytes(StandardCharsets.UTF_8))
  }

  private def runOn(root: Path): (Seq[FileResult], Seq[String]) = {
    val out = root.resolve("out")
    val rs = CorpusProfile.run(Config(root, out, Seq("VARawFiles", "VAPOData"), ""))
    (rs, Files.readAllLines(out.resolve("quarantine.csv")).asScala.toList)
  }

  test("classify maps APA monthly, budget and PO names; July-December months belong to the next fiscal year") {
    val a = classify("VARawFiles/FY 2003 Expenditure/Expenditures Aug 2002.csv")
    assert(a.family.exists(_.name == "apa_expenditure"))
    assert(a.fiscalYear.contains(2003) && a.fyFromPeriod.contains(2003) && a.period == "2002-08")
    val b = classify("VARawFiles/FY 2003 Revenue/Revenues Apr 2003.csv")
    assert(b.family.exists(_.name == "apa_revenue") && b.fyFromPeriod.contains(2003))
    val sept = classify("VARawFiles/All Expenditures for Fiscal Year 2014/Expenditures Sept 2013.csv")
    assert(sept.family.exists(_.name == "apa_expenditure") && sept.period == "2013-09" && sept.fyFromPeriod.contains(2014))
    assert(classify("VARawFiles/Budget FY2005.csv").fiscalYear.contains(2005))
    assert(classify("VAPOData/VA_opendata_FY2016.txt").family.exists(_.name == "eva_po"))
    assert(classify("VAPOData/2003 Combined.xlsx").family.isEmpty)
  }

  test("parseRecord handles escaped quotes, embedded delimiters and an unterminated quote") {
    val ok = parseRecord("\"a,b\",\"c\"\"d\",e,".getBytes(StandardCharsets.UTF_8), ',')
    assert(ok.closed && ok.fields.toList == List("a,b", "c\"d", "e", ""))
    val bad = parseRecord("\"abc,def".getBytes(StandardCharsets.UTF_8), ',')
    assert(!bad.closed)
  }

  test("parseDate validates calendar dates and two-digit years") {
    assert(parseDate("04-30-03") == 20030430)
    assert(parseDate("04/30/2003") == 20030430)
    assert(parseDate("02-29-04") == 20040229)
    assert(parseDate("02-29-03") == -1)
    assert(parseDate("13-01-03") == -1)
    assert(parseDate("") == -1)
  }

  test("expenditure file: counts, signed and absolute totals, dispositions, quarantine and line reconciliation") {
    withTempDirectory { root =>
      val rows = Seq(
        expHeader,
        expRow("100.10", "\"ACME\""),
        expRow("-40.05", "\"ACME\""),
        expRow("100.10", "\"ACME\""),                                       // legitimate repeat: duplicate candidate, kept
        expRow("7", "\"Multi\nLine Vendor\""),                              // quoted multiline record: accepted
        expRow("abc", "\"BADAMT\""),                                        // BAD_AMOUNT
        "\"Agency A\",5,\"GENERAL\"",                                       // FIELD_COUNT
        expRow("3", "\"\""),                                                // blank vendor key: accepted, counted blank
        "",                                                                 // blank physical line
        expRow("1", "\"V\"", date = "13-45-03")                             // date unparseable: accepted
      )
      write(root.resolve("VARawFiles/FY 2003 Expenditure/Expenditures Apr 2003.csv"), rows.mkString("\r\n") + "\r\n")
      val (rs, q) = runOn(root)
      val r = rs.head
      assert(r.status == "PROFILED_WITH_QUARANTINE")
      assert(r.headerRecs == 1)
      assert(r.parsed == 8 && r.accepted == 6 && r.quarantined == 2)
      assert(r.qReasons.toMap == Map("BAD_AMOUNT" -> 1L, "FIELD_COUNT" -> 1L))
      assert(r.multiline == 1)
      assert(r.dup == 1)
      assert(r.keyBlank == 1)
      assert(r.dateBad == 1)
      assert(r.amountSum.compareTo(new java.math.BigDecimal("171.15")) == 0)  // 100.10-40.05+100.10+7+3+1
      assert(r.amountAbs.compareTo(new java.math.BigDecimal("251.25")) == 0)
      assert(r.amountNeg == 1 && r.amountPos == 5)
      assert(r.reconciles, s"physical=${r.physLines} blank=${r.blankLines} inRecords=${r.linesInRecords}")
      assert(q.size == 3) // header + 2 quarantined originals preserved
      assert(q.exists(_.contains("BAD_AMOUNT")) && q.exists(_.contains("FIELD_COUNT")))
    }
  }

  test("SQL*Plus trailer lines are EXTRACT_ARTIFACT and the declared row count is compared with parsed records") {
    withTempDirectory { root =>
      val rows = Seq(expHeader, expRow("1", "\"A\""), expRow("2", "\"B\""), "", "2 rows selected. ",
        "SQL> spool \"D:\\\\DP Extracts\\\\next.csv\"")
      write(root.resolve("VARawFiles/FY 2003 Expenditure/Expenditures Apr 2003.csv"), rows.mkString("\n") + "\n")
      val out = root.resolve("out")
      val r = CorpusProfile.run(Config(root, out, Seq("VARawFiles"), "")).head
      assert(r.accepted == 2 && r.qReasons.toMap == Map("EXTRACT_ARTIFACT" -> 2L))
      assert(r.declaredRows == 2 && r.dataRecords == 2)
      assert(r.reconciles)
      val fp = Files.readAllLines(out.resolve("file_profile.csv")).asScala
      assert(fp.last.endsWith(",2,2,true"), fp.last)
    }
  }

  test("an unterminated quote quarantines one line and does not swallow the records after it") {
    withTempDirectory { root =>
      val rows = Seq(expHeader, expRow("1", "\"A\""), expRow("2", "\"BROKEN"), expRow("3", "\"C\""), expRow("4", "\"D\""))
      write(root.resolve("VARawFiles/FY 2003 Expenditure/Expenditures Apr 2003.csv"), rows.mkString("\n") + "\n")
      val r = runOn(root)._1.head
      assert(r.quarantined == 1, s"quarantined=${r.quarantined} reasons=${r.qReasons}")
      assert(r.accepted == 3)
      assert(r.reconciles)
    }
  }

  test("BOM is detected and a header-only file is classified, not treated as data") {
    withTempDirectory { root =>
      val p = root.resolve("VARawFiles/FY 2003 Expenditure/Expenditures May 2003.csv")
      Files.createDirectories(p.getParent)
      Files.write(p, Array[Byte](0xEF.toByte, 0xBB.toByte, 0xBF.toByte) ++ (expHeader + "\n").getBytes(StandardCharsets.UTF_8))
      val r = runOn(root)._1.head
      assert(r.bom && r.status == "HEADER_ONLY" && r.parsed == 0 && r.ncols == 9)
      assert(r.reconciles)
    }
  }

  test("empty file is EMPTY_FILE with zero bytes and shows in coverage as empty, missing years as MISSING") {
    withTempDirectory { root =>
      write(root.resolve("VAPOData/VA_opendata_FY2005.txt"), "")
      val out = root.resolve("out")
      val rs = CorpusProfile.run(Config(root, out, Seq("VARawFiles", "VAPOData"), ""))
      assert(rs.head.status == "EMPTY_FILE" && rs.head.bytes == 0)
      val cov = Files.readAllLines(out.resolve("coverage_matrix.csv")).asScala
      assert(cov.exists(l => l.startsWith("eva_po,2005,EMPTY_OR_HEADER_ONLY")))
      assert(cov.exists(l => l.startsWith("eva_po,2009,MISSING")))
    }
  }

  test("PO file: blank first line tolerated, ID shapes classified, extended amount, placeholder counted") {
    withTempDirectory { root =>
      val h = "PONUMBER\tAGENCY\tORDEREDDATE\tQUANTITYORDERED\tPRICE\tVENDORID\tVENDORNAME"
      val rows = Seq(
        "", h,
        "P1\t100\t07/15/2002\t2\t10.50\t123456789\tV1",   // 9 digits
        "P2\t100\t08/01/2002\t1\t5\tAB1234567\tV2",       // 2 letters + 7 digits
        "P3\t100\t09/01/2002\t1\t5\t999999999999\tV3",    // placeholder
        "P4\t100\t09/01/2002\t1\t5\t\tV4",                // blank
        "P5\t100\t09/01/2001\t1\t5\t987654321\tV5"        // outside FY2003 window
      )
      write(root.resolve("VAPOData/VA_opendata_FY2003.txt"), rows.mkString("\n") + "\n")
      val r = runOn(root)._1.head
      assert(r.status == "PROFILED_CLEAN" && r.accepted == 5 && r.blankLines == 1)
      assert(r.keyD9 == 2 && r.keyA2D7 == 1 && r.keyPlaceholder == 1 && r.keyBlank == 1)
      assert(r.amountSum.compareTo(new java.math.BigDecimal("41")) == 0) // 21 + 5 + 5 + 5 + 5
      assert(r.fyViolations == 1)
      assert(r.reconciles)
    }
  }

  test("two runs over identical input produce byte-identical outputs") {
    withTempDirectory { root =>
      write(root.resolve("VARawFiles/FY 2003 Expenditure/Expenditures Apr 2003.csv"),
        Seq(expHeader, expRow("1", "\"A\""), expRow("x", "\"B\"")).mkString("\n") + "\n")
      val out1 = root.resolve("out1"); val out2 = root.resolve("out2")
      CorpusProfile.run(Config(root, out1, Seq("VARawFiles"), ""))
      CorpusProfile.run(Config(root, out2, Seq("VARawFiles"), ""))
      Seq("source_catalog.csv", "file_profile.csv", "file_field_profile.csv", "schema_catalog.csv",
        "coverage_matrix.csv", "quarantine.csv", "corpus_summary.txt").foreach { n =>
        assert(Files.readAllBytes(out1.resolve(n)).sameElements(Files.readAllBytes(out2.resolve(n))), n)
      }
    }
  }
}
