package org.universalledger.foundation.va.ledger

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.{DigestInputStream, MessageDigest}
import java.time.LocalDate
import java.util.Base64
import org.apache.commons.csv.{CSVFormat, CSVParser}
import scala.collection.JavaConverters._
import scala.collection.mutable

/** Reporting adapters only; no accounting or source transformation. */
object ReportingSupport {
  val ledgerHeader: Vector[String] = Vector("IPID", "ContractID", "CommitmentID", "LdgrlID",
    "SourceSystemID", "LedgerID", "JrnlType", "BookCodeID", "LegalEntityID", "CenterID",
    "ProjectID", "ProductID", "NominalAccountID", "AltAccountID", "CurrencyCodeSourceID",
    "CurrencyTypeCodeSourceID", "CurrencyCodeTargetID", "CurrencyTypeCodeTargetID", "LedgerPeriod",
    "TransAmount", "UnitOfMeasure", "StatisticAmount", "DirVsOffsetFlg", "ReconcileFlg", "AdjustFlg",
    "ExtensionIDAuditTrail", "ExtensionIDSource", "ExtensionIDClass", "ExtensionIDDates", "ExtensionIDCustom").map("ldgr" + _)
  val dimensionNames: Vector[String] = Vector("ledger", "journal_type", "book", "agency", "fund",
    "project", "product", "account", "alt_account", "source_currency", "source_currency_type",
    "target_currency", "target_currency_type", "period")
  def csv(values: Seq[Any]): String = values.map { v =>
    val s = v.toString
    if (s.exists(c => c == ',' || c == '"' || c == '\n' || c == '\r')) "\"" + s.replace("\"", "\"\"") + "\"" else s
  }.mkString(",")
  def writer(dir: File, name: String, header: Seq[String]): PrintWriter = {
    val p = new PrintWriter(new File(dir, name), "UTF-8"); p.println(csv(header)); p
  }
  def row(p: PrintWriter, values: Any*): Unit = p.println(csv(values))
  def key(values: Seq[String]): String = values.map(v => Base64.getUrlEncoder.withoutPadding.encodeToString(v.getBytes(StandardCharsets.UTF_8))).mkString(".")
  def unkey(s: String): Vector[String] = s.split("\\.", -1).toVector.map(v => new String(Base64.getUrlDecoder.decode(v), StandardCharsets.UTF_8))
  def fiscalYear(s: String): Int = {
    require(s.matches("[0-9]{4}(/[0-9]{2})?"), s"Invalid fiscal period: $s")
    val p = s.split("/"); val y = p(0).toInt
    if (p.length == 2) require(p(1).toInt >= 1 && p(1).toInt <= 12, s"Invalid month: $s")
    if (p(0).startsWith("00")) 2000 + y else y + (if (p.length == 2 && p(1).toInt >= 7) 1 else 0)
  }
  def date(s: String): LocalDate = LocalDate.parse(s.replace('/', '-'))
  def attributed(v: String): Boolean = v.nonEmpty && !v.forall(_ == '0') && !v.startsWith("UNMATCHED_")

  final class Input(val file: File, val kind: String, headerRequired: Boolean) extends AutoCloseable {
    val sha = MessageDigest.getInstance("SHA-256")
    private val stream = new DigestInputStream(new FileInputStream(file), sha)
    private val parser = new CSVParser(new InputStreamReader(stream, StandardCharsets.UTF_8), CSVFormat.DEFAULT)
    private val records = parser.iterator()
    val header: Vector[String] = if (headerRequired) {
      if (!records.hasNext) { parser.close(); throw new IllegalArgumentException(s"Empty header file: $file") }
      records.next().iterator().asScala.toVector.reverse.dropWhile(_.isEmpty).reverse.map(_.stripPrefix("\uFEFF"))
    } else Vector.empty
    var count = 0L
    var previous = ""
    def next(): Option[Vector[String]] = if (!records.hasNext) None else {
      count += 1
      Some(records.next().iterator().asScala.toVector)
    }
    def checkVendor(v: String): Unit = {
      require(v >= previous, s"Unsorted vendor stream $file record $count: $v follows $previous")
      previous = v
    }
    def hash: String = sha.digest().map(b => f"${b & 255}%02x").mkString
    def close(): Unit = parser.close()
  }

  case class Sum(var amount: BigDecimal = 0, var rows: Long = 0, var contributors: Long = 0) {
    def add(a: BigDecimal, n: Long, c: Long): Unit = { amount += a; rows += n; contributors += c }
  }
  /** GERS-003 prototype: bounded key buffer, sorted partials, then a separate reducer JVM. */
  final class Spills(dir: File, capacity: Int) {
    require(capacity > 0)
    val buffer = mutable.HashMap[String, Sum]()
    var chunks = 0; var peak = 0; var partials = 0L; var bytes = 0L
    def add(k: String, a: BigDecimal, n: Long, c: Long): Unit = {
      if (!buffer.contains(k) && buffer.size >= capacity) flush()
      buffer.getOrElseUpdate(k, Sum()).add(a, n, c); peak = peak.max(buffer.size)
    }
    def flush(): Unit = if (buffer.nonEmpty) {
      val f = new File(dir, s"spill_0_$chunks.csv")
      val p = new PrintWriter(f, "UTF-8")
      try buffer.toVector.sortBy(_._1).foreach { case (k, v) => p.println(s"$k,${v.amount},${v.rows},${v.contributors}") }
      finally { p.flush(); val error = p.checkError(); p.close(); require(!error, s"Spill write failed: $f") }
      bytes += f.length(); partials += buffer.size; chunks += 1; buffer.clear()
    }
  }
}

object ReportingReduce {
  import ReportingSupport._
  def apply(params: Map[String, String]): Unit = {
    val started = System.nanoTime()
    val dir = new File(params("outPath"))
    require(new File(dir, "EXTRACT_COMPLETE").isFile, "Extraction did not complete/reconcile")
    require(!new File(dir, "REPORT_COMPLETE").exists(), "Report already reduced")
    val fanIn = params.getOrElse("mergeFanIn", "16").toInt
    require(fanIn >= 2 && fanIn <= 128)
    var count = new String(Files.readAllBytes(new File(dir, "EXTRACT_COMPLETE").toPath), StandardCharsets.UTF_8).trim.toInt
    val sortSpec = Seq(SortEngine.SortField("report_key", 0, "CH", "A"))
    var level = 0; var intermediateBytes = 0L; var mergedRows = 0L
    def reduceFile(input: File, output: File, expectedRows: Long): Unit = {
      val src = scala.io.Source.fromFile(input, "UTF-8"); val p = new PrintWriter(output, "UTF-8")
      var current: String = null; var acc = Sum(); var rowsRead = 0L
      def flush(): Unit = if (current != null) p.println(s"$current,${acc.amount},${acc.rows},${acc.contributors}")
      try src.getLines().foreach { line =>
        rowsRead += 1
        val r = line.split(",", -1); require(r.length == 4)
        if (current != r(0)) { flush(); current = r(0); acc = Sum() }
        acc.add(BigDecimal(r(1)), r(2).toLong, r(3).toLong)
      } finally { flush(); src.close(); p.flush(); val error = p.checkError(); p.close(); require(!error, s"Reduction write failed: $output") }
      require(rowsRead == expectedRows, s"Merge output truncated: $input")
    }
    while (count > 1) {
      var next = 0; var start = 0
      while (start < count) {
        val paths = (start until math.min(start + fanIn, count)).map(i => new File(dir, s"spill_${level}_$i.csv").getPath)
        val merged = new File(dir, s"merge_${level}_$next.csv")
        val n = SortEngine.mergeSpills(paths, merged.getPath, sortSpec)
        mergedRows += n
        intermediateBytes += merged.length()
        val reduced = new File(dir, s"spill_${level + 1}_$next.csv")
        reduceFile(merged, reduced, n); intermediateBytes += reduced.length()
        start += fanIn; next += 1
      }
      count = next; level += 1
    }
    val out = writer(dir, "cross_vendor_totals.csv", Seq("side", "fiscal_year", "agency", "fund", "account", "currency", "amount", "rows", "contributors"))
    var finalRows = 0L
    try if (count == 1) {
      val src = scala.io.Source.fromFile(new File(dir, s"spill_${level}_0.csv"), "UTF-8")
      try src.getLines().foreach { line =>
        val r = line.split(",", -1); out.println(csv(unkey(r(0)) ++ r.drop(1))); finalRows += 1
      } finally src.close()
    } finally { out.flush(); val error = out.checkError(); out.close(); require(!error, "Final reduction output write failed") }
    val metrics = writer(dir, "reduce_metrics.csv", Seq("metric", "value"))
    Seq("merge_passes" -> level, "fan_in" -> fanIn, "merged_rows" -> mergedRows,
      "intermediate_bytes" -> intermediateBytes, "output_rows" -> finalRows,
      "elapsed_seconds" -> ((System.nanoTime() - started) / 1e9)).foreach { case (k, v) => row(metrics, k, v) }
    metrics.flush(); val metricsError = metrics.checkError(); metrics.close(); require(!metricsError, "Metrics write failed")
    Files.write(new File(dir, "REPORT_COMPLETE").toPath, "PASS\n".getBytes(StandardCharsets.UTF_8))
    println(s"RESULT step=5r rc=0 records_written=$finalRows merge_passes=$level")
  }
}
