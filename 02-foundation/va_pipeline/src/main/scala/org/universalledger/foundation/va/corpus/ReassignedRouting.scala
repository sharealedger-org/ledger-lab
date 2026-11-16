package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.time.LocalDate
import org.universalledger.foundation.va.ledger.ReportingSupport._

/** Current agency/fund/object context state only; persistent shared suppliers are deterministic registry inputs. */
object ReassignedRouting {
  /** Frozen supplier assignment shared by sampled generation and full historical preparation. */
  def supplier(seed:String,population:String,agency:String,category:String,sourceName:String):(String,String,Boolean)={
    val nameSignal=IdentityAudit.normalize(sourceName)
    val shared=ComponentVariability.u(seed,nameSignal+"|"+category,"reuse-shared-pool")<0.2
    val pool=if(shared)"shared|"+category else "agency|"+agency+"|"+category
    val slots=if(shared)8 else 32
    val draw=ComponentVariability.u(seed,pool+"|"+nameSignal,"reuse-supplier-slot")
    val slot=math.min(slots-1,(draw*draw*slots).toInt)
    val actor="reassigned|"+pool+"|"+slot
    (StablePartyIdentity.generatedVendor(population,actor),actor,shared)
  }
  val attributes=Vector("agency","fund","object","generation_class","origin_locator","posting_month")
  def totals(root:Option[File],c:PhaseTwoCorpus.Config):Map[Int,Long]={
    val sums=scala.collection.mutable.Map[Int,Long]().withDefaultValue(0L)
    root.foreach { r=>require(new File(r,"REUSED_SELECTION_COMPLETE").isFile)
      ConnectedViewsCheck.scan(new File(r,"selected.csv")){x=>val y=x("source_year").toInt+28
        if(y>=c.first&&y<=c.last)sums(y)=Math.addExact(sums(y),x("amount_cents").toLong)
      }
    };sums.toMap
  }
  def append(root:File,c:PhaseTwoCorpus.Config,registry:PrintWriter,instructions:PrintWriter,ids:PrintWriter,privateDir:File,work:File,metrics:PrintWriter,chunk:Int):Long={
    require(new File(root,"REUSED_SELECTION_COMPLETE").isFile)
    val raw=new File(work,"reused_context_input.csv");val p=new PrintWriter(raw,"UTF-8")
    try ConnectedViewsCheck.scan(new File(root,"selected.csv")){r=>
      val year=r("source_year").toInt+28
      if(year>=c.first&&year<=c.last){val values=ReassignedActivity.header.map(r)
        row(p,(Vector(CorpusExternalSort.key(r("agency"),r("fund"),r("object"),year.toString,r("source_date"),r("source_locator")))++values):_*)
      }
    }finally IdentityAudit.close(p)
    val sorted=new File(work,"reused_context_sorted.csv");CorpusExternalSort(raw,sorted,chunk,metrics)
    privateDir.mkdirs()
    val lineage=writer(privateDir,"reassigned_source_uses.csv",Seq("event_id","synthetic_vendor","private_context","source_year","source_locator","source_snapshot_sha256","original_date","original_posting_period","amount_cents","assignment_rule","availability_status"))
    val contexts=writer(privateDir,"reassigned_contexts.csv",Seq("context_id","component_type","effective_from","effective_to","scope","truth_status"))
    val profiles=writer(privateDir,"reassigned_context_profiles.csv",Seq("private_context","agency","fund","object","roster_limit","assignment_rule"))
    val memberships=writer(privateDir,"reassigned_memberships.csv",Seq("context_id","namespace","party","role","effective_from","effective_to"))
    val definitions=writer(privateDir,"reassigned_actor_definitions.csv",Seq("vendor_id","private_actor_key","scope","identity_contract"))
    var current="";var count=0L
    val roster=scala.collection.mutable.Set[String]()
    try PhaseTwoCheck.raw(sorted){r=>
      val sourceYear=r(1).toInt;val year=sourceYear+28;val agency=r(2);val fund=r(3);val obj=r(4);val original=LocalDate.parse(r(5));val cents=r(6).toLong
      val locator=r(7);val snapshot=r(8);val sourceName=r(9);val period=r(11);val category=r(12)
      val context=PhaseTwoCorpus.id(c.seed,"reassigned-context",agency,fund,obj)
      if(context!=current){
        row(contexts,context,"reassigned_procurement_pattern",SyntheticCalendar.label(LocalDate.of(c.first-1,7,1)),SyntheticCalendar.label(LocalDate.of(c.last,7,1)),"fictional-fund","constructed_supplier_relationship")
        row(profiles,context,agency,fund,obj,40,"agency-category-local32-shared8-v1");current=context;roster.clear()
      }
      // Repeated source-name activity suggests a persistent fictional profile, never a verified real party.
      val (vendor,privateActor,shared)=supplier(c.seed,c.population,agency,category,sourceName)
      if(roster.add(vendor)){require(roster.size<=40,"Current-context supplier roster bound");row(memberships,context,"V",vendor,"eligible_supplier",SyntheticCalendar.label(LocalDate.of(c.first-1,7,1)),SyntheticCalendar.label(LocalDate.of(c.last,7,1)))}
      val source="R"+vendor
      val event=PhaseTwoCorpus.id(c.seed,"reassigned-event",sourceYear,locator,snapshot)
      val day=original.plusYears(28);val month=period.takeRight(2).toInt
      val posted=LocalDate.of(year-(if(month>=7)1 else 0),month,1).withDayOfMonth(LocalDate.of(year-(if(month>=7)1 else 0),month,1).lengthOfMonth())
      val available=(if(posted.isAfter(day))posted else day).plusDays(1+(ComponentVariability.u(c.seed,event,"reuse-arrival")*30).toInt)
      val name="Fictional Supplier "+vendor
      val template=f"SIM_I${1+(ComponentVariability.u(c.seed,privateActor,"reuse-template")*32).toInt}%06d"
      row(registry,CorpusExternalSort.key(source),source,"VENDOR",vendor,name,template)
      row(instructions,CorpusExternalSort.key(source,day.toString,event),source,event,day,available,"EXP",cents,"",context,year,agency,fund,obj,"reassigned_activity",locator,month)
      row(ids,CorpusExternalSort.key(event),event)
      row(lineage,event,vendor,context,sourceYear,locator,snapshot,original,period,cents,"agency-category-local32-shared8-v1","synthetic_arrival_lag")
      row(definitions,vendor,privateActor,if(shared)"shared_category" else "agency_category",StablePartyIdentity.Version)
      count+=1
    }finally {IdentityAudit.close(lineage);IdentityAudit.close(contexts);IdentityAudit.close(profiles);IdentityAudit.close(memberships);IdentityAudit.close(definitions)}
    count
  }
}
