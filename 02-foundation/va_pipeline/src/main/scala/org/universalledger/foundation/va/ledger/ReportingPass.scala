package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import scala.collection.mutable

/** Expanded 5v: read committed financial streams once; all outputs are analytical. */
object ReportingPass {
  import ReportingSupport._
  case class Financial(vendor: String, dimensions: Vector[String], amount: BigDecimal,
    flag: String, event: String, contract: String, source: Input, ordinal: Long, raw: Vector[String]) {
    def fy: Int = fiscalYear(dimensions(13))
  }
  case class Bucket(var movement: BigDecimal = 0, var balance: BigDecimal = 0,
    var opening: BigDecimal = 0, var movements: Long = 0, var balances: Long = 0, var openings: Long = 0)
  class Features {
    var debit = BigDecimal(0); var credit = BigDecimal(0); var rows = 0L
    var poAmount = BigDecimal(0); var poCount = 0L; var reversals = 0L
    var directPoAmount = BigDecimal(0); var directPoCount = 0L
    val accounts = mutable.HashSet[String](); val periods = mutable.HashSet[String]()
    val poAgencies = mutable.HashMap[String, mutable.Set[String]]()
    val poClasses = mutable.HashMap[String, mutable.Set[String]]()
    val agencies = mutable.HashSet[String](); val classes = mutable.HashSet[String]()
    class Activity {
      var rows = 0L; var direct = BigDecimal(0); var offset = BigDecimal(0); var negative = 0L
      val periods = mutable.HashSet[String]()
    }
    val events = mutable.HashMap[String, Activity]()
    def add(r: Financial): Unit = {
      rows += 1; if (r.flag == "D") debit += r.amount else credit += r.amount
      accounts += r.dimensions(7); periods += r.dimensions(13)
      val activity = events.getOrElseUpdate(r.event, new Activity)
      activity.rows += 1; activity.periods += r.dimensions(13)
      if (r.flag == "D") { activity.direct += r.amount; if (r.amount < 0) activity.negative += 1 }
      else activity.offset += r.amount
      if (r.flag == "D" && r.amount < 0) reversals += 1
      if (r.event == "COMMITMENT" && r.flag == "D") { directPoAmount += r.amount; directPoCount += 1 }
      // Exact old view selection: nonnegative sign, including zero offsets. Keep it named.
      if (r.event == "COMMITMENT" && r.amount >= 0) {
        poCount += 1; poAmount += r.amount
        val agency = r.dimensions(3); val cls = r.dimensions(5).take(3).padTo(3, '0'); val p = r.dimensions(13)
        poAgencies.getOrElseUpdate(p, mutable.HashSet[String]()) += agency
        poClasses.getOrElseUpdate(p, mutable.HashSet[String]()) += cls
        agencies += agency; classes += cls
      }
    }
    def movementValues: Seq[Any] = Seq(poCount, poAgencies.values.map(_.size).foldLeft(0)(math.max),
      poClasses.values.map(_.size).foldLeft(0)(math.max), poAmount, agencies.size, classes.size, directPoCount, directPoAmount)
    def balanceValues: Seq[Any] = Seq(debit, credit, rows, accounts.size, periods.size)
  }
  case class Dimension(kind: String, values: Map[String, String], source: String, ordinal: Long)

  def apply(params: Map[String, String]): Unit = {
    val start = System.nanoTime()
    val out = new File(params("outPath")); out.mkdirs()
    require(!new File(out, "input_manifest.csv").exists(), "Output already contains a reporting attempt")
    val basis = params.getOrElse("balanceBasis", "period_movements")
    require(Set("period_movements", "opening_plus_movements")(basis))
    val maxVendorRows = params.getOrElse("maxVendorRows", "250000").toInt
    val maxInputs = params.getOrElse("maxInputs", "128").toInt
    require(maxVendorRows > 0 && maxInputs > 0)
    val dirs = params.getOrElse("inPaths", "").split(",").filter(_.nonEmpty).map(p => new File(p).getCanonicalFile).toVector
    require(dirs.nonEmpty && dirs.forall(_.isDirectory), "Committed input directories required")
    val files = dirs.flatMap(d => Option(d.listFiles()).getOrElse(Array.empty[File]).filter(_.isFile))
    val sjes = SortedJEInputs.oneRead(files.filter(f => f.getName.startsWith("SortedJE") && f.getName.endsWith(".csv")))
    val balances = files.filter(_.getName.matches("LDGR[0-9]{4}\\.csv"))
    require(sjes.nonEmpty && balances.nonEmpty, "Both SJE and posted-balance streams required")
    val opening = params.getOrElse("openingFiles", "").split(",").filter(_.nonEmpty).map(new File(_)).toVector
    require((basis == "opening_plus_movements") == opening.nonEmpty, "Opening basis requires explicit openingFiles, and vice versa")
    val dimensions = Seq("vendorMaster" -> "vendor", "contractDimensions" -> "contract").flatMap {
      case (arg, kind) => params.get(arg).filter(_.nonEmpty).map(p => new File(p) -> kind)
    }
    val selected = (sjes.map(_ -> "movement") ++ balances.map(_ -> "balance") ++ opening.map(_ -> "opening") ++ dimensions)
      .map { case (f, k) => f.getCanonicalFile -> k }.sortBy { case (f, k) => (k, f.getPath) }
    require(selected.size <= maxInputs, s"Input handle bound exceeded: ${selected.size} > $maxInputs")
    require(selected.map(_._1).distinct.size == selected.size, "Duplicate physical input path")
    require(selected.forall(_._1.isFile), "Missing declared input")
    val writers = mutable.ArrayBuffer[PrintWriter]()
    try {
    def output(name: String, header: Seq[String]): PrintWriter = { val p = writer(out, name, header); writers += p; p }
    val manifest = output("input_manifest.csv", Seq("kind", "path", "rows", "bytes", "sha256"))
    val common = Seq("vendor_id") ++ dimensionNames
    val movementOut = output("movement_history.csv", common ++ Seq("amount", "rows"))
    val balanceOut = output("balance_history.csv", common ++ Seq("amount", "rows", "opening_amount"))
    val recon = output("reconciliation.csv", common ++ Seq("basis", "movement", "opening", "balance", "delta", "status", "movement_rows", "balance_rows"))
    val coverage = output("coverage.csv", Seq("vendor_id", "fiscal_year", "dimension", "key", "status"))
    val attrs = output("dimension_attributes.csv", Seq("vendor_id", "dimension", "attribute", "value", "source", "record"))
    val refOut = output("reference_attributes.csv", Seq("table", "key", "attribute", "value", "source", "record"))
    val packets = output("analysis_packets.csv", Seq("vendor_id", "kind", "source", "record", "canonical_record_csv"))
    val relationOut = output("relationships.csv", Seq("vendor_id", "fiscal_year", "event", "currency", "agency", "fund", "commodity", "contract_id", "amount", "rows", "absolute_amount", "absolute_share_within_event_currency", "contract_status", "agency_description", "fund_description"))
    val activity = output("vendor_activity.csv", Seq("vendor_id", "fiscal_year", "side", "event", "rows", "direct_amount", "offset_amount", "negative_direct_rows", "observed_periods", "largest_inactive_month_gap", "period_grain"))
    val movementNames = Seq("po_count", "max_monthly_agencies", "max_monthly_classes", "committed_amount", "annual_distinct_agencies", "annual_distinct_classes", "signed_direct_po_count", "signed_direct_po_amount")
    val balanceNames = Seq("debit_amount", "offset_amount", "ledger_rows", "distinct_accounts", "distinct_periods")
    val featureKey = Seq("vendor_id", "fiscal_year", "cutoff", "feature_version", "attributed", "currency")
    val moveFeatures = output("aim_movements.csv", featureKey ++ movementNames)
    val balFeatures = output("aim_balances.csv", featureKey ++ balanceNames)
    val combined = output("aim_combined.csv", featureKey ++ movementNames ++ balanceNames ++ Seq("has_movements", "has_balances", "eligible_matched_vendor", "vendor_name", "name_status"))
    val refs = mutable.HashMap[(String, String), Vector[String]]()
    val refHeaders = mutable.HashMap[String, Vector[String]]()
    val refFiles = params.get("refPath").toVector.flatMap { p =>
      val d = new File(p); require(d.isDirectory, "Missing reference directory")
      d.listFiles().filter(f => f.isFile && f.getName.startsWith("aRef") && f.getName.endsWith(".txt")).sortBy(_.getName)
    }
    val maxRefBytes = params.getOrElse("maxReferenceBytes", "16777216").toLong
    require(refFiles.map(_.length()).sum <= maxRefBytes, "Reference byte allowance exceeded")
    // Explicit known VA lookup contracts; all other reference attributes remain available by first-column key.
    val refSpec = Map("aRefAgencyCSV.txt" -> (1, 2), "aRefFundCSV.txt" -> (0, 2),
      "aRefObjectCSV.txt" -> (0, 4), "aRefProgramCSV.txt" -> (0, 7), "aRefSourceCSV.txt" -> (0, 5))
    def lookup(table: String, key: String): (String, String) = refs.get((table, key)) match {
      case Some(r) if r.isEmpty => ("", "ambiguous_reference")
      case Some(r) => (r.lift(refSpec.getOrElse(table, (0, 1))._2).getOrElse(""), "matched_current_reference")
      case None => ("", if (refHeaders.contains(table)) "missing" else "table_unavailable")
    }
    val inputs = mutable.ArrayBuffer[Input]()
    var readRows = 0L; var vendorRows = 0; var peakVendorRows = 0; var peakVendorKeys = 0
    var failed = 0L; var vendorCount = 0L
    val spills = new Spills(out, params.getOrElse("spillRows", "50000").toInt)
    val selectedPackets = params.getOrElse("packetVendors", "").split(",").filter(_.nonEmpty).toSet
    var current = ""
    val buckets = mutable.HashMap[Vector[String], Bucket]()
    val mf = mutable.HashMap[Int, Features](); val bf = mutable.HashMap[Int, Features]()
    val dims = mutable.ArrayBuffer[Dimension]()
    val relationships = mutable.HashMap[Vector[String], (Sum, BigDecimal)]()
    val eventAbs = mutable.HashMap[(String, String, String), BigDecimal]()
    val currencies = mutable.HashMap[Int, String]()
    def active(d: Dimension, cutoff: LocalDate): Boolean = {
      val from = d.values.getOrElse(if (d.kind == "vendor") "instEffectDate" else "effective_from", "")
      val to = d.values.getOrElse(if (d.kind == "vendor") "instEffectEndDate" else "effective_to", "")
      (from.isEmpty || !date(from).isAfter(cutoff)) &&
        (to.isEmpty || to == "9999-99-99" || !date(to).isBefore(cutoff))
    }
    def dimension(kind: String, fy: Int, contract: String = ""): (String, String) = {
      val candidates = dims.filter(d => d.kind == kind && (kind != "contract" || d.values("contract_id") == contract) && active(d, LocalDate.of(fy, 6, 30)))
      if (candidates.isEmpty) ("", "missing")
      else if (candidates.size > 1) ("", "ambiguous")
      else {
        val d = candidates.head
        val dated = d.values.getOrElse(if (kind == "vendor") "instEffectDate" else "effective_from", "").nonEmpty
        (d.values.getOrElse(if (kind == "vendor") "instHolderName" else "contract_id", ""), if (dated) "matched_asof" else "undated")
      }
    }
    def closeVendor(): Unit = if (current.nonEmpty) {
      vendorCount += 1
      peakVendorKeys = peakVendorKeys.max(buckets.size + relationships.size + mf.size + bf.size + dims.size)
      val cross = mutable.HashMap[String, Sum]()
      buckets.toVector.sortBy { case (k, _) => key(k) }.foreach { case (k, b) =>
        if (b.movements > 0) movementOut.println(csv(Seq(current) ++ k ++ Seq(b.movement, b.movements)))
        if (b.balances > 0 || b.openings > 0) balanceOut.println(csv(Seq(current) ++ k ++ Seq(b.balance, b.balances, b.opening)))
        val delta = b.balance - b.opening - b.movement
        val ok = delta == 0 && b.balances > 0 && (b.movements > 0 || b.openings > 0)
        if (!ok) failed += 1
        recon.println(csv(Seq(current) ++ k ++ Seq(basis, b.movement, b.opening, b.balance, delta, if (ok) "PASS" else "FAIL", b.movements, b.balances)))
        Seq(("movement", b.movement, b.movements), ("balance", b.balance, b.balances)).foreach { case (side, amt, n) =>
          if (n > 0) cross.getOrElseUpdate(key(Seq(side, fiscalYear(k(13)).toString, k(3), k(4), k(7), k(10))), Sum()).add(amt, n, 0)
        }
      }
      cross.toVector.sortBy(_._1).foreach { case (k, s) => spills.add(k, s.amount, s.rows, 1) }
      (mf.keySet ++ bf.keySet).toVector.sorted.foreach { fy =>
        val m = mf.getOrElse(fy, new Features); val b = bf.getOrElse(fy, new Features)
        val featurePrefix = Seq(current, fy, s"$fy-06-30", "legacy_sign_v1_plus_annual_distinct", attributed(current), currencies(fy))
        if (mf.contains(fy)) moveFeatures.println(csv(featurePrefix ++ m.movementValues))
        if (bf.contains(fy)) balFeatures.println(csv(featurePrefix ++ b.balanceValues))
        val (name, nameStatus) = dimension("vendor", fy)
        combined.println(csv(featurePrefix ++ m.movementValues ++ b.balanceValues ++ Seq(mf.contains(fy), bf.contains(fy), attributed(current) && m.poCount > 0 && bf.contains(fy), name, nameStatus)))
        row(coverage, current, fy, "vendor", current, if (attributed(current)) nameStatus else "unattributed")
        Seq("movement" -> m, "balance" -> b).foreach { case (side, f) =>
          f.events.toVector.sortBy(_._1).foreach { case (event, a) =>
            val annual = a.periods.exists(!_.contains("/"))
            val months = a.periods.filter(_.contains("/")).map { p => (p.takeRight(2).toInt + 5) % 12 }.toVector.sorted
            val gap = if (annual || months.size < 2) "" else months.sliding(2).map(x => x(1) - x(0) - 1).max.toString
            row(activity, current, fy, side, event, a.rows, a.direct, a.offset, a.negative, a.periods.size, gap, if (annual) "annual_or_mixed" else "monthly")
          }
        }
      }
      relationships.toVector.sortBy { case (k, _) => key(k) }.foreach { case (k, (s, absolute)) =>
        val denom = eventAbs((k(0), k(1), k(2)))
        val status = if (k(6).isEmpty || k(6) == "0") "unattributed" else dimension("contract", k(0).toInt, k(6))._2
        relationOut.println(csv(Seq(current) ++ k ++ Seq(s.amount, s.rows, absolute, if (denom == 0) BigDecimal(0) else absolute / denom, status, lookup("aRefAgencyCSV.txt", k(3))._1, lookup("aRefFundCSV.txt", k(4))._1)))
        row(coverage, current, k(0), "contract", k(6), status)
        Seq("agency" -> ("aRefAgencyCSV.txt", k(3)), "fund" -> ("aRefFundCSV.txt", k(4))).foreach {
          case (label, (table, value)) => row(coverage, current, k(0), label, value, lookup(table, value)._2)
        }
      }
      buckets.clear(); mf.clear(); bf.clear(); dims.clear(); relationships.clear(); eventAbs.clear(); currencies.clear(); vendorRows = 0
    }
    try {
      refFiles.foreach { f =>
        val in = new Input(f, "reference", true)
        try {
          refHeaders(f.getName) = in.header
          var r = in.next()
          while (r.nonEmpty) {
            val values = r.get
            require(values.size == in.header.size, s"Reference shape: $f record ${in.count}")
            val id = values(refSpec.getOrElse(f.getName, (0, 1))._1)
            refs((f.getName, id)) = if (refs.contains((f.getName, id))) Vector.empty else values
            in.header.zip(values).foreach { case (h, v) => row(refOut, f.getName, id, h, v, f.getCanonicalPath, in.count) }
            r = in.next()
          }
          row(manifest, "reference", f.getCanonicalPath, in.count, f.length(), in.hash)
        } finally in.close()
      }
      selected.foreach { case (f, kind) =>
        val in = new Input(f, kind, kind != "movement"); inputs += in
        if (Set("balance", "opening")(kind)) require(in.header == ledgerHeader, s"Ledger schema mismatch: $f")
        if (kind == "vendor") require(in.header.contains("instID"), "Vendor dimension requires instID")
        if (kind == "contract") require(Set("vendor_id", "contract_id").subsetOf(in.header.toSet), "Contract dimension requires vendor_id,contract_id")
      }
      case class Head(vendor: String, index: Int, values: Vector[String], ordinal: Long)
      val queue = mutable.PriorityQueue[Head]()(Ordering.by[Head, (String, Int)](h => (h.vendor, h.index)).reverse)
      def advance(i: Int): Unit = inputs(i).next().foreach { raw =>
        val in = inputs(i)
        val values = if (Set("balance", "opening")(in.kind) && raw.size == 31 && raw.last.isEmpty) raw.dropRight(1) else raw
        val expected = if (in.kind == "movement") 39 else in.header.size
        require(values.size == expected, s"Schema mismatch ${in.file}:${in.count}")
        val idIndex = if (in.kind == "vendor") in.header.indexOf("instID") else if (in.kind == "contract") in.header.indexOf("vendor_id") else 0
        val id = values(idIndex)
        require(id.nonEmpty, s"Empty instrument key ${in.file}:${in.count}; use explicit unattributed sentinel")
        in.checkVendor(id); queue.enqueue(Head(id, i, values, in.count))
      }
      inputs.indices.foreach(advance)
      while (queue.nonEmpty) {
        val h = queue.dequeue(); val in = inputs(h.index); val r = h.values
        if (h.vendor != current) { closeVendor(); current = h.vendor }
        vendorRows += 1; readRows += 1; peakVendorRows = peakVendorRows.max(vendorRows)
        require(vendorRows <= maxVendorRows, s"Vendor working-state bound exceeded: $current")
        if (Set("vendor", "contract")(in.kind)) {
          val d = Dimension(in.kind, in.header.zip(r).toMap, in.file.getPath, h.ordinal)
          // Validate dates even when no financial row later matches this dimension.
          Seq("instEffectDate", "instEffectEndDate", "effective_from", "effective_to").foreach { field =>
            d.values.get(field).filter(s => s.nonEmpty && s != "9999-99-99").foreach(date)
          }
          dims += d
          in.header.zip(r).foreach { case (field, value) => row(attrs, current, in.kind, field, value, in.file.getPath, h.ordinal) }
        } else {
          val movement = in.kind == "movement"
          val dimsKey = if (movement) r.slice(9, 23) else r.slice(5, 19)
          val amount = BigDecimal(r(if (movement) 25 else 19))
          val flag = r(if (movement) 31 else 22); require(Set("D", "O")(flag), s"Invalid direction $flag")
          val record = Financial(current, dimsKey, amount, flag, if (movement) r(5) else "POSTED", r(1), in, h.ordinal, r)
          require(!currencies.contains(record.fy) || currencies(record.fy) == dimsKey(10), "AIM vendor-year mixes currencies; partition inference features by currency before running")
          currencies(record.fy) = dimsKey(10)
          val b = buckets.getOrElseUpdate(dimsKey, Bucket())
          in.kind match {
            case "movement" => b.movement += amount; b.movements += 1; mf.getOrElseUpdate(record.fy, new Features).add(record)
            case "balance" => b.balance += amount; b.balances += 1; bf.getOrElseUpdate(record.fy, new Features).add(record)
            case "opening" => b.opening += amount; b.openings += 1
          }
          if (movement && flag == "D") {
            val k = Vector(record.fy.toString, record.event, dimsKey(10), dimsKey(3), dimsKey(4), dimsKey(5), record.contract)
            val (sum, abs) = relationships.getOrElse(k, (Sum(), BigDecimal(0)))
            sum.add(amount, 1, 0); relationships(k) = (sum, abs + amount.abs)
            val ek = (k(0), k(1), k(2)); eventAbs(ek) = eventAbs.getOrElse(ek, BigDecimal(0)) + amount.abs
          }
          if (selectedPackets(current)) row(packets, current, in.kind, in.file.getPath, h.ordinal, csv(r))
        }
        advance(h.index)
      }
      closeVendor(); spills.flush()
      inputs.foreach(in => row(manifest, in.kind, in.file.getPath, in.count, in.file.length(), in.hash))
      val metrics = output("extract_metrics.csv", Seq("metric", "value"))
      Seq("records_read" -> readRows, "vendors" -> vendorCount, "peak_vendor_rows" -> peakVendorRows,
        "peak_vendor_state_keys" -> peakVendorKeys, "peak_buffer_keys" -> spills.peak,
        "spill_chunks" -> spills.chunks, "partial_rows" -> spills.partials, "spill_bytes" -> spills.bytes,
        "reference_bytes" -> refFiles.map(_.length()).sum, "reconciliation_failures" -> failed,
        "elapsed_seconds" -> ((System.nanoTime() - start) / 1e9)).foreach { case (k, v) => row(metrics, k, v) }
      writers.foreach(_.flush()); require(!writers.exists(_.checkError()), "Reporting output write failed")
      require(failed == 0, s"Reporting reconciliation failed at $failed posting keys")
      Files.write(new File(out, "EXTRACT_COMPLETE").toPath, s"${spills.chunks}\n".getBytes(StandardCharsets.UTF_8))
      println(s"RESULT step=5v profile=expanded rc=0 records_read=$readRows vendors=$vendorCount spills=${spills.chunks}")
    } finally { inputs.foreach(_.close()); writers.foreach(_.close()) }
    } finally writers.foreach(_.close())
  }
}
