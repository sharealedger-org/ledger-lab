package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter, BufferedWriter, FileWriter, DataInputStream, DataOutputStream, BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream, EOFException}
import java.util.Calendar

import scala.collection.mutable
import scala.io.Source
import scala.util.hashing.MurmurHash3

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
//  MEMORY CONTRACT (revised: extract-time summarisation, same pattern as 5v View D / GERS-003)
//  -------------------------------------------------------------------------------------------
//  The sorted JE is read once.  Each row emits (GL key id, dimension, 64-bit value hash) facts for the
//  dimensions that vary in the year into a bounded buffer (permBufferCapacity records, default 1,000,000).
//  A full buffer is sorted, de-duplicated and spilled as a run; at the end a k-way merge counts distinct
//  hashes per (GL key, dimension).  Resident state is therefore the GL key index (one entry per GL key, as in
//  step 20), the buffer, and the dependency tables below, not one entry per distinct value of every column.
//  Dependencies: vendor-determined dimensions are detected exactly from the vendor order of the input (a
//  dimension constant within every vendor group); other determinants are tracked only while they have at
//  most permFdCap distinct values (default 20,000), above which they stop determining anything and are listed
//  in fd_untracked_dims (capacity can only be larger for it).  A vendor that reappears after its group closed
//  sets vendor_order=violated and the vendor-determined set is then not reported.
//  Values are held as 64-bit hashes, never as strings.
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


  // Dimension hash: 64 bits from two seeded 32-bit murmur hashes.  Values are never stored, only their
  // hashes; a wrong count needs two different values of one dimension under one GL key to share 64 bits.
  private def hash64(s: String): Long =
    (MurmurHash3.stringHash(s, 0x3c074a61).toLong << 32) | (MurmurHash3.stringHash(s, 0x1b873593) & 0xffffffffL)

  private final class GLInfo(val id: Int) { var rows: Long = 0L }

  // One fact: GL key id, dimension, value hash.  pk = glId << 6 | dimension.  Sorted, de-duplicated runs of
  // these facts are the partial aggregates; the final reduce counts distinct hashes per pk.
  private final case class Rec(pk: Long, h: Long)
  private def recLess(a: Rec, b: Rec): Boolean = a.pk < b.pk || (a.pk == b.pk && a.h < b.h)
  private val recComparator = new java.util.Comparator[Rec] {
    def compare(a: Rec, b: Rec): Int =
      if (a.pk != b.pk) java.lang.Long.compare(a.pk, b.pk) else java.lang.Long.compare(a.h, b.h)
  }

  private abstract class Run { var cur: Rec = null; def advance(): Unit; def close(): Unit = () }
  private final class MemRun(a: Array[Rec], n: Int) extends Run {
    private var i = 0
    advance()
    def advance(): Unit = if (i < n) { cur = a(i); i += 1 } else cur = null
  }
  private final class FileRun(f: File) extends Run {
    private val in = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 1 << 16))
    advance()
    def advance(): Unit = cur = try Rec(in.readLong(), in.readLong()) catch { case _: EOFException => null }
    override def close(): Unit = in.close()
  }

  // Year-wide functional-dependency detector over dimension HASHES.  conflict(a)(b) becomes true once two rows
  // share a value of a but differ on b; a pair that never conflicts is a dependency a -> b.  Memory is bounded:
  // a determinant whose distinct values pass `cap` stops being tracked and then determines nothing, which can
  // only make the reported capacity larger.  The vendor dimension (`skip`) is handled exactly by the
  // vendor-group check in the main pass, not here.  `first` is the first row's hashes (filled by the caller).
  private final class FdTracker(n: Int, skip: Int, cap: Int, first: Array[Long]) {
    private val conflict = Array.fill(n, n)(false)
    private val alive: Array[Array[Int]] = Array.tabulate(n)(a => (0 until n).filter(_ != a).toArray)
    private val table = new Array[mutable.HashMap[Long, Array[Long]]](n)
    val tracked = Array.fill(n)(true)

    def observe(hs: Array[Long], variedBefore: Array[Boolean], varied: Array[Boolean]): Unit = {
      var a = 0
      while (a < n) {
        if (a != skip && tracked(a) && varied(a)) {
          var changed = false
          if (!variedBefore(a)) {
            // a stops being constant on this row.  Every earlier row had first(a), so any b that already
            // had two values among them is not determined by a.
            var b = 0
            while (b < n) {
              if (b != a && variedBefore(b) && !conflict(a)(b)) { conflict(a)(b) = true; changed = true }
              b += 1
            }
            table(a) = new mutable.HashMap[Long, Array[Long]]
            table(a).put(first(a), first)
          }
          val tbl = table(a)
          val row = tbl.getOrElse(hs(a), null)
          if (row == null) {
            if (tbl.size >= cap) { tracked(a) = false; table(a) = null }
            else tbl.put(hs(a), hs.clone())
          } else {
            val list = alive(a)
            var i = 0
            while (i < list.length) {
              val b = list(i)
              if (!conflict(a)(b) && row(b) != hs(b)) { conflict(a)(b) = true; changed = true }
              i += 1
            }
          }
          if (changed) alive(a) = alive(a).filterNot(conflict(a)(_))
        }
        a += 1
      }
    }

    /** a -> b among dimensions that actually vary in the year (never for the vendor dimension). */
    def determines(a: Int, b: Int, varied: Array[Boolean]): Boolean =
      a != skip && a != b && tracked(a) && varied(a) && varied(b) && !conflict(a)(b)
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

    val bufCap = params.getOrElse("permBufferCapacity", "1000000").toInt
    val fdCap  = params.getOrElse("permFdCap", "20000").toInt
    require(DIM_COUNT < 64, "dimension index must fit in 6 bits")
    new File(outPath).mkdirs()

    // ── Pass over the sorted JE: per-row facts into a bounded buffer; full buffers spill as sorted runs ──
    val glIndex      = mutable.HashMap[GLKey, GLInfo]()
    var nextId       = 0
    val varied       = Array.fill(DIM_COUNT)(false)
    val variedBefore = new Array[Boolean](DIM_COUNT)
    val firstHash    = new Array[Long](DIM_COUNT)
    val hs           = new Array[Long](DIM_COUNT)
    val fd           = new FdTracker(DIM_COUNT, VENDOR_DIM, fdCap, firstHash)
    // Vendor-group check (exact, uses the CKB vendor order): a dimension is vendor-determined when it is
    // constant within every vendor group.  A vendor that reappears after its group closed makes it unreliable.
    val vendorConst  = Array.fill(DIM_COUNT)(true)
    val vendorFirst  = new Array[Long](DIM_COUNT)
    val vendorsDone  = mutable.HashSet[Long]()
    var curVendor    = 0L
    var haveVendor   = false
    var vendorOrdered = true
    var totalRead    = 0L
    var totalInputBytes = 0L

    val buf = new Array[Rec](bufCap)
    var bufN = 0
    val spills = mutable.ArrayBuffer[File]()
    var spillRecords = 0L
    def spillBuffer(): Unit = {
      java.util.Arrays.sort(buf, 0, bufN, recComparator)
      val f = new File(outPath, s"perm_spill_${shortYr}_${spills.size}.tmp")
      val out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f), 1 << 16))
      try {
        var i = 0
        while (i < bufN) {
          val r = buf(i)
          if (i == 0 || r.pk != buf(i - 1).pk || r.h != buf(i - 1).h) {
            out.writeLong(r.pk); out.writeLong(r.h); spillRecords += 1
          }
          i += 1
        }
      } finally out.close()
      spills += f
      java.util.Arrays.fill(buf.asInstanceOf[Array[AnyRef]], 0, bufN, null)
      bufN = 0
    }
    // Direct-mapped recent-fact cache: a fact identical to the one last stored in its slot is not re-emitted.
    // Bounded (2^20 slots), it only removes duplicates the spill de-duplication would remove anyway.
    val cacheMask = (1 << 20) - 1
    val cachePk   = Array.fill(cacheMask + 1)(-1L)
    val cacheH    = new Array[Long](cacheMask + 1)
    def emit(glId: Int, d: Int, h: Long): Unit = {
      val pk = (glId.toLong << 6) | d
      val slot = ((pk * 0x9E3779B97F4A7C15L) ^ h * 0xC2B2AE3D27D4EB4FL).hashCode & cacheMask
      if (cachePk(slot) == pk && cacheH(slot) == h) return
      cachePk(slot) = pk; cacheH(slot) = h
      if (bufN == bufCap) spillBuffer()
      buf(bufN) = Rec(pk, h)
      bufN += 1
    }

    val runs = mutable.ArrayBuffer[Run]()
    try {
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
              var info = glIndex.getOrElse(key, null)
              val isNew = info == null
              if (isNew) { info = new GLInfo(nextId); nextId += 1; glIndex.put(key, info) }
              info.rows += 1
              val existing = if (isNew) info.id else nextId   // GL ids that existed before this row

              var d = 0
              while (d < DIM_COUNT) {
                val c = DIM_COLUMNS(d)
                hs(d) = hash64(if (c < cols.length) cols(c).trim else "")
                d += 1
              }
              if (totalRead == 1L) System.arraycopy(hs, 0, firstHash, 0, DIM_COUNT)
              System.arraycopy(varied, 0, variedBefore, 0, DIM_COUNT)
              d = 0
              while (d < DIM_COUNT) {
                if (!varied(d) && hs(d) != firstHash(d)) {
                  varied(d) = true
                  // Every earlier row held the first value, so every GL key seen so far owes that one fact.
                  var g = 0
                  while (g < existing) { emit(g, d, firstHash(d)); g += 1 }
                }
                if (varied(d)) emit(info.id, d, hs(d))
                d += 1
              }
              fd.observe(hs, variedBefore, varied)

              val vh = hs(VENDOR_DIM)
              if (!haveVendor || vh != curVendor) {
                if (haveVendor) vendorsDone += curVendor
                if (vendorsDone.contains(vh)) vendorOrdered = false
                curVendor = vh; haveVendor = true
                System.arraycopy(hs, 0, vendorFirst, 0, DIM_COUNT)
              } else {
                d = 0
                while (d < DIM_COUNT) { if (hs(d) != vendorFirst(d)) vendorConst(d) = false; d += 1 }
              }
            }
          }
        } finally src.close()
      }

      // ── Final reduce: merge the sorted runs; distinct hashes per (GL key, dimension) ──
      java.util.Arrays.sort(buf, 0, bufN, recComparator)
      spills.foreach(f => runs += new FileRun(f))
      runs += new MemRun(buf, bufN)
      val variedDims = (0 until DIM_COUNT).filter(varied(_))   // differs anywhere in the year
      val pos = Array.fill(DIM_COUNT)(-1)
      variedDims.zipWithIndex.foreach { case (d, i) => pos(d) = i }
      val counts = new Array[Array[Int]](nextId)
      val tR = System.currentTimeMillis()
      val pq = new mutable.PriorityQueue[(Rec, Int)]()(
        Ordering.fromLessThan[(Rec, Int)]((x, y) => recLess(y._1, x._1)))   // smallest record first
      for (i <- runs.indices) if (runs(i).cur != null) pq.enqueue((runs(i).cur, i))
      var last: Rec = null
      while (pq.nonEmpty) {
        val (r, idx) = pq.dequeue()
        runs(idx).advance()
        if (runs(idx).cur != null) pq.enqueue((runs(idx).cur, idx))
        if (last == null || r.pk != last.pk || r.h != last.h) {
          val gl = (r.pk >>> 6).toInt
          val dm = (r.pk & 63L).toInt
          var c = counts(gl)
          if (c == null) { c = new Array[Int](variedDims.length); counts(gl) = c }
          c(pos(dm)) += 1
        }
        last = r
      }
      val reduceS = (System.currentTimeMillis() - tR) / 1000
      // Listed (d_ columns, varying_dims): dimensions on which some GL key holds two or more values.
      val varyingDims = variedDims.filter(d => counts.exists(c => c != null && c(pos(d)) >= 2))

      // ── Resolve dependencies: which dimensions stay in the product ────────────
      def determines(a: Int, b: Int): Boolean =
        if (a == VENDOR_DIM) a != b && varied(a) && varied(b) && vendorOrdered && vendorConst(b)
        else fd.determines(a, b, varied)
      // B is dropped when some A determines it and either B does not determine A back or A comes first
      // (so of two mutually dependent dimensions the earlier column is kept).
      val dropped = mutable.LinkedHashMap[Int, Int]()   // B -> the A that determines it
      for (b <- 0 until DIM_COUNT) {
        val a = (0 until DIM_COUNT).find(a => determines(a, b) && (!determines(b, a) || a < b))
        a.foreach(dropped(b) = _)
      }
      val capacityDims = (0 until DIM_COUNT).filterNot(dropped.contains)
      // CAR: everything the vendor determines, plus transContractID, which is CAR by definition
      // (a contract belongs to one vendor) even while it is a constant in every current producer.
      val carDims = (0 until DIM_COUNT).filter(b => b != VENDOR_DIM && (b == CONTRACT_DIM || determines(VENDOR_DIM, b)))
      val untrackedDims = (0 until DIM_COUNT).filter(d => d != VENDOR_DIM && varied(d) && !fd.tracked(d))

      // ── Write permutation capacity table ──────────────────────────────────────
      var capacityTotal = BigInt(0)
      var independentTotal = BigInt(0)
      var vendorRows    = 0L
      val pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))
      try {
        pw.println((Seq("agency", "nominal_account_id", "fund", "object_id", "fiscal_year", "dir_flag", "row_count") ++
          varyingDims.map(d => "d_" + dimName(d)) ++ Seq("capacity")).mkString(","))
        val sorted = glIndex.toSeq.sortBy(kv =>
          (kv._1.fiscalYear, kv._1.agency, kv._1.nominalAccount, kv._1.fund, kv._1.objectCode, kv._1.dirFlag))
        for ((key, info) <- sorted) {
          val c = counts(info.id)
          def distinct(d: Int): Int = if (c == null || pos(d) < 0) 1 else c(pos(d)).max(1)
          var capacity = BigInt(1)
          for (d <- capacityDims) capacity *= distinct(d)
          var independent = BigInt(1)
          var d = 0
          while (d < DIM_COUNT) { independent *= distinct(d); d += 1 }
          capacityTotal    += capacity
          independentTotal += independent
          vendorRows       += distinct(VENDOR_DIM)
          pw.println((Seq(key.agency, key.nominalAccount, key.fund, key.objectCode, key.fiscalYear, key.dirFlag, info.rows) ++
            varyingDims.map(distinct) ++ Seq(capacity)).mkString(","))
        }
      } finally pw.close()

      val glKeys   = glIndex.size.toLong
      val outBytes = outFile.length()
      val elapsedS = ((System.currentTimeMillis() - t0) / 1000).max(1)
      val peakRss  = Runtime.getRuntime.totalMemory()
      def names(ds: Seq[Int]): String = if (ds.isEmpty) "none" else ds.map(dimName).mkString(";")
      val varyingNames = names(varyingDims)
      val carNames     = names(carDims)
      val droppedNames = if (dropped.isEmpty) "none" else dropped.map { case (b, a) => dimName(b) + "<-" + dimName(a) }.mkString(";")
      val capacityNames = names(capacityDims.filter(varyingDims.contains))
      val untrackedNames = names(untrackedDims)
      val overflow = spills.nonEmpty

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
      println(s"  Buffer capacity / spills : $bufCap / ${spills.size} files, $spillRecords records, reduce ${reduceS}s")
      println(s"  Vendor order             : ${if (vendorOrdered) "ok" else "VIOLATED (vendor-determined dims not detected)"}")
      println(s"  Determinants not tracked : $untrackedNames")
      println(s"  Output                   : ${outFile.getPath}")
      println("STATUS: COMPLETE")
      println(s"RESULT step=21 year=$yearStr rc=0 records_read=$totalRead records_written=$glKeys elapsed_s=$elapsedS input_bytes=$totalInputBytes output_bytes=$outBytes vendor_rows=$vendorRows capacity=$capacityTotal capacity_independent=$independentTotal varying_dims=$varyingNames car_dims=$carNames dropped_dims=$droppedNames capacity_dims=$capacityNames buffer_overflow=$overflow spill_files=${spills.size} spill_records=$spillRecords reduce_seconds=$reduceS vendor_order=${if (vendorOrdered) "ok" else "violated"} fd_untracked_dims=$untrackedNames")
      println(s"RESOURCE step=21 year=$yearStr jvm_heap_bytes=$peakRss")

      println("*" * 100)
      println(s"Process End Time: ${Calendar.getInstance.getTime}")
      println("*" * 100)
    } finally {
      runs.foreach(r => try r.close() catch { case _: Exception => })
      spills.foreach(f => try f.delete() catch { case _: Exception => })
    }
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
