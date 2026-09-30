package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.file.{Files, Path, Paths}

import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source

class StandardizeAndSortSpec extends AnyFunSuite {
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

  private val sourceHeader =
    "AGY_AGENCY_KEY,AMOUNT,FNDDTL_FUND_DETAIL_KEY,OBJ_OBJECT_KEY,SPRG_SUB_PROGRAM_KEY,VENDOR_NAME,TRANS_DATE"

  private def writeInputs(inputDir: Path, sourceRow: String): Path = {
    Files.write(
      inputDir.resolve("FY14q1exp.csv"),
      s"$sourceHeader\n$sourceRow\n".getBytes("UTF-8")
    )
    Files.write(
      inputDir.resolve("VAAccountingRules.csv"),
      Seq(
        "recordType,ruleSetId,ruleVersion,ruleId,businessEventCode,debitAccountExpression,creditAccountExpression,debitProjectExpression,creditProjectExpression,offsetDescription,effectiveStart,effectiveEnd",
        "E,TEST,1,EXP,EXPENSE_PAYMENT,EXP+object,0000,object,zero,CASH_CLEARING,2003-01-01,9999-12-31"
      ).mkString("\n").getBytes("UTF-8")
    )
    val vendorMaster = inputDir.resolve("VendorMaster.csv")
    Files.write(
      vendorMaster,
      "instID,instHolderName\n0000000001,TEST VENDOR\n".getBytes("UTF-8")
    )
    vendorMaster
  }

  private def runTransform(inputDir: Path, outputDir: Path, vendorMaster: Path): Unit = {
    standardizeAndSort(Map(
      "inPath" -> inputDir.toString,
      "outPath" -> outputDir.toString,
      "vendormaster" -> vendorMaster.toString,
      "year" -> "2014",
      "quarter" -> "1",
      "type" -> "E"
    ))
  }

  test("CSV source header and quoted comma fields are parsed before SJE output") {
    withTempDirectory("ttl-csv-valid-") { tempDir =>
    val inputDir = Files.createDirectory(tempDir.resolve("input"))
    val outputDir = Files.createDirectory(tempDir.resolve("output"))
    val vendorMaster = writeInputs(inputDir, "267,12.34,1552,56,2701,\"Clintwood, Va.\",2014-01-15")

    runTransform(inputDir, outputDir, vendorMaster)

    val output = outputDir.resolve("SortedJEFY14q1exp.csv")
    val source = Source.fromFile(output.toFile, "UTF-8")
    val rows = try source.getLines().toVector finally source.close()
    assert(rows.length == 2)
    assert(rows.forall(_.split(",", -1).length == 39))
    assert(rows.map(_.split(",", -1)(25).toDouble).sum == 0.0)
    }
  }

  test("malformed CSV transaction rows are rejected") {
    withTempDirectory("ttl-csv-invalid-") { tempDir =>
    val inputDir = Files.createDirectory(tempDir.resolve("input"))
    val outputDir = Files.createDirectory(tempDir.resolve("output"))
    val vendorMaster = writeInputs(inputDir, "267,12.34,1552,56,2701,\"Clintwood, Va.\"")

    val error = intercept[IllegalArgumentException] {
      runTransform(inputDir, outputDir, vendorMaster)
    }

    assert(error.getMessage.contains("Source row 2 has 6 fields; expected 7 for C"))
    }
  }
}
