package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger._

class FusedSourcePrepareSpec extends AnyFunSuite {
  private def write(file:File,text:String):Unit={file.getParentFile.mkdirs();Files.write(file.toPath,text.getBytes("UTF-8"))}
  private def remove(file:File):Unit={if(file.isDirectory)file.listFiles().foreach(remove);Files.delete(file.toPath)}
  private def lines(file:File):Vector[String]={val s=scala.io.Source.fromFile(file,"UTF-8");try s.getLines().toVector finally s.close()}
  private val spec=new File("../sort_specs/SortedJE.sortspec")
  private def fixture(root:File,year:Int,generated:Boolean):File={
    val dir=new File(root,if(generated)"whole"else"history");dir.mkdirs()
    write(new File(dir,"aRefAgencyCSV.txt"),"AGY_AGENCY_CODE,AGY_AGENCY_KEY,AGY_AGENCY_NAME\n101,101,AGENCY\n")
    write(new File(dir,"aRefFundCSV.txt"),"FNDDTL_FUND_DETAIL_KEY,FNDDTL_FUND_DETAIL_CODE,FNDDTL_FUND_DETAIL_NAME\n201,201,FUND\n")
    write(new File(dir,"aRefObjectCSV.txt"),"OBJ_OBJECT_KEY,2-DIGIT OBJECT CODE,2-DIGIT OBJECT NAME,4-DIGIT OBJECT CODE,4-DIGIT OBJECT NAME\n301,23,SUPPLIES,2301,OBJECT\n")
    write(new File(dir,"aRefProgramCSV.txt"),"SPRG_SUB_PROGRAM_KEY,PFCN_FUNCTION_CODE,PFCN_FUNCTION_NAME,PRG_PROGRAM_CODE,PRG_PROGRAM_NAME,SPRG_SUB_PROGRAM_CODE,,SPRG_SUB_PROGRAM_NAME\n401,1,F,1,P,1,,PROGRAM\n")
    write(new File(dir,"aRefSourceCSV.txt"),"SRC_SOURCE_KEY,SRCCLS_SOURCE_CLASS_CODE,SRCCLS_SOURCE_CLASS_NAME,SRC_SOURCE_CODE,,SRC_SOURCE_NAME\n501,1,TAXES,1001,,SOURCE\n")
    val vendor=if(generated)StablePartyIdentity.generatedVendor("fixture","wholly")else"c9545"
    val name=if(generated)"WHOLLY SUPPLIER"else"KNOWN SUPPLIER"
    write(new File(dir,"VendorMaster.csv"),s"instID,instHolderName\n$vendor,$name\n")
    val header="AGENCY_NAME,AMOUNT,FUND_NAME,LOCALITY,OBJECT_NAME,PROGRAM_NAME,VENDOR_NAME,VOUCHER_NUMBER,VOUCHER_DATE\n"
    val expenses=s"AGENCY,25.00,FUND,VA,OBJECT,PROGRAM,$name,DOC,5/20/$year\n"+
      (if(generated)""else s"AGENCY,-12.50,FUND,VA,OBJECT,PROGRAM,UNKNOWN HISTORICAL SUPPLIER,REFUND,5/21/$year\n")
    write(new File(dir,s"VARawFiles/FY $year Expenditure/Expenditures May $year.csv"),header+expenses)
    if(!generated)write(new File(dir,s"VARawFiles/FY $year Revenue/Revenues May $year.csv"),
      "AGENCY_NAME,AMOUNT,FUND_NAME,LOCALITY,SOURCE_TYPE,SOURCE,DEPOSIT_DATE\n"+s"AGENCY,7.50,FUND,VA,TAXES,SOURCE,5/22/$year\n")
    val poHeader="PONUMBER\tAGENCY\tORDEREDDATE\tVENDORCOMMODITYDESC\tQUANTITYORDERED\tPRICE\tVENDORID\tVENDORNAME\tVENDORADDRESS\tVENDORCITY\tVENDORSTATE\tVENDORPOSTALCODE\tVENDORLOC_EMAILADDRESS\tNIGPCODE\tNIGPDESCRIPTION\tUOMCode\tUOMDesc\tVENDORPARTNUMBER\tMANPARTNUMBER\n"
    write(new File(dir,s"VAPOData/VA_opendata_FY$year.txt"),poHeader+s"PO$year\t101\t5/20/$year\tSUPPLIES\t2\t10.00\t$vendor\t$name\tADDR\tCITY\tVA\t00000\temail\t301\tOBJECT\tEA\teach\t\t\n")
    dir
  }
  test("paired exits and shared Pay/PO sort match independent normalization through posting and reporting") {
    val root=Files.createTempDirectory("fused-source-").toFile
    try{
      val history=fixture(root,2003,false);val whole=fixture(root,2031,true)
      def independent(input:File,year:Int):File={
        val out=new File(root,s"adapter_$year");out.mkdirs()
        // Frozen real Bash invocation, not the different four-digit APA profile.
        val p=Map("outPath"->out.getPath,"year"->f"${year%100}%02d","refPath"->input.getPath,
          "vendormaster"->new File(input,"VendorMaster.csv").getPath,"periodGranularity"->"monthly")
        standardizeAndSort.applyAPA(p++Map("inPath"->new File(input,"VARawFiles").getPath,"type"->"B"))
        standardizeAndSort.applyPO(p+ ("inPath"->new File(input,"VAPOData").getPath))
        val files=out.listFiles().filter(f=>f.getName.startsWith("SortedJE")&&f.getName.endsWith(".csv")).toVector
        val merged=new File(out,f"SortedJEFY${year%100}%02dmerged.csv")
        SortEngine.mergeSpills(files.map(_.getPath),merged.getPath,SortEngine.loadSpec(spec.getPath));merged
      }
      val native=independent(history,2003);val generated=independent(whole,2031)
      val normalized=new File(root,"normalized")
      PhaseTwoBaselineNormalize.prepare(native,new File(history,"aRefObjectCSV.txt"),spec,normalized,2003,chunk=1)
      val reference=new File(root,"reference");reference.mkdirs()
      SortEngine.mergeSpills(Seq(new File(normalized,"SortedJEFY31merged.csv").getPath,generated.getPath),new File(reference,"SortedJEFY31merged.csv").getPath,SortEngine.loadSpec(spec.getPath))
      val fused=new File(root,"fused");var originalRows=0L
      FusedSourcePrepare.prepare(Vector(FusedSourcePrepare.Input(history,2003,true),FusedSourcePrepare.Input(whole,2031,false)),spec,fused,
        (_,pair)=>{assert(pair.map(r=>BigDecimal(r(25))).sum==0);originalRows+=pair.size},chunk=1,byteCap=4096,fanIn=2)
      val actual=lines(new File(fused,"SortedJEFY31merged.csv"));assert(originalRows==12 && actual.size==12)
      val expenseRows=actual.map(_.split(",",-1)).filter(_(5)=="EXPENSE_PAYMENT")
      assert(expenseRows.nonEmpty && expenseRows.forall(_(22).startsWith("0031/")),"APA must preserve the Bash short-year period contract")
      assert(expenseRows.forall(r=>PhaseTwoBaselineNormalize.date(r(23)).nonEmpty),"Accounting dates remain parseable after normalization")
      assert(actual.sorted==lines(new File(reference,"SortedJEFY31merged.csv")).sorted)
      val keys=actual.map(SortEngine.extractKey(_,SortEngine.loadSpec(spec.getPath)).keyStr)
      assert(keys==keys.sorted)
      assert(!new File(fused,"shared_sort_scratch").exists())
      assert(!new File(fused,"adapters/historical").listFiles().exists(_.getName.startsWith("SortedJE")))
      post(reference.getPath+"/",reference.getPath+"/","31")
      GLPost(Map("inPath"->reference.getPath,"outPath"->reference.getPath,"year"->"31"))
      PermutationGenerator(Map("inPath"->reference.getPath,"outPath"->reference.getPath,"year"->"31","permBufferCapacity"->"2"))
      val journal=new File(fused,"SortedJEFY31merged.csv")
      val held=new File(fused,"held_audit_journal.csv")
      val result=FusedPosting.run(fused,fused,"31",Map("permBufferCapacity"->"2","permMergeFanIn"->"2"),file=>{
        // An observer that tries to reread a named SJE partition now has no such
        // input. The posting reader retains its already-open file descriptor.
        assert(file==journal && file.renameTo(held))
      })
      assert(result.sourceOpens==1 && result.journalRows==12)
      assert(held.renameTo(journal))
      Vector("LDGR2031.csv","GL2031.csv","PERM_TBL2031.csv","PERM_SUMMARY2031.csv").foreach{name=>assert(lines(new File(fused,name))==lines(new File(reference,name)),name)}
      write(new File(fused,"metrics/run_manifest.csv"),"status\ncomplete\n")
      val order=new File(root,"report_order");ReportingSort.prepare(Vector(fused),order,1,2)
      val report=new File(root,"report")
      CKBViews(Map("inPaths"->new File(order,"FY31").getPath,"outPath"->report.getPath,"inputOrder"->"presorted","reportProfile"->"expanded","balanceBasis"->"period_movements","aggregateKeys"->"2","spillRows"->"2","mergeFanIn"->"2"))
      ReportingReduce(Map("outPath"->report.getPath,"mergeFanIn"->"2"))
      var checks=0;ConnectedViewsCheck.scan(new File(report,"reconciliation.csv")){r=>assert(r("status")=="PASS");checks+=1};assert(checks>0)
      GLPost(Map("inPath"->native.getParent,"outPath"->native.getParent,"year"->"03"))
      val retained=new File(root,"retained")
      val originalGL=new File(retained,"financial_controls/FY03/GL2003.csv")
      originalGL.getParentFile.mkdirs()
      Files.copy(new File(native.getParentFile,"GL2003.csv").toPath,originalGL.toPath)
      val yearOut=new File(root,"financial_year")
      FusedFinancialYear.run(history,whole,retained,spec,yearOut,2003,
        Map("sortChunk"->"1","sortBytes"->"4096","mergeFanIn"->"2","permBufferCapacity"->"2","permMergeFanIn"->"2"))
      assert(new File(yearOut,"FINANCIAL_YEAR_COMPLETE").isFile)
      Vector("LDGR2031.csv","GL2031.csv","PERM_TBL2031.csv","PERM_SUMMARY2031.csv").foreach{name=>
        assert(lines(new File(yearOut,"output/FY31/"+name))==lines(new File(reference,name)),name)
      }
      assert(lines(new File(yearOut,"native/FY03/GL2003.csv"))==lines(originalGL))
    }finally remove(root)
  }
}
