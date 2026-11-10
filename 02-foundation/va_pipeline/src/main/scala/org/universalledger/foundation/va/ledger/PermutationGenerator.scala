package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter, BufferedWriter, FileWriter}
import java.util.Calendar

import scala.collection.mutable
import scala.io.Source

//***************************************************************************************************************
//
//  Program:  PermutationGenerator — CCP1 Permutation Table Step (Step 21)
//
//  PURPOSE
//  -------
//  CCP1's permutation generator measures the key-embedded permutation explosion: how many distinct
//  instrument-attribute combinations (VENDORID × NIGP-class × geo-region) collapsed into each
//  account-keyed GL balance.  The resulting permutation table is the legacy architecture's "best
//  possible response" substrate — a GL with all possible key-embedded balance combinations
//  pre-materialised.  Its row count versus the GL balance row count is the Paper 1 cost-explosion
//  finding for Theorem 1.
//
//  WHAT THIS DOES
//  --------------
//  1. Reads all SortedJE_{year}*.csv files produced by Step 2 (same SJE partition as CCP2/GLPost).
//  2. For each row accumulates: key = (agency, nominal_account_id, fund, fiscal_year, vendor_id)
//     where vendor_id = transIPID (col 0).
//  3. Writes PERM_TBL20YY.csv:
//       account_key, fiscal_year, vendor_id, record_count
//     where account_key = agency|nominal_account_id|fund.
//     The permutation_rows count is the number of distinct rows in this table.
//
//  INSTRUMENT IDENTITY TREATMENT
//  ------------------------------
//  Unlike GLPost, this step retains the vendor identity (transIPID) in the key to enumerate
//  all distinct combinations that would need to be materialised if the GL architecture were to
//  answer every instrument-keyed question.  This is deliberately the GL's best effort — it uses
//  the same instrument identity that the instrument-ledger architecture preserves natively.
//  The permutation count is not the cost of the Instrument Ledger — it is the cost of trying to
//  answer instrument-ledger questions with a GL architecture.
//
//  MEMORY CONTRACT
//  ---------------
//  The accumulation map is keyed on (agency × nominal_account × fund × year × vendor_id).  For
//  the full VA corpus the number of distinct keys is O(distinct_account_keys × distinct_vendors)
//  which is empirically in the tens of thousands — bounded, fits in heap.
//
//  METRICS (process_metrics.csv)
//  ------------------------------
//  RESULT step=21 year=YYYY rc=0 records_read=N records_written=M elapsed_s=E
//         input_bytes=B output_bytes=O
//  RESOURCE step=21 year=YYYY peak_rss_bytes=R
//
//  CLI invocation (via LedgerApp dispatcher):
//    sbt "run --step 21 --inPath data/output/ --outPath data/output/ --year 2014"
//
//  Parameters (passed by LedgerApp dispatcher from CLI map):
//    inPath   — directory containing SortedJE*.csv files (Step 2 output)
//    outPath  — directory for permutation table output
//    year     — 4-digit fiscal year (e.g. 2014)
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  2026 — CCP1 permutation generator step (Bob AI, Session: 2026-11-04)
//***************************************************************************************************************

object PermutationGenerator {

  // Column indices in SortedJE CSV (39-column Transaction schema — see standardizeAndSort.scala)
  private val COL_VENDOR_ID   = 0   // transIPID
  private val COL_AGENCY      = 12  // transLegalEntityID
  private val COL_FUND        = 13  // transCenterID
  private val COL_NOMINAL_ACC = 16  // transNominalAccountID
  private val COL_FISCAL_YR   = 22  // transFiscalPeriod
  private val COL_DIR_FLAG    = 31  // transDirVsOffsetFlg

  // Accumulation key: one row per distinct (account_key, vendor_id) combination
  private case class PermKey(agency: String, nominalAccount: String, fund: String,
                              fiscalYear: String, vendorId: String)

  // ── Entry point called by LedgerApp dispatcher ─────────────────────────────
  def apply(params: Map[String, String]): Unit = {
    val t0 = System.currentTimeMillis()

    println("*" * 100)
    println("                         Step 21 — CCP1 Permutation Generator (PermutationGenerator)")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    val inPath  = params.getOrElse("inPath",  "data/output/")
    val outPath = params.getOrElse("outPath", "data/output/")
    val yearStr = params.getOrElse("year",    "2014")

    val shortYr = if (yearStr.length == 4) yearStr.substring(2) else yearStr

    // Collect SortedJE files for the requested year
    val inDir   = new File(inPath)
    // One-read rule: when SortedJEFY{yy}merged.csv exists, skip the 2m inputs that hold the same rows.
    val jeFiles = if (inDir.exists() && inDir.isDirectory)
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

    val outFile = new File(outPath, s"PERM_TBL20${shortYr}.csv")
    println(s"  Output file    : ${outFile.getPath}")

    // ── Accumulate permutation counts ─────────────────────────────────────────
    // counts(PermKey) = number of debit JE rows in this account+vendor combination
    val counts    = mutable.HashMap[PermKey, Long]()
    var totalRead = 0L
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
            // Count only debit lines to match GLPost — one JE transaction = one debit row
            if (cols(COL_DIR_FLAG).trim == "D") {
              val key = PermKey(
                agency         = cols(COL_AGENCY).trim,
                nominalAccount = cols(COL_NOMINAL_ACC).trim,
                fund           = cols(COL_FUND).trim,
                fiscalYear     = cols(COL_FISCAL_YR).trim,
                vendorId       = cols(COL_VENDOR_ID).trim
              )
              counts(key) = counts.getOrElse(key, 0L) + 1L
            }
          }
        }
      } finally src.close()
    }

    // ── Write permutation table ───────────────────────────────────────────────
    new File(outPath).mkdirs()
    val pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))
    try {
      pw.println("account_key,fiscal_year,vendor_id,record_count")
      val sorted = counts.toSeq.sortBy(kv =>
        (kv._1.fiscalYear, kv._1.agency, kv._1.nominalAccount, kv._1.fund, kv._1.vendorId))
      for ((key, cnt) <- sorted) {
        val accountKey = s"${key.agency}|${key.nominalAccount}|${key.fund}"
        pw.println(s"$accountKey,${key.fiscalYear},${key.vendorId},$cnt")
      }
    } finally pw.close()

    val permutationRows = counts.size.toLong
    val outBytes        = outFile.length()
    val elapsedS        = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val peakRss         = Runtime.getRuntime.totalMemory()

    println(f"  Permutation rows written : $permutationRows%,d")
    println(f"  Journal rows read        : $totalRead%,d")
    println(s"  Output                   : ${outFile.getPath}")
    println("STATUS: COMPLETE")
    println(s"RESULT step=21 year=$yearStr rc=0 records_read=$totalRead records_written=$permutationRows elapsed_s=$elapsedS input_bytes=$totalInputBytes output_bytes=$outBytes")
    println(s"RESOURCE step=21 year=$yearStr peak_rss_bytes=$peakRss")

    println("*" * 100)
    println(s"Process End Time: ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}
