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
//      col  2  transCommitmentID      — PONUMBER for PO rows; "0" for payments
//      col  5  transBusinessEventCode — "COMMITMENT" (PO) or payment event code
//      col 12  transLegalEntityID     — agency
//      col 13  transCenterID          — fund code (payments); "0000" (PO rows)
//      col 14  transProjectID         — NIGP code for PO rows; object code for payments
//      col 22  transFiscalPeriod      — fiscal year (4-digit string)
//      col 24  transTransDate         — order date or accounting date
//      col 25  transTransAmount       — signed amount (debit positive, credit negative)
//
//    Output schema (analytical outputs — not canonical state, not posted to ledger):
//
//    View A  view_monthly_order_volume.csv
//      key  : (fiscal_period, nigp_class_3digit, agency)
//      cols : fiscal_period, nigp_class_3digit, agency, po_line_count,
//             total_committed_amount, revision_count
//      notes: agency added to key so AIM4 can produce per-agency Q4 seasonality flags.
//             revision_count enables intra-year revision-surge detection.
//
//    View B  view_vendor_year_survival.csv
//      key  : (vendor_id, fiscal_year)
//      cols : vendor_id, vendor_surrogate_key, fiscal_year, first_order_year,
//             present_next_year, total_amount, po_count,
//             nigp_class_modal, agency_count, distinct_nigp_classes
//      notes:
//        vendor_surrogate_key = first 8 hex chars of SHA-256(vendor_id).
//          Used as semantically-neutral shuffle/split key for ML train/test splits:
//          vendorSurrogateKey mod 5 → partition index (VA analog of insurance corpus key §5.2).
//        nigp_class_modal = most frequent 3-digit NIGP class this vendor-year.
//          Captures commodity identity; enables AIM3 year-over-year commodity class shift.
//        agency_count = distinct agencies served by this vendor in this year.
//          Measures market concentration; feeds AIM3 agency-dependency feature.
//        distinct_nigp_classes = count of distinct 3-digit NIGP classes this vendor-year.
//          Measures product breadth; extension point for Papers 3–7 allocation rules.
//
//    View C  view_agency_year_commodity_mix.csv
//      key  : (agency, fiscal_year, nigp_class_3digit)
//      cols : agency, fiscal_year, nigp_class_3digit, po_count,
//             total_committed_amount, revision_count,
//             vendor_count, min_order_amount, max_order_amount
//      notes:
//        vendor_count = distinct vendors supplying this agency-year-class.
//          Enables AIM2 supply-concentration feature.
//        min_order_amount, max_order_amount = range of individual line amounts.
//          Proxy for order-size distribution without requiring sort; enables AIM2 median proxy.
//
//    View D  view_agency_year_payment_totals.csv
//      key  : (agency, fiscal_year, fund_id, object_code)
//      cols : agency, fiscal_year, fund_id, object_code,
//             total_payment_amount, payment_count
//      notes:
//        fund_id = transCenterID (col 13); populated on payment rows.
//        object_code = transProjectID (col 14); populated on payment rows.
//        Finer key enables AIM2 Layer 2 baseline with object-code proxy for commodity.
//        fund_id is the natural allocation partition for Paper 3 allocation driver tables.
//
//    Materialization: single scan; four bounded in-memory maps; no spill at fixture scale.
//    Memory contract: O(distinct keys per view) ≪ O(N source rows).
//    View B accumulates per-vendor-year sub-maps for nigp_class frequency and agency set;
//    these are also O(distinct values per vendor-year) and collapse to scalars at write time.
//
//    Controls emitted:
//      RESULT step=5v year=... rc=0 records_read=N view_monthly_N view_vendor_N
//             view_agency_commodity_N view_agency_payment_N elapsed_s=N
//      RESOURCE step=5v year=... peak_rss_bytes=N
//
//  Canonical state vs analytical output: these files are analytical outputs only.
//  They are not SJE, are not posted, and do not require balancing controls.
//
//  Schema design session: 2026-11-04. See SESSION_HANDOFF.md for design rationale.
//  Three-question design principle per view (RESEARCH_STRATEGY.md §View schema design):
//    (a) minimum for paper 2 AIM model
//    (b) cross-model interaction features (AIM3→AIM2, AIM4→AIM2)
//    (c) extension point for papers 3–7
//
//  *(c) Copyright IBM Corporation. 2018
//  * SPDX-License-Identifier: Apache-2.0
//  * By Kip Twitchell
//  2026 — Sub-Task 3 implementation (Bob AI, Session N+1)
//  2026 — Enriched view schemas (Bob AI, Session N+2)
//***************************************************************************************************************

import java.io.{File, PrintWriter}
import java.security.MessageDigest
import java.util.Calendar

import scala.collection.mutable
import scala.io.Source

object CKBViews {

  // ── Accumulator types ─────────────────────────────────────────────────────

  // View A: monthly order volume
  //   key: (fiscalPeriod, nigpClass3, agency)
  private case class MonthlyVolumeAcc(
    var poLineCount:    Long       = 0L,
    var totalAmt:       BigDecimal = 0,
    var revisionCount:  Long       = 0L
  )

  // View B: vendor-year survival panel
  //   key: (vendorId, fiscalYear)
  //   nigpClassFreq: mutable sub-map for computing modal class at write time
  //   agencySet:     mutable set for counting distinct agencies at write time
  private case class VendorYearAcc(
    var firstOrderYear: String                        = "",
    var totalAmt:       BigDecimal                    = 0,
    var poCount:        Long                          = 0L,
    nigpClassFreq:      mutable.HashMap[String, Long] = mutable.HashMap.empty,
    agencySet:          mutable.HashSet[String]        = mutable.HashSet.empty
  )

  // View C: agency-year commodity mix
  //   key: (agency, fiscalYear, nigpClass3)
  //   vendorSet: mutable set for counting distinct vendors at write time
  private case class AgencyCommodityAcc(
    var poCount:       Long                 = 0L,
    var totalAmt:      BigDecimal           = 0,
    var revisionCount: Long                 = 0L,
    var minAmt:        BigDecimal           = BigDecimal(Long.MaxValue),
    var maxAmt:        BigDecimal           = BigDecimal(Long.MinValue),
    vendorSet:         mutable.HashSet[String] = mutable.HashSet.empty
  )

  // View D: agency-year payment totals
  //   key: (agency, fiscalYear, fundId, objectCode)
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
    val monthlyMap   = mutable.HashMap[(String, String, String), MonthlyVolumeAcc]()
    val vendorMap    = mutable.HashMap[(String, String), VendorYearAcc]()
    val commodityMap = mutable.HashMap[(String, String, String), AgencyCommodityAcc]()
    val paymentMap   = mutable.HashMap[(String, String, String, String), AgencyPaymentAcc]()

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
              val vendorId   = cols(0)
              val commitId   = if (cols.length > 2)  cols(2)  else ""
              val bizEvent   = cols(5)
              val agency     = cols(12)
              val fundId     = cols(13)
              val nigpCode   = cols(14)
              val fiscYear   = cols(22)
              val transDate  = cols(24)  // YYYY/MM/DD or MM/DD/YYYY
              val rawAmt     = cols(25)

              val amt = try BigDecimal(rawAmt) catch { case _: Exception => BigDecimal(0) }

              // Fiscal period = "YYYY/MM" derived from transDate (only debit rows counted)
              val fiscMonth = deriveFiscalMonth(transDate, fiscYear)

              val nigp3 = if (nigpCode.length >= 3) nigpCode.take(3) else nigpCode.padTo(3, '0')

              // Only accumulate debit rows (positive sign) to avoid double-counting.
              // Credits are structural offsets — they carry no additional information.
              val isDebit = amt >= BigDecimal(0)

              if (bizEvent == "COMMITMENT" && isDebit) {
                // ── View A: monthly order volume ─────────────────────────
                val keyA = (fiscMonth, nigp3, agency)
                val accA = monthlyMap.getOrElseUpdate(keyA, MonthlyVolumeAcc())
                accA.poLineCount += 1
                accA.totalAmt    += amt
                if (commitId.contains("-V")) accA.revisionCount += 1

                // ── View B: vendor-year survival ─────────────────────────
                val keyB = (vendorId, fiscYear)
                val accB = vendorMap.getOrElseUpdate(keyB, VendorYearAcc(firstOrderYear = fiscYear))
                if (accB.firstOrderYear.isEmpty || fiscYear < accB.firstOrderYear)
                  accB.firstOrderYear = fiscYear
                accB.totalAmt += amt
                accB.poCount  += 1
                accB.nigpClassFreq(nigp3) = accB.nigpClassFreq.getOrElse(nigp3, 0L) + 1L
                accB.agencySet += agency

                // Track all years this vendor appears in (for present_next_year)
                vendorYears.getOrElseUpdate(vendorId, mutable.SortedSet[String]()) += fiscYear

                // ── View C: agency-year commodity mix ─────────────────────
                val keyC = (agency, fiscYear, nigp3)
                val accC = commodityMap.getOrElseUpdate(keyC, AgencyCommodityAcc())
                accC.poCount  += 1
                accC.totalAmt += amt
                if (commitId.contains("-V")) accC.revisionCount += 1
                if (amt < accC.minAmt) accC.minAmt = amt
                if (amt > accC.maxAmt) accC.maxAmt = amt
                accC.vendorSet += vendorId
              }

              if (bizEvent != "COMMITMENT" && isDebit) {
                // ── View D: agency-year payment totals ────────────────────
                val keyD = (agency, fiscYear, fundId, nigpCode)
                val accD = paymentMap.getOrElseUpdate(keyD, AgencyPaymentAcc())
                accD.totalAmt     += amt
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
      writerA.println("fiscal_period,nigp_class_3digit,agency,po_line_count,total_committed_amount,revision_count")
      for (((fp, nigp3, ag), acc) <- monthlyMap.toSeq.sortBy(_._1))
        writerA.println(s"$fp,$nigp3,$ag,${acc.poLineCount},${acc.totalAmt},${acc.revisionCount}")
    } finally writerA.close()

    // View B — compute present_next_year flag, vendor_surrogate_key, and derived scalars
    val writerB = new PrintWriter(viewBFile)
    try {
      writerB.println(
        "vendor_id,vendor_surrogate_key,fiscal_year,first_order_year,present_next_year," +
        "total_amount,po_count,nigp_class_modal,agency_count,distinct_nigp_classes"
      )
      for (((vid, fy), acc) <- vendorMap.toSeq.sortBy(_._1)) {
        val yearsForVendor   = vendorYears.getOrElse(vid, mutable.SortedSet.empty[String])
        val presentNextYear  = if (yearsForVendor.exists(_ > fy)) 1 else 0
        val surrogateKey     = vendorSurrogateKey(vid)
        val modalClass       = if (acc.nigpClassFreq.isEmpty) "" else acc.nigpClassFreq.maxBy(_._2)._1
        val agencyCount      = acc.agencySet.size
        val distinctClasses  = acc.nigpClassFreq.size
        writerB.println(
          s"$vid,$surrogateKey,$fy,${acc.firstOrderYear},$presentNextYear," +
          s"${acc.totalAmt},${acc.poCount},$modalClass,$agencyCount,$distinctClasses"
        )
      }
    } finally writerB.close()

    // View C
    val writerC = new PrintWriter(viewCFile)
    try {
      writerC.println(
        "agency,fiscal_year,nigp_class_3digit,po_count,total_committed_amount,revision_count," +
        "vendor_count,min_order_amount,max_order_amount"
      )
      for (((ag, fy, n3), acc) <- commodityMap.toSeq.sortBy(_._1)) {
        // Guard min/max against the initial sentinel values (no rows accumulated)
        val minAmt = if (acc.poCount == 0L) BigDecimal(0) else acc.minAmt
        val maxAmt = if (acc.poCount == 0L) BigDecimal(0) else acc.maxAmt
        writerC.println(
          s"$ag,$fy,$n3,${acc.poCount},${acc.totalAmt},${acc.revisionCount}," +
          s"${acc.vendorSet.size},$minAmt,$maxAmt"
        )
      }
    } finally writerC.close()

    // View D — key now (agency, fiscal_year, fund_id, object_code)
    val writerD = new PrintWriter(viewDFile)
    try {
      writerD.println("agency,fiscal_year,fund_id,object_code,total_payment_amount,payment_count")
      for (((ag, fy, fund, obj), acc) <- paymentMap.toSeq.sortBy(_._1))
        writerD.println(s"$ag,$fy,$fund,$obj,${acc.totalAmt},${acc.paymentCount}")
    } finally writerD.close()

    // ── Emit structured metrics ────────────────────────────────────────────
    val elapsedS   = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val peakRss    = Runtime.getRuntime.totalMemory()
    val nMonthly   = monthlyMap.size
    val nVendor    = vendorMap.size
    val nCommodity = commodityMap.size
    val nPayment   = paymentMap.size

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
  //  Formula locked 2026-11-04: SHA256(VENDORID) first 8 hex chars ≡ mod 2^32.
  //  Do not change after any split assignment has been computed.
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
