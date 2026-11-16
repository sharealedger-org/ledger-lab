package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.nio.file.{Files,StandardCopyOption}
import java.util.Base64
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}

/** Ordered metadata preparation; no financial posting or inferred real-party relationships. */
object PhaseTwoBaselineDimensions {
  import ReportingSupport._
  def prepare(historical:File,generated:File,normalizations:Vector[File],out:File,chunk:Int=100000,historicalAssignments:Option[File]=None):Unit={
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    val masterHeader={val in=new Input(new File(historical,"VendorMaster.csv"),"master",true);try in.header finally in.close()}
    require(masterHeader.contains("instID") && masterHeader.contains("instHolderName"))
    val reusedFile=new File(generated,"private/reassigned_actor_definitions.csv")
    require(reusedFile.length()<=8L*1024*1024)
    val sampledReused=scala.collection.mutable.Set[String]()
    if(reusedFile.isFile)ConnectedViewsCheck.scan(reusedFile){r=>require(sampledReused.size<10000);sampledReused+=r("vendor_id")}
    val masterRaw=new File(attempt,"master_input.csv");val master=new PrintWriter(masterRaw,"UTF-8")
    val assignmentsRaw=new File(attempt,"assignment_input.csv");val assignments=new PrintWriter(assignmentsRaw,"UTF-8")
    def emit(p:PrintWriter,key:String,r:Vector[String]):Unit=row(p,CorpusExternalSort.key(key),Base64.getEncoder.encodeToString(csv(r).getBytes("UTF-8")))
    def emitMaster(values:Map[String,String]):Unit={val r=masterHeader.map(h=>values.getOrElse(h,""));emit(master,values("instID"),r)}
    try{
      ConnectedViewsCheck.scan(new File(historical,"VendorMaster.csv")){r=>
        val changed=r.map{case(k,v)=>k->(if(k=="instID")StablePartyIdentity.observedVendor(v)
          else if(Set("instEffectDate","instEffectEndDate")(k) && v.nonEmpty && v!="9999-99-99")PhaseTwoBaselineNormalize.shiftedDate(v)else v)}
        emitMaster(changed)
      }
      ConnectedViewsCheck.scan(new File(generated,"VendorMaster.csv")){r=>if(!sampledReused(r("instID")))emitMaster(r)}
      normalizations.foreach {root=>
        require(new File(root,"NORMALIZATION_COMPLETE").isFile)
        ConnectedViewsCheck.scan(new File(root,"actor_candidates.csv")){r=>if(r("generation_class")=="reassigned_activity"){
          val id=r("vendor_id")
          emitMaster(Map("instID"->id,"instHolderName"->("Fictional Supplier "+id),"instEffectDate"->"2030-01-01","instEffectEndDate"->"9999-99-99","instTypeID"->"FICTIONAL_REASSIGNED","instFXCurrency"->"USD"))
          emit(assignments,id,Vector(id,r("template_instrument_id")))
        }}
      }
      ConnectedViewsCheck.scan(historicalAssignments.getOrElse(new File(historical,"synthetic_assignments.csv"))){r=>val id=StablePartyIdentity.observedVendor(r("vendor_id"));emit(assignments,id,Vector(id,r("template_instrument_id")))}
      ConnectedViewsCheck.scan(new File(generated,"synthetic_assignments.csv")){r=>if(!sampledReused(r("vendor_id")))emit(assignments,r("vendor_id"),Vector(r("vendor_id"),r("template_instrument_id")))}
    }finally{IdentityAudit.close(master);IdentityAudit.close(assignments)}
    def publish(raw:File,name:String,header:Vector[String],single:Boolean):Unit={
      val sorted=new File(attempt,name+".sorted");CorpusExternalSort(raw,sorted,chunk,metrics)
      val p=writer(attempt,name,header)
      var previousKey="";var payloads=Set.empty[String]
      try PhaseTwoCheck.raw(sorted){r=>
        require(r.size==2)
        if(r(0)!=previousKey){previousKey=r(0);payloads=Set.empty}
        val decoded=new String(Base64.getDecoder.decode(r(1)),"UTF-8")
        require(payloads.size<1000,"Dimension versions per vendor bound")
        if(!payloads(decoded)){require(!single || payloads.isEmpty,"Conflicting assignment for stable vendor");p.println(decoded);payloads+=decoded}
      }finally IdentityAudit.close(p)
      require(raw.delete() && sorted.delete())
    }
    try{
      publish(masterRaw,"VendorMaster.csv",masterHeader,false)
      publish(assignmentsRaw,"synthetic_assignments.csv",Vector("vendor_id","template_instrument_id"),true)
      // Both current reference payloads remain inspectable. Existing reporting marks
      // conflicting shared numeric keys ambiguous instead of selecting a false label.
      SyntheticReferenceBundle.specs.foreach{case(name,_,_)=>
        require(Vector(historical,generated).map(root=>new File(root,name).length()).sum<=16L*1024*1024)
        val inputs=Vector(historical,generated).map(root=>new Input(new File(root,name),"reference",true))
        require(inputs.map(_.header).distinct.size==1)
        val p=writer(attempt,name,inputs.head.header)
        val seen=scala.collection.mutable.Set[String]()
        try inputs.foreach{in=>var n=in.next();while(n.nonEmpty){val line=csv(n.get);require(seen.size<100000);if(seen.add(line))p.println(line);n=in.next()}}finally{inputs.foreach(_.close());IdentityAudit.close(p)}
      }
    }finally IdentityAudit.close(metrics)
    Files.write(new File(attempt,"DIMENSIONS_COMPLETE").toPath,"historical_rekey_and_shift_generated_unchanged_conflicting_reference_labels_ambiguous\n".getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);prepare(new File(p("historical")),new File(p("generated")),p("normalizations").split(",").toVector.map(new File(_)),new File(p("out")),p.getOrElse("chunk","100000").toInt,p.get("historicalAssignments").map(new File(_)))}
}
