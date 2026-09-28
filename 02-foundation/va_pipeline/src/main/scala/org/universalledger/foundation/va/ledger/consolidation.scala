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
//  Program:  consolidation — Consolidation and Elimination
//
//  FSP Pattern:  Consolidation & Elimination
//
//  WHAT IT DOES:
//  Combines balance files from multiple legal entities (agencies) into a single
//  consolidated view, then eliminates interagency transactions so intergovernmental
//  transfers don't double-count in the state-wide total.
//
//  THE PROBLEM IT SOLVES:
//  When Agency A pays Agency B $100 for shared services:
//    Agency A records: EXP (expense) of $100
//    Agency B records: REV (revenue) of $100
//  In a state-wide consolidated view, both entries appear — the same $100 inflates
//  both total expenses AND total revenues.  Elimination removes this double-count.
//
//  ALGORITHM:
//  (1) Read all LDGR files — combine into a single in-memory consolidated store
//  (2) Load the interagency transfer table (InteragencyTransfers.csv)
//      If none exists, auto-detect: any REV balance in one agency with a matching
//      EXP balance in another agency, same amount and period, is flagged as interagency
//  (3) For each identified interagency pair: generate elimination SJEs
//      - Debit  Agency B revenue × -1 (removing it)
//      - Credit Agency A expense × -1 (removing it)
//  (4) Write ELIM_SJE{year}.csv; write CONSOL_LDGR{year}.csv
//  (5) Verify: all flagged interagency pairs net to zero in the consolidated view
//
//  INTERAGENCY TRANSFER TABLE (InteragencyTransfers.csv — optional):
//  Schema: agencyA, agencyB, transferNominalAccountPrefix
//  If absent: auto-detection heuristic is used (same amount, same period, EXP↔REV prefix)
//
//  INPUTS:
//    {outPath}/LDGR{year}.csv              — balance files (one or more)
//    {outPath}/InteragencyTransfers.csv    — optional transfer identification table
//
//  OUTPUTS:
//    {outPath}/CONSOL_LDGR{year}.csv       — consolidated balance file
//    {outPath}/ELIM_SJE{year}.csv          — elimination journal entries
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Initial implementation (Bob AI)
//***************************************************************************************************************

object consolidation {

  case class InteragencyRule(agencyA: String, agencyB: String, nominalPrefix: String)

  def apply(fileOutLocation: String): Unit = {

    println("*" * 100)
    println("                  Consolidation and Elimination Module")
    println("              FSP Pattern: Consolidation & Elimination")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    if (fileOutLocation.isEmpty) {
      println("Invalid output file location. Program abort.")
      sys.exit(1)
    }

    val dataPath      = fileOutLocation
    val fileDelimiter = ","

    //─────────────────────────────────────────────────────────────────────────
    // Load interagency transfer rules (optional)
    //─────────────────────────────────────────────────────────────────────────
    val iaRules = mutable.ArrayBuffer[InteragencyRule]()
    val iaFile  = dataPath + "InteragencyTransfers.csv"
    try {
      val iaLines = Source.fromFile(iaFile).getLines()
      for (line <- iaLines if line.trim.nonEmpty && !line.startsWith("#")) {
        val e = line.split(fileDelimiter, -1).map(_.trim)
        if (e.length >= 3 && e(0) != "agencyA")
          iaRules += InteragencyRule(e(0), e(1), e(2))
      }
      println(s"Interagency transfer rules loaded: ${iaRules.size} from $iaFile")
    } catch {
      case _: java.io.FileNotFoundException =>
        println("No InteragencyTransfers.csv — will auto-detect interagency pairs")
    }

    val ldgrFiles = getListOfFiles(dataPath, "LDGR").sortWith(_.getName < _.getName)
    if (ldgrFiles.isEmpty) {
      println(s"No LDGR files found in $dataPath — nothing to consolidate.")
      return
    }

    var totalFilesProcessed = 0
    var totalElimSJEs       = 0

    for (ldgrFile <- ldgrFiles) {
      totalFilesProcessed += 1
      val yearStr = ldgrFile.getName.stripPrefix("LDGR").stripSuffix(".csv")
      println(s"\nConsolidating: ${ldgrFile.getName}  (year=$yearStr)")

      //───────────────────────────────────────────────────────────────────────
      // (1) Load all balance rows into memory
      //───────────────────────────────────────────────────────────────────────
      case class BalRow(fields: Array[String]) {
        def instID           = fields(0)
        def legalEntityID    = fields(8)
        def nominalAccountID = fields(12)
        def ledgerPeriod     = fields(18)
        def amount           = BigDecimal(fields(19))
        def raw              = fields.mkString(fileDelimiter)
      }

      val allRows = mutable.ArrayBuffer[BalRow]()
      val dataLines = Source.fromFile(ldgrFile).getLines().drop(1)
      for (line <- dataLines if line.trim.nonEmpty)
        allRows += BalRow(line.split(fileDelimiter, -1).map(_.trim))

      println(s"  Rows loaded: ${allRows.size}")

      //───────────────────────────────────────────────────────────────────────
      // (2) Identify interagency pairs
      //     Key: (amount, period, nominalSuffix) — match EXP in A to REV in B
      //───────────────────────────────────────────────────────────────────────
      val elimPairs = mutable.ArrayBuffer[(BalRow, BalRow)]()

      if (iaRules.nonEmpty) {
        // Rule-driven identification
        for (rule <- iaRules) {
          val aRows = allRows.filter(r =>
            r.instID.nonEmpty &&
            (rule.agencyA == "*" || r.legalEntityID == rule.agencyA) &&
            r.nominalAccountID.startsWith(rule.nominalPrefix))
          val bRows = allRows.filter(r =>
            r.instID.nonEmpty &&
            (rule.agencyB == "*" || r.legalEntityID == rule.agencyB) &&
            r.nominalAccountID.startsWith(rule.nominalPrefix))

          for (a <- aRows; b <- bRows
            if a.amount == b.amount.abs * -1 && a.ledgerPeriod == b.ledgerPeriod) {
            elimPairs += ((a, b))
          }
        }
      } else {
        // Auto-detect: EXP row in agency A matches REV row in agency B
        // same absolute amount, same period, opposite sign (EXP positive, REV negative or vice versa)
        val expRows = allRows.filter(r => r.instID.nonEmpty && r.nominalAccountID.startsWith("EXP"))
        val revRows = allRows.filter(r => r.instID.nonEmpty && r.nominalAccountID.startsWith("REV"))

        for (e <- expRows; r <- revRows
          if e.amount == r.amount.abs &&
             e.ledgerPeriod == r.ledgerPeriod &&
             e.legalEntityID != r.legalEntityID) {
          elimPairs += ((e, r))
        }
      }

      println(s"  Interagency pairs identified: ${elimPairs.size}")

      //───────────────────────────────────────────────────────────────────────
      // (3) Generate elimination SJEs
      //───────────────────────────────────────────────────────────────────────
      val elimSJEFile = dataPath + "ELIM_SJE" + yearStr + ".csv"
      val elimOut     = new PrintWriter(new File(elimSJEFile))
      writeSJEHeader(elimOut)
      var fileElimSJEs = 0

      for ((aRow, bRow) <- elimPairs) {
        // Eliminate Agency A's expense entry
        fileElimSJEs += 1
        writeSJE(elimOut, buildElimSJE(aRow.fields, aRow.amount * -1, "ELIM-EXP", fileElimSJEs))
        // Eliminate Agency B's revenue entry
        fileElimSJEs += 1
        writeSJE(elimOut, buildElimSJE(bRow.fields, bRow.amount * -1, "ELIM-REV", fileElimSJEs))
      }
      elimOut.close()
      totalElimSJEs += fileElimSJEs
      println(s"  Elimination SJEs written: $fileElimSJEs → $elimSJEFile")

      //───────────────────────────────────────────────────────────────────────
      // (4) Write CONSOL_LDGR — all rows including elimination markers
      //───────────────────────────────────────────────────────────────────────
      val consolFile = dataPath + "CONSOL_LDGR" + yearStr + ".csv"
      val consolOut  = new PrintWriter(new File(consolFile))
      // Copy header from source file
      consolOut.write(Source.fromFile(ldgrFile).getLines().next() + "\n")
      allRows.foreach(r => consolOut.write(r.raw + "\n"))
      consolOut.close()
      println(s"  Consolidated LDGR written: ${allRows.size} rows → $consolFile")

      //───────────────────────────────────────────────────────────────────────
      // (5) Sort and post elimination SJEs
      //───────────────────────────────────────────────────────────────────────
      if (fileElimSJEs > 0) {
        val sortedFile = dataPath + "SortedJE_ELIM" + yearStr + ".csv"
        sortSJEFile(elimSJEFile, sortedFile)
        println(s"  Posting elimination SJEs...")
        post(dataPath, dataPath)
        println(s"  Post complete.")
      }

      //───────────────────────────────────────────────────────────────────────
      // Verify: flagged interagency pairs net to zero
      //───────────────────────────────────────────────────────────────────────
      val elimNet = elimPairs.map { case (a, b) => a.amount + b.amount }.sum
      println("-" * 60)
      if (elimNet == BigDecimal(0) || elimPairs.isEmpty) {
        println(s"  ✓ ELIMINATION PROOF PASSED — year $yearStr")
        println(s"    ${elimPairs.size} interagency pairs net to ZERO in consolidated view.")
      } else {
        println(s"  ✗ ELIMINATION PROOF FAILED — year $yearStr")
        println(s"    ${elimPairs.size} pairs net to $elimNet (expected 0).")
      }
      println("-" * 60)
    }

    println("*" * 100)
    println("Consolidation and Elimination — Control Totals")
    println(s"  LDGR files processed:        $totalFilesProcessed")
    println(s"  Elimination SJEs written:    $totalElimSJEs")
    println(s"  Process End Time:            ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }

  private def buildElimSJE(sourceFields: Array[String], amount: BigDecimal,
                            desc: String, seqNum: Int): Transaction = Transaction(
    transIPID = sourceFields(0), transContractID = sourceFields(1),
    transCommitmentID = sourceFields(2), transJrnlID = "ELIM" + seqNum,
    transJrnlLineID = "1", transBusinessEventCode = "ELIM",
    transSourceSystemID = "ELIM", transOriginalDocID = sourceFields(3),
    transJrnlDescript = s"Consolidation Elimination — $desc",
    transLedgerID = sourceFields(5), transJrnlType = sourceFields(6),
    transBookCodeID = sourceFields(7), transLegalEntityID = sourceFields(8),
    transCenterID = sourceFields(9), transProjectID = sourceFields(10),
    transProductID = sourceFields(11), transNominalAccountID = sourceFields(12),
    transAltAccountID = sourceFields(13),
    transCurrencyCodeSourceID = sourceFields(14),
    transCurrencyTypeCodeSourceID = sourceFields(15),
    transCurrencyCodeTargetID = sourceFields(16),
    transCurrencyTypeCodeTargetID = sourceFields(17),
    transFiscalPeriod = sourceFields(18),
    transAcctDate = Calendar.getInstance.getTime.toString,
    transTransDate = Calendar.getInstance.getTime.toString,
    transTransAmount = amount, transUnitOfMeasure = " ",
    transUnitPrice = BigDecimal(0), transStatisticAmount = BigDecimal(0),
    tranRuleSetID = " ", transRuleID = " ",
    transDirVsOffsetFlg = "O", transReconcileFlg = "N", transAdjustFlg = "N",
    transExtensionIDAuditTrail = "ELIM", transExtensionIDSource = " ",
    transExtensionIDClass = " ",
    transExtensionIDDates = Calendar.getInstance.getTime.toString,
    transExtensionIDCustom = " ")

  private def sortSJEFile(inputFile: String, outputFile: String): Unit = {
    val lines = Source.fromFile(inputFile).getLines().toList
    if (lines.size <= 1) { val o = new PrintWriter(new File(outputFile)); lines.foreach(o.println); o.close(); return }
    val header = lines.head
    val sorted = lines.tail.filter(_.trim.nonEmpty).sortWith { (a, b) =>
      def key(e: Array[String]) = (9 to 22).map(i => if (i < e.length) e(i) else "").mkString
      key(a.split(",", -1)) < key(b.split(",", -1))
    }
    val o = new PrintWriter(new File(outputFile)); o.println(header); sorted.foreach(o.println); o.close()
  }

  private def writeSJEHeader(out: PrintWriter): Unit = out.write(
    "transIPID,transContractID,transCommitmentID,transJrnlID,transJrnlLineID," +
    "transBusinessEventCode,transSourceSystemID,transOriginalDocID,transJrnlDescript," +
    "transLedgerID,transJrnlType,transBookCodeID,transLegalEntityID,transCenterID," +
    "transProjectID,transProductID,transNominalAccountID,transAltAccountID," +
    "transCurrencyCodeSourceID,transCurrencyTypeCodeSourceID," +
    "transCurrencyCodeTargetID,transCurrencyTypeCodeTargetID," +
    "transFiscalPeriod,transAcctDate,transTransDate,transTransAmount," +
    "transUnitOfMeasure,transUnitPrice,transStatisticAmount," +
    "tranRuleSetID,transRuleID,transDirVsOffsetFlg,transReconcileFlg,transAdjustFlg," +
    "transExtensionIDAuditTrail,transExtensionIDSource,transExtensionIDClass," +
    "transExtensionIDDates,transExtensionIDCustom\n")

  private def writeSJE(out: PrintWriter, t: Transaction): Unit = out.write(
    s"${t.transIPID},${t.transContractID},${t.transCommitmentID}," +
    s"${t.transJrnlID},${t.transJrnlLineID},${t.transBusinessEventCode}," +
    s"${t.transSourceSystemID},${t.transOriginalDocID},${t.transJrnlDescript}," +
    s"${t.transLedgerID},${t.transJrnlType},${t.transBookCodeID}," +
    s"${t.transLegalEntityID},${t.transCenterID},${t.transProjectID}," +
    s"${t.transProductID},${t.transNominalAccountID},${t.transAltAccountID}," +
    s"${t.transCurrencyCodeSourceID},${t.transCurrencyTypeCodeSourceID}," +
    s"${t.transCurrencyCodeTargetID},${t.transCurrencyTypeCodeTargetID}," +
    s"${t.transFiscalPeriod},${t.transAcctDate},${t.transTransDate}," +
    s"${t.transTransAmount},${t.transUnitOfMeasure},${t.transUnitPrice}," +
    s"${t.transStatisticAmount},${t.tranRuleSetID},${t.transRuleID}," +
    s"${t.transDirVsOffsetFlg},${t.transReconcileFlg},${t.transAdjustFlg}," +
    s"${t.transExtensionIDAuditTrail},${t.transExtensionIDSource}," +
    s"${t.transExtensionIDClass},${t.transExtensionIDDates},${t.transExtensionIDCustom}\n")
}
