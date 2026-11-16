package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.nio.file.{Files,StandardCopyOption}
import java.nio.charset.StandardCharsets
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport,SortEngine}

/** Frozen offline map and ordered structural SJE rekeying. No identity inference or financial rules. */
object PhaseOneIdentityUpgrade {
  import ReportingSupport._
  val header=Vector("original_id","artificial_id","status","identity_contract")
  def raw(file:File)(f:Vector[String]=>Unit):Long=PhaseTwoCheck.raw(file)(f)
  def prepare(coverage:File,out:File,chunk:Int=100000):Unit={
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    val input=new Input(coverage,"canonical_vendor_coverage",true);val at=input.header.indexOf("vendor_id");require(at>=0)
    val original=new File(attempt,"original_keys.csv");val p=new PrintWriter(original,"UTF-8")
    try {var n=input.next();while(n.nonEmpty){val r=n.get;require(r.size==input.header.size && r(at).length<=256)
      val old=r(at);val fresh=StablePartyIdentity.observedVendor(old)
      row(p,CorpusExternalSort.key(old),old,fresh,if(StablePartyIdentity.unresolved(old))"unresolved_preserved" else "existing_group_encoding_only",StablePartyIdentity.Version)
      n=input.next()
    }}finally {input.close();IdentityAudit.close(p)}
    val sorted=new File(attempt,"original_sorted.csv");CorpusExternalSort(original,sorted,chunk,metrics)
    val registry=writer(attempt,"identity_map.csv",header)
    val inverse=new File(attempt,"inverse_keys.csv");val ip=new PrintWriter(inverse,"UTF-8")
    var previous:Option[String]=None;var count=0L;var unresolved=0L
    try raw(sorted){r=>require(r.size==5 && !previous.contains(r(1)),"Duplicate original identity");previous=Some(r(1));count+=1
      registry.println(csv(r.drop(1)));if(r(3)=="unresolved_preserved")unresolved+=1
      row(ip,CorpusExternalSort.key(r(2)),r(2),r(1))
    }finally {IdentityAudit.close(registry);IdentityAudit.close(ip)}
    val reverse=new File(attempt,"inverse_sorted.csv");CorpusExternalSort(inverse,reverse,chunk,metrics)
    previous=None
    raw(reverse){r=>require(!previous.contains(r(1)),"Artificial identity collision; abort without renumbering");previous=Some(r(1))}
    IdentityAudit.close(metrics)
    val controls=writer(attempt,"identity_controls.csv",Seq("check","value","status"))
    try {row(controls,"unique_original_keys",count,"PASS");row(controls,"unique_artificial_keys",count,"PASS");row(controls,"unresolved_keys_preserved",unresolved,"PASS")}finally IdentityAudit.close(controls)
    val provenance=writer(attempt,"source_manifest.csv",Seq("file","bytes","sha256"))
    try row(provenance,coverage.getCanonicalPath,coverage.length(),ComponentPilot.sha(coverage))finally IdentityAudit.close(provenance)
    Files.write(new File(attempt,"IDENTITY_MAP_COMPLETE").toPath,(StablePartyIdentity.Version+"\nencoding_only_no_legal_party_inference\n").getBytes(StandardCharsets.UTF_8))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"PASS identity_map keys=$count unresolved=$unresolved existing_grouping_preserved=true")
  }
  def applyMap(source:File,mapRoot:File,out:File,sortSpec:File,chunk:Int=100000):Unit={
    val attempt=new File(out.getPath+".attempt");require(new File(mapRoot,"IDENTITY_MAP_COMPLETE").isFile && !out.exists() && !attempt.exists() && attempt.mkdirs())
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    val sourceWrapped=new File(attempt,"source_vendor_keys.csv");val sw=new PrintWriter(sourceWrapped,"UTF-8")
    val canonical=new Input(source,"canonical_sje",false,literalCanonical=true)
    try {var n=canonical.next();while(n.nonEmpty){val r=n.get;require(r.size==39);sw.println(CorpusExternalSort.key(r(0))+","+r.mkString(","));n=canonical.next()}}finally{canonical.close();IdentityAudit.close(sw)}
    val vendorSorted=new File(attempt,"source_vendor_sorted.csv");CorpusExternalSort(sourceWrapped,vendorSorted,chunk,metrics)
    val map=new Input(new File(mapRoot,"identity_map.csv"),"identity_registry",true);require(map.header==header)
    var mapping=map.next();var prior:Option[String]=None;var priorMap:Option[String]=None
    def validate(r:Vector[String]):Unit=require(r.size==4 && r(3)==StablePartyIdentity.Version && r(1)==StablePartyIdentity.observedVendor(r(0)),"Changed/unsupported identity mapping")
    mapping.foreach(validate)
    val spec=SortEngine.loadSpec(sortSpec.getPath)
    require(spec.map(_.colIndex)==Vector(0)++(9 to 22).toVector,"Declared financial comparator changed")
    val wrapped=new File(attempt,"rekeyed_wrapped.csv");val p=new PrintWriter(wrapped,"UTF-8")
    val input=scala.io.Source.fromFile(vendorSorted,"UTF-8");var count=0L
    def advance():Unit={priorMap=mapping.map(r=>CorpusExternalSort.key(r(0)));mapping=map.next();mapping.foreach{r=>validate(r);require(priorMap.forall(_<CorpusExternalSort.key(r(0))),"Registry disorder")}}
    try input.getLines().foreach{line=>val at=line.indexOf(',');val r=line.substring(at+1).split(",",-1).toVector;require(r.size==39)
      val seek=CorpusExternalSort.key(r(0));require(prior.forall(_<=seek),"SJE vendor disorder");prior=Some(seek)
      while(mapping.nonEmpty && CorpusExternalSort.key(mapping.get(0))<seek)advance()
      require(mapping.exists(_(0)==r(0)),s"Unmapped old vendor key ${r(0)}")
      val changed=r.updated(0,mapping.get(1)).mkString(",")
      val comparator=SortEngine.extractKey(changed,spec).keyStr
      // Preserve the engine's UTF-16 lexical comparator exactly, including prefix behavior.
      val order=comparator.iterator.map(c=>f"${c.toInt}%04x").mkString
      p.println(order+","+changed);count+=1
    }finally {input.close();map.close();IdentityAudit.close(p)}
    val sorted=new File(attempt,"rekeyed_sorted.csv")
    try CorpusExternalSort(wrapped,sorted,chunk,metrics)finally IdentityAudit.close(metrics)
    val result=new File(attempt,source.getName);val output=new PrintWriter(result,"UTF-8");var actual=0L
    val lines=scala.io.Source.fromFile(sorted,"UTF-8")
    try lines.getLines().foreach{line=>val at=line.indexOf(',');require(at>0);output.println(line.substring(at+1));actual+=1}finally {lines.close();IdentityAudit.close(output)}
    require(actual==count)
    val p2=writer(attempt,"application_controls.csv",Seq("check","expected","actual","status"))
    try {row(p2,"canonical_sje_rows",count,actual,"PASS");row(p2,"changed_columns","transIPID_only","transIPID_only","PASS")}finally IdentityAudit.close(p2)
    Files.write(new File(attempt,"IDENTITY_APPLICATION_COMPLETE").toPath,"source_preparation_requires_actual_posting_consumer\n".getBytes(StandardCharsets.UTF_8))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"PASS identity_application rows=$count file=$result posting_not_yet_claimed=true")
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);if(p.getOrElse("mode","prepare")=="prepare")prepare(new File(p("coverage")),new File(p("out")),p.getOrElse("chunk","100000").toInt)else applyMap(new File(p("source")),new File(p("map")),new File(p("out")),new File(p("sortSpec")),p.getOrElse("chunk","100000").toInt)}
}
