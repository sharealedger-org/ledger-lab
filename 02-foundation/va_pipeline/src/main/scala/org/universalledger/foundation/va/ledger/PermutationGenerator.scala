package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter, BufferedWriter, FileWriter}
import java.util.Calendar

import scala.collection.mutable
import scala.io.Source

//***************************************************************************************************************
//
//  Program:  PermutationGenerator — CCP1 Permutation Capacity Step (Step 21)
//
//  PURPOSE
//  -------
//  A GL answers few questions: its key is agency, fund, object, nominal account, period and the
//  debit/offset flag (step 20).  A user can ask about any other attribute captured on the SJE (vendor,
//  dates, business event, rule, ...).  To answer such a question a fixed-key system must hold a balance
//  row for each combination of those attributes under each GL key.  This step reports that number — the
//  permutation CAPACITY — and the per-dimension distinct counts behind it.  Capacity is the legacy
//  architecture's pre-materialisation cost; the Instrument Ledger (step 3) holds only the combinations
//  that occurred, at its own 15-field key.
//
//  WHAT THIS DOES
//  --------------
//  1. Reads all SortedJEFY{yy}*.csv files (one-read rule, same input as step 20).
//  2. Per GL key it counts the rows and the DISTINCT values of every other SJE column, except the
//     excluded ones below.
//  3. While reading, it detects year-wide FUNCTIONAL DEPENDENCIES between dimensions: A -> B holds when
//     every value of A co-occurred with exactly one value of B.  A dimension that another dimension
//     determines adds no combinations, so it is dropped from the product.
//  4. capacity(GL key) = PRODUCT, over the dimensions that are not determined by another dimension, of
//     the distinct-value count.  A constant column contributes a factor of 1.  Nothing is materialised.
//  5. Writes PERM_TBL20YY.csv, one row per GL key:
//       agency, nominal_account_id, fund, object_id, fiscal_year, dir_flag, row_count,
//       d_<column> ... (one distinct-count column per dimension that varies anywhere in the year),
//       capacity
//     and PERM_SUMMARY20YY.csv (one row of year totals, summed across years by step 21s).
//
//  CAR (CORE ACCOUNT RECORD) DIMENSIONS
//  ------------------------------------
//  Any dimension that is functionally determined by the vendor (transIPID) is a CAR field: the Instrument
//  Ledger carries it with the instrument and can answer questions on it.  The RESULT line lists the
//  dimensions this step found to be vendor-determined (car_dims).  transContractID is CAR by definition
//  (a contract belongs to one vendor); it is constant "0" in every current producer, so it is declared
//  rather than detected.  A pivot that is NOT CAR (for example injectivity) is one the IL cannot answer.
//
//  EXCLUDED FROM THE PRODUCT
//  -------------------------
//  Event identity and measures are not dimensions a user pivots on:
//    col 3 transJrnlID, col 25 transTransAmount, col 27 transUnitPrice, col 28 transStatisticAmount.
//
//  CAVEATS
//  -------
//  * Capacity is still an upper bound.  Dependencies are detected, but the remaining dimensions are
//    multiplied as if independent.  capacity_independent (no dependency removed) is reported beside it
//    so the effect of the tightening is visible.
//  * It is not the observed combination count.  Observed combinations at the Instrument Ledger key are
//    step 3's ledger rows.
//  * vendor_rows = sum over GL keys of distinct vendors, kept for continuity with earlier runs.
//
//  MEMORY CONTRACT
//  ---------------
//  Per GL key: row count and the first value per dimension, plus a set only where a dimension takes a
//  second value.  Dependency tracking keeps, for each non-constant dimension, one int array per distinct
//  value of that dimension (distinct values x dimensions ints).  Both are whole-file maps, as in step 20
//  (about 275K–310K keys per year in the VA corpus); neither is bounded by the row count.  Measured heap
//  must be recorded before this is used at larger scale.
//
//  METRICS (process_metrics.csv)
//  ------------------------------
//  RESULT step=21 year=YYYY rc=0 records_read=N records_written=M elapsed_s=E
//         input_bytes=B output_bytes=O vendor_rows=V capacity=C capacity_independent=I
//         varying_dims=name;name car_dims=name;name dropped_dims=B<-A;B<-A capacity_dims=name;name
//  RESOURCE step=21 year=YYYY jvm_heap_bytes=R
//
//  CLI invocation (via LedgerApp dispatcher):
//    sbt "run --step 21 --inPath data/output/ --outPath data/output/ --year 2014"
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  2026 — CCP1 permutation capacity step (Bob AI, Session: 2026-11-11)
//***************************************************************************************************************

object PermutationGenerator {

  // GL key columns in the 39-column SortedJE schema (see standardizeAndSort.scala)
  private val COL_AGENCY      = 12  // transLegalEntityID
  private val COL_FUND        = 13  // transCenterID
  private val COL_OBJECT      = 14  // transProjectID
  private val COL_NOMINAL_ACC = 16  // transNominalAccountID
  private val COL_FISCAL_YR   = 22  // transFiscalPeriod
  private val COL_DIR_FLAG    = 31  // transDirVsOffsetFlg (D = debit, O = offset); GL key field
  private val COL_VENDOR_ID   = 0   // transIPID

  private val SJE_COLUMN_COUNT = 39

  private val COLUMN_NAMES: Array[String] = Array(
    "transIPID", "transContractID", "transCommitmentID", "transJrnlID", "transJrnlLineID",
    "transBusinessEventCode", "transSourceSystemID", "transOriginalDocID", "transJrnlDescript",
    "transLedgerID", "transJrnlType", "transBookCodeID", "transLegalEntityID", "transCenterID",
    "transProjectID", "transProductID", "transNominalAccountID", "transAltAccountID",
    "transCurrencyCodeSourceID", "transCurrencyTypeCodeSourceID", "transCurrencyCodeTargetID",
    "transCurrencyTypeCodeTargetID", "transFiscalPeriod", "transAcctDate", "transTransDate",
    "transTransAmount", "transUnitOfMeasure", "transUnitPrice", "transStatisticAmount",
    "tranRuleSetID", "transRuleID", "transDirVsOffsetFlg", "transReconcileFlg", "transAdjustFlg",
    "transExtensionIDAuditTrail", "transExtensionIDSource", "transExtensionIDClass",
    "transExtensionIDDates", "transExtensionIDCustom")

  private val GL_KEY_COLUMNS = Set(COL_AGENCY, COL_FUND, COL_OBJECT, COL_NOMINAL_ACC, COL_FISCAL_YR, COL_DIR_FLAG)
  private val EXCLUDED_COLUMNS = Set(3, 25, 27, 28)

  // Dimension columns: everything that is neither GL key nor excluded, in column order.
  private val DIM_COLUMNS: Array[Int] =
    (0 until SJE_COLUMN_COUNT).filterNot(c => GL_KEY_COLUMNS(c) || EXCLUDED_COLUMNS(c)).toArray
  private val DIM_COUNT = DIM_COLUMNS.length
  private val VENDOR_DIM = DIM_COLUMNS.indexOf(COL_VENDOR_ID)
  private val CONTRACT_DIM = DIM_COLUMNS.indexOf(1)  // transContractID

  private def dimName(d: Int): String = COLUMN_NAMES(DIM_COLUMNS(d))

  private case class GLKey(agency: String, nominalAccount: String, fund: String, objectCode: String,
                           fiscalYear: String, dirFlag: String)

  // Per GL key: row count, the first value seen per dimension, and (only for dimensions that took a
  // second value) the set of distinct interned values.
  private final class KeyState {
    var rows: Long = 0L
    val first: Array[Int] = Array.fill(DIM_COUNT)(-1)
    var extra: Array[mutable.HashSet[Int]] = null  // allocated on first divergence

    def distinct(dim: Int): Int =
      if (extra == null || extra(dim) == null) 1 else extra(dim).size
  }

  // Year-wide functional-dependency detector.  conflict(a)(b) becomes true once two rows share a value
  // of dimension a but differ on dimension b; a pair that never conflicts is a dependency a -> b.
  // Constant dimensions are skipped while they are constant (they determine nothing and are determined by
  // everything); the history needed when one stops being constant is reconstructed from the first row.
  private final class FdTracker(n: Int) {
    private val conflict = Array.fill(n, n)(false)
    private val alive: Array[Array[Int]] = Array.tabulate(n)(a => (0 until n).filter(_ != a).toArray)
    private val table: Array[mutable.ArrayBuffer[Array[Int]]] = Array.fill(n)(new mutable.ArrayBuffer[Array[Int]])
    private var firstIds: Array[Int] = null

    private def put(buf: mutable.ArrayBuffer[Array[Int]], id: Int, row: Array[Int]): Unit = {
      while (buf.size <= id) buf += null
      buf(id) = row
    }

    def observe(ids: Array[Int], sizeBefore: Array[Int], sizeAfter: Array[Int]): Unit = {
      if (firstIds == null) firstIds = ids.clone()
      var a = 0
      while (a < n) {
        if (sizeAfter(a) >= 2) {
          var changed = false
          val buf = table(a)
          if (sizeBefore(a) <= 1) {
            // a stops being constant on this row.  Every earlier row had a = a0, so any b that already
            // had two values is not determined by a.
            var b = 0
            while (b < n) {
              if (b != a && sizeBefore(b) >= 2 && !conflict(a)(b)) { conflict(a)(b) = true; changed = true }
              b += 1
            }
            put(buf, 0, firstIds.clone())
          }
          val id  = ids(a)
          val row = if (id < buf.size) buf(id) else null
          if (row == null) put(buf, id, ids.clone())
          else {
            val list = alive(a)
            var i = 0
            while (i < list.length) {
              val b = list(i)
              if (!conflict(a)(b) && sizeAfter(b) >= 2 && row(b) != ids(b)) { conflict(a)(b) = true; changed = true }
              i += 1
            }
          }
          if (changed) alive(a) = alive(a).filterNot(conflict(a)(_))
        }
        a += 1
      }
    }

    /** a -> b among dimensions that actually vary in the year. */
    def determines(a: Int, b: Int, finalSize: Array[Int]): Boolean =
      a != b && finalSize(a) >= 2 && finalSize(b) >= 2 && !conflict(a)(b)
  }

  // ── Entry point called by LedgerApp dispatcher ─────────────────────────────
  def apply(params: Map[String, String]): Unit = {
    val t0 = System.currentTimeMillis()

    println("*" * 100)
    println("                         Step 21 — CCP1 Permutation Capacity (PermutationGenerator)")
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

    val outFile     = new File(outPath, s"PERM_TBL20${shortYr}.csv")
    val summaryFile = new File(outPath, s"PERM_SUMMARY20${shortYr}.csv")
    println(s"  Output file    : ${outFile.getPath}")

    // ── Accumulate per-GL-key distinct counts and year-wide dependencies ──────
    val states    = mutable.HashMap[GLKey, KeyState]()
    val dicts     = Array.fill(DIM_COUNT)(mutable.HashMap[String, Int]())
    val varied    = Array.fill(DIM_COUNT)(false)
    val fd        = new FdTracker(DIM_COUNT)
    val ids       = new Array[Int](DIM_COUNT)
    val sizeBefore = new Array[Int](DIM_COUNT)
    val sizeAfter  = new Array[Int](DIM_COUNT)
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
          // Same row-acceptance rule as step 20 (> col 31) so records_read matches; short rows read ""
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
            val st = states.getOrElseUpdate(key, new KeyState)
            st.rows += 1
            var d = 0
            while (d < DIM_COUNT) {
              val c  = DIM_COLUMNS(d)
              val v  = if (c < cols.length) cols(c).trim else ""
              val dict = dicts(d)
              sizeBefore(d) = dict.size
              val id = dict.getOrElseUpdate(v, dict.size)
              sizeAfter(d) = dict.size
              ids(d) = id
              val f  = st.first(d)
              if (f == -1) st.first(d) = id
              else if (f != id || (st.extra != null && st.extra(d) != null)) {
                if (st.extra == null) st.extra = new Array[mutable.HashSet[Int]](DIM_COUNT)
                if (st.extra(d) == null) st.extra(d) = mutable.HashSet[Int](f)
                st.extra(d) += id
                varied(d) = true
              }
              d += 1
            }
            fd.observe(ids, sizeBefore, sizeAfter)
          }
        }
      } finally src.close()
    }

    // ── Resolve dependencies: which dimensions stay in the product ────────────
    val finalSize = dicts.map(_.size)
    // B is dropped when some A determines it and either B does not determine A back or A comes first
    // (so of two mutually dependent dimensions the earlier column is kept).
    val dropped = mutable.LinkedHashMap[Int, Int]()   // B -> the A that determines it
    for (b <- 0 until DIM_COUNT) {
      val a = (0 until DIM_COUNT).find(a => fd.determines(a, b, finalSize) &&
        (!fd.determines(b, a, finalSize) || a < b))
      a.foreach(dropped(b) = _)
    }
    val capacityDims = (0 until DIM_COUNT).filterNot(dropped.contains)
    // CAR: everything the vendor determines, plus transContractID, which is CAR by definition
    // (a contract belongs to one vendor) even while it is a constant in every current producer.
    val carDims = (0 until DIM_COUNT).filter(b => b != VENDOR_DIM &&
      (b == CONTRACT_DIM || fd.determines(VENDOR_DIM, b, finalSize)))

    // ── Write permutation capacity table ──────────────────────────────────────
    new File(outPath).mkdirs()
    val varyingDims = (0 until DIM_COUNT).filter(varied(_))
    var capacityTotal = BigInt(0)
    var independentTotal = BigInt(0)
    var vendorRows    = 0L
    val pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))
    try {
      pw.println((Seq("agency", "nominal_account_id", "fund", "object_id", "fiscal_year", "dir_flag", "row_count") ++
        varyingDims.map(d => "d_" + dimName(d)) ++ Seq("capacity")).mkString(","))
      val sorted = states.toSeq.sortBy(kv =>
        (kv._1.fiscalYear, kv._1.agency, kv._1.nominalAccount, kv._1.fund, kv._1.objectCode, kv._1.dirFlag))
      for ((key, st) <- sorted) {
        var capacity = BigInt(1)
        for (d <- capacityDims) capacity *= st.distinct(d)
        var independent = BigInt(1)
        var d = 0
        while (d < DIM_COUNT) { independent *= st.distinct(d); d += 1 }
        capacityTotal    += capacity
        independentTotal += independent
        vendorRows       += st.distinct(VENDOR_DIM)
        pw.println((Seq(key.agency, key.nominalAccount, key.fund, key.objectCode, key.fiscalYear, key.dirFlag, st.rows) ++
          varyingDims.map(st.distinct) ++ Seq(capacity)).mkString(","))
      }
    } finally pw.close()

    val glKeys   = states.size.toLong
    val outBytes = outFile.length()
    val elapsedS = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val peakRss  = Runtime.getRuntime.totalMemory()
    def names(ds: Seq[Int]): String = if (ds.isEmpty) "none" else ds.map(dimName).mkString(";")
    val varyingNames = names(varyingDims)
    val carNames     = names(carDims)
    val droppedNames = if (dropped.isEmpty) "none" else dropped.map { case (b, a) => dimName(b) + "<-" + dimName(a) }.mkString(";")
    val capacityNames = names(capacityDims.filter(varyingDims.contains))

    val sw = new PrintWriter(new BufferedWriter(new FileWriter(summaryFile)))
    try {
      sw.println("fiscal_year,gl_keys,vendor_rows,capacity,capacity_independent")
      sw.println(s"20$shortYr,$glKeys,$vendorRows,$capacityTotal,$independentTotal")
    } finally sw.close()

    println(f"  GL keys (rows written)   : $glKeys%,d")
    println(f"  Journal rows read        : $totalRead%,d")
    println(f"  Vendor rows (observed)   : $vendorRows%,d")
    println(s"  Capacity (dependencies removed): $capacityTotal")
    println(s"  Capacity (independent bound)   : $independentTotal")
    println(s"  Varying dimensions       : $varyingNames")
    println(s"  Vendor-determined (CAR)  : $carNames")
    println(s"  Dropped by dependency    : $droppedNames")
    println(s"  Output                   : ${outFile.getPath}")
    println("STATUS: COMPLETE")
    println(s"RESULT step=21 year=$yearStr rc=0 records_read=$totalRead records_written=$glKeys elapsed_s=$elapsedS input_bytes=$totalInputBytes output_bytes=$outBytes vendor_rows=$vendorRows capacity=$capacityTotal capacity_independent=$independentTotal varying_dims=$varyingNames car_dims=$carNames dropped_dims=$droppedNames capacity_dims=$capacityNames")
    println(s"RESOURCE step=21 year=$yearStr jvm_heap_bytes=$peakRss")

    println("*" * 100)
    println(s"Process End Time: ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}

//***************************************************************************************************************
//  PermutationSummary — Step 21s: sum the per-year PERM_SUMMARY20YY.csv files written by step 21.
//
//  Capacity totals exceed 64-bit integers, so the cross-year sum is done here in Scala (BigInt) rather
//  than in the orchestration shell, which only forwards the RESULT line.
//
//  Params:  outPath — directory holding PERM_SUMMARY20YY.csv;  years — comma list of 2-digit years.
//  RESULT step=21s years=NN,NN rc=0 gl_keys=G vendor_rows=V capacity=C capacity_independent=I years_missing=...
//***************************************************************************************************************
object PermutationSummary {

  def apply(params: Map[String, String]): Unit = {
    val outPath = params.getOrElse("outPath", "data/output/")
    val years   = params.getOrElse("years", "").split(",").map(_.trim).filter(_.nonEmpty).toSeq

    var glKeys = BigInt(0); var vendorRows = BigInt(0)
    var capacity = BigInt(0); var independent = BigInt(0)
    val missing = mutable.ArrayBuffer[String]()

    for (y <- years) {
      val short = if (y.length == 4) y.substring(2) else y
      val f = new File(outPath, s"PERM_SUMMARY20$short.csv")
      if (!f.exists()) missing += short
      else {
        val src = Source.fromFile(f, "UTF-8")
        try {
          val row = src.getLines().drop(1).next().split(",", -1)
          glKeys += BigInt(row(1)); vendorRows += BigInt(row(2))
          capacity += BigInt(row(3)); independent += BigInt(row(4))
        } finally src.close()
      }
    }
    val missingStr = if (missing.isEmpty) "none" else missing.mkString(";")
    println(s"RESULT step=21s years=${years.mkString(",")} rc=0 gl_keys=$glKeys vendor_rows=$vendorRows capacity=$capacity capacity_independent=$independent years_missing=$missingStr")
  }
}
