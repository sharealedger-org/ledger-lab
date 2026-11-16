package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.{standardizeAndSort,SortEngine,post,GLPost,CKBViews,ReportingReduce}

class PhaseTwoDerivedBaselineSpec extends AnyFunSuite {
  test("fresh raw history and generated sources normalize merge post and reach sequential reporting") {
    val root=Files.createTempDirectory("derived-baseline-").toFile
    def write(file:File,s:String):Unit={file.getParentFile.mkdirs();Files.write(file.toPath,s.getBytes("UTF-8"))}
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try{
      val refs=new File(root,"refs");refs.mkdirs()
      write(new File(refs,"aRefAgencyCSV.txt"),"AGY_AGENCY_CODE,AGY_AGENCY_KEY,AGY_AGENCY_NAME\n101,101,AGENCY\n")
      write(new File(refs,"aRefFundCSV.txt"),"FNDDTL_FUND_DETAIL_KEY,FNDDTL_FUND_DETAIL_CODE,FNDDTL_FUND_DETAIL_NAME\n201,201,FUND\n")
      write(new File(refs,"aRefObjectCSV.txt"),"OBJ_OBJECT_KEY,2-DIGIT OBJECT CODE,2-DIGIT OBJECT NAME,4-DIGIT OBJECT CODE,4-DIGIT OBJECT NAME\n301,23,SUPPLIES,2301,OBJECT\n")
      write(new File(refs,"aRefProgramCSV.txt"),"SPRG_SUB_PROGRAM_KEY,PFCN_FUNCTION_CODE,PFCN_FUNCTION_NAME,PRG_PROGRAM_CODE,PRG_PROGRAM_NAME,SPRG_SUB_PROGRAM_CODE,,SPRG_SUB_PROGRAM_NAME\n401,1,F,1,P,1,,PROGRAM\n")
      write(new File(refs,"aRefSourceCSV.txt"),"SRC_SOURCE_KEY,SRCCLS_SOURCE_CLASS_CODE,SRCCLS_SOURCE_CLASS_NAME,SRC_SOURCE_CODE,,SRC_SOURCE_NAME\n501,1,TAXES,1001,,SOURCE\n")
      val wholeId=StablePartyIdentity.generatedVendor("fixture","wholly")
      val master=new File(refs,"VendorMaster.csv");write(master,"instID,instHolderName\n"+wholeId+",WHOLLY SUPPLIER\n")
      val header="AGENCY_NAME,AMOUNT,FUND_NAME,LOCALITY,OBJECT_NAME,PROGRAM_NAME,VENDOR_NAME,VOUCHER_NUMBER,VOUCHER_DATE\n"
      def transform(year:Int,name:String,amount:String,vendor:String):File={
        val raw=new File(root,name+"/VARawFiles");raw.mkdirs();val out=new File(root,name+"/output");out.mkdirs()
        write(new File(raw,s"FY $year Expenditure/Expenditures May $year.csv"),header+s"AGENCY,$amount,FUND,VA,OBJECT,PROGRAM,$vendor,DOC,5/20/$year\n")
        standardizeAndSort.applyAPA(Map("inPath"->raw.getPath,"outPath"->out.getPath,"refPath"->refs.getPath,"vendormaster"->master.getPath,"year"->year.toString,"type"->"E","periodGranularity"->"monthly","chunkSize"->"1"))
        val files=out.listFiles().filter(f=>f.getName.startsWith("SortedJE") && f.getName.endsWith(".csv"));assert(files.length==1);files.head
      }
      val historical=transform(2003,"historical","-12.50","HISTORICAL SOURCE SUPPLIER")
      val wholly=transform(2031,"wholly","25.00","WHOLLY SUPPLIER")
      val normalized=new File(root,"normalized");val spec=new File("../sort_specs/SortedJE.sortspec")
      PhaseTwoBaselineNormalize.prepare(historical,new File(refs,"aRefObjectCSV.txt"),spec,normalized,2003,chunk=1)
      val financial=new File(root,"financial");financial.mkdirs()
      val merged=new File(financial,"SortedJEFY31merged.csv")
      assert(SortEngine.mergeSpills(Seq(new File(normalized,"SortedJEFY31merged.csv").getPath,wholly.getPath),merged.getPath,SortEngine.loadSpec(spec.getPath))==4)
      post(financial.getPath+"/",financial.getPath+"/","31")
      GLPost(Map("inPath"->financial.getPath,"outPath"->financial.getPath,"year"->"31"))
      write(new File(financial,"metrics/run_manifest.csv"),"status\ncomplete\n")
      val order=new File(root,"report_order");ReportingSort.prepare(Vector(financial),order,1,2)
      val report=new File(root,"report")
      CKBViews(Map("inPaths"->new File(order,"FY31").getPath,"outPath"->report.getPath,"inputOrder"->"presorted","reportProfile"->"expanded","balanceBasis"->"period_movements","aggregateKeys"->"2","spillRows"->"2","mergeFanIn"->"2"))
      ReportingReduce(Map("outPath"->report.getPath,"mergeFanIn"->"2"))
      var checks=0
      ConnectedViewsCheck.scan(new File(report,"reconciliation.csv")){r=>assert(r("status")=="PASS");checks+=1}
      assert(checks>0)
    }finally remove(root)
  }
}
