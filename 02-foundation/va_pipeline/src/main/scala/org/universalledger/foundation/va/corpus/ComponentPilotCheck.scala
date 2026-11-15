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
  def inspect(root: File, requireLineage: Boolean = false, baseline: Option[File] = None): Unit = {
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
    // Exact raw positions are independently tied to retained SJE locators in lineage-v2.
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
    def sourceLocator(r: Row): String = "SRC1:" + java.util.Base64.getUrlEncoder.withoutPadding
      .encodeToString(r("source_file").getBytes("UTF-8")) + ":" + r("source_row")
    def decode(s: String, prefix: String): String = {
      require(s.startsWith(prefix), s"Missing versioned lineage: $s")
      new String(java.util.Base64.getUrlDecoder.decode(s.substring(prefix.length)),"UTF-8")
    }
    val byLocator=financial.map(r=>sourceLocator(r)->r).toMap
    require(byLocator.size==financial.size,"Duplicate raw source locator")
    val located=mutable.HashMap[String,Int]().withDefaultValue(0)
    val groups=mutable.HashMap[String,(BigDecimal,Int)]()
    val sjesByYear=mutable.HashMap[String,Vector[Vector[String]]]()
    val yearTotals=mutable.HashMap[(String,String),BigDecimal]().withDefaultValue(BigDecimal(0))
    var sjeRows=0
    Vector("03","04").foreach { year=>
      val in=new ReportingSupport.Input(new File(root,s"output/FY$year/SortedJEFY${year}merged.csv"),"movement",false)
      val yearRows=mutable.ArrayBuffer[Vector[String]]()
      try {
        var next=in.next()
        while(next.nonEmpty) {
          val r=next.get; require(r.size==39 && in.count<=ComponentPilot.MaxRows)
          yearRows+=r
          val fam=family(r(5)); val amount=BigDecimal(r(25)); val group=Seq(year,r(3),r(5)).mkString("|")
          if(requireLineage) {
            require(byLocator.contains(r(35)),s"Unknown/missing retained source locator: ${r(35)}")
            val raw=byLocator(r(35)); located(r(35))+=1
            require(raw("family")==fam && r(0)==(if(fam=="REV") "0000000000" else raw("entity_id")))
            require(amount==(if(r(31)=="D") BigDecimal(raw("amount")) else -BigDecimal(raw("amount"))))
            val day=LocalDate.parse(r(24),DateTimeFormatter.ofPattern(if(fam=="PO") "MM/dd/yyyy" else "MM-dd-yy"))
            require(day.toString==raw("effective_date"))
            if(fam=="REV") require(r(7)=="Unknown") else require(decode(r(7),"DOC1:")==raw("event_id"))
            require(decode(r(34),"RULE1:")=="1")
            val rule=Map("EXP"->("VA_PUBLIC_EXPENSE","VA_EXPENSE_OBJECT"),"REV"->("VA_PUBLIC_REVENUE","VA_REVENUE_SOURCE"),"PO"->("VA_PO_BUILTIN","PO_QUANTITY_PRICE"))(fam)
            require(r(29)==rule._1 && r(30)==rule._2,"Incorrect retained rule identity")
          }
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
      sjesByYear(year)=yearRows.toVector
    }
    require(actual.toMap==expected,"Financial event signature multiset differs after TTL/posting")
    if(requireLineage) require(located.keySet==byLocator.keySet && located.values.forall(_==2),"Expected two SJE legs per exact source record")
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
    baseline.foreach { old =>
      require(new File(old,"PILOT_COMPLETE").isFile,"Incomplete baseline")
      val ignored=Set(7,29,30,34,35)
      def projection(file: File): Vector[String] = {
        val in=new ReportingSupport.Input(file,"verification",false); val rows=mutable.ArrayBuffer[String]()
        try { var next=in.next(); while(next.nonEmpty) {
          require(rows.size<ComponentPilot.MaxRows && next.get.size==39)
          rows+=next.get.zipWithIndex.filterNot(x=>ignored(x._2)).map(_._1).mkString(","); next=in.next()
        } } finally in.close()
        rows.sorted.toVector
      }
      Vector("03","04").foreach { y =>
        val relative=s"output/FY$y/SortedJEFY${y}merged.csv"
        require(projection(new File(root,relative))==projection(new File(old,relative)),s"Financial SJE projection changed: FY$y")
        Vector(s"LDGR20$y.csv",s"GL20$y.csv").foreach { n=>
          require(ComponentPilot.sha(new File(root,s"output/FY$y/$n"))==ComponentPilot.sha(new File(old,s"output/FY$y/$n")),s"Baseline artifact changed: FY$y/$n")
        }
        val dimensions=Vector("agency"->12,"nominal_account_id"->16,"fund"->13,"object_id"->14,"fiscal_year"->22,"dir_flag"->31)
        def groupKey(r: Row): Vector[String] = dimensions.map(x=>r(x._1))
        val oldPerm=read(new File(old,s"output/FY$y/PERM_TBL20$y.csv")).map(r=>groupKey(r)->r("row_count")).toMap
        val newPerm=read(new File(root,s"output/FY$y/PERM_TBL20$y.csv"))
        require(newPerm.map(r=>groupKey(r)->r("row_count")).toMap==oldPerm,"Permutation financial populations changed")
        newPerm.foreach { r=>
          val sourceRows=sjesByYear(y).filter(s=>dimensions.map(x=>s(x._2).trim)==groupKey(r))
          require(sourceRows.size==r("row_count").toInt)
          Vector("d_transOriginalDocID"->7,"d_transExtensionIDSource"->35).foreach { case(name,col)=>
            require(r.contains(name) && r(name).toInt==sourceRows.map(_(col).trim).distinct.size,s"Incorrect retained-provenance cardinality: $name")
          }
        }
      }
    }
    val p=ReportingSupport.writer(checks,"pilot_checks.csv",Seq("check","status","detail"))
    try {
      ReportingSupport.row(p,"generation_context_and_identity","PASS","36 events; shared supplier across two components; background separate")
      ReportingSupport.row(p,"financial_event_multiset","PASS","30 financial source events; 60 balanced SJE rows; exact known totals by year/family")
      ReportingSupport.row(p,"dated_master_quiet_change","PASS","SMALL FY2003; LARGE FY2004; change after last transaction")
      ReportingSupport.row(p,"employee_cutoff","PASS","six responsibility records; 35 available evidence rows at 2004-01-20")
      ReportingSupport.row(p,"explicit_sje_source_event_lineage",if(requireLineage) "PASS" else "PARTIAL",
        if(requireLineage) "30 exact raw locators; two SJE legs each; original documents and rule versions verified; snapshot-scoped"
        else "Legacy check: downstream signature multiset only; explicit lineage not assessed")
      ReportingSupport.row(p,"unchanged_baseline_financial_outputs",if(baseline.nonEmpty) "PASS" else "NOT_TESTED",
        if(baseline.nonEmpty) "Only declared SJE metadata differs; ledger/GL byte-identical; permutation populations stable and new provenance cardinalities verified" else "No baseline root supplied")
      ReportingSupport.row(p,"va_financial_anchor","NOT_VERIFIED","envelopes synthetic; no FY2003/04 ACFR tie-out")
      ReportingSupport.row(p,"scale_and_ai_effectiveness","NOT_TESTED","bounded fixture only; no model evaluated")
    } finally p.close()
    println(s"PASS component pilot financial_rows=$sjeRows generated_events=${evidence.size} cutoff_rows=$cutoffRows; limitations in validation/pilot_checks.csv")
  }
  def main(args: Array[String]): Unit = {
    val p=org.universalledger.foundation.va.ledger.LedgerApp.parseArgs(args)
    inspect(new File(p("root")),p.getOrElse("requireLineage","false")=="true",p.get("baselineRoot").map(new File(_)))
  }
}
