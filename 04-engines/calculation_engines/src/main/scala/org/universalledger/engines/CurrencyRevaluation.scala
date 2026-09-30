package org.universalledger.engines

import java.io.{File, PrintWriter}
import java.nio.file.Files

import org.universalledger.foundation.va.datatypes.Transaction
import org.universalledger.foundation.va.ledger.SortEngine

import scala.collection.mutable
import scala.io.Source

object CurrencyRevaluation {
  private case class Rule(
    ruleSetId: String,
    ruleVersion: String,
    legalEntityId: String,
    sourceCurrency: String,
    targetCurrency: String,
    offsetAccount: String,
    effectiveStart: String,
    effectiveEnd: String,
    bookScale: Int,
    priorRateDate: String,
    currentRateDate: String,
    ledgerPeriod: String
  )

  private case class Rate(rateDate: String, value: BigDecimal)
  private case class SelectedRates(var prior: Option[Rate] = None, var current: Option[Rate] = None)

  private val LedgerHeader =
    "ldgrIPID,ldgrContractID,ldgrCommitmentID,ldgrLdgrlID,ldgrSourceSystemID,ldgrLedgerID,ldgrJrnlType,ldgrBookCodeID,ldgrLegalEntityID,ldgrCenterID,ldgrProjectID,ldgrProductID,ldgrNominalAccountID,ldgrAltAccountID,ldgrCurrencyCodeSourceID,ldgrCurrencyTypeCodeSourceID,ldgrCurrencyCodeTargetID,ldgrCurrencyTypeCodeTargetID,ldgrLedgerPeriod,ldgrTransAmount,ldgrUnitOfMeasure,ldgrStatisticAmount,ldgrDirVsOffsetFlg,ldgrReconcileFlg,ldgrAdjustFlg,ldgrExtensionIDAuditTrail,ldgrExtensionIDSource,ldgrExtensionIDClass,ldgrExtensionIDDates,ldgrExtensionIDCustom"

  private val RuleHeader =
    "rule_set_id,rule_version,legal_entity_id,source_currency,target_currency,offset_account,effective_start,effective_end,book_scale,prior_rate_date,current_rate_date,ledger_period"

  private val RateHeader = "rate_date,currency_from,currency_to,rate,rate_source,rate_status"

  def run(
    ledgerPath: String,
    ratesPath: String,
    rulesPath: String,
    outputPath: String,
    year: String,
    sortSpecPath: String
  ): Unit = {
    val rules = readRules(rulesPath)
    val ratesByRule = readEffectiveRates(ratesPath, rules)
    val rulesByPair = rules.groupBy(rule => (rule.legalEntityId, rule.sourceCurrency, rule.targetCurrency))
    val output = new File(outputPath)
    val outputParent = Option(output.getParentFile).getOrElse(new File("."))
    Files.createDirectories(outputParent.toPath)
    val unsorted = new File(outputParent, output.getName + ".unsorted")
    val writer = new PrintWriter(unsorted, "UTF-8")
    val ledger = Source.fromFile(ledgerPath, "UTF-8")
    var rowsRead = 0L
    var generatedGroups = 0L
    var adjustmentTotal = BigDecimal(0)

    try {
      val lines = ledger.getLines()
      if (!lines.hasNext || lines.next() != LedgerHeader)
        throw new IllegalArgumentException(s"Unexpected ledger header in $ledgerPath")

      lines.zipWithIndex.foreach { case (line, index) =>
        if (line.trim.nonEmpty) {
          rowsRead += 1
          val fields = line.split(",", -1)
          if (fields.length < 30)
            throw new IllegalArgumentException(s"Ledger row ${index + 2} has ${fields.length} fields; expected at least 30")

          val sourceCurrency = fields(14).trim
          val targetCurrency = fields(16).trim
            if (rules.exists(_.ledgerPeriod == fields(18).trim) &&
              sourceCurrency.nonEmpty && targetCurrency.nonEmpty && sourceCurrency != targetCurrency) {
            val matchingRules = rulesByPair.getOrElse(
              (fields(8).trim, sourceCurrency, targetCurrency),
              throw new IllegalArgumentException(
                s"No booked FX rule for entity=${fields(8)} currency=$sourceCurrency/$targetCurrency"
              )
            )
            val rule = matchingRules.find(r =>
              r.ledgerPeriod == fields(18).trim &&
              r.effectiveStart <= r.currentRateDate && r.currentRateDate <= r.effectiveEnd
            )
              .getOrElse(throw new IllegalArgumentException(
                s"No effective booked FX rule for entity=${fields(8)} currency=$sourceCurrency/$targetCurrency"
              ))

            val (priorRate, currentRate) = ratesByRule(rule)
            val balance = BigDecimal(fields(19).trim)
            val amount = (balance * (currentRate.value - priorRate.value))
              .setScale(rule.bookScale, BigDecimal.RoundingMode.HALF_UP)

            if (amount != 0) {
              generatedGroups += 1
              adjustmentTotal += amount
              val journalId = f"FXR-${generatedGroups}%08d"
              writer.println(serialize(transaction(fields, rule, journalId, "1", fields(12), amount, "D")))
              writer.println(serialize(transaction(fields, rule, journalId, "2", rule.offsetAccount, -amount, "O", bookCurrency = true)))
            }
          }
        }
      }
    } finally {
      ledger.close()
      writer.close()
    }

    val sortSpec = SortEngine.loadSpec(sortSpecPath)
    val sortTmp = new File(outputParent, output.getName + ".sort-tmp")
    val (rowsWritten, spills) = SortEngine.externalSort(
      Source.fromFile(unsorted, "UTF-8").getLines(),
      outputPath,
      sortSpec,
      tmpDir = sortTmp.getPath
    )
    Files.deleteIfExists(unsorted.toPath)
    if (rowsWritten != generatedGroups * 2)
      throw new IllegalStateException(s"Expected ${generatedGroups * 2} SJE rows, wrote $rowsWritten")

    println(s"CURRENCY_REVALUATION rows_read=$rowsRead generated_groups=$generatedGroups generated_rows=$rowsWritten adjustment_total=${adjustmentTotal.setScale(2, BigDecimal.RoundingMode.HALF_UP)} spills=$spills")
    val ruleSetIds = rules.map(_.ruleSetId).distinct.sorted.mkString("+")
    val ruleVersions = rules.map(_.ruleVersion).distinct.sorted.mkString("+")
    println(s"RESULT step=12 year=$year rc=0 records_read=$rowsRead records_written=$rowsWritten generated_groups=$generatedGroups balanced_groups=$generatedGroups unbalanced_groups=0 amount_delta=0.00 adjustment_total=${adjustmentTotal.setScale(2, BigDecimal.RoundingMode.HALF_UP)} spills=$spills rule_set_id=$ruleSetIds rule_version=$ruleVersions")
  }

  private def readRules(path: String): Seq[Rule] = {
    val source = Source.fromFile(path, "UTF-8")
    try {
      val lines = source.getLines()
      if (!lines.hasNext || lines.next() != RuleHeader)
        throw new IllegalArgumentException(s"Unexpected booked FX rule header in $path")
      val rules = lines.filter(_.trim.nonEmpty).map { line =>
        val f = line.split(",", -1).map(_.trim)
        if (f.length != 12) throw new IllegalArgumentException(s"Invalid booked FX rule row: $line")
        Rule(f(0), f(1), f(2), f(3), f(4), f(5), f(6), f(7), f(8).toInt, f(9), f(10), f(11))
      }.toVector
      if (rules.isEmpty) throw new IllegalArgumentException("Booked FX rules are empty")
      rules
    } finally source.close()
  }

  private def readEffectiveRates(path: String, rules: Seq[Rule]): Map[Rule, (Rate, Rate)] = {
    val selectedRates = Array.fill(rules.size)(SelectedRates())
    val rateDatesByPair = scala.collection.mutable.Map.empty[(String, String), scala.collection.mutable.Set[String]]
    val ruleIndexesByPair = rules.zipWithIndex.groupBy {
      case (rule, _) => (rule.sourceCurrency, rule.targetCurrency)
    }
    val source = Source.fromFile(path, "UTF-8")
    try {
      val lines = source.getLines()
      if (!lines.hasNext || lines.next() != RateHeader)
        throw new IllegalArgumentException(s"Unexpected booked FX rate header in $path")
      lines.filter(_.trim.nonEmpty).foreach { line =>
        val f = line.split(",", -1).map(_.trim)
        if (f.length != 6) throw new IllegalArgumentException(s"Invalid booked FX rate row: $line")
        val pair = (f(1), f(2))
        val rateDate = f(0)
        val seenDates = rateDatesByPair.getOrElseUpdate(pair, scala.collection.mutable.Set.empty[String])
        if (seenDates.contains(rateDate)) {
          throw new IllegalArgumentException(s"Duplicate rate for ${pair._1}/${pair._2} on $rateDate")
        }
        seenDates += rateDate
        val rate = Rate(rateDate, BigDecimal(f(3)))
        ruleIndexesByPair.get(pair).foreach(_.foreach { case (rule, index) =>
          if (rate.rateDate <= rule.priorRateDate &&
              selectedRates(index).prior.forall(_.rateDate < rate.rateDate))
            selectedRates(index).prior = Some(rate)
          if (rate.rateDate <= rule.currentRateDate &&
              selectedRates(index).current.forall(_.rateDate < rate.rateDate))
            selectedRates(index).current = Some(rate)
        })
      }
    } finally source.close()
    rules.zipWithIndex.map { case (rule, index) =>
      val priorRate = selectedRates(index).prior.getOrElse(throw new IllegalArgumentException(
        s"No rate for ${rule.sourceCurrency}/${rule.targetCurrency} on or before ${rule.priorRateDate}"
      ))
      val currentRate = selectedRates(index).current.getOrElse(throw new IllegalArgumentException(
        s"No rate for ${rule.sourceCurrency}/${rule.targetCurrency} on or before ${rule.currentRateDate}"
      ))
      rule -> (priorRate, currentRate)
    }.toMap
  }

  private def transaction(
    ledger: Array[String],
    rule: Rule,
    journalId: String,
    lineId: String,
    account: String,
    amount: BigDecimal,
    direction: String,
    bookCurrency: Boolean = false
  ): Transaction = {
    val sourceCurrency = if (bookCurrency) ledger(16) else ledger(14)
    val sourceCurrencyType = if (bookCurrency) ledger(17) else ledger(15)
    Transaction(
      transIPID = ledger(0),
      transContractID = ledger(1),
      transCommitmentID = ledger(2),
      transJrnlID = journalId,
      transJrnlLineID = lineId,
      transBusinessEventCode = "CURRENCY_REVALUATION",
      transSourceSystemID = "FX_REVALUATION",
      transOriginalDocID = ledger(3),
      transJrnlDescript = s"Booked FX revaluation ${ledger(14)}/${ledger(16)}",
      transLedgerID = ledger(5),
      transJrnlType = ledger(6),
      transBookCodeID = ledger(7),
      transLegalEntityID = ledger(8),
      transCenterID = ledger(9),
      transProjectID = ledger(10),
      transProductID = ledger(11),
      transNominalAccountID = account,
      transAltAccountID = ledger(13),
      transCurrencyCodeSourceID = sourceCurrency,
      transCurrencyTypeCodeSourceID = sourceCurrencyType,
      transCurrencyCodeTargetID = ledger(16),
      transCurrencyTypeCodeTargetID = ledger(17),
      transFiscalPeriod = ledger(18),
      transAcctDate = rule.currentRateDate,
      transTransDate = rule.currentRateDate,
      transTransAmount = amount,
      transDirVsOffsetFlg = direction,
      transAdjustFlg = "Y",
      tranRuleSetID = rule.ruleSetId,
      transRuleID = s"${rule.ruleSetId}-${rule.ruleVersion}",
      transExtensionIDAuditTrail = "CURRENCY_REVALUATION",
      transExtensionIDSource = ledger(4),
      transExtensionIDClass = "BOOKED_FX",
      transExtensionIDDates = s"${rule.priorRateDate}/${rule.currentRateDate}"
    )
  }

  private def serialize(t: Transaction): String = Seq(
    t.transIPID, t.transContractID, t.transCommitmentID, t.transJrnlID,
    t.transJrnlLineID, t.transBusinessEventCode, t.transSourceSystemID,
    t.transOriginalDocID, t.transJrnlDescript, t.transLedgerID,
    t.transJrnlType, t.transBookCodeID, t.transLegalEntityID, t.transCenterID,
    t.transProjectID, t.transProductID, t.transNominalAccountID,
    t.transAltAccountID, t.transCurrencyCodeSourceID,
    t.transCurrencyTypeCodeSourceID, t.transCurrencyCodeTargetID,
    t.transCurrencyTypeCodeTargetID, t.transFiscalPeriod, t.transAcctDate,
    t.transTransDate, t.transTransAmount.bigDecimal.toPlainString,
    t.transUnitOfMeasure, t.transUnitPrice.bigDecimal.toPlainString,
    t.transStatisticAmount.bigDecimal.toPlainString, t.tranRuleSetID,
    t.transRuleID, t.transDirVsOffsetFlg, t.transReconcileFlg,
    t.transAdjustFlg, t.transExtensionIDAuditTrail, t.transExtensionIDSource,
    t.transExtensionIDClass, t.transExtensionIDDates, t.transExtensionIDCustom
  ).mkString(",")
}