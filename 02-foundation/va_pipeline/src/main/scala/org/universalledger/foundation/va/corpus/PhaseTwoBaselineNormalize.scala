package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.nio.file.{Files,StandardCopyOption}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport,SortEngine}

/** Structural SJE preparation before posting; one bounded source journal group at a time. */
object PhaseTwoBaselineNormalize {
  import ReportingSupport._
  val Version="phase2-historical-normalization-v1"
  val canonicalDate=DateTimeFormatter.ofPattern("yy/MM/dd")
  def date(s:String):Option[LocalDate]={
    val parsed=if(s.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}"))
      try Some(LocalDate.parse(s,canonicalDate))catch{case _:java.time.DateTimeException=>None}
    else ReassignedActivity.sourceDate(s).orElse(try Some(LocalDate.parse(s))catch{case _:java.time.DateTimeException=>None})
    parsed
  }
  def shiftedDate(s:String):String=date(s).map { d=>
    val target=d.plusYears(28)
    require(target.minusYears(28)==d,"Non-reversible synthetic calendar shift")
    if(s.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}"))target.format(canonicalDate)
    else if(s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))target.toString
    else target.format(DateTimeFormatter.ofPattern("M/d/uuuu"))
  }.getOrElse(s)
  def period(s:String):String={require(s.matches("[0-9]{4}(/[0-9]{2})?"));f"${s.take(4).toInt+28}%04d"+s.drop(4)}
  def prepare(source:File,objects:File,sortSpec:File,out:File,year:Int,seed:String="phase2-v1",population:String=PhaseTwoCorpus.DefaultPopulation,chunk:Int=100000):Unit={
    require(year>=2003 && year<=2016 && objects.length()<=8L*1024*1024)
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    val wrapped=new File(attempt,"source_order_input.csv");val wp=new PrintWriter(wrapped,"UTF-8")
    val input=new Input(source,"historical_canonical",false,literalCanonical=true);var count=0L
    try{var n=input.next();while(n.nonEmpty){val r=n.get;require(r.size==39 && r(35).startsWith("SRC1:") && r(35).length<=2048)
      val encoded=Base64.getEncoder.encodeToString(r.mkString(",").getBytes("UTF-8"))
      row(wp,CorpusExternalSort.key(r(35)),encoded);count+=1;n=input.next()
    }}finally{input.close();IdentityAudit.close(wp)}
    val sorted=new File(attempt,"source_order_sorted.csv");CorpusExternalSort(wrapped,sorted,chunk,metrics)
    val financial=new File(attempt,"financial_order_input.csv");val fp=new PrintWriter(financial,"UTF-8")
    val spec=SortEngine.loadSpec(sortSpec.getPath);require(spec.map(_.colIndex)==Vector(0)++(9 to 22).toVector)
    val exit=new HistoricalJournalExit(objects,attempt,year,seed,population)
    var group=Vector.empty[Vector[String]];var key="";var actual=0L
    def flush():Unit=if(group.nonEmpty){
      exit(group).foreach {named=>
        val record=named.mkString(",");val order=SortEngine.extractKey(record,spec).keyStr.iterator.map(c=>f"${c.toInt}%04x").mkString
        fp.println(order+","+record);actual+=1
      }
      group=Vector.empty
    }
    try{PhaseTwoCheck.raw(sorted){r=>
      require(r.size==2);if(key!=r(0)){flush();key=r(0)}
      require(group.size<2,"Source journal group bound exceeded")
      group :+= new String(Base64.getDecoder.decode(r(1)),"UTF-8").split(",",-1).toVector
    };flush()}finally{IdentityAudit.close(fp);exit.close()}
    // Attempt-local wrappers are restart scratch, not published corpus records.
    require(wrapped.delete() && sorted.delete(),"Unable to retire consumed source-order scratch")
    val ordered=new File(attempt,"financial_order_sorted.csv")
    try CorpusExternalSort(financial,ordered,chunk,metrics)finally IdentityAudit.close(metrics)
    val result=new File(attempt,f"SortedJEFY${(year+28)%100}%02dmerged.csv");val output=new PrintWriter(result,"UTF-8")
    val lines=scala.io.Source.fromFile(ordered,"UTF-8")
    try lines.getLines().foreach(l=>output.println(l.substring(l.indexOf(',')+1)))finally{lines.close();IdentityAudit.close(output)}
    require(actual==count)
    require(financial.delete() && ordered.delete(),"Unable to retire consumed financial-order scratch")
    require(exit.rows==actual)
    exit.writeControls()
    Files.write(new File(attempt,"NORMALIZATION_COMPLETE").toPath,(Version+"\nrequires_actual_posting_consumer\n").getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);prepare(new File(p("source")),new File(p("objects")),new File(p("sortSpec")),new File(p("out")),p("year").toInt,p.getOrElse("seed","phase2-v1"),p.getOrElse("population",PhaseTwoCorpus.DefaultPopulation),p.getOrElse("chunk","100000").toInt)}
}
