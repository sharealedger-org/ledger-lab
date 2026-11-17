package org.universalledger.foundation.va.corpus

import java.io.File
import org.universalledger.foundation.va.ledger.ReportingSupport

/** Foundation-owned structural policy over one complete ARE journal group.
  * Invoked before sort-key extraction by fused producers, or by the legacy
  * source-ordered normalizer. It never retains another source journal group.
  */
final class HistoricalJournalExit(objects:File,out:File,year:Int,
    seed:String="phase2-v1",population:String=PhaseTwoCorpus.DefaultPopulation) extends AutoCloseable {
  import ReportingSupport._
  import PhaseTwoBaselineNormalize.{date,period,shiftedDate,Version}
  require(year>=2003 && year<=2016 && objects.length()<=8L*1024*1024)
  private val categories=scala.collection.mutable.Map[String,String]()
  private val blocked=scala.collection.mutable.Set[String]()++ReassignedActivity.blockedObjects(objects)
  ConnectedViewsCheck.scan(objects){r=>
    require(categories.size<10000 && !categories.contains(r("OBJ_OBJECT_KEY")),"Object reference duplicate/bound")
    categories(r("OBJ_OBJECT_KEY"))=r("2-DIGIT OBJECT CODE")
    val label=r("4-DIGIT OBJECT NAME").toUpperCase(java.util.Locale.ROOT)
    if(Vector("INTEREST","PRINCIPAL","LOAN","PENSION","GRANT","TRAVEL").exists(label.contains))blocked+=r("OBJ_OBJECT_KEY")
  }
  private val lineage=writer(out,"identity_lineage.csv",Seq("source_locator","original_vendor","derived_vendor","generation_class","private_actor_key","policy"))
  private val actors=writer(out,"actor_candidates.csv",Seq("vendor_id","original_vendor","generation_class","private_actor_key","template_instrument_id"))
  private val recent=new java.util.LinkedHashMap[String,java.lang.Boolean](4096,0.75f,true)
  val counters=scala.collection.mutable.Map[String,Long]().withDefaultValue(0L)
  private var emittedRows=0L
  def rows:Long=emittedRows

  def apply(group:Vector[Vector[String]]):Vector[Vector[String]]={
    require(group.size==2 && group.forall(_.size==39) && group.map(_(31)).toSet==Set("D","O"),"Expected exactly one nominal and one offset per source")
    val d=group.find(_(31)=="D").get;val o=group.find(_(31)=="O").get
    require(d(35)==o(35) && d(35).startsWith("SRC1:") && d(35).length<=2048,"Source group lineage mismatch")
    require(d(0)==o(0) && d(29)==o(29) && d(30)==o(30) && d(34)==o(34) && d(18)==o(18))
    require(group.map(r=>BigDecimal(r(25))).sum==0,"Source journal does not balance")
    val category=categories.getOrElse(d(14),"")
    val eligible=d(0).nonEmpty && d(0).forall(_=='0') && d(6)=="Payment Sys" && d(16).startsWith("EXP") &&
      !blocked(d(14)) && ReassignedEligibility.Allowed(category) && d(8).length<=512 &&
      IdentityAuditSummary.candidateClass(d(8))=="source_name_candidate_not_legal_identity" &&
      ReassignedActivity.sourceDate(d(24)).exists(x=>SyntheticCalendar.fiscalYear(x)==year) && BigDecimal(d(25))!=0
    val(vendor,actor,kind)=if(eligible){val(v,a,_)=ReassignedRouting.supplier(seed,population,d(12),category,d(8));(v,a,"reassigned_activity")}
      else(StablePartyIdentity.observedVendor(d(0)),"",if(StablePartyIdentity.unresolved(d(0)))"unresolved_preserved"else"observed_rekeyed")
    counters(kind)+=1
    row(lineage,d(35),d(0),vendor,kind,actor,Version)
    if(!recent.containsKey(vendor)){
      val template=if(eligible)f"SIM_I${1+(ComponentVariability.u(seed,actor,"reuse-template")*32).toInt}%06d"else""
      row(actors,vendor,d(0),kind,actor,template)
      recent.put(vendor,java.lang.Boolean.TRUE)
      if(recent.size()>4096){val it=recent.keySet().iterator();it.next();it.remove()}
    }else recent.get(vendor)
    val result=group.map{r=>
      Vector(23,24).foreach(i=>if(date(r(i)).isEmpty)counters("unparseable_date_fields")+=1)
      val changed=r.updated(0,vendor).updated(22,period(r(22))).updated(23,shiftedDate(r(23))).updated(24,shiftedDate(r(24)))
      val named=if(eligible && r(31)=="D")changed.updated(8,"Fictional Supplier "+vendor)else changed
      require(r.indices.filterNot(Set(0,8,22,23,24)).forall(i=>r(i)==named(i)),"Unexpected structural change")
      named
    }
    emittedRows+=result.size
    result
  }
  def writeControls():Unit={
    val p=writer(out,"normalization_controls.csv",Seq("check","value","status"))
    try{row(p,"journal_rows_preserved",rows,"PASS");counters.toVector.sortBy(_._1).foreach{case(k,v)=>row(p,k,v,"OBSERVED")}}finally IdentityAudit.close(p)
  }
  override def close():Unit={IdentityAudit.close(lineage);IdentityAudit.close(actors)}
}
