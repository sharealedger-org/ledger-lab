package org.universalledger.foundation.va.corpus
import java.io.File
import java.nio.file.{Files,StandardCopyOption}
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}
import scala.collection.mutable
/** Conservative second-stage grain/category filter over the frozen bounded sample, without rereading the corpus. */
object ReassignedEligibility {
  val Version="supplier-expense-eligibility-v2"
  val Allowed=Set("12","13","15","21","22","23")
  def restrict(source:File,objects:File,out:File):Unit={
    require(new File(source,"REUSED_SELECTION_COMPLETE").isFile)
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val excluded=mutable.Set[String]()
    ConnectedViewsCheck.scan(objects){r=>
      require(excluded.size<10000)
      val label=r("4-DIGIT OBJECT NAME").toUpperCase(java.util.Locale.ROOT)
      if(Vector("INTEREST","PRINCIPAL","LOAN","PENSION","GRANT","TRAVEL").exists(label.contains))excluded+=r("OBJ_OBJECT_KEY")
    }
    val input=new ReportingSupport.Input(new File(source,"selected.csv"),"bounded_selection",true);require(input.header==ReassignedActivity.header)
    val p=ReportingSupport.writer(attempt,"selected.csv",input.header)
    val counts=mutable.Map[(String,String),(Long,BigInt)]().withDefaultValue((0L,BigInt(0)))
    try {var n=input.next();while(n.nonEmpty){val r=n.get;require(r.size==input.header.size && input.count<=280000)
      val accepted=Allowed(r(11)) && !excluded(r(3))
      val reason=if(accepted)"supplier_candidate_retained"else"unsupported_summary_personal_or_financing_category"
      val key=(r(0),reason);val old=counts(key);counts(key)=(old._1+1,old._2+BigInt(r(5)))
      if(accepted)p.println(ReportingSupport.csv(r));n=input.next()
    }}finally {input.close();IdentityAudit.close(p)}
    val controls=ReportingSupport.writer(attempt,"eligibility_controls.csv",Vector("source_year","reason","rows","net_cents","policy"))
    try counts.toVector.sortBy(_._1).foreach{case((year,reason),(n,cents))=>ReportingSupport.row(controls,year,reason,n,cents,Version)}finally IdentityAudit.close(controls)
    counts.keys.map(_._1).toSet.foreach((y:String)=>require(counts((y,"supplier_candidate_retained"))._1>0,"No retained supplier candidates"))
    Vector("source_manifest.csv","selection_controls.csv").foreach(n=>Files.copy(new File(source,n).toPath,new File(attempt,"initial_"+n).toPath))
    Files.copy(objects.toPath,new File(attempt,"eligibility_object_reference.csv").toPath)
    val lineage=ReportingSupport.writer(attempt,"selection_manifest.csv",Vector("input","sha256","policy","method"))
    try ReportingSupport.row(lineage,"initial_selected.csv",ComponentPilot.sha(new File(source,"selected.csv")),Version,"bottom_hash_then_independent_category_filter_no_refill")finally IdentityAudit.close(lineage)
    Files.write(new File(attempt,"REUSED_SELECTION_COMPLETE").toPath,(Version+"\nconstructed_supplier_assignment_not_real_identity\n").getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"PASS eligible_reused rows=${counts.filter(_._1._2=="supplier_candidate_retained").values.map(_._1).sum}")
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);restrict(new File(p("source")),new File(p("objects")),new File(p("out")))}
}
