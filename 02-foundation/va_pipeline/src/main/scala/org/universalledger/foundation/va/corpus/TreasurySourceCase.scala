package org.universalledger.foundation.va.corpus
import java.io.File
import java.time.LocalDate
import org.universalledger.foundation.va.ledger.ReportingSupport.writer
/** Source-only contractual schedules and cash plans. No debt recognition, valuation, forecast or risk engine. */
object TreasurySourceCase {
  val cashHeader=Vector("plan_id","version","scope","issued_at","available_at","period_start","period_end","expected_expense_cents","expected_receipt_cents","currency","assumption_status")
  val instrumentHeader=Vector("instrument_id","scope","counterparty_id","contract_version","available_at","opening_principal_cents","currency","coupon_basis_points","coupon_convention","maturity_date","recognition_status")
  val scheduleHeader=Vector("schedule_id","instrument_id","contract_version","available_at","due_date","interest_cents","principal_cents","currency","schedule_status")
  def generate(out:File,c:PhaseTwoCorpus.Config,budgets:PhaseTwoCalibration.Budgets):Unit={
    val e=new File(out,"evidence");val plans=writer(e,"cash_plans.csv",cashHeader);val contracts=writer(e,"funding_instruments.csv",instrumentHeader);val schedules=writer(e,"funding_schedules.csv",scheduleHeader)
    val parties=new java.io.PrintWriter(new java.io.FileOutputStream(new File(e,"parties.csv"),true))
    val seenCounterparties=scala.collection.mutable.Set[String]()
    def at(d:LocalDate):String=d.toString+"T12:00:00Z"
    try {
      (c.first to c.last).foreach {y=>val start=LocalDate.of(y-1,7,1)
        (0 until 12).foreach {m=>val d=start.plusMonths(m)
          val expense=budgets.total(y,"EXP");val receipt=budgets.total(y,"REV")
          SyntheticCalendar.row(plans,PhaseTwoCorpus.id(c.seed,"cash-plan",y,m),1,"fictional-fund",at(start),at(start),d,d.plusMonths(1),if(m==11)expense-(expense/12)*11 else expense/12,if(m==11)receipt-(receipt/12)*11 else receipt/12,"USD","assumed_schedule_conditioned_on_annual_controls_not_a_forecast_result")
        }
      }
      val known=LocalDate.of(c.last,6,30);val maturity=known.plusYears(20)
      val count=math.max(1,math.min(32,c.components/100));val principal=math.max(10000L,budgets.total(c.last,"EXP")/10/count)
      val instrumentOrder=(1 to count).map(i=>i->StablePartyIdentity.opaque("synthetic-funding-instrument",c.population+"|"+i,"b")).sortBy(_._2)
      instrumentOrder.foreach {case(i,instrument)=>
        val counterparty=StablePartyIdentity.opaque("synthetic-funding-counterparty",c.population+"|"+(i%4),"p")
        if(seenCounterparties.add(counterparty))SyntheticCalendar.row(parties,"treasury",counterparty,"funding_counterparty","Fictional Funding Counterparty "+counterparty.takeRight(12),"Fictional treasury location",at(known))
        SyntheticCalendar.row(contracts,instrument,"treasury-source-case",counterparty,1,at(known),principal,"USD",400,"fixed_4pct_semiannual_30_360",maturity,"analytical_opening_contract_current_financial_workload_has_no_debt_adapter")
        (1 to 40).foreach {period=>val due=known.plusMonths(6L*period)
          SyntheticCalendar.row(schedules,PhaseTwoCorpus.id(c.seed,"funding-schedule",i,period),instrument,1,at(known),due,(BigInt(principal)*200/10000).toLong,if(period==40)principal else 0L,"USD","contractual_not_probability_or_realized_settlement")
        }
      }
    }finally Vector(plans,contracts,schedules,parties).foreach(IdentityAudit.close)
  }
}
