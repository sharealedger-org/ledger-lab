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
//  Program:  contraCreation — Reconciliation / Contra Creation
//
//  FSP Pattern:  Reconciliation
//
//  WHAT IT DOES:
//  Creates contra (offsetting) transactions that, when posted to the balance file, cause
//  the ENTIRE file to sum to zero.  This is the reconciliation proof:
//
//    sum(all vendor balances) + sum(all contra balances) = 0
//
//  If the file does not sum to zero after contra posting, an error exists somewhere in the
//  pipeline — a transaction was lost, doubled, or mis-posted.  This is the foundation of
//  double-entry bookkeeping made explicit and machine-verifiable.
//
//  THE CONTRA RECORD IS A SUMMARY:
//  Unlike vendor-level balance records, a contra record has NO Vendor ID (ldgrIPID = "")
//  and NO Balance ID (ldgrLdgrlID = "").  It represents the aggregate of all vendor balances
//  at the (Legal Entity × Nominal Account × Currency pair × Fiscal Period) level, negated.
//  This mirrors the GL-level view — the contra is the GL entry that offsets the subledger.
//
//  ORDER OF PROCESSING: MUST RUN LAST in the processing cycle — after all other processes
//  (Arrangement Reclass, Currency Conversion, Currency Revaluation) have updated balances.
//  Running it early and then running other processes will break the zero-sum proof.
//
//  ALGORITHM:
//  (1) For each LDGR{year}.csv file:
//        Single pass — accumulate balance amounts into a Map keyed by
//        (legalEntity, nominalAccount, currencyCodeSource, currencyCodeTarget,
//         currencyTypeSource, currencyTypeTarget, ledgerPeriod)
//        Skip any rows where ldgrIPID is empty — those are existing contra rows;
//        don't contra the contras.
//  (2) For each accumulated bucket: create one contra SJE — amount × -1, no Vendor ID
//  (3) Write all contra SJEs to CONTRA_SJE{year}.csv
//  (4) Sort the contra SJEs on the full balance key
//  (5) Post contra SJEs through the standard post engine
//  (6) Verify the file sums to zero; print the proof
//
//  INPUTS:
//    {outPath}/LDGR{year}.csv      — final balance files for the cycle
//
//  OUTPUTS:
//    {outPath}/CONTRA_SJE{year}.csv — contra journal entries (audit trail, per year)
//    {outPath}/LDGR{year}.csv       — updated with contra balance records appended
//    stdout: reconciliation proof — sum of all balances = 0 (or error if not)
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Initial implementation (Bob AI)
//***************************************************************************************************************

object contraCreation {

  // Accumulation key: the dimensions that define a unique GL-level balance bucket
  case class ContraKey(
    legalEntityID:           String,
    nominalAccountID:        String,
    currencyCodeSourceID:    String,
    currencyTypeCodeSourceID: String,
    currencyCodeTargetID:    String,
    currencyTypeCodeTargetID: String,
    ledgerPeriod:            String,
    // These carry through to the contra record but are not part of the key
    ledgerID:                String,
    jrnlType:                String,
    bookCodeID:              String,
    centerID:                String,
    projectID:               String,
    productID:               String,
    altAccountID:            String,
    dirVsOffsetFlg:          String,
    adjustFlg:               String
  )

  def apply(fileOutLocation: String): Unit = {

    println("*" * 100)
    println("                        Contra Creation Module")
    println("                    FSP Pattern: Reconciliation")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)
    println("NOTE: This module MUST run last — after all other balance-updating processes.")
    println("-" * 60)

    if (fileOutLocation.isEmpty) {
      println("Invalid output file location. Program abort.")
      sys.exit(1)
    }

    val dataPath      = fileOutLocation
    val fileDelimiter = ","

    var filesProcessed    = 0
    var balancesRead      = 0
    var contraSJEsWritten = 0
    var filesVerified     = 0
    var filesFailedProof  = 0

    //─────────────────────────────────────────────────────────────────────────
    // Process each LDGR file independently
    //─────────────────────────────────────────────────────────────────────────
    val ldgrFiles = getListOfFiles(dataPath, "LDGR").sortWith(_.getName < _.getName)

    if (ldgrFiles.isEmpty) {
      println(s"No LDGR files found in $dataPath — nothing to process.")
      printControlTotals(filesProcessed, balancesRead, contraSJEsWritten, filesVerified, filesFailedProof)
      return
    }

    for (ldgrFile <- ldgrFiles) {
      filesProcessed += 1

      // Derive year from filename e.g. "LDGR2003.csv" → "2003"
      val yearStr = ldgrFile.getName.stripPrefix("LDGR").stripSuffix(".csv")
      println(s"Processing: ${ldgrFile.getName}  (year=$yearStr)")

      //───────────────────────────────────────────────────────────────────────
      // (1) Single pass — accumulate vendor balances into contra buckets
      //     Skip rows where ldgrIPID is empty (those are existing contra rows)
      //───────────────────────────────────────────────────────────────────────
      val accumulator = mutable.Map[ContraKey, BigDecimal]().withDefaultValue(BigDecimal(0))
      var fileBalancesRead = 0

      val lines = Source.fromFile(ldgrFile).getLines().drop(1) // drop header
      for (line <- lines if line.trim.nonEmpty) {
        val e = line.split(fileDelimiter, -1).map(_.trim)
        val ipID = e(0)

        // Skip existing contra rows (no vendor ID)
        if (ipID.nonEmpty) {
          val amount = BigDecimal(e(19))
          val key = ContraKey(
            legalEntityID            = e(8),
            nominalAccountID         = e(12),
            currencyCodeSourceID     = e(14),
            currencyTypeCodeSourceID = e(15),
            currencyCodeTargetID     = e(16),
            currencyTypeCodeTargetID = e(17),
            ledgerPeriod             = e(18),
            // pass-through fields for building the contra record
            ledgerID                 = e(5),
            jrnlType                 = e(6),
            bookCodeID               = e(7),
            centerID                 = e(9),
            projectID                = e(10),
            productID                = e(11),
            altAccountID             = e(13),
            dirVsOffsetFlg           = e(22),
            adjustFlg                = e(24)
          )
          accumulator(key) = accumulator(key) + amount
          fileBalancesRead += 1
        }
      }

      balancesRead += fileBalancesRead
      println(s"  Vendor balance rows read: $fileBalancesRead")
      println(s"  Unique contra buckets:    ${accumulator.size}")

      if (accumulator.isEmpty) {
        println(s"  No vendor balances found — skipping contra creation for $yearStr")
      } else {

        //─────────────────────────────────────────────────────────────────────
        // (2) & (3) Build contra SJEs and write to CONTRA_SJE{year}.csv
        //─────────────────────────────────────────────────────────────────────
        val contraFile = dataPath + "CONTRA_SJE" + yearStr + ".csv"
        val contraOut  = new PrintWriter(new File(contraFile))
        writeSJEHeader(contraOut)

        var fileContraSJEs = 0
        for ((key, totalAmt) <- accumulator) {
          // Only write contra if total is non-zero (zero-balance buckets need no contra)
          if (totalAmt != BigDecimal(0)) {
            fileContraSJEs += 1
            val sje = buildContraSJE(key, totalAmt * -1, fileContraSJEs)
            writeSJE(contraOut, sje)
          }
        }
        contraOut.close()
        contraSJEsWritten += fileContraSJEs
        println(s"  Contra SJEs written:      $fileContraSJEs  → $contraFile")

        //─────────────────────────────────────────────────────────────────────
        // (4) Sort the contra SJE file on the full balance key
        //─────────────────────────────────────────────────────────────────────
        val contraSortedFile = dataPath + "SortedJE_CONTRA" + yearStr + ".csv"
        sortSJEFile(contraFile, contraSortedFile)

        //─────────────────────────────────────────────────────────────────────
        // (5) Post contra SJEs through the standard posting engine
        //─────────────────────────────────────────────────────────────────────
        println(s"  Posting contra SJEs via standard post engine...")
        post(dataPath, dataPath)
        println(s"  Post complete.")

        //─────────────────────────────────────────────────────────────────────
        // (6) Verify: re-read the updated LDGR file and sum ALL rows
        //     (vendor rows + contra rows) — must equal zero
        //─────────────────────────────────────────────────────────────────────
        var grandTotal: BigDecimal = BigDecimal(0)
        var verifyRows = 0
        val verifyLines = Source.fromFile(ldgrFile).getLines().drop(1)
        for (line <- verifyLines if line.trim.nonEmpty) {
          val e = line.split(fileDelimiter, -1).map(_.trim)
          grandTotal += BigDecimal(e(19))
          verifyRows += 1
        }

        println("-" * 60)
        if (grandTotal == BigDecimal(0)) {
          println(s"  ✓ RECONCILIATION PROOF PASSED — year $yearStr")
          println(s"    All $verifyRows balance rows sum to ZERO.")
          filesVerified += 1
        } else {
          println(s"  ✗ RECONCILIATION PROOF FAILED — year $yearStr")
          println(s"    $verifyRows rows sum to $grandTotal (expected 0).")
          println(s"    An error exists in the pipeline for this year.")
          filesFailedProof += 1
        }
        println("-" * 60)
      }
    }

    printControlTotals(filesProcessed, balancesRead, contraSJEsWritten, filesVerified, filesFailedProof)
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: build one contra SJE from an accumulation bucket
  //───────────────────────────────────────────────────────────────────────────
  private def buildContraSJE(key: ContraKey, contraAmount: BigDecimal,
                              seqNum: Int): Transaction = {
    Transaction(
      transIPID              = "",          // no Vendor ID — this is the GL-level summary
      transContractID        = "",
      transCommitmentID      = "",
      transJrnlID            = "CONTRA" + seqNum.toString,
      transJrnlLineID        = "1",
      transBusinessEventCode = "CONTRA",
      transSourceSystemID    = "CONTRA",
      transOriginalDocID     = "",
      transJrnlDescript      = "Contra — reconciliation offset",
      transLedgerID          = key.ledgerID,
      transJrnlType          = key.jrnlType,
      transBookCodeID        = key.bookCodeID,
      transLegalEntityID     = key.legalEntityID,
      transCenterID          = key.centerID,
      transProjectID         = key.projectID,
      transProductID         = key.productID,
      transNominalAccountID  = key.nominalAccountID,
      transAltAccountID      = key.altAccountID,
      transCurrencyCodeSourceID      = key.currencyCodeSourceID,
      transCurrencyTypeCodeSourceID  = key.currencyTypeCodeSourceID,
      transCurrencyCodeTargetID      = key.currencyCodeTargetID,
      transCurrencyTypeCodeTargetID  = key.currencyTypeCodeTargetID,
      transFiscalPeriod      = key.ledgerPeriod,
      transAcctDate          = Calendar.getInstance.getTime.toString,
      transTransDate         = Calendar.getInstance.getTime.toString,
      transTransAmount       = contraAmount,
      transUnitOfMeasure     = " ",
      transUnitPrice         = BigDecimal(0),
      transStatisticAmount   = BigDecimal(0),
      tranRuleSetID          = " ",
      transRuleID            = " ",
      transDirVsOffsetFlg    = "O",         // offset — this is the contra side
      transReconcileFlg      = "Y",         // flagged as reconciliation entry
      transAdjustFlg         = "N",
      transExtensionIDAuditTrail = "CONTRA",
      transExtensionIDSource     = " ",
      transExtensionIDClass      = " ",
      transExtensionIDDates      = Calendar.getInstance.getTime.toString,
      transExtensionIDCustom     = " "
    )
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: sort a SJE file on the full balance key (matches post.scala key contract)
  // Key fields (0-based in Transaction CSV output):
  //   ledgerID(9), jrnlType(10), bookCodeID(11), legalEntityID(12), centerID(13),
  //   projectID(14), productID(15), nominalAccountID(16), altAccountID(17),
  //   currencyCodeSourceID(18), currencyTypeCodeSourceID(19),
  //   currencyCodeTargetID(20), currencyTypeCodeTargetID(21), fiscalPeriod(22)
  //───────────────────────────────────────────────────────────────────────────
  private def sortSJEFile(inputFile: String, outputFile: String): Unit = {
    val lines = Source.fromFile(inputFile).getLines().toList
    if (lines.size <= 1) {
      val out = new PrintWriter(new File(outputFile))
      lines.foreach(out.println)
      out.close()
      return
    }
    val header = lines.head
    val sorted = lines.tail.filter(_.trim.nonEmpty).sortWith { (a, b) =>
      val ea = a.split(",", -1)
      val eb = b.split(",", -1)
      def key(e: Array[String]) = (9 to 22).map(i => if (i < e.length) e(i) else "").mkString
      key(ea) < key(eb)
    }
    val out = new PrintWriter(new File(outputFile))
    out.println(header)
    sorted.foreach(out.println)
    out.close()
    println(s"  Contra SJE file sorted: ${sorted.size} records → $outputFile")
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: SJE header — matches Transaction field order
  //───────────────────────────────────────────────────────────────────────────
  private def writeSJEHeader(out: PrintWriter): Unit = {
    out.write(
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
      "transExtensionIDDates,transExtensionIDCustom\n"
    )
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: write one SJE row
  //───────────────────────────────────────────────────────────────────────────
  private def writeSJE(out: PrintWriter, t: Transaction): Unit = {
    out.write(
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
      s"${t.transExtensionIDClass},${t.transExtensionIDDates},${t.transExtensionIDCustom}\n"
    )
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: control totals
  //───────────────────────────────────────────────────────────────────────────
  private def printControlTotals(filesProcessed: Int, balancesRead: Int,
                                  contraSJEsWritten: Int, filesVerified: Int,
                                  filesFailedProof: Int): Unit = {
    println("*" * 100)
    println("Contra Creation — Control Totals")
    println(s"  LDGR files processed:          $filesProcessed")
    println(s"  Vendor balance rows read:       $balancesRead")
    println(s"  Contra SJEs written:            $contraSJEsWritten")
    println(s"  Files passing zero-sum proof:   $filesVerified")
    println(s"  Files FAILING zero-sum proof:   $filesFailedProof")
    if (filesFailedProof > 0) {
      println("  *** ACTION REQUIRED: Files that failed the zero-sum proof have errors ***")
      println("  *** Check pipeline run order — Contra must run LAST in the cycle    ***")
    }
    println(s"  Process End Time:               ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}
