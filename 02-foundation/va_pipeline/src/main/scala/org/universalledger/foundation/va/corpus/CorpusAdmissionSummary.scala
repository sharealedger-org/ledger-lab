package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}

/** Bounded post-engine row/byte census of actual vendor-grain analytical products. */
object CorpusAdmissionSummary {
  def inspect(report:File,out:File):Unit={
    require(new File(report,"REPORT_COMPLETE").isFile && !out.exists() && out.mkdirs())
    val manifest=ReportingSupport.writer(out,"input_manifest.csv",Seq("path","bytes","physical_lines","sha256"))
    val result=ReportingSupport.writer(out,"report_profile.csv",Seq("file","rows","bytes","zero_rows","zero_bytes","max_record_bytes"))
    try Vector("movement_history.csv","balance_history.csv","reconciliation.csv","relationships.csv",
      "vendor_temporal_cuts.csv","vendor_attribute_summary.csv","synthetic_vendor_coverage.csv").foreach { name=>
      val file=new File(report,name); val in=new IdentityAudit.Read(file,manifest)
      var rows=0L; var zero=0L; var bytes=0L; var max=0
      try {
        require(in.next().startsWith("vendor_id,")); var line=in.next()
        while(line!=null){
          val n=line.getBytes(StandardCharsets.UTF_8).length; rows+=1; max=max.max(n)
          val id=line.takeWhile(_!=',')
          if(id.nonEmpty && id.forall(_=='0')){zero+=1;bytes+=n+1}
          line=in.next()
        }
      }finally in.close()
      ReportingSupport.row(result,name,rows,file.length(),zero,bytes,max)
    }finally Seq(manifest,result).foreach(IdentityAudit.close)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);inspect(new File(p("report")),new File(p("out")))}
}
