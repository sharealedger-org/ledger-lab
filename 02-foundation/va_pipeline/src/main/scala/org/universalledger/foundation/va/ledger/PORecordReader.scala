package org.universalledger.foundation.va.ledger

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

import scala.collection.mutable.ArrayBuffer

import org.universalledger.foundation.va.corpus.CorpusProfile

//***************************************************************************************************************
//
//  Object:  PORecordReader — structural reader for the VA PO open-data files (VA_opendata_FY20YY.txt)
//
//  The PO files are tab-delimited with "-quoting, but they are not valid RFC-4180: free-text fields hold
//  embedded line breaks, embedded tabs and stray inch-mark quotes. Counting quotes to find record
//  boundaries (the earlier approach) either absorbs every following record or splits one record in two.
//
//  This reader finds boundaries from structure, not from quote parity:
//
//    1. The first non-blank line is the header; its field count (H) is the expected width of every record.
//    2. A physical line STARTS a record when its third tab field (ORDEREDDATE) is a date, or is empty on a
//       full-width row (non-empty first field and at least H-1 tabs). Every other line is a continuation of
//       the previous record. (The earlier rule accepted any empty-date line with 6+ tabs as a start, which
//       made the tail of a multi-line vendor address a record of its own and shifted its columns.)
//    3. A record is the start line plus its continuation lines, joined with a space, then split on TAB.
//       Quotes are NOT interpreted: measured on FY2003, a quote-aware split rejects ~880 well-formed records
//       whose description ends in an inch mark (`"Tape 2""`), where a plain tab split yields 19 fields for
//       all but ~50 of 52,463 records. Outer quotes are stripped per field.
//    4. A record is accepted only with exactly H fields. Everything else is returned as a rejection with a
//       reason code, the original text, line number and byte offset — nothing is dropped without a trace:
//         ORPHAN_FRAGMENT     lines before the first record start (or whose first line is not a start)
//         FIELD_COUNT         field count differs from the header (embedded tabs or a lost line break)
//
//  Reconciliation: every physical line after the header belongs to exactly one record, so at end of input
//  physicalLines == groupedLines, and the caller checks logicalRecords == accepted + rejected.
//
//  Memory: one record (start line + continuations) at a time.
//***************************************************************************************************************

object PORecordReader {

  sealed trait Item {
    def lines: Int
    def startLine: Long
    def startOffset: Long
    def ordinal: Long
    def texts: Seq[String]
    /** Original physical lines joined with \n; built only when a record is written to the quarantine file. */
    def raw: Array[Byte] = texts.mkString("\n").getBytes(StandardCharsets.UTF_8)
  }
  final case class Rec(fields: Array[String], lines: Int, startLine: Long, startOffset: Long,
                       ordinal: Long, texts: Seq[String]) extends Item
  final case class Rej(reason: String, lines: Int, startLine: Long, startOffset: Long,
                       ordinal: Long, texts: Seq[String]) extends Item

  private val DatePattern = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4}")

  private def unq(s: String): String = s.stripPrefix("\"").stripSuffix("\"")
}

final class PORecordReader(in: InputStream) extends Iterator[PORecordReader.Item] {
  import PORecordReader._

  private final class L(val line: CorpusProfile.Line, val text: String)

  private val lr = new CorpusProfile.LineReader(in)
  private var ahead: L = null
  private var counting = false

  /** Physical lines read after the header (including blank lines). */
  var physicalLines: Long = 0L
  /** Physical lines assigned to a record, a rejection, or skipped as leading blanks; equals physicalLines at end of input. */
  var groupedLines: Long = 0L
  /** Records returned so far, accepted or rejected. */
  var logicalRecords: Long = 0L

  private def readL(): L = {
    val l = lr.read()
    if (l == null) null
    else {
      if (counting) physicalLines += 1
      new L(l, new String(l.bytes, StandardCharsets.UTF_8))
    }
  }
  private def peek(): L = { if (ahead == null) ahead = readL(); ahead }
  private def take(): L = { val x = peek(); ahead = null; x }

  /** Field count of the header line (expected width of every record); 0 for an empty file. */
  val headerFieldCount: Int = {
    var h = readL()
    while (h != null && h.text.trim.isEmpty) h = readL()
    val n =
      if (h == null) 0
      else h.text.stripPrefix("\uFEFF").split("\t", -1).length
    counting = true
    n
  }

  private def isRecordStart(text: String): Boolean = {
    val f = text.split("\t", 4)
    if (f.length < 4) false
    else {
      val d = unq(f(2).trim)
      DatePattern.matcher(d).matches()
    }
  }

  override def hasNext: Boolean = {
    if (headerFieldCount == 0) return false
    // Blank lines between the header and the first record belong to no record.
    if (logicalRecords == 0) while (peek() != null && peek().text.trim.isEmpty) { take(); groupedLines += 1 }
    peek() != null
  }

  override def next(): Item = {
    val first = take()
    val lines = ArrayBuffer[L](first)
    while (peek() != null && !isRecordStart(peek().text)) lines += take()
    groupedLines += lines.size
    logicalRecords += 1

    val startLine   = first.line.lineNo
    val startOffset = first.line.startOffset
    val ordinal     = logicalRecords
    val texts       = lines.map(_.text).toVector

    if (!isRecordStart(first.text)) Rej("ORPHAN_FRAGMENT", lines.size, startLine, startOffset, ordinal, texts)
    else {
      // Trailing blank lines belong to the record but must not add trailing text to the last field.
      var last = lines.size
      while (last > 1 && lines(last - 1).text.trim.isEmpty) last -= 1
      // A bare CR inside a description would split the SJE row on read-back (Java line readers treat CR as a
      // terminator); the old getLines-based reader turned it into a join, so do the same here.
      val fields = texts.take(last).mkString(" ").replace('\r', ' ').split("\t", -1)
      if (fields.length != headerFieldCount)
        Rej("FIELD_COUNT", lines.size, startLine, startOffset, ordinal, texts)
      else
        Rec(fields.map(f => unq(f.trim)), lines.size, startLine, startOffset, ordinal, texts)
    }
  }

  def close(): Unit = in.close()
}
