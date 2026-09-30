package org.universalledger.foundation.va.ledger

/*
 * (c) Copyright IBM Corporation. 2018
 * SPDX-License-Identifier: Apache-2.0
 * By Kip Twitchell
 */

import java.io.{File, PrintWriter}
import java.util.Calendar

import org.universalledger.foundation.va.datatypes._

import scala.collection.mutable
import scala.io.Source

//***************************************************************************************************************
//
//  Program:  forecastingBudgeting — Forecasting and Budgeting
//
//  FSP Pattern:  Forecasting & Budgeting
//
//  WHAT IT DOES:
//  Creates a budget/forecast version of the balance file alongside the actuals.
//  For each balance in the actuals, generates a budget balance using a projection rule.
//  Produces a variance report: actual vs. budget by agency, fund, program, and account.
//
//  KEY INSIGHT (from the monograph):
//  Forecasting works at the BALANCE level, not the transaction level.  This is why
//  keeping the balance file is essential — you can project next year's budgets from
//  this year's actuals in a single pass, without going back to transactions.
//  Change the growth factor and re-run: instant new forecast.
//
//  BUDGET RULES FILE (BudgetRules.csv — optional):
//  Schema: nominalAccountPrefix, growthFactor
//    e.g.  EXP, 1.03   (inflate all expense accounts by 3%)
//          REV, 0.98   (deflate all revenue accounts by 2%)
//  If absent: default rule is 1.03 (3% growth) for all accounts.
//
//  The budget balance file uses bookCodeID = "BUDGET" to distinguish from actuals.
//
//  INPUTS:
//    {outPath}/LDGR{year}.csv      — actuals balance file (base year)
//    {outPath}/BudgetRules.csv     — projection rule table (optional)
//
//  OUTPUTS:
//    {outPath}/BUDGET_LDGR{year}.csv   — budget balance file (same structure as LDGR)
//    {outPath}/VARIANCE_{year}.csv     — actual vs. budget variance report
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Initial implementation (Bob AI)
//***************************************************************************************************************

object forecastingBudgeting {

  def apply(fileOutLocation: String): Unit = {

    println("*" * 100)
    println("                  Forecasting and Budgeting Module")
    println("              FSP Pattern: Forecasting & Budgeting")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    if (fileOutLocation.isEmpty) {
      println("Invalid output file location. Program abort.")
      sys.exit(1)
    }

    val dataPath      = fileOutLocation
    val fileDelimiter = ","

    //─────────────────────────────────────────────────────────────────────────
    // Load budget rules: Map[nominalPrefix -> growthFactor]
    //─────────────────────────────────────────────────────────────────────────
    val budgetRules = mutable.Map[String, BigDecimal]()
    val rulesFile   = dataPath + "BudgetRules.csv"
    try {
      val source = Source.fromFile(rulesFile)
      try {
        val rLines = source.getLines().filter(line => line.trim.nonEmpty && !line.startsWith("#"))
        val expectedHeader = Seq("nominalAccountPrefix", "growthFactor")
        if (!rLines.hasNext) throw new IllegalArgumentException(s"Empty budget rules file: $rulesFile")
        val header = rLines.next().split(fileDelimiter, -1).map(_.trim).toSeq
        if (header != expectedHeader)
          throw new IllegalArgumentException(s"Unexpected budget rules header in $rulesFile")
        for (line <- rLines) {
          val e = line.split(fileDelimiter, -1).map(_.trim)
          if (e.length != expectedHeader.length)
            throw new IllegalArgumentException(s"Invalid budget rule row: $line")
          budgetRules(e(0)) = BigDecimal(e(1))
        }
      } finally {
        source.close()
      }
      println(s"Budget rules loaded: ${budgetRules.size} rules from $rulesFile")
      budgetRules.foreach { case (prefix, factor) =>
        println(s"  $prefix → factor $factor  (${((factor - 1) * 100).setScale(1)}%)")
      }
    } catch {
      case _: java.io.FileNotFoundException =>
        budgetRules("") = BigDecimal("1.03") // default: 3% growth on all accounts
        println(s"No BudgetRules.csv — applying default: all accounts × 1.03 (3% growth)")
    }

    // Find the applicable growth factor for a given nominal account
    def growthFactor(nominalAccount: String): BigDecimal = {
      // Match longest prefix first
      budgetRules.keys.toSeq
        .filter(prefix => nominalAccount.startsWith(prefix))
        .sortBy(_.length)(Ordering[Int].reverse)
        .headOption
        .map(budgetRules)
        .getOrElse(budgetRules.getOrElse("", BigDecimal("1.03")))
    }

    val ldgrFiles = getListOfFiles(dataPath, "LDGR").sortWith(_.getName < _.getName)
    if (ldgrFiles.isEmpty) {
      println(s"No LDGR files found in $dataPath — nothing to forecast.")
      return
    }

    var totalFilesProcessed = 0
    var totalBudgetRows     = 0
    var totalVarianceRows   = 0

    for (ldgrFile <- ldgrFiles) {
      totalFilesProcessed += 1
      val yearStr = ldgrFile.getName.stripPrefix("LDGR").stripSuffix(".csv")
      println(s"\nForecasting: ${ldgrFile.getName}  (year=$yearStr)")

      val headerLine = Source.fromFile(ldgrFile).getLines().next()

      //───────────────────────────────────────────────────────────────────────
      // Single pass: generate budget rows and accumulate variance data
      // Budget key: same as actuals key but bookCodeID replaced with "BUDGET"
      //───────────────────────────────────────────────────────────────────────
      val budgetFile   = dataPath + "BUDGET_LDGR" + yearStr + ".csv"
      val varianceFile = dataPath + "VARIANCE_" + yearStr + ".csv"
      val budgetOut    = new PrintWriter(new File(budgetFile))
      val varianceOut  = new PrintWriter(new File(varianceFile))

      budgetOut.write(headerLine + "\n")
      varianceOut.write("legalEntityID,centerID,projectID,nominalAccountID,ledgerPeriod," +
        "actualAmount,budgetAmount,variance,variancePct\n")

      var fileBudgetRows   = 0
      var fileVarianceRows = 0

      val lines = Source.fromFile(ldgrFile).getLines().drop(1)
      for (line <- lines if line.trim.nonEmpty) {
        val e      = line.split(fileDelimiter, -1).map(_.trim)
        val instID = e(0)

        // Skip contra rows
        if (instID.nonEmpty) {
          val actualAmt      = BigDecimal(e(19))
          val nominalAccount = e(12)
          val factor         = growthFactor(nominalAccount)
          val budgetAmt      = (actualAmt * factor).setScale(2, BigDecimal.RoundingMode.HALF_UP)

          // Write budget row: identical to actuals row but with BUDGET book code and budget amount
          val budgetFields = e.clone()
          budgetFields(7)  = "BUDGET"   // ldgrBookCodeID
          budgetFields(19) = budgetAmt.toString
          budgetOut.write(budgetFields.mkString(fileDelimiter) + "\n")
          fileBudgetRows += 1

          // Write variance row
          val variance    = actualAmt - budgetAmt
          val variancePct = if (budgetAmt != 0)
            ((variance / budgetAmt.abs) * 100).setScale(1, BigDecimal.RoundingMode.HALF_UP)
          else BigDecimal(0)

          varianceOut.write(
            s"${e(8)},${e(9)},${e(10)},${e(12)},${e(18)}," +
            s"$actualAmt,$budgetAmt,$variance,$variancePct%\n")
          fileVarianceRows += 1
        }
      }

      budgetOut.close()
      varianceOut.close()
      totalBudgetRows   += fileBudgetRows
      totalVarianceRows += fileVarianceRows

      println(s"  Budget rows written:   $fileBudgetRows → $budgetFile")
      println(s"  Variance rows written: $fileVarianceRows → $varianceFile")

      //───────────────────────────────────────────────────────────────────────
      // Quick summary: top 5 over-budget and top 5 under-budget agencies
      //───────────────────────────────────────────────────────────────────────
      case class VarianceRow(legalEntity: String, nominalAccount: String,
                             actual: BigDecimal, budget: BigDecimal) {
        def variance = actual - budget
      }
      val vRows = mutable.ArrayBuffer[VarianceRow]()
      Source.fromFile(varianceFile).getLines().drop(1).foreach { line =>
        val v = line.split(fileDelimiter, -1).map(_.trim)
        if (v.length >= 6)
          vRows += VarianceRow(v(0), v(3), BigDecimal(v(5)), BigDecimal(v(6)))
      }

      val byAgency = vRows.groupBy(_.legalEntity)
        .mapValues(rows => rows.map(_.variance).sum)
        .toSeq.sortBy(_._2)

      println("-" * 60)
      println(s"  Variance summary by agency — year $yearStr:")
      if (byAgency.nonEmpty) {
        println("  Most under-budget (actual < budget):")
        byAgency.take(3).foreach { case (agency, v) =>
          println(f"    Agency $agency: variance = $v%.2f")
        }
        println("  Most over-budget (actual > budget):")
        byAgency.takeRight(3).reverse.foreach { case (agency, v) =>
          println(f"    Agency $agency: variance = $v%.2f")
        }
      }
      println("-" * 60)
    }

    println("*" * 100)
    println("Forecasting and Budgeting — Control Totals")
    println(s"  LDGR files processed:    $totalFilesProcessed")
    println(s"  Budget rows written:     $totalBudgetRows")
    println(s"  Variance rows written:   $totalVarianceRows")
    println(s"  Process End Time:        ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}
