package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter, BufferedWriter, FileWriter}
import java.util.Calendar
import scala.collection.mutable
import scala.io.Source

//***************************************************************************************************************
//
//  Program:  standardizeAndSort — Step 2: Vendor-Keyed SortedJE Production
//
//  Replaces transStandardize.scala + sortJE.scala with a single scale-invariant step.
//
//  WHAT THIS DOES
//  --------------
//  transStandardize.scala (original) aggregated expense rows by (agency × fund × obj × program),
//  discarding the vendor name, and set transIPID = "0" for every row.  This destroyed the
//  Instrument dimension before posting and produced an agency ledger, not a vendor ledger.
//
//  This module does the job correctly:
//    1. Reads a single raw VA expenditure/revenue file (tab-delimited FY03–FY13, CSV FY14–FY16).
//    2. Looks up UPPER(vendor_name) → instID in VendorMaster (O(1) per row).
//    3. Emits two journal lines per transaction (debit + credit) keyed on instID.
//    4. Sorts the output on the full 15-field posting key using an external merge-sort:
//         chunk (chunkSize rows) → sort in memory → spill to temp file → k-way merge.
//       Peak JVM heap = O(chunkSize), not O(N).  N may be hundreds of millions.
//    5. Writes SortedJE_{year}_{quarter}_{type}.csv — drop-in input for post.scala (Step 3).
//
//  EXTERNAL MERGE-SORT
//  -------------------
//  This is the GenevaERS DFSORT analog.  On z/OS the sort utility ran in a separate job step
//  between Standardize and Post as a first-class architectural component.  This class restores
//  that discipline on the JVM without requiring OS-level DFSORT or a Spark cluster.
//
//  CLI invocation (via LedgerApp dispatcher):
//    sbt "run --step 2 --inPath data/VARawFiles/ --outPath data/output/
//             --vendormaster data/VendorMaster_full.csv
//             --year 2003 --quarter 1 --type E"
//
//  Parameters (passed by LedgerApp dispatcher from CLI map):
//    inPath        — directory containing raw VA files
//    outPath       — directory for SortedJE output
//    vendormaster  — path to VendorMaster CSV (instHolderName → instID)
//    year          — 4-digit fiscal year (e.g. 2003)
//    quarter       — 1–4 (expense) or 5 (revenue)
//    type          — E=Expense, R=Revenue
//
//  RECORD FORMATS
//  --------------
//    A  FY03–FY11 expense:  AGY  FUND  OBJ  PROG  VENDOR_NAME  AMOUNT        (tab)
//    B  FY12–FY13 expense:  AGY  FUND  OBJ  PROG  VENDOR_NAME  AMOUNT  DATE  (tab)
//    C  FY14–FY16 expense:  AGY  AMOUNT  FUND  OBJ  PROG  VENDOR_NAME  DATE  (csv)
//    RA FY03–FY13 revenue:  AGY  FUND  SRC  AMOUNT                           (tab)
//    RC FY14–FY16 revenue:  AGY  AMOUNT  FUND  SRC  DATE                     (csv)
//
//  SORTEDJE COLUMN ORDER (39 columns — matches Transaction.scala field order):
//    0  transIPID              ← instID from VendorMaster lookup
//    1  transContractID        = "0"
//    2  transCommitmentID      = "0"
//    3  transJrnlID            = "ID{n}{acctDate}"
//    4  transJrnlLineID        = "1" or "2"
//    5  transBusinessEventCode
//    6  transSourceSystemID
//    7  transOriginalDocID     = "Unknown"
//    8  transJrnlDescript
//    9  transLedgerID          = "ACTUALS"
//   10  transJrnlType          = "FIN"
//   11  transBookCodeID        = "SHRD-3RD-PARTY"
//   12  transLegalEntityID     ← agency
//   13  transCenterID          ← fund
//   14  transProjectID         ← object code
//   15  transProductID         = "0"
//   16  transNominalAccountID  ← "EXP{prog}" / "REV{src}" / "0000" (offset)
//   17  transAltAccountID      = "0"
//   18  transCurrencyCodeSourceID      = "USD"
//   19  transCurrencyTypeCodeSourceID  = "TXN"
//   20  transCurrencyCodeTargetID      = "USD"
//   21  transCurrencyTypeCodeTargetID  = "BASE-LE"
//   22  transFiscalPeriod      ← year (4-digit string)
//   23  transAcctDate          ← derived from quarter
//   24  transTransDate         ← same as acctDate, or file DATE col if present
//   25  transTransAmount
//   26  transUnitOfMeasure     = " "
//   27  transUnitPrice         = "0"
//   28  transStatisticAmount   = "0"
//   29  tranRuleSetID          = " "
//   30  transRuleID            = " "
//   31  transDirVsOffsetFlg    = "D"
//   32  transReconcileFlg      = "N"
//   33  transAdjustFlg         = "N"
//   34  transExtensionIDAuditTrail = " "
//   35  transExtensionIDSource     = " "
//   36  transExtensionIDClass      = " "
//   37  transExtensionIDDates      = " "
//   38  transExtensionIDCustom     = " "
//
//  SORT KEY — col indices (15 fields, matches post.scala testJrnlFullKey):
//    0,9,10,11,12,13,14,15,16,17,18,19,20,21,22
//    (transIPID + transLedgerID..transCurrencyTypeCodeTargetID + transFiscalPeriod)
//
//  *(c) Copyright IBM Corporation. 2018
//  * SPDX-License-Identifier: Apache-2.0
//  * By Kip Twitchell
//  2025 — Scala rewrite of Python standardize_and_sort.py (Bob AI, Session 21)
//         Corrects transIPID=0 defect; restores scale-invariant external merge-sort
//***************************************************************************************************************

object standardizeAndSort {

  private case class AccountingRule(
    recordType: String,
    ruleSetId: String,
    ruleVersion: String,
    ruleId: String,
    businessEventCode: String,
    debitAccountExpression: String,
    creditAccountExpression: String,
    debitProjectExpression: String,
    creditProjectExpression: String,
    offsetDescription: String
  )

  // ── Constants matching Transaction.scala defaults ──────────────────────────
  private val LEDGER_ID     = "ACTUALS"
  private val JRNL_TYPE     = "FIN"
  private val BOOK_CODE     = "SHRD-3RD-PARTY"
  private val PRODUCT_ID    = "0"
  private val ALT_ACCOUNT   = "0"
  private val CCY_SRC       = "USD"
  private val CCY_TYPE_SRC  = "TXN"
  private val CCY_TGT       = "USD"
  private val CCY_TYPE_TGT  = "BASE-LE"
  private val UNKNOWN_INST  = "0000000000"
  private val DEFAULT_VM_1  = "data/VendorMaster_full.csv"
  private val DEFAULT_VM_2  = "data/VendorMaster_enriched.csv"
  private val DEFAULT_VM_3  = "data/VendorMaster.csv"
  private val DEFAULT_SPEC_1 = "../sort_specs/SortedJE.sortspec"
  private val DEFAULT_SPEC_2 = "../../sort_specs/SortedJE.sortspec"

  private val DEFAULT_CHUNK = 200000  // rows per spill chunk (~60–80 MB of JE output)

  // ── Entry point called by LedgerApp dispatcher ─────────────────────────────
  def apply(params: Map[String, String]): Unit = {

    println("*" * 100)
    println("                         Step 2 — Standardize & Sort (standardizeAndSort)")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    val inPath       = params.getOrElse("inPath",       "data/VARawFiles/")
    val outPath      = params.getOrElse("outPath",      "data/output/")
    val vmPath       = params.getOrElse("vendormaster", "")
    val yearStr      = params.getOrElse("year",         "2003")
    val quarterStr   = params.getOrElse("quarter",      "1")
    val recType      = params.getOrElse("type",         "E").toUpperCase
    val chunkSize    = params.get("chunkSize").map(_.toInt).getOrElse(DEFAULT_CHUNK)
    val dryRun       = params.getOrElse("dryRun", "false").toLowerCase == "true"
    val accountingRule = loadAccountingRule(params.get("accountingRules"), params.get("inPath"), recType)

    val year    = yearStr
    val quarter = quarterStr.toInt
    val yearInt = yearStr.toInt

    // Auto-detect record format from year and type
    val recFormat = autoRecFormat(yearInt, recType)

    // Derive input file name from year/quarter/type
    val shortYr  = yearStr.takeRight(2)
    val ext      = if (yearInt >= 2014) ".csv" else ".txt"
    val suffix   = if (recType == "R") "rev" else s"q${quarter}exp"
    val inFile   = inPath.stripSuffix("/") + "/FY" + shortYr + suffix + ext
    val outFile  = outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + suffix + ".csv"
    val acctDate = quarterToDate(year, quarter)

    println(s"  Input file   : $inFile")
    println(s"  Output file  : $outFile")
    println(s"  Year         : $year  Quarter: $quarter  Type: $recType  RecFormat: $recFormat")
    println(s"  AcctDate     : $acctDate")
    // Load sort spec via SortEngine
    val specParam = params.getOrElse("sortspec", "")
    val specPath = resolveSpecPath(specParam)
    val sortSpec = SortEngine.loadSpec(specPath)
    println(s"  SortSpec     : $specPath  (${sortSpec.size} fields)")
    println(s"  ChunkSize    : $chunkSize")
    println(s"  DryRun       : $dryRun")

    // Load VendorMaster
    val resolvedVmPath = resolveVmPath(vmPath)
    val vendorMap      = loadVendorMaster(resolvedVmPath)
    println(s"  VendorMaster : $resolvedVmPath  (${vendorMap.size} entries)")

    // ── Chunk → spill loop ──────────────────────────────────────────────────
    val tmpDir    = System.getProperty("java.io.tmpdir") + "/sortedJE_spill_" + System.currentTimeMillis()
    new File(tmpDir).mkdirs()
    val spills    = mutable.ArrayBuffer[String]()
    val chunk     = mutable.ArrayBuffer[String]()

    var totalRows  = 0L
    var matchedRows = 0L
    var totalAmt   = BigDecimal(0)
    var jrnlID     = 0

    def flushChunk(): Unit = {
      if (chunk.nonEmpty) {
        val spillPath = SortEngine.spillChunk(SortEngine.sortChunk(chunk.toSeq, sortSpec), tmpDir, spills.size)
        spills += spillPath
        chunk.clear()
        print(s"  Spilled chunk ${spills.size}  ($totalRows rows so far)\r")
      }
    }

    val inF = new File(inFile)
    if (!inF.exists()) {
      System.err.println(s"ERROR: Input file not found: $inFile")
      sys.exit(1)
    }

    val delimiter = if (recFormat == "A" || recFormat == "B" || recFormat == "RA") "\t" else ","

    implicit val codec = scala.io.Codec.UTF8.onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
    val src = Source.fromFile(inFile)(codec)
    try {
      val lines = src.getLines()
      lines.next()  // skip header

      for (line <- lines) {
        val raw = line.trim
        if (raw.nonEmpty) {
          val cols = splitAndStrip(raw, delimiter)
          val pairs = buildJEPairs(cols, recFormat, recType, vendorMap,
                                   accountingRule, year, acctDate, jrnlID + 1)
          if (pairs.nonEmpty) {
            jrnlID += 1
            for ((rowStr, matched, amt) <- pairs) {
              chunk += rowStr
              totalRows += 1
              if (matched) matchedRows += 1
              totalAmt += amt
              if (chunk.size >= chunkSize) flushChunk()
            }
          }
        }
      }
    } finally {
      src.close()
    }

    flushChunk()  // final partial chunk

    println(s"\n  ${spills.size} spill file(s), $totalRows total rows")

    // ── Control totals ───────────────────────────────────────────────────────
    println("\nControl totals:")
    println(f"  Journal lines produced:  $totalRows%,d")
    println(f"  Lines with instID match: $matchedRows%,d  " +
            f"(${if (totalRows > 0) 100 * matchedRows / totalRows else 0}%%)")
    println(f"  Lines without match:     ${totalRows - matchedRows}%,d")
    println(f"  Sum of all amounts:      $totalAmt%.2f  (should be ~0 — debits cancel credits)")

    if (dryRun) {
      println("\n--dryRun: no output written.")
      // Clean up spills
      spills.foreach(p => new File(p).delete())
      new File(tmpDir).delete()
      return
    }

    // ── k-way merge to final output (delegated to SortEngine) ────────────────
    println(s"\nMerging ${spills.size} spill(s) → $outFile ...")
    val rowsWritten = SortEngine.mergeSpills(spills.toSeq, outFile, sortSpec)

    // Clean up spills
    spills.foreach(p => new File(p).delete())
    new File(tmpDir).delete()

    val sizeMb = new File(outFile).length().toDouble / 1024 / 1024
    println(f"Written: $outFile ($sizeMb%.1f MB, $rowsWritten%,d rows)")
    println("STATUS: COMPLETE")
    println("*" * 100)
    println(s"Process End Time: ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }

  // ── Record format auto-detection ──────────────────────────────────────────
  private def autoRecFormat(year: Int, recType: String): String = {
    if (recType == "R") { if (year >= 2014) "RC" else "RA" }
    else if (year >= 2014) "C"
    else if (year >= 2012) "B"
    else "A"
  }

  // ── Date derivation ───────────────────────────────────────────────────────
  private def quarterToDate(year: String, quarter: Int): String = {
    val month = quarter * 3
    f"$year/$month%02d/01"
  }

  // ── Field splitting and stripping ────────────────────────────────────────
  private def splitAndStrip(line: String, delim: String): Array[String] = {
    line.split(delim, -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
  }

  // ── VendorMaster loading (name → instID) ──────────────────────────────────
  private def resolveSpecPath(requested: String): String = {
    val candidates = if (requested.nonEmpty)
      Seq(requested, s"../$requested", DEFAULT_SPEC_1, DEFAULT_SPEC_2)
    else
      Seq(DEFAULT_SPEC_1, DEFAULT_SPEC_2)
    candidates.find(p => new File(p).exists()).getOrElse(requested match {
      case r if r.nonEmpty => r
      case _ => DEFAULT_SPEC_1
    })
  }

  private def loadAccountingRule(requested: Option[String], inputPath: Option[String], recType: String): AccountingRule = {
    val candidates = requested.toSeq ++ inputPath.toSeq.map(_.stripSuffix("/") + "/VAAccountingRules.csv") ++ Seq(
      "data/VAAccountingRules.csv",
      "../data/VAAccountingRules.csv",
      "../../data/VAAccountingRules.csv"
    )
    val path = candidates.find(p => new File(p).exists())
    path.flatMap { rulePath =>
      val source = Source.fromFile(rulePath)
      try {
        source.getLines()
          .filter(line => line.trim.nonEmpty && !line.trim.startsWith("#"))
          .drop(1)
          .map(_.split(",", -1).map(_.trim))
          .find(fields => fields.length >= 10 && fields(0).equalsIgnoreCase(recType))
          .map(fields => AccountingRule(
            fields(0), fields(1), fields(2), fields(3), fields(4), fields(5),
            fields(6), fields(7), fields(8), fields(9)
          ))
      } finally {
        source.close()
      }
    }.getOrElse {
      if (recType == "R")
        AccountingRule("R", "VA_COMPAT", "1", "VA_REVENUE_COMPAT", "REVENUE_RECEIPT", "REV+source", "0000", "zero", "zero", "CASH_CLEARING")
      else
        AccountingRule("E", "VA_COMPAT", "1", "VA_EXPENSE_COMPAT", "EXPENSE_PAYMENT", "EXP+program", "0000", "object", "zero", "CASH_CLEARING")
    }
  }

  private def evaluateExpression(expression: String, objectCode: String,
                                 program: String, source: String): String = expression match {
    case "EXP+object"  => s"EXP$objectCode"
    case "EXP+program" => s"EXP$program"
    case "REV+source"  => s"REV$source"
    case "object"      => objectCode
    case "program"     => program
    case "source"      => source
    case "zero"        => "0000"
    case literal        => literal
  }

  private def resolveVmPath(requested: String): String = {
    val candidates = if (requested.nonEmpty)
      Seq(requested, s"../$requested", DEFAULT_VM_1, s"../$DEFAULT_VM_1", DEFAULT_VM_2, s"../$DEFAULT_VM_2", DEFAULT_VM_3, s"../$DEFAULT_VM_3")
    else
      Seq(DEFAULT_VM_1, s"../$DEFAULT_VM_1", DEFAULT_VM_2, s"../$DEFAULT_VM_2", DEFAULT_VM_3, s"../$DEFAULT_VM_3")
    candidates.find(p => new File(p).exists()).getOrElse(DEFAULT_VM_1)
  }

  private def loadVendorMaster(path: String): Map[String, String] = {
    val vm = mutable.HashMap[String, String]()
    if (!new File(path).exists()) {
      System.err.println(s"WARNING: VendorMaster not found at $path — instIDs will be $UNKNOWN_INST")
      return vm.toMap
    }
    implicit val codec = scala.io.Codec.UTF8.onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
    val src = Source.fromFile(path)(codec)
    try {
      val lines = src.getLines().filter(l => !l.startsWith("#"))
      if (lines.hasNext) {
        val headerRaw = lines.next().stripPrefix("\uFEFF")
        val header = headerRaw.split(",").map(_.trim.stripPrefix("\"").stripSuffix("\""))
        val nameIdx = header.indexOf("instHolderName")
        val idIdx   = header.indexOf("instID")
        if (nameIdx < 0 || idIdx < 0) {
          System.err.println(s"WARNING: VendorMaster missing instHolderName or instID column")
          return vm.toMap
        }
        for (line <- lines) {
          val cols = line.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
          if (cols.length > math.max(nameIdx, idIdx)) {
            val name   = cols(nameIdx).toUpperCase
            val rawId  = cols(idIdx)
            val instId = try { rawId.toLong.formatted("%010d") }
                         catch { case _: NumberFormatException => rawId }
            if (name.nonEmpty && rawId.nonEmpty) vm(name) = instId
          }
        }
      }
    } finally {
      src.close()
    }
    vm.toMap
  }

  // ── Row builder (returns comma-joined 39-field string) ────────────────────
  private def makeRow(instId: String, jrnlId: String, lineId: String,
                      bizEvent: String, srcSys: String, descript: String,
                      agency: String, fund: String, objCode: String,
                      nominalAcct: String, year: String, acctDate: String,
                      transDate: String, amount: String,
                      ruleSetId: String, ruleId: String): String = {
    val descClean = descript.replace(",", " ")
    Seq(
      instId, "0", "0", jrnlId, lineId, bizEvent, srcSys, "Unknown", descClean,
      LEDGER_ID, JRNL_TYPE, BOOK_CODE,
      agency, fund, objCode, PRODUCT_ID, nominalAcct, ALT_ACCOUNT,
      CCY_SRC, CCY_TYPE_SRC, CCY_TGT, CCY_TYPE_TGT,
      year, acctDate, transDate, amount,
      " ", "0", "0", ruleSetId, ruleId, "D", "N", "N", " ", " ", " ", " ", " "
    ).mkString(",")
  }

  // ── JE pair builder — returns 0 or 2 (rowStr, matched, amount) tuples ─────
  private def buildJEPairs(
      cols: Array[String], recFormat: String, recType: String,
      vendorMap: Map[String, String],
      accountingRule: AccountingRule,
      year: String, acctDate: String, seqNo: Int
    ): Seq[(String, Boolean, BigDecimal)] = {

    try {
      if (recType == "E") buildExpensePairs(cols, recFormat, vendorMap, accountingRule, year, acctDate, seqNo)
      else                buildRevenuePairs(cols, recFormat, vendorMap, accountingRule, year, acctDate, seqNo)
    } catch {
      case _: Exception => Seq.empty
    }
  }

  private def buildExpensePairs(
      cols: Array[String], recFormat: String,
      vendorMap: Map[String, String],
      accountingRule: AccountingRule,
      year: String, acctDate: String, seqNo: Int
    ): Seq[(String, Boolean, BigDecimal)] = {

    val (agency, fund, objCode, prog, vendor, rawAmt, transDate) = recFormat match {
      case "A" if cols.length >= 6 =>
        (cols(0), cols(1), cols(2), cols(3), cols(4), cols(5), acctDate)
      case "B" if cols.length >= 6 =>
        val td = if (cols.length > 6) cols(6) else acctDate
        (cols(0), cols(1), cols(2), cols(3), cols(4), cols(5), td)
      case "C" if cols.length >= 6 =>
        val td = if (cols.length > 6) cols(6) else acctDate
        (cols(0), cols(2), cols(3), cols(4), cols(5), cols(1), td)
      case _ => return Seq.empty
    }

    val amt     = BigDecimal(rawAmt.replace(",", ""))
    val instId  = vendorMap.getOrElse(vendor.toUpperCase, UNKNOWN_INST)
    val matched = instId != UNKNOWN_INST
    val jid     = s"ID$seqNo$acctDate"
    val vDesc   = vendor.replace(",", " ")
    val debitAccount = evaluateExpression(accountingRule.debitAccountExpression, objCode, prog, "")
    val creditAccount = evaluateExpression(accountingRule.creditAccountExpression, objCode, prog, "")
    val debitProject = evaluateExpression(accountingRule.debitProjectExpression, objCode, prog, "")
    val creditProject = evaluateExpression(accountingRule.creditProjectExpression, objCode, prog, "")

    Seq(
      (makeRow(instId, jid, "1", accountingRule.businessEventCode, "Payment Sys", vDesc,
               agency, fund, debitProject, debitAccount, year, acctDate, transDate,
               amt.toString(), accountingRule.ruleSetId, accountingRule.ruleId), matched, amt),
      (makeRow(instId, jid, "2", accountingRule.businessEventCode, "Payment Sys", accountingRule.offsetDescription,
               agency, "0000", creditProject, creditAccount, year, acctDate, transDate,
               (-amt).toString(), accountingRule.ruleSetId, accountingRule.ruleId), matched, -amt)
    )
  }

  private def buildRevenuePairs(
      cols: Array[String], recFormat: String,
      vendorMap: Map[String, String],
      accountingRule: AccountingRule,
      year: String, acctDate: String, seqNo: Int
    ): Seq[(String, Boolean, BigDecimal)] = {

    val (agency, fund, src, rawAmt, transDate) = recFormat match {
      case "RA" if cols.length >= 4 =>
        (cols(0), cols(1), cols(2), cols(3), acctDate)
      case "RC" if cols.length >= 4 =>
        val td = if (cols.length > 4) cols(4) else acctDate
        (cols(0), cols(2), cols(3), cols(1), td)
      case _ => return Seq.empty
    }

    val amt     = BigDecimal(rawAmt.replace(",", ""))
    val instId  = vendorMap.getOrElse(agency.toUpperCase, UNKNOWN_INST)
    val matched = instId != UNKNOWN_INST
    val jid     = s"ID${seqNo}${acctDate}REV"
    val debitAccount = evaluateExpression(accountingRule.debitAccountExpression, "", "", src)
    val creditAccount = evaluateExpression(accountingRule.creditAccountExpression, "", "", src)
    val debitProject = evaluateExpression(accountingRule.debitProjectExpression, "", "", src)
    val creditProject = evaluateExpression(accountingRule.creditProjectExpression, "", "", src)

    Seq(
      (makeRow(instId, jid, "1", accountingRule.businessEventCode, "Revenue Sys", "Tax Receipt from Taxpayer",
               agency, fund, debitProject, debitAccount, year, acctDate, transDate,
               amt.toString(), accountingRule.ruleSetId, accountingRule.ruleId), matched, amt),
      (makeRow(instId, jid, "2", accountingRule.businessEventCode, "Revenue Sys", accountingRule.offsetDescription,
               agency, "0000", creditProject, creditAccount, year, acctDate, transDate,
               (-amt).toString(), accountingRule.ruleSetId, accountingRule.ruleId), matched, -amt)
    )
  }
}
