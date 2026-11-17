package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.{Files,StandardCopyOption}
import org.universalledger.foundation.va.ledger.{ReportingSupport,SharedJournalSort,SortEngine,standardizeAndSort}

/** Shared source/ARE output topology. The original-pair observer is the explicit
  * native historical/wholly financial-control branch, before structural changes.
  */
object FusedSourcePrepare {
  import ReportingSupport._
  case class Input(root:File,year:Int,historical:Boolean)
  def prepare(inputs:Vector[Input],sortSpec:File,out:File,
      original:(Input,Vector[Vector[String]])=>Unit,chunk:Int=100000,
      byteCap:Long=32L*1024*1024,fanIn:Int=16):Unit={
    require(inputs.size==2 && inputs.count(_.historical)==1)
    val historical=inputs.find(_.historical).get
    val wholly=inputs.find(!_.historical).get
    require(historical.year>=2003 && historical.year<=2016 && wholly.year==historical.year+28)
    require(inputs.map(_.root.getCanonicalPath).distinct.size==2)
    val attempt=new File(out.getPath+".attempt")
    require(!out.exists() && !attempt.exists() && attempt.mkdirs())
    val spec=SortEngine.loadSpec(sortSpec.getPath)
    require(spec.map(_.colIndex)==Vector(0)++(9 to 22).toVector)
    val short=f"${wholly.year%100}%02d"
    val normalization=new File(attempt,"normalizations/FY"+short)
    require(normalization.mkdirs())
    val exit=new HistoricalJournalExit(new File(historical.root,"aRefObjectCSV.txt"),normalization,historical.year)
    val sorter=new SharedJournalSort(new File(attempt,s"SortedJEFY${short}merged.csv"),spec,new File(attempt,"shared_sort_scratch"),chunk,byteCap,fanIn)
    val metrics=writer(attempt,"sort_metrics.csv",CorpusExternalSort.header)
    val controls=writer(attempt,"source_controls.csv",Seq("source_class","adapter","source_groups","journal_rows","status"))
    var accepted=0L
    try{
      inputs.foreach{input=>
        val kind=if(input.historical)"historical"else"wholly_generated"
        val adapterOut=new File(attempt,"adapters/"+kind);require(adapterOut.mkdirs())
        def consume(adapter:String)(pair:Vector[String]):Unit={
          val rows=pair.map(_.split(",",-1).toVector)
          require(rows.size==2 && rows.forall(_.size==39) && rows.map(_(31)).toSet==Set("D","O"))
          require(rows.map(_(35)).distinct.size==1 && rows.head(35).startsWith("SRC1:"))
          require(rows.map(r=>BigDecimal(r(25))).sum==0,"ARE output group does not balance")
          original(input,rows)
          val changed=if(input.historical)exit(rows)else rows
          require(changed.size==rows.size)
          changed.foreach(r=>sorter.add(r.mkString(",")));accepted+=changed.size
        }
        val common=Map("outPath"->adapterOut.getPath,"year"->input.year.toString,
          "periodGranularity"->"monthly","vendormaster"->new File(input.root,"VendorMaster.csv").getPath,
          "refPath"->input.root.getPath,"sortspec"->sortSpec.getPath)
        var before=accepted
        standardizeAndSort.applyAPA(common++Map("inPath"->new File(input.root,"VARawFiles").getPath,"type"->"B"),Some(consume("Pay")))
        row(controls,kind,"Pay",(accepted-before)/2,accepted-before,"PASS")
        before=accepted
        standardizeAndSort.applyPO(common++Map("inPath"->new File(input.root,"VAPOData").getPath,"quarter"->"1"),Some(consume("PO")))
        row(controls,kind,"PO",(accepted-before)/2,accepted-before,"PASS")
      }
      require(sorter.finish(metrics)==accepted)
      exit.writeControls()
      Files.write(new File(normalization,"NORMALIZATION_COMPLETE").toPath,
        "paired_ARE_output_exit_before_shared_financial_sort\n".getBytes("UTF-8"))
    }finally{exit.close();IdentityAudit.close(metrics);IdentityAudit.close(controls)}
    Files.write(new File(attempt,"SOURCE_PREPARATION_COMPLETE").toPath,
      s"journal_rows=$accepted\nrequires_actual_posting_consumer\n".getBytes("UTF-8"))
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
  }
}
