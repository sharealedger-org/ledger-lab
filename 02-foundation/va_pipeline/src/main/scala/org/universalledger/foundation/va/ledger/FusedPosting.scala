package org.universalledger.foundation.va.ledger

import java.io.File

/** One canonical journal scan in posting, with GL/permutation observations made
  * inside readJrnl. Final permutation spill reduction is still a declared pass.
  * Publication/restart belongs to the caller's isolated process attempt.
  */
object FusedPosting {
  case class Result(journalRows:Long,ledgerRows:Long,glRows:Long,sourceOpens:Int)
  def run(input:File,out:File,year:String,options:Map[String,String]=Map.empty,
      inputOpened:File=>Unit=(_:File)=>()):Result={
    require(input.isDirectory && out.isDirectory)
    val short=year.takeRight(2)
    val files=SortedJEInputs.oneRead(input.listFiles().filter(_.isFile)
      .filter(f=>f.getName.startsWith("SortedJEFY"+short)&&f.getName.endsWith(".csv")).sortBy(_.getName).toSeq)
    require(files.nonEmpty,"No fused posting journal inputs")
    val gl=new GLPost.Accumulator(new File(out,s"GL20$short.csv"),options.getOrElse("glMaxKeys","1000000").toInt)
    val permutation=new PermutationGenerator.Accumulator(
      Map("permMaxGLKeys"->"1000000","permMaxVendors"->"1000000","permMergeFanIn"->"16")++options++
      Map("outPath"->out.getPath,"year"->year))
    permutation.addInputBytes(files.map(_.length()).sum)
    var journals=0L;var balances=0L;var opens=0
    val observer=new post.Observer {
      override def opened(file:File):Unit={opens+=1;inputOpened(file)}
      override def journal(columns:Array[String]):Unit={
        require(columns.length==39,"Fused posting requires canonical 39-field SJEs")
        gl.accept(columns);permutation.accept(columns);journals+=1
      }
      override def balance():Unit={balances+=1}
    }
    try{
      post(input.getPath+"/",out.getPath+"/",year,observer=Some(observer))
      require(gl.rows==journals && permutation.rows==journals && opens==files.size,"Fused observer input conservation")
      gl.finish();permutation.finish()
      val metrics=ReportingSupport.writer(out,"fused_posting_controls.csv",Seq("check","expected","actual","status"))
      try{
        ReportingSupport.row(metrics,"gl_observer_rows",journals,gl.rows,"PASS")
        ReportingSupport.row(metrics,"permutation_observer_rows",journals,permutation.rows,"PASS")
        ReportingSupport.row(metrics,"journal_input_opens",files.size,opens,"PASS")
        ReportingSupport.row(metrics,"ledger_rows_emitted",balances,balances,"OBSERVED")
      }finally metrics.close()
      Result(journals,balances,gl.size,opens)
    }finally permutation.close()
  }
}
