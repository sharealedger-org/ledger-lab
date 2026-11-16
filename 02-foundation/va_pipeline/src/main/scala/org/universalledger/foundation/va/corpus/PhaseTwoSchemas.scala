package org.universalledger.foundation.va.corpus
/** Version-one source evidence headers; intentionally distinct from canonical financial schemas. */
object PhaseTwoSchemas {
  val headers:Map[String,Vector[String]]=Map(
    "financial_events.csv"->Vector("event_id","family","local_party","effective_date","available_date","amount_cents","currency","business_ref","identity_status","generation_class","posting_period","agency","fund","object"),
    "responsibility.csv"->Vector("record_id","local_employee","effective_date","available_date","business_ref","role"),
    "parties.csv"->Vector("system","local_party","role","name","location","available_at"),
    "proposals.csv"->PhaseTwoLifecycle.proposalHeader,
    "records.csv"->PhaseTwoLifecycle.recordHeader,
    "references.csv"->Vector("reference_id","version","scenario","scope","parent_scope","effective_from","effective_to","issued_at","available_at"),
    "scenarios.csv"->Vector("scenario","parent","issued_at","status","reference_id","reference_version","delivery_assumption","receipt_assumption","weight_source"),
    "openings.csv"->Vector("opening_id","scope","as_of","available_at","currency","cash_cents"),
    "drivers.csv"->Vector("driver_id","version","effective_from","available_at","conditional_delay_days","weight_source"),
    "dependencies.csv"->Vector("local_party","driver_id","effective_from","available_at","status"),
    "cash_plans.csv"->TreasurySourceCase.cashHeader,
    "funding_instruments.csv"->TreasurySourceCase.instrumentHeader,
    "funding_schedules.csv"->TreasurySourceCase.scheduleHeader
  )
  def identity(name:String,r:Vector[String]):String=name match {
    case "parties.csv"=>CorpusExternalSort.key(r(0),r(1))
    case "references.csv"=>CorpusExternalSort.key(r(0),r(1),r(2))
    case "drivers.csv"=>CorpusExternalSort.key(r(0),r(1))
    case "dependencies.csv"=>CorpusExternalSort.key(r(0),r(1),r(2))
    case "proposals.csv"=>CorpusExternalSort.key(r(0),r(1))
    case _=>CorpusExternalSort.key(r(0))
  }
}
