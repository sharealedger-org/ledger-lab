package org.universalledger.foundation.va.corpus
import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.ReportingSupport

class ReassignedCorpusAuditSpec extends AnyFunSuite {
  val repo=new File("../..").getCanonicalFile
  def temp(f:File=>Unit):Unit={val d=Files.createTempDirectory("reused-audit-").toFile
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try f(d)finally remove(d)
  }
  def write(f:File,s:String):Unit={f.getParentFile.mkdirs();Files.write(f.toPath,s.getBytes("UTF-8"))}
  def fixture(root:File,corrupt:Boolean):File={
    val selection=new File(root,"selection");selection.mkdirs()
    val p=ReportingSupport.writer(selection,"selected.csv",ReassignedActivity.header)
    try Vector((2003,"F1",12345L,"a"),(2003,"F2",1000L,"b"),(2003,"F1",-100L,"c"),(2004,"F1",54321L,"d")).foreach{case(y,f,c,id)=>
      ReportingSupport.row(p,y,"A1",f,"301",s"$y-05-20",c,"SRC1:test:"+id,"0"*64,"Named source pattern",id,f"${y%100}%04d/06","23")
    }finally IdentityAudit.close(p)
    write(new File(selection,"REUSED_SELECTION_COMPLETE"),"test-fixture\n")
    val input=new File(root,"inputs")
    PhaseTwoCorpus.generate(new File(repo,"data/synthetic/phase2_generation_v1/fixture.csv"),new File(repo,"data/synthetic/connected_reporting_v1"),new File(repo,"data/synthetic/reclass_history_v1"),input,16,None,Some(selection))
    // Deliberately minimal canonical fixtures test this checker; they are not financial-producer evidence.
    val attrs=scala.collection.mutable.Map[String,Vector[String]]()
    ConnectedViewsCheck.scan(new File(input,"evidence/financial_events.csv")){r=>if(r("generation_class")=="reassigned_activity")attrs(r("event_id"))=Vector(r("agency"),r("fund"),r("object"))}
    val outputs=(2031 to 2032).map(y=>y->new java.io.PrintWriter({val f=new File(root,s"output/FY${y%100}/SortedJEFY${y%100}merged.csv");f.getParentFile.mkdirs();f},"UTF-8")).toMap
    try ConnectedViewsCheck.scan(new File(input,"source_lineage.csv")){r=>
      val year=SyntheticCalendar.fiscalYear(java.time.LocalDate.parse(r("effective_date")))
      Vector("D","O").foreach {side=>
        val a=attrs.getOrElse(r("event_id"),Vector("101","201","301"))
        val row=Vector.fill(39)("").updated(0,r("entity_id")).updated(6,"Payment Sys").updated(12,if(side=="D")a(0)else"0")
          .updated(13,if(side=="D" && corrupt && attrs.contains(r("event_id")))"WRONG_FUND"else if(side=="D")a(1)else"0000")
          .updated(14,if(side=="D")a(2)else"0000").updated(16,if(side=="D")"EXP"+a(2)else"0000").updated(31,side).updated(35,r("locator"))
        outputs(year).println(ReportingSupport.csv(row))
      }
    }finally outputs.values.foreach(IdentityAudit.close)
    write(new File(root,"CORPUS_EVALUATED"),"unit-fixture-only\n")
    input
  }
  test("original/public/posted classifications and actual multi-year supplier recurrence are checked") {temp{root=>
    val input=fixture(root,false);val out=new File(root,"audit")
    ReassignedCorpusAudit.audit(input,new File(root,"output"),out)
    val values=scala.collection.mutable.Map[String,Long]()
    ConnectedViewsCheck.scan(new File(out,"audit_summary.csv")){r=>assert(r("status")=="PASS");values(r("metric"))=r("value").toLong}
    assert(values("selected_originals")==4 && values("posted_classification_matches")==4)
    assert(values("fictional_reused_suppliers")==1 && values("suppliers_in_multiple_years")==1 && values("suppliers_in_multiple_afo_contexts")==1)
    assert(new File(out,"REASSIGNED_AUDIT_COMPLETE").isFile)
  }}
  test("a changed nominal classification fails without publishing an audit completion") {temp{root=>
    val input=fixture(root,true);val out=new File(root,"audit")
    intercept[IllegalArgumentException]{ReassignedCorpusAudit.audit(input,new File(root,"output"),out)}
    assert(!out.exists() && !new File(root,"audit.attempt/REASSIGNED_AUDIT_COMPLETE").exists())
  }}
}
