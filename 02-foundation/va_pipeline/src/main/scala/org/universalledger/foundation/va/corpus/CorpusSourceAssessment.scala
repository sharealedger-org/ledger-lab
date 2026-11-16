package org.universalledger.foundation.va.corpus
import java.io.File
import java.time.LocalDate
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}
import scala.collection.mutable
/** Post-generation inspection statistics. No forecasting, accounting production or AIM model evaluation. */
object CorpusSourceAssessment {
  case class Stats(var n:Long=0,var sum:Long=0,var negative:Long=0,var zero:Long=0,var minimum:Long=Long.MaxValue,var maximum:Long=Long.MinValue,var lag7:Long=0,var lag30:Long=0,var later:Long=0)
  def assess(input:File,out:File):Unit={
    require(new File(input,"GENERATION_COMPLETE").isFile && !out.exists() && out.mkdirs())
    val groups=mutable.Map[(Int,String,String),Stats]()
    ConnectedViewsCheck.scan(new File(input,"source_lineage.csv")){r=>
      val date=LocalDate.parse(r("effective_date"));val available=LocalDate.parse(r("available_date"));require(!available.isBefore(date))
      val key=(SyntheticCalendar.fiscalYear(date),r("generation_class"),r("family"));require(groups.contains(key)||groups.size<1000)
      val s=groups.getOrElseUpdate(key,Stats());val cents=(BigDecimal(r("amount"))*100).bigDecimal.longValueExact()
      val lag=java.time.temporal.ChronoUnit.DAYS.between(date,available)
      s.n+=1;s.sum=Math.addExact(s.sum,cents);if(cents<0)s.negative+=1;if(cents==0)s.zero+=1;s.minimum=s.minimum.min(cents);s.maximum=s.maximum.max(cents)
      if(lag<=7)s.lag7+=1 else if(lag<=30)s.lag30+=1 else s.later+=1
    }
    val p=ReportingSupport.writer(out,"source_activity.csv",Vector("synthetic_year","generation_class","family","events","net_cents","negative_events","zero_events","min_cents","max_cents","arrival_lag_0_7_days","arrival_lag_8_30_days","arrival_lag_over_30_days"))
    try groups.toVector.sortBy(_._1).foreach{case((y,c,f),s)=>ReportingSupport.row(p,SyntheticCalendar.year(y),c,f,s.n,s.sum,s.negative,s.zero,s.minimum,s.maximum,s.lag7,s.lag30,s.later)}finally IdentityAudit.close(p)
    val counts=mutable.Map[(String,String),Long]().withDefaultValue(0L)
    def count(file:String,field:String):Unit={val f=new File(input,file);if(f.isFile)ConnectedViewsCheck.scan(f){r=>val key=(file,r(field));require(counts.contains(key)||counts.size<1000);counts(key)+=1}}
    count("private/contexts.csv","component_type");count("private/reassigned_contexts.csv","component_type");count("private/outcomes.csv","observation_status");count("private/relationships.csv","inferability");count("evidence/records.csv","kind");count("evidence/parties.csv","role")
    val census=ReportingSupport.writer(out,"source_condition_census.csv",Vector("source","condition","rows"))
    try counts.toVector.sortBy(_._1).foreach{case((f,c),n)=>ReportingSupport.row(census,f,c,n)}finally IdentityAudit.close(census)
    val checks=ReportingSupport.writer(out,"treasury_contract_checks.csv",Vector("instrument_id","principal_cents","scheduled_principal_cents","payments","last_due_date","status"))
    val terms=mutable.Map[String,Long]();ConnectedViewsCheck.scan(new File(input,"evidence/funding_instruments.csv")){r=>require(terms.size<32 && !terms.contains(r("instrument_id")));terms(r("instrument_id"))=r("opening_principal_cents").toLong}
    val seen=mutable.Set[String]()
    require(terms.nonEmpty,"At least one declared funding source contract is required")
    var current="";var principal=0L;var payments=0;var previousDate:Option[LocalDate]=None
    def flush():Unit=if(current.nonEmpty){require(principal==terms(current) && payments==40 && !seen(current));seen+=current;ReportingSupport.row(checks,current,terms(current),principal,payments,SyntheticCalendar.label(previousDate.get),"PASS_source_contract_only")}
    try ConnectedViewsCheck.scan(new File(input,"evidence/funding_schedules.csv")){r=>
      val instrument=r("instrument_id");require(terms.contains(instrument))
      if(instrument!=current){flush();require(instrument>current);current=instrument;principal=0;payments=0;previousDate=None}
      val due=SyntheticCalendar.parseDate(r("due_date"));require(previousDate.forall(_.isBefore(due)));previousDate=Some(due)
      principal=Math.addExact(principal,r("principal_cents").toLong);payments+=1
    }finally {flush();IdentityAudit.close(checks)}
    require(seen.toSet==terms.keySet.toSet,"Every funding instrument must have a complete source schedule")
    java.nio.file.Files.write(new File(out,"SOURCE_ASSESSMENT_COMPLETE").toPath,"inspection_only_no_model_evaluation\n".getBytes("UTF-8"))
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);assess(new File(p("input")),new File(p("out")))}
}
