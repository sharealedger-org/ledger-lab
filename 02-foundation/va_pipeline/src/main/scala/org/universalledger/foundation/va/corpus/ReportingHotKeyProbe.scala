package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.nio.file.Files
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport,SortEngine,post}
import scala.collection.mutable

/** Source fixture preparation and post-engine planning analysis only; report engine unchanged. */
object ReportingHotKeyProbe {
  private def read(f: File): Vector[Map[String,String]] = {
    val rows=mutable.ArrayBuffer[Map[String,String]]()
    ConnectedViewsCheck.scan(f) { r=>require(rows.size<4096); rows+=r }; rows.toVector
  }
  private def metric(f: File): Map[String,String] = read(f).map(r=>r("metric")->r("value")).toMap
  private def files(dir: File): Vector[File] = Option(dir.listFiles()).getOrElse(Array.empty[File]).toVector.flatMap(f=>if(f.isDirectory) files(f) else Vector(f))
  def prepare(out: File, movementRows: Int, shape: String): Unit = {
    require(!out.exists() && out.mkdirs()); require(movementRows>=1000 && movementRows<=2000000 && movementRows%2==0)
    require(Set("repetitive","matched","diverse")(shape))
    val input=new File(out,"input"); input.mkdirs()
    val pairs=movementRows/2
    val contexts=shape match { case "repetitive"=>32; case "matched"=>math.max(32,movementRows/50); case "diverse"=>pairs }
    val rows=(0 until pairs).iterator.flatMap { n=>
      val group=n%contexts; val month=group%12+1; val year=if(month>=7) 2011 else 2012
      val event=if(group%50==0) "REVENUE_RECEIPT" else "EXPENSE_PAYMENT"
      val a=Array.fill(39)("0")
      a(0)="0000000000"; a(3)=s"PROBE-$n"; a(4)="1"; a(5)=event; a(6)=if(event=="REVENUE_RECEIPT") "Revenue Sys" else "Payment Sys"
      a(7)=s"PROBE-DOC-$n"; a(8)="SIMULATED HOT KEY"; a(9)="ACTUALS"; a(10)="FIN"; a(11)="BOOK"
      a(12)=f"A${group%128}%03d"; a(13)=f"F${group%8}%02d"; a(14)=f"$group%07d"
      a(16)=(if(event=="REVENUE_RECEIPT") "REV" else "EXP")+f"${group%64}%02d"
      a(18)="USD"; a(19)="TXN"; a(20)="USD"; a(21)="BASE"; a(22)=f"$year%04d/$month%02d"
      a(23)=f"$year%04d/$month%02d/01"; a(24)=a(23); a(25)="1.25"; a(29)="PROBE_RULES"; a(30)="PROBE_PAIR"
      a(31)="D"; a(32)="N"; a(33)="N"; a(34)="RULE1:MQ"; a(35)=s"PROBE-SOURCE-$n"
      val b=a.clone(); b(4)="2"; b(13)="0000"; b(14)="0000"; b(16)="0000"; b(25)="-1.25"; b(31)="O"
      Iterator(a.mkString(","),b.mkString(","))
    }
    val spec=SortEngine.loadSpec("../sort_specs/SortedJE.sortspec")
    val sorted=new File(input,"SortedJEFY12merged.csv")
    val result=SortEngine.externalSort(rows,sorted.getPath,spec,50000,new File(out,"sort").getPath)
    require(result._1==movementRows && result._2<=64)
    post(input.getPath+"/",input.getPath+"/","12")
    val meta=new File(input,"metrics"); meta.mkdirs()
    val manifest=ReportingSupport.writer(meta,"run_manifest.csv",Seq("run_id","experiment_id","status"))
    try ReportingSupport.row(manifest,out.getName,"posted-sje-hot-key-fixture","complete") finally manifest.close()
    val assignment=ReportingSupport.writer(out,"assignments.csv",Seq("vendor_id","template_instrument_id")); assignment.close()
    val config=ReportingSupport.writer(out,"probe_config.csv",Seq("metric","value"))
    try {
      ReportingSupport.row(config,"movement_rows",movementRows); ReportingSupport.row(config,"source_pairs",pairs)
      ReportingSupport.row(config,"contexts",contexts); ReportingSupport.row(config,"shape",shape)
      ReportingSupport.row(config,"sort_spills",result._2); ReportingSupport.row(config,"source_bytes",sorted.length())
    } finally config.close()
    Files.write(new File(out,"FIXTURE_COMPLETE").toPath,"posted-sje-fixture-not-ttl-cost-point\n".getBytes("UTF-8"))
  }
  def inspect(root: File): Unit = {
    ConnectedViewsCheck.inspect(new File(root,"report"))
    val config=metric(new File(root,"probe_config.csv")); val report=new File(root,"report")
    val extract=metric(new File(report,"extract_metrics.csv")); val syn=metric(new File(report,"synthetic_metrics.csv")); val reduce=metric(new File(report,"reduce_metrics.csv"))
    val coverage=read(new File(report,"synthetic_vendor_coverage.csv")); require(coverage.size==1 && coverage.head("attribution")=="unattributed")
    require(coverage.head("vendor_id")=="0000000000" && coverage.head("template_instrument_id").isEmpty)
    val movements=config("movement_rows").toLong; val total=extract("records_read").toLong
    require(total==syn("financial_rows").toLong && total>movements && extract("vendors")=="1")
    val inputBytes=read(new File(report,"input_manifest.csv")).filter(r=>Set("movement","balance")(r("kind"))).map(_("bytes").toLong).sum
    val p=ReportingSupport.writer(root,"measurement.csv",Seq("case_id","shape","movement_rows","balance_rows","financial_rows","extract_seconds","reduce_seconds","order_seconds","input_bytes","spill_write_bytes","report_retained_bytes","merge_passes"))
    try ReportingSupport.row(p,root.getName,config("shape"),movements,total-movements,total,extract("elapsed_seconds"),reduce("elapsed_seconds"),extract("order_preparation_seconds"),inputBytes,
      extract("aggregate_spill_bytes").toLong+extract("spill_bytes").toLong+syn("partial_spill_bytes").toLong,
      files(report).map(_.length()).sum,extract("aggregate_merge_passes").toLong+syn("partial_merge_passes").toLong) finally p.close()
  }
  def project(root: File, audit: File): Unit = {
    val cases=Option(root.listFiles()).getOrElse(Array.empty[File]).filter(f=>f.isDirectory && new File(f,"measurement.csv").isFile).toVector.sortBy(_.getName)
    require(cases.size==3,"All named profiles required")
    val measurements=cases.map(f=>read(new File(f,"measurement.csv")).head)
    val summary=metric(new File(audit,"summary/summary.csv"))
    val parser=read(new File(audit,"parser_summary/parser_lookup_summary.csv"))
    def count(family: String,classification: String): Long = parser.filter(r=>r("family")==family && r("classification")==classification).map(_("source_rows").toLong).sum
    val unmatched=count("expense","still_unattributed")*2
    val revenue=parser.filter(_("family")=="revenue").map(_("source_rows").toLong).sum*2
    val allApa=parser.map(_("source_rows").toLong).sum*2
    val canonical=metric(new File(audit,"canonical/audit_metrics.csv"))
    val scenarios=Vector(
      ("historical_audited",summary("zero_key_financial_input_rows").toLong,"measured_historical_not_future_count"),
      ("unmatched_expense_all_revenue_zero_same_zero_balance",unmatched+revenue+summary("zero_key_posted_rows").toLong,"scenario_balance_count_assumed"),
      ("unmatched_expense_all_revenue_zero_all_old_balances",unmatched+revenue+canonical("canonical_posted_rows").toLong,"conservative_balance_sensitivity"),
      ("all_old_accepted_APA_zero_all_old_balances",allApa+canonical("canonical_posted_rows").toLong+58,"old_balance_sensitivity_not_current_ceiling"),
      ("all_old_accepted_APA_zero_one_balance_per_movement",allApa*2+100,"conditional_extreme_same_accepted_population_no_opening_no_new_sources"))
    val p=ReportingSupport.writer(root,"zero_key_projection.csv",Seq("scenario","target_financial_rows","count_status","profile","measured_rows","measured_seconds","row_rate_extrapolated_hours","merge_depth_sensitivity_hours","two_input_pass_read_gib","cumulative_spill_write_gib","retained_report_gib","interpretation"))
    try scenarios.foreach { case(name,n,status)=>measurements.foreach { r=>
      val scale=BigDecimal(n)/BigDecimal(r("financial_rows")); val seconds=BigDecimal(r("extract_seconds"))+BigDecimal(r("reduce_seconds"))
      val hours=seconds*scale/3600
      ReportingSupport.row(p,name,n,status,r("shape"),r("financial_rows"),seconds,hours.setScale(3,BigDecimal.RoundingMode.HALF_UP),
        (hours*2).setScale(3,BigDecimal.RoundingMode.HALF_UP),
        (BigDecimal(r("input_bytes"))*scale*2/BigDecimal(1073741824)).setScale(3,BigDecimal.RoundingMode.HALF_UP),
        (BigDecimal(r("spill_write_bytes"))*scale/BigDecimal(1073741824)).setScale(3,BigDecimal.RoundingMode.HALF_UP),
        (BigDecimal(r("report_retained_bytes"))*scale/BigDecimal(1073741824)).setScale(3,BigDecimal.RoundingMode.HALF_UP),
        "conditional_proxy_not_upper_bound_not_full_run;2x_merge_depth_sensitivity_not_calibrated;write_bytes_not_peak_scratch")
    } } finally p.close()
  }
  def main(args: Array[String]): Unit = {
    val p=LedgerApp.parseArgs(args)
    p("mode") match {
      case "prepare"=>prepare(new File(p("out")),p("movementRows").toInt,p("shape"))
      case "inspect"=>inspect(new File(p("out")))
      case "project"=>project(new File(p("out")),new File(p("audit")))
    }
  }
}
