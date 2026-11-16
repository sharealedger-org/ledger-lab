package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.nio.file.{Files,StandardCopyOption}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport,SortEngine}

/** Structural SJE preparation before posting; one bounded source journal group at a time. */
object PhaseTwoBaselineNormalize {
  import ReportingSupport._
  val Version="phase2-historical-normalization-v1"
  val canonicalDate=DateTimeFormatter.ofPattern("yy/MM/dd")
  def date(s:String):Option[LocalDate]={
    val parsed=if(s.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}"))
      try Some(LocalDate.parse(s,canonicalDate))catch{case _:java.time.DateTimeException=>None}
    else ReassignedActivity.sourceDate(s).orElse(try Some(LocalDate.parse(s))catch{case _:java.time.DateTimeException=>None})
    parsed
  }
  def shiftedDate(s:String):String=date(s).map { d=>
    val target=d.plusYears(28)
    require(target.minusYears(28)==d,"Non-reversible synthetic calendar shift")
    if(s.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}"))target.format(canonicalDate)
    else if(s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))target.toString
    else target.format(DateTimeFormatter.ofPattern("M/d/uuuu"))
  }.getOrElse(s)
  def period(s:String):String={require(s.matches("[0-9]{4}(/[0-9]{2})?"));f"${s.take(4).toInt+28}%04d"+s.drop(4)}
  def prepare(source:File,objects:File,sortSpec:File,out:File,year:Int,seed:String="phase2-v1",population:String=PhaseTwoCorpus.DefaultPopulation,chunk:Int=100000):Unit={
    require(year>=2003 && year<=2016 && objects.length()<=8L*1024*1024)
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val categories=scala.collection.mutable.Map[String,String]();val blocked=scala.collection.mutable.Set[String]()++ReassignedActivity.blockedObjects(objects)
    ConnectedViewsCheck.scan(objects){r=>
      require(categories.size<10000 && !categories.contains(r("OBJ_OBJECT_KEY")),"Object reference duplicate/bound")
      categories(r("OBJ_OBJECT_KEY"))=r("2-DIGIT OBJECT CODE")
      val label=r("4-DIGIT OBJECT NAME").toUpperCase(java.util.Locale.ROOT)
      if(Vector("INTEREST","PRINCIPAL","LOAN","PENSION","GRANT","TRAVEL").exists(label.contains))blocked+=r("OBJ_OBJECT_KEY")
    }
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    val wrapped=new File(attempt,"source_order_input.csv");val wp=new PrintWriter(wrapped,"UTF-8")
    val input=new Input(source,"historical_canonical",false,literalCanonical=true);var count=0L
    try{var n=input.next();while(n.nonEmpty){val r=n.get;require(r.size==39 && r(35).startsWith("SRC1:") && r(35).length<=2048)
      val encoded=Base64.getEncoder.encodeToString(r.mkString(",").getBytes("UTF-8"))
      row(wp,CorpusExternalSort.key(r(35)),encoded);count+=1;n=input.next()
    }}finally{input.close();IdentityAudit.close(wp)}
    val sorted=new File(attempt,"source_order_sorted.csv");CorpusExternalSort(wrapped,sorted,chunk,metrics)
    val financial=new File(attempt,"financial_order_input.csv");val fp=new PrintWriter(financial,"UTF-8")
    val spec=SortEngine.loadSpec(sortSpec.getPath);require(spec.map(_.colIndex)==Vector(0)++(9 to 22).toVector)
    val lineage=writer(attempt,"identity_lineage.csv",Seq("source_locator","original_vendor","derived_vendor","generation_class","private_actor_key","policy"))
    val actors=writer(attempt,"actor_candidates.csv",Seq("vendor_id","original_vendor","generation_class","private_actor_key","template_instrument_id"))
    val recent=new java.util.LinkedHashMap[String,java.lang.Boolean](4096,0.75f,true)
    val counters=scala.collection.mutable.Map[String,Long]().withDefaultValue(0L)
    var group=Vector.empty[Vector[String]];var key="";var actual=0L
    def flush():Unit=if(group.nonEmpty){
      require(group.size==2 && group.map(_(31)).toSet==Set("D","O"),"Expected exactly one nominal and one offset per source")
      val d=group.find(_(31)=="D").get;val o=group.find(_(31)=="O").get
      require(d(0)==o(0) && d(29)==o(29) && d(30)==o(30) && d(34)==o(34) && d(18)==o(18))
      require(group.map(r=>BigDecimal(r(25))).sum==0,"Source journal does not balance")
      val category=categories.getOrElse(d(14),"")
      val eligible=d(0).nonEmpty && d(0).forall(_=='0') && d(6)=="Payment Sys" && d(16).startsWith("EXP") &&
        !blocked(d(14)) && ReassignedEligibility.Allowed(category) && d(8).length<=512 &&
        IdentityAuditSummary.candidateClass(d(8))=="source_name_candidate_not_legal_identity" &&
        ReassignedActivity.sourceDate(d(24)).exists(x=>SyntheticCalendar.fiscalYear(x)==year) && BigDecimal(d(25))!=0
      val (vendor,actor,kind)=if(eligible){val(v,a,_)=ReassignedRouting.supplier(seed,population,d(12),category,d(8));(v,a,"reassigned_activity")}
        else (StablePartyIdentity.observedVendor(d(0)),"",if(StablePartyIdentity.unresolved(d(0)))"unresolved_preserved"else"observed_rekeyed")
      counters(kind)+=1
      row(lineage,d(35),d(0),vendor,kind,actor,Version)
      if(!recent.containsKey(vendor)){
        val template=if(eligible)f"SIM_I${1+(ComponentVariability.u(seed,actor,"reuse-template")*32).toInt}%06d"else""
        row(actors,vendor,d(0),kind,actor,template)
        recent.put(vendor,java.lang.Boolean.TRUE)
        if(recent.size()>4096){val it=recent.keySet().iterator();it.next();it.remove()}
      }else recent.get(vendor)
      group.foreach {r=>
        Vector(23,24).foreach(i=>if(date(r(i)).isEmpty)counters("unparseable_date_fields")+=1)
        val changed=r.updated(0,vendor).updated(22,period(r(22))).updated(23,shiftedDate(r(23))).updated(24,shiftedDate(r(24)))
        val named=if(eligible && r(31)=="D")changed.updated(8,"Fictional Supplier "+vendor)else changed
        require(r.indices.filterNot(Set(0,8,22,23,24)).forall(i=>r(i)==named(i)),"Unexpected structural change")
        val record=named.mkString(",");val order=SortEngine.extractKey(record,spec).keyStr.iterator.map(c=>f"${c.toInt}%04x").mkString
        fp.println(order+","+record);actual+=1
      }
      group=Vector.empty
    }
    try{PhaseTwoCheck.raw(sorted){r=>
      require(r.size==2);if(key!=r(0)){flush();key=r(0)}
      require(group.size<2,"Source journal group bound exceeded")
      group :+= new String(Base64.getDecoder.decode(r(1)),"UTF-8").split(",",-1).toVector
    };flush()}finally{IdentityAudit.close(fp);IdentityAudit.close(lineage);IdentityAudit.close(actors)}
    // Attempt-local wrappers are restart scratch, not published corpus records.
    require(wrapped.delete() && sorted.delete(),"Unable to retire consumed source-order scratch")
    val ordered=new File(attempt,"financial_order_sorted.csv")
    try CorpusExternalSort(financial,ordered,chunk,metrics)finally IdentityAudit.close(metrics)
    val result=new File(attempt,f"SortedJEFY${(year+28)%100}%02dmerged.csv");val output=new PrintWriter(result,"UTF-8")
    val lines=scala.io.Source.fromFile(ordered,"UTF-8")
    try lines.getLines().foreach(l=>output.println(l.substring(l.indexOf(',')+1)))finally{lines.close();IdentityAudit.close(output)}
    require(actual==count)
    require(financial.delete() && ordered.delete(),"Unable to retire consumed financial-order scratch")
    val controls=writer(attempt,"normalization_controls.csv",Seq("check","value","status"))
    try{row(controls,"journal_rows_preserved",actual,"PASS");counters.toVector.sortBy(_._1).foreach{case(k,v)=>row(controls,k,v,"OBSERVED")}}finally IdentityAudit.close(controls)
    Files.write(new File(attempt,"NORMALIZATION_COMPLETE").toPath,(Version+"\nrequires_actual_posting_consumer\n").getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);prepare(new File(p("source")),new File(p("objects")),new File(p("sortSpec")),new File(p("out")),p("year").toInt,p.getOrElse("seed","phase2-v1"),p.getOrElse("population",PhaseTwoCorpus.DefaultPopulation),p.getOrElse("chunk","100000").toInt)}
}
