// Post-engine audit only. Compile outside the frozen measured engine build.
import java.io._
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.Base64
import java.util.zip.GZIPInputStream
import scala.collection.mutable
import org.universalledger.foundation.va.ledger.SortEngine

object ReportingOutputEquivalence {
  val names = Vector("movement_history", "balance_history", "reconciliation", "relationships", "vendor_activity",
    "aim_movements", "aim_balances", "aim_combined", "cross_vendor_totals", "vendor_temporal_cuts",
    "vendor_temporal_summary", "vendor_attribute_totals", "vendor_attribute_summary", "synthetic_conservation",
    "synthetic_vendor_coverage", "synthetic_validation", "analysis_packets", "coverage",
    "dimension_attributes", "reference_attributes", "synthetic_view_config")
  private val spec = Seq(SortEngine.SortField("complete_record_base64", 0, "CH", "A"))
  private val chunkRows = 4096
  private def reader(f: File): BufferedReader = {
    val in = new FileInputStream(f)
    new BufferedReader(new InputStreamReader(if (f.getName.endsWith(".gz")) new GZIPInputStream(in, 65536) else in, UTF_8), 65536)
  }
  private def source(dir: File, name: String): File = {
    val plain = new File(dir, name + ".csv"); val packed = new File(dir, name + ".csv.gz")
    require(plain.isFile ^ packed.isFile, s"Require exactly one plain/archive source: $dir/$name")
    if (plain.isFile) plain else packed
  }
  private def firstField(s: String): String = {
    if (!s.startsWith("\"")) s.takeWhile(_ != ',')
    else {
      val b = new StringBuilder; var i = 1; var done = false
      while (i < s.length && !done) {
        if (s(i) == '"') {
          if (i + 1 < s.length && s(i + 1) == '"') { b += '"'; i += 2 }
          else { done = true; i += 1; require(i == s.length || s(i) == ',') }
        } else { b += s(i); i += 1 }
      }
      require(done, "Invalid first CSV field"); b.toString
    }
  }
  private final class Rows(f: File) extends AutoCloseable {
    val in = reader(f); val header = in.readLine(); require(header != null, s"Missing header: $f")
    val vendor = header.startsWith("vendor_id,")
    var next: String = in.readLine(); private var previous = ""
    def group: String = if (next == null) "" else if (vendor) firstField(next) else "ALL"
    def takeGroup(key: String): Iterator[String] = new Iterator[String] {
      def hasNext: Boolean = Rows.this.next != null && group == key
      def next(): String = {
        require(hasNext)
        val line = Rows.this.next
        require(line.length <= 4096 && line.nonEmpty, "Audit line bound or empty record")
        if (vendor) { require(key >= previous, s"Vendor order violation: $key after $previous"); previous = key }
        Rows.this.next = in.readLine()
        Base64.getEncoder.encodeToString(line.getBytes(UTF_8))
      }
    }
    def close(): Unit = in.close()
  }
  // Existing sort primitives, explicitly bounded to 16 readers and 4096 records.
  // Only one vendor's rows are materialized; successful scratch is removed immediately.
  private def canonical(rows: Iterator[String], dir: File): (Iterator[String], () => Unit) = {
    val buffer = mutable.ArrayBuffer[String]()
    while (rows.hasNext && buffer.size < chunkRows) buffer += rows.next()
    if (!rows.hasNext) return (buffer.sorted.iterator, () => ())
    require(dir.mkdir()); var count = 0; var level = 0
    var paths = Vector.empty[String]
    def spill(): Unit = {
      require(paths.size < 100000, "Audit run path bound")
      paths :+= SortEngine.spillChunk(SortEngine.sortChunk(buffer, spec), dir.getPath, count)
      count += 1; buffer.clear()
    }
    spill()
    while (rows.hasNext) { buffer += rows.next(); if (buffer.size == chunkRows) spill() }
    if (buffer.nonEmpty) spill()
    while (paths.size > 1) {
      paths = paths.grouped(16).zipWithIndex.map { case (group, i) =>
        val out = new File(dir, s"audit_merge_${level}_$i")
        SortEngine.mergeSpills(group, out.getPath, spec)
        group.foreach(p => require(new File(p).delete())); out.getPath
      }.toVector
      level += 1
    }
    val result = reader(new File(paths.head))
    val it = new Iterator[String] {
      var pending = result.readLine()
      def hasNext: Boolean = pending != null
      def next(): String = { require(hasNext); val x = pending; pending = result.readLine(); x }
    }
    (it, () => { result.close(); require(new File(paths.head).delete()); require(dir.delete()) })
  }
  def compare(left: File, right: File, scratch: File): Long = {
    val a = new Rows(left); val b = new Rows(right); var n = 0L; var groups = 0L
    try {
      require(a.header == b.header, s"Header mismatch: ${left.getName}")
      while (a.next != null || b.next != null) {
        require(a.next != null && b.next != null && a.group == b.group, s"Group mismatch: ${left.getName} row=$n")
        val key = a.group
        val (ai, ac) = canonical(a.takeGroup(key), new File(scratch, "left"))
        val (bi, bc) = canonical(b.takeGroup(key), new File(scratch, "right"))
        try {
          while (ai.hasNext && bi.hasNext) { require(ai.next() == bi.next(), s"Record mismatch: ${left.getName} group=$key row=$n"); n += 1 }
          require(!ai.hasNext && !bi.hasNext, s"Duplicate/count mismatch: ${left.getName} group=$key")
        } finally { ac(); bc() }
        groups += 1
      }
      println(s"PASS ${left.getName} rows=$n groups=$groups exact_record_multiset=true"); n
    } finally { a.close(); b.close() }
  }
  def main(args: Array[String]): Unit = {
    require(args.length == 3, "BASELINE_REPORT NEW_REPORT NEW_SCRATCH")
    val scratch = new File(args(2)); require(!scratch.exists() && scratch.mkdirs())
    names.foreach(name => compare(source(new File(args(0)), name), source(new File(args(1)), name), scratch))
    require(scratch.delete()); println("PASS all declared reporting outputs exactly equivalent")
  }
}
