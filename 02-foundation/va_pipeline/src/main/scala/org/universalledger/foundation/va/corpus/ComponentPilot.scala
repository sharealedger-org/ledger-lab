package org.universalledger.foundation.va.corpus

import java.io.{File, PrintWriter}
import java.nio.file.{Files, StandardCopyOption}
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport, SortEngine}
import scala.collection.mutable

/** Bounded, reproducible INPUT fixture generation, not a financial engine or corpus registry. */
object ComponentPilot {
  import ReportingSupport._
  val MaxRows = 4096
  val MaxSlots = 64
  val registryHeader = Vector("source_key", "process", "entity_id", "name", "template_id")
  val budgetHeader = Vector("context_id", "fiscal_year", "family", "budget_usd")
  val slotHeader = Vector("context_id", "fiscal_year", "family", "source_key", "event_id", "effective_date", "available_date", "weight", "visible_project")
  val lineageHeader = Vector("event_id", "family", "entity_id", "effective_date", "available_date", "project_id", "amount", "source_file", "source_row", "synthetic")
  private val dateFormat = DateTimeFormatter.ofPattern("MM-dd-yy")
  def sha(f: File): String = {
    val in = Files.newInputStream(f.toPath); val d = MessageDigest.getInstance("SHA-256"); val b = new Array[Byte](65536)
    try { var n = in.read(b); while (n >= 0) { d.update(b, 0, n); n = in.read(b) } } finally in.close()
    d.digest().map(x => f"${x & 255}%02x").mkString
  }
  private def output(f: File, header: Seq[String]): PrintWriter = { f.getParentFile.mkdirs(); writer(f.getParentFile, f.getName, header) }
  private def close(p: PrintWriter): Unit = { p.flush(); val bad = p.checkError(); p.close(); require(!bad, "Output write failed") }
  private def scan(f: File, header: Vector[String])(consume: Vector[String] => Unit): Long = {
    require(f.length() <= 4L * 1024 * 1024, s"Pilot input byte limit: $f")
    val in = new Input(f, "pilot", header.nonEmpty)
    try {
      if (header.nonEmpty) require(in.header == header, s"Schema: $f")
      var next = in.next()
      while (next.nonEmpty) {
        require(in.count <= MaxRows && (header.isEmpty || next.get.size == header.size), s"Pilot row/shape limit: $f")
        consume(next.get); next = in.next()
      }
      in.count
    } finally in.close()
  }
  // Single encoded key avoids SortEngine's adjacent variable-width field concatenation ambiguity.
  private def key(parts: String*): String = parts.map(_.getBytes("UTF-8").map(b => f"${b & 255}%02x").mkString).mkString("!")
  private def sorted(in: File, out: File, chunk: Int, metrics: PrintWriter, process: String): Unit = {
    require(chunk >= 1 && chunk <= MaxRows)
    val src = scala.io.Source.fromFile(in, "UTF-8")
    try {
      var n = 0
      val rows = src.getLines().map { line => n += 1; require(n <= MaxRows && (n + chunk - 1) / chunk <= 64, "Pilot sort row/fan-in limit"); line }
      val result = SortEngine.externalSort(rows, out.getPath, Seq(SortEngine.SortField("encoded_key", 0, "CH", "A")), chunk, out.getPath + ".sort")
      row(metrics, process, result._1, result._1, result._2, out.length())
    } finally src.close()
  }
  def generate(spec: File, base: File, histories: File, out: File, chunk: Int = 16): Unit = {
    require(!out.exists() && out.mkdirs(), "New output root required")
    val work = new File(out, "generation"); work.mkdirs()
    val metrics = output(new File(work, "process_metrics.csv"), Seq("process_id", "rows_in", "rows_out", "spills", "output_bytes"))
    try {
      val combined = new File(work, "context_input.csv"); val p = new PrintWriter(combined, "UTF-8")
      try {
        scan(new File(spec, "budgets.csv"), budgetHeader) { r =>
          require(Set("2003", "2004")(r(1)) && Set("EXP", "REV", "PO", "EMP")(r(2)))
          require(BigDecimal(r(3)) >= 0 && (r(2) != "EMP" || BigDecimal(r(3)) == 0))
          p.println(csv(Vector(key(r(0),r(1),r(2),"0"),r(0),r(1),r(2),"B",r(3))))
        }
        scan(new File(spec, "slots.csv"), slotHeader) { r =>
          val d = LocalDate.parse(r(5)); require(LocalDate.parse(r(6)).compareTo(d) >= 0)
          require((d.getYear + (if (d.getMonthValue >= 7) 1 else 0)).toString == r(1), "Event outside fiscal year")
          require(BigDecimal(r(7)) > 0)
          p.println(csv(Vector(key(r(0),r(1),r(2),"1",r(4)),r(0),r(1),r(2),"S") ++ r.drop(3)))
        }
      } finally close(p)
      val contextSorted = new File(work, "context_sorted.csv"); sorted(combined, contextSorted, chunk, metrics, "context_sort")
      val instructions = new File(work, "instructions.csv"); val emit = new PrintWriter(instructions, "UTF-8")
      val controls = output(new File(work, "context_controls.csv"), Seq("context_id", "fiscal_year", "family", "budget", "generated", "events", "status", "anchor_status"))
      var current = Vector.empty[String]; var budget: Option[BigDecimal] = None
      val slots = mutable.ArrayBuffer[Vector[String]](); var contexts = 0L; var emitted = 0L
      val seenEvents = mutable.HashSet[String]()
      def flush(): Unit = if (current.nonEmpty) {
        require(budget.nonEmpty && slots.nonEmpty, "Missing budget or slots")
        val totalWeight = slots.map(r => BigDecimal(r(9))).sum
        var used = BigDecimal(0)
        slots.zipWithIndex.foreach { case (r, i) =>
          val amount = if (i == slots.size - 1) budget.get - used else (budget.get * BigDecimal(r(9)) / totalWeight).setScale(2, BigDecimal.RoundingMode.HALF_UP)
          used += amount; require(amount >= 0 && seenEvents.add(r(6)), "Duplicate event or negative envelope remainder")
          require(seenEvents.size <= MaxRows)
          emit.println(csv(Vector(key(r(5),r(7),r(6)),r(5),r(6),r(7),r(8),r(3),amount.bigDecimal.toPlainString,r(10),r(1),r(2))))
          emitted += 1
        }
        require(used == budget.get)
        row(controls, current(0), current(1), current(2), budget.get, used, slots.size, "PASS", "SYNTHETIC_ENVELOPE_NOT_VA_ANCHOR")
        contexts += 1
      }
      try scan(contextSorted, Vector.empty) { r =>
        val group = r.slice(1,4)
        if (group != current) { flush(); current = group; budget = None; slots.clear() }
        if (r(4) == "B") { require(budget.isEmpty, "Duplicate budget"); budget = Some(BigDecimal(r(5))) }
        else { require(r.size == 11 && slots.size < MaxSlots, "Context slot bound/shape"); slots += r }
      } finally { try flush() finally { close(emit); close(controls) } }
      row(metrics, "context_generation", contexts, emitted, 0, instructions.length())

      val registryRaw = new File(work, "registry_input.csv"); val reg = new PrintWriter(registryRaw, "UTF-8")
      try scan(new File(spec, "identities.csv"), registryHeader) { r => reg.println(csv(Vector(key(r(0))) ++ r)) } finally close(reg)
      val registrySorted = new File(work, "registry_sorted.csv"); sorted(registryRaw, registrySorted, chunk, metrics, "registry_sort")
      val assignmentSorted = new File(work, "assignment_sorted.csv"); sorted(instructions, assignmentSorted, chunk, metrics, "determination_sort")
      val resolved = new File(work, "resolved.csv"); val resolvedOut = new PrintWriter(resolved, "UTF-8")
      val registry = new Input(registrySorted, "identity", false)
      var identity = registry.next(); var previousSource = ""
      val entityIds = mutable.HashSet[String](); val entityDefinitions = output(new File(out, "entity_registry.csv"), registryHeader)
      def accept(r: Vector[String]): Unit = {
        require(r.size == 6 && r(1) > previousSource, "Duplicate/unsorted source assignment")
        require(Set("VENDOR", "REV", "EMP")(r(2)) && r(3).nonEmpty && entityIds.add(r(3)), "Invalid or ambiguous entity registry")
        if (r(2) == "VENDOR") require(r(3).matches("c[0-9]+") && r(5).nonEmpty)
        previousSource = r(1); entityDefinitions.println(csv(r.drop(1)))
      }
      identity.foreach(accept)
      var assigned = 0L
      try {
        scan(assignmentSorted, Vector.empty) { r =>
          while (identity.nonEmpty && identity.get(1) < r(1)) { identity = registry.next(); identity.foreach(accept) }
          require(identity.exists(_(1) == r(1)), s"Unmatched source assignment: ${r(1)}")
          val id = identity.get; val expected = if (Set("EXP", "PO")(r(5))) "VENDOR" else r(5)
          require(id(2) == expected, "Family/identity process mismatch")
          resolvedOut.println(csv(Vector(key(id(2),id(3),r(3),r(2)),id(2),id(3),id(4),id(5)) ++ r.slice(2,10) ++ Vector(r(1))))
          assigned += 1
        }
        while (identity.nonEmpty) { identity = registry.next(); identity.foreach(accept) }
      } finally { registry.close(); close(resolvedOut); close(entityDefinitions) }
      require(assigned == emitted)
      row(metrics, "sorted_assignment_merge", emitted, assigned, 0, resolved.length())
      val entitySorted = new File(work, "entity_sorted.csv"); sorted(resolved, entitySorted, chunk, metrics, "entity_sort")
      emitSources(entitySorted, out, metrics)
      Vector("aRefAgencyCSV.txt", "aRefFundCSV.txt", "aRefObjectCSV.txt", "aRefProgramCSV.txt", "aRefSourceCSV.txt", "VAAccountingRules.csv").foreach { n => Files.copy(new File(base,n).toPath,new File(out,n).toPath) }
      val historyOut = new File(out, "synthetic_histories"); historyOut.mkdirs()
      histories.listFiles().filter(_.isFile).foreach(f => Files.copy(f.toPath, new File(historyOut,f.getName).toPath))
      val config = output(new File(out, "generation_config.csv"), Seq("key", "value"))
      try {
        row(config,"version","component-pilot-0304-v1"); row(config,"identity_allocation","supplied_registry_no_runtime_allocator")
        row(config,"amount_policy","declared_weights_last_slot_cent_remainder"); row(config,"va_anchor_status","unverified_for_2003_2004")
        row(config,"employee_scope","responsibility_evidence_not_payroll"); row(config,"max_input_rows",MaxRows); row(config,"max_context_slots",MaxSlots)
      } finally close(config)
      val receipt = output(new File(out,"input_manifest.csv"),Seq("file","bytes","sha256"))
      try { (Vector("budgets.csv","slots.csv","identities.csv").map(n => new File(spec,n)) ++ Vector("VAAccountingRules.csv","aRefAgencyCSV.txt","aRefFundCSV.txt","aRefObjectCSV.txt","aRefProgramCSV.txt","aRefSourceCSV.txt").map(n => new File(base,n))).foreach(f => row(receipt,f.getName,f.length(),sha(f))) } finally close(receipt)
    } finally close(metrics)
    Files.write(new File(out,"GENERATION_COMPLETE").toPath,"component-pilot-0304-v1\n".getBytes("UTF-8"))
  }

  private def emitSources(sortedFile: File, out: File, metrics: PrintWriter): Unit = {
    val expHeader = Vector("AGENCY_NAME","AMOUNT","FUND_NAME","LOCALITY","OBJECT_NAME","PROGRAM_NAME","VENDOR_NAME","VOUCHER_NUMBER","VOUCHER_DATE")
    val revHeader = Vector("AGENCY_NAME","AMOUNT","FUND_NAME","LOCALITY","SOURCE_TYPE","SOURCE","DEPOSIT_DATE")
    val poHeader = Vector("PONUMBER","AGENCY","ORDEREDDATE","VENDORCOMMODITYDESC","QUANTITYORDERED","PRICE","VENDORID","VENDORNAME","VENDORADDRESS","VENDORCITY","VENDORSTATE","VENDORPOSTALCODE","VENDORLOC_EMAILADDRESS","NIGPCODE","NIGPDESCRIPTION","UOMCode","UOMDesc","VENDORPARTNUMBER","MANPARTNUMBER")
    val files = mutable.Map[String,(PrintWriter,Long)]()
    def source(path: String, header: Vector[String], r: Vector[String], po: Boolean = false): Long = {
      require(files.contains(path) || files.size < 32, "Source writer bound")
      val pair = files.getOrElseUpdate(path, {
        val f = new File(out,path); f.getParentFile.mkdirs(); val p = new PrintWriter(f,"UTF-8")
        p.println(if(po) header.map(x => "\""+x+"\"").mkString("\t") else csv(header)); (p,0L)
      })
      pair._1.println(if(po) r.map(x => "\""+x.replace("\"","\"\"")+"\"").mkString("\t") else csv(r))
      files(path) = (pair._1,pair._2+1); pair._2+1
    }
    val lineage = output(new File(out,"visible/event_evidence.csv"),lineageHeader)
    val employees = output(new File(out,"visible/employee_responsibility.csv"),Seq("event_id","employee_id","effective_start","available_date","project_id","role","synthetic"))
    val truth = output(new File(out,"withheld/generation_truth.csv"),Seq("event_id","context_id","family","entity_id","synthetic"))
    var count=0L; var employeeRows=0L; var current=""; var lastDate=""
    try scan(sortedFile,Vector.empty) { r =>
      require(r.size == 14)
      val entityKey=r(1)+"|"+r(2)
      if(entityKey != current) { current=entityKey; lastDate="" }
      require(r(6)>=lastDate,"Entity temporal order"); lastDate=r(6)
      val family=r(8); val day=LocalDate.parse(r(6)); val fy=r(12); val amount=r(9); val event=r(5)
      val month=day.format(DateTimeFormatter.ofPattern("MMM",java.util.Locale.US)); val date=day.format(dateFormat)
      val (path,ordinal) = family match {
        case "EXP" =>
          val path=s"VARawFiles/FY $fy Expenditure/Expenditures $month ${day.getYear}.csv"
          (path,source(path,expHeader,Vector("SIMULATED AGENCY",amount,"SIMULATED FUND","VA","SIMULATED OBJECT, SERVICES","SIMULATED PROGRAM",r(3),event,date)))
        case "REV" =>
          val path=s"VARawFiles/FY $fy Revenue/Revenues $month ${day.getYear}.csv"
          (path,source(path,revHeader,Vector("SIMULATED AGENCY",amount,"SIMULATED FUND","VA","SIMULATED","SIMULATED RECEIPTS",date)))
        case "PO" =>
          val path=s"VAPOData/VA_opendata_FY$fy.txt"
          val values=Vector(event,"SIMULATED AGENCY",day.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),"SIMULATED COMPONENT SERVICES","1",amount,r(2),r(3),"SIMULATED ADDRESS","SIMULATED CITY","VA","00000","simulation@example.invalid","06000","SIMULATED SERVICES","EA","each","","")
          (path,source(path,poHeader,values,po=true))
        case "EMP" =>
          employeeRows += 1; row(employees,event,r(2),r(6),r(7),r(10),"RESPONSIBLE","true")
          ("visible/employee_responsibility.csv",employeeRows)
      }
      row(lineage,event,family,r(2),r(6),r(7),r(10),amount,path,ordinal,"true")
      row(truth,event,r(11),family,r(2),"true"); count+=1
    } finally { files.values.foreach(x => close(x._1)); close(lineage); close(employees); close(truth) }
    val master=output(new File(out,"VendorMaster.csv"),Seq("instID","instHolderName","instEffectDate","instEffectEndDate","instSizeTier","instGeoRegion","instContractType","instPaymentChannel","instCounterpartySector","instExposureType","instFXCurrency"))
    val assignments=output(new File(out,"synthetic_assignments.csv"),Seq("vendor_id","template_instrument_id"))
    val vendors=mutable.ArrayBuffer[Vector[String]]()
    scan(new File(out,"entity_registry.csv"),registryHeader)(r => if(r(1)=="VENDOR") vendors+=r)
    try vendors.sortBy(_(2)).foreach { r =>
      row(master,r(2),r(3),"2000-01-01","2004-05-31","SMALL","VA","CONTRACTED","ACH","COMMERCIAL","SERVICE","USD")
      row(master,r(2),r(3),"2004-06-01","9999-99-99","LARGE","VA","CONTRACTED","ACH","COMMERCIAL","SERVICE","USD")
      row(assignments,r(2),r(4))
    } finally { close(master); close(assignments) }
    row(metrics,"entity_source_emission",count,count,0,files.keys.map(p=>new File(out,p).length()).sum)
  }

  def main(args: Array[String]): Unit = {
    val p=LedgerApp.parseArgs(args); val out=new File(p("out")); val attempt=new File(out.getPath+".attempt")
    require(!out.exists() && !attempt.exists(),"New output required")
    generate(new File(p("spec")),new File(p("base")),new File(p("histories")),attempt,p.getOrElse("chunk","16").toInt)
    Files.move(attempt.toPath,out.toPath,StandardCopyOption.ATOMIC_MOVE)
    println(s"RESULT component_pilot=$out status=generated_not_posted")
  }
}
