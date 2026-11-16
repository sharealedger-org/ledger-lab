package org.universalledger.foundation.va.corpus
import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.ReportingSupport
class CombinedCorpusSpec extends AnyFunSuite {
  val repo=new File("../..").getCanonicalFile
  def temp(f:File=>Unit):Unit={val d=Files.createTempDirectory("combined-corpus-").toFile
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try f(d)finally remove(d)
  }
  def write(f:File,s:String):Unit={f.getParentFile.mkdirs();Files.write(f.toPath,s.getBytes("UTF-8"))}
  test("supplier eligibility excludes personal summary and financing activity without refilling the sample") {temp{d=>
    val source=new File(d,"selection");source.mkdirs()
    write(new File(source,"REUSED_SELECTION_COMPLETE"),"fixture\n")
    Vector("source_manifest.csv","selection_controls.csv").foreach(n=>write(new File(source,n),"fixture\n"))
    val p=ReportingSupport.writer(source,"selected.csv",ReassignedActivity.header)
    try Vector(("301","23"),("302","11"),("303","23")).foreach{case(obj,category)=>
      ReportingSupport.row(p,2003,"A1","F1",obj,"2003-05-20",100,"SRC1:"+obj,"0"*64,"Source pattern",obj,"0003/06",category)
    }finally IdentityAudit.close(p)
    val objects=new File(d,"objects.csv")
    write(objects,"OBJ_OBJECT_KEY,4-DIGIT OBJECT NAME\n301,Supplies\n302,Personnel\n303,Loan interest\n")
    val out=new File(d,"eligible");ReassignedEligibility.restrict(source,objects,out)
    var n=0;ConnectedViewsCheck.scan(new File(out,"selected.csv")){r=>n+=1;assert(r("object")=="301")}
    assert(n==1 && new File(out,"REUSED_SELECTION_COMPLETE").isFile)
  }}
  test("reassigned and recipe activity conserve joint annual budgets and recur across years and funds") {temp{d=>
    val selection=new File(d,"selection");selection.mkdirs()
    val p=ReportingSupport.writer(selection,"selected.csv",ReassignedActivity.header)
    try Vector((2003,"F1",12345L,"a"),(2003,"F2",1000L,"b"),(2003,"F1",-100L,"c"),(2004,"F1",54321L,"d")).foreach{case(y,f,c,id)=>
      ReportingSupport.row(p,y,"A1",f,"301",s"$y-05-20",c,"SRC1:test:"+id,"0"*64,"Named source pattern",id,f"${y%100}%04d/06","23")
    }finally IdentityAudit.close(p)
    write(new File(selection,"REUSED_SELECTION_COMPLETE"),"test-fixture\n")
    val out=new File(d,"out")
    PhaseTwoCorpus.generate(new File(repo,"data/synthetic/phase2_generation_v1/fixture.csv"),new File(repo,"data/synthetic/connected_reporting_v1"),new File(repo,"data/synthetic/reclass_history_v1"),out,2,None,Some(selection))
    assert(PhaseTwoCheck.cardinality(out,"financial_sources")==228)
    val reused=scala.collection.mutable.ArrayBuffer[Map[String,String]]()
    ConnectedViewsCheck.scan(new File(out,"private/reassigned_source_uses.csv"))(reused+=_)
    assert(reused.size==4 && reused.map(_("synthetic_vendor")).distinct.size==1)
    assert(reused.map(_("private_context")).distinct.size==2)
    var selected=0;ConnectedViewsCheck.scan(new File(out,"source_lineage.csv")){r=>if(r("generation_class")=="reassigned_activity"){selected+=1;assert(r("posting_period").endsWith("/06"));assert(r("effective_date").substring(5,7)=="05")}}
    assert(selected==4)
    PhaseTwoCheck.sources(out,new File(d,"inspect"),"2030-09-01T23:59:59Z")
    ConnectedViewsCheck.scan(new File(out,"evidence/financial_events.csv"))(r=>assert(r("effective_date").startsWith("SY") && Set("wholly_generated","reassigned_activity")(r("generation_class"))))
    assert(new String(Files.readAllBytes(new File(out,"aRefAgencyCSV.txt").toPath),"UTF-8").contains("Fictional Agency A1"))
  }}
  test("bounded variable recipes preserve totals while changing sizes counts and dates") {temp{d=>
    val cfg=new File(d,"config.csv")
    val base=new String(Files.readAllBytes(new File(repo,"data/synthetic/phase2_generation_v1/fixture.csv").toPath),"UTF-8")
    write(cfg,base+"variability,component-variability-v2\n")
    val c=PhaseTwoCorpus.config(cfg);val budgets=new PhaseTwoCalibration.Budgets(c,None)
    val counts=(1 to 100).map(i=>ComponentVariability.count(c,i,2031,"EXP"));assert(counts.distinct.size>5 && counts.forall(n=>n>=1&&n<=24))
    assert((1 to 4).map(i=>budgets.component(2031,"EXP",i)).sum==budgets.total(2031,"EXP"))
    assert((1 to 4).map(i=>budgets.component(2031,"EXP",i)).distinct.size>1)
    val out=new File(d,"out")
    PhaseTwoCorpus.generate(cfg,new File(repo,"data/synthetic/connected_reporting_v1"),new File(repo,"data/synthetic/reclass_history_v1"),out,16)
    PhaseTwoCheck.sources(out,new File(d,"inspect"),"2030-09-01T23:59:59Z")
    assert(PhaseTwoCheck.cardinality(out,"financial_sources")!=224)
    CorpusSourceAssessment.assess(out,new File(d,"assessment"))
    val alternativeConfig=new File(d,"alternative.csv")
    write(alternativeConfig,new String(Files.readAllBytes(cfg.toPath),"UTF-8").replace("seed,phase2-v1","seed,alternative-draw"))
    val alternative=new File(d,"alternative")
    PhaseTwoCorpus.generate(alternativeConfig,new File(repo,"data/synthetic/connected_reporting_v1"),new File(repo,"data/synthetic/reclass_history_v1"),alternative,16)
    PhaseTwoCheck.sources(alternative,new File(d,"alternative-inspection"),"2030-09-01T23:59:59Z")
    Vector("VendorMaster.csv","private/party_crosswalk.csv","evidence/funding_instruments.csv").foreach(n=>assert(ComponentPilot.sha(new File(out,n))==ComponentPilot.sha(new File(alternative,n)),s"Actor identities changed with activity seed: $n"))
    assert(ComponentPilot.sha(new File(out,"evidence/financial_events.csv"))!=ComponentPilot.sha(new File(alternative,"evidence/financial_events.csv")))
    assert(PhaseTwoCorpus.vendor(c,1)!=PhaseTwoCorpus.vendor(c.copy(population="another-population"),1))
    write(new File(out,"evidence/funding_schedules.csv"),PhaseTwoSchemas.headers("funding_schedules.csv").mkString(",")+"\n")
    intercept[IllegalArgumentException]{CorpusSourceAssessment.assess(out,new File(d,"missing-schedule-assessment"))}
    val dates=scala.collection.mutable.Set[String]();ConnectedViewsCheck.scan(new File(out,"evidence/financial_events.csv"))(r=>dates+=r("effective_date"))
    assert(dates.size>20)
  }}
  test("SY labels round trip fiscal-boundary and leap-day dates without lexical date assumptions") {
    Vector("2030-07-01","2031-01-01","2031-06-30","2031-07-01","2032-02-29","2064-06-30").foreach{s=>val date=java.time.LocalDate.parse(s);assert(SyntheticCalendar.parseDate(SyntheticCalendar.label(date))==date)}
    assert(SyntheticCalendar.label(java.time.LocalDate.parse("2031-06-30"))=="SY01-06-30")
    assert(SyntheticCalendar.label(java.time.LocalDate.parse("2031-07-01"))=="SY02-07-01")
  }
}
