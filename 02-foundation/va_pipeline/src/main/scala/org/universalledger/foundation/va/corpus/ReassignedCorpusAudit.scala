package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.{Files,StandardCopyOption}
import java.time.LocalDate
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}
import scala.collection.mutable

/** Post-execution integrity/recurrence inspection. Never produces accounting or model outputs. */
object ReassignedCorpusAudit {
  import ReportingSupport._
  val Version="reassigned-corpus-audit-v1"
  def audit(input:File,output:File,target:File):Unit={
    require(new File(output.getParentFile,"CORPUS_EVALUATED").isFile,"Complete the financial/report workflow first")
    require(new File(input,"GENERATION_COMPLETE").isFile)
    val c=PhaseTwoCorpus.config(new File(input,"config.csv"))
    val out=new File(target.getPath+".attempt");require(!target.exists() && !out.exists() && out.mkdirs())
    val metrics=writer(out,"sort_metrics.csv",CorpusExternalSort.header)
    val receipts=writer(out,"audit_inputs.csv",Vector("input","rows","sha256"))
    def closed(in:Input,f:File):Unit={row(receipts,f.getCanonicalPath,in.count,in.hash);in.close()}
    def sorted(raw:File,name:String):File={val f=new File(out,name);CorpusExternalSort(raw,f,100000,metrics);f}
    var closedResources=false
    try {
    val originalRaw=new File(out,"original.csv");val originalWriter=new java.io.PrintWriter(originalRaw,"UTF-8")
    val selected=new File(input,"reused_activity/selected.csv");val original=new Input(selected,"selected",true)
    require(original.header==ReassignedActivity.header)
    var selectedCount=0L
    try {var n=original.next();while(n.nonEmpty){val r=n.get;require(r.size==12 && original.count<=280000)
      val y=r(0).toInt+28
      if(y>=c.first && y<=c.last){
        val event=PhaseTwoCorpus.id(c.seed,"reassigned-event",r(0).toInt,r(6),r(7))
        val date=LocalDate.parse(r(4)).plusYears(28);require(SyntheticCalendar.fiscalYear(date)==y)
        row(originalWriter,CorpusExternalSort.key(event),event,r(1),r(2),r(3),r(5),date,y,r(10).takeRight(2));selectedCount+=1
      };n=original.next()
    }}finally {closed(original,selected);IdentityAudit.close(originalWriter)}
    require(selectedCount>0)
    val observedRaw=new File(out,"observed.csv");val observedWriter=new java.io.PrintWriter(observedRaw,"UTF-8")
    val lineageFile=new File(input,"source_lineage.csv");val eventsFile=new File(input,"evidence/financial_events.csv")
    val lineage=new Input(lineageFile,"lineage",true);val events=new Input(eventsFile,"events",true)
    require(lineage.header==PhaseTwoCorpus.lineageHeader && events.header==PhaseTwoSchemas.headers("financial_events.csv"))
    var allEvents=0L;var observedCount=0L
    try {var next=lineage.next();while(next.nonEmpty){val l=next.get;val e=events.next().getOrElse(throw new IllegalArgumentException("Missing public financial event"))
      require(l.size==11 && e.size==14 && l(1)==e(0) && l(2)==e(1) && l(10)==e(9),"Lineage/public event bridge")
      require(LocalDate.parse(l(4))==SyntheticCalendar.parseDate(e(3)) && (BigDecimal(l(6))*100)==BigDecimal(e(5)))
      allEvents+=1
      if(e(9)=="reassigned_activity"){
        require(e(1)=="EXP" && e(8)=="resolved" && e(2)=="L"+l(3) && l(3).matches("v[0-9]{36}"))
        val y=SyntheticCalendar.fiscalYear(LocalDate.parse(l(4)))
        row(observedWriter,CorpusExternalSort.key(e(0)),e(0),e(11),e(12),e(13),e(5),l(4),y,l(9).takeRight(2),l(0),l(3),e(10));observedCount+=1
      };next=lineage.next()
    };require(events.next().isEmpty)}finally {closed(lineage,lineageFile);closed(events,eventsFile);IdentityAudit.close(observedWriter)}
    require(allEvents==PhaseTwoCheck.cardinality(input,"financial_sources") && observedCount==selectedCount)
    val originalSorted=sorted(originalRaw,"original_sorted.csv");val observedSorted=sorted(observedRaw,"observed_sorted.csv")
    val expectedCanonicalRaw=new File(out,"expected_canonical.csv");val expectedWriter=new java.io.PrintWriter(expectedCanonicalRaw,"UTF-8")
    val recurrenceRaw=new File(out,"recurrence.csv");val recurrenceWriter=new java.io.PrintWriter(recurrenceRaw,"UTF-8")
    val oi=new Input(originalSorted,"original_sorted",false);val ei=new Input(observedSorted,"observed_sorted",false)
    var previous="";var bridged=0L
    try {var n=oi.next();while(n.nonEmpty){val o=n.get;val e=ei.next().getOrElse(throw new IllegalArgumentException("Missing reused event"))
      require(o(0)>previous && e(0)==o(0));previous=o(0)
      require(o.slice(1,9)==e.slice(1,9),"Original/public reused classification, cents, date or posting-month mismatch")
      require(e(11)==SyntheticCalendar.year(e(7).toInt)+"-"+e(8),"Public posting period mismatch")
      row(expectedWriter,CorpusExternalSort.key(e(9)),e(9),e(10),e(2),e(3),e(4))
      row(recurrenceWriter,CorpusExternalSort.key(e(10),e(2),e(3),e(4),e(7),e(1)),e(10),e(2),e(3),e(4),e(7));bridged+=1;n=oi.next()
    };require(ei.next().isEmpty)}finally {oi.close();ei.close();IdentityAudit.close(expectedWriter);IdentityAudit.close(recurrenceWriter)}
    val actualRaw=new File(out,"actual_canonical.csv");val actualWriter=new java.io.PrintWriter(actualRaw,"UTF-8")
    var actualRows=0L;var reusedLegs=0L;var directRows=0L
    try { (c.first to c.last).foreach {y=>
      val registryFile=new File(input,"generation/registry_entity_sorted.csv");val registry=new Input(registryFile,"registry",false)
      var lastRegistry=""
      def nextActor():Option[String]={var n=registry.next();while(n.nonEmpty){val r=n.get;require(r.size==6)
        if(r(2)=="VENDOR" && r(1).startsWith("R")){require(r(1)=="R"+r(3) && r(3)>lastRegistry);lastRegistry=r(3);return Some(r(3))};n=registry.next()
      };None}
      var actor=nextActor();var previousVendor=""
      val yy=f"${y%100}%02d";val file=new File(output,s"FY$yy/SortedJEFY${yy}merged.csv");val in=new Input(file,"canonical",false)
      try {var n=in.next();while(n.nonEmpty){val r=n.get;require(r.size==39 && r(0)>=previousVendor,"Actual canonical vendor order");previousVendor=r(0);actualRows+=1
        while(actor.exists(_<r(0)))actor=nextActor()
        if(actor.contains(r(0))){reusedLegs+=1;require(Set("D","O")(r(31)) && r(6)=="Payment Sys")
          if(r(31)=="D"){require(r(16).startsWith("EXP"));row(actualWriter,CorpusExternalSort.key(r(35)),r(35),r(0),r(12),r(13),r(14));directRows+=1}
        };n=in.next()
      };while(actor.nonEmpty)actor=nextActor()}finally {closed(in,file);closed(registry,registryFile)}
    }}finally IdentityAudit.close(actualWriter)
    require(actualRows==allEvents*2 && directRows==selectedCount && reusedLegs==selectedCount*2,"Reused actor isolation/counts")
    val expectedSorted=sorted(expectedCanonicalRaw,"expected_canonical_sorted.csv");val actualSorted=sorted(actualRaw,"actual_canonical_sorted.csv")
    val expected=new Input(expectedSorted,"expected",false);val actual=new Input(actualSorted,"actual",false);var matched=0L;previous=""
    try {var n=expected.next();while(n.nonEmpty){val e=n.get;val a=actual.next().getOrElse(throw new IllegalArgumentException("Missing actual reused direct record"))
      require(e(0)>previous && e==a,"Actual reused nominal classification mismatch");previous=e(0);matched+=1;n=expected.next()
    };require(actual.next().isEmpty)}finally {expected.close();actual.close()}
    val recurrence=sorted(recurrenceRaw,"recurrence_sorted.csv")
    val parties=writer(out,"supplier_recurrence.csv",Vector("vendor_id","events","synthetic_years","afo_contexts","first_year","last_year"))
    var current="";var rows=0L;var contexts=0L;var lastContext="";val years=mutable.Set[Int]()
    var vendors=0L;var recurring=0L;var shared=0L;var membershipPairs=0L
    def flush():Unit=if(current.nonEmpty){vendors+=1;if(years.size>1)recurring+=1;if(contexts>1)shared+=1;membershipPairs+=contexts
      row(parties,current,rows,years.size,contexts,SyntheticCalendar.year(years.min),SyntheticCalendar.year(years.max))}
    try PhaseTwoCheck.raw(recurrence){r=>if(r(1)!=current){flush();current=r(1);rows=0;contexts=0;lastContext="";years.clear()}
      val context=CorpusExternalSort.key(r(2),r(3),r(4));if(context!=lastContext){contexts+=1;lastContext=context}
      val year=r(5).toInt;require(year>=c.first && year<=c.last);years+=year;require(years.size<=14);rows+=1
    }finally {flush();IdentityAudit.close(parties)}
    require(recurring>0 && shared>0 && matched==selectedCount)
    val summary=writer(out,"audit_summary.csv",Vector("metric","value","status"))
    try Vector("selected_originals"->selectedCount,"public_original_matches"->bridged,"posted_classification_matches"->matched,"actual_sje_rows_read"->actualRows,"reused_sje_legs"->reusedLegs,"fictional_reused_suppliers"->vendors,"suppliers_in_multiple_years"->recurring,"suppliers_in_multiple_afo_contexts"->shared,"supplier_context_memberships"->membershipPairs).foreach{case(k,v)=>row(summary,k,v,"PASS")}finally IdentityAudit.close(summary)
    IdentityAudit.close(metrics);IdentityAudit.close(receipts)
    closedResources=true
    Files.copy(new File("src/main/scala/org/universalledger/foundation/va/corpus/ReassignedCorpusAudit.scala").toPath,new File(out,"audit_source.scala").toPath)
    Files.write(new File(out,"REASSIGNED_AUDIT_COMPLETE").toPath,(Version+"\nread_only_no_financial_replay\n").getBytes("UTF-8"))
    Files.move(out.toPath,target.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"PASS reassigned_audit originals=$matched suppliers=$vendors multiyear=$recurring multicontext=$shared")
    } finally {if(!closedResources){metrics.close();receipts.close()}}
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);audit(new File(p("input")),new File(p("output")),new File(p("out")))}
}
