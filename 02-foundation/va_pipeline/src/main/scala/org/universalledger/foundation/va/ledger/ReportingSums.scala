package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter}
import scala.collection.mutable
import scala.io.Source

/** Analytical partials only. Bounded keys and merge handles, independent of vendor size. */
final class ReportingSums(dir: File, capacity: Int, fanIn: Int) extends AutoCloseable {
  require(capacity > 0 && fanIn >= 2 && fanIn <= 128)
  require(dir.mkdirs(), s"Partial directory already exists: $dir")
  private val buffer = mutable.HashMap[String, Vector[BigDecimal]]()
  private var count = 0
  var peak = 0; var bytes = 0L; var passes = 0; var partials = 0L
  private def file(level: Int, n: Int) = new File(dir, s"$level-$n.csv")
  private def addValues(a: Vector[BigDecimal], b: Vector[BigDecimal]): Vector[BigDecimal] = {
    require(a.size == b.size, "Partial schema mismatch"); a.zip(b).map { case (x, y) => x + y }
  }
  def add(k: Seq[String], values: BigDecimal*): Unit = {
    val encoded = ReportingSupport.key(k)
    if (!buffer.contains(encoded) && buffer.size >= capacity) flush()
    buffer(encoded) = buffer.get(encoded).map(addValues(_, values.toVector)).getOrElse(values.toVector)
    peak = peak.max(buffer.size)
  }
  private def checked(p: PrintWriter): Unit = {
    p.flush(); val error = p.checkError(); p.close(); require(!error, "Analytical partial write failed")
  }
  private def flush(): Unit = if (buffer.nonEmpty) {
    val f = file(0, count); val p = new PrintWriter(f, "UTF-8")
    try buffer.toVector.sortBy(_._1).foreach { case (k, v) => p.println((k +: v.map(_.toString)).mkString(",")) }
    finally checked(p)
    bytes += f.length(); partials += buffer.size; count += 1; buffer.clear()
  }
  /** Consume each fully reduced key once; input partial files are removed after successful merge. */
  def foreach(f: (Vector[String], Vector[BigDecimal]) => Unit): Unit = {
    flush()
    var level = 0
    while (count > 1) {
      var next = 0
      (0 until count by fanIn).foreach { start =>
        val group = (start until math.min(start + fanIn, count)).map(file(level, _))
        val sources = group.map(Source.fromFile(_, "UTF-8"))
        val p = new PrintWriter(file(level + 1, next), "UTF-8")
        case class Head(k: String, v: Vector[BigDecimal], i: Int)
        val q = mutable.PriorityQueue[Head]()(Ordering.by[Head, (String, Int)](h => (h.k, h.i)).reverse)
        val iterators = sources.map(_.getLines())
        def advance(i: Int): Unit = if (iterators(i).hasNext) {
          val r = iterators(i).next().split(",", -1)
          q.enqueue(Head(r(0), r.drop(1).map(BigDecimal(_)).toVector, i))
        }
        var k: String = null; var value = Vector.empty[BigDecimal]
        def emit(): Unit = if (k != null) p.println((k +: value.map(_.toString)).mkString(","))
        try {
          iterators.indices.foreach(advance)
          while (q.nonEmpty) {
            val h = q.dequeue()
            if (k == h.k) value = addValues(value, h.v)
            else { emit(); k = h.k; value = h.v }
            advance(h.i)
          }
          emit()
        } finally { sources.foreach(_.close()); checked(p) }
        bytes += file(level + 1, next).length()
        group.foreach(x => require(x.delete(), s"Cannot remove merged partial $x"))
        next += 1
      }
      count = next; level += 1; passes += 1
    }
    if (count == 1) {
      val source = Source.fromFile(file(level, 0), "UTF-8")
      try source.getLines().foreach { line =>
        val r = line.split(",", -1)
        f(ReportingSupport.unkey(r(0)), r.drop(1).map(BigDecimal(_)).toVector)
      } finally source.close()
      require(file(level, 0).delete())
    }
    count = 0
  }
  def close(): Unit = { require(buffer.isEmpty && count == 0, s"Unconsumed analytical partials: $dir"); require(dir.delete()) }
}
