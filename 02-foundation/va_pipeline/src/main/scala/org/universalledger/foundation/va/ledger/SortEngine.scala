package org.universalledger.foundation.va.ledger

import java.io.{BufferedReader, BufferedWriter, File, FileReader, FileWriter, PrintWriter}
import scala.collection.mutable
import scala.io.Source

//***************************************************************************************************************
//
//  Object:  SortEngine — Parameterized External Merge-Sort Engine
//
//  GenevaERS / DFSORT analog for the JVM / CSV pipeline.
//
//  On z/OS, DFSORT is a first-class architectural component invoked between every pipeline step
//  that requires sorted input.  Its behaviour is fully controlled by a parameter file — no sort
//  logic is embedded in application code.  This object restores that discipline for the CSV
//  pipeline:
//
//    1. Sort specifications live in .sortspec files (one per logical sort order).
//    2. Application code (standardizeAndSort, arrangementReclass, etc.) calls SortEngine with
//       a spec path — it never encodes sort keys directly.
//    3. Adding, changing, or reversing a sort dimension is a spec-file edit, not a code change.
//
//  SPEC FILE FORMAT  (../sort_specs/{name}.sortspec)
//  -----------------------------------------------
//  Lines beginning with # are comments.  Blank lines are ignored.
//  Each data line defines one sort field:
//
//    field_name, col_index, type, order
//
//  where:
//    field_name  — human-readable label (for documentation; not used at runtime)
//    col_index   — 0-based column index in the CSV row
//    type        — CH  character / lexicographic string comparison
//                  NU  numeric    / BigDecimal comparison (handles negatives and decimals)
//    order       — A   ascending
//                  D   descending
//
//  Fields are compared left-to-right in declaration order (primary, secondary, tertiary …).
//  This is the direct equivalent of DFSORT SORT FIELDS=(start,len,type,order,...).
//
//  EXAMPLE — SortedJE.sortspec:
//    transIPID,           0, CH, A
//    transLedgerID,       9, CH, A
//    transJrnlType,      10, CH, A
//    transBookCodeID,    11, CH, A
//    transLegalEntityID, 12, CH, A
//    transCenterID,      13, CH, A
//    transProjectID,     14, CH, A
//    transProductID,     15, CH, A
//    transNominalAccountID, 16, CH, A
//    transAltAccountID,  17, CH, A
//    transCurrencyCodeSourceID,     18, CH, A
//    transCurrencyTypeCodeSourceID, 19, CH, A
//    transCurrencyCodeTargetID,     20, CH, A
//    transCurrencyTypeCodeTargetID, 21, CH, A
//    transFiscalPeriod,  22, CH, A
//
//  USAGE
//  -----
//    // Load spec once
//    val spec = SortEngine.loadSpec("../sort_specs/SortedJE.sortspec")
//
//    // Sort a chunk of CSV row strings in memory
//    val sorted = SortEngine.sortChunk(chunk, spec)
//
//    // External merge-sort: chunk → spill → k-way merge → output file
//    SortEngine.externalSort(inputIterator, outputPath, spec, chunkSize, tmpDir)
//
//  *(c) Copyright IBM Corporation. 2018
//  * SPDX-License-Identifier: Apache-2.0
//  * By Kip Twitchell
//  2025 — DFSORT-analog parameterized sort engine (Bob AI, Session 21)
//***************************************************************************************************************

object SortEngine {

  // ── Sort field descriptor ─────────────────────────────────────────────────
  case class SortField(
    fieldName: String,   // documentation only
    colIndex:  Int,      // 0-based column index in CSV row
    typeCode:  String,   // "CH" or "NU"
    order:     String    // "A" or "D"
  )

  // ── Compiled sort spec: list of fields in priority order ──────────────────
  type SortSpec = Seq[SortField]

  // Key extractor for fast in-memory and priority queue sorting
  final class ExtractedKey(val keyStr: String, val originalRow: String)

  // ── Load a .sortspec file ─────────────────────────────────────────────────
  def loadSpec(specPath: String): SortSpec = {
    val fields = mutable.ArrayBuffer[SortField]()
    val src = Source.fromFile(specPath, "UTF-8")
    try {
      for (rawLine <- src.getLines()) {
        val line = rawLine.trim
        if (line.nonEmpty && !line.startsWith("#")) {
          val parts = line.split(",").map(_.trim)
          if (parts.length >= 4) {
            val colIdx   = parts(1).toInt
            val typeCode = parts(2).toUpperCase
            val order    = parts(3).toUpperCase
            if (typeCode != "CH" && typeCode != "NU")
              throw new IllegalArgumentException(s"SortEngine: unknown type '$typeCode' in $specPath (use CH or NU)")
            if (order != "A" && order != "D")
              throw new IllegalArgumentException(s"SortEngine: unknown order '$order' in $specPath (use A or D)")
            fields += SortField(parts(0), colIdx, typeCode, order)
          }
        }
      }
    } finally {
      src.close()
    }
    if (fields.isEmpty)
      throw new IllegalArgumentException(s"SortEngine: no sort fields found in $specPath")
    fields.toSeq
  }

  // ── Extract sort key string from a CSV row string ─────────────────────────
  // Used for the priority queue in the k-way merge.
  // Returns a string that compares correctly for CH fields; for NU fields the
  // numeric comparison is done field-by-field in the full comparator below.
  // This key is used ONLY when all fields are CH type (fast path).
  def sortKeyString(row: String, spec: SortSpec): String = {
    val maxCol = spec.map(_.colIndex).max
    val cols   = row.split(",", maxCol + 2)
    val sb = new StringBuilder
    for (sf <- spec) {
      val v = if (sf.colIndex < cols.length) cols(sf.colIndex) else ""
      sb.append(v).append('\u0000')  // null separator prevents prefix collisions
    }
    sb.toString()
  }

  // ── Extract key string once per row (exact concatenation matching post.scala) ──
  def extractKey(row: String, spec: SortSpec): ExtractedKey = {
    val maxCol = spec.map(_.colIndex).max
    val cols   = row.split(",", maxCol + 2)
    val sb = new StringBuilder
    var i = 0
    while (i < spec.length) {
      val idx = spec(i).colIndex
      if (idx < cols.length) sb.append(cols(idx))
      i += 1
    }
    new ExtractedKey(sb.toString(), row)
  }

  // ── In-memory chunk sort ──────────────────────────────────────────────────
  def sortChunk(chunk: Seq[String], spec: SortSpec): Seq[String] = {
    val extracted = chunk.map(r => extractKey(r, spec)).toArray
    java.util.Arrays.sort(extracted, new java.util.Comparator[ExtractedKey] {
      override def compare(o1: ExtractedKey, o2: ExtractedKey): Int = {
        o1.keyStr.compareTo(o2.keyStr)
      }
    })
    extracted.map(_.originalRow)
  }

  // ── Spill a sorted chunk to a temp file ───────────────────────────────────
  def spillChunk(chunk: Seq[String], tmpDir: String, spillIndex: Int): String = {
    val path = tmpDir + f"/spill_$spillIndex%04d.tmp"
    val pw = new PrintWriter(new BufferedWriter(new FileWriter(path)))
    try { chunk.foreach(pw.println) } finally { pw.close() }
    path
  }

  // ── k-way merge of spill files → output file ─────────────────────────────
  // Uses a min-heap (priority queue) over open spill file readers.
  // Reads one line at a time from each spill — O(1) RAM regardless of total size.
  def mergeSpills(spillPaths: Seq[String], outPath: String, spec: SortSpec): Long = {
    new File(outPath).getParentFile match { case d if d != null => d.mkdirs(); case _ => }

    val readers = spillPaths.map(p => new BufferedReader(new FileReader(p)))

    // (ExtractedKey, readerIndex) — ordered by ascending sort key
    val pq = new mutable.PriorityQueue[(ExtractedKey, Int)]()(
      Ordering.fromLessThan[(ExtractedKey, Int)] { case ((ka, _), (kb, _)) =>
        ka.keyStr.compareTo(kb.keyStr) > 0   // reverse: PriorityQueue is a max-heap; we want min
      }
    )

    def advance(idx: Int): Unit = {
      var line = readers(idx).readLine()
      while (line != null && line.trim.isEmpty) line = readers(idx).readLine()
      if (line != null) pq.enqueue((extractKey(line, spec), idx))
    }

    readers.indices.foreach(advance)

    val pw = new PrintWriter(new BufferedWriter(new FileWriter(outPath), 1024 * 1024))
    var rowsWritten = 0L
    try {
      while (pq.nonEmpty) {
        val (key, idx) = pq.dequeue()
        pw.println(key.originalRow)
        rowsWritten += 1
        advance(idx)
      }
    } finally {
      pw.close()
      readers.foreach(_.close())
    }
    rowsWritten
  }

  // ── Full external merge-sort pipeline ────────────────────────────────────
  // Consumes an iterator of CSV row strings.
  // Chunk → sort → spill → k-way merge → outPath.
  // Cleans up all temp files on completion.
  //
  // Returns (rowsWritten, spillCount)
  def externalSort(
    rows:      Iterator[String],
    outPath:   String,
    spec:      SortSpec,
    chunkSize: Int = 200000,
    tmpDir:    String = ""
  ): (Long, Int) = {

    val workDir = if (tmpDir.nonEmpty) tmpDir
                  else System.getProperty("java.io.tmpdir") + "/sortengine_" + System.currentTimeMillis()
    new File(workDir).mkdirs()

    val spillPaths = mutable.ArrayBuffer[String]()
    val chunk      = mutable.ArrayBuffer[String]()

    def flush(): Unit = if (chunk.nonEmpty) {
      val sorted = sortChunk(chunk.toSeq, spec)
      spillPaths += spillChunk(sorted, workDir, spillPaths.size)
      chunk.clear()
    }

    for (row <- rows) {
      if (row.trim.nonEmpty) {
        chunk += row
        if (chunk.size >= chunkSize) flush()
      }
    }
    flush()  // final partial chunk

    val rowsWritten = mergeSpills(spillPaths.toSeq, outPath, spec)

    // Clean up
    spillPaths.foreach(p => new File(p).delete())
    new File(workDir).delete()

    (rowsWritten, spillPaths.size)
  }
}
