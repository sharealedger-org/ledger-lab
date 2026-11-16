package org.universalledger.foundation.va.corpus

import java.io.{File, PrintWriter}
import java.nio.file.{Files, StandardCopyOption}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.security.MessageDigest
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport}
import scala.collection.mutable

/** Fictional source preparation. Accounting and posting remain in the existing Scala engine. */
object PhaseTwoCorpus {
  import ReportingSupport._
  import CorpusExternalSort.key
  val DefaultPopulation="phase2-fictional-population-v1"
  case class Config(seed: String, components: Int, first: Int, last: Int, expense: Long, receipt: Long, commitment: Long,variable:Boolean=false,population:String=DefaultPopulation) {
    require(seed.matches("[A-Za-z0-9_-]{1,64}") && components >= 2 && components <= 100000)
    require(population.matches("[A-Za-z0-9_-]{1,64}"))
    require(first >= 2031 && last <= 2044 && first <= last)
    require(Vector(expense,receipt,commitment).forall(x => x >= 1200 && x <= 1000000000000L))
  }
  def config(file: File): Config = {
    require(file.length() <= 16384)
    val data = mutable.Map[String,String]()
    val n = ConnectedViewsCheck.scan(file) { r => require(r.keySet == Set("key","value") && !data.contains(r("key"))); data(r("key"))=r("value") }
    val required=Set("seed","components","firstYear","lastYear","expenseCents","receiptCents","commitmentCents")
    require(n>=7 && n<=9 && required.subsetOf(data.keySet.toSet) && data.keySet.subsetOf(required++Set("variability","population")))
    require(data.get("variability").forall(_==ComponentVariability.Version))
    Config(data("seed"),data("components").toInt,data("firstYear").toInt,data("lastYear").toInt,data("expenseCents").toLong,data("receiptCents").toLong,data("commitmentCents").toLong,data.contains("variability"),data.getOrElse("population",DefaultPopulation))
  }
  def close(p: PrintWriter): Unit = IdentityAudit.close(p)
  private[corpus] def id(seed: String, parts: Any*): String = "N" + MessageDigest.getInstance("SHA-256").digest((seed+"|"+parts.mkString("|")).getBytes("UTF-8")).take(16).map(b=>f"${b & 255}%02x").mkString
  private[corpus] def vendor(c:Config,index:Int):String=StablePartyIdentity.generatedVendor(c.population,"recipe-supplier:"+index)
  private[corpus] def vendorName(c:Config,index:Int):String="Fictional Supplier "+vendor(c,index)
  private[corpus] def employee(c:Config,index:Int):String=StablePartyIdentity.opaque("synthetic-employee",c.population+"|"+index,"e")
  private[corpus] def payer(c:Config,index:Int):String=StablePartyIdentity.opaque("synthetic-payer",c.population+"|"+index,"r")
  private[corpus] def eventId(c:Config,index:Int,year:Int,family:String,slot:Int):String=if(c.variable)id(c.seed,ComponentVariability.Version,index,year,family,slot)else id(c.seed,index,year,family,slot)
  private[corpus] def eventDay(fy:Int,month:Int,i:Int):LocalDate={
    val first=LocalDate.of(fy-1,7,1).plusMonths(month)
    if((month==7 && first.isLeapYear && i%29==0) || (month==11 && i%13==0))first.withDayOfMonth(first.lengthOfMonth()) else first.plusDays(9+(i%10))
  }
  private def scanRaw(file: File)(f: Vector[String] => Unit): Unit = {
    val s=scala.io.Source.fromFile(file,"UTF-8")
    try s.getLines().foreach(l=>f(IdentityAudit.strict(l).getOrElse(throw new IllegalArgumentException("Invalid source CSV")))) finally s.close()
  }
  val lineageHeader=Vector("locator","event_id","family","entity_id","effective_date","available_date","amount","source_file","source_row","posting_period","generation_class")
  def generate(configFile: File, base: File, histories: File, target: File, chunk: Int, calibration:Option[File]=None,reused:Option[File]=None): Unit = {
    val c=config(configFile); val budgets=new PhaseTwoCalibration.Budgets(c,calibration,ReassignedRouting.totals(reused,c));val out=new File(target.getPath+".attempt")
    require(!target.exists() && !out.exists() && out.mkdirs())
    val gen=new File(out,"generation"); gen.mkdirs()
    val metrics=writer(gen,"sort_metrics.csv",CorpusExternalSort.header)
    Files.copy(configFile.toPath,new File(out,"config.csv").toPath)
    SyntheticCalendar.manifest(out,c.first,c.last+20)
    calibration.foreach(f=>Files.copy(f.toPath,new File(out,"annual_calibration.csv").toPath))
    val templates=new File(out,"preparation/input_templates");require(templates.mkdirs())
    Vector("base"->base,"histories"->histories).foreach{case(kind,dir)=>
      val dst=new File(templates,kind);dst.mkdirs()
      dir.listFiles().filter(f=>f.isFile && (if(kind=="base")f.getName.startsWith("aRef")||f.getName=="VAAccountingRules.csv"else f.getName.endsWith(".csv")||f.getName=="GENERATION_COMPLETE")).foreach(f=>Files.copy(f.toPath,new File(dst,f.getName).toPath))
    }
    val registry=new File(gen,"registry.csv"); val rp=new PrintWriter(registry,"UTF-8")
    val vendors=(c.components+1)/2
    try {
      (1 to vendors).foreach { i=> row(rp,key(f"V$i%08d"),f"V$i%08d","VENDOR",vendor(c,i),vendorName(c,i),f"SIM_I${(i-1)%32+1}%06d") }
      row(rp,key("MISSING"),"MISSING","VENDOR","0000000000","Fictional Unresolved Supplier","")
      (1 to c.components).foreach { i=> row(rp,key(f"E$i%08d"),f"E$i%08d","EMP",employee(c,i),"Fictional Officer "+employee(c,i).takeRight(12),"") }
      row(rp,key("RECEIPTS"),"RECEIPTS","REV","R_AGENCY_SUMMARY","Fictional Receipts","")
    } finally close(rp)
    val raw=new File(gen,"instructions.csv"); val p=new PrintWriter(raw,"UTF-8")
    val controls=writer(gen,"context_controls.csv",Seq("private_context","fiscal_year","family","expected_cents","actual_cents","events","status"))
    val ids=new File(gen,"event_ids.csv"); val ip=new PrintWriter(ids,"UTF-8")
    var expectedAll=0L;var expectedFinancial=0L
    val contextInput=new File(gen,"context_definitions.csv");val cp=new PrintWriter(contextInput,"UTF-8")
    try { (1 to c.components).foreach(i=>(c.first to c.last).foreach(fy=>row(cp,key(f"$i%012d",fy.toString),i,fy,id(c.seed,"context",i,fy),f"V${(i+1)/2}%08d",f"E$i%08d",i%3))) }finally close(cp)
    var lastContext=""
    try scanRaw(contextInput) { definition=>
      require(definition.size==7 && definition(0)>lastContext,"Context definition key/shape");lastContext=definition(0)
      val i=definition(1).toInt;val fy=definition(2).toInt;val context=definition(3)
      Vector("EXP"->budgets.component(fy,"EXP",i),"REV"->budgets.component(fy,"REV",i),"PO"->budgets.component(fy,"PO",i),"EMP"->0L).foreach { case(family,total)=>
        var used=0L
        val events=ComponentVariability.events(c,i,fy,family,total)
        events.foreach { e=>
          val slot=e.slot;val day=e.day
          val event=eventId(c,i,fy,family,slot)
          val source=family match { case "REV"=>"RECEIPTS"; case "EMP"=>definition(5); case "EXP" if i%20==0=>"MISSING"; case _=>definition(4) }
          val amount=e.cents; used+=amount
          val available=e.available
          // Cross-process references are deliberately absent for disconnected profiles.
          val background=(family=="EMP" && slot==1) || (family=="REV" && i%10==0)
          val visible=if(i%3==0 && !background)id(c.seed,"business",i,fy) else ""
          row(p,key(source,day.toString,event),source,event,day,available,family,amount,visible,if(background)id(c.seed,"background",i,fy,family) else context,fy,"101","201","301","wholly_generated","",day.getMonthValue)
          row(ip,key(event),event)
          expectedAll+=1;if(family!="EMP")expectedFinancial+=1
        }
        require(used==total); row(controls,context,SyntheticCalendar.year(fy),family,total,used,events.size,"PASS")
      }
    } finally { close(p); close(ip); close(controls) }
    reused.foreach {root=>
      val rr=new PrintWriter(new java.io.FileOutputStream(registry,true));val pp=new PrintWriter(new java.io.FileOutputStream(raw,true));val ii=new PrintWriter(new java.io.FileOutputStream(ids,true))
      val n=try ReassignedRouting.append(root,c,rr,pp,ii,new File(out,"private"),gen,metrics,chunk)finally{close(rr);close(pp);close(ii)}
      expectedAll+=n;expectedFinancial+=n
      val dst=new File(out,"reused_activity");require(dst.mkdirs())
      root.listFiles().filter(_.isFile).foreach(f=>Files.copy(f.toPath,new File(dst,f.getName).toPath))
    }
    val registryAll=new File(gen,"registry_all_sorted.csv");CorpusExternalSort(registry,registryAll,chunk,metrics)
    val registrySorted=new File(gen,"registry_sorted.csv");val uniqueRegistry=new PrintWriter(registrySorted,"UTF-8");var previousRegistry=Vector.empty[String]
    try scanRaw(registryAll){r=>if(previousRegistry.nonEmpty && r(1)==previousRegistry(1))require(r==previousRegistry,"Ambiguous registry source key")else{uniqueRegistry.println(csv(r));previousRegistry=r}}finally close(uniqueRegistry)
    val entityKeys=new File(gen,"registry_entity_keys.csv");val ek=new PrintWriter(entityKeys,"UTF-8")
    try scanRaw(registrySorted){r=>row(ek,(Vector(key(r(2),r(3)))++r.drop(1)):_*)}finally close(ek)
    val entityKeysSorted=new File(gen,"registry_entity_sorted.csv");CorpusExternalSort(entityKeys,entityKeysSorted,chunk,metrics)
    var previousEntity="";scanRaw(entityKeysSorted){r=>require(r(0)!=previousEntity,"Synthetic registry actor collision");previousEntity=r(0)}
    val unique=new File(gen,"event_ids_sorted.csv"); CorpusExternalSort(ids,unique,chunk,metrics)
    var previous=""; var uniqueCount=0L
    scanRaw(unique){r=>require(r(1)!=previous,"Duplicate event ID");previous=r(1);uniqueCount+=1}
    require(uniqueCount==expectedAll)
    val counts=writer(out,"cardinality_manifest.csv",Seq("metric","value"))
    try {row(counts,"instructions",expectedAll);row(counts,"financial_sources",expectedFinancial);row(counts,"responsibility_sources",expectedAll-expectedFinancial)}finally close(counts)
    val determination=new File(gen,"determination_sorted.csv"); CorpusExternalSort(raw,determination,chunk,metrics)
    val resolved=new File(gen,"resolved.csv"); val op=new PrintWriter(resolved,"UTF-8")
    val reg=new Input(registrySorted,"registry",false); var current=reg.next();var lastRegistry=""
    def advance():Unit={lastRegistry=current.get(1);current=reg.next();current.foreach(r=>require(r(1)>lastRegistry,"Registry duplicate/order"))}
    try scanRaw(determination){r=>
      while(current.nonEmpty && current.get(1)<r(1))advance()
      require(current.exists(_(1)==r(1)),"Unmatched identity");val v=current.get
      require(v(2)==(if(Set("EXP","PO")(r(5)))"VENDOR" else r(5)))
      row(op,(Vector(key(v(2),v(3),r(3),r(2)),v(2),v(3),v(4),v(5))++r.slice(2,10)++Vector(r(1))++r.drop(10)):_*)
    } finally {reg.close();close(op)}
    val entity=new File(gen,"entity_sorted.csv"); CorpusExternalSort(resolved,entity,chunk,metrics)
    emit(entity,entityKeysSorted,out,c)
    PhaseTwoLifecycle.generate(out,c,budgets,Some(entityKeysSorted))
    TreasurySourceCase.generate(out,c,budgets)
    copyReferences(base,histories,out,c)
    ReassignedReferences.augment(entity,out,gen,metrics,chunk)
    provenance(base,out)
    close(metrics)
    // Sorted boundaries are retained for first review; no unbounded registry state.
    val manifest=writer(out,"manifest.csv",Seq("path","bytes","sha256"))
    def files(d:File):Unit=Option(d.listFiles()).getOrElse(Array.empty[File]).sortBy(_.getName).foreach { f=>
      if(f.isDirectory)files(f) else if(f.getName!="manifest.csv")row(manifest,out.toPath.relativize(f.toPath),f.length(),ComponentPilot.sha(f))
    }
    try files(out) finally close(manifest)
    Files.write(new File(out,"GENERATION_COMPLETE").toPath,"fictional-phase2-v1 source-preparation-only\n".getBytes("UTF-8"))
    Files.move(out.toPath,target.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"RESULT phase2_generation components=${c.components} years=${c.last-c.first+1} financial_sources=$expectedFinancial output=$target")
  }
  private def provenance(base:File,out:File):Unit={
    val repo=base.getCanonicalFile.getParentFile.getParentFile.getParentFile
    val source="02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/"
    val paths=Vector("PhaseTwoCorpus.scala","PhaseTwoLifecycle.scala","PhaseTwoCheck.scala","PhaseTwoSchemas.scala","PhaseTwoCalibration.scala","CorpusExternalSort.scala","StablePartyIdentity.scala","SyntheticCalendar.scala","ComponentVariability.scala","ReassignedActivity.scala","ReassignedRouting.scala","ReassignedReferences.scala","ReassignedEligibility.scala","TreasurySourceCase.scala","CorpusSourceAssessment.scala").map(source+_) ++
      Vector("scripts/run_phase2_corpus.sh","docs/PHASE_TWO_GENERATOR_CONTRACT_V1.md","docs/PHASE_TWO_CALIBRATION_MAP_V1.md","docs/PHASE_TWO_CORPUS_SPEC_V1.md","docs/PHASE_TWO_EXECUTION_PLAN.md",
      "data/test_evidence/report_sorted_v3_20261116/README.md","data/test_evidence/report_sorted_v3_20261116/report_profile.csv","data/test_evidence/report_sorted_v3_20261116/extract_metrics.csv","data/test_evidence/report_sorted_v3_20261116/sort_manifest.csv")
    paths.foreach {p=>val f=new File(repo,p);require(f.isFile && f.length()<=4*1024*1024,s"Missing bounded preparation source $p");val d=new File(out,"preparation/"+p);d.getParentFile.mkdirs();Files.copy(f.toPath,d.toPath)}
    Files.write(new File(out,"README.txt").toPath,("Phase 2 two-class synthetic corpus v2. All modeled actors are fictional.\n"+
      "wholly_generated activity comes from recipes; reassigned_activity reuses selected real expense amounts/dates/classifications with constructed supplier identities.\n"+
      "Primary source evidence uses SY labels; calendar.csv explains ISO backing dates used only by legacy financial adapters. Do not compare SY date strings lexically.\n"+
      "fictional-fund is the aggregate experimental scope over all generated posting funds; treasury-source-case is an isolated source-only contractual specimen.\n"+
      "config.csv and preparation/ preserve recipes, assumptions and aggregate calibration provenance.\n"+
      "annual_calibration.csv, when present, overrides EXP/REV config fallback budgets; PO envelopes remain assumed.\n"+
      "Calibrated opening cash is an assumed quarter of first-year expenses, not a posted or empirically calibrated opening ledger.\n"+
      "VARawFiles, VAPOData, VendorMaster and references feed unchanged Scala TTL/ARE/posting.\n"+
      "evidence/ holds operational observations; private/, reused_activity/ and generation/ contain source-use/control/answer information and must not be historical model evidence.\n"+
      "Event identity is scoped to the immutable corpus snapshot; actor identity follows the separately versioned stable-party contract.\n"+
      "generation/ boundaries are retained for inspection. Histories explicitly translate recipe ISO dates by +28 years; reference manifest is rebuilt.\n"+
      "Scenario arithmetic and future references are input conditions, not implemented forecasting or probability estimates.\n"+
      "No real VA party links are inferred; no Q2 evaluation or target-runtime support is claimed.\n").getBytes("UTF-8"))
  }
  private def emit(entity:File, registry:File, out:File,c:Config):Unit={
    val files=mutable.Map[String,(PrintWriter,Long)]()
    def source(path:String,header:Vector[String],r:Vector[String],po:Boolean=false):Long={
      require(files.contains(path)||files.size<400,"Source partition writer bound")
      def encode(r:Vector[String]):String=if(po)r.map(x=>"\""+x.replace("\"","\"\"")+"\"").mkString("\t") else csv(r)
      val (p,n)=files.getOrElseUpdate(path,{
        val f=new File(out,path); f.getParentFile.mkdirs();val w=new PrintWriter(f,"UTF-8");w.println(encode(header));(w,0L)
      });p.println(encode(r));files(path)=(p,n+1);n+1
    }
    val lineage=writer(out,"source_lineage.csv",lineageHeader)
    val evidenceDir=new File(out,"evidence");evidenceDir.mkdirs()
    val evidence=writer(evidenceDir,"financial_events.csv",PhaseTwoSchemas.headers("financial_events.csv"))
    val employees=writer(evidenceDir,"responsibility.csv",Seq("record_id","local_employee","effective_date","available_date","business_ref","role"))
    val priv=new File(out,"private");priv.mkdirs()
    val truth=writer(priv,"event_truth.csv",Seq("event_id","private_context","family","process_entity_key"))
    val expH=Vector("AGENCY_NAME","AMOUNT","FUND_NAME","LOCALITY","OBJECT_NAME","PROGRAM_NAME","VENDOR_NAME","VOUCHER_NUMBER","VOUCHER_DATE")
    val revH=Vector("AGENCY_NAME","AMOUNT","FUND_NAME","LOCALITY","SOURCE_TYPE","SOURCE","DEPOSIT_DATE")
    val poH=Vector("PONUMBER","AGENCY","ORDEREDDATE","VENDORCOMMODITYDESC","QUANTITYORDERED","PRICE","VENDORID","VENDORNAME","VENDORADDRESS","VENDORCITY","VENDORSTATE","VENDORPOSTALCODE","VENDORLOC_EMAILADDRESS","NIGPCODE","NIGPDESCRIPTION","UOMCode","UOMDesc","VENDORPARTNUMBER","MANPARTNUMBER")
    try scanRaw(entity){r=>
      require(r.size==20);val family=r(8);val day=LocalDate.parse(r(6));val fy=r(12);val event=r(5)
      val amount=BigDecimal(r(9))./(100).setScale(2).bigDecimal.toPlainString
      val postingMonth=r(19).toInt
      val postingDay=LocalDate.of(fy.toInt-(if(postingMonth>=7)1 else 0),postingMonth,1)
      val month=postingDay.format(DateTimeFormatter.ofPattern("MMM",java.util.Locale.US))
      val date=day.format(DateTimeFormatter.ofPattern("MM-dd-yy"))
      val local=if(family=="REV")"A1" else if(family=="EMP")id(c.population,"responsibility-local",r(2)) else "L"+r(2)
      if(family=="EMP")SyntheticCalendar.row(employees,event,local,day,r(7),r(10),"responsible")
      else {
        val (path,ordinal)=family match {
          case "EXP"=> val path=s"VARawFiles/FY $fy Expenditure/Expenditures $month ${postingDay.getYear}.csv"
            (path,source(path,expH,Vector(if(r(17)=="reassigned_activity")ReassignedReferences.name("agency",r(14))else"SIMULATED AGENCY",amount,if(r(17)=="reassigned_activity")ReassignedReferences.name("fund",r(15))else"SIMULATED FUND","FICTIONAL",if(r(17)=="reassigned_activity")ReassignedReferences.name("object",r(16))else"SIMULATED OBJECT, SERVICES","SIMULATED PROGRAM",r(3),event,date)))
          case "REV"=> val path=s"VARawFiles/FY $fy Revenue/Revenues $month ${day.getYear}.csv"
            (path,source(path,revH,Vector("SIMULATED AGENCY",amount,"SIMULATED FUND","FICTIONAL","SIMULATED","SIMULATED RECEIPTS",date)))
          case "PO"=> val path=s"VAPOData/VA_opendata_FY$fy.txt"
            (path,source(path,poH,Vector(event,"SIMULATED AGENCY",day.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),"Fictional services","1",amount,r(2),r(3),"Shared fictional address","Fictional City","VA","00000","fiction@example.invalid","06000","Fictional services","EA","each","",""),true))
        }
        val locator="SRC1:"+java.util.Base64.getUrlEncoder.withoutPadding.encodeToString(path.getBytes("UTF-8"))+":"+ordinal
        val period=if(family=="PO")day.getYear+f"/${day.getMonthValue}%02d"else f"${fy.toInt%100}%04d/$postingMonth%02d"
        row(lineage,locator,event,family,if(family=="REV")"0000000000" else r(2),day,r(7),amount,path,ordinal,period,r(17))
        SyntheticCalendar.row(evidence,event,family,if(r(2)=="0000000000" && family!="REV")"" else local,day,r(7),r(9),"USD",r(10),if(family=="REV")"agency_summary" else if(r(2)=="0000000000")"missing_vendor" else "resolved",r(17),SyntheticCalendar.year(fy.toInt)+f"-$postingMonth%02d",r(14),r(15),r(16))
      }
      row(truth,event,r(11),family,r(2))
    }finally {files.values.foreach(x=>close(x._1));close(lineage);close(evidence);close(employees);close(truth)}
    val master=writer(out,"VendorMaster.csv",Seq("instID","instHolderName","instEffectDate","instEffectEndDate","instSizeTier","instGeoRegion","instContractType","instPaymentChannel","instCounterpartySector","instExposureType","instFXCurrency"))
    val assignments=writer(out,"synthetic_assignments.csv",Seq("vendor_id","template_instrument_id"))
    try scanRaw(registry){r=>if(r(2)=="VENDOR" && r(3)!="0000000000"){
      row(master,r(3),r(4),s"${c.first-1}-01-01",s"${c.first+1}-05-31","SMALL","VA","CONTRACTED","ACH","COMMERCIAL","SERVICE","USD")
      row(master,r(3),r(4),s"${c.first+1}-06-01","9999-99-99","LARGE","VA","CONTRACTED","ACH","COMMERCIAL","SERVICE","USD")
      row(assignments,r(3),r(5))
    }}finally {close(master);close(assignments)}
  }
  private def copyReferences(base:File,histories:File,out:File,c:Config):Unit={
    Vector("aRefAgencyCSV.txt","aRefFundCSV.txt","aRefObjectCSV.txt","aRefProgramCSV.txt","aRefSourceCSV.txt","VAAccountingRules.csv").foreach { n=>Files.copy(new File(base,n).toPath,new File(out,n).toPath) }
    val dst=new File(out,"synthetic_histories");dst.mkdirs()
    // Explicit source-preparation translation of the bounded historical recipe calendar.
    histories.listFiles().filter(f=>f.isFile && f.getName.endsWith(".csv") && f.getName!="manifest.csv").foreach { f=>
      val in=new Input(f,"history_recipe",true);val p=writer(dst,f.getName,in.header)
      try {var next=in.next();while(next.nonEmpty){
        require(in.count<=10000);p.println(csv(next.get.map(x=>if(x.matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}"))LocalDate.parse(x).plusYears(28).toString else x)));next=in.next()
      }}finally {in.close();close(p)}
    }
    val hm=writer(dst,"manifest.csv",Seq("file","bytes","sha256"))
    try dst.listFiles().filter(f=>f.isFile && f.getName!="manifest.csv").sortBy(_.getName).foreach(f=>row(hm,f.getName,f.length(),ComponentPilot.sha(f)))finally close(hm)
    Files.write(new File(dst,"GENERATION_COMPLETE").toPath,"explicit-calendar-translation-plus28-years\n".getBytes("UTF-8"))
  }
  def main(args:Array[String]):Unit={val p=LedgerApp.parseArgs(args);generate(new File(p("config")),new File(p("base")),new File(p("histories")),new File(p("out")),p.getOrElse("chunk","100000").toInt,p.get("calibration").map(new File(_)),p.get("reused").map(new File(_)))}
}
