package org.universalledger.foundation.va.ledger

import java.io.File
import java.nio.charset.StandardCharsets
import scala.collection.mutable.ArrayBuffer

/** Incremental output of multiple ARE adapters into the established financial
  * sort contract. Runs contain canonical SJEs, not encoded wrapper copies.
  * Only run metadata survives a flush; each merge opens at most fanIn readers.
  */
final class SharedJournalSort(out:File,spec:SortEngine.SortSpec,scratch:File,
    chunkRows:Int=100000,byteCap:Long=32L*1024*1024,fanIn:Int=16) {
  require(chunkRows>0 && byteCap>0 && fanIn>=2 && fanIn<=128)
  require(!out.exists() && !scratch.exists() && scratch.mkdirs())
  private case class Run(file:File,rows:Long,bytes:Long)
  private val chunk=ArrayBuffer.empty[String]
  private var runs=Vector.empty[Run]
  private var bufferedBytes=0L
  private var inputRows=0L
  private var inputBytes=0L
  private var initialSpills=0
  private var levels=0
  private var materializedBytes=0L
  private var peakBufferBytes=0L
  private var finished=false
  private val started=System.nanoTime()
  def rows:Long=inputRows
  def add(row:String):Unit={
    require(!finished && row.nonEmpty && !row.exists(c=>c=='\n'||c=='\r'),"Invalid shared-sort record")
    val bytes=row.getBytes(StandardCharsets.UTF_8).length.toLong+1
    require(bytes<=byteCap,"Shared-sort record exceeds byte cap")
    if(chunk.nonEmpty && (chunk.size>=chunkRows || bufferedBytes+bytes>byteCap))flush()
    chunk+=row;bufferedBytes+=bytes;inputRows+=1;inputBytes+=bytes
    peakBufferBytes=peakBufferBytes.max(bufferedBytes)
  }
  private def flush():Unit=if(chunk.nonEmpty){
    require(initialSpills<100000,"Shared-sort run metadata bound exceeded; partition the workload")
    val file=new File(SortEngine.spillChunk(SortEngine.sortChunk(chunk.toVector,spec),scratch.getPath,initialSpills))
    require(file.length()==bufferedBytes,"Shared-sort spill byte conservation")
    runs :+= Run(file,chunk.size,bufferedBytes)
    initialSpills+=1;materializedBytes+=bufferedBytes;chunk.clear();bufferedBytes=0
  }
  private def merge(group:Vector[Run],file:File):Run={
    val count=SortEngine.mergeSpills(group.map(_.file.getPath),file.getPath,spec)
    require(count==group.map(_.rows).sum && file.length()==group.map(_.bytes).sum,"Shared-sort merge conservation")
    materializedBytes+=file.length()
    group.foreach(r=>require(r.file.delete(),"Unable to retire consumed shared-sort run"))
    Run(file,count,file.length())
  }
  def finish(metrics:java.io.PrintWriter):Long={
    require(!finished);flush()
    while(runs.size>fanIn){
      levels+=1
      runs=runs.grouped(fanIn).zipWithIndex.map{case(group,i)=>merge(group,new File(scratch,s"level_${levels}_$i.csv"))}.toVector
    }
    levels+=1
    val result=merge(runs,out)
    require(result.rows==inputRows && result.bytes==inputBytes && scratch.delete())
    finished=true
    ReportingSupport.row(metrics,out.getName,inputRows,inputBytes,result.bytes,initialSpills,levels,fanIn,chunkRows,
      materializedBytes,(System.nanoTime()-started)/1e9,peakBufferBytes,byteCap)
    result.rows
  }
}
