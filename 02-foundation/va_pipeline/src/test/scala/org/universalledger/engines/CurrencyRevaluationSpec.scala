package org.universalledger.engines

import java.io.File
import java.nio.file.{Files, Path, Paths}

import org.scalatest.funsuite.AnyFunSuite

class CurrencyRevaluationSpec extends AnyFunSuite {
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


  private val ledgerHeader =
    "ldgrIPID,ldgrContractID,ldgrCommitmentID,ldgrLdgrlID,ldgrSourceSystemID,ldgrLedgerID,ldgrJrnlType,ldgrBookCodeID,ldgrLegalEntityID,ldgrCenterID,ldgrProjectID,ldgrProductID,ldgrNominalAccountID,ldgrAltAccountID,ldgrCurrencyCodeSourceID,ldgrCurrencyTypeCodeSourceID,ldgrCurrencyCodeTargetID,ldgrCurrencyTypeCodeTargetID,ldgrLedgerPeriod,ldgrTransAmount,ldgrUnitOfMeasure,ldgrStatisticAmount,ldgrDirVsOffsetFlg,ldgrReconcileFlg,ldgrAdjustFlg,ldgrExtensionIDAuditTrail,ldgrExtensionIDSource,ldgrExtensionIDClass,ldgrExtensionIDDates,ldgrExtensionIDCustom"

  private def writeValidFxInputs(tempDir: Path): (Path, Path) = {
    val ratesPath = tempDir.resolve("booked_fx_rates.csv")
    val rulesPath = tempDir.resolve("booked_fx_rules.csv")
    Files.write(
      ratesPath,
      Seq(
        "rate_date,currency_from,currency_to,rate,rate_source,rate_status",
        "2024-12-31,EUR,USD,1.00,TEST,ACTIVE",
        "2025-01-15,EUR,USD,1.02,TEST,ACTIVE"
      ).mkString("\n").getBytes("UTF-8")
    )
    Files.write(
      rulesPath,
      Seq(
        "rule_set_id,rule_version,legal_entity_id,source_currency,target_currency,offset_account,effective_start,effective_end,book_scale,prior_rate_date,current_rate_date,ledger_period",
        "RS1,V1,0001,EUR,USD,2100,2024-01-01,2025-12-31,2,2024-12-31,2025-01-15,2025"
      ).mkString("\n").getBytes("UTF-8")
    )
    (ratesPath, rulesPath)
  }

  test("absent, empty, and header-only opening ledgers produce an empty SJE partition") {
    withTempDirectory("fx-empty-ledger-") { tempDir =>
    val (ratesPath, rulesPath) = writeValidFxInputs(tempDir)
    val sortSpecPath = "../sort_specs/SortedJE.sortspec"
    val ledgerPaths = Seq(
      tempDir.resolve("LDGR2025_OPENING_ABSENT.csv"),
      tempDir.resolve("LDGR2025_OPENING_EMPTY.csv"),
      tempDir.resolve("LDGR2025_OPENING_HEADER.csv")
    )
    Files.createFile(ledgerPaths(1))
    Files.write(ledgerPaths(2), ledgerHeader.getBytes("UTF-8"))

    ledgerPaths.zipWithIndex.foreach { case (ledgerPath, index) =>
      val outputPath = tempDir.resolve(s"SortedJEFY25_FXR_$index.csv")
      CurrencyRevaluation.run(
        ledgerPath.toString,
        ratesPath.toString,
        rulesPath.toString,
        outputPath.toString,
        "25",
        sortSpecPath
      )
      assert(Files.exists(outputPath))
      assert(Files.size(outputPath) == 0)
    }
    }
  }

  test("headerless opening ledger rows produce balanced SJE records") {
    withTempDirectory("fx-headerless-ledger-") { tempDir =>
    val ledgerPath = tempDir.resolve("LDGR2025_OPENING.csv")
    val outputPath = tempDir.resolve("SortedJEFY25_FXR.csv")
    val (ratesPath, rulesPath) = writeValidFxInputs(tempDir)
    val ledgerRow = "1,contract,commitment,doc,src,LDGR,JV,BOOK,0001,CTR,PROJ,PROD,1010,ALT,EUR,FX,USD,FX,2025,559.47,EA,0,D,0,N,trail,source,CLASS,2025-01-01,custom"
    Files.write(ledgerPath, ledgerRow.getBytes("UTF-8"))

    CurrencyRevaluation.run(
      ledgerPath.toString,
      ratesPath.toString,
      rulesPath.toString,
      outputPath.toString,
      "25",
      "../sort_specs/SortedJE.sortspec"
    )

    val rows = scala.io.Source.fromFile(outputPath.toString, "UTF-8").getLines().toVector
    val amounts = rows.map(_.split(",", -1)(25)).map(BigDecimal(_)).sorted
    assert(rows.length == 2)
    assert(rows.forall(_.split(",", -1).length == 39))
    assert(amounts == Seq(BigDecimal("-11.19"), BigDecimal("11.19")))
    }
  }

  test("duplicate rate dates are rejected") {
    withTempDirectory("fx-dup-rate-") { tempDir =>
    val ledgerPath = tempDir.resolve("LDGR2025_OPENING.csv")
    val ratesPath = tempDir.resolve("booked_fx_rates.csv")
    val rulesPath = tempDir.resolve("booked_fx_rules.csv")
    val outputPath = tempDir.resolve("SortedJEFY25_FXR.csv")
    val sortSpecPath = "../sort_specs/SortedJE.sortspec"

    Files.write(
      ledgerPath,
      Seq(
        "ldgrIPID,ldgrContractID,ldgrCommitmentID,ldgrLdgrlID,ldgrSourceSystemID,ldgrLedgerID,ldgrJrnlType,ldgrBookCodeID,ldgrLegalEntityID,ldgrCenterID,ldgrProjectID,ldgrProductID,ldgrNominalAccountID,ldgrAltAccountID,ldgrCurrencyCodeSourceID,ldgrCurrencyTypeCodeSourceID,ldgrCurrencyCodeTargetID,ldgrCurrencyTypeCodeTargetID,ldgrLedgerPeriod,ldgrTransAmount,ldgrUnitOfMeasure,ldgrStatisticAmount,ldgrDirVsOffsetFlg,ldgrReconcileFlg,ldgrAdjustFlg,ldgrExtensionIDAuditTrail,ldgrExtensionIDSource,ldgrExtensionIDClass,ldgrExtensionIDDates,ldgrExtensionIDCustom",
        "1,contract,commitment,doc,src,LDGR,JV,BOOK,0001,CTR,PROJ,PROD,1010,ALT,EUR,FX,USD,FX,2025,559.47,EA,0,D,0,N,trail,source,CLASS,2025-01-01,custom"
      ).mkString("\n").getBytes("UTF-8")
    )

    Files.write(
      ratesPath,
      Seq(
        "rate_date,currency_from,currency_to,rate,rate_source,rate_status",
        "2024-12-31,EUR,USD,1.00,TEST,ACTIVE",
        "2025-01-15,EUR,USD,1.02,TEST,ACTIVE",
        "2025-01-15,EUR,USD,1.03,TEST,ACTIVE"
      ).mkString("\n").getBytes("UTF-8")
    )

    Files.write(
      rulesPath,
      Seq(
        "rule_set_id,rule_version,legal_entity_id,source_currency,target_currency,offset_account,effective_start,effective_end,book_scale,prior_rate_date,current_rate_date,ledger_period",
        "RS1,V1,0001,EUR,USD,2100,2024-01-01,2025-12-31,2,2024-12-31,2025-01-15,2025"
      ).mkString("\n").getBytes("UTF-8")
    )

    val ex = intercept[IllegalArgumentException] {
      CurrencyRevaluation.run(
        ledgerPath.toString,
        ratesPath.toString,
        rulesPath.toString,
        outputPath.toString,
        "25",
        sortSpecPath
      )
    }

    assert(ex.getMessage.contains("Duplicate rate"))
    }
  }
}
