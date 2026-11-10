package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter, BufferedWriter, FileWriter}
import java.io.{File, PrintWriter, BufferedWriter, FileWriter}
import java.nio.charset.StandardCharsets
import java.util.Calendar

import org.apache.commons.csv.{CSVFormat, CSVParser}

import org.universalledger.foundation.va.corpus.CorpusProfile

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

// ── PO column indices in the VA_opendata_FY20YY.txt tab-delimited file ──────────────────────────
//   0  PONUMBER
//   1  AGENCY
//   2  ORDEREDDATE
//   3  VENDORCOMMODITYDESC
//   4  QUANTITYORDERED
//   5  PRICE
//   6  VENDORID
//   7  VENDORNAME
//   8  VENDORADDRESS
//   9  VENDORCITY
//  10  VENDORSTATE
//  11  VENDORPOSTALCODE
//  12  VENDORLOC_EMAILADDRESS
//  13  NIGPCODE
//  14  NIGPDESCRIPTION
//  15  UOMCode
//  16  UOMDesc
//  17  VENDORPARTNUMBER
//  18  MANPARTNUMBER
//
// PO SJE column mapping (same 39-field schema as payment SJEs):
//   transIPID (0)             = VENDORID (col 6)
//   transContractID (1)       = "0"
//   transCommitmentID (2)     = PONUMBER (col 0)  ← commitment identity
//   transBusinessEventCode(5) = "COMMITMENT"
//   transLegalEntityID (12)   = AGENCY (col 1)
//   transCenterID (13)        = "0000"
//   transProjectID (14)       = NIGPCODE (col 13) ← commodity classification
//   transNominalAccountID(16) = "COMMIT"
//   transFiscalPeriod (22)    = fiscal year (4-digit)
//   transTransDate (24)       = ORDEREDDATE (col 2)
//   transTransAmount (25)     = PRICE * QUANTITYORDERED (positive on debit, negative on credit)
//   transUnitOfMeasure (26)   = UOMCode (col 15)
//   transUnitPrice (27)       = PRICE (col 5)
//   transStatisticAmount (28) = QUANTITYORDERED (col 4)
//
// CLI invocation for Step 2p (via LedgerApp dispatcher):
//   sbt "run --step 2p --inPath data/VAPOData/ --outPath data/output/ --year 2003 --quarter 1"

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
    val t0 = System.currentTimeMillis()

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
    val tmpDir    = new File(outPath, ".sortedJE_spill_" + System.currentTimeMillis()).getPath
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

    def processRow(cols: Array[String], rowNumber: Int): Unit = {
      validateSourceRow(cols, recFormat, rowNumber)
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

    val inF = new File(inFile)
    if (!inF.exists()) {
      System.err.println(s"ERROR: Input file not found: $inFile")
      sys.exit(1)
    }

    val delimiter = if (recFormat == "A" || recFormat == "B" || recFormat == "RA") "\t" else ","

    if (delimiter == ",") {
      val parser = CSVParser.parse(inF, StandardCharsets.UTF_8, CSVFormat.DEFAULT)
      try {
        val records = parser.iterator()
        if (!records.hasNext) throw new IllegalArgumentException(s"Missing source header in $inFile")
        val header = records.next()
        validateSourceHeader((0 until header.size()).map(header.get).toArray, recFormat)
        var rowNumber = 2
        while (records.hasNext) {
          val record = records.next()
          val cols = (0 until record.size()).map(record.get).toArray
          if (cols.exists(_.trim.nonEmpty)) processRow(cols, rowNumber)
          rowNumber += 1
        }
      } finally {
        parser.close()
      }
    } else {
      implicit val codec = scala.io.Codec.UTF8.onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
      val src = Source.fromFile(inFile)(codec)
      try {
        val lines = src.getLines()
        if (!lines.hasNext) throw new IllegalArgumentException(s"Missing source header in $inFile")
        validateSourceHeader(splitAndStrip(lines.next(), delimiter), recFormat)
        for ((line, rowIndex) <- lines.zipWithIndex) {
          val raw = line.trim
          if (raw.nonEmpty) processRow(splitAndStrip(raw, delimiter), rowIndex + 2)
        }
      } finally {
        src.close()
      }
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

    val elapsedS  = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val inBytes   = new File(inFile).length()
    val outBytes  = new File(outFile).length()
    val peakRss   = Runtime.getRuntime.totalMemory()
    println(s"RESULT step=2 year=$year rc=0 records_read=$jrnlID records_written=$rowsWritten elapsed_s=$elapsedS input_bytes=$inBytes output_bytes=$outBytes")
    println(s"RESOURCE step=2 year=$year peak_rss_bytes=$peakRss")

    println("*" * 100)
    println(s"Process End Time: ${Calendar.getInstance.getTime}")
    println("*" * 100)
  }

  // ── Step 2p: PO standardize + external sort → SortedJEFY{yr}q{n}PO.csv ─────
  //
  //  Reads a tab-delimited VA PO file (VA_opendata_FY20YY.txt), emits two 39-field
  //  SJE rows per PO line (debit + credit, both signed so they cancel), sorts them
  //  using SortEngine.externalSort with the same SortedJE.sortspec as payment SJEs,
  //  and writes SortedJEFY{yr}q{n}PO.csv.
  //
  //  Memory contract: externalSort is O(chunkSize).  No ArrayBuffer of all rows.
  def applyPO(params: Map[String, String]): Unit = {
    val t0 = System.currentTimeMillis()

    println("*" * 100)
    println("                     Step 2p — PO Standardize & Sort (standardizeAndSort.applyPO)")
    println("Start Time: " + java.util.Calendar.getInstance.getTime)
    println("*" * 100)

    val inPath     = params.getOrElse("inPath",  "data/VAPOData/")
    val outPath    = params.getOrElse("outPath", "data/output/")
    val yearStr    = params.getOrElse("year",    "2003")
    val quarterStr = params.getOrElse("quarter", "1")
    val chunkSize  = params.get("chunkSize").map(_.toInt).getOrElse(DEFAULT_CHUNK)
    val dryRun     = params.getOrElse("dryRun", "false").toLowerCase == "true"
    // Axis 2 — period granularity: "monthly" → transFiscalPeriod = "YYYY/MM" from ORDEREDDATE.
    // "annual" (default) → transFiscalPeriod = "YYYY".
    val periodGranularity = params.getOrElse("periodGranularity", "annual").toLowerCase

    val shortYr  = yearStr.takeRight(2)
    val quarter  = quarterStr.toInt
    val inFile   = inPath.stripSuffix("/") + "/VA_opendata_FY20" + shortYr + ".txt"
    val outFile  = outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + "q" + quarter + "PO.csv"
    val acctDate = quarterToDate(yearStr, quarter)

    println(s"  Input file   : $inFile")
    println(s"  Output file  : $outFile")
    println(s"  Year         : $yearStr  Quarter: $quarter")
    println(s"  AcctDate     : $acctDate")

    val specParam = params.getOrElse("sortspec", "")
    val specPath  = resolveSpecPath(specParam)
    val sortSpec  = SortEngine.loadSpec(specPath)
    println(s"  SortSpec     : $specPath  (${sortSpec.size} fields)")
    println(s"  ChunkSize    : $chunkSize")

    val inF = new File(inFile)
    if (!inF.exists()) {
      System.err.println(s"ERROR: PO input file not found: $inFile")
      sys.exit(1)
    }

    val tmpDir = outPath.stripSuffix("/") + "/.sortedPO_spill_" + System.currentTimeMillis()
    new File(tmpDir).mkdirs()

    var sourceRows    = 0L   // records accepted into SJE rows
    var skippedRows   = 0L   // every quarantined record: reader rejects + the three field guards
    var readerRejects = 0L   // subset of skippedRows rejected by PORecordReader (structure)
    var totalRows     = 0L
    var totalAmt      = BigDecimal(0)
    var jrnlID        = 0

    // Structural reader: record boundaries come from the ORDEREDDATE anchor and the header field count,
    // not from quote parity (see PORecordReader). Nothing is dropped without a row in the quarantine file.
    val reader = new PORecordReader(new java.io.FileInputStream(inF))
    val quarantineFile = outPath.stripSuffix("/") + "/POQuarantineFY" + shortYr + ".csv"
    val qWriter = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(quarantineFile), StandardCharsets.UTF_8))
    qWriter.write(CorpusProfile.csvLine(CorpusProfile.QuarantineHeader))
    val quarantine = new CorpusProfile.Quarantine(qWriter)
    val sourceName = inF.getName
    val reasonCounts = mutable.LinkedHashMap[String, Long]()
    def quarantineItem(item: PORecordReader.Item, reason: String): Unit = {
      skippedRows += 1
      reasonCounts(reason) = reasonCounts.getOrElse(reason, 0L) + 1
      quarantine.add(sourceName, item.ordinal, item.startLine, item.startOffset, item.lines, reason, item.raw)
    }

    // Stream the PO file through the sort engine one record at a time.
    // The iterator yields comma-joined 39-field SJE strings; SortEngine.externalSort
    // handles the chunk/spill/merge cycle at O(chunkSize) peak heap.
    val rowIterator: Iterator[String] = new Iterator[String] {
      private var pending: List[String] = Nil

      override def hasNext: Boolean = pending.nonEmpty || fetchNext()

      private def fetchNext(): Boolean = {
        while (reader.hasNext) {
          reader.next() match {
            case rej: PORecordReader.Rej =>
              readerRejects += 1
              quarantineItem(rej, rej.reason)

            case rec: PORecordReader.Rec =>
              // Fields are already unquoted and trimmed by the reader (19 fields, header width).
              val cols     = rec.fields
              val poNumber = cols(0)
              // Pre-SJE address-fragment guard: a state+ZIP fragment in the PONUMBER field.
              // Pattern: optional whitespace, two uppercase letters, whitespace, five digits.
              if (poNumber.matches("\\s*[A-Z]{2}\\s+\\d{5}.*")) {
                Console.err.println(s"[applyPO] QUARANTINE address-fragment in PONUMBER field" +
                  s" (record ${rec.ordinal}, line ${rec.startLine}): '$poNumber' — skipping row")
                quarantineItem(rec, "ADDRESS_FRAGMENT")
              } else if (tryBigDecimal(cols, 5, null.asInstanceOf[BigDecimal]) == null && poNumber.matches(".*\\d.*")) {
                // Price field (col 5) is non-numeric and the PONUMBER looks real: a column shift that the
                // reader's field count cannot see. These rows would produce unbalanced SJE pairs.
                Console.err.println(s"[applyPO] QUARANTINE non-numeric price field" +
                  s" (record ${rec.ordinal}, line ${rec.startLine}): poNumber='$poNumber'" +
                  s" price='${cols(5)}' — skipping row")
                quarantineItem(rec, "BAD_PRICE")
              } else if (!cols(6).matches("[A-Za-z]{0,3}\\d+(-[A-Za-z]+)?")) {
                // Vendor-ID shape guard: transIPID is the leading CKB sort key, so a price, decimal or
                // description fragment in VENDORID would create a bogus vendor group.
                // Accepted shape (observed, not a vendor-master rule): 0-3 letters, digits, optional -SUFFIX (e.g. 178461687-BAD).
                Console.err.println(s"[applyPO] QUARANTINE implausible VENDORID" +
                  s" (record ${rec.ordinal}, line ${rec.startLine}): poNumber='$poNumber'" +
                  s" vendorId='${cols(6)}' — skipping row")
                quarantineItem(rec, "BAD_VENDOR_ID")
              } else {
                sourceRows += 1
                jrnlID     += 1
                val agency     = cols(1)
                val ordDate    = if (cols(2).nonEmpty) cols(2) else acctDate
                val commDesc   = cols(3).replace(",", " ")
                val qty        = tryBigDecimal(cols, 4, BigDecimal(1))
                val price      = tryBigDecimal(cols, 5, BigDecimal(0))
                val vendorId   = cols(6)
                val nigpCode   = cols(13)
                val uomCode    = cols(15)
                val amt        = price * qty

                val jid = "ID" + jrnlID + acctDate + "PO"

                // Build the fiscal period key for this PO row.
                // monthly → parse ORDEREDDATE (MM/DD/YYYY) → "YYYY/MM"; fall back to yearStr.
                val poPeriodKey: String = if (periodGranularity == "monthly") {
                  val dateParts = ordDate.split("/")
                  if (dateParts.length == 3 && dateParts(2).length == 4)
                    dateParts(2) + "/" + f"${dateParts(0).toInt}%02d"
                  else yearStr
                } else yearStr

                val debitRow  = makePORow(vendorId, poNumber, jid, "1", "COMMITMENT",
                                          agency, nigpCode, poPeriodKey, acctDate, ordDate,
                                          amt.toString(), uomCode, price.toString(), qty.toString(),
                                          commDesc)
                val creditRow = makePORow(vendorId, poNumber, jid, "2", "COMMITMENT",
                                          agency, "0000", poPeriodKey, acctDate, ordDate,
                                          (-amt).toString(), uomCode, price.toString(), qty.toString(),
                                          "COMMITMENT_OFFSET")

                totalRows += 2
                totalAmt  += amt + (-amt)   // always 0; tracked for control assertion
                pending = debitRow :: creditRow :: Nil
                return true
              }
          }
        }
        false
      }

      override def next(): String = {
        val h = pending.head
        pending = pending.tail
        h
      }
    }


    // Reconciliation (runs after the reader is drained): every physical line after the header is in exactly
    // one record, and every record is either accepted into SJE rows or written to the quarantine file.
    def reconcile(): Unit = {
      qWriter.close()
      reader.close()
      if (reader.physicalLines != reader.groupedLines)
        throw new IllegalStateException(s"[applyPO] line reconcile failed for $inFile: physical=${reader.physicalLines} grouped=${reader.groupedLines}")
      if (reader.logicalRecords != sourceRows + skippedRows)
        throw new IllegalStateException(s"[applyPO] record reconcile failed for $inFile: records=${reader.logicalRecords} accepted=$sourceRows quarantined=$skippedRows")
    }

    if (dryRun) {
      // Drain the iterator for control-total accounting without writing
      rowIterator.foreach(_ => ())
      reconcile()
      println(s"\n--dryRun: source_rows=$sourceRows sje_rows=$totalRows quarantined=$skippedRows")
      new File(tmpDir).delete()
      return
    }

    val (rowsWritten, spillCount) = SortEngine.externalSort(rowIterator, outFile, sortSpec, chunkSize, tmpDir)
    new File(tmpDir).delete()
    reconcile()

    val elapsedS = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val inBytes  = inF.length()
    val outBytes = new File(outFile).length()
    val peakRss  = Runtime.getRuntime.totalMemory()

    println(f"\nControl totals:")
    println(f"  Physical lines      : ${reader.physicalLines}%,d  (all assigned to a record: ${reader.groupedLines}%,d)")
    println(f"  Logical records     : ${reader.logicalRecords}%,d  (= accepted + quarantined)")
    println(f"  PO records accepted : $sourceRows%,d")
    println(f"  Records quarantined : $skippedRows%,d  (reader rejects $readerRejects%,d; see $quarantineFile)")
    reasonCounts.foreach { case (r, n) => println(f"    $r%-20s $n%,d") }
    println(f"  SJE rows produced   : $rowsWritten%,d  (= 2 × accepted records)")
    println(f"  Spill files used    : $spillCount%d")
    println(f"  Sum of all amounts  : $totalAmt%.2f  (should be 0.00)")
    println(s"\nRESULT step=2p year=$yearStr rc=0 records_read=${reader.logicalRecords} records_written=$rowsWritten records_accepted=$sourceRows skipped_quarantine=$skippedRows reader_rejects=$readerRejects physical_lines=${reader.physicalLines} elapsed_s=$elapsedS input_bytes=$inBytes output_bytes=$outBytes")
    println(s"RESOURCE step=2p year=$yearStr peak_rss_bytes=$peakRss")
    println("STATUS: COMPLETE")
    println("*" * 100)
    println(s"Process End Time: ${java.util.Calendar.getInstance.getTime}")
    println("*" * 100)
  }

  // ── Helper: safely read a BigDecimal from a column, returning default on failure ──
  private def tryBigDecimal(cols: Array[String], idx: Int, default: BigDecimal): BigDecimal =
    if (idx < cols.length) try BigDecimal(cols(idx).replace(",", "")) catch { case _: Exception => default }
    else default

  // ── PO SJE row builder — 39-field comma-joined string ────────────────────────
  //  Same 39-column schema as makeRow() used by payment SJEs.
  //  Key differences from payments:
  //    col 2  (transCommitmentID) = poNumber  (marks this as a commitment event)
  //    col 5  (transBusinessEventCode) = bizEvent ("COMMITMENT")
  //    col 14 (transProjectID)   = nigpCode  (commodity classification)
  //    col 16 (transNominalAccountID) = "COMMIT" on debit, "0000" on credit
  //    col 26 (transUnitOfMeasure)    = uomCode
  //    col 27 (transUnitPrice)        = price
  //    col 28 (transStatisticAmount)  = qty
  private def makePORow(
      instId:    String,
      poNumber:  String,
      jrnlId:    String,
      lineId:    String,
      bizEvent:  String,
      agency:    String,
      nigpCode:  String,
      year:      String,
      acctDate:  String,
      transDate: String,
      amount:    String,
      uomCode:   String,
      price:     String,
      qty:       String,
      descript:  String
  ): String = {
    def clean(s: String): String = if (s != null) s.replace(",", " ") else ""
    Seq(
      clean(instId), "0", clean(poNumber), clean(jrnlId), clean(lineId), clean(bizEvent), "PO Sys", "Unknown", clean(descript),
      LEDGER_ID, JRNL_TYPE, BOOK_CODE,
      clean(agency), "0000", clean(nigpCode), PRODUCT_ID, if (lineId == "1") "COMMIT" else "0000", ALT_ACCOUNT,
      CCY_SRC, CCY_TYPE_SRC, CCY_TGT, CCY_TYPE_TGT,
      clean(year), clean(acctDate), clean(transDate), amount,
      clean(uomCode), price, qty, " ", " ", "D", "N", "N", " ", " ", " ", " ", " "
    ).mkString(",")
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

  private def validateSourceHeader(rawHeader: Array[String], recFormat: String): Unit = {
    val header = rawHeader.map(_.stripPrefix("\uFEFF").trim.toUpperCase).toSeq
    val expenseBase = Seq(
      "AGY_AGENCY_KEY", "FNDDTL_FUND_DETAIL_KEY", "OBJ_OBJECT_KEY",
      "SPRG_SUB_PROGRAM_KEY", "VENDOR_NAME", "AMOUNT"
    )
    val valid = recFormat match {
      case "A" => header == expenseBase
      case "B" => header.length == 7 && header.take(6) == expenseBase && header(6).contains("DATE")
      case "C" => header.length == 7 && header.take(6) == Seq(
        "AGY_AGENCY_KEY", "AMOUNT", "FNDDTL_FUND_DETAIL_KEY", "OBJ_OBJECT_KEY",
        "SPRG_SUB_PROGRAM_KEY", "VENDOR_NAME"
      ) && header(6).contains("DATE")
      case "RA" => header == Seq(
        "AGY_AGENCY_KEY", "FNDDTL_FUND_DETAIL_KEY", "SRC_SOURCE_KEY", "AMOUNT"
      )
      case "RC" => header.length == 5 && header.take(4) == Seq(
        "AGY_AGENCY_KEY", "AMOUNT", "FNDDTL_FUND_DETAIL_KEY", "SRC_SOURCE_KEY"
      ) && header(4).contains("DATE")
      case _ => false
    }
    if (!valid)
      throw new IllegalArgumentException(s"Unexpected $recFormat source header: ${header.mkString(",")}")
  }

  private def validateSourceRow(cols: Array[String], recFormat: String, rowNumber: Int): Unit = {
    val (expectedColumns, amountIndex) = recFormat match {
      case "A" => (6, 5)
      case "B" => (7, 5)
      case "C" => (7, 1)
      case "RA" => (4, 3)
      case "RC" => (5, 1)
      case _ => throw new IllegalArgumentException(s"Unsupported source format: $recFormat")
    }
    if (cols.length != expectedColumns)
      throw new IllegalArgumentException(
        s"Source row $rowNumber has ${cols.length} fields; expected $expectedColumns for $recFormat"
      )
    try BigDecimal(cols(amountIndex).replace(",", ""))
    catch {
      case _: NumberFormatException =>
        throw new IllegalArgumentException(s"Invalid amount in source row $rowNumber: ${cols(amountIndex)}")
    }
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
        val lines = source.getLines().filter(line => line.trim.nonEmpty && !line.trim.startsWith("#"))
        val expectedHeader = Seq(
          "recordType", "ruleSetId", "ruleVersion", "ruleId", "businessEventCode",
          "debitAccountExpression", "creditAccountExpression", "debitProjectExpression",
          "creditProjectExpression", "offsetDescription", "effectiveStart", "effectiveEnd"
        )
        if (!lines.hasNext) throw new IllegalArgumentException(s"Empty accounting rules file: $rulePath")
        val header = lines.next().stripPrefix("\uFEFF").split(",", -1).map(_.trim).toSeq
        if (header != expectedHeader)
          throw new IllegalArgumentException(s"Unexpected accounting rules header in $rulePath")
        lines.map { line =>
          val fields = line.split(",", -1).map(_.trim)
          if (fields.length != expectedHeader.length)
            throw new IllegalArgumentException(s"Invalid accounting rule row in $rulePath: $line")
          fields
        }
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
    def clean(s: String): String = if (s != null) s.replace(",", " ") else ""
    Seq(
      clean(instId), "0", "0", clean(jrnlId), clean(lineId), clean(bizEvent), clean(srcSys), "Unknown", clean(descript),
      LEDGER_ID, JRNL_TYPE, BOOK_CODE,
      clean(agency), clean(fund), clean(objCode), PRODUCT_ID, clean(nominalAcct), ALT_ACCOUNT,
      CCY_SRC, CCY_TYPE_SRC, CCY_TGT, CCY_TYPE_TGT,
      clean(year), clean(acctDate), clean(transDate), amount,
      " ", "0", "0", clean(ruleSetId), clean(ruleId), "D", "N", "N", " ", " ", " ", " ", " "
    ).mkString(",")
  }

  // ── JE pair builder — returns 0 or 2 (rowStr, matched, amount) tuples ─────
  private def buildJEPairs(
      cols: Array[String], recFormat: String, recType: String,
      vendorMap: Map[String, String],
      accountingRule: AccountingRule,
      year: String, acctDate: String, seqNo: Int
    ): Seq[(String, Boolean, BigDecimal)] = {

    if (recType == "E") buildExpensePairs(cols, recFormat, vendorMap, accountingRule, year, acctDate, seqNo)
    else                buildRevenuePairs(cols, recFormat, vendorMap, accountingRule, year, acctDate, seqNo)
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

  // ── Step ttl: APA expenditure/revenue TTL (Technical Transform Layer) ────────
  //
  //  The APA direct-download files carry human-readable names only — no numeric keys.
  //  This step is the TTL pass that resolves names → keys using the DPB reference tables
  //  (aRefAgencyCSV.txt, aRefFundCSV.txt, aRefObjectCSV.txt, aRefProgramCSV.txt /
  //  aRefSourceCSV.txt) and writes one SortedJEFY{yr}q{n}exp.csv per Virginia fiscal quarter
  //  (and one SortedJEFY{yr}rev.csv for revenue).
  //
  //  Virginia FY quarters (Jul-Jun):
  //    Q1 = Jul, Aug, Sep   Q2 = Oct, Nov, Dec
  //    Q3 = Jan, Feb, Mar   Q4 = Apr, May, Jun
  //
  //  APA expenditure schema (comma-delimited, BOM-prefixed):
  //    AGENCY_NAME | AMOUNT | FUND_NAME | LOCALITY | OBJECT_NAME | PROGRAM_NAME |
  //    VENDOR_NAME | VOUCHER_NUMBER | VOUCHER_DATE
  //
  //  APA revenue schema (comma-delimited, BOM-prefixed):
  //    AGENCY_NAME | AMOUNT | FUND_NAME | LOCALITY | SOURCE_TYPE | SOURCE | DEPOSIT_DATE
  //
  //  Output: same 39-field SortedJE CSV as step 2 — drop-in input for post.scala.
  //
  //  CLI invocation (via LedgerApp dispatcher):
  //    sbt "run --step ttl --inPath data/VARawFiles --refPath data --outPath data/output
  //             --year 2003"
  //
  //  Parameters:
  //    inPath   — directory containing the FY subdirectories (e.g. "FY 2003 Expenditure")
  //    refPath  — directory containing aRef*.txt reference tables (default = inPath/../)
  //    outPath  — directory for SortedJE output
  //    year     — 4-digit fiscal year (e.g. 2003)
  //    type     — E=Expenditure (default), R=Revenue, B=Both

  // ── Reference data container ─────────────────────────────────────────────────
  case class ApaReferenceData(
    agencyByName:  Map[String, String],   // UPPER(name) → AGY_AGENCY_KEY string
    fundByName:    Map[String, String],   // UPPER(name) → FNDDTL_FUND_DETAIL_KEY string
    objectByName:  Map[String, String],   // UPPER(name) → OBJ_OBJECT_KEY string
    programByName: Map[String, String],   // UPPER(name) → SPRG_SUB_PROGRAM_KEY string
    sourceByName:  Map[String, String]    // UPPER(name) → SRC_SOURCE_KEY string
  )

  // Load one reference table: returns Map[UPPER(nameCol) → keyCol]
  private def loadRefTable(path: String, keyCol: String, nameCol: String): Map[String, String] = {
    val result = mutable.HashMap[String, String]()
    val f = new File(path)
    if (!f.exists()) {
      System.err.println(s"WARNING: Reference table not found: $path — name lookups will miss")
      return result.toMap
    }
    implicit val codec = scala.io.Codec.UTF8
      .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
      .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
    val src = Source.fromFile(f)(codec)
    try {
      val lines = src.getLines()
      if (!lines.hasNext) return result.toMap
      val rawHeader = lines.next().stripPrefix("\uFEFF").split(",", -1).map(_.trim)
      val ki = rawHeader.indexOf(keyCol)
      val ni = rawHeader.indexOf(nameCol)
      if (ki < 0 || ni < 0) {
        System.err.println(s"WARNING: $path missing column '$keyCol' or '$nameCol'")
        return result.toMap
      }
      for (line <- lines) {
        val cols = line.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
        if (cols.length > math.max(ki, ni)) {
          val key  = cols(ki)
          val name = cols(ni).toUpperCase
          if (key.nonEmpty && name.nonEmpty) result(name) = key
        }
      }
    } finally src.close()
    result.toMap
  }

  def loadApaReferenceData(refPath: String): ApaReferenceData = {
    val base = refPath.stripSuffix("/")
    ApaReferenceData(
      agencyByName  = loadRefTable(s"$base/aRefAgencyCSV.txt",  "AGY_AGENCY_KEY",        "AGY_AGENCY_NAME"),
      fundByName    = loadRefTable(s"$base/aRefFundCSV.txt",    "FNDDTL_FUND_DETAIL_KEY", "FNDDTL_FUND_DETAIL_NAME"),
      objectByName  = loadRefTable(s"$base/aRefObjectCSV.txt",  "OBJ_OBJECT_KEY",         "4-DIGIT OBJECT NAME"),
      programByName = loadRefTable(s"$base/aRefProgramCSV.txt", "SPRG_SUB_PROGRAM_KEY",   "SPRG_SUB_PROGRAM_NAME"),
      sourceByName  = loadRefTable(s"$base/aRefSourceCSV.txt",  "SRC_SOURCE_KEY",         "SRC_SOURCE_NAME")
    )
  }

  // ── Month name → (quarter, calendarMonth) mapping for Virginia FY (Jul-Jun) ──
  // Returns (quarter 1-4, calendarMonth 1-12) or None for unrecognised names.
  private def apaMonthInfo(monthToken: String): Option[(Int, Int)] = {
    monthToken.toLowerCase.take(3) match {
      case "jul" => Some((1,  7))
      case "aug" => Some((1,  8))
      case "sep" | "sep" => Some((1,  9))
      case "oct" => Some((2, 10))
      case "nov" => Some((2, 11))
      case "dec" => Some((2, 12))
      case "jan" => Some((3,  1))
      case "feb" => Some((3,  2))
      case "mar" => Some((3,  3))
      case "apr" => Some((4,  4))
      case "may" => Some((4,  5))
      case "jun" => Some((4,  6))
      case _     => None
    }
  }

  // ── Discover APA monthly files for a given FY and type ───────────────────────
  // Returns list of (file, quarter, calendarMonth) sorted by month.
  private def discoverApaFiles(inPath: String, yearInt: Int, recType: String): Seq[(File, Int, Int)] = {
    val typeLabel = if (recType == "R") "Revenue" else "Expenditure"
    // Folder naming: "FY 2003 Expenditure", "All Expenditures for Fiscal Year 2014", etc.
    val root = new File(inPath.stripSuffix("/"))
    if (!root.exists()) return Seq.empty

    // Match the 4-digit fiscal year. "--year 04" arrives as 4; matching the bare digit would also
    // select "...Fiscal Year 2014". Folder names always carry the 4-digit year.
    val fullYear = if (yearInt < 100) 2000 + yearInt else yearInt
    val candidates = Option(root.listFiles()).getOrElse(Array.empty[File])
      .filter(_.isDirectory)
      .filter { dir =>
        val n = dir.getName.toLowerCase
        n.contains(fullYear.toString) && (
          if (recType == "R") n.contains("revenue") || n.contains("revenues")
          else n.contains("expenditure") || n.contains("expenditures")
        )
      }

    if (candidates.isEmpty) {
      System.err.println(s"WARNING: No $typeLabel directory found for FY$yearInt under $inPath")
      return Seq.empty
    }

    if (candidates.length > 1)
      System.err.println(s"WARNING: ${candidates.length} $typeLabel directories match FY$fullYear: " +
                         candidates.map(_.getName).sorted.mkString(", ") + " — using the first by name")
    val fyDir = candidates.sortBy(_.getName).head
    println(s"  Source folder ($typeLabel FY$fullYear): ${fyDir.getAbsolutePath}")
    val csvFiles = Option(fyDir.listFiles()).getOrElse(Array.empty[File])
      .filter(f => f.isFile && f.getName.toLowerCase.endsWith(".csv"))

    csvFiles.flatMap { f =>
      // Extract month token from filename, e.g. "Expenditures Apr 2003.csv" → "Apr"
      val monthToken = f.getName.split("[ _]").find(t => apaMonthInfo(t).isDefined).getOrElse("")
      apaMonthInfo(monthToken).map { case (q, m) => (f, q, m) }
    }.sortBy { case (_, _, m) =>
      // Sort so that within a FY: Jul(7)..Dec(12) then Jan(1)..Jun(6)
      if (m >= 7) m else m + 12
    }
  }

  // ── APA row TTL: name-resolve one expenditure row to format-A equivalent cols ─
  // Returns (agencyKey, fundKey, objKey, progKey, vendorName, amount, transDate) or None.
  private def resolveApaExpRow(cols: Array[String], ref: ApaReferenceData, rowNum: Int)
      : Option[(String, String, String, String, String, String, String)] = {
    if (cols.length < 7) return None
    val agencyName  = cols(0)
    val rawAmt      = cols(1)
    val fundName    = cols(2)
    // cols(3) = LOCALITY — not used
    val objectName  = cols(4)
    val programName = cols(5)
    val vendorName  = cols(6)
    val transDate   = if (cols.length > 8) cols(8) else ""

    try { BigDecimal(rawAmt.replace(",", "")) } catch { case _: Exception => return None }

    val agencyKey  = ref.agencyByName.getOrElse(agencyName.toUpperCase, "0")
    val fundKey    = ref.fundByName.getOrElse(fundName.toUpperCase, "0")
    val objKey     = ref.objectByName.getOrElse(objectName.toUpperCase, "0")
    val progKey    = ref.programByName.getOrElse(programName.toUpperCase, "0")

    Some((agencyKey, fundKey, objKey, progKey, vendorName, rawAmt, transDate))
  }

  // APA revenue row TTL: resolve one revenue row.
  private def resolveApaRevRow(cols: Array[String], ref: ApaReferenceData, rowNum: Int)
      : Option[(String, String, String, String, String)] = {
    if (cols.length < 6) return None
    val agencyName = cols(0)
    val rawAmt     = cols(1)
    val fundName   = cols(2)
    // cols(3) = LOCALITY, cols(4) = SOURCE_TYPE — not used
    val sourceName = cols(5)
    val transDate  = if (cols.length > 6) cols(6) else ""

    try { BigDecimal(rawAmt.replace(",", "")) } catch { case _: Exception => return None }

    val agencyKey  = ref.agencyByName.getOrElse(agencyName.toUpperCase, "0")
    val fundKey    = ref.fundByName.getOrElse(fundName.toUpperCase, "0")
    val srcKey     = ref.sourceByName.getOrElse(sourceName.toUpperCase, "0")

    Some((agencyKey, fundKey, srcKey, rawAmt, transDate))
  }

  // ── applyAPA: TTL entry point ─────────────────────────────────────────────────
  def applyAPA(params: Map[String, String]): Unit = {
    val t0 = System.currentTimeMillis()

    println("*" * 100)
    println("                   Step ttl — APA Technical Transform Layer (standardizeAndSort.applyAPA)")
    println("Start Time: " + java.util.Calendar.getInstance.getTime)
    println("*" * 100)

    val inPath    = params.getOrElse("inPath",   "data/VARawFiles/")
    val refPath   = params.getOrElse("refPath",  inPath.stripSuffix("/") + "/../")
    val outPath   = params.getOrElse("outPath",  "data/output/")
    val yearStr   = params.getOrElse("year",     "2003")
    val recTypes  = params.getOrElse("type",     "E").toUpperCase match {
                     case "B" => Seq("E", "R")
                     case t   => Seq(t)
                   }
    val chunkSize = params.get("chunkSize").map(_.toInt).getOrElse(DEFAULT_CHUNK)
    val dryRun    = params.getOrElse("dryRun", "false").toLowerCase == "true"
    // parseOnly: read, name-resolve and count each source file, but build no SJE pairs, sort
    // nothing and write nothing. Proves which folder/files are read and that every data line is
    // accounted for (source + unresolved_ref + blank). Not a measured run.
    val parseOnly = params.getOrElse("parseOnly", "false").toLowerCase == "true"
    val yearInt   = yearStr.toInt
    val shortYr   = yearStr.takeRight(2)
    // Axis 2 — period granularity: "monthly" → transFiscalPeriod = "YYYY/MM" per source file.
    // "annual" (default) → transFiscalPeriod = "YYYY" (one key for the whole FY).
    val periodGranularity = params.getOrElse("periodGranularity", "annual").toLowerCase

    val specParam = params.getOrElse("sortspec", "")
    val specPath  = resolveSpecPath(specParam)
    val sortSpec  = SortEngine.loadSpec(specPath)

    val vmPath       = params.getOrElse("vendormaster", "")
    val resolvedVmPath = resolveVmPath(vmPath)
    val vendorMap    = loadVendorMaster(resolvedVmPath)

    println(s"  inPath      : $inPath")
    println(s"  refPath     : $refPath")
    println(s"  outPath     : $outPath")
    println(s"  year        : $yearStr  types: ${recTypes.mkString(",")}")
    println(s"  SortSpec    : $specPath  (${sortSpec.size} fields)")
    println(s"  VendorMaster: $resolvedVmPath  (${vendorMap.size} entries)")
    println(s"  ChunkSize   : $chunkSize")

    // Load reference tables once for the whole FY run
    val ref = loadApaReferenceData(refPath)
    println(s"  Reference   : agency=${ref.agencyByName.size} fund=${ref.fundByName.size} " +
            s"object=${ref.objectByName.size} program=${ref.programByName.size} source=${ref.sourceByName.size}")

    var totalSourceRows  = 0L
    var totalSjeRows     = 0L
    var totalUnresolved  = 0L

    for (recType <- recTypes) {
      val accountingRule = loadAccountingRule(params.get("accountingRules"), params.get("refPath"), recType)
      val files = discoverApaFiles(inPath, yearInt, recType)

      if (files.isEmpty) {
        println(s"  WARNING: No APA ${recType} files discovered for FY$yearInt — skipping")
      } else {
        println(s"  Discovered ${files.size} ${recType} files for FY$yearInt")
      }

      // ── Dispatch: per-source-file loop (monthly) vs. quarterly grouping (annual/quarterly) ──
      //
      // monthly: one output file per discovered source file.
      //   Expenditure output name: SortedJEFY{yr}_{YYYY}_{MM}_exp.csv
      //   Revenue output name:     SortedJEFY{yr}_rev.csv  (revenue is always a single annual file)
      //   transFiscalPeriod = "YYYY/MM" per source file.
      //
      // annual / quarterly (default): group files by quarter, one output per quarter.
      //   Expenditure output name: SortedJEFY{yr}q{n}exp.csv
      //   Revenue output name:     SortedJEFY{yr}rev.csv
      //   transFiscalPeriod = "YYYY" (annual) or "YYYY/Q{n}" (quarterly — not yet implemented).

      // ── Helper: process a batch of APA files into a single SortedJE output file ──
      def processApaFileBatch(
          batchFiles:  Seq[(File, Int, Int)],
          batchLabel:  String,
          outFile:     String,
          acctDate:    String,
          granularity: String
      ): Unit = {
        println(s"\n  Processing ${recType} $batchLabel → $outFile  (${batchFiles.size} file(s))")

        val tmpDir = outPath.stripSuffix("/") + "/.apa_spill_" + recType + "_" +
                     batchLabel.replace("/", "_") + "_" + System.currentTimeMillis()
        new File(tmpDir).mkdirs()
        val spills = mutable.ArrayBuffer[String]()
        val chunk  = mutable.ArrayBuffer[String]()
        var sourceRows   = 0L
        var sjeRows      = 0L
        var unmatchedRef = 0L
        var jrnlID       = 0
        var fileDataLines = 0L
        var fileBlank     = 0L
        var fileSource    = 0L
        var fileUnmatched = 0L

        def flushChunk(): Unit = {
          if (chunk.nonEmpty) {
            val spillPath = SortEngine.spillChunk(SortEngine.sortChunk(chunk.toSeq, sortSpec), tmpDir, spills.size)
            spills += spillPath
            chunk.clear()
            print(s"  Spilled chunk ${spills.size}  ($sjeRows SJE rows so far)\r")
          }
        }

        implicit val codec = scala.io.Codec.UTF8
          .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
          .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)

        for ((inFile, _, calMonth) <- batchFiles) {
          // Period key: for monthly granularity use "YYYY/MM" per source file;
          // for annual use yearStr; quarterly not yet implemented (falls back to yearStr).
          val filePeriodKey = if (granularity == "monthly")
            f"$yearInt%04d/$calMonth%02d"
          else
            yearStr
          val src = Source.fromFile(inFile)(codec)
          fileDataLines = 0L; fileBlank = 0L
          fileSource = sourceRows; fileUnmatched = unmatchedRef
          try {
            val lines = src.getLines()
            if (!lines.hasNext) { /* empty file */ }
            else {
              val headerLine = lines.next()  // consume BOM + header
              var rowNum = 1
              for (line <- lines) {
                val raw = line.trim
                fileDataLines += 1
                if (raw.isEmpty) fileBlank += 1
                if (raw.nonEmpty) {
                  rowNum += 1
                  val cols = splitAndStrip(raw, ",")
                  val resolved = if (recType == "R")
                    resolveApaRevRow(cols, ref, rowNum).map { case (agk, fnk, srck, amt, td) =>
                      val transDate = if (td.nonEmpty) td else acctDate
                      Array(agk, fnk, srck, amt, transDate)
                    }
                  else
                    resolveApaExpRow(cols, ref, rowNum).map { case (agk, fnk, obk, prk, vnd, amt, td) =>
                      val transDate = if (td.nonEmpty) td else acctDate
                      Array(agk, fnk, obk, prk, vnd, amt, transDate)
                    }

                  resolved match {
                    case None => unmatchedRef += 1
                    case Some(normalized) =>
                      sourceRows += 1
                      jrnlID     += 1
                      if (!parseOnly) {
                        val pairs = buildJEPairsAPA(normalized, recType, vendorMap,
                                                    accountingRule, filePeriodKey, acctDate, jrnlID)
                        for ((rowStr, _, _) <- pairs) {
                          chunk += rowStr
                          sjeRows += 1
                          if (chunk.size >= chunkSize) flushChunk()
                        }
                      }
                  }
                }
              }
            }
          } finally src.close()
          // Reconciliation: data_lines (file lines minus header) = source + unresolved_ref + blank.
          println(s"\n  File ${inFile.getName}: data_lines=$fileDataLines " +
                  s"source=${sourceRows - fileSource} unresolved_ref=${unmatchedRef - fileUnmatched} blank=$fileBlank")
        }

        flushChunk()

        println(s"\n  $batchLabel: source=$sourceRows sje=$sjeRows unresolved_ref=$unmatchedRef spills=${spills.size}")

        if (!dryRun && sjeRows > 0) {
          println(s"  Merging ${spills.size} spill(s) → $outFile ...")
          val rowsWritten = SortEngine.mergeSpills(spills.toSeq, outFile, sortSpec)
          println(f"  Written: $outFile ($rowsWritten%,d rows)")
        } else if (dryRun || parseOnly) {
          println(s"  --${if (parseOnly) "parseOnly" else "dryRun"}: no output written for $batchLabel")
        } else {
          println(s"  No rows produced for $batchLabel — output file not written")
        }

        spills.foreach(p => new File(p).delete())
        new File(tmpDir).delete()

        totalSourceRows += sourceRows
        totalSjeRows    += sjeRows
        totalUnresolved += unmatchedRef
      }

      if (recType == "R") {
        // Revenue: always a single annual output file regardless of granularity.
        val outFile  = outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + "rev.csv"
        val acctDate = quarterToDate(yearStr, 1)
        processApaFileBatch(files, "rev", outFile, acctDate, periodGranularity)
        val elapsedS = ((System.currentTimeMillis() - t0) / 1000).max(1)
        val inBytes  = files.foldLeft(0L) { case (acc, (f, _, _)) => acc + f.length() }
        val outBytes = new File(outFile).length()
        val peakRss  = Runtime.getRuntime.totalMemory()
        println(s"\nRESULT step=ttl year=$yearStr rc=0 records_read=$totalSourceRows records_written=$totalSjeRows elapsed_s=$elapsedS input_bytes=$inBytes output_bytes=$outBytes")
        println(s"RESOURCE step=ttl year=$yearStr peak_rss_bytes=$peakRss")
      } else if (periodGranularity == "monthly") {
        // Expenditure monthly: one output file per discovered source month.
        for ((inFile, quarter, calMonth) <- files) {
          val outFile  = outPath.stripSuffix("/") + f"/SortedJEFY${shortYr}_${yearInt}%04d_${calMonth}%02d_exp.csv"
          val acctDate = quarterToDate(yearStr, quarter)
          processApaFileBatch(Seq((inFile, quarter, calMonth)), f"${yearInt}%04d/${calMonth}%02d", outFile, acctDate, periodGranularity)
        }
      } else {
        // Expenditure annual/quarterly: group by quarter, one output per quarter.
        val byQuarter = files.groupBy { case (_, q, _) => q }
        for (quarter <- 1 to 4) {
          val filesForQ = byQuarter.getOrElse(quarter, Seq.empty)
          if (filesForQ.nonEmpty) {
            val outFile  = outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + s"q${quarter}exp.csv"
            val acctDate = quarterToDate(yearStr, quarter)
            processApaFileBatch(filesForQ, s"Q$quarter", outFile, acctDate, periodGranularity)
          }
        }
      }

      if (recType == "E") {
        val elapsedS  = ((System.currentTimeMillis() - t0) / 1000).max(1)
        val inBytes   = files.foldLeft(0L) { case (acc, (f, _, _)) => acc + f.length() }
        val outFileSample = outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + "q1exp.csv"
        val outBytes  = new File(outFileSample).length()
        val peakRss   = Runtime.getRuntime.totalMemory()
        println(s"\nControl totals:")
        println(s"  Source rows processed : $totalSourceRows")
        println(s"  SJE rows produced     : $totalSjeRows  (= ~2 × source rows)")
        println(s"  Unresolved ref lookups: $totalUnresolved")
        println(s"\nRESULT step=ttl year=$yearStr rc=0 records_read=$totalSourceRows records_written=$totalSjeRows elapsed_s=$elapsedS input_bytes=$inBytes output_bytes=$outBytes")
        println(s"RESOURCE step=ttl year=$yearStr peak_rss_bytes=$peakRss")
        println("STATUS: COMPLETE")
      }
    }

    println("*" * 100)
    println(s"Process End Time: ${java.util.Calendar.getInstance.getTime}")
    println("*" * 100)
  }

  // ── JE pair builder for APA-resolved rows (normalized cols) ──────────────────
  // For expenditure: normalized = Array(agencyKey, fundKey, objKey, progKey, vendorName, amount, transDate)
  // For revenue:     normalized = Array(agencyKey, fundKey, sourceKey, amount, transDate)
  private def buildJEPairsAPA(
      normalized:     Array[String],
      recType:        String,
      vendorMap:      Map[String, String],
      accountingRule: AccountingRule,
      year:           String,
      acctDate:       String,
      seqNo:          Int
  ): Seq[(String, Boolean, BigDecimal)] = {
    if (recType == "R") {
      // revenue: reuse buildRevenuePairs with format "RA" (agencyKey, fundKey, srcKey, amount)
      // normalized(4) = transDate — inject as col 4 so buildRevenuePairs sees it as RC-style
      val cols = Array(normalized(0), normalized(1), normalized(2), normalized(3), normalized(4))
      buildRevenuePairs(cols, "RC", vendorMap, accountingRule, year, acctDate, seqNo)
    } else {
      // expense: reuse buildExpensePairs with format "B" (agencyKey, fundKey, objKey, progKey, vendorName, amount, transDate)
      buildExpensePairs(normalized, "B", vendorMap, accountingRule, year, acctDate, seqNo)
    }
  }
}
