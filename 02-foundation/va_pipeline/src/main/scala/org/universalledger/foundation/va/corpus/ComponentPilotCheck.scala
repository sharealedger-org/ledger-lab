package org.universalledger.foundation.va.corpus

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.universalledger.foundation.va.ledger.ReportingSupport
import scala.collection.mutable

/** Independent bounded pilot checks, not a financial producer. */
object ComponentPilotCheck {
  type Row=Map[String,String]
  private def read(file: File): Vector[Row] = {
    val rows=mutable.ArrayBuffer[Row]()
    ConnectedViewsCheck.scan(file) { r=>require(rows.size<ComponentPilot.MaxRows); rows+=r }
    rows.toVector
  }
  private def family(event: String): String = Map("EXPENSE_PAYMENT"->"EXP","REVENUE_RECEIPT"->"REV","COMMITMENT"->"PO")(event)
  def inspect(root: File): Unit = {
    val input=new File(root,"inputs"); val report=new File(root,"report")
    require(new File(input,"GENERATION_COMPLETE").isFile)
    ConnectedViewsCheck.inspect(report)
    val evidence=read(new File(input,"visible/event_evidence.csv"))
    val truth=read(new File(input,"withheld/generation_truth.csv"))
    require(evidence.size==36 && evidence.map(_("event_id")).distinct.size==36)
    require(truth.map(_("event_id")).toSet==evidence.map(_("event_id")).toSet)
    require(evidence.forall(r=> !r.contains("context_id") && r("event_id").matches("EV-[0-9a-f]{24}")))
    val vendorTruth=truth.filter(r=>Set("EXP","PO")(r("family")))
    require(vendorTruth.filter(_("entity_id")=="c930001").map(_("context_id")).toSet==Set("C001","C002"))
    require(vendorTruth.filter(_("entity_id")=="c930002").map(_("context_id")).toSet==Set("C003"))
    val financial=evidence.filter(_("family")!="EMP")
    // Verify exact pre-TTL source positions and amounts; downstream TTL does not retain these IDs.
    financial.groupBy(_("source_file")).foreach { case (path, rows)=>
      if(path.endsWith(".csv")) {
        val raw=read(new File(input,path)); require(raw.size==rows.size)
        rows.foreach { r=>
          val source=raw(r("source_row").toInt-1)
          require(BigDecimal(source("AMOUNT"))==BigDecimal(r("amount")))
          val day=LocalDate.parse(source(if(r("family")=="EXP") "VOUCHER_DATE" else "DEPOSIT_DATE"),DateTimeFormatter.ofPattern("MM-dd-yy"))
          require(day.toString==r("effective_date"))
          if(r("family")=="EXP") require(source("VOUCHER_NUMBER")==r("event_id"))
        }
      }
    }
    def signature(f: String,id: String,date: String,amount: BigDecimal): String = Seq(f,id,date,amount.bigDecimal.stripTrailingZeros.toPlainString).mkString("|")
    val expected=financial.map { r=>signature(r("family"),if(r("family")=="REV") "0000000000" else r("entity_id"),r("effective_date"),BigDecimal(r("amount"))) }.groupBy(identity).map { case(k,rs)=>k->rs.size }
    val actual=mutable.HashMap[String,Int]().withDefaultValue(0)
    val groups=mutable.HashMap[String,(BigDecimal,Int)]()
    val yearTotals=mutable.HashMap[(String,String),BigDecimal]().withDefaultValue(BigDecimal(0))
    var sjeRows=0
    Vector("03","04").foreach { year=>
      val in=new ReportingSupport.Input(new File(root,s"output/FY$year/SortedJEFY${year}merged.csv"),"movement",false)
      try {
        var next=in.next()
        while(next.nonEmpty) {
          val r=next.get; require(r.size==39 && in.count<=ComponentPilot.MaxRows)
          val fam=family(r(5)); val amount=BigDecimal(r(25)); val group=Seq(year,r(3),r(5)).mkString("|")
          val prior=groups.getOrElse(group,(BigDecimal(0),0)); groups(group)=(prior._1+amount,prior._2+1)
          if(r(31)=="D") {
            val fmt=if(fam=="PO") "MM/dd/yyyy" else "MM-dd-yy"
            val day=LocalDate.parse(r(24),DateTimeFormatter.ofPattern(fmt)).toString
            val sig=signature(fam,r(0),day,amount); actual(sig)+=1
            yearTotals((year,fam))+=amount
            if(fam!="PO") require(r(29).trim.nonEmpty && r(30).trim.nonEmpty,"Missing APA SJE rule lineage")
          }
          sjeRows+=1; next=in.next()
        }
      } finally in.close()
    }
    require(actual.toMap==expected,"Financial event signature multiset differs after TTL/posting")
    require(sjeRows==60 && groups.values.forall(x=>x._1==0 && x._2==2),"Unbalanced/duplicate journal group")
    require(yearTotals.toMap==Map(("03","EXP")->BigDecimal(1600),("04","EXP")->BigDecimal(1920),("03","REV")->BigDecimal(2400),("04","REV")->BigDecimal(2880),("03","PO")->BigDecimal(400),("04","PO")->BigDecimal(480)))
    val attrs=read(new File(report,"vendor_attribute_totals.csv"))
    Vector("c930001","c930002").foreach { id=>
      require(attrs.exists(r=>r("vendor_id")==id && r("fiscal_year")=="2003" && r("attribute")=="instSizeTier" && r("attribute_value")=="SMALL"))
      require(attrs.exists(r=>r("vendor_id")==id && r("fiscal_year")=="2004" && r("attribute")=="instSizeTier" && r("attribute_value")=="LARGE"))
    }
    val employees=read(new File(input,"visible/employee_responsibility.csv")); require(employees.size==6)
    val at=employees.filter(r=>r("effective_start")<="2004-01-20" && r("available_date")<="2004-01-20").groupBy(_("project_id")).map { case(k,rs)=>k->rs.maxBy(_("effective_start")).apply("employee_id") }
    require(at("PROJECT-1")=="SIM_E002" && at("PROJECT-2")=="SIM_E002" && at("PROJECT-UNRELATED")=="SIM_E003")
    val checks=new File(root,"validation"); checks.mkdirs()
    val cutoff=ReportingSupport.writer(checks,"cutoff_event_evidence.csv",ComponentPilot.lineageHeader)
    var cutoffRows=0
    try evidence.filter(r=>r("effective_date")<="2004-01-20" && r("available_date")<="2004-01-20").foreach { r=>cutoff.println(ReportingSupport.csv(ComponentPilot.lineageHeader.map(r))); cutoffRows+=1 } finally cutoff.close()
    require(cutoffRows==35 && !read(new File(checks,"cutoff_event_evidence.csv")).exists(_("available_date")=="2004-02-01"))
    val p=ReportingSupport.writer(checks,"pilot_checks.csv",Seq("check","status","detail"))
    try {
      ReportingSupport.row(p,"generation_context_and_identity","PASS","36 events; shared supplier across two components; background separate")
      ReportingSupport.row(p,"financial_event_multiset","PASS","30 financial source events; 60 balanced SJE rows; exact known totals by year/family")
      ReportingSupport.row(p,"dated_master_quiet_change","PASS","SMALL FY2003; LARGE FY2004; change after last transaction")
      ReportingSupport.row(p,"employee_cutoff","PASS","six responsibility records; 35 available evidence rows at 2004-01-20")
      ReportingSupport.row(p,"explicit_sje_source_event_lineage","PARTIAL","APA TTL drops voucher/source event identity; raw sidecar exact, downstream signature multiset only; PO SJE rule fields blank")
      ReportingSupport.row(p,"va_financial_anchor","NOT_VERIFIED","envelopes synthetic; no FY2003/04 ACFR tie-out")
      ReportingSupport.row(p,"scale_and_ai_effectiveness","NOT_TESTED","bounded fixture only; no model evaluated")
    } finally p.close()
    println(s"PASS component pilot financial_rows=$sjeRows generated_events=${evidence.size} cutoff_rows=$cutoffRows; limitations in validation/pilot_checks.csv")
  }
  def main(args: Array[String]): Unit = inspect(new File(org.universalledger.foundation.va.ledger.LedgerApp.parseArgs(args)("root")))
}
