package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.universalledger.foundation.va.ledger._

/** Persist two raw-input years for the actual Bash scheduler contract check.
  * Reference GLs use the independent adapter/sort/GL entry points.
  */
object FusedDriverFixture {
  private def write(file:File,value:String):Unit={
    file.getParentFile.mkdirs();Files.write(file.toPath,value.getBytes("UTF-8"))
  }
  private def files(root:File):Vector[File]=
    Option(root.listFiles()).toVector.flatten.toVector.flatMap(f=>if(f.isDirectory)files(f)else Vector(f))
  def main(args:Array[String]):Unit={
    val root=new File(args(0)).getCanonicalFile
    require(!root.exists() && root.mkdirs())
    val fixture=new FusedSourcePrepareSpec
    val spec=new File("../sort_specs/SortedJE.sortspec")
    for(year<-Vector(2003,2004)){
      val history=fixture.fixture(root,year,false)
      fixture.fixture(root,year+28,true)
      val out=new File(root,s"independent/$year");require(out.mkdirs())
      val short=f"${year%100}%02d"
      val params=Map("outPath"->out.getPath,"year"->short,"refPath"->history.getPath,
        "vendormaster"->new File(history,"VendorMaster.csv").getPath,"periodGranularity"->"monthly")
      standardizeAndSort.applyAPA(params++Map("inPath"->new File(history,"VARawFiles").getPath,"type"->"B"))
      standardizeAndSort.applyPO(params+("inPath"->new File(history,"VAPOData").getPath))
      val journals=out.listFiles().filter(f=>f.getName.startsWith("SortedJE") && f.getName.endsWith(".csv")).toVector
      val merged=new File(out,s"SortedJEFY${short}merged.csv")
      SortEngine.mergeSpills(journals.map(_.getPath),merged.getPath,SortEngine.loadSpec(spec.getPath))
      GLPost(params+("inPath"->out.getPath))
      val retained=new File(root,s"retained/financial_controls/FY$short")
      require(retained.mkdirs())
      Files.copy(new File(out,s"GL$year.csv").toPath,new File(retained,s"GL$year.csv").toPath)
      val source=scala.io.Source.fromFile(merged,"UTF-8")
      val rows=try source.getLines().size finally source.close()
      write(new File(retained,"metrics/process_metrics.csv"),
        s"pass_id,output_rows,output_bytes,status\n2m,$rows,${merged.length()},complete\n")
    }
    write(new File(root,"history/INPUTS_COMPLETE"),"fixture\n")
    write(new File(root,"whole/WHOLLY_SOURCE_SUBSET_COMPLETE"),"fixture\n")
    for(name<-Vector("history","whole")){
      val entries=files(new File(root,name)).sortBy(_.getPath).map(f=>s"${ComponentPilot.sha(f)}  ${f.getPath}\n").mkString
      write(new File(root,s"${name}_sha256.txt"),entries)
    }
    write(new File(root,"FIXTURE_READY"),"independent_short_year_reference_GLs_two_raw_years\n")
  }
}
