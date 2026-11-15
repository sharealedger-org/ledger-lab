package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport}
import scala.collection.mutable

/** Bounded synthetic reference INPUT preparation; never selects a real historical crosswalk. */
object SyntheticReferenceBundle {
  val specs = Vector(("aRefAgencyCSV.txt", 1, 2), ("aRefFundCSV.txt", 0, 2),
    ("aRefObjectCSV.txt", 0, 4), ("aRefProgramCSV.txt", 0, 7), ("aRefSourceCSV.txt", 0, 5))
  def prepare(in: File, out: File): Unit = {
    require(!out.exists() && out.mkdirs(), "Use a new bundle directory")
    val sourceFiles = specs.map(s => new File(in, s._1))
    require(sourceFiles.forall(_.isFile) && sourceFiles.map(_.length()).sum <= 16L * 1024 * 1024, "Bounded references required")
    val lineage = ReportingSupport.writer(out, "synthetic_reference_crosswalk.csv", Seq("table", "source_record", "source_surrogate", "source_record_csv", "normalized_name", "synthetic_key", "selection", "distinct_payloads", "synthetic"))
    val manifest = ReportingSupport.writer(out, "source_manifest.csv", Seq("path", "rows", "bytes", "sha256", "generated_names", "nonunique_names"))
    try specs.foreach { case (name, ki, ni) =>
      val file = new File(in, name); val input = new ReportingSupport.Input(file, "reference", true)
      try {
        val rows = mutable.ArrayBuffer[(Long, Vector[String])]()
        var next = input.next()
        while (next.nonEmpty) {
          require(rows.size < 100000 && next.get.size == input.header.size, s"Reference shape/row bound: $file")
          rows += input.count -> next.get.map(_.trim); next = input.next()
        }
        val groups = rows.groupBy(_._2(ni).toUpperCase(java.util.Locale.ROOT)).toVector.sortBy(_._1)
        val selectedKeys = mutable.HashSet[String]()
        val p = ReportingSupport.writer(out, name, input.header)
        try groups.zipWithIndex.foreach { case ((normalized, candidates), index) =>
          require(normalized.nonEmpty, s"Blank reference name: $name")
          val payloads = candidates.map { case (_, r) => ReportingSupport.csv(r.zipWithIndex.filter(_._2 != ki).map(_._1)) }.distinct
          val chosenPayload = payloads.sorted.head
          val representative = candidates.filter { case (_, r) => ReportingSupport.csv(r.zipWithIndex.filter(_._2 != ki).map(_._1)) == chosenPayload }
            .maxBy { case (_, r) => (BigInt(r(ki)), r(ki)) }._2
          val syntheticKey = representative(ki)
          require(selectedKeys.add(syntheticKey), s"Selected reference key collision: $name $syntheticKey")
          p.println(ReportingSupport.csv(representative.updated(ki, syntheticKey)))
          candidates.foreach { case (ordinal, r) =>
            ReportingSupport.row(lineage, name, ordinal, r(ki), ReportingSupport.csv(r), normalized, syntheticKey,
              if (payloads.size == 1) "coalesced_identical_payload" else "synthetic_lexical_representative_not_real_resolution", payloads.size, true)
          }
        } finally { p.flush(); val bad = p.checkError(); p.close(); require(!bad) }
        ReportingSupport.row(manifest, file.getCanonicalPath, input.count, file.length(), input.hash, groups.size, groups.count(_._2.size > 1))
      } finally input.close()
    } finally Seq(lineage, manifest).foreach { p => p.flush(); val bad = p.checkError(); p.close(); require(!bad) }
    Files.write(new File(out, "SYNTHETIC_REFERENCE_BUNDLE").toPath,
      "synthetic_name_equivalence_v1\nNot a recovered real VA crosswalk. Unambiguous original keys are preserved.\nAttribute/context conflicts use the lexically first non-surrogate CSV payload, then the largest numeric source surrogate; every candidate is recorded.\n".getBytes(StandardCharsets.UTF_8))
  }
  def prepareMaster(file: File, out: File): Unit = {
    require(!out.exists() && file.length() <= 16L * 1024 * 1024)
    val in = new ReportingSupport.Input(file, "vendor_master", true)
    val idColumn = in.header.indexOf("instID"); require(idColumn >= 0)
    val rows = mutable.ArrayBuffer[Vector[String]]()
    val audit = ReportingSupport.writer(out.getParentFile, "vendor_id_projection.csv", Seq("source_record", "original_id", "canonical_id", "contract", "financial_id_changed"))
    try {
      var next = in.next()
      while (next.nonEmpty) {
        require(rows.size < 100000 && next.get.size == in.header.size)
        val r = next.get
        val canonical = try r(idColumn).toLong.formatted("%010d") catch { case _: NumberFormatException => r(idColumn) }
        require(canonical.nonEmpty)
        rows += r.updated(idColumn, canonical)
        ReportingSupport.row(audit, in.count, r(idColumn), canonical, "existing_APA_vendor_ID_projection", false)
        next = in.next()
      }
      val p = ReportingSupport.writer(out.getParentFile, out.getName, in.header)
      try rows.sortBy(r => (r(idColumn), ReportingSupport.csv(r))).foreach(r => p.println(ReportingSupport.csv(r)))
      finally { p.flush(); val bad = p.checkError(); p.close(); require(!bad) }
    } finally { in.close(); audit.flush(); val bad = audit.checkError(); audit.close(); require(!bad) }
  }
  def main(args: Array[String]): Unit = {
    val p = LedgerApp.parseArgs(args)
    prepare(new File(p("in")), new File(p("out")))
    p.get("vendorMaster").foreach(path => prepareMaster(new File(path), new File(p("preparedMaster"))))
  }
}
