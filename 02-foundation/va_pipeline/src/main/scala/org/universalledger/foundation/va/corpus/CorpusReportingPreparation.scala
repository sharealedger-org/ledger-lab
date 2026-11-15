package org.universalledger.foundation.va.corpus

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, StandardCopyOption}
import org.universalledger.foundation.va.ledger.{DatedSyntheticInputs, LedgerApp, ReportingSupport, SortEngine}
import scala.collection.mutable
import scala.io.Source

/** Unmeasured input preparation from committed financial outputs; never changes financial IDs. */
object CorpusReportingPreparation {
  import ReportingSupport._
  val Version = "sorted_population_cycle_v2"
  final class IdSort(work: File, capacity: Int, fanIn: Int) {
    require(capacity > 0 && fanIn >= 2 && fanIn <= 16)
    require(work.mkdirs())
    private val spec = Seq(SortEngine.SortField("literal_id", 0, "CH", "A"))
    private val buffer = mutable.ArrayBuffer[String]()
    private val runs = mutable.ArrayBuffer[String]()
    var spills = 0; var levels = 0; var peak = 0
    def add(id: String): Unit = {
      require(id.nonEmpty && id.length <= 256 && !id.exists(c => c == ',' || c == '"' || c.isWhitespace), "Invalid canonical ID")
      buffer += id; peak = peak.max(buffer.size)
      if (buffer.size == capacity) flush()
    }
    private def flush(): Unit = if (buffer.nonEmpty) {
      require(runs.size < 100000, "Preparation run-file bound exceeded")
      runs += SortEngine.spillChunk(buffer.distinct.sorted, work.getPath, spills)
      spills += 1; buffer.clear()
    }
    private def unique(input: File, output: File): Unit = {
      val in = Source.fromFile(input, "UTF-8"); val out = new PrintWriter(output, "UTF-8")
      var previous = ""
      try in.getLines().foreach { id =>
        require(id >= previous, "ID sort order violated")
        if (id != previous) out.println(id)
        previous = id
      } finally { in.close(); IdentityAudit.close(out) }
    }
    def finish(consume: String => Unit): Unit = {
      flush(); var current = runs.toVector
      while (current.size > 1) {
        current = current.grouped(fanIn).zipWithIndex.map { case (group, index) =>
          val merged = new File(work, s"merge_${levels}_$index.csv")
          val reduced = new File(work, s"unique_${levels}_$index.csv")
          SortEngine.mergeSpills(group, merged.getPath, spec); unique(merged, reduced)
          group.foreach(p => require(new File(p).delete())); require(merged.delete())
          reduced.getPath
        }.toVector
        levels += 1
      }
      current.foreach { path =>
        val in = Source.fromFile(path, "UTF-8"); var previous = ""
        try in.getLines().foreach { id =>
          require(id >= previous)
          if (id != previous) consume(id)
          previous = id
        } finally in.close()
        require(new File(path).delete())
      }
    }
  }

  def prepare(dirs: Vector[File], histories: File, out: File, capacity: Int = 50000, fanIn: Int = 16): Unit = {
    require(dirs.nonEmpty && dirs.distinct.size == dirs.size && !out.exists())
    val attempt = new File(out.getPath + ".attempt"); require(!attempt.exists() && attempt.mkdirs())
    val manifest = writer(attempt, "input_manifest.csv", Seq("path", "bytes", "physical_lines", "sha256"))
    val historyManifest = writer(attempt, "history_manifest.csv", Seq("kind", "path", "rows", "bytes", "sha256"))
    val profiles = writer(attempt, "profile.csv", Seq("file", "kind", "rows", "bytes", "zero_rows", "zero_bytes", "max_record_bytes", "zero_max_record_bytes", "expense_rows", "expense_zero_rows", "revenue_rows", "revenue_zero_rows"))
    val assignment = writer(attempt, "synthetic_assignments.csv", Seq("vendor_id", "template_instrument_id"))
    val sort = new IdSort(new File(attempt, "sort"), capacity, fanIn)
    var assignments = 0L; var excluded = 0L; var allRows = 0L
    try {
      val templates = new DatedSyntheticInputs(histories, historyManifest).instruments
      val selected = dirs.flatMap { dir =>
        require(new File(dir, "metrics/run_manifest.csv").isFile, s"Missing committed manifest: $dir")
        var complete = 0
        ConnectedViewsCheck.scan(new File(dir, "metrics/run_manifest.csv")) { r => require(r("status") == "complete"); complete += 1 }
        require(complete > 0)
        val files = Option(dir.listFiles()).getOrElse(Array.empty[File]).toVector
        val movement = files.filter(f => f.getName.matches("SortedJEFY[0-9]{2}merged\\.csv"))
        val balances = files.filter(f => f.getName.matches("LDGR[0-9]{4}\\.csv"))
        require(movement.size == 1 && balances.size == 1, s"Exactly one merged SJE and ledger required: $dir")
        movement.map(_ -> "movement") ++ balances.map(_ -> "balance")
      }
      require(selected.map(_._1.getCanonicalPath).distinct.size == selected.size, "Duplicate physical inputs")
      selected.foreach { case (file, kind) =>
        val in = new IdentityAudit.Read(file, manifest)
        var rows = 0L; var zero = 0L; var zeroBytes = 0L; var maxBytes = 0; var zeroMax = 0
        var expense = 0L; var expenseZero = 0L; var revenue = 0L; var revenueZero = 0L; var previous = ""
        try {
          if (kind == "balance") require(IdentityAudit.strict(in.next()).exists(_.reverse.dropWhile(_.isEmpty).reverse == ledgerHeader), s"Ledger header: $file")
          var line = in.next()
          while (line != null) {
            require(line.nonEmpty && !line.startsWith("\""), s"Canonical headerless ID contract: $file:${in.lines}")
            val raw = line.split(",", -1)
            val columns = if (kind == "balance" && raw.length == 31 && raw.last.isEmpty) raw.dropRight(1) else raw
            require(columns.length == (if (kind == "movement") 39 else 30), s"Canonical width: $file:${in.lines}")
            val id = columns(0); val bytes = line.getBytes(StandardCharsets.UTF_8).length
            val isZero = id.nonEmpty && id.forall(_ == '0')
            if (id != previous) { sort.add(id); previous = id }
            rows += 1; maxBytes = maxBytes.max(bytes)
            if (isZero) { zero += 1; zeroBytes += bytes + 1; zeroMax = zeroMax.max(bytes) }
            if (kind == "movement") {
              if (columns(5) == "EXPENSE_PAYMENT") { expense += 1; if (isZero) expenseZero += 1 }
              if (columns(5) == "REVENUE_RECEIPT") { revenue += 1; if (isZero) revenueZero += 1 }
            }
            line = in.next()
          }
        } finally in.close()
        row(profiles, file.getCanonicalPath, kind, rows, file.length(), zero, zeroBytes, maxBytes, zeroMax, expense, expenseZero, revenue, revenueZero)
        require(revenue == revenueZero, s"Agency receipts must remain unattributed: $revenue rows, $revenueZero zero rows in $file")
        allRows += rows
      }
      sort.finish { id =>
        if (attributed(id)) {
          row(assignment, id, templates((assignments % templates.size).toInt)); assignments += 1
        } else excluded += 1
      }
      val metrics = writer(attempt, "preparation_metrics.csv", Seq("metric", "value"))
      try Seq("version" -> Version, "financial_rows" -> allRows.toString, "assigned_ids" -> assignments.toString,
        "unattributed_ids" -> excluded.toString, "sort_spills" -> sort.spills.toString, "sort_levels" -> sort.levels.toString,
        "peak_buffer_ids" -> sort.peak.toString, "merge_fan_in" -> fanIn.toString).foreach { case (k,v) => row(metrics,k,v) }
      finally IdentityAudit.close(metrics)
    } finally Seq(manifest,historyManifest,profiles,assignment).foreach(IdentityAudit.close)
    Files.write(new File(attempt,"PREPARATION_COMPLETE").toPath,(Version + "\n").getBytes(StandardCharsets.UTF_8))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"RESULT corpus_reporting_preparation=$out rows=$allRows assignments=$assignments excluded_ids=$excluded")
  }
  def main(args: Array[String]): Unit = {
    val p = LedgerApp.parseArgs(args)
    prepare(p("inPaths").split(",").toVector.map(p => new File(p).getCanonicalFile),new File(p("histories")),new File(p("out")),
      p.getOrElse("capacity","50000").toInt,p.getOrElse("fanIn","16").toInt)
  }
}
