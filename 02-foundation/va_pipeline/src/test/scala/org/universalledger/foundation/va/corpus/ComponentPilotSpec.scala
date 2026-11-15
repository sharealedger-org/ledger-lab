package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite

class ComponentPilotSpec extends AnyFunSuite {
  private val spec=new File("../../data/synthetic/component_pilot_0304_v1")
  private val base=new File("../../data/synthetic/connected_reporting_v1")
  private val pack=new File("../../data/synthetic/reclass_history_v1")
  private def temp(f: File=>Unit): Unit = {
    val root=Files.createTempDirectory("component-pilot-").toFile
    def remove(x: File): Unit = { if(x.isDirectory) x.listFiles().foreach(remove); Files.delete(x.toPath) }
    try f(root) finally remove(root)
  }
  private def fixture(dir: File): File = {
    dir.mkdirs(); Vector("budgets.csv","slots.csv","identities.csv").foreach(n=>Files.copy(new File(spec,n).toPath,new File(dir,n).toPath)); dir
  }
  private def text(f: File): String = new String(Files.readAllBytes(f.toPath),"UTF-8")
  private def edit(f: File)(fn: String=>String): Unit = Files.write(f.toPath,fn(text(f)).getBytes("UTF-8"))
  test("context generation and sorted assignment are invariant to input order and sort chunking") {
    temp { root =>
      val shuffled=fixture(new File(root,"shuffled"))
      shuffled.listFiles().foreach(f=>edit(f) { s => val lines=s.split("\n").toVector; (lines.head +: lines.tail.reverse).mkString("\n")+"\n" })
      val a=new File(root,"a"); val b=new File(root,"b")
      ComponentPilot.generate(spec,base,pack,a,16); ComponentPilot.generate(shuffled,base,pack,b,2)
      def files(dir: File): Vector[File] = dir.listFiles().toVector.flatMap(f=>if(f.isDirectory) files(f) else Vector(f))
      files(a).filterNot(f=>f.getPath.contains("/generation/") || f.getName=="input_manifest.csv").foreach { f=>
        val rel=a.toPath.relativize(f.toPath); assert(text(f)==text(b.toPath.resolve(rel).toFile),rel.toString)
      }
      var controls=0; ConnectedViewsCheck.scan(new File(a,"generation/context_controls.csv")) { r=>assert(r("status")=="PASS"); controls+=1 }; assert(controls==24)
      var events=0; var expense=BigDecimal(0); var receipt=BigDecimal(0)
      ConnectedViewsCheck.scan(new File(a,"visible/event_evidence.csv")) { r=>
        events+=1; if(r("family")=="EXP") expense+=BigDecimal(r("amount")); if(r("family")=="REV") receipt+=BigDecimal(r("amount"))
        assert(!r.contains("context_id"))
      }
      assert(events==36 && expense==3520 && receipt==5280)
      val master=text(new File(a,"VendorMaster.csv")); assert(master.contains("2004-06-01") && master.contains("c930001"))
      assert(text(new File(a,"visible/event_evidence.csv")).contains("2004-02-01"))
    }
  }
  test("missing and duplicate assignment fail before completion") {
    temp { root=>
      val missing=fixture(new File(root,"missing")); edit(new File(missing,"identities.csv"))(_.split("\n").filterNot(_.startsWith("SUPPLIER-A,")).mkString("\n")+"\n")
      val a=new File(root,"a"); val e=intercept[IllegalArgumentException](ComponentPilot.generate(missing,base,pack,a)); assert(e.getMessage.contains("Unmatched")); assert(!new File(a,"GENERATION_COMPLETE").exists())
      val duplicate=fixture(new File(root,"duplicate")); edit(new File(duplicate,"identities.csv"))(s=>s+s.split("\n")(1)+"\n")
      val b=new File(root,"b"); intercept[IllegalArgumentException](ComponentPilot.generate(duplicate,base,pack,b)); assert(!new File(b,"GENERATION_COMPLETE").exists())
    }
  }
  test("invalid fiscal dates and duplicate events are rejected") {
    temp { root=>
      val bad=fixture(new File(root,"bad")); edit(new File(bad,"slots.csv"))(_.replaceFirst("2003-06-22","2004-06-22"))
      intercept[IllegalArgumentException](ComponentPilot.generate(bad,base,pack,new File(root,"a")))
      val dup=fixture(new File(root,"dup")); edit(new File(dup,"slots.csv"))(s=>s+s.split("\n")(1)+"\n")
      intercept[IllegalArgumentException](ComponentPilot.generate(dup,base,pack,new File(root,"b")))
    }
  }
  test("bounded sort fan-in rejects inputs exceeding declared resource envelope") {
    temp { root=>
      val over=fixture(new File(root,"over")); edit(new File(over,"slots.csv"))(s=>s+s.split("\n").tail.take(10).mkString("\n")+"\n")
      val e=intercept[IllegalArgumentException](ComponentPilot.generate(over,base,pack,new File(root,"a"),1)); assert(e.getMessage.contains("sort row/fan-in"))
    }
  }
}
