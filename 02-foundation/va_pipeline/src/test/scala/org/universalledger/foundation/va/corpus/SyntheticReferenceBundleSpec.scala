package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.ReportingSupport

class SyntheticReferenceBundleSpec extends AnyFunSuite {
  test("synthetic normalized-name references are deterministic, unique and retain every conflicting source") {
    val root = Files.createTempDirectory("synthetic-reference-bundle-")
    def remove(f: File): Unit = { if (f.isDirectory) f.listFiles().foreach(remove); Files.delete(f.toPath) }
    try {
      val input = Files.createDirectory(root.resolve("input"))
      val spec = Vector(
        "aRefAgencyCSV.txt" -> "AGY_AGENCY_CODE,AGY_AGENCY_KEY,AGY_AGENCY_NAME\n912,160,Department\n912,360,Department\n",
        "aRefFundCSV.txt" -> "FNDDTL_FUND_DETAIL_KEY,FNDDTL_FUND_DETAIL_CODE,FNDDTL_FUND_DETAIL_NAME,FND_FUND_CODE,FND_FUND_NAME\n1,0100,GENERAL,01,GENERAL\n2,0200,GENERAL,02,SPECIAL\n",
        "aRefObjectCSV.txt" -> "OBJ_OBJECT_KEY,C2,N2,C4,4-DIGIT OBJECT NAME\n1,12,TEST,1200,OBJECT\n",
        "aRefProgramCSV.txt" -> "SPRG_SUB_PROGRAM_KEY,C1,N1,C2,N2,C3,,SPRG_SUB_PROGRAM_NAME\n1,1,FUNC,10,PGM,1000,,PROGRAM\n",
        "aRefSourceCSV.txt" -> "SRC_SOURCE_KEY,C,N,CODE,,SRC_SOURCE_NAME\n1,1,CATEGORY,100,,SOURCE\n")
      spec.foreach { case (name, text) => Files.write(input.resolve(name), text.getBytes(StandardCharsets.UTF_8)) }
      val a = root.resolve("a").toFile; val b = root.resolve("b").toFile
      SyntheticReferenceBundle.prepare(input.toFile, a); SyntheticReferenceBundle.prepare(input.toFile, b)
      a.listFiles().foreach { f => assert(java.util.Arrays.equals(Files.readAllBytes(f.toPath), Files.readAllBytes(new File(b, f.getName).toPath))) }
      val agency = new ReportingSupport.Input(new File(a, "aRefAgencyCSV.txt"), "test", true)
      try { val row = agency.next().get; assert(row(1) == "360" && row(2) == "Department"); assert(agency.next().isEmpty) } finally agency.close()
      val crosswalk = new ReportingSupport.Input(new File(a, "synthetic_reference_crosswalk.csv"), "test", true)
      try {
        var next = crosswalk.next(); var rows = Vector.empty[Vector[String]]
        while (next.nonEmpty) { rows :+= next.get; next = crosswalk.next() }
        assert(rows.size == 7)
        assert(rows.count(_(6) == "synthetic_lexical_representative_not_real_resolution") == 2)
        assert(rows.forall(_.last == "true"))
      } finally crosswalk.close()
      val master = input.resolve("master.csv")
      Files.write(master, "instID,instHolderName\n88,ONE\nc88,TWO\n".getBytes(StandardCharsets.UTF_8))
      val projected = root.resolve("projected.csv").toFile
      SyntheticReferenceBundle.prepareMaster(master.toFile, projected)
      val contents = new String(Files.readAllBytes(projected.toPath), StandardCharsets.UTF_8)
      assert(contents.contains("0000000088,ONE") && contents.contains("c88,TWO"))
    } finally remove(root.toFile)
  }
}
