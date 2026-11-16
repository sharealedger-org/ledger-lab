package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.nio.file.{Files,StandardCopyOption}
import java.util.Base64
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}

/** Bounded source-level selection. Reused events are replacements, never additive copies. */
object PhaseTwoWhollySources {
  import ReportingSupport._
  def locator(path:String,n:Long):String="SRC1:"+Base64.getUrlEncoder.withoutPadding.encodeToString(path.getBytes("UTF-8"))+":"+n
  def prepare(input:File,out:File,chunk:Int=100000):Unit={
    val attempt=new File(out.getPath+".attempt")
    require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    val descriptors=new File(attempt,"source_descriptors.csv");val p=new PrintWriter(descriptors,"UTF-8")
    var expected=0L;var excluded=0L
    try ConnectedViewsCheck.scan(new File(input,"source_lineage.csv")){r=>
      val path=r("source_file");val n=r("source_row").toLong
      require(n>0 && !new File(path).isAbsolute && !path.split("/",-1).contains(".."))
      require(path.startsWith("VARawFiles/") || path.startsWith("VAPOData/"))
      require(r("locator")==locator(path,n),"Lineage source locator mismatch")
      val kind=r("generation_class");require(Set("wholly_generated","reassigned_activity")(kind))
      if(kind=="wholly_generated")expected+=1 else excluded+=1
      row(p,CorpusExternalSort.key(path,f"$n%020d"),path,n,kind,r("event_id"),r("locator"))
    }finally IdentityAudit.close(p)
    val sorted=new File(attempt,"source_descriptors_sorted.csv")
    try CorpusExternalSort(descriptors,sorted,chunk,metrics)finally IdentityAudit.close(metrics)
    val links=writer(attempt,"source_subset_map.csv",Seq("event_id","original_locator","subset_locator","source_file","original_row","subset_row"))
    val manifest=writer(attempt,"source_manifest.csv",Seq("source_file","original_rows","retained_rows","excluded_rows","original_sha256","subset_sha256"))
    var path="";var source:scala.io.Source=null;var lines:Iterator[String]=Iterator.empty
    var output:PrintWriter=null;var original=0L;var retained=0L;var removed=0L;var kept=0L;var dropped=0L
    def closeFile():Unit=if(source!=null){
      require(!lines.hasNext,"Source records missing from frozen lineage")
      source.close();source=null;IdentityAudit.close(output);output=null
      row(manifest,path,original,retained,removed,ComponentPilot.sha(new File(input,path)),ComponentPilot.sha(new File(attempt,path)))
    }
    try PhaseTwoCheck.raw(sorted){r=>
      require(r.size==6)
      if(r(1)!=path){closeFile();path=r(1);original=0;retained=0;removed=0
        source=scala.io.Source.fromFile(new File(input,path),"UTF-8");lines=source.getLines()
        val target=new File(attempt,path);target.getParentFile.mkdirs();output=new PrintWriter(target,"UTF-8")
        require(lines.hasNext,"Missing raw header");output.println(lines.next())
      }
      require(r(2).toLong==original+1 && lines.hasNext,"Duplicate, missing or unordered source record")
      val line=lines.next();original+=1
      if(r(3)=="wholly_generated"){
        output.println(line);retained+=1;kept+=1
        row(links,r(4),r(5),locator(path,retained),path,original,retained)
      }else{removed+=1;dropped+=1}
    }finally{try closeFile()finally{IdentityAudit.close(links);IdentityAudit.close(manifest)}}
    require(kept==expected && dropped==excluded)
    val controls=writer(attempt,"subset_controls.csv",Seq("check","value","status"))
    try{row(controls,"wholly_generated_retained",kept,"PASS");row(controls,"reassigned_copies_excluded",dropped,"PASS")}finally IdentityAudit.close(controls)
    Files.write(new File(attempt,"WHOLLY_SOURCE_SUBSET_COMPLETE").toPath,"source_selection_only_requires_native_TTL_ARE\n".getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);prepare(new File(p("input")),new File(p("out")),p.getOrElse("chunk","100000").toInt)}
}
