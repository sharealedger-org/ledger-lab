package org.universalledger.foundation.va.ledger

//***************************************************************************************************************
//
//  Program:  CKBViews — Step 5v: Extract-Time Summarization Views from the SortedJE Stream
//
//  Workload declaration (AGENTIC_ARCHITECTURE_GATES.md §1):
//
//    Research question:
//      Do ordering seasonality, vendor survival, commodity mix, and agency payment patterns
//      carry predictive signal for Paper 2 research questions (AIM1–AIM4)?
//      Produce the four small feature tables that Python statistical fitting (Sub-Tasks 4–5)
//      consumes without requiring it to scan the 133M-record source corpus.
//
//    Logical owner: 02-foundation/va_pipeline — runs after Step 3 (post) or Step 2/2m as a
//      separate view-extraction pass.  Step ID: 5v (distinct from legacy step 5 dataAggregation).
//
//    Physical process: single pass over SortedJEFY*.csv files (same 39-field Transaction rows
//      produced by Step 2/2m).  Four bounded aggregation maps; no additional source scan.
//
//    Input schema: 39-field comma-delimited Transaction rows.
//      col  0  transIPID              — vendor / instrument ID
//      col  5  transBusinessEventCode — "COMMITMENT" (PO) or payment event code
//      col 12  transLegalEntityID     — agency
//      col 14  transProjectID         — NIGP code for PO rows; object code for payments
//      col 22  transFiscalPeriod      — fiscal year (4-digit string)
//      col 24  transTransDate         — order date or accounting date
//      col 25  transTransAmount       — signed amount (debit positive, credit negative)
//
//    Output schema (analytical outputs — not canonical state, not posted to ledger):
//      view_monthly_order_volume.csv       : fiscal_period, nigp_class_3digit, po_line_count,
//                                            total_committed_amount
//      view_vendor_year_survival.csv       : vendor_id, vendor_surrogate_key, fiscal_year,
//                                            first_order_year, present_next_year, total_amount,
//                                            po_count
//        vendor_surrogate_key = first 8 hex chars of SHA-256(vendor_id).
//        Used by Python fitting scripts as a semantically-neutral shuffle/split key
//        (vendorSurrogateKey mod 5 → train/validate/test) — the VA analog of the insurance
//        corpus surrogate key (Paper 2 §5.2).
//      view_agency_year_commodity_mix.csv  : agency, fiscal_year, nigp_class_3digit,
//                                            po_count, total_committed_amount, revision_count
//      view_agency_year_payment_totals.csv : agency, fiscal_year, total_payment_amount,
//                                            payment_count
//
//    Materialization: single scan; four bounded in-memory maps; no spill at fixture scale.
//    Memory contract: O(distinct keys per view) ≪ O(N source rows).
//
//    Controls emitted:
//      RESULT step=5v year=... rc=0 records_read=N view_monthly_N view_vendor_N
//             view_agency_commodity_N view_agency_payment_N elapsed_s=N
//      RESOURCE step=5v year=... peak_rss_bytes=N
//
//  Canonical state vs analytical output: these files are analytical outputs only.
//  They are not SJE, are not posted, and do not require balancing controls.
//
//  *(c) Copyright IBM Corporation. 2018
//  * SPDX-License-Identifier: Apache-2.0
//  * By Kip Twitchell
//  2026 — Sub-Task 3 implementation (Bob AI, Session N+1)
//***************************************************************************************************************

import java.io.{File, PrintWriter}
import java.security.MessageDigest
import java.util.Calendar

import scala.collection.mutable
import scala.io.Source

object CKBViews {

  // ── Accumulator types ─────────────────────────────────────────────────────

  // View A: monthly order volume
  //   key: (fiscalPeriod, nigpClass3)
  private case class MonthlyVolumeAcc(var poLineCount: Long = 0L, var totalAmt: BigDecimal = 0)

  // View B: vendor-year survival panel
  //   key: (vendorId, fiscalYear)
  private case class VendorYearAcc(
    var firstOrderYear: String = "",
    var totalAmt: BigDecimal = 0,
    var poCount: Long = 0L
  )

  // View C: agency-year commodity mix
  //   key: (agency, fiscalYear, nigpClass3)
  private case class AgencyCommodityAcc(
    var poCount: Long = 0L,
    var totalAmt: BigDecimal = 0,
    var revisionCount: Long = 0L    // rows whose transCommitmentID contains a "-V" suffix
  )

  // View D: agency-year payment totals
  //   key: (agency, fiscalYear)
  private case class AgencyPaymentAcc(var totalAmt: BigDecimal = 0, var paymentCount: Long = 0L)

  // ── Entry point called by LedgerApp dispatcher ─────────────────────────────
  def apply(params: Map[String, String]): Unit = {
    val t0 = System.currentTimeMillis()

    println("*" * 100)
    println("                     Step 5v — CKB View Extraction (CKBViews)")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    val inPath  = params.getOrElse("inPath",  "data/output/")
    val outPath = params.getOrElse("outPath", "data/output/")
    val yearStr = params.getOrElse("year",    "")

    // ── Aggregation maps ───────────────────────────────────────────────────
    val monthlyMap   = mutable.HashMap[(String, String), MonthlyVolumeAcc]()
    val vendorMap    = mutable.HashMap[(String, String), VendorYearAcc]()
    val commodityMap = mutable.HashMap[(String, String, String), AgencyCommodityAcc]()
    val paymentMap   = mutable.HashMap[(String, String), AgencyPaymentAcc]()

    // ── Collect all years seen (for vendor-year survival "present_next_year") ─
    val vendorYears = mutable.HashMap[String, mutable.SortedSet[String]]()

    var totalRecordsRead = 0L

    // ── Resolve input file list ────────────────────────────────────────────
    val inputDir = new File(inPath)
    val jeFiles: List[File] = if (inputDir.exists() && inputDir.isDirectory) {
      val allJE = inputDir.listFiles()
        .filter(f => f.isFile && f.getName.startsWith("SortedJE"))
        .toList
        .sortBy(_.getName)
      if (yearStr.nonEmpty) {
        val shortYr = if (yearStr.length == 4) yearStr.substring(2, 4) else yearStr
        allJE.filter(_.getName.contains(s"FY$shortYr"))
      } else {
        allJE
      }
    } else {
      System.err.println(s"WARNING: Input directory not found: $inPath — no SortedJE files processed.")
      List.empty
    }

    println(s"  Input directory : $inPath")
    println(s"  Output directory: $outPath")
    println(s"  Year filter     : ${if (yearStr.nonEmpty) yearStr else "(all)"}")
    println(s"  SortedJE files  : ${jeFiles.size}")

    // ── Main accumulation loop — one pass per file ─────────────────────────
    for (jeFile <- jeFiles) {
      println(s"  Processing: ${jeFile.getName}")
      implicit val codec = scala.io.Codec.UTF8
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
      val src = Source.fromFile(jeFile)(codec)
      try {
        for (line <- src.getLines()) {
          val raw = line.trim
          if (raw.nonEmpty) {
            val cols = raw.split(",", -1)
            if (cols.length >= 26) {
              totalRecordsRead += 1
              val vendorId  = cols(0)
              val commitId  = if (cols.length > 2) cols(2) else ""
              val bizEvent  = cols(5)
              val agency    = cols(12)
              val nigpCode  = cols(14)
              val fiscYear  = cols(22)
              val transDate = cols(24)  // YYYY/MM/DD or MM/DD/YYYY
              val rawAmt    = cols(25)

              val amt = try BigDecimal(rawAmt) catch { case _: Exception => BigDecimal(0) }

              // Fiscal period = "YYYY/MM" derived from transDate (only debit rows counted)
              val fiscMonth = deriveFiscalMonth(transDate, fiscYear)

              val nigp3 = if (nigpCode.length >= 3) nigpCode.take(3) else nigpCode.padTo(3, '0')

              // Only accumulate debit rows (positive sign) to avoid double-counting.
              // Credits are structural offsets — they carry no additional information.
              val isDebit = amt >= BigDecimal(0)

              if (bizEvent == "COMMITMENT" && isDebit) {
                // ── View A: monthly order volume ─────────────────────────
                val keyA = (fiscMonth, nigp3)
                val accA = monthlyMap.getOrElseUpdate(keyA, MonthlyVolumeAcc())
                accA.poLineCount += 1
                accA.totalAmt += amt

                // ── View B: vendor-year survival ─────────────────────────
                val keyB = (vendorId, fiscYear)
                val accB = vendorMap.getOrElseUpdate(keyB, VendorYearAcc(firstOrderYear = fiscYear))
                if (accB.firstOrderYear.isEmpty || fiscYear < accB.firstOrderYear)
                  accB.firstOrderYear = fiscYear
                accB.totalAmt += amt
                accB.poCount  += 1

                // Track all years this vendor appears in (for present_next_year)
                vendorYears.getOrElseUpdate(vendorId, mutable.SortedSet[String]()) += fiscYear

                // ── View C: agency-year commodity mix ─────────────────────
                val keyC = (agency, fiscYear, nigp3)
                val accC = commodityMap.getOrElseUpdate(keyC, AgencyCommodityAcc())
                accC.poCount  += 1
                accC.totalAmt += amt
                // A PO is a revision if its commitmentID contains "-V" (e.g. "A501_0000200268-V2")
                if (commitId.contains("-V")) accC.revisionCount += 1
              }

              if (bizEvent != "COMMITMENT" && isDebit) {
                // ── View D: agency-year payment totals ────────────────────
                val keyD = (agency, fiscYear)
                val accD = paymentMap.getOrElseUpdate(keyD, AgencyPaymentAcc())
                accD.totalAmt    += amt
                accD.paymentCount += 1
              }
            }
          }
        }
      } finally {
        src.close()
      }
    }

    // ── Write output files ─────────────────────────────────────────────────
    new File(outPath).mkdirs()

    val viewAFile = new File(outPath, "view_monthly_order_volume.csv")
    val viewBFile = new File(outPath, "view_vendor_year_survival.csv")
    val viewCFile = new File(outPath, "view_agency_year_commodity_mix.csv")
    val viewDFile = new File(outPath, "view_agency_year_payment_totals.csv")

    // View A
    val writerA = new PrintWriter(viewAFile)
    try {
      writerA.println("fiscal_period,nigp_class_3digit,po_line_count,total_committed_amount")
      for (((fp, nigp3), acc) <- monthlyMap.toSeq.sortBy(_._1))
        writerA.println(s"$fp,$nigp3,${acc.poLineCount},${acc.totalAmt}")
    } finally writerA.close()

    // View B — compute present_next_year flag and vendor_surrogate_key
    val writerB = new PrintWriter(viewBFile)
    try {
      writerB.println("vendor_id,vendor_surrogate_key,fiscal_year,first_order_year,present_next_year,total_amount,po_count")
      for (((vid, fy), acc) <- vendorMap.toSeq.sortBy(_._1)) {
        val yearsForVendor = vendorYears.getOrElse(vid, mutable.SortedSet.empty[String])
        // "present_next_year" = 1 if there is any year > fy in this vendor's year set
        val presentNextYear = if (yearsForVendor.exists(_ > fy)) 1 else 0
        val surrogateKey    = vendorSurrogateKey(vid)
        writerB.println(s"$vid,$surrogateKey,$fy,${acc.firstOrderYear},$presentNextYear,${acc.totalAmt},${acc.poCount}")
      }
    } finally writerB.close()

    // View C
    val writerC = new PrintWriter(viewCFile)
    try {
      writerC.println("agency,fiscal_year,nigp_class_3digit,po_count,total_committed_amount,revision_count")
      for (((ag, fy, n3), acc) <- commodityMap.toSeq.sortBy(_._1))
        writerC.println(s"$ag,$fy,$n3,${acc.poCount},${acc.totalAmt},${acc.revisionCount}")
    } finally writerC.close()

    // View D
    val writerD = new PrintWriter(viewDFile)
    try {
      writerD.println("agency,fiscal_year,total_payment_amount,payment_count")
      for (((ag, fy), acc) <- paymentMap.toSeq.sortBy(_._1))
        writerD.println(s"$ag,$fy,${acc.totalAmt},${acc.paymentCount}")
    } finally writerD.close()

    // ── Emit structured metrics ────────────────────────────────────────────
    val elapsedS    = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val peakRss     = Runtime.getRuntime.totalMemory()
    val nMonthly    = monthlyMap.size
    val nVendor     = vendorMap.size
    val nCommodity  = commodityMap.size
    val nPayment    = paymentMap.size

    println("*" * 100)
    println(s"Files processed     : ${jeFiles.size}")
    println(s"Records read        : $totalRecordsRead")
    println(s"View A rows (monthly order volume)      : $nMonthly")
    println(s"View B rows (vendor-year survival)      : $nVendor")
    println(s"View C rows (agency-year commodity mix) : $nCommodity")
    println(s"View D rows (agency-year payments)      : $nPayment")
    println(s"RESULT step=5v year=$yearStr rc=0 records_read=$totalRecordsRead view_monthly=$nMonthly view_vendor=$nVendor view_agency_commodity=$nCommodity view_agency_payment=$nPayment elapsed_s=$elapsedS")
    println(s"RESOURCE step=5v year=$yearStr peak_rss_bytes=$peakRss")
    println("Process End Time: " + Calendar.getInstance.getTime)
    println("*" * 100)
  }

  // ── Compute vendor surrogate key ───────────────────────────────────────────
  //
  //  Returns the first 8 hex characters of the SHA-256 digest of the vendor ID string.
  //  This produces a value that is:
  //    - deterministic (same vendor always gets the same key)
  //    - semantically neutral (carries no registration-era or business ordering)
  //    - suitable for mod-N train/validate/test splits without explicit shuffling
  //
  //  Used by Python fitting scripts as: vendorSurrogateKey mod 5 → partition index.
  private[ledger] def vendorSurrogateKey(vendorId: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes  = digest.digest(vendorId.getBytes("UTF-8"))
    bytes.take(4).map(b => f"${b & 0xff}%02x").mkString
  }

  // ── Derive "YYYY/MM" fiscal period from a transaction date string ──────────
  //
  //  Handles two formats observed in the SortedJE stream:
  //    "YYYY/MM/DD" — standard SortedJE acctDate format (standardizeAndSort)
  //    "MM/DD/YYYY" — ORDEREDDATE format from PO source files
  //  Falls back to the record's fiscal year with "/01" when the date is unparseable.
  private[ledger] def deriveFiscalMonth(transDate: String, fiscYear: String): String = {
    val fallback = s"$fiscYear/01"
    if (transDate.isEmpty) return fallback
    val parts = transDate.split("/")
    if (parts.length != 3) return fallback
    if (parts(0).length == 4) {
      // YYYY/MM/DD
      s"${parts(0)}/${parts(1)}"
    } else if (parts(2).length == 4) {
      // MM/DD/YYYY
      s"${parts(2)}/${parts(0)}"
    } else {
      fallback
    }
  }
}
