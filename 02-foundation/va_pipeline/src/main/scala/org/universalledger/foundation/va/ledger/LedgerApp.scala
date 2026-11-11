package org.universalledger.foundation.va.ledger

//***************************************************************************************************************
//
//  Program:  LedgerApp - Main Financial System Patterns Dispatcher
//
//  This module is the main program of this project; from it all other modules may be selected.
//
//  USAGE — two modes:
//
//  (1) CLI / scriptable / agentic mode (preferred):
//      sbt "run --step <N> --inPath <path> --outPath <path> --year <YY> --quarter <QQ>"
//
//      Parameters:
//        --step     N    Step to run: 2=transStandardize, 3=post,
//                        5v=CKBViews (extract-time view aggregation, CCP3),
//                        20=GLPost (CCP1), 21=PermutationGenerator (CCP1)
//        --inPath   path Input data directory  (default: /VAdata/data/)
//        --outPath  path Output data directory (default: /VAdata/data/)
//        --year     YY   Two-digit fiscal year  (default: 03)
//        --quarter  QQ   Two-digit quarter, 05=revenue (default: 01)
//
//  (2) Interactive mode (original behaviour — unchanged):
//      sbt run          (no arguments → prompts exactly as before)
//
//  *(c) Copyright IBM Corporation. 2018
//  * SPDX-License-Identifier: Apache-2.0
//  * By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Added CLI arg mode for scriptable / agentic pipeline execution (Bob AI)
//***************************************************************************************************************

object LedgerApp {

  // ── defaults ──────────────────────────────────────────────────────────────
  val DefaultInPath  = "/VAdata/data/"
  val DefaultOutPath = "/VAdata/data/"
  val DefaultYear    = "03"
  val DefaultQuarter = "01"

  // ── simple arg parser: "--key value" pairs ────────────────────────────────
  def parseArgs(args: Array[String]): Map[String, String] = {
    val pairs = args.sliding(2, 2).collect {
      case Array(k, v) if k.startsWith("--") => k.stripPrefix("--") -> v
    }
    pairs.toMap
  }

  // ── banner labels (same step names as scripts/dashboard_full_corpus.sh) ───
  def stepLabel(step: String): String = step match {
    case "ttl" => "Pay JEs (APA transform)"
    case "2"   => "Standardize & Journalize"
    case "2p"  => "PO JEs"
    case "2m"  => "Merge payment + PO JEs"
    case "3"   => "Post"
    case "5v"  => "Views (CKB)"
    case "20"  => "GL Bal"
    case "21"  => "Permute"
    case other => other
  }

  def fmtElapsed(ms: Long): String = {
    val s = ms / 1000
    f"${s / 3600}%d:${(s % 3600) / 60}%02d:${s % 60}%02d"
  }

  /** Scope shown in the banner: one fiscal year, or all years for a multi-directory 5v pass. */
  def scopeLabel(step: String, year: String, params: Map[String, String]): String = {
    val dirs = params.get("inPaths").map(_.split(",").count(_.trim.nonEmpty)).getOrElse(0)
    if (step == "5v" && dirs > 0) s"all years ($dirs input directories)" else s"FY20$year"
  }

  def main(args: Array[String]): Unit = {

    val cli     = args.nonEmpty
    val params  = if (cli) parseArgs(args) else Map.empty[String, String]
    val step    = params.getOrElse("step", "interactive")
    val year    = params.getOrElse("year", DefaultYear)
    val quarter = params.getOrElse("quarter", DefaultQuarter)
    val label   = stepLabel(step)
    val summary =
      if (cli) s"step=$step${if (label != step) s" - $label" else ""}  ${scopeLabel(step, year, params)}  quarter=$quarter"
      else "interactive mode"
    val startMs = System.currentTimeMillis()
    // A step that calls sys.exit skips `finally`, so the end banner is also printed from a shutdown
    // hook. The hook cannot see the exit code, so that case is reported as "exited before completion".
    @volatile var status = "exited before completion (sys.exit)"
    val endPrinted = new java.util.concurrent.atomic.AtomicBoolean(false)
    def endBanner(): Unit = if (endPrinted.compareAndSet(false, true)) {
      println("*" * 100)
      println("                                  End of Ledger Lab step")
      println("                  " + summary)
      println(s"                  status=$status  elapsed=${fmtElapsed(System.currentTimeMillis() - startMs)}")
      println("*" * 100)
    }
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable { override def run(): Unit = endBanner() }))

    println("*" * 100)
    println("                                    Ledger Lab")
    println("                                    VA Corpus")
    println("                  Using State of Virginia Public Financial Data")
    println("                  " + summary)
    println("                  started " + java.time.LocalDateTime.now().withNano(0))
    println("*" * 100)

    // ── choose mode ──────────────────────────────────────────────────────────
    try {
      if (cli) runCLI(args) else runInteractive()
      status = "ok"
    } catch {
      case t: Throwable => status = "FAILED"; throw t
    } finally {
      endBanner()
    }
  }

  // ── CLI mode ─────────────────────────────────────────────────────────────
  def runCLI(args: Array[String]): Unit = {
    val params = parseArgs(args)

    val step        = params.getOrElse("step",    { printUsage(); sys.exit(1); "" })
    val inPath      = params.getOrElse("inPath",  DefaultInPath)
    val outPath     = params.getOrElse("outPath", DefaultOutPath)
    val year        = params.getOrElse("year",    DefaultYear)
    val quarter     = params.getOrElse("quarter", DefaultQuarter)
    val POfileIn    = "VA_opendata_FY20" + year + ".txt"
    // Step 2p reads PO files from --inPath directly (not a poData/ subdirectory)
    val inputPOPath = inPath

    println(s"[CLI] step=$step  inPath=$inPath  outPath=$outPath  year=$year  quarter=$quarter")
    println("-" * 35)

    dispatch(step, inPath, outPath, inputPOPath, POfileIn, year, params)
  }

  // ── Interactive mode (original behaviour, fully preserved) ────────────────
  def runInteractive(): Unit = {

    println("Type q at any prompt to quit the program")
    println("-" * 35)
    println("Select one of the following programs to run")
    println("   1 -Match POs to Payments (Spark):")
    println("   2 -Standardize & Journalize:")
    println("   3 -Post (match-merge posting engine):")
    println("   4 -Reconciliation / Contra Creation:")
    println("   5 -Data Aggregation:")
    println("   6 -Financial Allocation:")
    println("   7 -Consolidation and Elimination:")
    println("   8 -Forecasting and Budgeting:")
    println("   9 -Financial Modeling / Arrangement Reclass:")
    println("   10-Initialize Process Files:")
    println("   11-David Paget-Brown Spark:")
    println("-" * 35)

    println("Which program would you like to run?")
    val runModule = scala.io.StdIn.readLine().trim.toUpperCase()
    if (runModule.startsWith("Q")) sys.exit(0)
    println("OK, we'll run program number " + runModule + ".")
    println("-" * 35)

    println(s"The default input data location is $DefaultInPath")
    println("Would you like to change this? (y/n)")
    val chgIn = scala.io.StdIn.readLine().trim.toUpperCase()
    if (chgIn.startsWith("Q")) sys.exit(0)
    val fileInLocation = if (chgIn == "Y") scala.io.StdIn.readLine().trim else DefaultInPath

    println(s"The default output data location is $DefaultOutPath")
    println("Would you like to change this? (y/n)")
    val chgOut = scala.io.StdIn.readLine().trim.toUpperCase()
    if (chgOut.startsWith("Q")) sys.exit(0)
    val fileOutLocation = if (chgOut == "Y") scala.io.StdIn.readLine().trim else DefaultOutPath

    println(s"The default year is $DefaultYear")
    println("Would you like to change this? (y/n)")
    val chgYR = scala.io.StdIn.readLine().trim.toUpperCase()
    if (chgYR.startsWith("Q")) sys.exit(0)
    val inputYear = if (chgYR == "Y") scala.io.StdIn.readLine().trim else DefaultYear

    println(s"The default quarter is $DefaultQuarter  (enter 05 for Revenue Files)")
    println("Would you like to change this? (y/n)")
    val chgQTR = scala.io.StdIn.readLine().trim.toUpperCase()
    if (chgQTR.startsWith("Q")) sys.exit(0)
    val inputQTR = if (chgQTR == "Y") scala.io.StdIn.readLine().trim else DefaultQuarter

    println("start file handling:")
    println("module assumes input subdirectories of common, payment, poData")

    val inputPOPath = fileInLocation + "poData/"
    val POfileIn    = "VA_opendata_FY20" + inputYear + ".txt"

    dispatch(runModule, fileInLocation, fileOutLocation, inputPOPath, POfileIn, inputYear)
  }

  // ── shared dispatch ───────────────────────────────────────────────────────
  def dispatch(step: String, inPath: String, outPath: String,
               inputPOPath: String, POfileIn: String, year: String = "",
               params: Map[String, String] = Map.empty): Unit = {
    step match {
      // Step ttl: APA Technical Transform Layer — resolves name columns → key columns,
      // discovers FY monthly CSV subdirectories, and writes SortedJEFY{yr}q{n}exp.csv
      // (and SortedJEFY{yr}rev.csv when --type R or B).
      // --inPath  must point to the directory containing "FY 2003 Expenditure" etc.
      // --refPath must point to the directory containing aRef*.txt (default = inPath/../)
      case "ttl" => standardizeAndSort.applyAPA(params + ("inPath" -> inPath, "outPath" -> outPath, "year" -> year))

      // Step 2: standardize raw VA payment files + external merge-sort → SortedJE
      case "2"  => standardizeAndSort(params + ("inPath" -> inPath, "outPath" -> outPath, "year" -> year))

      // Step 2p: standardize PO file + external sort → SortedJEFY{yr}q{n}PO.csv
      // --inPath must point to the directory containing VA_opendata_FY20YY.txt
      case "2p" => standardizeAndSort.applyPO(params + ("inPath" -> inputPOPath, "outPath" -> outPath, "year" -> year))

      // Step 2m: k-way merge of payment SortedJE + PO SortedJE → SortedJEFY{yr}merged.csv
      // Requires --payment (path to payment SortedJE file) and --po (path to PO SortedJE file).
      // Defaults to the standard derived file names when not supplied.
      case "2m" => runMerge(params + ("outPath" -> outPath, "year" -> year))

      // Step 3: post sorted SJE to instrument ledger.
      // post.scala reads all SortedJEFY{yr}*.csv files as a k-way match-merge — no pre-merge needed.
      case "3"  => post(outPath, outPath, year)
      case "4"  => contraCreation(outPath)
      case "5"  => dataAggregation(outPath, inPath + "ViewSpec.csv")
      case "6"  => financialAllocation(outPath, params.getOrElse("allocationRules", inPath))
      case "7"  => consolidation(outPath)
      case "8"  => forecastingBudgeting(outPath)
      case "9"  => arrangementReclass(outPath)
      case "12" =>
        org.universalledger.engines.CurrencyRevaluation.run(
          new java.io.File(outPath, s"LDGR20${year}_OPENING.csv").getPath,
          params.getOrElse("fxRates", new java.io.File(inPath, "booked_fx_rates.csv").getPath),
          params.getOrElse("fxRules", new java.io.File(inPath, "booked_fx_rules.csv").getPath),
          new java.io.File(outPath, s"SortedJEFY${year}_FXR.csv").getPath,
          year,
          params.getOrElse("sortSpec", "../sort_specs/SortedJE.sortspec")
        )
      // CCP3 step — read SortedJE output, produce four bounded aggregation view tables
      case "5v" => CKBViews(params + ("inPath" -> outPath, "outPath" -> outPath, "year" -> year))

      // CCP1 baseline steps — read SortedJE output, produce GL balances and permutation table
      case "20" => GLPost(params + ("inPath" -> inPath, "outPath" -> outPath, "year" -> year))
      case "21" => PermutationGenerator(params + ("inPath" -> inPath, "outPath" -> outPath, "year" -> year))
      case _    => println(s"Step '$step' is not implemented.")
    }
  }

  // ── Step 2m: k-way merge of payment + PO sorted SJE files ────────────────
  //
  //  Pure sort-partition coordination: no financial logic.
  //  Calls SortEngine.mergeSpills on all SortedJE input files to produce a single
  //  merged sorted file for post.scala.  Memory contract: O(k open file handles).
  //
  //  Input discovery (when --payment is not explicitly supplied):
  //    Monthly mode:   SortedJEFY{yr}_{YYYY}_{MM}_exp.csv (one per source month)
  //    Quarterly mode: SortedJEFY{yr}q{n}exp.csv (one per quarter)
  //    PO file:        SortedJEFY{yr}q{n}PO.csv (first match)
  //  If --payment is supplied, that single file is used (legacy/explicit override).
  //
  //  Params:
  //    --payment  explicit path to a single payment SortedJE file (optional — disables auto-discover)
  //    --po       path to PO SortedJE file (default: auto-discover SortedJEFY{yr}*PO.csv)
  //    --outPath  output directory
  //    --year     4-digit year (used for default file name derivation)
  private def runMerge(params: Map[String, String]): Unit = {
    val t0      = System.currentTimeMillis()
    val outPath = params.getOrElse("outPath", DefaultOutPath)
    val yearStr = params.getOrElse("year", DefaultYear)
    val shortYr = yearStr.takeRight(2)
    val outDir  = new java.io.File(outPath.stripSuffix("/"))
    val mergedFile = outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + "merged.csv"

    // Discover all payment SortedJE files from outPath (monthly or quarterly naming).
    // If --payment is explicitly supplied, use that single file instead.
    val paymentFiles: Seq[String] = params.get("payment") match {
      case Some(explicit) => Seq(explicit)
      case None =>
        val allSortedJE = Option(outDir.listFiles()).getOrElse(Array.empty[java.io.File])
          .filter(_.isFile)
          .map(_.getName)
          .filter { n =>
            n.startsWith("SortedJEFY" + shortYr) &&
            n.endsWith(".csv") &&
            !n.contains("PO") &&
            !n.contains("merged") &&
            !n.contains("_FXR") &&
            !n.contains("_ALLOC")
          }
          .sorted   // deterministic order
          .map(n => outPath.stripSuffix("/") + "/" + n)
          .toSeq
        if (allSortedJE.isEmpty)
          Seq(outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + "q1exp.csv")
        else allSortedJE
    }

    // PO file: explicit --po, or first SortedJEFY{yr}*PO.csv found.
    val poFile: String = params.getOrElse("po", {
      Option(outDir.listFiles()).getOrElse(Array.empty[java.io.File])
        .filter(_.isFile)
        .map(_.getName)
        .filter(n => n.startsWith("SortedJEFY" + shortYr) && n.endsWith("PO.csv"))
        .sorted
        .headOption
        .map(n => outPath.stripSuffix("/") + "/" + n)
        .getOrElse(outPath.stripSuffix("/") + "/SortedJEFY" + shortYr + "q1PO.csv")
    })

    println("*" * 100)
    println("                     Step 2m — Merge Payment + PO SortedJE streams")
    println("Start Time: " + java.util.Calendar.getInstance.getTime)
    println("*" * 100)
    println(s"  Payment files: ${paymentFiles.size} file(s)")
    paymentFiles.foreach(f => println(s"    $f"))
    println(s"  PO file      : $poFile")
    println(s"  Merged output: $mergedFile")

    val specParam = params.getOrElse("sortspec", "")
    val specPath  = {
      val candidates = if (specParam.nonEmpty) Seq(specParam) else Seq.empty
      val defaults   = Seq("../sort_specs/SortedJE.sortspec", "../../sort_specs/SortedJE.sortspec")
      (candidates ++ defaults).find(p => new java.io.File(p).exists())
        .getOrElse(defaults.head)
    }
    val sortSpec = SortEngine.loadSpec(specPath)
    println(s"  SortSpec     : $specPath  (${sortSpec.size} fields)")

    (paymentFiles :+ poFile).foreach { f =>
      if (!new java.io.File(f).exists()) {
        System.err.println(s"ERROR: Merge input not found: $f")
        sys.exit(1)
      }
    }

    val rowsWritten = SortEngine.mergeSpills(paymentFiles :+ poFile, mergedFile, sortSpec)

    val elapsedS  = ((System.currentTimeMillis() - t0) / 1000).max(1)
    val outBytes  = new java.io.File(mergedFile).length()
    val peakRss   = Runtime.getRuntime.totalMemory()

    println(f"\n  Merged rows written: $rowsWritten%,d")
    println(s"\nRESULT step=2m year=$yearStr rc=0 records_read=0 records_written=$rowsWritten elapsed_s=$elapsedS input_bytes=0 output_bytes=$outBytes")
    println(s"RESOURCE step=2m year=$yearStr jvm_heap_bytes=$peakRss")
    println("STATUS: COMPLETE")
    println("*" * 100)
    println(s"Process End Time: ${java.util.Calendar.getInstance.getTime}")
    println("*" * 100)
  }

  // ── usage help ────────────────────────────────────────────────────────────
  def printUsage(): Unit = {
    println("")
    println("USAGE (CLI mode):")
    println("  sbt \"run --step <N> [--inPath <path>] [--outPath <path>] [--vendormaster <path>] [--year <YYYY>] [--quarter <Q>] [--type <E|R>]\"")
    println("")
    println("  --step         N     Required.")
    println("                         2   = standardizeAndSort (payment files → SortedJEFY*.csv)")
    println("                         2p  = PO standardize+sort (VA_opendata_FY20YY.txt → SortedJEFY*PO.csv)")
    println("                         2m  = merge payment + PO SortedJE → SortedJEFY*merged.csv")
    println("                         3   = post, 4=contraCreation, 5=dataAggregation,")
    println("                         5v  = CKBViews (CCP3 extract-time view aggregation)")
    println("                         6   = financialAllocation, 7=consolidation, 8=forecastingBudgeting,")
    println("                         9   = arrangementReclass, 12=currencyRevaluation,")
    println("                         20  = GLPost (CCP1 GL baseline), 21=PermutationGenerator (CCP1)")
    println(s"  --inPath       path  Optional. Raw file directory.  Default: $DefaultInPath")
    println(s"  --outPath      path  Optional. Output directory.    Default: $DefaultOutPath")
    println( "  --vendormaster path  Optional. VendorMaster CSV.    Default: data/VendorMaster_full.csv")
    println(s"  --year         YYYY  Optional. 4-digit fiscal year. Default: 20$DefaultYear")
    println(s"  --quarter      Q     Optional. 1-4=expense, 5=revenue. Default: $DefaultQuarter")
    println( "  --type         E|R   Optional. E=Expense, R=Revenue.  Default: E")
    println( "  --payment      path  Optional (step 2m). Payment SortedJE file.")
    println( "  --po           path  Optional (step 2m). PO SortedJE file.")
    println("")
    println("USAGE (interactive mode):")
    println("  sbt run         (no arguments)")
    println("")
  }
}
