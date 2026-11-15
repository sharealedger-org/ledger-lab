package org.universalledger.foundation.va.corpus

import java.io.{File, PrintWriter, StringWriter}
import java.nio.file.{Files, Path, Paths}
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.standardizeAndSort
import scala.collection.JavaConverters._
import scala.io.Source

class IdentityAuditSpec extends AnyFunSuite {
  import IdentityAudit._
  private def temp(body: Path => Unit): Unit = {
    val base = Paths.get("../../session-run.tmp"); Files.createDirectories(base)
    val dir = Files.createTempDirectory(base, "identity-").toAbsolutePath
    try body(dir) finally { val s = Files.walk(dir); try s.iterator().asScala.toVector.reverse.foreach(Files.deleteIfExists(_)) finally s.close() }
  }
  private def put(p: Path, s: String): Unit = { Files.createDirectories(p.getParent); Files.write(p, s.getBytes("UTF-8")); () }
  private def lines(p: Path): Vector[String] = { val s = Source.fromFile(p.toFile, "UTF-8"); try s.getLines().toVector finally s.close() }
  private val masterText = "instID,instHolderName\n1,\"ACME, INC\"\n2,\"ACME, LLC\"\n3,AGENCY\n"
  private def readMaster(p: Path): Master = master(p.toFile, new PrintWriter(new StringWriter), new PrintWriter(new StringWriter), "fixture")

  test("quoted names expose actual legacy truncation and last-row-wins collision") { temp { dir =>
    val f = dir.resolve("master.csv"); put(f, masterText)
    val m = readMaster(f)
    assert(m.lookup("ACME") == "0000000002")
    assert(m.status("ACME, INC") == ("unique", "0000000001"))
    assert(m.status("ACME, LLC") == ("unique", "0000000002"))
    put(f, "instID,instHolderName\n1,Acme\n2,ACME\n")
    val duplicate = readMaster(f)
    assert(duplicate.lookup("ACME") == "0000000002")
    assert(duplicate.status("acme")._1 == "ambiguous")
  }}

  test("case whitespace BOM numeric keys and generic labels remain explicit") { temp { dir =>
    val f = dir.resolve("master.csv"); put(f, "\uFEFFinstID,instHolderName\n88,\" Acme \"\n")
    val m = readMaster(f)
    assert(m.status("acme") == ("unique", "0000000088"))
    assert(m.lookup("ACME") == Zero) // legacy does not trim inside quoted fields
    assert(pad("88") == "0000000088")
    assert(pad("0000000088") == "0000000088")
    assert(label(" Journal Entry ") == "generic_label_heuristic")
    assert(label("Payroll Services Inc") == "meaningful_name_candidate")
    assert(strict("\"unclosed").isEmpty)
  }}

  test("bounded spill reduction combines duplicate keys across several merge levels") { temp { dir =>
    val c = new Counts(dir.resolve("spills").toFile, 2, 2)
    (1 to 15).foreach(i => { c.add(Vector("comma,name", (i % 3).toString)); c.add(Vector("another", "key")) })
    val result = scala.collection.mutable.Map[Vector[String], Long]()
    c.finish((k, n) => result(k) = n)
    assert(result(Vector("another", "key")) == 15)
    (0 to 2).foreach(i => assert(result(Vector("comma,name", i.toString)) == 5))
    assert(c.peakKeys <= 2 && c.levels >= 2)
  }}

  test("single-vendor count above production row guard retains only source counters") {
    val got = scala.collection.mutable.ArrayBuffer[(String, String, Long)]()
    val f = new VendorFold((id, source, n) => got += ((id, source, n)))
    (1 to 250001).foreach(_ => f.add(Zero, "Revenue Sys"))
    f.add("88", "eVA"); f.flush()
    assert(got.head == ((Zero, "Revenue Sys", 250001L)))
    assert(f.peakSourceKeys == 1 && f.rows == 250002)
    intercept[IllegalArgumentException] { f.add("1", "eVA") }
  }

  test("diagnostic records literal-key disorder and reentry without losing counts") {
    val got = scala.collection.mutable.ArrayBuffer[(String, String, Long)]()
    val segments = scala.collection.mutable.ArrayBuffer[String]()
    val bad = scala.collection.mutable.ArrayBuffer[(String, String)]()
    val f = new VendorFold((id, source, n) => got += ((id, source, n)), requireOrder = false,
      onDisorder = (a, b, _) => bad += ((a, b)), onSegment = id => segments += id)
    f.add("c12464", "PO Sys"); f.add("c1246", "PO Sys"); f.add("c12464", "PO Sys"); f.flush()
    assert(got.map(_._3).sum == 3 && f.rows == 3)
    assert(segments.count(_ == "c12464") == 2)
    assert(bad.toVector == Vector(("c12464", "c1246")))
  }

  test("a partial read digest is explicitly labeled rather than masking a scan failure") { temp { dir =>
    put(dir.resolve("input.csv"), "header\ndata\n")
    val text = new StringWriter; val manifest = new PrintWriter(text)
    val in = new Read(dir.resolve("input.csv").toFile, manifest)
    in.next(); in.close(); manifest.flush()
    assert(text.toString.contains("PARTIAL_READ:"))
  }}

  test("APA producer preserves quoted-name identity while audit retains legacy emulator") { temp { dir =>
    put(dir.resolve("master.csv"), masterText)
    val input = dir.resolve("raw/FY 2003 Expenditure/Expenditures Jul 2002.csv")
    put(input, "AGENCY_NAME,AMOUNT,FUND_NAME,LOCALITY,OBJECT_NAME,PROGRAM_NAME,VENDOR_NAME,VOUCHER_NUMBER,VOUCHER_DATE\n" +
      "AGENCY,10,FUND,VA,OBJECT,PROGRAM,\"ACME, INC\",V1,07-01-02\n")
    val output = dir.resolve("posted_input"); Files.createDirectories(output)
    standardizeAndSort.applyAPA(Map("inPath" -> dir.resolve("raw").toString, "outPath" -> output.toString,
      "refPath" -> dir.toString, "vendormaster" -> dir.resolve("master.csv").toString,
      "year" -> "03", "periodGranularity" -> "monthly"))
    val rows = lines(output.resolve("SortedJEFY03_0003_07_exp.csv"))
    assert(rows.size == 2 && rows.forall(_.split(",", -1).length == 39))
    assert(rows.forall(_.startsWith("0000000001,")))
    assert(rows.map(_.split(",", -1)(25)).map(BigDecimal(_)).sum == 0)
  }}

  test("quoted object text preserves the actual producer vendor lookup") { temp { dir =>
    put(dir.resolve("master.csv"), "instID,instHolderName\n1,REAL VENDOR\n")
    val row = "AGENCY,10,FUND,VA,\"SALARIES, TEACHING\",PROGRAM,REAL VENDOR,V1,07-01-02"
    assert(legacy(row)(6) == "PROGRAM")
    assert(strict(row).get(6) == "REAL VENDOR")
    put(dir.resolve("raw/FY 2003 Expenditure/Expenditures Jul 2002.csv"),
      "AGENCY_NAME,AMOUNT,FUND_NAME,LOCALITY,OBJECT_NAME,PROGRAM_NAME,VENDOR_NAME,VOUCHER_NUMBER,VOUCHER_DATE\n" + row + "\n")
    val output = dir.resolve("sje"); Files.createDirectories(output)
    standardizeAndSort.applyAPA(Map("inPath" -> dir.resolve("raw").toString, "outPath" -> output.toString,
      "refPath" -> dir.toString, "vendormaster" -> dir.resolve("master.csv").toString,
      "year" -> "03", "periodGranularity" -> "monthly"))
    val rows = lines(output.resolve("SortedJEFY03_0003_07_exp.csv"))
    assert(rows.size == 2 && rows.forall(_.startsWith("0000000001,")))
    assert(rows.map(_.split(",", -1)(25)).map(BigDecimal(_)).sum == 0)
  }}

  test("raw audit reconciles actual acceptance and resolves revenue lookup to agency key") { temp { dir =>
    put(dir.resolve("master.csv"), masterText)
    val months = Vector("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    for (kind <- Vector("Expenditure", "Revenue"); month <- months) {
      val expense = kind == "Expenditure"; val calYear = if (months.indexOf(month) >= 6) 2002 else 2003
      val header = if (expense) "AGENCY_NAME,AMOUNT,FUND_NAME,LOCALITY,OBJECT_NAME,PROGRAM_NAME,VENDOR_NAME,VOUCHER_NUMBER,VOUCHER_DATE" else "AGENCY_NAME,AMOUNT,FUND_NAME,LOCALITY,SOURCE_TYPE,SOURCE,DEPOSIT_DATE"
      val row = if (expense) "AGENCY,10,FUND,VA,OBJECT,PROGRAM,\"ACME, INC\",V1,07-01-02" else "AGENCY,20,FUND,VA,TAX,SOURCE,07-01-02"
      put(dir.resolve(s"raw/FY 2003 $kind/${if (expense) "Expenditures" else "Revenues"} $month $calYear.csv"), header + "\n" + (if (month == "Jul") row + "\n" else ""))
    }
    put(dir.resolve("run/FY03/metrics/process_metrics.csv"), "process_name,input_rows\nstep-ttl,2\n")
    main(Array("--mode", "raw", "--out", dir.resolve("audit").toString, "--root", dir.resolve("raw").toString,
      "--master", dir.resolve("master.csv").toString, "--currentMaster", dir.resolve("master.csv").toString,
      "--refPath", dir.toString, "--runRoot", dir.resolve("run").toString, "--years", "03", "--maxKeys", "2", "--fanIn", "2"))
    assert(lines(dir.resolve("audit/source_count_controls.csv")).last.endsWith(",0,PASS"))
    val names = lines(dir.resolve("audit/source_names.csv")).tail.map(strict(_).get)
    val revenue = names.find(_(0) == "revenue").get
    assert(revenue(2) == "AGENCY" && revenue(3) == "0" && revenue(4) == Zero)
    assert(revenue(9) == "agency_not_counterparty")
    assert(Files.exists(dir.resolve("audit/AUDIT_COMPLETE")))
  }}

  test("canonical scan combines years but does not silently equate padded PO keys") { temp { dir =>
    put(dir.resolve("master.csv"), "instID,instHolderName\n88,ACME\n")
    for (year <- Vector("03", "04")) {
      def row(id: String, source: String, movement: Boolean): String = {
        val r = Array.fill(if (movement) 39 else 31)("0"); r(0) = id; r(if (movement) 6 else 4) = source; r.mkString(",")
      }
      put(dir.resolve(s"run/FY$year/SortedJEFY${year}merged.csv"), Vector(row(Zero, "Payment Sys", true), row(Zero, "Revenue Sys", true), row("88", "eVA", true)).mkString("\n") + "\n")
      put(dir.resolve(s"run/FY$year/LDGR20$year.csv"), "ldgrIPID,fixture_header\n" + Vector(row(Zero, "Payment Sys", false), row("0000000088", "Payment Sys", false), row("88", "eVA", false)).mkString("\n") + "\n")
    }
    main(Array("--mode", "canonical", "--out", dir.resolve("audit").toString, "--master", dir.resolve("master.csv").toString,
      "--currentMaster", dir.resolve("master.csv").toString, "--runRoot", dir.resolve("run").toString, "--years", "03,04", "--maxKeys", "2", "--fanIn", "2"))
    val rows = lines(dir.resolve("audit/vendor_concentration.csv")).tail.map(_.split(",", -1))
    assert(rows.find(_(0) == Zero).get(3) == "6")
    assert(rows.find(_(0) == "88").get.last == "true")
    assert(rows.find(_(0) == "0000000088").get(6) == "true")
  }}
}
