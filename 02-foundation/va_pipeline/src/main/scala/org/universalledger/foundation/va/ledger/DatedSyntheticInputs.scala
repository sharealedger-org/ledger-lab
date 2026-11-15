package org.universalledger.foundation.va.ledger

import java.io.File
import java.time.LocalDate
import org.universalledger.foundation.va.corpus.SyntheticReferenceHistory
import scala.collection.mutable

/** Small, explicitly bounded scenario references, not a corpus-wide entity registry. */
final class DatedSyntheticInputs(dir: File, manifest: java.io.PrintWriter) {
  import ReportingSupport._
  type Row = Map[String, String]
  require(new File(dir, "GENERATION_COMPLETE").isFile, "Incomplete synthetic history pack")
  val files = SyntheticReferenceHistory.schemas.map(s => new File(dir, s.name + ".csv"))
  require(files.forall(_.isFile) && files.map(_.length()).sum <= 8L * 1024 * 1024, "Synthetic template byte bound exceeded")
  private var total = 0
  val tables: Map[String, Vector[Row]] = SyntheticReferenceHistory.schemas.map { schema =>
    val in = new Input(new File(dir, schema.name + ".csv"), "synthetic_history", true)
    val rows = mutable.ArrayBuffer[Row]()
    try {
      require(in.header == schema.header, s"Synthetic schema mismatch: ${in.file}")
      var next = in.next()
      while (next.nonEmpty) {
        total += 1; require(total <= 10000, "Synthetic template row bound exceeded")
        val r = next.get; require(r.size == schema.header.size)
        val row = schema.header.zip(r).toMap
        require(row("synthetic") == "true"); LocalDate.parse(row("effective_start")); rows += row; next = in.next()
      }
      ReportingSupport.row(manifest, "synthetic_history", in.file.getCanonicalPath, in.count, in.file.length(), in.hash)
    } finally in.close()
    schema.name -> rows.toVector
  }.toMap
  private val indices = SyntheticReferenceHistory.schemas.map { schema =>
    val groups = tables(schema.name).groupBy(r => schema.keys.map(r).mkString("|"))
    groups.foreach { case (_, rs) => require(rs.map(_("effective_start")).distinct.size == rs.size, "Duplicate synthetic start") }
    schema.name -> groups.map { case (k, rs) => k -> rs.sortBy(_("effective_start")) }
  }.toMap
  val boundaries: Vector[String] = tables.values.flatten.map(_("effective_start")).toVector.distinct.sorted
  require(boundaries.size <= 128)
  def segment(day: String): String = boundaries.takeWhile(_ <= day).lastOption.getOrElse("before_baseline")
  private val profiles = mutable.HashMap[(String, String), Vector[String]]()
  def cachedProfile(template: String, segment: String): Vector[String] = profiles.getOrElseUpdate((template, segment), {
    require(profiles.size < 32768, "Synthetic profile cache allowance exceeded")
    if (segment == "before_baseline") Vector("before_baseline", template, "", "", "", "", "", "") else profile(template, segment)
  })
  val instruments: Vector[String] = tables("sal_history").map(_("instrument_id")).distinct.sorted
  require(instruments.nonEmpty && instruments.size <= 1000, "Bounded synthetic scenarios required")
  private val contracts = tables("car_history").filter(_("role") == "OWNER").groupBy(_("instrument_id")).map { case (k, rs) =>
    require(rs.map(_("contract_id")).distinct.size == 1, "One synthetic owner contract per template required")
    k -> rs.head("contract_id")
  }
  def select(table: String, businessKey: String, day: String): Option[Row] =
    indices(table).getOrElse(businessKey, Vector.empty).filter(_("effective_start") <= day).lastOption
  def path(hierarchy: String, typ: String, key: String, day: String): (String, String) = {
    val seen = mutable.HashSet[String](); val nodes = mutable.ArrayBuffer[String](); val versions = mutable.ArrayBuffer[String]()
    var t = typ; var k = key; var done = false
    while (!done) {
      require(seen.size < 64 && seen.add(t + ":" + k), "Synthetic hierarchy cycle/depth bound")
      require(select("reference_history", t + "|" + k, day).nonEmpty, "Synthetic hierarchy orphan")
      nodes += t + ":" + k
      select("hierarchy_history", s"$hierarchy|$t|$k", day) match {
        case Some(r) => versions += r("version_id"); t = r("parent_type"); k = r("parent_key")
        case None => require(t == "root", "Unrooted synthetic hierarchy"); done = true
      }
    }
    (nodes.mkString(">"), versions.mkString(";"))
  }
  // Vector columns: status, template, agency, risk, sector, owner-party-template, hierarchy path, version lineage.
  def profile(template: String, day: String): Vector[String] = select("sal_history", template, day) match {
    case None => Vector("before_baseline", template, "", "", "", "", "", "")
    case Some(sal) =>
      val car = select("car_history", s"$template|${contracts(template)}|OWNER", day).getOrElse(sys.error("Missing synthetic CAR"))
      val party = select("party_history", car("party_id"), day).getOrElse(sys.error("Missing synthetic party"))
      val agency = select("reference_history", "agency|" + sal("agency_key"), day).getOrElse(sys.error("Missing synthetic agency"))
      val (p, versions) = path("reporting", "instrument", template, day)
      Vector("synthetic_matched", template, sal("agency_key"), sal("risk_band"), party("sector"), car("party_id"), p,
        Seq(sal("version_id"), car("version_id"), party("version_id"), agency("version_id"), versions).mkString(";"))
  }
  def validateCases(out: File): Unit = {
    def input(name: String): Input = new Input(new File(dir, name + ".csv"), "synthetic_expectations", true)
    val results = writer(out, "synthetic_validation.csv", Seq("case_id", "family", "expected", "actual", "status"))
    try {
      Seq("lookup_cases", "hierarchy_cases").foreach { name =>
        val in = input(name)
        try {
          var next = in.next()
          while (next.nonEmpty) {
            require(next.get.size == in.header.size); val r = in.header.zip(next.get).toMap
            val (expected, actual) = if (name == "lookup_cases") {
              val dateKey = Map("AS_OF" -> "as_of_date", "EVENT_DATE" -> "event_date", "RUN_DATE" -> "run_date")(r("date_mode"))
              val day = r(dateKey); LocalDate.parse(day)
              val selected = select(r("table"), r("business_key"), day)
              (r("expected_version_id") + "|" + r("expected_value"), selected.map(x => x("version_id") + "|" + x(r("value_field"))).getOrElse("NOT_FOUND|"))
            } else (r("expected_path"), path(r("hierarchy_id"), r("child_type"), r("child_key"), r("lookup_date"))._1)
            row(results, r("case_id"), name, expected, actual, if (expected == actual) "PASS" else "FAIL")
            require(expected == actual, s"Synthetic expectation failed: ${r("case_id")}")
            next = in.next()
          }
          row(manifest, "synthetic_expectations", in.file.getCanonicalPath, in.count, in.file.length(), in.hash)
        } finally in.close()
      }
    } finally { results.flush(); val bad = results.checkError(); results.close(); require(!bad) }
  }
}
