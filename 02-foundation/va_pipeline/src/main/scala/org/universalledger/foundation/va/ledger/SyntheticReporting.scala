package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter}
import java.time.{LocalDate, YearMonth}
import scala.collection.mutable
import scala.util.Try

/** Aggregation-only synthetic and enriched-master cuts inside the current-vendor 5v boundary. */
final class SyntheticReporting(params: Map[String, String], out: File, manifest: PrintWriter) extends AutoCloseable {
  import ReportingSupport._
  val inputs = new DatedSyntheticInputs(new File(params("syntheticHistories")), manifest)
  val asOf = LocalDate.parse(params.getOrElse("syntheticAsOf", "2014-06-30")).toString
  val runDate = LocalDate.parse(params("syntheticRunDate")).toString
  val capacity = params.getOrElse("syntheticAggregateKeys", math.min(params.getOrElse("aggregateKeys", "50000").toInt, 10000).toString).toInt
  val fanIn = params.getOrElse("mergeFanIn", "16").toInt
  private val raw = new ReportingSums(new File(out, "synthetic_raw_partials"), capacity, fanIn)
  private val temporal = new ReportingSums(new File(out, "synthetic_cut_partials"), capacity, fanIn)
  private val master = new ReportingSums(new File(out, "master_cut_partials"), capacity, fanIn)
  private val summary = new ReportingSums(new File(out, "synthetic_summary_partials"), capacity, fanIn)
  private val masterSummary = new ReportingSums(new File(out, "master_summary_partials"), capacity, fanIn)
  private val writers = mutable.ArrayBuffer[PrintWriter]()
  private def output(name: String, head: Seq[String]): PrintWriter = { val p = writer(out, name, head); writers += p; p }
  private val amounts = Seq("amount", "rows", "direct_amount", "absolute_direct_amount", "direct_rows")
  private val postingContext = Seq("ledger", "journal_type", "book", "source_project", "source_product", "alt_account", "source_currency_type", "target_currency", "target_currency_type")
  private val temporalOut = output("vendor_temporal_cuts.csv", Seq("vendor_id", "side", "event", "fiscal_year", "period", "currency", "direction", "source_agency", "source_fund", "account", "date_mode", "date_basis", "profile_status", "template_instrument_id", "synthetic_agency", "synthetic_risk", "synthetic_sector", "synthetic_owner_party", "synthetic_hierarchy_path", "selected_versions") ++ postingContext ++ amounts)
  private val masterOut = output("vendor_attribute_totals.csv", Seq("vendor_id", "side", "fiscal_year", "account", "currency", "direction", "attribute", "attribute_value", "attribute_status", "ledger", "journal_type", "book") ++ amounts)
  private val summaryOut = output("vendor_temporal_summary.csv", Seq("side", "event", "fiscal_year", "currency", "direction", "date_mode", "profile_status", "synthetic_agency", "synthetic_risk", "synthetic_sector", "ledger", "journal_type", "book") ++ amounts)
  private val masterSummaryOut = output("vendor_attribute_summary.csv", Seq("side", "fiscal_year", "currency", "direction", "attribute", "attribute_value", "attribute_status", "ledger", "journal_type", "book") ++ amounts)
  private val assignmentOut = output("synthetic_vendor_coverage.csv", Seq("vendor_id", "attribution", "template_instrument_id", "assignment_rule", "financial_rows", "identity_rekeyed"))
  private val controls = output("synthetic_conservation.csv", Seq("vendor_id", "view", "side", "currency", "mode_or_attribute", "input_amount", "output_amount", "input_rows", "output_rows", "input_direct_amount", "output_direct_amount", "input_absolute_direct_amount", "output_absolute_direct_amount", "status"))
  private val config = output("synthetic_view_config.csv", Seq("parameter", "value"))
  Seq("assignment" -> "prepared_sorted_assignment_stream", "as_of_date" -> asOf, "run_date" -> runDate,
    "profile_namespace" -> "vendor_scoped_synthetic_not_legal_identity", "balance_event_date" -> "period_cutoff",
    "canonical_identity_changed" -> "false", "recorded_date_is_knowledge_filter" -> "false",
    "partial_capacity_keys" -> capacity.toString).foreach { case (k, v) => row(config, k, v) }
  try inputs.validateCases(out) catch { case scala.util.control.NonFatal(e) => writers.foreach(_.close()); throw e }
  require(params.contains("syntheticAssignments"), "A prepared sorted assignment stream is required")
  val attributes = Vector("instTypeID", "instSizeTier", "instGeoRegion", "instContractType", "instPaymentChannel", "instCounterpartySector", "instExposureType", "instFXCurrency", "instFederalAgency", "instNIGPClass", "instRiskWeightProxy", "instMaturityProxy", "instFederalCFDA")
  private val modes = Vector("EVENT_DATE", "AS_OF", "RUN_DATE")
  private val zero = Vector.fill(5)(BigDecimal(0))
  private def plus(a: Vector[BigDecimal], b: Vector[BigDecimal]): Vector[BigDecimal] = a.zip(b).map { case (x, y) => x + y }
  private val dateCache = mutable.HashMap[(String, String, String), (String, String)]()
  var financialRows = 0L; var outputRows = 0L; var controlRows = 0L; var missingDateRows = 0L
  var spillBytes = 0L; var mergePasses = 0L
  private var vendorRawRows = 0L
  def periodCutoff(period: String): LocalDate = {
    val p = period.split("/"); val fy = fiscalYear(period)
    if (p.length == 1) LocalDate.of(fy, 6, 30)
    else {
      val month = p(1).toInt
      val year = if (p(0).startsWith("00")) fy - (if (month >= 7) 1 else 0) else p(0).toInt
      YearMonth.of(year, month).atEndOfMonth()
    }
  }
  private def eventDate(rawDate: String, period: String, side: String): (String, String) = {
    if (dateCache.size >= 8192) dateCache.clear()
    dateCache.getOrElseUpdate((rawDate, period, side), {
      if (side != "movement") (periodCutoff(period).toString, "posted_period_cutoff")
      else {
        val s = rawDate.trim
        val parsed = Try {
          val d = s.takeWhile(c => c != ' ' && c != 'T').replace('/', '-').split("-")
          require(d.length == 3)
          if (d(0).length == 4) LocalDate.of(d(0).toInt, d(1).toInt, d(2).toInt)
          else if (d(2).length == 4) LocalDate.of(d(2).toInt, d(0).toInt, d(1).toInt)
          else if (s.contains("-") && d(2).length == 2) LocalDate.of(2000 + d(2).toInt, d(0).toInt, d(1).toInt)
          else throw new IllegalArgumentException("Ambiguous date")
        }.toOption
        parsed.map(d => (d.toString, "source_transaction_date")).getOrElse((periodCutoff(period).toString, "missing_transaction_date_period_cutoff"))
      }
    })
  }
  def add(r: ReportingPass.Financial, side: String): Unit = {
    val period = r.dimensions(13)
    val (day, basis) = eventDate(if (side == "movement") r.raw(24) else "", period, side)
    if (basis.startsWith("missing")) missingDateRows += 1
    val d = if (r.flag == "D") r.amount else BigDecimal(0)
    raw.add(Vector(side, r.event, r.fy.toString, period, r.dimensions(9), r.flag, r.dimensions(3), r.dimensions(4), r.dimensions(7), basis, inputs.segment(day)) ++ Vector(0, 1, 2, 5, 6, 8, 10, 11, 12).map(r.dimensions),
      r.amount, 1, d, d.abs, if (r.flag == "D") BigDecimal(1) else BigDecimal(0))
    vendorRawRows += 1; financialRows += 1
  }
  def finishVendor(vendor: String, attribute: (Int, String) => (String, String), assignment: Option[String]): Unit = {
    if (vendorRawRows == 0) return
    val assigned = if (!attributed(vendor)) { require(assignment.isEmpty); "" } else {
      require(assignment.exists(inputs.instruments.contains), s"Missing/invalid prepared synthetic assignment: $vendor")
      assignment.get
    }
    row(assignmentOut, vendor, if (attributed(vendor)) "attributed" else "unattributed", assigned,
      if (assigned.isEmpty) "no_synthetic_party_for_unattributed" else "prepared_sorted_assignment_stream", vendorRawRows, false)
    val expected = mutable.HashMap[(String, String), Vector[BigDecimal]]()
    val actual = mutable.HashMap[(String, String, String, String), Vector[BigDecimal]]()
    raw.foreach { (k, v) =>
      val sk = (k(0), k(4)); expected(sk) = plus(expected.getOrElse(sk, zero), v)
      modes.foreach { mode =>
        val (segment, basis) = mode match {
          case "EVENT_DATE" => (k(10), k(9))
          case "AS_OF" => (inputs.segment(asOf), "fixed_as_of:" + asOf)
          case "RUN_DATE" => (inputs.segment(runDate), "supplied_run_date:" + runDate)
        }
        val profile = if (assigned.isEmpty) Vector("unattributed", "", "", "", "", "", "", "") else inputs.cachedProfile(assigned, segment)
        val scoped = if (profile(5).isEmpty) profile else profile.updated(5, vendor + "::" + profile(5))
        temporal.add(k.take(9) ++ Vector(mode, basis) ++ scoped ++ k.drop(11), v: _*)
      }
      attributes.foreach { field =>
        val (value, status) = attribute(k(2).toInt, field)
        master.add(Vector(k(0), k(2), k(8), k(4), k(5), field, value, status) ++ k.slice(11, 14), v: _*)
      }
    }
    temporal.foreach { (k, v) =>
      temporalOut.println(csv(Seq(vendor) ++ k ++ v)); outputRows += 1
      summary.add(Vector(k(0), k(1), k(2), k(4), k(5), k(9), k(11), k(13), k(14), k(15)) ++ k.slice(19, 22), v: _*)
      val ak = ("temporal", k(0), k(4), k(9)); actual(ak) = plus(actual.getOrElse(ak, zero), v)
    }
    master.foreach { (k, v) =>
      masterOut.println(csv(Seq(vendor) ++ k ++ v)); outputRows += 1
      masterSummary.add(Vector(k(0), k(1), k(3), k(4), k(5), k(6), k(7)) ++ k.slice(8, 11), v: _*)
      val ak = ("master_attribute", k(0), k(3), k(5)); actual(ak) = plus(actual.getOrElse(ak, zero), v)
    }
    expected.toVector.sortBy(_._1).foreach { case ((side, currency), v) =>
      (modes.map("temporal" -> _) ++ attributes.map("master_attribute" -> _)).foreach { case (family, variant) =>
        val a = actual.getOrElse((family, side, currency, variant), zero); val ok = v == a
        row(controls, vendor, family, side, currency, variant, v(0), a(0), v(1), a(1), v(2), a(2), v(3), a(3), if (ok) "PASS" else "FAIL")
        controlRows += 1; require(ok, s"Synthetic financial conservation failed: $vendor $side $variant")
      }
    }
    vendorRawRows = 0
  }
  def close(): Unit = {
    require(vendorRawRows == 0, "Unflushed synthetic vendor")
    summary.foreach((k, v) => summaryOut.println(csv(k ++ v)))
    masterSummary.foreach((k, v) => masterSummaryOut.println(csv(k ++ v)))
    spillBytes = raw.bytes + temporal.bytes + master.bytes + summary.bytes + masterSummary.bytes
    mergePasses = raw.passes + temporal.passes + master.passes + summary.passes + masterSummary.passes
    raw.close(); temporal.close(); master.close(); summary.close(); masterSummary.close()
    val m = output("synthetic_metrics.csv", Seq("metric", "value"))
    Seq("financial_rows" -> financialRows, "output_rows" -> outputRows, "conservation_checks" -> controlRows,
      "missing_transaction_date_rows" -> missingDateRows, "partial_spill_bytes" -> spillBytes, "partial_merge_passes" -> mergePasses,
      "peak_partial_keys" -> Seq(raw.peak, temporal.peak, master.peak, summary.peak, masterSummary.peak).max, "partial_capacity_keys" -> capacity).foreach { case (k, v) => row(m, k, v) }
    writers.foreach(_.flush()); val bad = writers.exists(_.checkError()); writers.foreach(_.close()); require(!bad, "Synthetic view output write failed")
  }
  def abort(): Unit = writers.foreach(_.close())
}
