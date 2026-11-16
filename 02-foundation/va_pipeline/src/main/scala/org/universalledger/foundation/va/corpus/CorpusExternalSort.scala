package org.universalledger.foundation.va.corpus

import java.io.{File, PrintWriter}
import java.nio.file.Files
import org.universalledger.foundation.va.ledger.{ReportingSupport, SortEngine}
import scala.collection.mutable

/** Source-preparation order boundary; fixed fan-in and chunk/state limits. */
object CorpusExternalSort {
  val header=Vector("partition","rows","input_bytes","output_bytes","spills","merge_levels","fan_in","chunk_rows","cumulative_sort_bytes","elapsed_seconds","peak_buffer_bytes","buffer_byte_cap")
  def key(parts: String*): String = parts.map(_.getBytes("UTF-8").map(b => f"${b & 255}%02x").mkString).mkString("!")
  def apply(input: File, target: File, chunk: Int, metrics: PrintWriter): Long = {
    require(chunk > 0 && chunk <= 100000 && !target.exists())
    val start = System.nanoTime()
    val work = new File(target.getPath + ".sort"); require(work.mkdirs())
    val spec = Seq(SortEngine.SortField("encoded_tuple", 0, "CH", "A"))
    val paths = mutable.ArrayBuffer[String](); val buffer = mutable.ArrayBuffer[String]()
    var rows = 0L; var spills = 0; var levels = 0; var written = 0L
    val byteCap=32L*1024*1024;var bufferedBytes=0L;var peakBytes=0L
    def flush(): Unit = if (buffer.nonEmpty) {
      require(paths.size < 100000, "Sort run path bound")
      val p = SortEngine.spillChunk(SortEngine.sortChunk(buffer, spec), work.getPath, spills)
      paths += p; written += new File(p).length(); spills += 1; buffer.clear();bufferedBytes=0
    }
    val in = scala.io.Source.fromFile(input, "UTF-8")
    try in.getLines().foreach { line =>
      val bytes=line.getBytes("UTF-8").length
      require(line.nonEmpty && bytes <= 16384, "Source-preparation record bound")
      if(bufferedBytes+bytes>byteCap)flush()
      bufferedBytes+=bytes;peakBytes=peakBytes.max(bufferedBytes)
      buffer += line; rows += 1; if (buffer.size >= chunk) flush()
    } finally in.close()
    flush(); var current = paths.toVector
    while (current.size > 1) {
      current = current.grouped(16).zipWithIndex.map { case (group, i) =>
        val f = new File(work, s"merge_${levels}_$i.csv")
        SortEngine.mergeSpills(group, f.getPath, spec); written += f.length()
        group.foreach(p => require(new File(p).delete())); f.getPath
      }.toVector
      levels += 1
    }
    if (current.isEmpty) Files.createFile(target.toPath) else Files.move(new File(current.head).toPath, target.toPath)
    require(work.delete())
    val check=scala.io.Source.fromFile(target,"UTF-8");var actual=0L
    try check.getLines().foreach(_=>actual+=1)finally check.close()
    require(actual==rows && target.length()==input.length(),"Sort row/byte conservation; internal partitions require LF-terminated records")
    ReportingSupport.row(metrics, target.getName, rows, input.length(), target.length(), spills, levels, 16, chunk, written, (System.nanoTime()-start)/1e9,peakBytes,byteCap)
    rows
  }
}
