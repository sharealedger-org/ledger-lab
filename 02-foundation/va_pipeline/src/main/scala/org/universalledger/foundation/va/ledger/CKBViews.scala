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
//    Physical process (revised 2026-11-10): ONE ordered k-way merge over the SortedJE partitions of
//      every year supplied, keyed by the engine key (SortedJE.sortspec: column 0 = vendor, then columns
//      9–22, compared byte-wise exactly as SortEngine.extractKey/mergeSpills).  The stream is therefore
//      vendor-grouped across ALL years, and a vendor's whole history is in memory only while that
//      vendor's key group is being processed (CKB: memory is one key group, not the key space and not
//      the number of years).  Each year contributes its merged file (SortedJEInputs.oneRead: a year with
//      a merged file does not also contribute its 2m inputs, so no row is read twice).
//
//      Intended placement: the last year's CKB pass of a corpus run, with inPaths = every year's output
//      directory.  The standard ledger WRITE of that pass still selects only the current year; this is
//      an extra pass for an odd (cross-period) view, chosen because it is easier to control-total than
//      changing the standard path.
//
//    Order guard (stops processing): every input is read through SortEngine.extractKey; a row whose key
//      is lower than the previous row of the same file stops the step (IllegalStateException naming the
//      file and line).  A vendor whose key group reappears after another vendor stops the step too, since
//      per-vendor counts would silently be wrong.  This guard is local to 5v; a shared guard for other
//      CKB readers is a follow-up.
//
//    Parameters:
//      inPaths   comma-separated directories; all SortedJE* files in each are merged (no year filter).
//      inPath    single directory (used when inPaths is absent); `year` then filters to that year.
//      outPath   directory receiving the four view files (overwritten on every call).
//      sortspec  optional path to SortedJE.sortspec (defaults as in step 2m).
//
//    Input schema: 39-field comma-delimited Transaction rows.
//      col  0  transIPID              — vendor / instrument ID
//      col  2  transCommitmentID      — PONUMBER for PO rows; "0" for payments
//      col  5  transBusinessEventCode — "COMMITMENT" (PO) or payment event code
//      col 12  transLegalEntityID     — agency
//      col 13  transCenterID          — fund code (payments); "0000" (PO rows)
//      col 14  transProjectID         — NIGP code for PO rows; object code for payments
//      col 22  transFiscalPeriod      — "YYYY" (annual) or "YYYY/MM" (monthly) for PO rows;
//                                       file period such as "0003/07" for payment rows
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
//      key  : (vendor_id, fiscal_year)   — fiscal_year is the raw column-22 value of the PO row
//      cols : vendor_id, vendor_surrogate_key, fiscal_year, first_order_year,
//             present_next_year, total_amount, po_count,
//             nigp_class_modal, agency_count, distinct_nigp_classes
//      notes:
//        vendor_surrogate_key = first 8 hex chars of SHA-256(vendor_id).
//          Used as semantically-neutral shuffle/split key for ML train/test splits:
//          vendorSurrogateKey mod 5 → partition index (VA analog of insurance corpus key §5.2).
//        present_next_year = 1 when the vendor has a COMMITMENT debit in a LATER Virginia fiscal year
//          (July–June).  The fiscal year compared is derived from column 22 for this flag only:
//          "YYYY" is taken as the fiscal year; "YYYY/MM" is a calendar month and maps to
//          YYYY (month 01–06) or YYYY+1 (month 07–12).  Column and key values are unchanged.
//        nigp_class_modal = most frequent 3-digit NIGP class this vendor-year.
//          Captures commodity identity; enables AIM3 year-over-year commodity class shift.
//        agency_count = distinct agencies served by this vendor in this year.
//          Measures market concentration; feeds AIM3 agency-dependency feature.
//        distinct_nigp_classes = count of distinct 3-digit NIGP classes this vendor-year.
//          Measures product breadth; extension point for Papers 3–7 allocation rules.
//        Rows are written when the vendor's key group closes (order: key-group order, then fiscal_year).
//
//    View C  view_agency_year_commodity_mix.csv
//      key  : (agency, fiscal_year, nigp_class_3digit)
//      cols : agency, fiscal_year, nigp_class_3digit, po_count,
//             total_committed_amount, revision_count,
//             vendor_count, min_order_amount, max_order_amount
//      notes:
//        vendor_count = distinct vendors supplying this agency-year-class (counted once per vendor
//          key group, which is exact because a vendor's rows are contiguous).
//        min_order_amount, max_order_amount = range of individual line amounts.
//
//    View D  view_agency_year_payment_totals.csv
//      key  : (agency, fiscal_year, fund_id, object_code)
//      cols : agency, fiscal_year, fund_id, object_code,
//             total_payment_amount, payment_count
//      notes:
//        fund_id = transCenterID (col 13); populated on payment rows.
//        object_code = transProjectID (col 14); populated on payment rows.
//
//    Materialization: one merge pass, no spill.  Views A, C, D are bounded maps over their declared keys;
//    View B is streamed per vendor key group.  The completed-vendor set used by the split-group guard is
//    O(distinct vendor IDs) strings.
//
//    Controls emitted:
//      RESULT step=5v year=... rc=0 records_read=N rows_skipped_short=N files_read=N
//             view_monthly=N view_vendor=N view_agency_commodity=N view_agency_payment=N elapsed_s=N
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
//  2026 — Cross-year k-way merge, order guard, Virginia fiscal year for present_next_year (Bob AI)
//***************************************************************************************************************

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader, PrintWriter}
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.security.MessageDigest
import java.util.Calendar

import scala.collection.mutable

object CKBViews {

  // ── Accumulator types ─────────────────────────────────────────────────────

  // View A: monthly order volume
  //   key: (fiscalPeriod, nigpClass3, agency)
  private case class MonthlyVolumeAcc(
    var poLineCount:    Long       = 0L,
    var totalAmt:       BigDecimal = 0,
    var revisionCount:  Long       = 0L
  )

  // View B: vendor-year survival panel — lives only while its vendor key group is open
  //   key: fiscalYear (raw column 22) within the current vendor
  private case class VendorYearAcc(
    var firstOrderYear: String                        = "",
    var totalAmt:       BigDecimal                    = 0,
    var poCount:        Long                          = 0L,
    nigpClassFreq:      mutable.HashMap[String, Long] = mutable.HashMap.empty,
    agencySet:          mutable.HashSet[String]        = mutable.HashSet.empty
  )

  // View C: agency-year commodity mix
  //   key: (agency, fiscalYear, nigpClass3)
  //   vendorCount is incremented once per vendor key group that touches the key.
  private case class AgencyCommodityAcc(
    var poCount:       Long       = 0L,
    var totalAmt:      BigDecimal = 0,
    var revisionCount: Long       = 0L,
    var minAmt:        BigDecimal = BigDecimal(Long.MaxValue),
    var maxAmt:        BigDecimal = BigDecimal(Long.MinValue),
    var vendorCount:   Long       = 0L
  )

  // View D: agency-year payment totals
  //   key: (agency, fiscalYear, fundId, objectCode)
  private case class AgencyPaymentAcc(var totalAmt: BigDecimal = 0, var paymentCount: Long = 0L)

  // One open input of the k-way merge
  private final class MergeInput(val file: File, val reader: BufferedReader) {
    var prevKey: String = null
    var lineNo:  Long   = 0L
  }

  private val FiscalYear4   = """(\d{4})""".r
  private val FiscalYearMon = """(\d{4})/(\d{2})""".r

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
    val inPaths = params.getOrElse("inPaths", "").split(",").map(_.trim).filter(_.nonEmpty).toSeq

    // ── Resolve input file list ────────────────────────────────────────────
    val dirs: Seq[File] = if (inPaths.nonEmpty) inPaths.map(new File(_)) else Seq(new File(inPath))
    val candidates: Seq[File] = dirs.flatMap { d =>
      if (d.exists() && d.isDirectory) {
        val all = d.listFiles().filter(f => f.isFile && f.getName.startsWith("SortedJE")).toSeq
        if (inPaths.isEmpty && yearStr.nonEmpty) {
          val shortYr = if (yearStr.length == 4) yearStr.substring(2, 4) else yearStr
          all.filter(_.getName.contains(s"FY$shortYr"))
        } else all
      } else {
        System.err.println(s"WARNING: Input directory not found: $d — no SortedJE files processed from it.")
        Seq.empty[File]
      }
    }
    val jeFiles: Seq[File] = SortedJEInputs.oneRead(candidates.sortBy(_.getName))

    println(s"  Input director${if (dirs.size == 1) "y " else "ies"}: ${dirs.mkString(", ")}")
    println(s"  Output directory: $outPath")
    println(s"  Year filter     : ${if (inPaths.isEmpty && yearStr.nonEmpty) yearStr else "(all)"}")
    println(s"  SortedJE files  : ${jeFiles.size}")
    jeFiles.foreach(f => println(s"    ${f.getPath}"))

    val spec = SortEngine.loadSpec(resolveSortSpec(params))

    // ── Aggregation maps (A, C, D) ─────────────────────────────────────────
    val monthlyMap   = mutable.HashMap[(String, String, String), MonthlyVolumeAcc]()
    val commodityMap = mutable.HashMap[(String, String, String), AgencyCommodityAcc]()
    val paymentMap   = mutable.HashMap[(String, String, String, String), AgencyPaymentAcc]()

    new File(outPath).mkdirs()
    val viewAFile = new File(outPath, "view_monthly_order_volume.csv")
    val viewBFile = new File(outPath, "view_vendor_year_survival.csv")
    val viewCFile = new File(outPath, "view_agency_year_commodity_mix.csv")
    val viewDFile = new File(outPath, "view_agency_year_payment_totals.csv")

    var totalRecordsRead = 0L
    var rowsSkippedShort = 0L
    var nVendor          = 0L

    val inputs = jeFiles.map { f =>
      val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
      new MergeInput(f, new BufferedReader(new InputStreamReader(new FileInputStream(f), decoder), 1 << 20))
    }

    val writerB = new PrintWriter(viewBFile)
    try {
      writerB.println(
        "vendor_id,vendor_surrogate_key,fiscal_year,first_order_year,present_next_year," +
        "total_amount,po_count,nigp_class_modal,agency_count,distinct_nigp_classes"
      )

      // k-way merge queue ordered by ascending engine key
      val pq = new mutable.PriorityQueue[(SortEngine.ExtractedKey, Int)]()(
        Ordering.fromLessThan[(SortEngine.ExtractedKey, Int)] { case ((ka, _), (kb, _)) =>
          ka.keyStr.compareTo(kb.keyStr) > 0
        }
      )

      def advance(idx: Int): Unit = {
        val in = inputs(idx)
        var line = in.reader.readLine()
        if (line != null) in.lineNo += 1
        while (line != null && line.trim.isEmpty) {
          line = in.reader.readLine()
          if (line != null) in.lineNo += 1
        }
        if (line != null) {
          val key = SortEngine.extractKey(line.trim, spec)
          if (in.prevKey != null && key.keyStr.compareTo(in.prevKey) < 0)
            throw new IllegalStateException(
              s"5v order guard: ${in.file.getPath} line ${in.lineNo} is out of engine-key order " +
              s"(key '${key.keyStr}' < previous '${in.prevKey}'); CKB input must be sorted by SortedJE.sortspec")
          in.prevKey = key.keyStr
          pq.enqueue((key, idx))
        }
      }

      inputs.indices.foreach(advance)

      // ── Vendor key-group state ───────────────────────────────────────────
      var curVendor: String = null
      val completed    = mutable.HashSet[String]()
      val groupYears   = mutable.HashMap[String, VendorYearAcc]()
      val groupNormFy  = mutable.HashMap[String, Int]()            // raw col-22 -> Virginia fiscal year
      val groupTouched = mutable.HashSet[(String, String, String)]()  // View C keys touched by this vendor

      def closeGroup(): Unit = {
        val normFys = groupNormFy.values.toSeq
        for ((fy, acc) <- groupYears.toSeq.sortBy(_._1)) {
          val thisFy          = groupNormFy.getOrElse(fy, -1)
          val presentNextYear = if (normFys.exists(_ > thisFy)) 1 else 0
          val surrogateKey    = vendorSurrogateKey(curVendor)
          val modalClass      = if (acc.nigpClassFreq.isEmpty) "" else acc.nigpClassFreq.maxBy(_._2)._1
          writerB.println(
            s"$curVendor,$surrogateKey,$fy,${acc.firstOrderYear},$presentNextYear," +
            s"${acc.totalAmt},${acc.poCount},$modalClass,${acc.agencySet.size},${acc.nigpClassFreq.size}"
          )
          nVendor += 1
        }
        groupTouched.foreach(k => commodityMap(k).vendorCount += 1)
        completed += curVendor
        groupYears.clear(); groupNormFy.clear(); groupTouched.clear()
      }

      while (pq.nonEmpty) {
        val (key, idx) = pq.dequeue()
        advance(idx)

        val cols = key.originalRow.split(",", -1)
        if (cols.length >= 26) {
          totalRecordsRead += 1
          val vendorId   = cols(0)
          val commitId   = cols(2)
          val bizEvent   = cols(5)
          val agency     = cols(12)
          val fundId     = cols(13)
          val nigpCode   = cols(14)
          val fiscYear   = cols(22)
          val transDate  = cols(24)  // YYYY/MM/DD or MM/DD/YYYY
          val rawAmt     = cols(25)

          if (vendorId != curVendor) {
            if (curVendor != null) closeGroup()
            if (completed.contains(vendorId))
              throw new IllegalStateException(
                s"5v order guard: vendor '$vendorId' key group reappears after other vendors " +
                s"(split group in ${inputs(idx).file.getPath}); per-vendor views would be wrong")
            curVendor = vendorId
          }

          val amt = try BigDecimal(rawAmt) catch { case _: Exception => BigDecimal(0) }

          // Fiscal period = "YYYY/MM" derived from transDate (only debit rows counted)
          val fiscMonth = deriveFiscalMonth(transDate, fiscYear)

          val nigp3 = if (nigpCode.length >= 3) nigpCode.take(3) else nigpCode.padTo(3, '0')

          // Only accumulate debit rows (positive sign) to avoid double-counting.
          // Credits are structural offsets — they carry no additional information.
          val isDebit = amt >= BigDecimal(0)

          if (bizEvent == "COMMITMENT" && isDebit) {
            // ── View A: monthly order volume ─────────────────────────────
            val accA = monthlyMap.getOrElseUpdate((fiscMonth, nigp3, agency), MonthlyVolumeAcc())
            accA.poLineCount += 1
            accA.totalAmt    += amt
            if (commitId.contains("-V")) accA.revisionCount += 1

            // ── View B: vendor-year survival (current vendor group only) ──
            val accB = groupYears.getOrElseUpdate(fiscYear, VendorYearAcc(firstOrderYear = fiscYear))
            if (accB.firstOrderYear.isEmpty || fiscYear < accB.firstOrderYear)
              accB.firstOrderYear = fiscYear
            accB.totalAmt += amt
            accB.poCount  += 1
            accB.nigpClassFreq(nigp3) = accB.nigpClassFreq.getOrElse(nigp3, 0L) + 1L
            accB.agencySet += agency
            if (!groupNormFy.contains(fiscYear)) groupNormFy(fiscYear) = virginiaFiscalYear(fiscYear)

            // ── View C: agency-year commodity mix ─────────────────────────
            val keyC = (agency, fiscYear, nigp3)
            val accC = commodityMap.getOrElseUpdate(keyC, AgencyCommodityAcc())
            accC.poCount  += 1
            accC.totalAmt += amt
            if (commitId.contains("-V")) accC.revisionCount += 1
            if (amt < accC.minAmt) accC.minAmt = amt
            if (amt > accC.maxAmt) accC.maxAmt = amt
            groupTouched += keyC
          }

          if (bizEvent != "COMMITMENT" && isDebit) {
            // ── View D: agency-year payment totals ────────────────────────
            val accD = paymentMap.getOrElseUpdate((agency, fiscYear, fundId, nigpCode), AgencyPaymentAcc())
            accD.totalAmt     += amt
            accD.paymentCount += 1
          }
        } else rowsSkippedShort += 1
      }
      if (curVendor != null) closeGroup()
    } finally {
      writerB.close()
      inputs.foreach(i => try i.reader.close() catch { case _: Exception => })
    }

    // ── Write the bounded views ────────────────────────────────────────────
    // View A
    val writerA = new PrintWriter(viewAFile)
    try {
      writerA.println("fiscal_period,nigp_class_3digit,agency,po_line_count,total_committed_amount,revision_count")
      for (((fp, nigp3, ag), acc) <- monthlyMap.toSeq.sortBy(_._1))
        writerA.println(s"$fp,$nigp3,$ag,${acc.poLineCount},${acc.totalAmt},${acc.revisionCount}")
    } finally writerA.close()

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
          s"${acc.vendorCount},$minAmt,$maxAmt"
        )
      }
    } finally writerC.close()

    // View D — key (agency, fiscal_year, fund_id, object_code)
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
    val nCommodity = commodityMap.size
    val nPayment   = paymentMap.size

    println("*" * 100)
    println(s"Files processed     : ${jeFiles.size}")
    println(s"Records read        : $totalRecordsRead")
    println(s"Rows skipped (short): $rowsSkippedShort")
    println(s"View A rows (monthly order volume)      : $nMonthly")
    println(s"View B rows (vendor-year survival)      : $nVendor")
    println(s"View C rows (agency-year commodity mix) : $nCommodity")
    println(s"View D rows (agency-year payments)      : $nPayment")
    println(s"RESULT step=5v year=$yearStr rc=0 records_read=$totalRecordsRead rows_skipped_short=$rowsSkippedShort files_read=${jeFiles.size} view_monthly=$nMonthly view_vendor=$nVendor view_agency_commodity=$nCommodity view_agency_payment=$nPayment elapsed_s=$elapsedS")
    println(s"RESOURCE step=5v year=$yearStr peak_rss_bytes=$peakRss")
    println("Process End Time: " + Calendar.getInstance.getTime)
    println("*" * 100)
  }

  // ── Sort spec resolution (same defaults as step 2m) ────────────────────────
  private def resolveSortSpec(params: Map[String, String]): String = {
    val specParam  = params.getOrElse("sortspec", "")
    val candidates = if (specParam.nonEmpty) Seq(specParam) else Seq.empty[String]
    val defaults   = Seq("../sort_specs/SortedJE.sortspec", "../../sort_specs/SortedJE.sortspec")
    (candidates ++ defaults).find(p => new File(p).exists()).getOrElse(defaults.head)
  }

  // ── Virginia fiscal year (July–June) for the present_next_year flag only ───
  //
  //  "YYYY"     → YYYY (already a fiscal year)
  //  "YYYY/MM"  → calendar month: MM 01–06 → YYYY; MM 07–12 → YYYY+1
  //  anything else → -1 (never later than another year)
  private[ledger] def virginiaFiscalYear(period: String): Int = period match {
    case FiscalYear4(y)       => y.toInt
    case FiscalYearMon(y, m)  => y.toInt + (if (m.toInt >= 7) 1 else 0)
    case _                    => -1
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
