package org.universalledger.foundation.va.corpus
import java.io.File
import java.nio.file.{Files,StandardCopyOption}
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}
import scala.collection.mutable
/** Post-report aggregate calibration only. Does not replay financial processing or infer actors. */
object PhaseTwoCalibration {
  val header=Vector("year","family","source_year","source_net_usd","source_nominal_rows","scale_fraction","synthetic_envelope_cents","rounding_residual_usd","scope","basis","currency","source_sha256")
  def prepare(source:File,target:File):Unit={
    require(source.isFile && !target.exists());val before=source.length();val modified=source.lastModified()
    val in=new ReportingSupport.Input(source,"completed_report_calibration",true)
    require(in.header==Vector("side","fiscal_year","agency","fund","account","currency","amount","rows","contributors"))
    val amounts=mutable.Map[(Int,String),BigDecimal]().withDefaultValue(BigDecimal(0))
    val counts=mutable.Map[(Int,String),Long]().withDefaultValue(0L)
    val nets=mutable.Map[String,BigDecimal]().withDefaultValue(BigDecimal(0));var selected=0L
    var hash="";var rows=0L
    try {var next=in.next();while(next.nonEmpty){
      val r=next.get;require(r.size==in.header.size)
      if(r(0)=="movement"){
        require(nets.contains(r(5)) || nets.size<16,"Currency scope bound")
        val amount=BigDecimal(r(6));nets(r(5))+=amount
        val fy=r(1).toInt;val family=if(r(4).startsWith("EXP"))"EXP" else if(r(4).startsWith("REV"))"REV" else ""
        if(family.nonEmpty && fy>=2003 && fy<=2016){
          require(r(5)=="USD","Do not mix currencies in USD envelopes")
          val k=(fy,family);amounts(k)+=amount;counts(k)+=r(7).toLong;selected+=1
        }
      }
      next=in.next()
    };hash=in.hash;rows=in.count}finally in.close()
    require(source.length()==before && source.lastModified()==modified,"Calibration input changed")
    require(nets.values.forall(_==0),"Completed movement report does not balance across nominal and offset accounts")
    require(amounts.size==28 && amounts.values.forall(_>0),"Require positive EXP/REV net totals for every FY2003–16")
    target.getParentFile.mkdirs();val attempt=new File(target.getPath+".attempt");require(!attempt.exists())
    val p=ReportingSupport.writer(attempt.getParentFile,attempt.getName,header)
    try amounts.toVector.sortBy(_._1).foreach{case((fy,family),amount)=>
      val share=BigDecimal("0.01")
      val cents=(amount*share*100).setScale(0,BigDecimal.RoundingMode.HALF_UP).bigDecimal.longValueExact()
      val residual=amount*share-BigDecimal(cents)/100
      ReportingSupport.row(p,fy+28,family,fy,amount,counts((fy,family)),share,cents,residual,"accepted-report-movements-all-agencies-funds","nominal-net-movement-not-official-statement","USD",hash)
    }finally IdentityAudit.close(p)
    Files.move(attempt.toPath,target.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"PASS calibration report_rows=$rows selected_groups=$selected envelope_rows=${amounts.size} source_sha256=$hash no_actor_relationship_inference=true")
  }
  /** Small frozen summary table. Never loads transaction history. */
  final class Budgets(c:PhaseTwoCorpus.Config, file:Option[File],reassigned:Map[Int,Long]=Map.empty) {
    private val annual=mutable.Map[(Int,String),Long]()
    private val denominators=mutable.Map[(Int,String),BigInt]()
    private val remainders=mutable.Map[(Int,String),Long]()
    private def denominator(year:Int,family:String):BigInt=denominators.getOrElseUpdate(year->family,(1 to c.components).foldLeft(BigInt(0))((sum,i)=>sum+ComponentVariability.weight(c,i,year,family)))
    file.foreach {f=>
      require(f.length()<=65536)
      val in=new ReportingSupport.Input(f,"annual_calibration",true);require(in.header==header)
      try {var n=in.next();while(n.nonEmpty){
        val r=n.get;require(r.size==header.size && annual.size<28)
        val key=(r(0).toInt,r(1));require(!annual.contains(key) && Set("EXP","REV")(r(1)) && r(10)=="USD")
        val cents=r(6).toLong;require(cents>=c.components.toLong*1200 && cents<=1000000000000000L)
        annual(key)=cents;n=in.next()
      }}finally in.close()
      (c.first to c.last).foreach(y=>Vector("EXP","REV").foreach(family=>require(annual.contains(y->family),"Missing annual calibration")))
    }
    def total(year:Int,family:String):Long=annual.getOrElse(year->family,(if(family=="EXP")c.expense else if(family=="REV")c.receipt else c.commitment)*c.components)
    def wholeTotal(year:Int,family:String):Long={val value=total(year,family)-(if(family=="EXP")reassigned.getOrElse(year,0L)else 0L);require(value>=c.components.toLong*1200,"Reassigned sample leaves insufficient wholly-generated budget");value}
    def component(year:Int,family:String,index:Int):Long={
      val value=wholeTotal(year,family)
      if(!c.variable)value/c.components+(if(index<=value%c.components)1 else 0)
      else {
        val sum=denominator(year,family)
        def base(i:Int):Long=(BigInt(value)*ComponentVariability.weight(c,i,year,family)/sum).toLong
        val remainder=remainders.getOrElseUpdate(year->family,value-(1 to c.components).foldLeft(0L)((s,i)=>s+base(i)))
        base(index)+(if(index<=remainder)1 else 0)
      }
    }
    val calibrated:Boolean=file.nonEmpty
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);prepare(new File(p("report")),new File(p("out")))}
}
