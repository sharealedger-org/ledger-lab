package org.universalledger.engines

import java.nio.file.{Files, Path}

import org.scalatest.funsuite.AnyFunSuite

class CurrencyRevaluationSpec extends AnyFunSuite {

  test("duplicate rate dates are rejected") {
    val tempDir: Path = Files.createTempDirectory("fx-dup-rate-")
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
