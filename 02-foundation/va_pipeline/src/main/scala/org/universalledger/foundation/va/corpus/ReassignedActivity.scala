package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.{Files,StandardCopyOption}
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}
import scala.collection.mutable

/** Bounded read-only sampling of accepted zero-ID expense activity; supplier assignments remain fictional. */
object ReassignedActivity {
  import ReportingSupport._
  val header=Vector("source_year","agency","fund","object","source_date","amount_cents","source_locator","source_snapshot_sha256","source_description","selection_key","source_period","object_category")
  private val dateFormats=Vector("M/d/uuuu","M-d-uuuu","M/d/yy","M-d-yy").map(DateTimeFormatter.ofPattern)
  def sourceDate(s:String):Option[LocalDate]=dateFormats.iterator.flatMap(f=>try Some(LocalDate.parse(s,f))catch{case _:java.time.format.DateTimeParseException=>None}).take(1).toVector.headOption
  def blockedObjects(file:File):Set[String]={
    require(file.length()<=8L*1024*1024);val in=new Input(file,"object_reference",true)
    val key=in.header.indexOf("OBJ_OBJECT_KEY");val name=in.header.indexOf("4-DIGIT OBJECT NAME");require(key>=0 && name>=0)
    val blocked=mutable.Set[String]()
    try {var n=in.next();while(n.nonEmpty){require(in.count<=10000);val r=n.get;require(r.size==in.header.size)
      val label=r(name).toUpperCase(java.util.Locale.ROOT)
      if(Vector("SALAR","WAGE","PAYROLL","PERSONNEL","PERSONAL SERVICES","EMPLOYEE","BENEFIT","BURDEN","TRANSFER","DEBT SERVICE").exists(label.contains))blocked+=r(key)
      n=in.next()
    }}finally in.close();blocked.toSet
  }
  case class Selected(priority:Long,fields:Vector[String],bytes:Int)
  private implicit val selectedOrder:Ordering[Selected]=new Ordering[Selected]{def compare(a:Selected,b:Selected):Int={val p=java.lang.Long.compareUnsigned(a.priority,b.priority);if(p!=0)p else a.fields(6).compareTo(b.fields(6))}}
  def extract(sourceRoot:File,years:Vector[Int],objectRef:File,out:File,seed:String,cap:Int):Unit={
    require(cap>=1 && cap<=20000 && years.nonEmpty && years.distinct.size==years.size && years.forall(y=>y>=2003&&y<=2016))
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val blocked=blockedObjects(objectRef)
    val categories=mutable.Map[String,String]()
    ConnectedViewsCheck.scan(objectRef){r=>require(categories.size<10000);categories(r("OBJ_OBJECT_KEY"))=r.getOrElse("2-DIGIT OBJECT CODE","UNCLASSIFIED")}
    val p=writer(attempt,"selected.csv",header)
    val manifest=writer(attempt,"source_manifest.csv",Seq("source_year","path","bytes","sha256","journal_rows","eligible_rows","selected_rows","selected_net_cents","max_sample_bytes"))
    val controls=writer(attempt,"selection_controls.csv",Seq("source_year","reason","rows"))
    try years.foreach { year=>
      val yy=f"${year%100}%02d";val file=new File(sourceRoot,s"FY$yy/SortedJEFY${yy}merged.csv")
      val size=file.length();val modified=file.lastModified();require(file.isFile)
      var committed=0;ConnectedViewsCheck.scan(new File(sourceRoot,s"FY$yy/metrics/run_manifest.csv")){r=>require(r("status")=="complete");committed+=1};require(committed>0)
      val in=new Input(file,"retained_canonical",false,literalCanonical=true)
      val sample=mutable.PriorityQueue[Selected]();val counts=mutable.Map[String,Long]().withDefaultValue(0L)
      val digest=MessageDigest.getInstance("SHA-256");var bytes=0L;var maxBytes=0L;var hash=""
      try {var next=in.next();while(next.nonEmpty){val r=next.get;require(r.size==39)
        val reason=if(!r(0).forall(_=='0') || r(0).isEmpty)"known_or_other_identity"
          else if(r(31)!="D" || r(6)!="Payment Sys" || !r(16).startsWith("EXP"))"not_zero_expense_nominal"
          else if(blocked(r(14)))"excluded_object_class"
          else if(IdentityAuditSummary.candidateClass(r(8))!="source_name_candidate_not_legal_identity")"excluded_summary_system_or_generic_label"
          else if(r(8).length>512 || r(12).length>128 || r(13).length>128 || r(14).length>128 || r(35).length>2048)"oversized_source_payload"
          else if(!r(35).startsWith("SRC1:"))"missing_source_locator"
          else {
            sourceDate(r(24)) match {
              case None=>"unparseable_source_date"
              case Some(d) if SyntheticCalendar.fiscalYear(d)!=year=>"date_outside_source_fiscal_year"
              case Some(d)=>
                val cents=(BigDecimal(r(25))*100).bigDecimal.longValueExact()
                if(cents==0)"zero_amount" else {
                  val priority=ByteBuffer.wrap(digest.digest((seed+"|reused-selection|"+year+"|"+r(35)).getBytes("UTF-8"))).getLong
                  require(r(22).matches("[0-9]{4}/[0-9]{2}"),"Reassignment requires declared monthly posting period")
                  val fields=Vector(year.toString,r(12),r(13),r(14),d.toString,cents.toString,r(35),"",r(8),java.lang.Long.toUnsignedString(priority),r(22),categories.getOrElse(r(14),"UNCLASSIFIED"))
                  val candidate=Selected(priority,fields,fields.map(_.getBytes("UTF-8").length).sum)
                  if(sample.size<cap || selectedOrder.compare(candidate,sample.head)<0){
                    if(sample.size==cap)bytes-=sample.dequeue().bytes
                    sample.enqueue(candidate);bytes+=candidate.bytes;maxBytes=maxBytes.max(bytes)
                    require(bytes<=64L*1024*1024,"Sample byte bound")
                  }
                  "eligible"
                }
            }
          }
        counts(reason)+=1;next=in.next()
      };hash=in.hash}finally in.close()
      require(file.length()==size && file.lastModified()==modified,"Observed source changed")
      val selected=sample.dequeueAll.reverse
      require(selected.nonEmpty,"No eligible source records in requested year")
      val total=selected.foldLeft(BigInt(0))((s,r)=>s+BigInt(r.fields(5)))
      selected.foreach(r=>p.println(csv(r.fields.updated(7,hash))))
      row(manifest,year,file.getCanonicalPath,size,hash,in.count,counts("eligible"),selected.size,total,maxBytes)
      counts.toVector.sortBy(_._1).foreach{case(reason,n)=>row(controls,year,reason,n)}
      println(s"PASS reused_selection source_year=$year eligible=${counts("eligible")} selected=${selected.size} sample_bytes=$maxBytes")
    }finally Seq(p,manifest,controls).foreach(IdentityAudit.close)
    Files.write(new File(attempt,"REUSED_SELECTION_COMPLETE").toPath,("reassigned_activity_v1\nseed="+seed+"\ncap_per_year="+cap+"\noriginal_data_unchanged\n").getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);extract(new File(p("sourceRoot")),p("years").split(",").toVector.map(_.toInt),new File(p("objects")),new File(p("out")),p.getOrElse("seed","phase2-v1"),p.getOrElse("cap","1000").toInt)}
}
