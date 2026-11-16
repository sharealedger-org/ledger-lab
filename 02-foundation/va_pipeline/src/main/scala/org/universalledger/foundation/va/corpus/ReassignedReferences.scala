package org.universalledger.foundation.va.corpus
import java.io.{File,PrintWriter}
import java.nio.file.{Files,StandardCopyOption}
import org.universalledger.foundation.va.ledger.ReportingSupport._
/** Sorted/coalesced fictional reference labels preserve selected accounting keys without real actor names. */
object ReassignedReferences {
  val tables=Map("agency"->("aRefAgencyCSV.txt","101"),"fund"->("aRefFundCSV.txt","201"),"object"->("aRefObjectCSV.txt","301"))
  def name(kind:String,key:String):String=if(key==tables(kind)._2)Map("agency"->"SIMULATED AGENCY","fund"->"SIMULATED FUND","object"->"SIMULATED OBJECT, SERVICES")(kind)else"Fictional "+kind.capitalize+" "+key
  def augment(entity:File,out:File,work:File,metrics:PrintWriter,chunk:Int):Unit={
    val raw=new File(work,"reassigned_reference_keys.csv");val p=new PrintWriter(raw,"UTF-8")
    try PhaseTwoCheck.raw(entity){r=>if(r(17)=="reassigned_activity")Vector("agency"->r(14),"fund"->r(15),"object"->r(16)).foreach{case(kind,key)=>if(key!=tables(kind)._2)row(p,CorpusExternalSort.key(kind,key),kind,key)}}finally IdentityAudit.close(p)
    val sorted=new File(work,"reassigned_references_sorted.csv");CorpusExternalSort(raw,sorted,chunk,metrics)
    val writers=tables.map{case(kind,(file,_))=>
      val input=new Input(new File(out,file),"base_reference",true);val target=new File(out,file+".augmented");val writer0=writer(out,target.getName,input.header)
      try {var n=input.next();while(n.nonEmpty){writer0.println(csv(n.get));n=input.next()}}finally input.close()
      kind->(writer0,target,new File(out,file))
    }
    var previous=""
    try PhaseTwoCheck.raw(sorted){r=>if(r(0)!=previous){
      val kind=r(1);val key=r(2);val values=kind match {
        case "agency"=>Vector(key,key,name(kind,key))
        case "fund"=>Vector(key,key,name(kind,key))
        case "object"=>Vector(key,"99","FICTIONAL",key,name(kind,key))
      };writers(kind)._1.println(csv(values));previous=r(0)
    }}finally writers.values.foreach(x=>IdentityAudit.close(x._1))
    writers.values.foreach{case(_,fresh,original)=>Files.move(fresh.toPath,original.toPath,StandardCopyOption.REPLACE_EXISTING)}
  }
}
