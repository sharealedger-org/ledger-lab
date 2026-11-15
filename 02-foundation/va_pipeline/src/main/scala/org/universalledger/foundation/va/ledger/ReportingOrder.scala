package org.universalledger.foundation.va.ledger

import java.io._
import java.nio.charset.StandardCharsets
import java.security.{DigestInputStream, MessageDigest}
import org.apache.commons.csv.{CSVFormat, CSVParser}
import scala.collection.JavaConverters._
import scala.collection.mutable

/** Reporting-only byte-range index. Canonical posting keys and files remain unchanged.
  * Index state is bounded by maxBlocks; rows in a block are never buffered.
  */
final class ReportingOrder(file: File, kind: String, header: Vector[String], indexFile: File,
  maxBlocks: Int, maxRecordBytes: Int) extends AutoCloseable {
  require(maxBlocks > 0 && maxRecordBytes > 0)
  private val originalBytes = file.length()
  private val originalModified = file.lastModified()
  case class Block(id: String, start: Long, end: Long, firstRecord: Long)
  private val blocks = mutable.ArrayBuffer[Block]()
  private val digest = MessageDigest.getInstance("SHA-256")
  private val scanner = new BufferedInputStream(new DigestInputStream(new FileInputStream(file), digest), 65536)
  private var position = 0L
  private var lineStart = 0L
  private var id = ""
  private var blockStart = 0L
  private var blockOrdinal = 0L
  private var records = 0L
  private val idColumn = if (kind == "vendor") header.indexOf("instID") else if (kind == "contract") header.indexOf("vendor_id") else 0
  require(idColumn >= 0, s"Missing ID column in $file")
  private def csv(line: String): Vector[String] = {
    val p = CSVParser.parse(line, CSVFormat.DEFAULT)
    try { val rows = p.getRecords; require(rows.size() == 1, s"Physical-line reporting contract: $file"); rows.get(0).iterator.asScala.toVector }
    finally p.close()
  }
  private def add(end: Long): Unit = if (id.nonEmpty) {
    require(blocks.size < maxBlocks, s"Reporting block index allowance exceeded: $file ($maxBlocks)")
    blocks += Block(id, blockStart, end, blockOrdinal)
  }
  try {
    var firstLine = true; var done = false
    while (!done) {
      lineStart = position
      val bytes = new ByteArrayOutputStream()
      var b = scanner.read()
      while (b != -1 && b != 10) {
        require(bytes.size() < maxRecordBytes, s"Reporting record byte allowance exceeded: $file")
        bytes.write(b); position += 1; b = scanner.read()
      }
      if (b == 10) position += 1 else done = true
      val line = new String(bytes.toByteArray, StandardCharsets.UTF_8).stripSuffix("\r")
      if (firstLine && kind != "movement") { require(csv(line).map(_.stripPrefix("\uFEFF")).reverse.dropWhile(_.isEmpty).reverse == header); firstLine = false }
      else if (line.nonEmpty) {
        firstLine = false; records += 1
        val nextId = if (idColumn == 0 && !line.startsWith("\"")) line.takeWhile(_ != ',') else csv(line)(idColumn)
        require(nextId.nonEmpty, s"Empty reporting ID: $file record $records")
        if (nextId != id) { add(lineStart); id = nextId; blockStart = lineStart; blockOrdinal = records }
      }
    }
    add(position)
  } finally scanner.close()
  val hash: String = digest.digest().map(b => f"${b & 255}%02x").mkString
  val indexedBlocks: Int = blocks.size
  val indexedRows: Long = records
  private val ordered = blocks.sortBy(b => (b.id, b.start)).iterator
  private val index = new PrintWriter(indexFile, "UTF-8")
  try {
    index.println("vendor_id,start_byte,end_byte,first_record")
    blocks.foreach(b => index.println(ReportingSupport.csv(Seq(b.id, b.start, b.end, b.firstRecord))))
  } finally { index.flush(); val error = index.checkError(); index.close(); require(!error, "Reporting index write failed") }
  private val source = new RandomAccessFile(file, "r")
  private var parser: CSVParser = null
  private var iterator: java.util.Iterator[org.apache.commons.csv.CSVRecord] = null
  // Canonical SJE/ledger records are literal comma-delimited fields. Writers
  // sanitize commas but preserve quotes as data; only dimension CSVs use RFC quoting.
  private val literal = Set("movement", "balance", "opening")(kind)
  private var canonicalReader: BufferedReader = null
  private var canonicalRow: String = null
  private def readCanonical(): String = {
    var line = canonicalReader.readLine()
    while (line != null && line.isEmpty) line = canonicalReader.readLine()
    line
  }
  private var nextOrdinal = 0L
  var ordinal = 0L
  def next(): Option[Vector[String]] = {
    while (if (literal) canonicalRow == null else iterator == null || !iterator.hasNext) {
      if (parser != null) { parser.close(); parser = null }
      if (canonicalReader != null) { canonicalReader.close(); canonicalReader = null }
      if (!ordered.hasNext) {
        require(file.length() == originalBytes && file.lastModified() == originalModified, s"Indexed input changed: $file")
        return None
      }
      val block = ordered.next(); source.seek(block.start); nextOrdinal = block.firstRecord
      var remaining = block.end - block.start
      val bounded = new InputStream {
        override def read(): Int = if (remaining == 0) -1 else { val b = source.read(); require(b >= 0, "Truncated indexed input"); remaining -= 1; b }
        override def read(a: Array[Byte], offset: Int, length: Int): Int = if (remaining == 0) -1 else {
          val n = source.read(a, offset, math.min(remaining, length.toLong).toInt)
          require(n > 0, "Truncated indexed input"); remaining -= n; n
        }
      }
      if (literal) {
        canonicalReader = new BufferedReader(new InputStreamReader(bounded, StandardCharsets.UTF_8),65536)
        canonicalRow = readCanonical()
      } else {
        parser = new CSVParser(new InputStreamReader(bounded, StandardCharsets.UTF_8), CSVFormat.DEFAULT)
        iterator = parser.iterator()
      }
    }
    ordinal = nextOrdinal; nextOrdinal += 1
    if (literal) {
      val line = canonicalRow; canonicalRow = readCanonical()
      Some(line.split(",",-1).toVector)
    } else Some(iterator.next().iterator.asScala.toVector)
  }
  def close(): Unit = { if (parser != null) parser.close(); if (canonicalReader != null) canonicalReader.close(); source.close() }
}
