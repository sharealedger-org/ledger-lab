package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport}
import scala.collection.mutable

/** Read-only inventory; never selects an ambiguous accounting reference. */
object ReferenceLookupAudit {
  def main(args: Array[String]): Unit = {
    val params = LedgerApp.parseArgs(args)
    val root = new File(params("refPath")); val out = new File(params("out"))
    require(root.isDirectory && !out.exists() && out.mkdirs())
    val specs = Vector(
      ("aRefAgencyCSV.txt", "AGY_AGENCY_KEY", "AGY_AGENCY_NAME"),
      ("aRefFundCSV.txt", "FNDDTL_FUND_DETAIL_KEY", "FNDDTL_FUND_DETAIL_NAME"),
      ("aRefObjectCSV.txt", "OBJ_OBJECT_KEY", "4-DIGIT OBJECT NAME"),
      ("aRefProgramCSV.txt", "SPRG_SUB_PROGRAM_KEY", "SPRG_SUB_PROGRAM_NAME"),
      ("aRefSourceCSV.txt", "SRC_SOURCE_KEY", "SRC_SOURCE_NAME"))
    val conflicts = ReportingSupport.writer(out, "conflicts.csv", Seq("table", "normalized_name", "candidate_key", "source_record", "raw_record_csv"))
    val manifest = ReportingSupport.writer(out, "input_manifest.csv", Seq("path", "rows", "bytes", "sha256", "conflicting_names"))
    try specs.foreach { case (name, keyColumn, nameColumn) =>
      val file = new File(root, name); require(file.length() <= 16L * 1024 * 1024)
      val in = new ReportingSupport.Input(file, "reference", true)
      try {
        val ki = in.header.indexOf(keyColumn); val ni = in.header.indexOf(nameColumn)
        require(ki >= 0 && ni >= 0)
        val byName = mutable.HashMap[String, Vector[(String, Long, Vector[String])]]()
        var next = in.next()
        while (next.nonEmpty) {
          val row = next.get; require(row.size == in.header.size)
          val normalized = row(ni).trim.toUpperCase(java.util.Locale.ROOT)
          byName(normalized) = byName.getOrElse(normalized, Vector.empty) :+ ((row(ki).trim, in.count, row))
          next = in.next()
        }
        val ambiguous = byName.toVector.filter(_._2.map(_._1).distinct.size > 1).sortBy(_._1)
        ambiguous.foreach { case (n, candidates) => candidates.foreach { case (key, ordinal, row) =>
          ReportingSupport.row(conflicts, name, n, key, ordinal, ReportingSupport.csv(row))
        } }
        ReportingSupport.row(manifest, file.getCanonicalPath, in.count, file.length(), in.hash, ambiguous.size)
      } finally in.close()
    } finally {
      Seq(conflicts, manifest).foreach { p => p.flush(); val error = p.checkError(); p.close(); require(!error) }
    }
    Files.write(new File(out, "REFERENCE_AUDIT_COMPLETE").toPath, "READ_ONLY\n".getBytes(StandardCharsets.UTF_8))
  }
}
