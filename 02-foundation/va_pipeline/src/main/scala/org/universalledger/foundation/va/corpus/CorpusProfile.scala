package org.universalledger.foundation.va.corpus

//***************************************************************************************************************
//
//  Program:  CorpusProfile - single-pass streaming inventory and profile of the external VA source corpus
//
//  Scope:    Data inspection only. This is NOT part of the measured financial path: it posts nothing, builds
//            no SJE and produces no ledger state. It exists so every contributor can identify the exact input
//            snapshot, see how every physical input record was disposed of, and reproduce source control totals.
//
//  CKB-style: each file is read exactly once, in sorted path order, as a byte stream. Checksum, line counting,
//            record assembly, disposition (accepted / quarantined with reason code), control totals and
//            per-column statistics are all accumulated in that one pass with bounded memory. The only
//            per-file state that grows with input size is the duplicate-candidate hash set (open addressing at
//            <=50% load: about 18-36 bytes/record, more briefly while the table doubles; unmeasured).
//
//  Outputs (all deterministic for identical inputs; no run timestamps are written):
//            source_catalog.csv    one row per file
//            file_profile.csv      one row per recognised data file (denominators, dispositions, controls)
//            file_field_profile.csv one row per file column
//            schema_catalog.csv    one row per distinct header
//            coverage_matrix.csv   family x fiscal year, including expected-but-absent rows
//            quarantine.csv        every quarantined record with reason code and (truncated) original text
//            corpus_summary.txt    human-readable roll-up
//
//  Usage:    java -cp <classes>:<scala-library.jar> org.universalledger.foundation.va.corpus.CorpusProfile \
//                 --root data --out data/corpus_profile [--dirs VARawFiles,VAPOData] [--only substring]
//
//  Change Log:
//  2026-10-31 - Created (Bob). Spec: SESSION_HANDOFF.md "Corpus Work Assignment to Bob", tasks 1-3.
//***************************************************************************************************************

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.security.{DigestInputStream, MessageDigest}
import java.math.{BigDecimal => JBigDecimal}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object CorpusProfile {

  val ProfilerVersion = "corpus-profile-1"

  /** A logical record that cannot close its quotes within this many physical lines is not merged. */
  val MaxLinesPerRecord = 20
  val QuarantineRawLimit = 2000

  // ── source classification ────────────────────────────────────────────────────────────────────────────────

  final case class Family(name: String, delimiter: Byte, amountBasis: String, dateCol: String,
                          keyCol: Option[String], placeholderKeys: Set[String], expectedYears: Range)

  val ApaExp = Family("apa_expenditure", ',', "AMOUNT", "VOUCHER_DATE", Some("VENDOR_NAME"), Set.empty, 2003 to 2016)
  val ApaRev = Family("apa_revenue", ',', "AMOUNT", "DEPOSIT_DATE", None, Set.empty, 2003 to 2016)
  val ApaBud = Family("apa_budget", ',', "AMOUNT", "BATCH_DATE", None, Set.empty, 2003 to 2016)
  val EvaPo  = Family("eva_po", '\t', "QUANTITYORDERED*PRICE", "ORDEREDDATE", Some("VENDORID"),
                      Set("999999999999"), 2001 to 2018)
  val Families = Seq(ApaExp, ApaRev, ApaBud, EvaPo)

  final case class SourceInfo(family: Option[Family], fiscalYear: Option[Int], period: String,
                              fyFromPeriod: Option[Int], synthetic: Boolean) {
    def familyName: String = family.map(_.name).getOrElse("unclassified")
  }

  private val ApaMonthly = """^(Expenditures|Revenues)\s+([A-Za-z]{3,4})\s+(\d{4})\.csv$""".r
  private val RowsSelected = """^\s*([\d,]+)\s+rows?\s+selected\.?\s*$""".r
  private val ApaBudget  = """^Budget FY(\d{4})\.csv$""".r
  private val PoFile     = """^VA_opendata_FY(\d{4})(_small)?\.txt$""".r
  private val FirstYear  = """(\d{4})""".r
  private val Months = Seq("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

  def classify(rel: String): SourceInfo = {
    val segs = rel.split('/')
    val name = segs.last
    val parent = if (segs.length >= 2) segs(segs.length - 2) else ""
    val synthetic = rel.toLowerCase.contains("fixture") || rel.toLowerCase.contains("synthetic")
    name match {
      case ApaMonthly(kind, mon, yr) =>
        val m = Months.indexOf(mon.toLowerCase.take(3)) + 1
        if (m == 0) SourceInfo(None, None, "", None, synthetic)
        else {
          val fam = if (kind == "Expenditures") ApaExp else ApaRev
          val y = yr.toInt
          val fyPeriod = if (m >= 7) y + 1 else y
          val folderFy = FirstYear.findFirstIn(parent).map(_.toInt)
          SourceInfo(Some(fam), folderFy, f"$y%04d-$m%02d", Some(fyPeriod), synthetic)
        }
      case ApaBudget(yr) => SourceInfo(Some(ApaBud), Some(yr.toInt), "", None, synthetic)
      case PoFile(yr, _) => SourceInfo(Some(EvaPo), Some(yr.toInt), "", None, synthetic)
      case _             => SourceInfo(None, None, "", None, synthetic)
    }
  }

  // ── byte-level line reader (tracks offsets; one pass; the digest stream sees every byte) ─────────────────────

  final class Line(val bytes: Array[Byte], val startOffset: Long, val lineNo: Long, val hadCR: Boolean)

  final class LineReader(in: InputStream) {
    private val buf = new Array[Byte](1 << 20)
    private var len = 0
    private var pos = 0
    private var offset = 0L
    private var lineNo = 0L
    private var eof = false
    private val acc = new ByteArrayOutputStream(256)

    /** Next physical line without terminator, or null at end of input. */
    def read(): Line = {
      acc.reset()
      val start = offset
      var sawNewline = false
      var done = false
      while (!done) {
        if (pos >= len) {
          if (eof) { done = true }
          else {
            len = in.read(buf, 0, buf.length)
            pos = 0
            if (len <= 0) { len = 0; eof = true; done = true }
          }
        }
        if (!done) {
          var i = pos
          while (i < len && buf(i) != '\n') i += 1
          acc.write(buf, pos, i - pos)
          if (i < len) { offset += (i - pos + 1); pos = i + 1; sawNewline = true; done = true }
          else { offset += (len - pos); pos = len }
        }
      }
      if (!sawNewline && acc.size() == 0) null
      else {
        var b = acc.toByteArray
        var cr = false
        if (b.length > 0 && b(b.length - 1) == '\r') { b = java.util.Arrays.copyOf(b, b.length - 1); cr = true }
        lineNo += 1
        new Line(b, start, lineNo, cr)
      }
    }
  }

  // ── quote-aware record parser ────────────────────────────────────────────────────────────────────────────
  // A quote opens a quoted field only at the start of a field. A quote inside a quoted field closes it only when
  // followed by the delimiter or end of record; "" is an escaped quote; any other interior quote is literal.
  // A quote in the middle of an unquoted field is literal.

  final class Parsed(val fields: ArrayBuffer[String], val closed: Boolean)

  def parseRecord(b: Array[Byte], delim: Byte): Parsed = {
    val fields = new ArrayBuffer[String]()
    val n = b.length
    val tmp = new ByteArrayOutputStream(64)
    var i = 0
    var more = true
    while (more) {
      if (i < n && b(i) == '"') {
        tmp.reset()
        var j = i + 1
        var fin = false
        while (!fin && j < n) {
          val c = b(j)
          if (c == '"') {
            if (j + 1 < n && b(j + 1) == '"') { tmp.write('"'); j += 2 }
            else if (j + 1 == n || b(j + 1) == delim) { fin = true }
            else { tmp.write('"'); j += 1 }
          } else { tmp.write(c); j += 1 }
        }
        if (!fin) return new Parsed(fields, false)
        fields += new String(tmp.toByteArray, StandardCharsets.UTF_8)
        if (j + 1 < n) { i = j + 2; if (i == n) { fields += ""; more = false } }
        else more = false
      } else {
        var j = i
        while (j < n && b(j) != delim) j += 1
        fields += new String(b, i, j - i, StandardCharsets.UTF_8)
        if (j < n) { i = j + 1; if (i == n) { fields += ""; more = false } }
        else more = false
      }
    }
    new Parsed(fields, true)
  }

  // ── small helpers ────────────────────────────────────────────────────────────────────────────────────────

  /** Open-addressing set of longs for duplicate-candidate detection (full-record 64-bit FNV-1a). */
  final class LongSet {
    private var table = new Array[Long](1 << 16)
    private var used = new Array[Boolean](1 << 16)
    private var size = 0
    /** Returns true if the value was already present. */
    def addIfAbsent(v: Long): Boolean = {
      if (size * 2 >= table.length) grow()
      var idx = ((v ^ (v >>> 32)).toInt) & (table.length - 1)
      while (used(idx)) {
        if (table(idx) == v) return true
        idx = (idx + 1) & (table.length - 1)
      }
      used(idx) = true; table(idx) = v; size += 1
      false
    }
    private def grow(): Unit = {
      val oldT = table; val oldU = used
      table = new Array[Long](oldT.length * 2); used = new Array[Boolean](oldT.length * 2); size = 0
      var k = 0
      while (k < oldT.length) { if (oldU(k)) addIfAbsent(oldT(k)); k += 1 }
    }
  }

  def fnv1a64(b: Array[Byte]): Long = {
    var h = 0xcbf29ce484222325L
    var i = 0
    while (i < b.length) { h ^= (b(i) & 0xffL); h *= 0x100000001b3L; i += 1 }
    h
  }

  def hex(b: Array[Byte]): String = { val sb = new StringBuilder; b.foreach(x => sb.append(f"${x & 0xff}%02x")); sb.toString }

  def csvEscape(s: String): String =
    if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0)
      "\"" + s.replace("\"", "\"\"") + "\"" else s

  def csvLine(cells: Seq[Any]): String = cells.map(c => csvEscape(String.valueOf(c))).mkString(",") + "\n"

  private val daysIn = Array(0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31) // Feb=29 checked below

  /** MM-dd-yy, MM-dd-yyyy, MM/dd/yy or MM/dd/yyyy to yyyymmdd; -1 if not parseable. Two-digit years: <=50 -> 20yy. */
  def parseDate(s0: String): Int = {
    val s = s0.trim
    val parts = if (s.indexOf('/') >= 0) s.split('/') else s.split('-')
    if (parts.length != 3) return -1
    try {
      val m = parts(0).trim.toInt; val d = parts(1).trim.toInt; var y = parts(2).trim.toInt
      if (parts(2).trim.length == 2) y = if (y <= 50) 2000 + y else 1900 + y
      else if (parts(2).trim.length != 4) return -1
      if (m < 1 || m > 12 || d < 1) return -1
      val leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
      val dim = if (m == 2 && !leap) 28 else daysIn(m)
      if (d > dim) -1 else y * 10000 + m * 100 + d
    } catch { case _: NumberFormatException => -1 }
  }

  def fmtDate(v: Int): String = if (v < 0) "" else f"${v / 10000}%04d-${(v / 100) % 100}%02d-${v % 100}%02d"

  /** Cheap pre-check so non-numeric text does not pay for an exception. */
  def looksNumeric(s: String): Boolean = {
    var i = 0; var digit = false
    if (s.isEmpty) return false
    if (s.charAt(0) == '-' || s.charAt(0) == '+') i = 1
    while (i < s.length) {
      val c = s.charAt(i)
      if (c >= '0' && c <= '9') digit = true
      else if (c != '.') return false
      i += 1
    }
    digit
  }

  def parseDec(s: String): Option[JBigDecimal] =
    if (!looksNumeric(s)) None
    else try Some(new JBigDecimal(s)) catch { case _: NumberFormatException => None }

  private def isDigits(s: String): Boolean = s.nonEmpty && s.forall(c => c >= '0' && c <= '9')
  private def isA2D7(s: String): Boolean =
    s.length == 9 && s.substring(0, 2).forall(_.isLetter) && isDigits(s.substring(2))

  // ── results ──────────────────────────────────────────────────────────────────────────────────────────────

  final class ColStats {
    var nonblank = 0L; var blank = 0L; var minLen = Int.MaxValue; var maxLen = 0
    var numeric = 0L; var leadingZero = 0L; var padded = 0L
  }

  final class FileResult(val rel: String, val info: SourceInfo) {
    var bytes = 0L; var sha = ""   // bytes is the file system size, set before the pass
    var bom = false; var ncols = 0; var schemaId = ""; var header: Seq[String] = Nil
    var physLines = 0L; var blankLines = 0L; var crlfLines = 0L
    var headerRecs = 0L; var accepted = 0L; var quarantined = 0L; var multiline = 0L; var linesInRecords = 0L
    val qReasons = mutable.TreeMap[String, Long]()
    var keyBlank = 0L; var keyPlaceholder = 0L; var keyD9 = 0L; var keyA2D7 = 0L; var keyOther = 0L
    var amountBlank = 0L; var amountNeg = 0L; var amountZero = 0L; var amountPos = 0L
    var amountSum = JBigDecimal.ZERO; var amountAbs = JBigDecimal.ZERO
    var amountMin: JBigDecimal = null; var amountMax: JBigDecimal = null
    var dateBlank = 0L; var dateBad = 0L; var dateMin = Int.MaxValue; var dateMax = -1; var fyViolations = 0L
    var dup = 0L
    var declaredRows = -1L            // "N rows selected." trailer written by the source extract tool, if present
    var cols: Array[ColStats] = Array.empty
    var status = "CATALOGED_ONLY"
    def parsed: Long = accepted + quarantined
    def artifacts: Long = qReasons.getOrElse("EXTRACT_ARTIFACT", 0L)
    /** Records the extract could have declared: everything except recognised tool trailer lines. */
    def dataRecords: Long = parsed - artifacts
    def reconciles: Boolean = physLines == blankLines + linesInRecords
  }

  final class Quarantine(w: Writer) {
    def add(rel: String, ordinal: Long, startLine: Long, startOffset: Long, lineCount: Int,
            reason: String, raw: Array[Byte]): Unit = {
      var text = new String(raw, StandardCharsets.UTF_8)
      val truncated = text.length > QuarantineRawLimit
      if (truncated) text = text.substring(0, QuarantineRawLimit)
      text = text.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
      w.write(csvLine(Seq(rel, ordinal, startLine, startOffset, lineCount, reason, truncated, text)))
    }
  }

  val QuarantineHeader = Seq("rel_path", "record_ordinal", "start_line", "start_byte_offset", "line_count",
    "reason", "raw_truncated", "raw_escaped")

  // ── per-file single pass ─────────────────────────────────────────────────────────────────────────────────

  def profileFile(path: Path, rel: String, info: SourceInfo, quarantine: Quarantine): FileResult = {
    val res = new FileResult(rel, info)
    res.bytes = Files.size(path)
    val md = MessageDigest.getInstance("SHA-256")
    val in = new DigestInputStream(new FileInputStream(path.toFile), md)
    try {
      info.family match {
        case None =>
          val buf = new Array[Byte](1 << 20)
          while (in.read(buf) >= 0) {}   // digest only; the whole file is read once
        case Some(fam) => scanRecords(in, fam, info, res, quarantine)
      }
    } finally in.close()
    res.sha = hex(md.digest())
    res.status =
      if (res.bytes == 0) "EMPTY_FILE"
      else if (info.family.isEmpty) "CATALOGED_ONLY"
      else if (res.headerRecs == 0) "NO_HEADER"
      else if (res.parsed == 0) "HEADER_ONLY"
      else if (res.quarantined > 0) "PROFILED_WITH_QUARANTINE"
      else "PROFILED_CLEAN"
    res
  }

  private def scanRecords(in: InputStream, fam: Family, info: SourceInfo, res: FileResult, q: Quarantine): Unit = {
    val reader = new LineReader(in)
    val pushback = new java.util.ArrayDeque[Line]()
    def nextLine(): Line = if (!pushback.isEmpty) pushback.pollFirst() else {
      val l = reader.read()
      if (l != null) {
        res.physLines += 1
        if (l.hadCR) res.crlfLines += 1
      }
      l
    }
    val seen = new LongSet
    var keyIdx = -1; var amtIdx = -1; var qtyIdx = -1; var priceIdx = -1; var dateIdx = -1
    val fyLo = info.fiscalYear.map(fy => (fy - 1) * 10000 + 701).getOrElse(-1)
    val fyHi = info.fiscalYear.map(fy => fy * 10000 + 630).getOrElse(-1)
    var ordinal = 0L
    var first = true

    def reject(lines: ArrayBuffer[Line], reason: String): Unit = {
      res.quarantined += 1; res.linesInRecords += lines.size
      res.qReasons(reason) = res.qReasons.getOrElse(reason, 0L) + 1
      val raw = joined(lines)
      q.add(res.rel, ordinal, lines.head.lineNo, lines.head.startOffset, lines.size, reason, raw)
    }
    def joined(lines: ArrayBuffer[Line]): Array[Byte] = {
      if (lines.size == 1) lines.head.bytes
      else {
        val o = new ByteArrayOutputStream()
        var k = 0
        while (k < lines.size) { if (k > 0) o.write('\n'); o.write(lines(k).bytes); k += 1 }
        o.toByteArray
      }
    }

    var line = nextLine()
    while (line != null) {
      if (first) {
        // BOM is inside the first line's bytes; strip it before parsing the header.
        val b = line.bytes
        if (b.length >= 3 && (b(0) & 0xff) == 0xEF && (b(1) & 0xff) == 0xBB && (b(2) & 0xff) == 0xBF) {
          res.bom = true
          line = new Line(java.util.Arrays.copyOfRange(b, 3, b.length), line.startOffset, line.lineNo, line.hadCR)
        }
      }
      if (line.bytes.length == 0) {
        res.blankLines += 1
        line = nextLine()
      } else {
        val lines = new ArrayBuffer[Line](); lines += line
        var parsed = parseRecord(joined(lines), fam.delimiter)
        var exhausted = false
        while (!parsed.closed && lines.size < MaxLinesPerRecord && !exhausted) {
          val nl = nextLine()
          if (nl == null) exhausted = true
          else { lines += nl; parsed = parseRecord(joined(lines), fam.delimiter) }
        }
        val needResync =
          !parsed.closed || (lines.size > 1 && res.ncols > 0 && !first && parsed.fields.size != res.ncols)
        if (needResync && !first) {
          // Do not let one stray quote swallow good records: quarantine the first line only, re-read the rest.
          val reason = if (!parsed.closed) "UNTERMINATED_QUOTE" else "UNBALANCED_QUOTE_RESYNC"
          var k = lines.size - 1
          while (k >= 1) { pushback.addFirst(lines(k)); k -= 1 }
          val one = new ArrayBuffer[Line](); one += lines.head
          ordinal += 1
          reject(one, reason)
        } else if (first) {
          // Header record (always the first non-blank record); if its quotes do not close, use the first line.
          first = false
          var headerLines = lines.size
          if (!parsed.closed) {
            var k = lines.size - 1
            while (k >= 1) { pushback.addFirst(lines(k)); k -= 1 }
            parsed = parseRecord(lines.head.bytes, fam.delimiter)
            headerLines = 1
          }
          res.headerRecs += 1
          res.linesInRecords += headerLines
          res.header = parsed.fields.map(_.trim)
          res.ncols = res.header.size
          res.cols = Array.fill(res.ncols)(new ColStats)
          res.schemaId = fam.name + "-" + hex(MessageDigest.getInstance("SHA-256")
            .digest(res.header.mkString("\u001f").getBytes(StandardCharsets.UTF_8))).substring(0, 8)
          def idx(n: String) = res.header.indexOf(n)
          keyIdx = fam.keyCol.map(idx).getOrElse(-1)
          dateIdx = idx(fam.dateCol)
          if (fam.name == "eva_po") { qtyIdx = idx("QUANTITYORDERED"); priceIdx = idx("PRICE") } else amtIdx = idx("AMOUNT")
        } else {
          ordinal += 1
          val f = parsed.fields
          val raw = joined(lines)
          val artifactLine =
            if (lines.size == 1 && f.size != res.ncols && fam.name.startsWith("apa_")) {
              val t = new String(lines.head.bytes, StandardCharsets.UTF_8).trim
              t match {
                case RowsSelected(n) => res.declaredRows = n.replace(",", "").toLong; true
                case _ => t.startsWith("SQL>")
              }
            } else false
          if (artifactLine) reject(lines, "EXTRACT_ARTIFACT")
          else if (f.size != res.ncols) reject(lines, "FIELD_COUNT")
          else {
            // amount
            var ext: Option[JBigDecimal] = None
            var badAmount = false
            var blankAmount = false
            if (fam.name == "eva_po") {
              if (qtyIdx >= 0 && priceIdx >= 0) {
                val qs = f(qtyIdx).trim; val ps = f(priceIdx).trim
                if (qs.isEmpty || ps.isEmpty) blankAmount = true
                else (parseDec(qs), parseDec(ps)) match {
                  case (Some(qv), Some(pv)) => ext = Some(qv.multiply(pv))
                  case _ => badAmount = true
                }
              }
            } else if (amtIdx >= 0) {
              val s = f(amtIdx).trim
              if (s.isEmpty) blankAmount = true else parseDec(s) match { case Some(v) => ext = Some(v); case None => badAmount = true }
            }
            if (badAmount) reject(lines, "BAD_AMOUNT")
            else {
              res.accepted += 1; res.linesInRecords += lines.size
              if (lines.size > 1) res.multiline += 1
              if (seen.addIfAbsent(fnv1a64(raw))) res.dup += 1
              if (blankAmount) res.amountBlank += 1
              ext.foreach { v =>
                res.amountSum = res.amountSum.add(v); res.amountAbs = res.amountAbs.add(v.abs())
                val sg = v.signum()
                if (sg < 0) res.amountNeg += 1 else if (sg == 0) res.amountZero += 1 else res.amountPos += 1
                if (res.amountMin == null || v.compareTo(res.amountMin) < 0) res.amountMin = v
                if (res.amountMax == null || v.compareTo(res.amountMax) > 0) res.amountMax = v
              }
              // date
              if (dateIdx >= 0) {
                val ds = f(dateIdx).trim
                if (ds.isEmpty) res.dateBlank += 1
                else {
                  val d = parseDate(ds)
                  if (d < 0) res.dateBad += 1
                  else {
                    if (d < res.dateMin) res.dateMin = d
                    if (d > res.dateMax) res.dateMax = d
                    if (fyLo > 0 && (d < fyLo || d > fyHi)) res.fyViolations += 1
                  }
                }
              }
              // key
              if (keyIdx >= 0) {
                val k = f(keyIdx).trim
                if (k.isEmpty) res.keyBlank += 1
                else if (fam.placeholderKeys.contains(k)) res.keyPlaceholder += 1
                else if (fam.name == "eva_po") {
                  if (k.length == 9 && isDigits(k)) res.keyD9 += 1 else if (isA2D7(k)) res.keyA2D7 += 1 else res.keyOther += 1
                } else res.keyOther += 1
              }
              // per-column
              var c = 0
              while (c < res.ncols) {
                val s = f(c); val st = res.cols(c); val t = s.trim
                if (t.isEmpty) st.blank += 1 else st.nonblank += 1
                if (s.length < st.minLen) st.minLen = s.length
                if (s.length > st.maxLen) st.maxLen = s.length
                if (looksNumeric(t)) st.numeric += 1
                if (t.length > 1 && t.charAt(0) == '0' && isDigits(t)) st.leadingZero += 1
                if (s.nonEmpty && (s.charAt(0) == ' ' || s.charAt(s.length - 1) == ' ')) st.padded += 1
                c += 1
              }
            }
          }
        }
        line = nextLine()
      }
    }
  }

  // ── driver ───────────────────────────────────────────────────────────────────────────────────────────────

  final case class Config(root: Path, out: Path, dirs: Seq[String], only: String)

  def parseArgs(args: Array[String]): Map[String, String] =
    args.sliding(2, 2).collect { case Array(k, v) if k.startsWith("--") => k.stripPrefix("--") -> v }.toMap

  def listFiles(cfg: Config): Seq[(Path, String)] = {
    val all = ArrayBuffer[(Path, String)]()
    cfg.dirs.foreach { d =>
      val base = cfg.root.resolve(d)
      if (Files.isDirectory(base)) {
        val st = Files.walk(base)
        try st.iterator().asScala.filter(p => Files.isRegularFile(p)).foreach { p =>
          val rel = cfg.root.relativize(p).toString.replace('\\', '/')
          if (cfg.only.isEmpty || rel.contains(cfg.only)) all += ((p, rel))
        } finally st.close()
      }
    }
    all.sortBy(_._2)
  }

  def run(cfg: Config): Seq[FileResult] = {
    Files.createDirectories(cfg.out)
    val qWriter = new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(cfg.out.resolve("quarantine.csv").toFile), StandardCharsets.UTF_8))
    val results = ArrayBuffer[FileResult]()
    try {
      qWriter.write(csvLine(QuarantineHeader))
      val quarantine = new Quarantine(qWriter)
      val files = listFiles(cfg)
      files.zipWithIndex.foreach { case ((p, rel), i) =>
        val t0 = System.nanoTime()
        val r = profileFile(p, rel, classify(rel), quarantine)
        results += r
        println(f"[${i + 1}%d/${files.size}%d] $rel%s  ${r.status}%s  parsed=${r.parsed}%d quarantined=${r.quarantined}%d  ${(System.nanoTime() - t0) / 1e9}%.1fs")
      }
    } finally qWriter.close()
    writeOutputs(cfg, results)
    results
  }

  private def writer(p: Path): BufferedWriter =
    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(p.toFile), StandardCharsets.UTF_8))

  private def sourceNote(info: SourceInfo): String = info.family.map(_.name).getOrElse("") match {
    case "apa_expenditure" | "apa_revenue" | "apa_budget" =>
      "per repo docs: Virginia APA datapoint.apa.virginia.gov historicaldata zip (pattern in scripts/download_va_data.py); not verified per file"
    case "eva_po" =>
      "per repo docs: Virginia eVA open data; FY2003-FY2015 not on portal, archived at doi:10.5281/zenodo.23846362; not verified per file"
    case _ => ""
  }

  def writeOutputs(cfg: Config, results: Seq[FileResult]): Unit = {
    val rs = results.sortBy(_.rel)

    // source_catalog.csv
    val cat = writer(cfg.out.resolve("source_catalog.csv"))
    try {
      cat.write(csvLine(Seq("rel_path", "source_family", "source_url", "acquisition_date", "source_note",
        "bytes", "sha256", "fiscal_year_claimed", "period_claimed", "fy_from_period",
        "fy_consistent", "schema_id", "delimiter", "bom", "synthetic", "inspection_status", "completeness",
        "profiler_version")))
      rs.foreach { r =>
        val i = r.info
        val consistent = (i.fiscalYear, i.fyFromPeriod) match {
          case (Some(a), Some(b)) => (a == b).toString
          case _ => ""
        }
        val completeness = if (r.bytes == 0) "empty" else "unknown"
        cat.write(csvLine(Seq(r.rel, i.familyName, "", "", sourceNote(i), r.bytes, r.sha,
          i.fiscalYear.map(_.toString).getOrElse(""), i.period, i.fyFromPeriod.map(_.toString).getOrElse(""),
          consistent, r.schemaId, i.family.map(f => if (f.delimiter == '\t') "TAB" else "COMMA").getOrElse(""),
          if (i.family.isDefined && r.headerRecs > 0) r.bom.toString else "", i.synthetic, r.status, completeness,
          ProfilerVersion)))
      }
    } finally cat.close()

    val profiled = rs.filter(_.info.family.isDefined)

    // file_profile.csv
    val fp = writer(cfg.out.resolve("file_profile.csv"))
    try {
      fp.write(csvLine(Seq("rel_path", "source_family", "fiscal_year_claimed", "bytes", "physical_lines",
        "blank_lines", "crlf_lines", "header_records", "data_records_parsed", "accepted", "quarantined",
        "quarantine_reasons", "multiline_accepted", "lines_in_records", "line_reconciles", "key_col", "key_blank",
        "key_placeholder", "key_digits9", "key_alpha2_digits7", "key_other", "amount_basis", "amount_blank",
        "amount_signed_sum", "amount_abs_sum", "amount_negative", "amount_zero", "amount_positive", "amount_min",
        "amount_max", "date_col", "date_blank", "date_unparseable", "date_min", "date_max", "out_of_fy_window",
        "duplicate_candidates", "ncols", "schema_id", "extract_artifact_lines", "source_declared_rows",
        "declared_matches_data_records")))
      profiled.foreach { r =>
        val fam = r.info.family.get
        val isPo = fam.name == "eva_po"
        def shape(v: Long): String = if (isPo) v.toString else ""
        fp.write(csvLine(Seq(r.rel, fam.name, r.info.fiscalYear.map(_.toString).getOrElse(""), r.bytes,
          r.physLines, r.blankLines, r.crlfLines, r.headerRecs, r.parsed, r.accepted, r.quarantined,
          r.qReasons.map { case (k, v) => s"$k=$v" }.mkString(";"), r.multiline, r.linesInRecords, r.reconciles,
          fam.keyCol.getOrElse(""), if (fam.keyCol.isDefined) r.keyBlank.toString else "",
          shape(r.keyPlaceholder), shape(r.keyD9), shape(r.keyA2D7),
          if (fam.keyCol.isDefined) r.keyOther.toString else "", fam.amountBasis, r.amountBlank,
          r.amountSum.toPlainString, r.amountAbs.toPlainString, r.amountNeg, r.amountZero, r.amountPos,
          Option(r.amountMin).map(_.toPlainString).getOrElse(""), Option(r.amountMax).map(_.toPlainString).getOrElse(""),
          fam.dateCol, r.dateBlank, r.dateBad, if (r.dateMax >= 0) fmtDate(r.dateMin) else "", fmtDate(r.dateMax),
          r.fyViolations, r.dup, r.ncols, r.schemaId, r.artifacts,
          if (r.declaredRows >= 0) r.declaredRows.toString else "",
          if (r.declaredRows >= 0) (r.declaredRows == r.dataRecords).toString else "")))
      }
    } finally fp.close()

    // file_field_profile.csv
    val ff = writer(cfg.out.resolve("file_field_profile.csv"))
    try {
      ff.write(csvLine(Seq("rel_path", "col_index", "col_name", "nonblank", "blank", "min_len", "max_len",
        "numeric_looking", "leading_zero_digits", "space_padded")))
      profiled.filter(_.headerRecs > 0).foreach { r =>
        var c = 0
        while (c < r.ncols) {
          val s = r.cols(c)
          ff.write(csvLine(Seq(r.rel, c + 1, r.header(c), s.nonblank, s.blank,
            if (s.minLen == Int.MaxValue) 0 else s.minLen, s.maxLen, s.numeric, s.leadingZero, s.padded)))
          c += 1
        }
      }
    } finally ff.close()

    // schema_catalog.csv
    val sc = writer(cfg.out.resolve("schema_catalog.csv"))
    try {
      sc.write(csvLine(Seq("schema_id", "source_family", "delimiter", "ncols", "blank_header_names", "files",
        "first_file", "header")))
      profiled.filter(_.schemaId.nonEmpty).groupBy(_.schemaId).toSeq.sortBy(_._1).foreach { case (id, g) =>
        val h = g.head
        sc.write(csvLine(Seq(id, h.info.familyName, if (h.info.family.get.delimiter == '\t') "TAB" else "COMMA",
          h.ncols, h.header.count(_.isEmpty), g.size, g.map(_.rel).min, h.header.mkString("|"))))
      }
    } finally sc.close()

    // coverage_matrix.csv
    val cm = writer(cfg.out.resolve("coverage_matrix.csv"))
    try {
      cm.write(csvLine(Seq("source_family", "fiscal_year", "status", "files", "expected_files", "periods_present",
        "periods_missing", "bytes", "data_records_parsed", "accepted", "quarantined", "empty_or_header_only_files",
        "date_min", "date_max", "out_of_fy_window", "files_with_fy_mismatch")))
      Families.foreach { fam =>
        val inFam = profiled.filter(_.info.family.get.name == fam.name)
        val years = (fam.expectedYears ++ inFam.flatMap(_.info.fiscalYear)).distinct.sorted
        years.foreach { fy =>
          val g = inFam.filter(_.info.fiscalYear.contains(fy))
          val monthly = fam == ApaExp || fam == ApaRev
          val expectedPeriods =
            if (monthly) ((7 to 12).map(m => f"${fy - 1}%04d-$m%02d") ++ (1 to 6).map(m => f"$fy%04d-$m%02d")).sorted else Seq()
          val present = g.map(_.info.period).filter(_.nonEmpty).sorted
          val missing = expectedPeriods.filterNot(present.contains)
          val emptyish = g.count(r => r.status == "EMPTY_FILE" || r.status == "HEADER_ONLY" || r.status == "NO_HEADER")
          val status =
            if (g.isEmpty) "MISSING"
            else if (emptyish == g.size) "EMPTY_OR_HEADER_ONLY"
            else if (monthly && missing.nonEmpty) "PARTIAL_PERIODS"
            else "PRESENT_COMPLETENESS_UNKNOWN"
          val dmin = g.filter(_.dateMax >= 0).map(_.dateMin)
          val dmax = g.map(_.dateMax)
          cm.write(csvLine(Seq(fam.name, fy, status, g.size, if (monthly) 12 else 1, present.mkString(";"),
            missing.mkString(";"), g.map(_.bytes).sum, g.map(_.parsed).sum, g.map(_.accepted).sum,
            g.map(_.quarantined).sum, emptyish, if (dmin.nonEmpty) fmtDate(dmin.min) else "",
            if (dmax.nonEmpty) fmtDate(dmax.max) else "", g.map(_.fyViolations).sum,
            g.count(r => r.info.fyFromPeriod.isDefined && r.info.fiscalYear != r.info.fyFromPeriod))))
        }
      }
    } finally cm.close()

    // corpus_summary.txt
    val sm = writer(cfg.out.resolve("corpus_summary.txt"))
    try {
      sm.write(s"profiler_version=$ProfilerVersion\n")
      sm.write(s"files_cataloged=${rs.size}\nfiles_profiled=${profiled.size}\n")
      sm.write(s"bytes_total=${rs.map(_.bytes).sum}\n")
      sm.write(s"data_records_parsed=${profiled.map(_.parsed).sum}\naccepted=${profiled.map(_.accepted).sum}\n")
      sm.write(s"quarantined=${profiled.map(_.quarantined).sum}\n")
      sm.write(s"files_failing_line_reconciliation=${profiled.count(!_.reconciles)}\n")
      rs.groupBy(_.status).toSeq.sortBy(_._1).foreach { case (s, g) => sm.write(s"status.$s=${g.size}\n") }
    } finally sm.close()
  }

  def main(args: Array[String]): Unit = {
    val p = parseArgs(args)
    val cfg = Config(
      Paths.get(p.getOrElse("root", "data")).toAbsolutePath.normalize(),
      Paths.get(p.getOrElse("out", "data/corpus_profile")).toAbsolutePath.normalize(),
      p.getOrElse("dirs", "VARawFiles,VAPOData").split(',').map(_.trim).filter(_.nonEmpty).toSeq,
      p.getOrElse("only", ""))
    println(s"[CorpusProfile] root=${cfg.root} out=${cfg.out} dirs=${cfg.dirs.mkString(",")} only='${cfg.only}'")
    val rs = run(cfg)
    println(s"[CorpusProfile] files=${rs.size} parsed=${rs.map(_.parsed).sum} quarantined=${rs.map(_.quarantined).sum}" +
      s" unreconciled_files=${rs.count(r => r.info.family.isDefined && !r.reconciles)}")
  }
}
