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
//  Program:  financialAllocation — Financial Allocation
//
//  FSP Pattern:  Financial Allocation
//
//  WHAT IT DOES:
//  Takes source balances (shared overhead costs) and distributes them across receiving
//  cost objects using a weighted allocation rule.  Each allocation produces:
//    - Debits to each receiver  (receiver's share of the source amount)
//    - A credit to the source   (zeroing the source balance)
//  The total financial position is unchanged — only the distribution changes.
//
//  This is the reverse of aggregation: aggregation collapses detail into summary;
//  allocation explodes a summary into detail.
//
//  VA DATA CONTEXT:
//  Agency-level overhead coded to a single program is distributed across all programs
//  in that agency, weighted by each program's share of total agency expenditure.
//
//  ALLOCATION RULES FILE (AllocationRules.csv):
//  Defines which balances are sources and how to find the receiver pool.
//  Schema: sourceNominalAccount, sourceAgency, driverNominalAccount, receiverAgency
//    - sourceNominalAccount:  nominal account prefix to treat as overhead source (e.g. "EXP999")
//    - sourceAgency:          legal entity ID of the source agency
//    - driverNominalAccount:  nominal account to use as the weighting driver (e.g. "EXP" — all expense)
//    - receiverAgency:        legal entity to receive allocation (same as source for intra-agency)
//
//  If no AllocationRules.csv is found, a default rule is applied:
//    Source: any EXP balance in program "999" (conventional overhead program code)
//    Driver and receiver: all EXP balances in the same agency
//
//  INPUTS:
//    {outPath}/LDGR{year}.csv           — posted balance file
//    {outPath}/AllocationRules.csv      — allocation rule table (optional — default applied if absent)
//
//  OUTPUTS:
//    {outPath}/ALLOC_SJE{year}.csv      — allocation journal entries (audit trail)
//    {outPath}/LDGR{year}.csv           — updated with allocation results
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Initial implementation (Bob AI)
//***************************************************************************************************************

object financialAllocation {

  case class AllocationRule(
    sourceNominalAccount: String,
    sourceAgency:         String,
    driverNominalAccount: String,
    receiverAgency:       String
  )

  def apply(fileOutLocation: String): Unit = {

    println("*" * 100)
    println("                    Financial Allocation Module")
    println("                FSP Pattern: Financial Allocation")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    if (fileOutLocation.isEmpty) {
      println("Invalid output file location. Program abort.")
      sys.exit(1)
    }

    val dataPath      = fileOutLocation
    val fileDelimiter = ","

    //─────────────────────────────────────────────────────────────────────────
    // Load allocation rules
    //─────────────────────────────────────────────────────────────────────────
    val rules = mutable.ArrayBuffer[AllocationRule]()
    val rulesFile = dataPath + "AllocationRules.csv"
    try {
      val rLines = Source.fromFile(rulesFile).getLines()
      for (line <- rLines if line.trim.nonEmpty && !line.startsWith("#")) {
        val e = line.split(fileDelimiter, -1).map(_.trim)
        if (e.length >= 4 && e(0) != "sourceNominalAccount")
          rules += AllocationRule(e(0), e(1), e(2), e(3))
      }
      println(s"Allocation rules loaded: ${rules.size} rules from $rulesFile")
    } catch {
      case _: java.io.FileNotFoundException =>
        // Default rule: any nominal account starting with "EXP" in project "999"
        // is overhead; distribute across all EXP balances in the same agency
        rules += AllocationRule("EXP999", "*", "EXP", "*")
        println(s"No AllocationRules.csv found — applying default rule: EXP999 → all EXP in same agency")
    }

    var totalFilesProcessed = 0
    var totalAllocSJEs      = 0

    val ldgrFiles = getListOfFiles(dataPath, "LDGR").sortWith(_.getName < _.getName)
    if (ldgrFiles.isEmpty) {
      println(s"No LDGR files found in $dataPath — nothing to allocate.")
      printControlTotals(totalFilesProcessed, totalAllocSJEs)
      return
    }

    for (ldgrFile <- ldgrFiles) {
      totalFilesProcessed += 1
      val yearStr = ldgrFile.getName.stripPrefix("LDGR").stripSuffix(".csv")
      println(s"\nAllocating: ${ldgrFile.getName}  (year=$yearStr)")

      //───────────────────────────────────────────────────────────────────────
      // Load the entire LDGR file into memory (keyed by row for update)
      // We need random access by key to find source and receiver balances
      //───────────────────────────────────────────────────────────────────────
      case class LdgrRow(fields: Array[String]) {
        def instID           = fields(0)
        def legalEntityID    = fields(8)
        def projectID        = fields(10)
        def nominalAccountID = fields(12)
        def amount           = BigDecimal(fields(19))
        def period           = fields(18)
        // Build balance key (same as post.scala)
        def balKey = (5 to 18).map(i => if (i < fields.length) fields(i) else "").mkString("|")
      }

      val allRows    = mutable.ArrayBuffer[LdgrRow]()
      val headerLine = Source.fromFile(ldgrFile).getLines().next()
      val dataLines  = Source.fromFile(ldgrFile).getLines().drop(1)
      for (line <- dataLines if line.trim.nonEmpty)
        allRows += LdgrRow(line.split(fileDelimiter, -1).map(_.trim))

      println(s"  Rows loaded: ${allRows.size}")

      //───────────────────────────────────────────────────────────────────────
      // Open ALLOC_SJE output
      //───────────────────────────────────────────────────────────────────────
      val allocSJEFile = dataPath + "ALLOC_SJE" + yearStr + ".csv"
      val allocOut     = new PrintWriter(new File(allocSJEFile))
      writeSJEHeader(allocOut)
      var fileAllocSJEs = 0

      //───────────────────────────────────────────────────────────────────────
      // Apply each allocation rule
      //───────────────────────────────────────────────────────────────────────
      for (rule <- rules) {
        // Find source rows matching this rule
        val sourceRows = allRows.filter { r =>
          r.instID.nonEmpty &&
          r.nominalAccountID.startsWith(rule.sourceNominalAccount.replace("*","")) &&
          (rule.sourceAgency == "*" || r.legalEntityID == rule.sourceAgency)
        }

        if (sourceRows.isEmpty) {
          println(s"  Rule ${rule.sourceNominalAccount}/${rule.sourceAgency}: no source balances found — skipping")
        } else {
          // Find driver rows (used to calculate receiver weights)
          val driverRows = allRows.filter { r =>
            r.instID.nonEmpty &&
            r.nominalAccountID.startsWith(rule.driverNominalAccount.replace("*","")) &&
            (rule.receiverAgency == "*" || r.legalEntityID == rule.receiverAgency) &&
            !sourceRows.contains(r)  // exclude source rows from driver pool
          }

          val totalDriverAmt = driverRows.map(_.amount.abs).sum
          if (totalDriverAmt == 0) {
            println(s"  Rule ${rule.sourceNominalAccount}: driver total is zero — cannot weight, skipping")
          } else {
            println(s"  Rule: source=${rule.sourceNominalAccount} sourceRows=${sourceRows.size} " +
              s"driverRows=${driverRows.size} totalDriver=$totalDriverAmt")

            for (srcRow <- sourceRows) {
              val srcAmt = srcRow.amount
              if (srcAmt != BigDecimal(0)) {
                // Credit SJE: zero out the source balance
                fileAllocSJEs += 1
                val creditSJE = buildAllocSJE(srcRow.fields, srcRow.nominalAccountID,
                  srcAmt * -1, "CREDIT", fileAllocSJEs)
                writeSJE(allocOut, creditSJE)

                // Debit SJEs: one per receiver, weighted
                for (rcvRow <- driverRows) {
                  val weight   = rcvRow.amount.abs / totalDriverAmt
                  val allocAmt = (srcAmt * weight).setScale(2, BigDecimal.RoundingMode.HALF_UP)
                  if (allocAmt != BigDecimal(0)) {
                    fileAllocSJEs += 1
                    val debitSJE = buildAllocSJE(rcvRow.fields, rcvRow.nominalAccountID,
                      allocAmt, "DEBIT", fileAllocSJEs)
                    writeSJE(allocOut, debitSJE)
                  }
                }
              }
            }
          }
        }
      }

      allocOut.close()
      totalAllocSJEs += fileAllocSJEs
      println(s"  Allocation SJEs written: $fileAllocSJEs → $allocSJEFile")

      // Sort and post if any SJEs were generated
      if (fileAllocSJEs > 0) {
        val sortedFile = dataPath + "SortedJE_ALLOC" + yearStr + ".csv"
        sortSJEFile(allocSJEFile, sortedFile)
        println(s"  Posting allocation SJEs...")
        post(dataPath, dataPath)
        println(s"  Post complete.")
      }
    }

    printControlTotals(totalFilesProcessed, totalAllocSJEs)
  }

  private def buildAllocSJE(sourceFields: Array[String], nominalAccount: String,
                             amount: BigDecimal, side: String, seqNum: Int): Transaction = {
    Transaction(
      transIPID              = sourceFields(0),
      transContractID        = sourceFields(1),
      transCommitmentID      = sourceFields(2),
      transJrnlID            = "ALLOC" + seqNum,
      transJrnlLineID        = "1",
      transBusinessEventCode = "ALLOC",
      transSourceSystemID    = "ALLOC",
      transOriginalDocID     = sourceFields(3),
      transJrnlDescript      = s"Financial Allocation — $side",
      transLedgerID          = sourceFields(5),
      transJrnlType          = sourceFields(6),
      transBookCodeID        = sourceFields(7),
      transLegalEntityID     = sourceFields(8),
      transCenterID          = sourceFields(9),
      transProjectID         = sourceFields(10),
      transProductID         = sourceFields(11),
      transNominalAccountID  = nominalAccount,
      transAltAccountID      = sourceFields(13),
      transCurrencyCodeSourceID      = sourceFields(14),
      transCurrencyTypeCodeSourceID  = sourceFields(15),
      transCurrencyCodeTargetID      = sourceFields(16),
      transCurrencyTypeCodeTargetID  = sourceFields(17),
      transFiscalPeriod      = sourceFields(18),
      transAcctDate          = Calendar.getInstance.getTime.toString,
      transTransDate         = Calendar.getInstance.getTime.toString,
      transTransAmount       = amount,
      transUnitOfMeasure     = " ",
      transUnitPrice         = BigDecimal(0),
      transStatisticAmount   = BigDecimal(0),
      tranRuleSetID          = " ",
      transRuleID            = " ",
      transDirVsOffsetFlg    = if (side == "DEBIT") "D" else "O",
      transReconcileFlg      = "N",
      transAdjustFlg         = "N",
      transExtensionIDAuditTrail = "ALLOC",
      transExtensionIDSource     = " ",
      transExtensionIDClass      = " ",
      transExtensionIDDates      = Calendar.getInstance.getTime.toString,
      transExtensionIDCustom     = " "
    )
  }

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

  private def printControlTotals(filesProcessed: Int, allocSJEs: Int): Unit = {
    println("*" * 100)
    println("Financial Allocation — Control Totals")
    println(s"  LDGR files processed:     $filesProcessed")
    println(s"  Allocation SJEs written:  $allocSJEs")
    println(s"  Process End Time:         ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}
