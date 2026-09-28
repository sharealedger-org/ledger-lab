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
//  Program:  dataAggregation — Data Aggregation / Subledger Views
//
//  FSP Pattern:  Subledgers / Aggregation
//
//  WHAT IT DOES:
//  Produces analytical views from the posted balance file (LDGR) joined to the
//  VendorMaster (Contract Attributes Record / CAR).  The views produced and their
//  grouping dimensions are declared in ViewSpec.csv — no Scala changes are needed
//  to add, remove, or reconfigure views.
//
//  ViewSpec.csv drives two experiments:
//
//  EXPERIMENT 1 — Input-side cost curve (materialization configs):
//    Suppress views by setting enabled=N in ViewSpec.csv to simulate reduced
//    materialization.  Each enabled=Y row is one output file that must be maintained
//    (one unit of reconciliation obligation / AHI metric, Paper 1 §5.2).
//
//  EXPERIMENT 2 — Output-side Pivot Theorem (CAR attribute expansion):
//    Each new CAR attribute round uncomments one block of view rows in ViewSpec.csv.
//    The pivot_results.csv accumulator records how many views became answerable and
//    the equivalent key-embedded balance cost (permutation formula).
//    No Scala changes are needed between rounds — only ViewSpec.csv changes.
//
//  STANDARD VOCABULARY:
//    VendorMaster.csv      = Contract Attributes Record (CAR)
//    instID / ldgrIPID     = Instrument ID
//    LDGR{year}.csv        = Instrument Ledger (IL)
//
//  INPUTS:
//    {outPath}/LDGR{year}.csv       — final balance file
//    {outPath}/VendorMaster.csv     — Contract Attributes Record (CAR)
//    {inPath}/ViewSpec.csv          — declarative view definitions
//
//  OUTPUTS:
//    {outPath}/{view_name}_{year}.csv  — one file per enabled ViewSpec row
//    {outPath}/pivot_results.csv       — accumulator: views_answerable, permutation cost
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Initial implementation (Bob AI)
//  2025 - Generalized to ViewSpec.csv-driven engine; pivot_results.csv accumulator added (Bob AI)
//***************************************************************************************************************

object dataAggregation {

  // ── LDGR column index map ─────────────────────────────────────────────────
  // Matches the header written by post.scala / ldgrHeader()
  val LdgrCols: Map[String, Int] = Map(
    "ldgrIPID"                    -> 0,
    "ldgrContractID"              -> 1,
    "ldgrCommitmentID"            -> 2,
    "ldgrLdgrlID"                 -> 3,
    "ldgrSourceSystemID"          -> 4,
    "ldgrLedgerID"                -> 5,
    "ldgrJrnlType"                -> 6,
    "ldgrBookCodeID"              -> 7,
    "ldgrLegalEntityID"           -> 8,
    "ldgrCenterID"                -> 9,
    "ldgrProjectID"               -> 10,
    "ldgrProductID"               -> 11,
    "ldgrNominalAccountID"        -> 12,
    "ldgrAltAccountID"            -> 13,
    "ldgrCurrencyCodeSourceID"    -> 14,
    "ldgrCurrencyTypeCodeSourceID"-> 15,
    "ldgrCurrencyCodeTargetID"    -> 16,
    "ldgrCurrencyTypeCodeTargetID"-> 17,
    "ldgrLedgerPeriod"            -> 18,
    "ldgrTransAmount"             -> 19,
    "ldgrUnitOfMeasure"           -> 20,
    "ldgrStatisticAmount"         -> 21,
    "ldgrDirVsOffsetFlg"          -> 22,
    "ldgrReconcileFlg"            -> 23,
    "ldgrAdjustFlg"               -> 24
  )

  // ── ViewSpec row ──────────────────────────────────────────────────────────
  case class ViewDef(
    viewName:       String,
    groupByLdgr:    Seq[String],   // LDGR column names
    groupBySal:     Seq[String],   // VendorMaster column names
    filterLdgrCol:  String,        // "" = no filter
    filterLdgrVal:  String,
    filterSalCol:   String,
    filterSalVal:   String,
    topN:           Int,           // 0 = no limit
    salAttrCount:   Int,           // for permutation cost formula
    description:    String
  )

  // ── Load ViewSpec.csv ─────────────────────────────────────────────────────
  def loadViewSpec(specFile: String): Seq[ViewDef] = {
    val views = mutable.ArrayBuffer[ViewDef]()
    try {
      val lines = Source.fromFile(specFile).getLines()
      for (raw <- lines) {
        val line = raw.trim
        // skip blank lines and comment lines
        if (line.isEmpty || line.startsWith("#")) ()
        else {
          val e = line.split(",", -1).map(_.trim)
          if (e.length >= 11 && e(0) != "view_name") {
            val enabled = e(9).toUpperCase
            if (enabled == "Y") {
              views += ViewDef(
                viewName      = e(0),
                groupByLdgr   = if (e(1).isEmpty) Seq() else e(1).split("\\|").map(_.trim).toSeq,
                groupBySal    = if (e(2).isEmpty) Seq() else e(2).split("\\|").map(_.trim).toSeq,
                filterLdgrCol = e(3),
                filterLdgrVal = e(4),
                filterSalCol  = e(5),
                filterSalVal  = e(6),
                topN          = if (e(7).isEmpty) 0 else e(7).toInt,
                salAttrCount  = if (e(8).isEmpty) 0 else e(8).toInt,
                description   = e(10)
              )
            }
          }
        }
      }
    } catch {
      case _: java.io.FileNotFoundException =>
        println(s"WARNING: ViewSpec not found at $specFile — using built-in defaults")
        // Fall back to the three classic views
        views += ViewDef("TOP50",       Seq("ldgrIPID"), Seq(), "", "", "", "", 50, 0, "Top 50 vendors by spend")
        views += ViewDef("DETAIL_BAL",  Seq("ldgrIPID","ldgrNominalAccountID","ldgrLedgerPeriod","ldgrLedgerID","ldgrBookCodeID"), Seq(), "", "", "", "", 0, 0, "Subledger view")
        views += ViewDef("SUMMARY_BAL", Seq("ldgrLegalEntityID","ldgrCenterID","ldgrProjectID","ldgrProductID","ldgrNominalAccountID","ldgrLedgerPeriod","ldgrLedgerID","ldgrBookCodeID"), Seq(), "", "", "", "", 0, 0, "GL summary view")
    }
    views.toSeq
  }

  // ── Load VendorMaster into a Map[instID -> Map[colName -> value]] ─────────
  // Reads the header row so it works regardless of column order or additions.
  def loadVendorMaster(vmFile: String): Map[String, Map[String, String]] = {
    val result = mutable.Map[String, Map[String, String]]()
    try {
      val src   = Source.fromFile(vmFile)
      val lines = src.getLines()
      if (!lines.hasNext) return result.toMap
      val headers = lines.next().split(",", -1).map(_.trim)
      val instIDIdx = headers.indexOf("instID")
      if (instIDIdx < 0) {
        println(s"WARNING: VendorMaster at $vmFile has no instID column — skipping")
        return result.toMap
      }
      for (line <- lines if line.trim.nonEmpty) {
        val e = line.split(",", -1).map(_.trim)
        if (e.length == headers.length) {
          val instID = e(instIDIdx)
          result(instID) = headers.zip(e).toMap
        }
      }
      src.close()
      println(s"VendorMaster loaded: ${result.size} instruments  columns: ${headers.mkString(", ")}")
    } catch {
      case _: java.io.FileNotFoundException =>
        println(s"WARNING: No VendorMaster at $vmFile — CAR attributes will be blank")
    }
    result.toMap
  }

  // ── Permutation cost formula: Σ C(m,k) * v^k  for k = 1..m ─────────────
  // m = number of CAR attributes; v = number of unique instruments
  // This is the equivalent key-embedded balance cost to answer the same views.
  def permutationCost(m: Int, v: Long): Long = {
    if (m <= 0) return 0L
    var total = 0L
    for (k <- 1 to m) {
      val comb = combinations(m, k)
      val vpow = math.pow(v.toDouble, k).toLong
      total += comb * vpow
    }
    total
  }

  def combinations(n: Int, k: Int): Long = {
    if (k > n) return 0L
    if (k == 0 || k == n) return 1L
    var num = 1L; var den = 1L
    for (i <- 0 until k) { num *= (n - i); den *= (i + 1) }
    num / den
  }

  // ── Main entry point ──────────────────────────────────────────────────────
  def apply(fileOutLocation: String): Unit = {
    // ViewSpec.csv lives alongside VendorMaster.csv in the data directory.
    // Try outPath first, then fall back to the repo data/ directory.
    val specFile = fileOutLocation + "ViewSpec.csv"
    apply(fileOutLocation, specFile)
  }

  def apply(fileOutLocation: String, viewSpecFile: String): Unit = {

    println("*" * 100)
    println("                      Data Aggregation Module")
    println("               FSP Pattern: Subledgers / Aggregation")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    if (fileOutLocation.isEmpty) {
      println("Invalid output file location. Program abort.")
      sys.exit(1)
    }

    val dataPath      = fileOutLocation
    // Config files (ViewSpec, VendorMaster) live in the same directory as ViewSpec.csv
    // (inPath when called from CLI), not necessarily in outPath.
    val configPath    = {
      val sep = viewSpecFile.lastIndexOf('/')
      val sep2 = viewSpecFile.lastIndexOf('\\')
      val idx = math.max(sep, sep2)
      if (idx >= 0) viewSpecFile.substring(0, idx + 1) else "./"
    }
    val fileDelimiter = ","

    // Load view spec
    val views = loadViewSpec(viewSpecFile)
    println(s"ViewSpec loaded: ${views.size} enabled views from $viewSpecFile")
    views.foreach(v => println(s"  view: ${v.viewName}  ldgr_dims: ${v.groupByLdgr.mkString("|")}  sal_dims: ${v.groupBySal.mkString("|")}"))

    // Load VendorMaster (Contract Attributes Record) from configPath (same dir as ViewSpec)
    val vendorMaster = loadVendorMaster(configPath + "VendorMaster.csv")
    val vendorCount  = vendorMaster.size

    // Open (or append to) pivot_results.csv
    val pivotFile    = dataPath + "pivot_results.csv"
    val pivotExists  = new File(pivotFile).exists()
    val pivotOut     = new PrintWriter(new java.io.FileOutputStream(new File(pivotFile), true))
    if (!pivotExists)
      pivotOut.write("run_timestamp,year,view_name,sal_attr_count,views_answerable,instrument_count," +
                     "key_embedded_equivalent_balances,ratio,description\n")

    val runTimestamp  = Calendar.getInstance.getTime.toString
    val ldgrFiles     = getListOfFiles(dataPath, "LDGR").sortWith(_.getName < _.getName)

    if (ldgrFiles.isEmpty) {
      println(s"No LDGR files found in $dataPath — nothing to aggregate.")
      pivotOut.close(); return
    }

    var totalFilesProcessed = 0

    for (ldgrFile <- ldgrFiles) {
      totalFilesProcessed += 1
      val yearStr = ldgrFile.getName.stripPrefix("LDGR").stripSuffix(".csv")
      println(s"\nAggregating: ${ldgrFile.getName}  (year=$yearStr)  views=${views.size}")

      // ── Single pass: load all LDGR rows into memory ──────────────────────
      // We need multiple passes (one per view) — load once, iterate N times.
      case class LdgrRow(e: Array[String], instID: String, amount: BigDecimal)

      val allRows    = mutable.ArrayBuffer[LdgrRow]()
      var balancesRead  = 0
      var contraSkipped = 0

      val lines = Source.fromFile(ldgrFile).getLines().drop(1)
      for (line <- lines if line.trim.nonEmpty) {
        val e      = line.split(fileDelimiter, -1).map(_.trim)
        val instID = e(0)
        if (instID.nonEmpty) {
          balancesRead += 1
          allRows += LdgrRow(e, instID, BigDecimal(e(19)))
        } else {
          contraSkipped += 1
        }
      }
      println(s"  LDGR rows loaded: $balancesRead  Contra rows skipped: $contraSkipped")

      // ── Run each view ─────────────────────────────────────────────────────
      var viewsAnswerable = 0

      for (vd <- views) {

        // Build the composite grouping key for this row
        def rowKey(row: LdgrRow): Option[String] = {
          // Check LDGR filter
          if (vd.filterLdgrCol.nonEmpty) {
            val colIdx = LdgrCols.getOrElse(vd.filterLdgrCol, -1)
            if (colIdx < 0 || colIdx >= row.e.length) return None
            if (row.e(colIdx) != vd.filterLdgrVal) return None
          }

          // Resolve CAR row (may be absent)
          val salRow: Map[String, String] =
            if (vd.groupBySal.nonEmpty || vd.filterSalCol.nonEmpty)
              vendorMaster.getOrElse(row.instID, Map.empty)
            else Map.empty

          // Check CAR filter
          if (vd.filterSalCol.nonEmpty) {
            val v = salRow.getOrElse(vd.filterSalCol, "")
            if (v != vd.filterSalVal) return None
          }

          // Build key parts from LDGR dimensions
          val ldgrParts = vd.groupByLdgr.map { col =>
            val idx = LdgrCols.getOrElse(col, -1)
            if (idx >= 0 && idx < row.e.length) row.e(idx) else ""
          }

          // Build key parts from CAR dimensions
          val salParts = vd.groupBySal.map { col =>
            salRow.getOrElse(col, "")
          }

          Some((ldgrParts ++ salParts).mkString("|"))
        }

        // Accumulate: key -> (totalAmount, vendorName for TOP-N)
        val accum = mutable.Map[String, BigDecimal]().withDefaultValue(BigDecimal(0))
        val keyToName = mutable.Map[String, String]()   // for TOP-N label enrichment

        for (row <- allRows) {
          rowKey(row) match {
            case Some(k) =>
              accum(k) = accum(k) + (if (vd.topN > 0) row.amount.abs else row.amount)
              if (vd.topN > 0 && !keyToName.contains(k))
                keyToName(k) = vendorMaster.getOrElse(row.instID, Map.empty)
                                           .getOrElse("instHolderName", "")
            case None => // filtered out
          }
        }

        if (accum.isEmpty) {
          println(s"  [${vd.viewName}] 0 rows — skipped (all rows filtered or no data)")
        } else {
          viewsAnswerable += 1

          // Build header: LDGR dims + CAR dims + [vendorName if TOP-N] + totalAmount
          val ldgrHeaders = vd.groupByLdgr.mkString(",")
          val salHeaders  = if (vd.groupBySal.nonEmpty) "," + vd.groupBySal.mkString(",") else ""
          val nameHeader  = if (vd.topN > 0) ",rank,vendorName" else ""
          val header      = ldgrHeaders + salHeaders + nameHeader + ",totalAmount\n"

          val outFile = dataPath + s"${vd.viewName}_$yearStr.csv"
          val out     = new PrintWriter(new File(outFile))
          out.write(header)

          val sorted = if (vd.topN > 0)
            accum.toSeq.sortBy(_._2)(Ordering[BigDecimal].reverse).take(vd.topN)
          else
            accum.toSeq.sortBy(_._1)

          sorted.zipWithIndex.foreach { case ((k, amt), idx) =>
            val rankCol = if (vd.topN > 0) s",${idx + 1},${keyToName.getOrElse(k, "")}" else ""
            out.write(s"$k$rankCol,$amt\n")
          }
          out.close()
          println(s"  [${vd.viewName}] ${accum.size} rows → $outFile")

          // ── Pivot results log ───────────────────────────────────────────
          val permCost = permutationCost(vd.salAttrCount, vendorCount)
          val ratio    = if (vd.salAttrCount > 0 && vendorCount > 0)
            f"${permCost.toDouble / vendorCount.toDouble}%.2f" else "N/A"
          pivotOut.write(s"$runTimestamp,$yearStr,${vd.viewName},${vd.salAttrCount}," +
            s"$viewsAnswerable,$vendorCount,$permCost,$ratio,${vd.description}\n")
        }
      }

      // ── Subledger-to-GL reconciliation proof ─────────────────────────────
      // Re-run DETAIL_BAL and SUMMARY_BAL views if both exist in spec, and compare totals.
      val hasDetail  = views.exists(v => v.viewName == "DETAIL_BAL")
      val hasSummary = views.exists(v => v.viewName == "SUMMARY_BAL")

      if (hasDetail && hasSummary) {
        println("-" * 60)
        println("  Subledger-to-GL Reconciliation Proof:")

        // Accumulate detail by (legalEntity, nominalAccount)
        val detailByLE  = mutable.Map[(String, String), BigDecimal]().withDefaultValue(BigDecimal(0))
        val summaryByLE = mutable.Map[(String, String), BigDecimal]().withDefaultValue(BigDecimal(0))

        for (row <- allRows) {
          val le   = if (row.e.length > 8)  row.e(8)  else ""
          val na   = if (row.e.length > 12) row.e(12) else ""
          detailByLE((le, na))  = detailByLE((le, na))  + row.amount
          summaryByLE((le, na)) = summaryByLE((le, na)) + row.amount
        }

        var proofPassed = true; var diffCount = 0
        val allKeys = (detailByLE.keySet ++ summaryByLE.keySet).toSeq.sorted
        for (k <- allKeys) {
          val d = detailByLE(k); val s = summaryByLE(k)
          if (d != s) {
            proofPassed = false; diffCount += 1
            println(s"    ✗ MISMATCH  LegalEntity=${k._1} NominalAccount=${k._2}  detail=$d  summary=$s  diff=${d - s}")
          }
        }
        if (proofPassed) println(s"  ✓ SUBLEDGER-TO-GL PROOF PASSED — year $yearStr  (${allKeys.size} buckets)")
        else             println(s"  ✗ SUBLEDGER-TO-GL PROOF FAILED — year $yearStr  ($diffCount mismatched buckets)")
        println("-" * 60)
      }
    }

    pivotOut.close()

    // ── Control totals ────────────────────────────────────────────────────
    println("*" * 100)
    println("Data Aggregation — Control Totals")
    println(s"  LDGR files processed:   $totalFilesProcessed")
    println(s"  Instruments in CAR:     $vendorCount")
    println(s"  Views defined:          ${views.size}")
    println(s"  Pivot results log:      ${dataPath}pivot_results.csv")
    println(s"  Process End Time:       ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }
}
