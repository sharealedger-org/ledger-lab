package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.time.LocalDate
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport}
import scala.collection.mutable

/** Deterministic INPUT preparation only. No accounting, posting or report-time engine. */
object SyntheticReferenceHistory {
  val Version = "reclass_history_v1.0"
  val Baseline = "2003-01-01" // synthetic assumption, not a claim about any Virginia entity
  case class Schema(name: String, keys: Vector[String], fields: Vector[String]) {
    val header: Vector[String] = fields ++ Vector("effective_start", "version_id", "change_id", "synthetic")
  }
  val schemas = Vector(
    Schema("reference_history", Vector("reference_type", "reference_key"), Vector("reference_type", "reference_key", "label", "status")),
    Schema("sal_history", Vector("instrument_id"), Vector("instrument_id", "label", "risk_band", "agency_key")),
    Schema("party_history", Vector("party_id"), Vector("party_id", "name", "sector", "country")),
    Schema("car_history", Vector("instrument_id", "contract_id", "role"), Vector("instrument_id", "contract_id", "role", "party_id")),
    Schema("alias_history", Vector("reference_type", "context_key", "source_label"), Vector("reference_type", "context_key", "source_label", "target_key")),
    Schema("hierarchy_history", Vector("hierarchy_id", "child_type", "child_key"), Vector("hierarchy_id", "child_type", "child_key", "parent_type", "parent_key")))
  private def sha(file: File): String = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath)).map(b => f"${b & 255}%02x").mkString
  private def write(out: File, name: String, header: Seq[String], rows: Seq[Seq[String]]): Unit = {
    val p = ReportingSupport.writer(out, name + ".csv", header)
    try rows.foreach(r => p.println(ReportingSupport.csv(r)))
    finally { p.flush(); val bad = p.checkError(); p.close(); require(!bad, s"Write failed: $name") }
  }
  def generate(out: File, instruments: Int, sourceSha: String): Unit = {
    require(instruments >= 2 && instruments <= 10000, "Bounded preparation: 2..10000 instruments")
    require(sourceSha.matches("[0-9a-f]{64}"), "Generator source SHA-256 required")
    require(!out.exists() && out.mkdirs(), "Use a new output directory")
    val data = schemas.map(s => s.name -> mutable.ArrayBuffer[Vector[String]]()).toMap
    def add(table: String, fields: Seq[String], start: String = Baseline, version: String, change: String = "BASELINE"): Unit = {
      LocalDate.parse(start)
      val s = schemas.find(_.name == table).get
      require(fields.size == s.fields.size)
      data(table) += (fields.toVector ++ Vector(start, version, change, "true"))
    }
    def ref(t: String, k: String, label: String): Unit = add("reference_history", Seq(t, k, label, "ACTIVE"), version = s"REF-$t-$k-v1")
    def edge(h: String, ct: String, ck: String, pt: String, pk: String, start: String = Baseline, change: String = "BASELINE", suffix: String = "v1"): Unit =
      add("hierarchy_history", Seq(h, ct, ck, pt, pk), start, s"EDGE-$h-$ct-$ck-$suffix", change)
    val changes = Vector(
      Vector("C_ATTRIBUTE", "2014-03-03", "2014-04-23", "attribute_only", "INSPIRE_COA_2014"),
      Vector("C_LEAF", "2014-06-23", "2018-07-12", "leaf_parent_late_recorded", "INSPIRE_COA_2014"),
      Vector("C_ANCESTOR", "2015-01-01", "2015-01-01", "ancestor_only", "SYNTHETIC_ASSUMPTION"),
      Vector("C_PARTY", "2014-07-01", "2014-07-01", "party_and_role", "SYNTHETIC_ASSUMPTION"),
      Vector("C_QUIET", "2015-06-26", "2015-06-26", "no_new_business_event", "INSPIRE_COA_2015_SYNTHETIC_DATE"),
      Vector("C_ALIAS", "2015-01-01", "2015-01-01", "contextual_alias_version", "SYNTHETIC_ASSUMPTION"))
    ref("root", "SIM_ROOT", "SIMULATED ALL")
    Seq("SIM_A405", "SIM_A748", "SIM_A792", "SIM_A793").foreach(ref("agency", _, "SIMULATED ADMINISTRATIVE AGENCY"))
    ref("secretarial_area", "SIM_S01", "SIMULATED COMMERCE GROUP")
    ref("secretarial_area", "SIM_S02", "SIMULATED AGRICULTURE GROUP")
    Seq("SIM_FN3", "SIM_FN7").foreach(ref("function", _, "SIMULATED FUNCTION"))
    Seq("SIM_P715", "SIM_P716").foreach(ref("program", _, "SIMULATED PROGRAM"))
    Seq("SIM_SA71501", "SIM_SA71502").foreach(ref("service_area", _, "SIMULATED SERVICE AREA"))
    Seq("SIM_F405", "SIM_F748", "SIM_F405B").foreach(ref("fund", _, "SIMULATED GENERAL FUND"))
    ref("major_object", "SIM_M15", "SIMULATED CONTINUOUS CHARGES")
    ref("object", "SIM_O1535", "SIMULATED RENTAL SERVICES")
    ref("source_class", "SIM_SC5", "SIMULATED REVENUE CLASS")
    ref("source", "SIM_R501", "SIMULATED RECEIPTS")
    add("reference_history", Seq("agency", "SIM_A405", "SIMULATED SERVICE AGENCY", "ACTIVE"), "2014-03-03", "REF-agency-SIM_A405-v2", "C_ATTRIBUTE")
    edge("reporting", "agency", "SIM_A405", "secretarial_area", "SIM_S01")
    edge("reporting", "agency", "SIM_A405", "secretarial_area", "SIM_S02", "2014-06-23", "C_LEAF", "v2")
    edge("reporting", "agency", "SIM_A748", "agency", "SIM_A792")
    edge("reporting", "agency", "SIM_A748", "agency", "SIM_A793", "2015-06-26", "C_QUIET", "v2")
    Seq("SIM_A792", "SIM_A793").foreach(edge("reporting", "agency", _, "secretarial_area", "SIM_S01"))
    Seq("SIM_S01", "SIM_S02").foreach(edge("reporting", "secretarial_area", _, "root", "SIM_ROOT"))
    Seq("SIM_SA71501", "SIM_SA71502").foreach(edge("program", "service_area", _, "program", "SIM_P715"))
    edge("program", "program", "SIM_P715", "function", "SIM_FN7")
    edge("program", "program", "SIM_P715", "function", "SIM_FN3", "2015-01-01", "C_ANCESTOR", "v2")
    edge("program", "program", "SIM_P716", "function", "SIM_FN7")
    Seq("SIM_FN3", "SIM_FN7").foreach(edge("program", "function", _, "root", "SIM_ROOT"))
    edge("object", "object", "SIM_O1535", "major_object", "SIM_M15")
    edge("object", "major_object", "SIM_M15", "root", "SIM_ROOT")
    edge("revenue", "source", "SIM_R501", "source_class", "SIM_SC5")
    edge("revenue", "source_class", "SIM_SC5", "root", "SIM_ROOT")
    for (i <- 1 to instruments) {
      val id = f"SIM_I$i%06d"; val party = f"SIM_P$i%06d"; val contract = f"SIM_C$i%06d"
      ref("instrument", id, s"SIMULATED INSTRUMENT $i")
      add("sal_history", Seq(id, s"SIMULATED INSTRUMENT $i", "LOW", if (i == 2) "SIM_A748" else "SIM_A405"), version = s"SAL-$id-v1")
      add("party_history", Seq(party, s"SIMULATED PARTY $i", "COMMERCIAL", "US"), version = s"PARTY-$party-v1")
      add("car_history", Seq(id, contract, "OWNER", party), version = s"CAR-$id-OWNER-v1")
      edge("reporting", "instrument", id, "agency", if (i == 2) "SIM_A748" else "SIM_A405")
    }
    add("sal_history", Seq("SIM_I000001", "SIMULATED INSTRUMENT 1", "HIGH", "SIM_A405"), "2014-03-03", "SAL-SIM_I000001-v2", "C_ATTRIBUTE")
    add("party_history", Seq("SIM_P000001", "SIMULATED PARTY 1", "PUBLIC", "US"), "2014-07-01", "PARTY-SIM_P000001-v2", "C_PARTY")
    add("car_history", Seq("SIM_I000001", "SIM_C000001", "OWNER", "SIM_P000002"), "2014-07-01", "CAR-SIM_I000001-OWNER-v2", "C_PARTY")
    def alias(context: String, label: String, target: String, start: String = Baseline, version: String, change: String = "BASELINE"): Unit =
      add("alias_history", Seq("fund", context, label, target), start, version, change)
    alias("SIM_A405", "SIMULATED GENERAL", "SIM_F405", version = "ALIAS-405-GENERAL-v1")
    alias("SIM_A405", "SIMULATED GENERAL FUND", "SIM_F405", version = "ALIAS-405-FUND-v1")
    alias("SIM_A748", "SIMULATED GENERAL FUND", "SIM_F748", version = "ALIAS-748-FUND-v1")
    alias("SIM_A405", "SIMULATED GENERAL FUND", "SIM_F405B", "2015-01-01", "ALIAS-405-FUND-v2", "C_ALIAS")
    schemas.foreach { s =>
      val order = s.keys.map(s.header.indexOf) :+ s.header.indexOf("effective_start")
      val sorted = data(s.name).sortBy(r => order.map(r).mkString("\u0000")).toVector
      write(out, s.name, s.header, sorted)
    }
    write(out, "change_events", Vector("change_id", "effective_date", "recorded_date", "scenario", "inspiration_id", "synthetic"), changes.map(_ :+ "true"))
    write(out, "activity_context", Vector("instrument_id", "last_business_event_date", "reference_change_id", "new_activity_on_change_date", "synthetic"),
      Vector(Vector("SIM_I000002", "2013-07-01", "C_QUIET", "false", "true")))
    write(out, "generation_config", Vector("parameter", "value"), Vector(
      Vector("generator_version", Version), Vector("generator_source_sha256", sourceSha), Vector("instruments", instruments.toString),
      Vector("baseline_date", Baseline), Vector("run_date", "2015-07-01"), Vector("date_semantics", "greatest_start_not_after_lookup_date"),
      Vector("external_sources_required", "false"), Vector("generated_data_classification", "synthetic_source_informed")))
    writeCases(out)
    write(out, "inspiration_register", Vector("inspiration_id", "url", "use", "required_for_generation"), Vector(
      Vector("INSPIRE_COA_2014", "https://dpb.virginia.gov/forms/20140717-1/CHARTOFACCOUNTSCHANGES6-6-13through6-25-14-updated2018-07-12.pdf", "Dated attribute/parent changes and a later recorded correction; SIM labels/keys/baseline are invented", "false"),
      Vector("INSPIRE_COA_2015_SYNTHETIC_DATE", "https://dpb.virginia.gov/forms/20140717-1/FINAL--CHARTOFACCOUNTSCHANGES6-26-2014through6-30-2015.pdf", "Responsible-parent move; adopting action date as synthetic effective date is an explicit scenario assumption", "false"),
      Vector("SYNTHETIC_ASSUMPTION", "", "Population/keys/party-role/risk/ancestor/alias cases and all ungrounded dates are invented", "false")))
    write(out, "schema_contract", Vector("table", "business_key_columns", "version_date_column", "version_identity", "semantics"),
      schemas.map(s => Vector(s.name, s.keys.mkString("|"), "effective_start", "version_id", "greatest_start_not_after_lookup_date")))
    write(out, "assumptions", Vector("assumption", "value", "status"), Vector(
      Vector("population", s"$instruments instruments and $instruments parties", "synthetic_configurable_not_estimated_from_VA"),
      Vector("baseline", Baseline, "invented_start_for_all_generated_baselines"),
      Vector("run_date", "2015-07-01", "supplied_lookup_operand_not_machine_clock_or_knowledge_cutoff"),
      Vector("date_endpoints", "start_inclusive_later_start_supersedes", "no_explicit_end_date_contract_claim"),
      Vector("source_data_dependency", "none", "URLs_inform_design_only_access_not_required"),
      Vector("VA_crosswalk", "none", "SIM_keys_do_not_resolve_real_VA_surrogates"),
      Vector("quiet_parent_move_date", "2015-06-26", "reported_action_date_adopted_as_synthetic_effective_date"),
      Vector("historical_knowledge", "all_pack_versions_available", "recorded_date_retained_but_not_a_snapshot_filter")))
    Files.write(new File(out, "README.txt").toPath, (
      "Synthetic reclassification reference histories v1\n" +
      "All records, keys, names, baseline starts and scenario assignments are synthetic.\n" +
      "Virginia publications informed selected change patterns/dates; they are optional provenance, not inputs.\n" +
      "No network access, downloaded reference publication, random state or system date is used.\n" +
      "Regenerate: bash scripts/generate_reclass_history.sh --out NEW_ABSOLUTE_PATH --instruments " + instruments + "\n" +
      "Schema: schema_contract.csv. Dates: ISO yyyy-MM-dd; sort business keys then effective_start.\n" +
      "Lookup: fixed AS_OF, supplied RUN_DATE or EVENT_DATE; greatest start <= selected date.\n" +
      "Recorded dates describe change arrival, not an implicit whole-database or knowledge snapshot.\n" +
      "Main histories are start-only; explicit end-date/gap semantics require a separate target contract.\n" +
      "change_events.csv records change identity, effective and recorded dates.\n" +
      "lookup_cases.csv and hierarchy_cases.csv contain manually declared fixture expectations.\n" +
      "negative/ contains intentionally invalid hierarchy variants; never load these as positive inputs.\n" +
      "activity_context.csv marks an instrument requiring evaluation despite no new business activity.\n" +
      "This pack prepares inputs; it does not generate accounting/reclass journals or change a financial ledger.\n" +
      "Existing VA corpus keys, ambiguous real-name mappings and Gold remain untouched.\n" +
      "GENERATION_COMPLETE and manifest.csv identify a complete reproducible pack.\n").getBytes(StandardCharsets.UTF_8))
    val dir = new File(out, "negative"); require(dir.mkdir())
    val hierarchy = schemas.find(_.name == "hierarchy_history").get
    val hrows = data(hierarchy.name).toVector
    write(dir, "duplicate_start", hierarchy.header, hrows :+ hrows.head.updated(hierarchy.header.indexOf("version_id"), "NEG-DUPLICATE"))
    write(dir, "orphan", hierarchy.header, hrows.updated(0, hrows.head.updated(hierarchy.header.indexOf("parent_key"), "SIM_MISSING_PARENT")))
    write(dir, "cycle", hierarchy.header, hrows :+ Vector("reporting", "root", "SIM_ROOT", "agency", "SIM_A405", Baseline, "NEG-CYCLE", "BASELINE", "true"))
    // Oracles above are declared independently of the validator's selection algorithm.
    SyntheticReferenceHistoryCheck.validate(out)
    val files = (out.listFiles().filter(_.isFile).toVector ++ dir.listFiles().toVector).sortBy(_.getPath)
    write(out, "manifest", Vector("file", "bytes", "sha256"), files.map(f => Vector(out.toPath.relativize(f.toPath).toString, f.length().toString, sha(f))))
    Files.write(new File(out, "GENERATION_COMPLETE").toPath, (Version + "\n").getBytes(StandardCharsets.UTF_8))
    println(s"RESULT synthetic_reference_history version=$Version instruments=$instruments files=${files.size} external_sources_required=false")
  }
  private def writeCases(out: File): Unit = {
    val h = Vector("case_id", "table", "business_key", "date_mode", "as_of_date", "event_date", "run_date", "value_field", "expected_version_id", "expected_value")
    def c(id: String, table: String, key: String, day: String, field: String, v: String, value: String, mode: String = "AS_OF"): Vector[String] =
      Vector(id, table, key, mode, if (mode == "AS_OF") day else "", if (mode == "EVENT_DATE") day else "2013-07-01", if (mode == "RUN_DATE") day else "2015-07-01", field, v, value)
    write(out, "lookup_cases", h, Vector(
      c("before_baseline", "reference_history", "agency|SIM_A405", "2002-12-31", "label", "NOT_FOUND", ""),
      c("attribute_before", "reference_history", "agency|SIM_A405", "2014-03-02", "label", "REF-agency-SIM_A405-v1", "SIMULATED ADMINISTRATIVE AGENCY"),
      c("attribute_on", "reference_history", "agency|SIM_A405", "2014-03-03", "label", "REF-agency-SIM_A405-v2", "SIMULATED SERVICE AGENCY"),
      c("attribute_after", "reference_history", "agency|SIM_A405", "2014-03-04", "label", "REF-agency-SIM_A405-v2", "SIMULATED SERVICE AGENCY"),
      c("event_date_switch", "reference_history", "agency|SIM_A405", "2013-07-01", "label", "REF-agency-SIM_A405-v1", "SIMULATED ADMINISTRATIVE AGENCY", "EVENT_DATE"),
      c("supplied_run_date", "reference_history", "agency|SIM_A405", "2015-07-01", "label", "REF-agency-SIM_A405-v2", "SIMULATED SERVICE AGENCY", "RUN_DATE"),
      c("sal_change", "sal_history", "SIM_I000001", "2014-03-03", "risk_band", "SAL-SIM_I000001-v2", "HIGH"),
      c("party_change", "party_history", "SIM_P000001", "2014-07-01", "sector", "PARTY-SIM_P000001-v2", "PUBLIC"),
      c("car_before", "car_history", "SIM_I000001|SIM_C000001|OWNER", "2014-06-30", "party_id", "CAR-SIM_I000001-OWNER-v1", "SIM_P000001"),
      c("car_on", "car_history", "SIM_I000001|SIM_C000001|OWNER", "2014-07-01", "party_id", "CAR-SIM_I000001-OWNER-v2", "SIM_P000002"),
      c("alias_many_to_one", "alias_history", "fund|SIM_A405|SIMULATED GENERAL", "2015-01-01", "target_key", "ALIAS-405-GENERAL-v1", "SIM_F405"),
      c("alias_context", "alias_history", "fund|SIM_A748|SIMULATED GENERAL FUND", "2015-01-01", "target_key", "ALIAS-748-FUND-v1", "SIM_F748"),
      c("alias_dated_before", "alias_history", "fund|SIM_A405|SIMULATED GENERAL FUND", "2014-12-31", "target_key", "ALIAS-405-FUND-v1", "SIM_F405"),
      c("alias_dated_on", "alias_history", "fund|SIM_A405|SIMULATED GENERAL FUND", "2015-01-01", "target_key", "ALIAS-405-FUND-v2", "SIM_F405B")))
    val ph = Vector("case_id", "hierarchy_id", "child_type", "child_key", "lookup_date", "expected_path")
    write(out, "hierarchy_cases", ph, Vector(
      Vector("leaf_before", "reporting", "agency", "SIM_A405", "2014-06-22", "agency:SIM_A405>secretarial_area:SIM_S01>root:SIM_ROOT"),
      Vector("leaf_on", "reporting", "agency", "SIM_A405", "2014-06-23", "agency:SIM_A405>secretarial_area:SIM_S02>root:SIM_ROOT"),
      Vector("leaf_after", "reporting", "agency", "SIM_A405", "2014-06-24", "agency:SIM_A405>secretarial_area:SIM_S02>root:SIM_ROOT"),
      Vector("ancestor_before", "program", "service_area", "SIM_SA71501", "2014-12-31", "service_area:SIM_SA71501>program:SIM_P715>function:SIM_FN7>root:SIM_ROOT"),
      Vector("ancestor_on", "program", "service_area", "SIM_SA71501", "2015-01-01", "service_area:SIM_SA71501>program:SIM_P715>function:SIM_FN3>root:SIM_ROOT"),
      Vector("quiet_instrument", "reporting", "instrument", "SIM_I000002", "2015-06-26", "instrument:SIM_I000002>agency:SIM_A748>agency:SIM_A793>secretarial_area:SIM_S01>root:SIM_ROOT")))
  }
  def main(args: Array[String]): Unit = {
    val p = LedgerApp.parseArgs(args)
    generate(new File(p("out")), p.getOrElse("instruments", "32").toInt, p("sourceSha256"))
  }
}
