package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.nio.file.{Files,StandardCopyOption}
import org.universalledger.foundation.va.ledger.{LedgerApp,ReportingSupport}

/** Independent ordered GL conservation and producer/consumer row checks. */
object PhaseTwoBaselineControls {
  import ReportingSupport._
  def rows(root:File,pass:String,column:String):Long={var found=0;var result=0L
    ConnectedViewsCheck.scan(new File(root,"metrics/process_metrics.csv")){r=>if(r("pass_id")==pass){require(r("status")=="complete");found+=1;result+=r(column).toLong}}
    require(found==1,"Missing/duplicate process metric "+pass);result
  }
  def check(root:File,retained:File,years:Vector[Int],out:File,full:Boolean):Unit={
    val attempt=new File(out.getPath+".attempt");require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val controls=writer(attempt,"baseline_controls.csv",Seq("check","expected","actual","status"))
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    var historicalRows=0L;var wholeRows=0L;var finalRows=0L
    try years.foreach{year=>
      val old=f"${year%100}%02d";val fresh=f"${(year+28)%100}%02d"
      val native=new File(root,"native/FY"+old);val whole=new File(root,"wholly/FY"+fresh);val financial=new File(root,"output/FY"+fresh)
      val previousGL=new File(retained,s"financial_controls/FY$old/GL$year.csv")
      val nativeGL=new File(native,s"GL$year.csv")
      require(ComponentPilot.sha(previousGL)==ComponentPilot.sha(nativeGL),"Historical raw replay GL changed")
      val h=rows(native,"2m","output_rows");val w=rows(whole,"2m","output_rows");val actual=rows(financial,"3","input_rows")
      require(h+w==actual,"Merged journal row conservation")
      historicalRows+=h;wholeRows+=w;finalRows+=actual
      row(controls,s"FY$fresh journal_rows",h+w,actual,"PASS")
      val raw=new File(attempt,s"gl_$fresh.csv");val p=new PrintWriter(raw,"UTF-8")
      def append(file:File,shift:Boolean,sign:Int):Unit=ConnectedViewsCheck.scan(file){r=>
        val period=if(shift)PhaseTwoBaselineNormalize.period(r("fiscal_year"))else r("fiscal_year")
        row(p,CorpusExternalSort.key(r("agency"),r("nominal_account_id"),r("fund"),r("object_id"),period,r("dir_flag")),BigDecimal(r("total_amount"))*sign)
      }
      try{append(nativeGL,true,1);append(new File(whole,s"GL${year+28}.csv"),false,1);append(new File(financial,s"GL${year+28}.csv"),false,-1)}finally IdentityAudit.close(p)
      val sorted=new File(attempt,s"gl_${fresh}_sorted.csv");CorpusExternalSort(raw,sorted,100000,metrics)
      var key="";var sum=BigDecimal(0);var keys=0L
      def flush():Unit=if(key.nonEmpty){require(sum==0,"Combined GL differs from shifted historical plus wholly generated GL");keys+=1}
      PhaseTwoCheck.raw(sorted){r=>require(r.size==2);if(key!=r(0)){flush();key=r(0);sum=0};sum+=BigDecimal(r(1))};flush()
      row(controls,s"FY$fresh conserved_GL_keys",keys,keys,"PASS")
      require(raw.delete() && sorted.delete())
    }finally{IdentityAudit.close(metrics);IdentityAudit.close(controls)}
    if(full){require(historicalRows==261529946L && wholeRows==4789872L && finalRows==266319818L,"Full population acceptance changed")}
    Files.write(new File(attempt,"BASELINE_FINANCIAL_CONTROLS_COMPLETE").toPath,s"historical_journal_rows=$historicalRows\nwholly_journal_rows=$wholeRows\nfinal_journal_rows=$finalRows\n".getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);check(new File(p("root")),new File(p("retained")),p("years").split(",").toVector.map(x=>2000+x.toInt),new File(p("out")),p.getOrElse("full","false").toBoolean)}
}
