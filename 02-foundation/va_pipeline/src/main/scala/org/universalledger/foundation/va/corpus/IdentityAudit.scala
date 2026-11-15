package org.universalledger.foundation.va.corpus

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.{DigestInputStream, MessageDigest}
import java.util.{Base64, Locale}
import org.universalledger.foundation.va.ledger.{SortEngine, standardizeAndSort}
import scala.collection.mutable
import scala.io.Source

/** Read-only, unmeasured diagnostic. Never creates identities or financial state. */
object IdentityAudit {
  val Version = "identity-audit-2"
  val Zero = "0000000000"
  val Generic = Set("", "JOURNAL ENTRY", "JOURNAL ENTRIES", "PAYROLL", "UNKNOWN", "N/A", "NONE", "VARIOUS", "MULTIPLE VENDORS")
  def normalize(s: String): String = s.trim.toUpperCase(Locale.ROOT)
  def label(s: String): String = if (Generic(normalize(s))) "generic_label_heuristic" else "meaningful_name_candidate"
  def pad(s: String): String = try s.toLong.formatted("%010d") catch { case _: NumberFormatException => s }
  def legacy(s: String): Vector[String] = s.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\"")).toVector
  def strict(s: String): Option[Vector[String]] = {
    val parsed = CorpusProfile.parseRecord(s.stripPrefix("\uFEFF").getBytes(StandardCharsets.UTF_8), ','.toByte)
    if (parsed.closed) Some(parsed.fields.toVector) else None
  }
  def csv(values: Seq[Any]): String = values.map { v =>
    val s = v.toString; if (s.exists(c => c == ',' || c == '"' || c == '\n' || c == '\r')) "\"" + s.replace("\"", "\"\"") + "\"" else s
  }.mkString(",")
  def writer(f: File, header: Seq[String]): PrintWriter = {
    val p = new PrintWriter(f, "UTF-8"); p.println(csv(header)); p
  }
  def write(p: PrintWriter, values: Any*): Unit = p.println(csv(values))
  def close(p: PrintWriter): Unit = { p.flush(); val bad = p.checkError(); p.close(); require(!bad, "Diagnostic write failed") }
  def enc(k: Vector[String]): String = k.map(s => Base64.getUrlEncoder.withoutPadding.encodeToString(s.getBytes(StandardCharsets.UTF_8))).mkString(".")
  def dec(s: String): Vector[String] = s.split("\\.", -1).map(v => new String(Base64.getUrlDecoder.decode(v), StandardCharsets.UTF_8)).toVector

  final class Read(val file: File, manifest: PrintWriter) extends AutoCloseable {
    private val length = file.length(); private val modified = file.lastModified()
    private val digest = MessageDigest.getInstance("SHA-256")
    private val stream = new DigestInputStream(Files.newInputStream(file.toPath), digest)
    private val reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 1024 * 1024)
    var lines = 0L; private var eof = false
    def next(): String = { val s = reader.readLine(); if (s == null) eof = true else { lines += 1; require(s.length <= 1024 * 1024, s"Oversized physical line: $file:$lines") }; s }
    override def close(): Unit = {
      reader.close()
      require(length == file.length() && modified == file.lastModified(), s"Input changed: $file")
      val sha = digest.digest().map(b => f"${b & 255}%02x").mkString
      // Preserve the original scan exception; never advertise a prefix digest as full-file SHA.
      write(manifest, file.getCanonicalPath, length, lines, if (eof) sha else "PARTIAL_READ:" + sha)
    }
  }

  case class Master(plain: Map[String, String], quoted: Map[String, Set[String]], ids: Set[String]) {
    def lookup(name: String): String = plain.getOrElse(name.toUpperCase, Zero)
    def status(name: String): (String, String) = quoted.get(normalize(name)) match {
      case None => ("missing", Zero)
      case Some(ids) if ids.size != 1 => ("ambiguous", "")
      case Some(ids) => ("unique", ids.head)
    }
  }
  def master(file: File, manifest: PrintWriter, findings: PrintWriter, tag: String): Master = {
    require(file.length() <= 16L * 1024 * 1024, "Master byte allowance exceeded")
    val in = new Read(file, manifest); val plain = mutable.HashMap[String, String]()
    val quoted = mutable.HashMap[String, Set[String]](); val allIds = mutable.HashSet[String]()
    var header: Vector[String] = Vector.empty; var plainHeader: Vector[String] = Vector.empty
    try {
      var line = in.next()
      while (line != null) {
        if (!line.startsWith("#")) {
          if (header.isEmpty) { header = strict(line).getOrElse(sys.error("Invalid master header")); plainHeader = legacy(line.stripPrefix("\uFEFF")) }
          else {
            require(in.lines <= 100001, "Master row allowance exceeded")
            val ni = header.indexOf("instHolderName"); val ii = header.indexOf("instID")
            require(ni >= 0 && ii >= 0 && plainHeader.indexOf("instHolderName") >= 0 && plainHeader.indexOf("instID") >= 0)
            val p = legacy(line); val pni = plainHeader.indexOf("instHolderName"); val pii = plainHeader.indexOf("instID")
            if (p.size > math.max(pni, pii)) {
              val n = p(pni).toUpperCase; val id = pad(p(pii))
              if (n.nonEmpty && p(pii).nonEmpty) {
                if (plain.contains(n)) write(findings, tag, in.lines, "legacy_name_overwrite", n, plain(n), id)
                plain(n) = id
              }
            }
            strict(line) match {
              case Some(r) if r.size == header.size =>
                val name = normalize(r(ni)); val id = pad(r(ii).trim)
                allIds += id
                val prior = quoted.getOrElse(name, Set.empty[String]); quoted(name) = prior + id
                if (prior.nonEmpty) write(findings, tag, in.lines, if (prior(id)) "duplicate_same_identity" else "ambiguous_name", name, prior.toSeq.sorted.mkString(";"), id)
                if (p.size <= pni || p(pni).toUpperCase != name || p.size <= pii || pad(p(pii)) != id)
                  write(findings, tag, in.lines, "plain_vs_csv_difference", r(ni), if (p.size > pni) p(pni) else "", id)
                if (r(ii) != id) write(findings, tag, in.lines, "numeric_padding", name, r(ii), id)
              case _ => write(findings, tag, in.lines, "invalid_csv_shape", "", "", "")
            }
          }
        }
        line = in.next()
      }
    } finally in.close()
    require(header.nonEmpty, "Empty master")
    Master(plain.toMap, quoted.toMap, allIds.toSet)
  }

  /** Bounded name/group counters with existing external sort and bounded fan-in. */
  final class Counts(dir: File, maxKeys: Int, fanIn: Int) {
    require(maxKeys > 0 && fanIn >= 2 && fanIn <= 64)
    dir.mkdirs()
    private val values = mutable.HashMap[Vector[String], Long]()
    private val spec = Seq(SortEngine.SortField("encoded_key", 0, "CH", "A"))
    private val runs = mutable.ArrayBuffer[String]()
    var peakKeys = 0; var spills = 0; var levels = 0
    def add(k: Vector[String], n: Long = 1): Unit = {
      require(n >= 0); values(k) = Math.addExact(values.getOrElse(k, 0L), n)
      peakKeys = peakKeys.max(values.size)
      if (values.size >= maxKeys) flush()
    }
    private def flush(): Unit = if (values.nonEmpty) {
      require(runs.size < 8192, "Diagnostic spill-file allowance exceeded")
      val rows = values.iterator.map { case (k, n) => s"${enc(k)},$n" }.toVector
      runs += SortEngine.spillChunk(SortEngine.sortChunk(rows, spec), dir.getPath, spills)
      spills += 1; values.clear()
    }
    private def reduce(input: File, output: File): Unit = {
      val src = Source.fromFile(input, "UTF-8"); val p = new PrintWriter(output, "UTF-8")
      var key: String = null; var total = 0L
      def emit(): Unit = if (key != null) p.println(s"$key,$total")
      try src.getLines().foreach { line =>
        val at = line.lastIndexOf(','); val k = line.substring(0, at); val n = line.substring(at + 1).toLong
        if (key != k) { emit(); key = k; total = 0 }; total = Math.addExact(total, n)
      } finally { emit(); src.close(); close(p) }
    }
    def finish(consume: (Vector[String], Long) => Unit): Unit = {
      flush(); var current = runs.toVector
      while (current.size > 1) {
        current = current.grouped(fanIn).zipWithIndex.map { case (g, i) =>
          val merged = new File(dir, s"merge_${levels}_$i.csv"); val reduced = new File(dir, s"reduce_${levels}_$i.csv")
          SortEngine.mergeSpills(g, merged.getPath, spec); reduce(merged, reduced)
          g.foreach(p => new File(p).delete()); merged.delete(); reduced.getPath
        }.toVector
        levels += 1
      }
      current.foreach { path =>
        // Reduce a lone initial spill as well, keeping the consumer contract uniform.
        val reduced = new File(dir, "final.csv"); reduce(new File(path), reduced)
        val src = Source.fromFile(reduced, "UTF-8")
        try src.getLines().foreach { line => val at = line.lastIndexOf(','); consume(dec(line.substring(0, at)), line.substring(at + 1).toLong) }
        finally src.close()
        new File(path).delete(); reduced.delete()
      }
    }
  }

  /** Current instrument only; independent of the number of transaction rows. */
  final class VendorFold(emit: (String, String, Long) => Unit, requireOrder: Boolean = true,
    onDisorder: (String, String, Long) => Unit = (_: String, _: String, _: Long) => (),
    onSegment: String => Unit = (_: String) => ()) {
    private var current: String = null
    private val sources = mutable.HashMap[String, Long]()
    var peakSourceKeys = 0; var rows = 0L
    def add(id: String, source: String): Unit = {
      require(id.nonEmpty, "Empty instrument ID")
      if (current != null && id != current) {
        if (id < current) { onDisorder(current, id, rows + 1); require(!requireOrder, s"Instrument disorder: $id after $current") }
        flush()
      }
      if (current != id) onSegment(id)
      current = id; sources(source) = sources.getOrElse(source, 0L) + 1; rows += 1
      require(sources.size <= 64, "Source-system allowance per vendor exceeded"); peakSourceKeys = peakSourceKeys.max(sources.size)
    }
    def flush(): Unit = { if (current != null) sources.toSeq.sortBy(_._1).foreach { case (s, n) => emit(current, s, n) }; sources.clear() }
  }

  def validAmount(row: Vector[String], minimum: Int): Boolean = row.size >= minimum &&
    (try { BigDecimal(row(1).replace(",", "")); true } catch { case _: NumberFormatException => false })

  def main(args: Array[String]): Unit = {
    require(args.length % 2 == 0, "Arguments require key/value pairs")
    val p = args.grouped(2).map(a => a(0).stripPrefix("--") -> a(1)).toMap
    val out = new File(p("out")); require(!out.exists(), "Use a new diagnostic output directory"); out.mkdirs()
    val manifest = writer(new File(out, "input_manifest.csv"), Seq("path", "bytes", "physical_lines", "sha256"))
    val findings = writer(new File(out, "master_findings.csv"), Seq("snapshot", "record", "finding", "name", "before", "after"))
    val metrics = writer(new File(out, "audit_metrics.csv"), Seq("metric", "value"))
    val frozen = master(new File(p("master")), manifest, findings, "measured")
    val enriched = master(new File(p("currentMaster")), manifest, findings, "enriched")
    val maxKeys = p.getOrElse("maxKeys", "50000").toInt; val fanIn = p.getOrElse("fanIn", "16").toInt
    val years = p.getOrElse("years", "03,04,05,06,07,08,09,10,11,12,13,14,15,16").split(",").toVector
    write(metrics, "version", Version); write(metrics, "measured_master_legacy_keys", frozen.plain.size)
    write(metrics, "measured_master_strict_names", frozen.quoted.size); write(metrics, "enriched_master_strict_names", enriched.quoted.size)
    write(metrics, "master_names_with_changed_legacy_lookup", (frozen.plain.keySet ++ enriched.plain.keySet).count(n => frozen.lookup(n) != enriched.lookup(n)))
    write(metrics, "generic_exact_labels", Generic.toSeq.sorted.mkString(";"))
    try p("mode") match {
      case "master" =>
        val names = writer(new File(out, "master_comparison.csv"), Seq("name", "measured_legacy_id", "measured_csv_status", "measured_csv_id", "enriched_legacy_id", "enriched_csv_status", "enriched_csv_id"))
        try { (frozen.quoted.keySet ++ enriched.quoted.keySet ++ frozen.plain.keySet ++ enriched.plain.keySet).toSeq.sorted.foreach { n =>
          val a = frozen.status(n); val b = enriched.status(n); write(names, n, frozen.lookup(n), a._1, a._2, enriched.lookup(n), b._1, b._2)
        }} finally close(names)
      case "raw" => raw(p, out, years, frozen, enriched, manifest, metrics, maxKeys, fanIn)
      case "canonical" => canonical(p, out, years, frozen, manifest, metrics, maxKeys, fanIn)
      case other => sys.error(s"Unknown mode: $other")
    } finally { close(manifest); close(findings); close(metrics) }
    val done = new PrintWriter(new File(out, "AUDIT_COMPLETE"), "UTF-8"); done.println(Version); close(done)
  }

  private def raw(p: Map[String, String], out: File, years: Vector[String], frozen: Master, enriched: Master,
    manifest: PrintWriter, metrics: PrintWriter, maxKeys: Int, fanIn: Int): Unit = {
    val references = Option(new File(p("refPath")).listFiles()).getOrElse(Array.empty[File])
      .filter(f => f.getName.startsWith("aRef") && f.getName.endsWith(".txt")).sortBy(_.getName)
    require(references.map(_.length()).sum <= 16L * 1024 * 1024, "Reference byte allowance exceeded")
    val ref = standardizeAndSort.loadApaReferenceData(p("refPath"))
    references.foreach { f =>
        val input = new Read(f, manifest); try { while (input.next() != null) {} } finally input.close()
      }
    val root = new File(p("root")); val files = Option(root.listFiles()).getOrElse(Array.empty[File]).filter(_.isDirectory)
      .flatMap(d => Option(d.listFiles()).getOrElse(Array.empty[File])).filter(f => f.isFile).sortBy(_.getPath)
      .filter { f => val c = CorpusProfile.classify("VARawFiles/" + f.getParentFile.getName + "/" + f.getName)
        c.family.exists(fam => fam == CorpusProfile.ApaExp || fam == CorpusProfile.ApaRev) && c.fiscalYear.exists(y => years.contains(f"${y % 100}%02d")) }
    require(files.length == years.size * 24, s"Expected 24 APA files/year; found ${files.length}")
    val counts = new Counts(new File(out, "name_spills"), maxKeys, fanIn)
    val disposition = writer(new File(out, "source_disposition.csv"), Seq("path", "family", "year", "data_lines", "blank", "legacy_accepted", "legacy_rejected", "csv_valid", "csv_invalid", "lookup_name_difference"))
    val totals = mutable.HashMap[(String, String), Long]().withDefaultValue(0L)
    var acceptedAll = 0L
    try files.foreach { f =>
      val info = CorpusProfile.classify("VARawFiles/" + f.getParentFile.getName + "/" + f.getName)
      val family = if (info.family.contains(CorpusProfile.ApaExp)) "expense" else "revenue"
      val year = info.fiscalYear.get.toString; val min = if (family == "expense") 7 else 6
      val in = new Read(f, manifest); var data = 0L; var blank = 0L; var accepted = 0L; var rejected = 0L
      var quotedValid = 0L; var quotedInvalid = 0L; var nameDifference = 0L
      try {
        val header = in.next(); require(header != null && header.stripPrefix("\uFEFF").contains("AGENCY_NAME"), s"Unexpected APA header: $f")
        var line = in.next()
        while (line != null) {
          data += 1; val text = line.trim
          if (text.isEmpty) blank += 1
          else {
            val a = legacy(text); val q = strict(text); val valid = validAmount(a, min)
            if (q.exists(r => validAmount(r, min))) quotedValid += 1 else quotedInvalid += 1
            if (valid) {
              accepted += 1
              val name = if (family == "expense") a(6) else a(0)
              val lookup = if (family == "expense") name else ref.agencyByName.getOrElse(name.toUpperCase, "0")
              val quotedName = q.filter(_.size >= min).map(r => if (family == "expense") r(6) else r(0)).getOrElse("<INVALID_CSV>")
              val qLookup = if (family == "expense") quotedName else ref.agencyByName.getOrElse(quotedName.toUpperCase, "0")
              if (name != quotedName) nameDifference += 1
              val status = frozen.status(qLookup)
              counts.add(Vector(family, year, name, lookup, frozen.lookup(lookup), quotedName, status._1, status._2,
                enriched.lookup(lookup), if (family == "revenue") "agency_not_counterparty" else label(name)))
            } else rejected += 1
          }
          line = in.next()
        }
      } finally in.close()
      require(data == blank + accepted + rejected)
      write(disposition, f.getCanonicalPath, family, year, data, blank, accepted, rejected, quotedValid, quotedInvalid, nameDifference)
      acceptedAll += accepted; totals((family, year)) += accepted
      println(s"RAW ${f.getName}: legacy_accepted=$accepted legacy_rejected=$rejected")
    } finally close(disposition)
    val table = writer(new File(out, "source_names.csv"), Seq("family", "year", "legacy_source_name", "actual_lookup_key", "measured_legacy_id", "csv_source_name", "csv_master_status", "csv_master_id", "enriched_legacy_id", "classification", "source_rows"))
    val global = new Counts(new File(out, "global_name_spills"), maxKeys, fanIn)
    val classes = mutable.HashMap[Vector[String], Long]().withDefaultValue(0L)
    val expectedJournal = mutable.HashMap[Vector[String], Long]().withDefaultValue(0L)
    var emitted = 0L
    try counts.finish { (k, n) =>
      table.println(csv(k :+ n.toString)); emitted += n
      val status = if (k(4) == Zero) "unmatched_zero" else "mapped"
      val summaryKey = Vector(k(1), k(0), k(9), status, (k(2) != k(5)).toString, (k(4) != k(8)).toString)
      classes(summaryKey) += n
      expectedJournal(Vector(k(1), k(0), status)) += n * 2
      global.add(Vector("legacy", k(0), normalize(k(2)), k(9), status, k(4)), n)
      val cls = if (k(0) == "revenue") "agency_not_counterparty" else if (k(5) == "<INVALID_CSV>") "invalid_csv" else label(k(5))
      global.add(Vector("quote_aware", k(0), normalize(k(5)), cls, k(6), k(7)), n)
    } finally close(table)
    require(emitted == acceptedAll)
    val summaries = writer(new File(out, "source_summary.csv"), Seq("year", "family", "classification", "legacy_status", "parser_name_difference", "enriched_lookup_change", "source_rows"))
    try classes.toSeq.sortBy(_._1.mkString("|")).foreach { case (k, n) => summaries.println(csv(k :+ n.toString)) } finally close(summaries)
    val projected = writer(new File(out, "expected_journal_identity_counts.csv"), Seq("year", "family", "legacy_status", "expected_sje_rows"))
    try expectedJournal.toSeq.sortBy(_._1.mkString("|")).foreach { case (k, n) => projected.println(csv(k :+ n.toString)) } finally close(projected)
    val names = writer(new File(out, "global_names.csv"), Seq("interpretation", "family", "normalized_name", "classification", "status", "id", "source_rows"))
    val nameSummary = mutable.HashMap[Vector[String], (Long, Long)]()
    val top = mutable.PriorityQueue[(Long, Vector[String])]()(Ordering.by[(Long, Vector[String]), Long](_._1).reverse)
    try global.finish { (k, n) =>
      names.println(csv(k :+ n.toString))
      val sk = Vector(k(0), k(1), k(3), k(4)); val before = nameSummary.getOrElse(sk, (0L, 0L))
      nameSummary(sk) = (before._1 + 1, before._2 + n)
      if (k(1) == "expense" && (k(4) == "unmatched_zero" || k(4) == "missing" || k(4) == "ambiguous")) {
        top.enqueue(n -> k); if (top.size > 100) top.dequeue()
      }
    } finally close(names)
    val ns = writer(new File(out, "distinct_name_summary.csv"), Seq("interpretation", "family", "classification", "status", "distinct_normalized_names", "source_rows"))
    try nameSummary.toSeq.sortBy(_._1.mkString("|")).foreach { case (k, (distinct, n)) => ns.println(csv(k ++ Vector(distinct.toString, n.toString))) } finally close(ns)
    val tp = writer(new File(out, "top_unmatched_names.csv"), Seq("interpretation", "family", "normalized_name", "classification", "status", "id", "source_rows"))
    try top.dequeueAll.foreach { case (n, k) => tp.println(csv(k :+ n.toString)) } finally close(tp)
    val control = writer(new File(out, "source_count_controls.csv"), Seq("year", "expense_accepted", "revenue_accepted", "combined_accepted", "recorded_ttl_rows", "delta", "status"))
    var mismatches = 0
    try years.foreach { yy =>
      val f = new File(p("runRoot"), s"FY$yy/metrics/process_metrics.csv")
      val in = new Read(f, manifest); var recorded = 0L
      try { val h = strict(in.next()).get; var line = in.next()
        while (line != null) { val r = strict(line).get; if (r(h.indexOf("process_name")) == "step-ttl") recorded += r(h.indexOf("input_rows")).toLong; line = in.next() }
      } finally in.close()
      val e = totals(("expense", "20" + yy)); val r = totals(("revenue", "20" + yy)); val delta = e + r - recorded
      if (delta != 0) mismatches += 1
      write(control, "20" + yy, e, r, e + r, recorded, delta, if (delta == 0) "PASS" else "MISMATCH")
    } finally close(control)
    write(metrics, "raw_accepted_rows", acceptedAll); write(metrics, "raw_emitted_rows", emitted)
    write(metrics, "source_count_mismatches", mismatches); write(metrics, "peak_name_keys", counts.peakKeys)
    write(metrics, "name_spills", counts.spills); write(metrics, "name_merge_levels", counts.levels)
    write(metrics, "global_name_spills", global.spills); write(metrics, "peak_global_name_keys", global.peakKeys)
  }

  private def canonical(p: Map[String, String], out: File, years: Vector[String], frozen: Master,
    manifest: PrintWriter, metrics: PrintWriter, maxKeys: Int, fanIn: Int): Unit = {
    val counts = new Counts(new File(out, "group_spills"), maxKeys, fanIn)
    val segments = new Counts(new File(out, "segment_spills"), maxKeys, fanIn)
    val order = writer(new File(out, "vendor_order_violations.csv"), Seq("path", "year", "side", "row", "previous_vendor", "next_vendor"))
    val byYear = writer(new File(out, "vendor_year_groups.csv"), Seq("vendor_id", "year", "side", "source_system", "rows"))
    val zero = writer(new File(out, "zero_source_breakdown.csv"), Seq("year", "side", "source_system", "rows"))
    val total = mutable.HashMap[String, Long]().withDefaultValue(0L)
    var peakKeys = 0; var orderViolations = 0L
    try years.foreach { yy => Seq("movement" -> s"SortedJEFY${yy}merged.csv", "balance" -> s"LDGR20$yy.csv").foreach { case (side, name) =>
      val f = new File(p("runRoot"), s"FY$yy/$name").getCanonicalFile
      val fold = new VendorFold((id, source, n) => {
        write(byYear, id, "20" + yy, side, source, n); counts.add(Vector(id, side, source), n)
        if (id.nonEmpty && id.forall(_ == '0')) write(zero, "20" + yy, side, source, n)
      }, requireOrder = false, onDisorder = (before, after, row) => {
        orderViolations += 1; write(order, f.getCanonicalPath, "20" + yy, side, row, before, after)
      }, onSegment = id => segments.add(Vector(id, "20" + yy, side)))
      val in = new Read(f, manifest)
      try {
        if (side == "balance") require(in.next().startsWith("ldgrIPID,"), "Ledger header mismatch")
        var line = in.next()
        while (line != null) {
          // These generated contracts are comma-delimited; only leading identity/source fields
          // are read. No financial amount is parsed or recomputed by the audit.
          val r = line.split(",", if (side == "movement") 8 else 6)
          require(r.length == (if (side == "movement") 8 else 6), s"Short canonical row $f:${in.lines}")
          fold.add(r(0), r(if (side == "movement") 6 else 4)); line = in.next()
        }
        fold.flush()
      } finally in.close()
      total(side) += fold.rows; peakKeys = peakKeys.max(fold.peakSourceKeys)
      println(s"CANONICAL ${f.getName}: rows=${fold.rows}")
    }} finally { close(byYear); close(zero); close(order) }
    val segmentOut = writer(new File(out, "vendor_segments.csv"), Seq("vendor_id", "year", "side", "segments"))
    var reentries = 0L
    try segments.finish { (k, n) => segmentOut.println(csv(k :+ n.toString)); if (n > 1) reentries += 1 } finally close(segmentOut)
    val groups = writer(new File(out, "vendor_source_groups.csv"), Seq("vendor_id", "side", "source_system", "rows"))
    val concentration = writer(new File(out, "vendor_concentration.csv"), Seq("vendor_id", "movement_rows", "posted_balance_rows", "financial_input_rows", "exceeds_250000", "id_status", "measured_master_literal_match", "padding_candidate_only"))
    var current: String = null; var movement = 0L; var balance = 0L; var allM = 0L; var allB = 0L
    var vendors = 0L; var oversized = 0L; var largest = 0L
    def emit(): Unit = if (current != null) {
      val n = movement + balance; vendors += 1; if (n > 250000) oversized += 1; largest = largest.max(n)
      write(concentration, current, movement, balance, n, n > 250000,
        if (current.forall(_ == '0')) "unattributed_zero" else if (current.startsWith("UNMATCHED_")) "unmatched_surrogate" else "observed_key_not_identity_proof",
        frozen.ids(current), current != pad(current) && frozen.ids(pad(current)))
      allM += movement; allB += balance
    }
    try counts.finish { (k, n) =>
      groups.println(csv(k :+ n.toString))
      if (current != k(0)) { emit(); current = k(0); movement = 0; balance = 0 }
      if (k(1) == "movement") movement += n else balance += n
    } finally { emit(); close(groups); close(concentration) }
    require(allM == total("movement") && allB == total("balance"), "Canonical group counts do not reconcile")
    write(metrics, "canonical_movement_rows", allM); write(metrics, "canonical_posted_rows", allB)
    write(metrics, "vendor_keys", vendors); write(metrics, "groups_exceeding_250000", oversized)
    write(metrics, "largest_financial_input_group", largest); write(metrics, "peak_sources_in_one_vendor", peakKeys)
    write(metrics, "peak_combiner_keys", counts.peakKeys); write(metrics, "group_spills", counts.spills)
    write(metrics, "literal_vendor_order_violations", orderViolations); write(metrics, "vendor_year_side_groups_with_reentry", reentries)
    write(metrics, "report_dimension_rows", 0); write(metrics, "scope", "one_run_all_years_financial_inputs;failed_report_had_no_dimensions")
  }
}
