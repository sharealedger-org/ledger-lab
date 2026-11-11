package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter, BufferedWriter, FileWriter}
import java.util.Calendar

import scala.collection.mutable
import scala.io.Source

//***************************************************************************************************************
//
//  Program:  GLPost — CCP1 GL Post Step (Step 20)
//
//  PURPOSE
//  -------
//  CCP1 is the status-quo General Ledger architecture baseline.  Every institution already runs
//  something like this: sort events, accumulate to account-keyed balances, discard instrument
//  identity.  This step measures the cost of that exactly.
//
//  WHAT THIS DOES
//  --------------
//  1. Reads all SortedJE_{year}*.csv files produced by Step 2 (same SJE partition as CCP2).
//  2. Accumulates account-keyed balances:
//       key = (transLegalEntityID, transCenterID, transProjectID, transNominalAccountID,
//              transFiscalPeriod, transDirVsOffsetFlg)
//           = agency, fund, object, nominal account, period, debit/offset flag
//       value = SUM(transTransAmount)   [every line — debit and offset/credit]
//     This is the typical government GL key.  Instrument identity (transIPID) is intentionally
//     NOT in the key — this is the defining property of the GL architecture.
//     transDirVsOffsetFlg is a GL field: "D" on the debit line, "O" on the offset line (it was "D"
//     on both lines before 2026-11-11, which is why an earlier debit-only filter selected nothing).
//     Both sides are real GL postings and both are kept, separated by the flag.
//  3. Writes GL20YY.csv:
//       agency, nominal_account_id, fund, object_id, fiscal_year, dir_flag, total_amount
//
//  MEMORY CONTRACT
//  ---------------
//  The accumulation map is keyed on (agency × fund × object × nominal_account × year).  Measured
//  on the VA corpus (FY03–FY16, before object was added to the key) it held about 275K–310K
//  entries per year; adding object can only increase that.  The map is bounded by the key
//  cardinality, not the row count.  No external sort is required.
//
//  METRICS (process_metrics.csv)
//  ------------------------------
//  RESULT step=20 year=YYYY rc=0 records_read=N records_written=M elapsed_s=E
//         input_bytes=B output_bytes=O net_amount=X
//  net_amount is the sum of all amounts written; it is 0.00 when the SJE pairs balance.
//  RESOURCE step=20 year=YYYY jvm_heap_bytes=R
//
//  CLI invocation (via LedgerApp dispatcher):
//    sbt "run --step 20 --inPath data/output/ --outPath data/output/ --year 2014"
//
//  Parameters (passed by LedgerApp dispatcher from CLI map):
//    inPath   — directory containing SortedJE*.csv files (Step 2 output)
//    outPath  — directory for GL output
//    year     — 4-digit fiscal year (e.g. 2014)
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  2026 — CCP1 GL post step (Bob AI, Session: 2026-11-04)
//***************************************************************************************************************

object GLPost {

  // Column indices in SortedJE CSV (39-column Transaction schema — see standardizeAndSort.scala)
  private val COL_AGENCY      = 12  // transLegalEntityID
  private val COL_FUND        = 13  // transCenterID
  private val COL_OBJECT      = 14  // transProjectID
  private val COL_NOMINAL_ACC = 16  // transNominalAccountID
  private val COL_FISCAL_YR   = 22  // transFiscalPeriod
  private val COL_AMOUNT      = 25  // transTransAmount
  private val COL_DIR_FLAG    = 31  // transDirVsOffsetFlg — GL key field (D = debit, O = offset)

  // Case class for accumulation key
  private case class GLKey(agency: String, nominalAccount: String, fund: String, objectCode: String,
                           fiscalYear: String, dirFlag: String)

  // ── Entry point called by LedgerApp dispatcher ─────────────────────────────
  def apply(params: Map[String, String]): Unit = {
    val t0 = System.currentTimeMillis()

    println("*" * 100)
    println("                         Step 20 — CCP1 GL Post (GLPost)")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    val inPath  = params.getOrElse("inPath",  "data/output/")
    val outPath = params.getOrElse("outPath", "data/output/")
    val yearStr = params.getOrElse("year",    "2014")

    // Derive short year for filename matching (e.g. "2014" → "14")
    val shortYr = if (yearStr.length == 4) yearStr.substring(2) else yearStr

    // Collect SortedJE files for the requested year
    val inDir    = new File(inPath)
    // One-read rule: when SortedJEFY{yy}merged.csv exists, skip the 2m inputs that hold the same rows.
    val jeFiles  = if (inDir.exists() && inDir.isDirectory)
      SortedJEInputs.oneRead(
        inDir.listFiles().filter(_.isFile)
          .filter(f => f.getName.startsWith(s"SortedJEFY$shortYr") && f.getName.endsWith(".csv"))
          .sortBy(_.getName)
          .toSeq)
    else Seq.empty

    if (jeFiles.isEmpty) {
      System.err.println(s"ERROR: No SortedJEFY${shortYr}*.csv files found in $inPath")
      sys.exit(1)
    }
    println(s"  SortedJE files : ${jeFiles.map(_.getName).mkString(", ")}")

    val outFile = new File(outPath, s"GL20${shortYr}.csv")
    println(s"  Output file    : ${outFile.getPath}")

    // ── Accumulate account-keyed balances ─────────────────────────────────────
    val balances   = mutable.HashMap[GLKey, BigDecimal]()
    var netAmount  = BigDecimal(0)
    var totalRead  = 0L
    var totalInputBytes = 0L

    for (jeFile <- jeFiles) {
      totalInputBytes += jeFile.length()
      implicit val codec = scala.io.Codec.UTF8
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
      val src = Source.fromFile(jeFile)(codec)
      try {
        for (line <- src.getLines()) {
          val cols = line.split(",", -1)
          if (cols.length > COL_DIR_FLAG) {
            totalRead += 1
            val key = GLKey(
              agency         = cols(COL_AGENCY).trim,
              nominalAccount = cols(COL_NOMINAL_ACC).trim,
              fund           = cols(COL_FUND).trim,
              objectCode     = cols(COL_OBJECT).trim,
              fiscalYear     = cols(COL_FISCAL_YR).trim,
              dirFlag        = cols(COL_DIR_FLAG).trim
            )
            val amt = try BigDecimal(cols(COL_AMOUNT).trim) catch { case _: NumberFormatException => BigDecimal(0) }
            balances(key) = balances.getOrElse(key, BigDecimal(0)) + amt
            netAmount += amt
          }
        }
      } finally src.close()
    }

    // ── Write GL output ───────────────────────────────────────────────────────
    new File(outPath).mkdirs()
    val pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))
    try {
      pw.println("agency,nominal_account_id,fund,object_id,fiscal_year,dir_flag,total_amount")
      // Sort output by key for deterministic output
      val sorted = balances.toSeq.sortBy(kv =>
        (kv._1.fiscalYear, kv._1.agency, kv._1.nominalAccount, kv._1.fund, kv._1.objectCode, kv._1.dirFlag))
      for ((key, total) <- sorted) {
        pw.println(s"${key.agency},${key.nominalAccount},${key.fund},${key.objectCode},${key.fiscalYear},${key.dirFlag},$total")
      }
    } finally pw.close()

    val totalWritten = balances.size.toLong
    val outBytes     = outFile.length()
    val elapsedS     = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val peakRss      = Runtime.getRuntime.totalMemory()

    println(f"  GL balance rows written : $totalWritten%,d")
    println(f"  Journal rows read       : $totalRead%,d")
    println(s"  Output                  : ${outFile.getPath}")
    println("STATUS: COMPLETE")
    println(s"RESULT step=20 year=$yearStr rc=0 records_read=$totalRead records_written=$totalWritten elapsed_s=$elapsedS input_bytes=$totalInputBytes output_bytes=$outBytes net_amount=${netAmount.bigDecimal.toPlainString}")
    println(s"RESOURCE step=20 year=$yearStr jvm_heap_bytes=$peakRss")

    println("*" * 100)
    println(s"Process End Time: ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}
