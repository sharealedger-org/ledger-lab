package org.universalledger.foundation.va.corpus
import java.io.{File,PrintWriter}
import java.nio.file.Files
import java.time.{Instant,LocalDate}
import java.time.format.DateTimeFormatter
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}
/** Bounded source inspection and exact post-engine provenance verification. No accounting production. */
object PhaseTwoCheck {
  import ReportingSupport._
  import PhaseTwoCorpus.close
  def cardinality(input:File,name:String):Long={var result:Option[Long]=None;ConnectedViewsCheck.scan(new File(input,"cardinality_manifest.csv")){r=>if(r("metric")==name){require(result.isEmpty);result=Some(r("value").toLong)}};result.getOrElse(throw new IllegalArgumentException("Missing cardinality "+name))}
  def raw(file:File)(f:Vector[String]=>Unit):Long={
    val s=scala.io.Source.fromFile(file,"UTF-8");var n=0L
    try s.getLines().foreach{l=>require(l.length<=16384);f(IdentityAudit.strict(l).getOrElse(throw new IllegalArgumentException("Bad CSV")));n+=1}finally s.close();n
  }
  def sources(input:File,out:File,cutoff:String):Unit={
    val c=PhaseTwoCorpus.config(new File(input,"config.csv"));require(!out.exists() && out.mkdirs())
    val t=Instant.parse(cutoff);val checks=writer(out,"source_checks.csv",Seq("check","rows","status"))
    val sm=writer(out,"sort_metrics.csv",CorpusExternalSort.header)
    try {
      var financial=0L
      val totals=scala.collection.mutable.Map[(Int,String),BigDecimal]().withDefaultValue(BigDecimal(0))
      val calibration=new File(input,"annual_calibration.csv")
      val budgets=new PhaseTwoCalibration.Budgets(c,if(calibration.isFile)Some(calibration)else None)
      ConnectedViewsCheck.scan(new File(input,"source_lineage.csv")){r=>
        val d=LocalDate.parse(r("effective_date"));require(r("available_date")>=r("effective_date"));val money=BigDecimal(r("amount"));financial+=1
        val y=d.getYear+(if(d.getMonthValue>=7)1 else 0);require(y>=c.first && y<=c.last)
        totals((y,r("family")))+=money*100
      }
      require(financial==cardinality(input,"financial_sources"));row(checks,"source_lineage_expected_count",financial,"PASS")
      (c.first to c.last).foreach(y=>Vector("EXP","REV","PO").foreach { f=>require(totals((y,f))==BigDecimal(budgets.total(y,f)),s"Annual envelope mismatch $y/$f");row(checks,s"annual_envelope_${y}_$f",1,"PASS") })
      // Explicit allowlist. No traversal of private or generation directories.
      val families=Vector("financial_events.csv"->"available_date","responsibility.csv"->"available_date","parties.csv"->"available_at","proposals.csv"->"available_at","records.csv"->"available_at","references.csv"->"available_at","scenarios.csv"->"issued_at","openings.csv"->"available_at","drivers.csv"->"available_at","dependencies.csv"->"available_at","cash_plans.csv"->"available_at","funding_instruments.csv"->"available_at","funding_schedules.csv"->"available_at")
      families.foreach {case(name,availability)=>
        val in=new Input(new File(input,"evidence/"+name),"evidence",true)
        require(in.header==PhaseTwoSchemas.headers(name),s"Unsupported evidence schema $name")
        require(in.header.contains(availability) && !in.header.exists(x=>Set("private_context","mechanism","truth")(x)))
        val p=writer(out,name,in.header);var emitted=0L
        val keys=new File(out,name+".keys");val kp=new PrintWriter(keys,"UTF-8")
        try {var n=in.next();while(n.nonEmpty){
          val r=n.get;require(r.size==in.header.size);val at=r(in.header.indexOf(availability))
          kp.println(PhaseTwoSchemas.identity(name,r)+",identity")
          val instant=if(at.length==10)SyntheticCalendar.parseDate(at).atStartOfDay(java.time.ZoneOffset.UTC).toInstant else SyntheticCalendar.parseInstant(at)
          if(!instant.isAfter(t)){p.println(csv(r));emitted+=1};n=in.next()
        }}finally {in.close();close(p);close(kp)}
        val sorted=new File(out,name+".sorted_keys");CorpusExternalSort(keys,sorted,100000,sm)
        var previous="";val unique=raw(sorted){r=>require(r(0)!=previous,s"Duplicate evidence identity $name");previous=r(0)}
        if(name=="responsibility.csv")require(unique==cardinality(input,"responsibility_sources"),"Missing responsibility output")
        require(keys.delete() && sorted.delete());row(checks,"unique_identity_"+name,unique,"PASS")
        row(checks,"cutoff_export_"+name,emitted,"PASS")
      }
      var missing=0L;var canceled=0L
      ConnectedViewsCheck.scan(new File(input,"private/outcomes.csv")){r=>
        if(Set("canceled","unobserved")(r("observation_status")))require(r("value").isEmpty,"Missing/canceled outcome incorrectly numeric")
        if(r("observation_status")=="unobserved"){require(r("available_at").isEmpty);missing+=1}
        if(r("observation_status")=="canceled")canceled+=1
      }
      require(missing>0 && canceled>0);row(checks,"explicit_censoring_and_cancellation",missing+canceled,"PASS")
      var hashCount=0L
      ConnectedViewsCheck.scan(new File(input,"manifest.csv")){r=>
        val f=new File(input,r("path"));require(f.isFile && f.length()==r("bytes").toLong && ComponentPilot.sha(f)==r("sha256"),s"Manifest mismatch: $f");hashCount+=1
      }
      row(checks,"immutable_package_hashes",hashCount,"PASS")
    }finally {close(checks);close(sm)}
    Files.write(new File(out,"SOURCE_INSPECTION_COMPLETE").toPath,(cutoff+"\n").getBytes("UTF-8"))
  }
  def financial(input:File,output:File,out:File,chunk:Int):Unit={
    val c=PhaseTwoCorpus.config(new File(input,"config.csv"));require(!out.exists() && out.mkdirs())
    val metrics=writer(out,"sort_metrics.csv",CorpusExternalSort.header)
    val expectedRaw=new File(out,"expected.csv");val ep=new PrintWriter(expectedRaw,"UTF-8")
    var expected=0L
    try ConnectedViewsCheck.scan(new File(input,"source_lineage.csv")){r=>
      row(ep,CorpusExternalSort.key(r("locator")),r("locator"),r("event_id"),r("family"),r("entity_id"),r("effective_date"),r("amount"),r("posting_period"));expected+=1
    }finally close(ep)
    val actualRaw=new File(out,"actual.csv");val ap=new PrintWriter(actualRaw,"UTF-8");var actual=0L
    try { (c.first to c.last).foreach { fy=>
      val yy=(fy%100).formatted("%02d");val dir=new File(output,"FY"+yy)
      var complete=0;ConnectedViewsCheck.scan(new File(dir,"metrics/run_manifest.csv")){r=>require(r("status")=="complete");complete+=1};require(complete>0)
      val in=new Input(new File(dir,s"SortedJEFY${yy}merged.csv"),"movement",false)
      try {var n=in.next();while(n.nonEmpty){val r=n.get;require(r.size==39)
        row(ap,CorpusExternalSort.key(r(35)),r(35),r(0),r(24),r(25),r(31),r(7),r(29),r(30),r(34),r(22));actual+=1;n=in.next()
      }}finally in.close()
    } }finally close(ap)
    val es=new File(out,"expected_sorted.csv");val as=new File(out,"actual_sorted.csv")
    CorpusExternalSort(expectedRaw,es,chunk,metrics);CorpusExternalSort(actualRaw,as,chunk,metrics);close(metrics)
    val e=new Input(es,"expected",false);val a=new Input(as,"actual",false)
    var pairs=0L;var previous=""
    def decode(s:String,prefix:String):String={require(s.startsWith(prefix));new String(java.util.Base64.getUrlDecoder.decode(s.substring(prefix.length)),"UTF-8")}
    try {var next=e.next();while(next.nonEmpty){
      val r=next.get;require(r(1)!=previous,"Duplicate expected locator");previous=r(1)
      var amount=BigDecimal(0);var sides=Set.empty[String]
      (0 until 2).foreach{_=>val v=a.next().getOrElse(throw new IllegalArgumentException("Missing SJE leg"))
        require(v(1)==r(1) && v(2)==r(4),"SJE source/identity mismatch")
        val side=v(5);require(Set("D","O")(side) && !sides(side),"Expected canonical debit/offset sides");sides+=side
        val money=BigDecimal(v(4));require(money==(if(side=="D")BigDecimal(r(6)) else -BigDecimal(r(6))));amount+=money
        val fmt=if(r(3)=="PO")"MM/dd/yyyy" else "MM-dd-yy"
        require(LocalDate.parse(v(3),DateTimeFormatter.ofPattern(fmt)).toString==r(5),"SJE date mismatch")
        require(v(10)==r(7),"SJE posting period mismatch")
        val rule=Map("EXP"->("VA_PUBLIC_EXPENSE","VA_EXPENSE_OBJECT"),"REV"->("VA_PUBLIC_REVENUE","VA_REVENUE_SOURCE"),"PO"->("VA_PO_BUILTIN","PO_QUANTITY_PRICE"))(r(3))
        require(v(7)==rule._1 && v(8)==rule._2 && decode(v(9),"RULE1:")=="1","Rule/version mismatch")
        if(r(3)!="REV")require(decode(v(6),"DOC1:")==r(2),"Source document mismatch")
      }
      require(amount==0 && sides==Set("D","O"));pairs+=1;next=e.next()
    };require(a.next().isEmpty,"Unexpected SJE output")}finally {e.close();a.close()}
    require(pairs==expected && actual==2*expected && expected==cardinality(input,"financial_sources"))
    val p=writer(out,"financial_checks.csv",Seq("check","expected","actual","status"))
    try {row(p,"source_pairs_balanced_lineage_date_rule",expected,pairs,"PASS");row(p,"sje_rows",expected*2,actual,"PASS")}finally close(p)
    Files.write(new File(out,"FINANCIAL_CHECK_COMPLETE").toPath,"PASS\n".getBytes("UTF-8"))
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);if(p.getOrElse("mode","sources")=="financial")financial(new File(p("input")),new File(p("output")),new File(p("out")),p.getOrElse("chunk","100000").toInt)else sources(new File(p("input")),new File(p("out")),p("cutoff"))}
}
