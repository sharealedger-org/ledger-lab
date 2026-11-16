package org.universalledger.foundation.va.corpus
import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.ReportingSupport
class PhaseTwoCorpusSpec extends AnyFunSuite {
  val root=new File("../..").getCanonicalFile
  def temp(f:File=>Unit):Unit={
    val d=Files.createTempDirectory("phase2-test-").toFile
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try f(d) finally remove(d)
  }
  def generate(d:File,chunk:Int):Unit=PhaseTwoCorpus.generate(new File(root,"data/synthetic/phase2_generation_v1/fixture.csv"),new File(root,"data/synthetic/connected_reporting_v1"),new File(root,"data/synthetic/reclass_history_v1"),d,chunk)
  test("deterministic package content across external merge levels and source chunk sizes") {temp { d=>
    val a=new File(d,"a");val b=new File(d,"b");generate(a,2);generate(b,100)
    def files(dir:File):Vector[File]=dir.listFiles().toVector.flatMap(f=>if(f.isDirectory)files(f) else Vector(f))
    files(a).filterNot(f=>f.getPath.contains("/generation/")||f.getName=="manifest.csv").foreach { f=>
      val relative=a.toPath.relativize(f.toPath)
      assert(ComponentPilot.sha(f)==ComponentPilot.sha(b.toPath.resolve(relative).toFile),relative.toString)
    }
    var rows=0;var expenses=BigDecimal(0)
    ConnectedViewsCheck.scan(new File(a,"source_lineage.csv")){r=>rows+=1;if(r("family")=="EXP")expenses+=BigDecimal(r("amount"));assert(r("available_date")>=r("effective_date"))}
    assert(rows==224 && expenses==80000)
    assert(new File(a,"VARawFiles/FY 2031 Expenditure/Expenditures Jul 2030.csv").isFile)
    assert(new File(a,"VAPOData/VA_opendata_FY2032.txt").isFile)
  }}
  test("generation retains canceled and unobserved outcomes without scoring missing as zero") {temp {d=>
    val out=new File(d,"out");generate(out,16)
    var canceled=0;var unobserved=0;var late=0
    ConnectedViewsCheck.scan(new File(out,"private/outcomes.csv")){r=>
      if(r("observation_status")=="canceled"){canceled+=1;assert(r("value").isEmpty)}
      if(r("observation_status")=="unobserved"){unobserved+=1;assert(r("value").isEmpty && r("available_at").isEmpty)}
    }
    ConnectedViewsCheck.scan(new File(out,"evidence/records.csv")){r=>if(r("kind")=="delivery" && r("available_at").take(10)>r("effective_date"))late+=1;assert(!r.contains("private_context"))}
    assert(canceled==2 && unobserved==2 && late>0)
  }}
  test("invalid generation horizon is rejected before attempt publication") {temp {d=>
    val f=new File(d,"bad.csv")
    Files.write(f.toPath,"key,value\nseed,v1\ncomponents,4\nfirstYear,2030\nlastYear,2031\nexpenseCents,1000000\nreceiptCents,1200000\ncommitmentCents,300000\n".getBytes("UTF-8"))
    intercept[IllegalArgumentException](PhaseTwoCorpus.generate(f,new File(root,"data/synthetic/connected_reporting_v1"),new File(root,"data/synthetic/reclass_history_v1"),new File(d,"out"),16))
    assert(!new File(d,"out/GENERATION_COMPLETE").exists())
  }}
  test("cutoff consumer excludes private answers and late corrections and rejects changed source bytes") {temp {d=>
    val out=new File(d,"input");generate(out,16)
    val early=new File(d,"early");PhaseTwoCheck.sources(out,early,"2030-09-01T23:59:59Z")
    assert(!new File(early,"outcomes.csv").exists() && !new File(early,"modernization.csv").exists())
    ConnectedViewsCheck.scan(new File(early,"records.csv")){r=>assert(r("kind")!="invoice" && r("kind")!="delivery");assert(!r.contains("private_context"))}
    val f=new File(out,"evidence/openings.csv")
    Files.write(f.toPath,new String(Files.readAllBytes(f.toPath),"UTF-8").replace("4000000","4000001").getBytes("UTF-8"))
    val bad=new File(d,"bad")
    intercept[IllegalArgumentException](PhaseTwoCheck.sources(out,bad,"2030-09-01T23:59:59Z"))
    assert(!new File(bad,"SOURCE_INSPECTION_COMPLETE").exists())
  }}
  test("calendar emits a real leap day and the final day of the fiscal year") {
    assert(PhaseTwoCorpus.eventDay(2032,7,29).toString=="2032-02-29")
    assert(PhaseTwoCorpus.eventDay(2031,11,13).toString=="2031-06-30")
  }
  test("frozen report envelopes allocate exact annual cents and preserve all private context references") {temp {d=>
    val cfg=new File(root,"data/synthetic/phase2_generation_v1/fixture.csv")
    val cal=new File(root,"data/synthetic/phase2_generation_v1/reported_annual_calibration.csv")
    val c=PhaseTwoCorpus.config(cfg);val budgets=new PhaseTwoCalibration.Budgets(c,Some(cal))
    val out=new File(d,"calibrated")
    PhaseTwoCorpus.generate(cfg,new File(root,"data/synthetic/connected_reporting_v1"),new File(root,"data/synthetic/reclass_history_v1"),out,16,Some(cal))
    val sums=scala.collection.mutable.Map[(Int,String),BigDecimal]().withDefaultValue(BigDecimal(0))
    ConnectedViewsCheck.scan(new File(out,"source_lineage.csv")){r=>
      val day=java.time.LocalDate.parse(r("effective_date"));val fy=day.getYear+(if(day.getMonthValue>=7)1 else 0)
      sums((fy,r("family")))+=BigDecimal(r("amount"))*100
    }
    (2031 to 2032).foreach(y=>Vector("EXP","REV","PO").foreach(f=>assert(sums((y,f))==budgets.total(y,f))))
    val contexts=scala.collection.mutable.Set[String]()
    ConnectedViewsCheck.scan(new File(out,"private/contexts.csv"))(r=>assert(contexts.add(r("context_id"))))
    assert(contexts.size==16)
    ConnectedViewsCheck.scan(new File(out,"private/event_truth.csv"))(r=>assert(contexts(r("private_context"))))
    ConnectedViewsCheck.scan(new File(out,"private/memberships.csv"))(r=>assert(contexts(r("context_id"))))
    PhaseTwoCheck.sources(out,new File(d,"cutoff"),"2030-09-01T23:59:59Z")
    assert(!new File(d,"cutoff/party_crosswalk.csv").exists())
  }}
  test("external sort fails byte conservation for an unterminated internal partition") {temp {d=>
    val in=new File(d,"input.csv");Files.write(in.toPath,"ab,payload".getBytes("UTF-8"))
    val m=ReportingSupport.writer(d,"metrics.csv",CorpusExternalSort.header)
    try intercept[IllegalArgumentException](CorpusExternalSort(in,new File(d,"sorted.csv"),2,m))finally IdentityAudit.close(m)
  }}
}
