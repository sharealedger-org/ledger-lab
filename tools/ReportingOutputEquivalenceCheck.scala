import java.io._
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

/** Bounded audit-tool falsifiers; no financial engine execution. */
object ReportingOutputEquivalenceCheck {
  def main(args: Array[String]): Unit = {
    val root = Files.createTempDirectory("report-equivalence-check").toFile
    def file(name: String, header: String, rows: Seq[String]): File = {
      val f = new File(root, name)
      val stream = new FileOutputStream(f)
      val out = new PrintWriter(new OutputStreamWriter(if (name.endsWith(".gz")) new GZIPOutputStream(stream) else stream, UTF_8))
      try { out.println(header); rows.foreach(out.println) } finally out.close()
      f
    }
    def compare(a: File, b: File, label: String): Unit = {
      val scratch = new File(root, label); require(scratch.mkdir())
      ReportingOutputEquivalence.compare(a, b, scratch)
      require(scratch.delete())
    }
    def rejects(body: => Unit): Unit = {
      var rejected = false
      try body catch { case _: IllegalArgumentException => rejected = true }
      require(rejected, "Cheapest falsifier was incorrectly accepted")
    }
    val rows = (0 until 4300).map(i => s"v1,${i % 317},${i % 7}") ++ Seq("v2,1,2", "v2,1,2")
    val reversed = rows.take(4300).reverse ++ rows.drop(4300)
    val a = file("a.csv", "vendor_id,key,value", rows)
    val b = file("b.csv.gz", "vendor_id,key,value", reversed)
    compare(a, b, "spill-gzip-reorder")
    rejects(compare(a, file("changed.csv", "vendor_id,key,value", reversed.updated(0, "v1,999,999")), "changed"))
    rejects(compare(a, file("duplicate.csv", "vendor_id,key,value", reversed.dropRight(1)), "duplicate"))
    rejects(compare(a, file("header.csv", "vendor_id,key,wrong", reversed), "header"))
    val q = Seq("\"a,b\",2", "\"a,b\",1", "z,3")
    compare(file("q.csv", "vendor_id,value", q), file("qr.csv", "vendor_id,value", q.take(2).reverse ++ q.drop(2)), "quoted")
    compare(file("g.csv", "side,value", Seq("x,2", "y,1", "x,2")), file("gr.csv.gz", "side,value", Seq("x,2", "x,2", "y,1")), "global")
    // The directory is exclusively this test's temporary output, including failed-case scratch.
    def remove(f: File): Unit = { if (f.isDirectory) f.listFiles().foreach(remove); require(f.delete()) }
    remove(root)
    println("PASS audit reorder/gzip/spill/duplicates/changed-record/header/quoted/global checks")
  }
}
