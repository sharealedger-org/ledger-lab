package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.{Files,StandardCopyOption}
import org.universalledger.foundation.va.ledger.{FusedPosting,GLPost,LedgerApp,ReportingSupport}

/** One isolated fused source/sort/post/GL/permutation attempt for a backing year.
  * Native historical GL is observed before normalization, without a native SJE
  * sort/file scan. No completed marker is published until all financial controls
  * pass against the independently retained historical GL.
  */
object FusedFinancialYear {
  import ReportingSupport._
  def run(historical:File,wholly:File,retained:File,spec:File,out:File,year:Int,
      options:Map[String,String]=Map.empty):Unit={
    require(year>=2003 && year<=2016)
    val old=f"${year%100}%02d";val fresh=f"${(year+28)%100}%02d"
    val attempt=new File(out.getPath+".attempt")
    require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val start=System.nanoTime()
    def status(value:String):Unit=options.get("statusFile").foreach(path=>
      Files.write(new File(path).toPath,(value+"\n").getBytes("UTF-8")))
    val native=new File(attempt,"native/FY"+old);val whole=new File(attempt,"wholly/FY"+fresh)
    val financial=new File(attempt,"output/FY"+fresh)
    Vector(native,whole,financial).foreach(f=>require(f.mkdirs()))
    val cap=options.getOrElse("glMaxKeys","1000000").toInt
    val nativeGL=new GLPost.Accumulator(new File(native,s"GL$year.csv"),cap)
    val wholeGL=new GLPost.Accumulator(new File(whole,s"GL${year+28}.csv"),cap)
    val source=new File(attempt,"source")
    status("fused_source_"+old)
    FusedSourcePrepare.prepare(Vector(FusedSourcePrepare.Input(historical,year,true),
      FusedSourcePrepare.Input(wholly,year+28,false)),spec,source,(input,pair)=>{
      val gl=if(input.historical)nativeGL else wholeGL
      pair.foreach(r=>gl.accept(r.toArray))
    },options.getOrElse("sortChunk","100000").toInt,
      options.getOrElse("sortBytes",(32L*1024*1024).toString).toLong,options.getOrElse("mergeFanIn","16").toInt)
    status("native_gl_controls_"+old)
    nativeGL.finish();wholeGL.finish()
    Vector(native,whole).foreach{dir=>Files.copy(new File(source,"source_controls.csv").toPath,new File(dir,"source_controls.csv").toPath)}
    require(ComponentPilot.sha(new File(native,s"GL$year.csv"))==
      ComponentPilot.sha(new File(retained,s"financial_controls/FY$old/GL$year.csv")),"Historical raw-pair GL changed")
    val journal=new File(financial,s"SortedJEFY${fresh}merged.csv")
    Files.createLink(journal.toPath,new File(source,journal.getName).toPath)
    status("fused_post_"+fresh)
    val posted=FusedPosting.run(financial,financial,fresh,options)
    require(posted.journalRows==nativeGL.rows+wholeGL.rows,"Source-to-posting journal conservation")
    status("financial_controls_"+fresh)
    PhaseTwoBaselineControls.check(attempt,retained,Vector(year),new File(attempt,"financial_controls"),false,
      "source:historical","source:wholly_generated","fused_post")
    val meta=new File(financial,"metrics");require(meta.mkdirs())
    val manifest=writer(meta,"run_manifest.csv",Seq("run_id","experiment_id","status","profile","journal_rows","ledger_rows","journal_input_opens"))
    try row(manifest,options.getOrElse("runId",out.getCanonicalPath),"synthetic-generation-proof","complete","phase2-fused-v1",posted.journalRows,posted.ledgerRows,posted.sourceOpens)
    finally IdentityAudit.close(manifest)
    val summary=writer(attempt,"fused_year_summary.csv",Seq("historical_year","synthetic_year","historical_journal_rows","wholly_journal_rows","combined_journal_rows","ledger_rows","gl_rows","journal_input_opens","process_seconds","status"))
    try row(summary,year,year+28,nativeGL.rows,wholeGL.rows,posted.journalRows,posted.ledgerRows,posted.glRows,posted.sourceOpens,(System.nanoTime()-start)/1e9,"PASS")
    finally IdentityAudit.close(summary)
    Files.write(new File(attempt,"FINANCIAL_YEAR_COMPLETE").toPath,"shared_source_sort_and_posting_observers_controls_passed\n".getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
    status("financial_year_"+fresh+"_complete")
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args)
    run(new File(p("historical")),new File(p("wholly")),new File(p("retained")),new File(p("sortSpec")),new File(p("out")),p("year").toInt,p)
  }
}
