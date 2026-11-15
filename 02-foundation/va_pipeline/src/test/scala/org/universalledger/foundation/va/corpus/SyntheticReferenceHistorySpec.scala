package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.scalatest.funsuite.AnyFunSuite

class SyntheticReferenceHistorySpec extends AnyFunSuite {
  private def temp(f: Path => Unit): Unit = {
    val dir = Files.createTempDirectory("reclass-history-")
    def remove(x: File): Unit = { if (x.isDirectory) x.listFiles().foreach(remove); Files.delete(x.toPath) }
    try f(dir) finally remove(dir.toFile)
  }
  private def make(root: Path, name: String = "pack", count: Int = 4): File = {
    val out = root.resolve(name).toFile
    SyntheticReferenceHistory.generate(out, count, "a" * 64); out
  }
  test("self-contained generation is byte-identical including manifests and covers all date/path oracles") {
    temp { root =>
      val a = make(root, "a"); val b = make(root, "b")
      def files(dir: File): Vector[File] = dir.listFiles().toVector.flatMap(f => if (f.isDirectory) files(f) else Vector(f))
      val names = files(a).map(f => a.toPath.relativize(f.toPath)).sortBy(_.toString)
      assert(names == files(b).map(f => b.toPath.relativize(f.toPath)).sortBy(_.toString))
      names.foreach(p => assert(java.util.Arrays.equals(Files.readAllBytes(a.toPath.resolve(p)), Files.readAllBytes(b.toPath.resolve(p))), p.toString))
      assert(new File(a, "GENERATION_COMPLETE").isFile)
      assert(!files(a).exists(f => f.getName.endsWith("pdf") || f.getName.endsWith("html")))
      val config = new String(Files.readAllBytes(new File(a, "generation_config.csv").toPath), StandardCharsets.UTF_8)
      assert(config.contains("external_sources_required,false"))
      SyntheticReferenceHistoryCheck.validate(a)
    }
  }
  test("negative hierarchy variants reject duplicate starts, orphan parents and temporal cycles") {
    temp { root =>
      val out = make(root)
      Seq("duplicate_start" -> "duplicate start", "orphan" -> "orphan", "cycle" -> "cycle").foreach { case (file, message) =>
        val error = intercept[IllegalArgumentException] {
          SyntheticReferenceHistoryCheck.validate(out, Some(new File(out, s"negative/$file.csv")))
        }
        assert(error.getMessage.contains(message), error.getMessage)
      }
    }
  }
  test("the exact change day selects the new version; an incorrect oracle cannot pass") {
    temp { root =>
      val out = make(root)
      val file = new File(out, "lookup_cases.csv").toPath
      val text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
      // Move the on-boundary query back one day but leave the expected new version.
      val wrong = text.replace("attribute_on,reference_history,agency|SIM_A405,AS_OF,2014-03-03", "attribute_on,reference_history,agency|SIM_A405,AS_OF,2014-03-02")
      assert(wrong != text); Files.write(file, wrong.getBytes(StandardCharsets.UTF_8))
      assert(intercept[IllegalArgumentException] { SyntheticReferenceHistoryCheck.validate(out) }.getMessage.contains("attribute_on"))
    }
  }
  test("hierarchy ancestors are selected at the lookup date, independently of unchanged leaf links") {
    temp { root =>
      val out = make(root)
      val schema = SyntheticReferenceHistory.schemas.find(_.name == "hierarchy_history").get
      val rows = SyntheticReferenceHistoryCheck.read(new File(out, "hierarchy_history.csv"), schema.header)
      assert(rows.count(r => r("child_key") == "SIM_SA71501") == 1)
      assert(rows.count(r => r("child_key") == "SIM_P715") == 2)
      val file = new File(out, "hierarchy_cases.csv").toPath
      val text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
      val wrong = text.replace("ancestor_on,program,service_area,SIM_SA71501,2015-01-01", "ancestor_on,program,service_area,SIM_SA71501,2014-12-31")
      assert(wrong != text); Files.write(file, wrong.getBytes(StandardCharsets.UTF_8))
      assert(intercept[IllegalArgumentException] { SyntheticReferenceHistoryCheck.validate(out) }.getMessage.contains("ancestor_on"))
    }
  }
  test("repeated labels preserve context and late-recorded dates do not become effective starts") {
    temp { root =>
      val out = make(root)
      val alias = SyntheticReferenceHistory.schemas.find(_.name == "alias_history").get
      val rows = SyntheticReferenceHistoryCheck.read(new File(out, "alias_history.csv"), alias.header)
      val general = rows.filter(r => r("source_label") == "SIMULATED GENERAL FUND" && r("effective_start") == SyntheticReferenceHistory.Baseline)
      assert(general.map(_("target_key")).toSet == Set("SIM_F405", "SIM_F748"))
      assert(rows.count(r => r("target_key") == "SIM_F405") == 2)
      val changes = new String(Files.readAllBytes(new File(out, "change_events.csv").toPath), StandardCharsets.UTF_8)
      assert(changes.contains("C_LEAF,2014-06-23,2018-07-12"))
      SyntheticReferenceHistoryCheck.validate(out)
    }
  }
  test("scale is explicit and bounded; regeneration cannot overwrite an existing output") {
    temp { root =>
      val out = make(root, count = 32)
      intercept[IllegalArgumentException] { SyntheticReferenceHistory.generate(out, 32, "a" * 64) }
      intercept[IllegalArgumentException] { SyntheticReferenceHistory.generate(root.resolve("too-large").toFile, 10001, "a" * 64) }
      intercept[IllegalArgumentException] { SyntheticReferenceHistory.generate(root.resolve("bad-hash").toFile, 32, "unknown") }
    }
  }
  test("SAL agency and the instrument hierarchy cannot contradict one another") {
    temp { root =>
      val out = make(root)
      val path = new File(out, "sal_history.csv").toPath
      val text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
      val wrong = text.replace("SIM_I000002,SIMULATED INSTRUMENT 2,LOW,SIM_A748", "SIM_I000002,SIMULATED INSTRUMENT 2,LOW,SIM_A405")
      assert(wrong != text); Files.write(path, wrong.getBytes(StandardCharsets.UTF_8))
      assert(intercept[IllegalArgumentException] { SyntheticReferenceHistoryCheck.validate(out) }.getMessage.contains("agency mismatch"))
    }
  }
}
