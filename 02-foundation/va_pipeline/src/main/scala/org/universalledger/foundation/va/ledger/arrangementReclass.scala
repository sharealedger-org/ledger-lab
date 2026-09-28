package org.universalledger.foundation.va.ledger

/*
 * (c) Copyright IBM Corporation. 2018
 * SPDX-License-Identifier: Apache-2.0
 * By Kip Twitchell
 */

import java.io.{File, FileNotFoundException, PrintWriter}
import java.util.Calendar

import org.universalledger.foundation.va.datatypes._

import scala.collection.mutable
import scala.io.Source

//***************************************************************************************************************
//
//  Program:  arrangementReclass — Arrangement Reclassification
//
//  FSP Pattern:  Financial Modeling / Arrangement Reclass
//
//  This is the most complex process in the Universal Ledger pipeline.
//
//  WHAT IT DOES:
//  When a Vendor Master record changes in a way that affects how balances are classified,
//  reclassification journal entries are generated to move existing balances from the old
//  classification to the new one.
//
//  In the VA data: when a vendor's instTypeID changes from EXP to REV (or vice versa),
//  all existing balances for that vendor must move from EXP{nnn} nominal accounts to
//  REV{nnn} accounts (or vice versa).  If the change is backdated, this must happen for
//  every affected accounting period back to the effective date.
//
//  TRIGGER MECHANISM (minimum cost curve laboratory):
//  Change a vendor's type in VendorUpdate.csv → re-run this step → observe:
//    - How many new SJEs were generated (RECLASS_SJE.csv record count)
//    - How many balance rows were updated (LDGR files)
//    - How total storage footprint changed
//  This is one point on the minimum cost curve.  Change the rule, re-run, observe a new point.
//
//  INPUTS:
//    {outPath}/VendorMaster.csv    — current vendor master
//    {outPath}/VendorUpdate.csv    — vendors whose instTypeID changed since last run
//    {outPath}/LDGR{year}.csv      — balance files for all years in scope
//
//  OUTPUTS:
//    {outPath}/RECLASS_SJE.csv     — reclassification journal entries (audit trail)
//    {outPath}/LDGR{year}.csv      — updated balance files (overwrites via temp rename)
//    {outPath}/VendorMaster.csv    — updated with new type and effective dates
//
//  ORDER OF PROCESSING NOTE:
//  Arrangement Reclass MUST run BEFORE Currency Revaluation in the processing cycle.
//  Currency Revaluation also spawns multiple transactions per period; the multiplying
//  effect between the two processes means reclass must fire first to establish the
//  correct balance structure.
//
//  ALGORITHM:
//  (1) Load VendorUpdate records into a Map keyed by instID
//  (2) Load VendorMaster into memory; apply updates; write updated VendorMaster
//  (3) Open RECLASS_SJE output file
//  (4) For each LDGR{year}.csv file found in outPath:
//        Scan every balance row
//        If balance's ldgrIPID is in the changed-vendor map:
//          Determine old nominal account prefix (from current ledger row)
//          Determine new nominal account prefix (from VendorUpdate record)
//          Generate two SJEs:
//            SJE 1: Credit old nominal account × -1  (reverses the old balance)
//            SJE 2: Debit  new nominal account × +1  (opens the new balance)
//          Write both SJEs to RECLASS_SJE.csv
//          Write updated balance row (with zeroed amount) to temp LDGR file
//          Write new balance row (with original amount, new nominal account) to temp LDGR file
//        Else: write balance row unchanged
//        Rename temp LDGR to permanent LDGR
//  (5) Post all reclass SJEs back through the standard posting engine
//  (6) Print control totals
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Initial implementation (Bob AI)
//***************************************************************************************************************

object arrangementReclass {

  def apply(fileOutLocation: String): Unit = {

    println("*" * 100)
    println("                        Arrangement Reclass Module")
    println("                    FSP Pattern: Financial Modeling")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    if (fileOutLocation.isEmpty) {
      println("Invalid output file location.  Program abort.")
      sys.exit(1)
    }

    val dataPath = fileOutLocation

    //─────────────────────────────────────────────────────────────────────────
    // Control counters
    //─────────────────────────────────────────────────────────────────────────
    var vendorUpdatesRead    = 0
    var vendorMasterRead     = 0
    var vendorMasterUpdated  = 0
    var balancesRead         = 0
    var balancesReclassed    = 0
    var reclassSJEsWritten   = 0
    var filesProcessed       = 0

    val fileDelimiter = ","

    //─────────────────────────────────────────────────────────────────────────
    // (1) Read VendorUpdate.csv into a Map[instID -> VendorUpdate]
    //─────────────────────────────────────────────────────────────────────────
    val vendorUpdateFile = dataPath + "VendorUpdate.csv"
    val vendorUpdates = mutable.Map[String, VendorUpdate]()

    try {
      val lines = Source.fromFile(vendorUpdateFile).getLines().drop(1) // drop header
      for (line <- lines if line.trim.nonEmpty) {
        val e = line.split(fileDelimiter, -1).map(_.trim)
        val vu = VendorUpdate(
          instID         = e(0),
          instTypeID     = e(1),
          instEffectDate = e(2)
        )
        vendorUpdates(vu.instID) = vu
        vendorUpdatesRead += 1
      }
    } catch {
      case _: FileNotFoundException =>
        println(s"No VendorUpdate file found at $vendorUpdateFile — nothing to reclass.")
        printControlTotals(vendorUpdatesRead, vendorMasterRead, vendorMasterUpdated,
          balancesRead, balancesReclassed, reclassSJEsWritten, filesProcessed)
        return
    }

    println(s"Vendor updates read: $vendorUpdatesRead")
    if (vendorUpdates.isEmpty) {
      println("No vendor updates — nothing to reclass.")
      printControlTotals(vendorUpdatesRead, vendorMasterRead, vendorMasterUpdated,
        balancesRead, balancesReclassed, reclassSJEsWritten, filesProcessed)
      return
    }

    //─────────────────────────────────────────────────────────────────────────
    // (2) Streaming update of VendorMaster.csv -> VendorMasterTemp.csv (O(1) RAM)
    //─────────────────────────────────────────────────────────────────────────
    val vendorMasterFile    = dataPath + "VendorMaster.csv"
    val vendorMasterTmpFile = dataPath + "VendorMasterTemp.csv"
    val now = Calendar.getInstance.getTime.toString

    try {
      val vmSource = Source.fromFile(vendorMasterFile)
      val vmLines = vmSource.getLines()
      if (vmLines.hasNext) {
        val header = vmLines.next()
        val vmOut = new PrintWriter(new File(vendorMasterTmpFile))
        vmOut.write(header + "\n")

        for (line <- vmLines if line.trim.nonEmpty) {
          vendorMasterRead += 1
          val e = line.split(fileDelimiter, -1).map(_.trim)
          val instID = e(0)
          vendorUpdates.get(instID) match {
            case Some(vu) =>
              // Update type and effect date while preserving remaining fields
              e(1) = vu.instEffectDate
              e(4) = vu.instTypeID
              if (e.length > 9) e(9) = now
              vmOut.write(e.mkString(",") + "\n")
              vendorMasterUpdated += 1
            case None =>
              vmOut.write(line + "\n")
          }
        }
        vmOut.close()
        vmSource.close()
        new File(vendorMasterTmpFile).renameTo(new File(vendorMasterFile))
      } else {
        vmSource.close()
      }
    } catch {
      case _: FileNotFoundException =>
        println(s"WARNING: No VendorMaster file at $vendorMasterFile — will proceed without it.")
    }

    //─────────────────────────────────────────────────────────────────────────
    // (3) Open RECLASS_SJE output file
    //─────────────────────────────────────────────────────────────────────────
    val reclassSJEFile = dataPath + "RECLASS_SJE.csv"
    val reclassOut = new PrintWriter(new File(reclassSJEFile))
    writeSJEHeader(reclassOut)

    //─────────────────────────────────────────────────────────────────────────
    // (4) Process each LDGR file — scan, reclass affected rows, rewrite
    //─────────────────────────────────────────────────────────────────────────
    val ldgrFiles = getListOfFiles(dataPath, "LDGR").sortWith(_.getName < _.getName)

    for (ldgrFile <- ldgrFiles) {
      filesProcessed += 1
      println(s"Processing ledger file: ${ldgrFile.getName}")

      val tmpFile = ldgrFile.getAbsolutePath.replace("LDGR", "LDGRtemp")
      val ldgrOut = new PrintWriter(new File(tmpFile))
      writeLedgerHeader(ldgrOut)

      var fileLdgrRead     = 0
      var fileLdgrReclassed = 0

      val lines = Source.fromFile(ldgrFile).getLines().drop(1) // drop header
      for (line <- lines if line.trim.nonEmpty) {
        val e = line.split(fileDelimiter, -1).map(_.trim)

        // Parse the ledger row — 30 fields, amount is field index 19
        val ldgrRec = Ledger(
          ldgrIPID                   = e(0),
          ldgrContractID             = e(1),
          ldgrCommitmentID           = e(2),
          ldgrLdgrlID                = e(3),
          ldgrSourceSystemID         = e(4),
          ldgrLedgerID               = e(5),
          ldgrJrnlType               = e(6),
          ldgrBookCodeID             = e(7),
          ldgrLegalEntityID          = e(8),
          ldgrCenterID               = e(9),
          ldgrProjectID              = e(10),
          ldgrProductID              = e(11),
          ldgrNominalAccountID       = e(12),
          ldgrAltAccountID           = e(13),
          ldgrCurrencyCodeSourceID   = e(14),
          ldgrCurrencyTypeCodeSourceID = e(15),
          ldgrCurrencyCodeTargetID   = e(16),
          ldgrCurrencyTypeCodeTargetID = e(17),
          ldgrLedgerPeriod           = e(18),
          ldgrTransAmount            = BigDecimal(e(19)),
          ldgrUnitOfMeasure          = e(20),
          ldgrStatisticAmount        = BigDecimal(e(21)),
          ldgrDirVsOffsetFlg         = e(22),
          ldgrReconcileFlg           = e(23),
          ldgrAdjustFlg              = e(24),
          ldgrExtensionIDAuditTrail  = e(25),
          ldgrExtensionIDSource      = e(26),
          ldgrExtensionIDClass       = e(27),
          ldgrExtensionIDDates       = e(28),
          ldgrExtensionIDCustom      = e(29)
        )

        fileLdgrRead += 1

        vendorUpdates.get(ldgrRec.ldgrIPID) match {
          case Some(vu) =>
            // This balance belongs to a vendor whose type changed.
            // Determine old nominal prefix from the current account ID.
            val oldNominal = ldgrRec.ldgrNominalAccountID         // e.g. "EXP202"
            val newPrefix  = vu.instTypeID                        // "EXP" or "REV"
            val oldPrefix  = if (newPrefix == "EXP") "REV" else "EXP"
            val suffix     = oldNominal.stripPrefix("EXP").stripPrefix("REV")
            val newNominal = newPrefix + suffix                   // e.g. "REV202"

            // Only reclass if the prefix actually needs to change
            if (!oldNominal.startsWith(newPrefix)) {
              fileLdgrReclassed += 1
              balancesReclassed += 1
              val amt = ldgrRec.ldgrTransAmount

              //─────────────────────────────────────────────────────────────
              // SJE 1 — Credit old nominal account (reverse the old balance)
              //─────────────────────────────────────────────────────────────
              val sje1 = buildReclassSJE(
                ldgrRec   = ldgrRec,
                newNominal = oldNominal,
                amount    = amt * -1,
                lineNum   = "1",
                seqNum    = reclassSJEsWritten + 1
              )
              writeSJE(reclassOut, sje1)
              reclassSJEsWritten += 1

              //─────────────────────────────────────────────────────────────
              // SJE 2 — Debit new nominal account (open the new balance)
              //─────────────────────────────────────────────────────────────
              val sje2 = buildReclassSJE(
                ldgrRec   = ldgrRec,
                newNominal = newNominal,
                amount    = amt,
                lineNum   = "2",
                seqNum    = reclassSJEsWritten + 1
              )
              writeSJE(reclassOut, sje2)
              reclassSJEsWritten += 1

              // Write the OLD balance row zeroed out (amount = 0 after reversal)
              writeLedgerRow(ldgrOut, ldgrRec.copy(ldgrTransAmount = BigDecimal(0)))

              // Write the NEW balance row with the new nominal account
              writeLedgerRow(ldgrOut, ldgrRec.copy(
                ldgrNominalAccountID = newNominal,
                ldgrTransAmount      = amt
              ))
            } else {
              // Prefix already correct — no reclass needed, write unchanged
              writeLedgerRow(ldgrOut, ldgrRec)
            }

          case None =>
            // Not a changed vendor — write through unchanged
            writeLedgerRow(ldgrOut, ldgrRec)
        }
      }

      ldgrOut.close()
      new File(tmpFile).renameTo(ldgrFile)

      balancesRead += fileLdgrRead
      println(s"  Balances read: $fileLdgrRead  Reclassed: $fileLdgrReclassed")
    }

    reclassOut.close()

    //─────────────────────────────────────────────────────────────────────────
    // (5) Post the reclass SJEs back through the standard posting engine
    //     This ensures the balance file reflects the SJEs and remains provable.
    //─────────────────────────────────────────────────────────────────────────
    if (reclassSJEsWritten > 0) {
      println("-" * 60)
      println("Posting reclass SJEs back to ledger via standard post engine...")

      // Sort the reclass SJE file so the posting engine can consume it
      // (post.scala expects SortedJE-prefixed files sorted on the full balance key)
      val reclassSortedFile = fileOutLocation + "SortedJE_RECLASS.csv"
      sortSJEFile(reclassSJEFile, reclassSortedFile)

      post(fileOutLocation, fileOutLocation)
      println("Reclass SJEs posted successfully.")
    }

    //─────────────────────────────────────────────────────────────────────────
    // (6) Control totals
    //─────────────────────────────────────────────────────────────────────────
    printControlTotals(vendorUpdatesRead, vendorMasterRead, vendorMasterUpdated,
      balancesRead, balancesReclassed, reclassSJEsWritten, filesProcessed)
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: build a reclass SJE from a ledger row
  //───────────────────────────────────────────────────────────────────────────
  private def buildReclassSJE(ldgrRec: Ledger, newNominal: String,
                               amount: BigDecimal, lineNum: String,
                               seqNum: Int): Transaction = {
    Transaction(
      transIPID              = ldgrRec.ldgrIPID,
      transContractID        = ldgrRec.ldgrContractID,
      transCommitmentID      = ldgrRec.ldgrCommitmentID,
      transJrnlID            = "RECLASS" + seqNum.toString,
      transJrnlLineID        = lineNum,
      transBusinessEventCode = "RECLASS",
      transSourceSystemID    = "RECLASS",
      transOriginalDocID     = ldgrRec.ldgrLdgrlID,
      transJrnlDescript      = "Arrangement Reclass — type change",
      transLedgerID          = ldgrRec.ldgrLedgerID,
      transJrnlType          = ldgrRec.ldgrJrnlType,
      transBookCodeID        = ldgrRec.ldgrBookCodeID,
      transLegalEntityID     = ldgrRec.ldgrLegalEntityID,
      transCenterID          = ldgrRec.ldgrCenterID,
      transProjectID         = ldgrRec.ldgrProjectID,
      transProductID         = ldgrRec.ldgrProductID,
      transNominalAccountID  = newNominal,
      transAltAccountID      = ldgrRec.ldgrAltAccountID,
      transCurrencyCodeSourceID      = ldgrRec.ldgrCurrencyCodeSourceID,
      transCurrencyTypeCodeSourceID  = ldgrRec.ldgrCurrencyTypeCodeSourceID,
      transCurrencyCodeTargetID      = ldgrRec.ldgrCurrencyCodeTargetID,
      transCurrencyTypeCodeTargetID  = ldgrRec.ldgrCurrencyTypeCodeTargetID,
      transFiscalPeriod      = ldgrRec.ldgrLedgerPeriod,
      transAcctDate          = Calendar.getInstance.getTime.toString,
      transTransDate         = Calendar.getInstance.getTime.toString,
      transTransAmount       = amount,
      transUnitOfMeasure     = ldgrRec.ldgrUnitOfMeasure,
      transUnitPrice         = BigDecimal(0),
      transStatisticAmount   = BigDecimal(0),
      tranRuleSetID          = " ",
      transRuleID            = " ",
      transDirVsOffsetFlg    = if (lineNum == "1") "O" else "D",
      transReconcileFlg      = "N",
      transAdjustFlg         = "N",
      transExtensionIDAuditTrail = "RECLASS",
      transExtensionIDSource     = " ",
      transExtensionIDClass      = " ",
      transExtensionIDDates      = Calendar.getInstance.getTime.toString,
      transExtensionIDCustom     = " "
    )
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: sort a reclass SJE file using SortEngine with SortedJE.sortspec
  //───────────────────────────────────────────────────────────────────────────
  private def sortSJEFile(inputFile: String, outputFile: String): Unit = {
    val src = Source.fromFile(inputFile)
    val lines = src.getLines()
    if (!lines.hasNext) {
      src.close()
      return
    }
    val _ = lines.next() // drop header because SortedJE input for post.scala is headerless

    val specPathCandidates = Seq("../sort_specs/SortedJE.sortspec", "../../sort_specs/SortedJE.sortspec")
    val specPath = specPathCandidates.find(p => new File(p).exists()).getOrElse("../sort_specs/SortedJE.sortspec")
    val sortSpec = SortEngine.loadSpec(specPath)

    val (totalRecords, spillCount) = SortEngine.externalSort(lines.filter(_.trim.nonEmpty), outputFile, sortSpec)
    src.close()

    println(s"  Reclass SJE file sorted: $totalRecords records across $spillCount spills → $outputFile")
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: write SJE header
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
  // Helper: write ledger header
  //───────────────────────────────────────────────────────────────────────────
  private def writeLedgerHeader(out: PrintWriter): Unit = {
    out.write(
      "ldgrIPID,ldgrContractID,ldgrCommitmentID,ldgrLdgrlID,ldgrSourceSystemID," +
      "ldgrLedgerID,ldgrJrnlType,ldgrBookCodeID,ldgrLegalEntityID,ldgrCenterID," +
      "ldgrProjectID,ldgrProductID,ldgrNominalAccountID,ldgrAltAccountID," +
      "ldgrCurrencyCodeSourceID,ldgrCurrencyTypeCodeSourceID," +
      "ldgrCurrencyCodeTargetID,ldgrCurrencyTypeCodeTargetID," +
      "ldgrLedgerPeriod,ldgrTransAmount,ldgrUnitOfMeasure,ldgrStatisticAmount," +
      "ldgrDirVsOffsetFlg,ldgrReconcileFlg,ldgrAdjustFlg," +
      "ldgrExtensionIDAuditTrail,ldgrExtensionIDSource,ldgrExtensionIDClass," +
      "ldgrExtensionIDDates,ldgrExtensionIDCustom\n"
    )
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: write one ledger row
  //───────────────────────────────────────────────────────────────────────────
  private def writeLedgerRow(out: PrintWriter, r: Ledger): Unit = {
    out.write(
      s"${r.ldgrIPID},${r.ldgrContractID},${r.ldgrCommitmentID},${r.ldgrLdgrlID}," +
      s"${r.ldgrSourceSystemID},${r.ldgrLedgerID},${r.ldgrJrnlType},${r.ldgrBookCodeID}," +
      s"${r.ldgrLegalEntityID},${r.ldgrCenterID},${r.ldgrProjectID},${r.ldgrProductID}," +
      s"${r.ldgrNominalAccountID},${r.ldgrAltAccountID}," +
      s"${r.ldgrCurrencyCodeSourceID},${r.ldgrCurrencyTypeCodeSourceID}," +
      s"${r.ldgrCurrencyCodeTargetID},${r.ldgrCurrencyTypeCodeTargetID}," +
      s"${r.ldgrLedgerPeriod},${r.ldgrTransAmount},${r.ldgrUnitOfMeasure}," +
      s"${r.ldgrStatisticAmount},${r.ldgrDirVsOffsetFlg},${r.ldgrReconcileFlg}," +
      s"${r.ldgrAdjustFlg},${r.ldgrExtensionIDAuditTrail},${r.ldgrExtensionIDSource}," +
      s"${r.ldgrExtensionIDClass},${r.ldgrExtensionIDDates},${r.ldgrExtensionIDCustom}\n"
    )
  }

  //───────────────────────────────────────────────────────────────────────────
  // Helper: print control totals
  //───────────────────────────────────────────────────────────────────────────
  private def printControlTotals(vendorUpdatesRead: Int, vendorMasterRead: Int,
                                  vendorMasterUpdated: Int, balancesRead: Int,
                                  balancesReclassed: Int, reclassSJEsWritten: Int,
                                  filesProcessed: Int): Unit = {
    println("*" * 100)
    println("Arrangement Reclass — Control Totals")
    println(s"  Vendor Update records read:   $vendorUpdatesRead")
    println(s"  Vendor Master records read:   $vendorMasterRead")
    println(s"  Vendor Master records updated:$vendorMasterUpdated")
    println(s"  LDGR files processed:         $filesProcessed")
    println(s"  Balance records read:         $balancesRead")
    println(s"  Balance records reclassed:    $balancesReclassed")
    println(s"  Reclass SJEs written:         $reclassSJEsWritten")
    println(s"  Process End Time:             ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}
