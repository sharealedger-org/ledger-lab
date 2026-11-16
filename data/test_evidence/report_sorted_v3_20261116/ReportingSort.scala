package org.universalledger.foundation.va.corpus

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files,StandardCopyOption}
import java.security.{DigestInputStream,DigestOutputStream,MessageDigest}
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport,SortEngine}
import scala.collection.mutable

/** Explicit reporting-order materialization. Financial files and their fields remain unchanged. */
object ReportingSort {
  import ReportingSupport._
  private def hex(d:MessageDigest):String=d.digest().map(b=>f"${b&255}%02x").mkString
  def sortFile(source:File,target:File,kind:String,chunk:Int,fan:Int,metrics:PrintWriter):Unit={
    require(Set("movement","balance")(kind) && chunk>0 && chunk<=200000 && fan>=2 && fan<=16)
    val start=System.nanoTime();val size=source.length();val modified=source.lastModified()
    val work=new File(target.getPath+".sort");require(!target.exists() && work.mkdirs())
    val sha=MessageDigest.getInstance("SHA-256")
    val in=new BufferedReader(new InputStreamReader(new DigestInputStream(new FileInputStream(source),sha),StandardCharsets.UTF_8),1048576)
    val spec=Seq(SortEngine.SortField("reporting_tuple",0,"CH","A"))
    val buffer=mutable.ArrayBuffer[String]();val runs=mutable.ArrayBuffer[String]()
    var rows=0L;var header="";var spills=0;var levels=0
    def flush():Unit=if(buffer.nonEmpty){
      require(runs.size<100000,"Sort run allowance exceeded")
      runs+=SortEngine.spillChunk(SortEngine.sortChunk(buffer,spec),work.getPath,spills)
      spills+=1;buffer.clear()
    }
    try {
      if(kind=="balance"){
        header=in.readLine();require(header!=null && IdentityAudit.strict(header).exists(_.reverse.dropWhile(_.isEmpty).reverse==ledgerHeader),"Ledger header mismatch")
      }
      var line=in.readLine()
      while(line!=null){
        require(line.nonEmpty && line.length<=1048576,"Canonical record allowance/empty row")
        val r=line.split(",",-1).toVector
        require(r.size==(if(kind=="movement")39 else 30) || (kind=="balance" && r.size==31 && r.last.isEmpty),"Canonical width")
        buffer+=reportingKey(r,kind)+","+line;rows+=1
        if(buffer.size==chunk)flush()
        line=in.readLine()
      }
    }finally in.close()
    val inputHash=hex(sha);require(source.length()==size && source.lastModified()==modified,"Sort input changed")
    flush();var current=runs.toVector
    while(current.size>1){
      current=current.grouped(fan).zipWithIndex.map{case(group,index)=>
        val merged=new File(work,s"merge_${levels}_$index.csv")
        SortEngine.mergeSpills(group,merged.getPath,spec)
        group.foreach(p=>require(new File(p).delete()))
        merged.getPath
      }.toVector;levels+=1
    }
    val outputDigest=MessageDigest.getInstance("SHA-256")
    val out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(new DigestOutputStream(new FileOutputStream(target),outputDigest),StandardCharsets.UTF_8),1048576))
    var written=0L;var previous=""
    try {
      if(kind=="balance")out.println(header)
      current.foreach{path=>
        val reader=new BufferedReader(new InputStreamReader(new FileInputStream(path),StandardCharsets.UTF_8),1048576)
        try {var line=reader.readLine();while(line!=null){
          val at=line.indexOf(',');require(at>0);val key=line.substring(0,at)
          require(key>=previous,"Reporting sort order");previous=key
          out.println(line.substring(at+1));written+=1;line=reader.readLine()
        }}finally reader.close()
        require(new File(path).delete())
      }
    }finally IdentityAudit.close(out)
    require(rows==written && target.length()==size,"Sort row/byte conservation failed")
    require(work.delete())
    row(metrics,source.getCanonicalPath,target.getParentFile.getName+"/"+target.getName,kind,rows,size,inputHash,hex(outputDigest),spills,levels,chunk,fan,(System.nanoTime()-start)/1e9)
    println(s"RESULT reporting_sort kind=$kind rows=$rows spills=$spills levels=$levels bytes=$size file=${target.getName}")
  }
  def prepare(dirs:Vector[File],out:File,chunk:Int=100000,fan:Int=16):Unit={
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val metrics=writer(attempt,"sort_manifest.csv",Seq("source","output","kind","rows","bytes","source_sha256","sorted_sha256","spills","merge_levels","chunk_rows","fan_in","elapsed_seconds"))
    try dirs.foreach{dir=>
      var complete=0
      ConnectedViewsCheck.scan(new File(dir,"metrics/run_manifest.csv")){r=>require(r("status")=="complete");complete+=1};require(complete>0)
      val files=Option(dir.listFiles()).getOrElse(Array.empty[File]).toVector
      val journal=files.filter(_.getName.matches("SortedJEFY[0-9]{2}merged\\.csv"));val ledger=files.filter(_.getName.matches("LDGR[0-9]{4}\\.csv"))
      require(journal.size==1 && ledger.size==1)
      val year=journal.head.getName.substring(10,12);val target=new File(attempt,"FY"+year);require(target.mkdirs())
      Vector(journal.head->"movement",ledger.head->"balance").foreach{case(f,k)=>sortFile(f,new File(target,f.getName),k,chunk,fan,metrics)}
      val meta=new File(target,"metrics");meta.mkdirs()
      val manifest=writer(meta,"run_manifest.csv",Seq("run_id","experiment_id","status","source"))
      try row(manifest,out.getName,"report-order-materialization-not-canonical-state","complete",dir.getCanonicalPath)finally IdentityAudit.close(manifest)
    }finally IdentityAudit.close(metrics)
    Files.write(new File(attempt,"SORT_COMPLETE").toPath,"literal_vendor_and_financial_dimensions_v1\n".getBytes(StandardCharsets.UTF_8))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);prepare(p("inPaths").split(",").toVector.map(new File(_)),new File(p("out")),p.getOrElse("chunk","100000").toInt,p.getOrElse("fanIn","16").toInt)}
}
