package org.universalledger.foundation.va.corpus
import java.io.File
import java.time.LocalDate
import org.universalledger.foundation.va.ledger.ReportingSupport.{row=>plainRow,_}
/** Operational evidence, never a financial posting producer. Truth is separately held. */
object PhaseTwoLifecycle {
  private def row(p:java.io.PrintWriter,x:Any*):Unit=SyntheticCalendar.row(p,x:_*)
  val proposalHeader=Vector("proposal_id","version","issued_at","effective_date","available_at","target_date","scope","currency","expected_cost_cents","expected_receipt_cents","assumptions")
  val recordHeader=Vector("record_id","system","local_party","kind","effective_date","available_at","local_ref","business_ref","proposal_ref","supersedes","amount_cents","currency","quantity","unit","status")
  val outcomeHeader=Vector("resolution_id","proposal_id","measure","value","unit","occurred_on","available_at","cutoff","observation_status","support_records","match_basis")
  def generate(out:File,c:PhaseTwoCorpus.Config,budgets:PhaseTwoCalibration.Budgets,registry:Option[File]=None):Unit={
    import PhaseTwoCorpus.{id,close}
    val e=new File(out,"evidence");val t=new File(out,"private")
    val proposals=writer(e,"proposals.csv",proposalHeader);val records=writer(e,"records.csv",recordHeader)
    val parties=writer(e,"parties.csv",Seq("system","local_party","role","name","location","available_at"))
    val modern=writer(e,"modernization.csv",Seq("record_id","correlation_ref","available_at","profile"))
    val outcomes=writer(t,"outcomes.csv",outcomeHeader)
    val relations=writer(t,"relationships.csv",Seq("truth_id","left_id","right_id","predicate","truth","inferability","mechanism"))
    val mechanisms=writer(t,"mechanisms.csv",Seq("private_context","proposal_id","mechanism","candidate_partition","scenario_weight_source"))
    val refs=writer(e,"references.csv",Seq("reference_id","version","scenario","scope","parent_scope","effective_from","effective_to","issued_at","available_at"))
    val scenarios=writer(e,"scenarios.csv",Seq("scenario","parent","issued_at","status","reference_id","reference_version","delivery_assumption","receipt_assumption","weight_source"))
    val openings=writer(e,"openings.csv",Seq("opening_id","scope","as_of","available_at","currency","cash_cents"))
    val drivers=writer(e,"drivers.csv",Seq("driver_id","version","effective_from","available_at","conditional_delay_days","weight_source"))
    val dependencies=writer(e,"dependencies.csv",Seq("local_party","driver_id","effective_from","available_at","status"))
    val contexts=writer(t,"contexts.csv",Seq("context_id","component_type","effective_from","effective_to","scope","truth_status"))
    val crosswalk=writer(t,"party_crosswalk.csv",Seq("system","local_party","canonical_namespace","canonical_party","truth_status"))
    val members=writer(t,"memberships.csv",Seq("context_id","namespace","party","role","effective_from","effective_to"))
    def at(d:LocalDate):String=d.toString+"T12:00:00Z"
    def localId(system:String,i:Int):String=id(c.population,"local-party",system,i)
    try {
      val start=LocalDate.of(c.first-1,7,1);val end=LocalDate.of(c.last,7,1)
      row(openings,"O1","fictional-fund",start,at(start),"USD",if(budgets.calibrated)budgets.total(c.first,"EXP")/4 else BigInt(c.expense)*c.components)
      row(refs,"ORG1",1,"baseline","fictional-service","community",start,end,at(start),at(start))
      row(refs,"ORG1",2,"future-view","fictional-service","infrastructure",start.plusMonths(9),end,at(start.plusMonths(6)),at(start.plusMonths(6)))
      row(scenarios,"baseline","",at(start),"accepted","ORG1",1,"on-time","scheduled","none")
      row(scenarios,"future-view","baseline",at(start.plusMonths(6)),"temporary","ORG1",2,"delayed","delayed","none")
      row(drivers,"REGIONAL_WINDOW",1,start,at(start),0,"invented-scenario-no-probability")
      row(drivers,"REGIONAL_WINDOW",2,"2037-07-01","2037-01-01T12:00:00Z",120,"invented-regime-no-probability")
      row(parties,"financial","A1","agency","Fictional Community Office","Fictional headquarters",at(start))
      row(crosswalk,"financial","A1","A","A1","agency_grain_only")
      (1 to (c.components+1)/2).foreach {v=>val actor=PhaseTwoCorpus.vendor(c,v);row(parties,"financial","L"+actor,"vendor",PhaseTwoCorpus.vendorName(c,v),"Shared fictional address",at(start));row(crosswalk,"financial","L"+actor,"V",actor,"generated_private")}
      registry.foreach(f=>PhaseTwoCheck.raw(f){r=>if(r(1).startsWith("R") && r(2)=="VENDOR"){row(parties,"financial","L"+r(3),"vendor",r(4),"Fictional supplier location",at(start));row(crosswalk,"financial","L"+r(3),"V",r(3),"reassigned_activity_actor")}})
      (1 to c.components).foreach { i=>
        row(parties,"staff",localId("staff",i),"employee","Officer "+id(c.seed,"staff-name",i).take(12),"Shared fictional address",at(start))
        row(parties,"receipts",localId("receipts",i),"revenue_party","Payer "+id(c.seed,"payer-name",i).take(12),"Shared fictional address",at(start))
        row(parties,"service",localId("service",i),"vendor","Service "+id(c.seed,"service-name",(i+1)/2).take(12),"Shared fictional address",at(start))
        row(parties,"responsibility",id(c.population,"responsibility-local",PhaseTwoCorpus.employee(c,i)),"employee","Officer "+id(c.seed,"responsibility-name",i).take(12),"Fictional office",at(start))
        row(crosswalk,"staff",localId("staff",i),"E",PhaseTwoCorpus.employee(c,i),"generated_private")
        row(crosswalk,"responsibility",id(c.population,"responsibility-local",PhaseTwoCorpus.employee(c,i)),"E",PhaseTwoCorpus.employee(c,i),"generated_private")
        row(crosswalk,"receipts",localId("receipts",i),"R",PhaseTwoCorpus.payer(c,i),"generated_expected_payer")
        row(crosswalk,"service",localId("service",i),"V",PhaseTwoCorpus.vendor(c,(i+1)/2),"generated_private")
        if(i%8==6)row(dependencies,localId("service",i),"REGIONAL_WINDOW",start,at(start),"supported_dependency")
        (c.first to c.last).foreach { fy=>
          val expense=budgets.component(fy,"EXP",i);val receipt=budgets.component(fy,"REV",i)
          val commitment=budgets.component(fy,"PO",i)
          val day=LocalDate.of(fy-1,7,1);val context=id(c.seed,"context",i,fy);val proposal=id(c.seed,"proposal",i,fy)
          val until=day.plusYears(1)
          row(contexts,context,Vector("service_delivery","facility_support","recurring_supply")(i%3),day,until,"fictional-fund","generated_private")
          row(contexts,id(c.seed,"background",i,fy,"EMP"),"independent_staffing",day,until,"fictional-fund","generated_private")
          row(members,context,"V",PhaseTwoCorpus.vendor(c,(i+1)/2),"supplier",day,until)
          row(members,context,"E",PhaseTwoCorpus.employee(c,i),"responsible",day.plusDays(6),day.plusMonths(3))
          row(members,context,"E",PhaseTwoCorpus.employee(c,i%c.components+1),"responsible",day.plusMonths(3),until)
          row(members,context,"R",PhaseTwoCorpus.payer(c,i),"expected_payer",day,until)
          row(members,id(c.seed,"background",i,fy,"EMP"),"E",PhaseTwoCorpus.employee(c,i),"independent_assignment",day,until)
          if(i%10==0){row(contexts,id(c.seed,"background",i,fy,"REV"),"independent_agency_receipt",day,until,"fictional-fund","agency_grain_only");row(members,id(c.seed,"background",i,fy,"REV"),"A","A1","recipient_agency",day,until)}
          val m=i%8;val link=id(c.seed,"modernization",i,fy);val target=day.plusMonths(3).minusDays(1)
          val done=day.plusMonths(if(m==6 && fy>=2038)8 else if(m==0)2 else 4).plusDays(9);val known=done.plusDays(if(m==1)45 else 10)
          val completion=id(c.seed,"completion",i,fy);val order=id(c.seed,"operational-order",i,fy)
          val ref=if(i%3==0)id(c.seed,"business",i,fy) else "";val local=localId("service",i)
          val expected=expense+(if(m%2==0)expense/5 else -expense/10)
          row(proposals,proposal,1,at(day),day,at(day),target,"fictional-service","USD",expected,receipt,"illustrative-schedule-not-probability")
          val archetype=i%3
          val orderKind=if(!c.variable)"commitment"else Vector("service_order","work_order","purchase_order")(archetype)
          val completionKind=if(!c.variable)"delivery"else Vector("service_acceptance","inspection","delivery")(archetype)
          val outputUnit=if(c.variable && archetype==1)"inspection_points"else if(c.variable && archetype==2)"supply_units"else"service_units"
          row(records,order,"service",local,orderKind,day.plusDays(5),at(day.plusDays(6)),"",ref,proposal,"",commitment,"USD","","","tentative")
          val staff=id(c.seed,"staff",i,fy);val schedule=id(c.seed,"schedule",i,fy);val changed=id(c.seed,"changed-responsibility",i,fy)
          row(records,staff,"staff",localId("staff",i),"responsibility",day.plusDays(6),at(day.plusDays(8)),"",ref,"","","","","","","active")
          row(records,schedule,"receipts",localId("receipts",i),"expected_receipt",day.plusMonths(2),at(day.plusDays(9)),"",ref,"","",receipt,"USD","","","expected")
          if(m!=3)row(records,completion,"service",local,if(m==2)"cancellation" else completionKind,done,at(known),order,ref,proposal,"","","",if(m==2)"" else if(m==1)"60" else "100",outputUnit,if(m==2)"canceled" else if(m==1)"partial" else "observed")
          if(c.variable && archetype==2 && m!=2 && m!=3){
            val consumption=id(c.seed,"consumption",i,fy)
            row(records,consumption,"inventory","","consumption",done.plusDays(20),at(known.plusDays(25)),"",ref,"","","","",if(m==1)48 else 80,outputUnit,"observed_quantity_no_inventory_accounting")
            row(modern,consumption,link,at(known.plusDays(25)),"integrated")
            row(relations,id(c.seed,"consumption-relation",i,fy),completion,consumption,"same_supply_batch","true","unsupported_in_disconnected","separate_inventory_system")
          }
          val invoice=id(c.seed,"invoice",i,fy);val corrected=id(c.seed,"corrected",i,fy)
          row(records,invoice,"service",local,"invoice",done,at(known),order,ref,proposal,"",expense+expense/10,"USD","","","issued")
          row(records,corrected,"service",local,"invoice",done,at(known.plusDays(7)),order,ref,proposal,invoice,expense,"USD","","","corrected")
          row(records,changed,"staff",localId("staff",i%c.components+1),"responsibility",day.plusMonths(3),at(day.plusMonths(5)),"",ref,"","","","","","","active")
          Vector((order,at(day.plusDays(6))),(staff,at(day.plusDays(8))),(schedule,at(day.plusDays(9))),(changed,at(day.plusMonths(5)))).foreach {case(r,a)=>row(modern,r,link,a,"integrated")}
          Vector("EXP","REV").filterNot(f=>f=="REV" && i%10==0).foreach { family=>
            ComponentVariability.events(c,i,fy,family,if(family=="EXP")expense else receipt).foreach(e=>row(modern,PhaseTwoCorpus.eventId(c,i,fy,family,e.slot),link,at(e.available),"integrated"))
          }
          row(relations,id(c.seed,"relation",i,fy),order,staff,"same_service_activity","true",if(ref.nonEmpty)"explicit_reference" else "unsupported_in_disconnected","separate_source_identifiers")
          row(relations,id(c.seed,"negative",i,fy),order,id(c.seed,"operational-order",i%c.components+1,fy),"same_service_activity","false","ambiguous_location","independent_background")
          row(outcomes,id(c.seed,"resolution",i,fy),proposal,"completion",if(m==2||m==3)"" else done.toString,"date",if(m==3)"" else done.toString,if(m==3)"" else at(known),s"$fy-06-30",if(m==3)"unobserved" else if(m==2)"canceled" else if(m==1)"partial" else "observed",if(m==3)"" else completion,"explicit_proposal_ref")
          row(outcomes,id(c.seed,"cost-resolution",i,fy),proposal,"final_cost",expense,"USD_cents",done,at(known.plusDays(7)),s"$fy-06-30","observed",corrected,"explicit_proposal_ref")
          if(i%10==0)row(outcomes,id(c.seed,"receipt-resolution",i,fy),proposal,"receipt_to_cutoff","","USD_cents","","",s"$fy-06-30","unobserved","","no_supported_component_receipt")
          else {
            val scheduled=ComponentVariability.events(c,i,fy,"REV",receipt)
            val cash=scheduled.filter(_.available.toString<=s"$fy-06-30")
            if(cash.isEmpty)row(outcomes,id(c.seed,"receipt-resolution",i,fy),proposal,"receipt_to_cutoff","","USD_cents","","",s"$fy-06-30","unobserved","","not_yet_observed")
            else row(outcomes,id(c.seed,"receipt-resolution",i,fy),proposal,"receipt_to_cutoff",cash.map(_.cents).sum,"USD_cents",cash.map(_.day.toString).max,at(cash.maxBy(_.available.toEpochDay).available),s"$fy-06-30",if(cash.size==scheduled.size)"observed" else "partial",cash.map(x=>PhaseTwoCorpus.eventId(c,i,fy,"REV",x.slot)).mkString(";"),"private_generation_only")
          }
          row(mechanisms,context,proposal,Vector("timely","partial-late","canceled-with-sunk-cost","completion-unobserved","shared-vendor","incidental-overlap","joint-delay","changed-responsibility")(m),if(m==6)"candidate-held-mechanism" else "inspection","invented-frequency-not-estimated-probability")
        }
      }
    }finally Seq(proposals,records,parties,modern,outcomes,relations,mechanisms,refs,scenarios,openings,drivers,dependencies,contexts,crosswalk,members).foreach(close)
  }
}
