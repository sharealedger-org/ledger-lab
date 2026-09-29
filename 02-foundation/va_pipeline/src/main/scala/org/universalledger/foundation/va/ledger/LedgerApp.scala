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
//        --step     N    Step to run: 1=poToPayMatch, 2=transStandardize, 3=post,
//                        10=initFiles, 11=DPBSpark
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

  def main(args: Array[String]): Unit = {

    println("*" * 100)
    println("                                Universal Ledger")
    println("                                   Demo System")
    println("                  Using State of Virginia Public Financial Data")
    println("*" * 100)

    // ── choose mode ──────────────────────────────────────────────────────────
    if (args.nonEmpty) {
      runCLI(args)
    } else {
      runInteractive()
    }

    println("*" * 100)
    println("                           End of Financial Patterns Processes")
    println("*" * 100)
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
    val inputPOPath = inPath + "poData/"

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
      // Step 2: standardize raw VA files + external merge-sort → SortedJE (Scala, Session 21)
      // Replaces transStandardize (lost vendor identity) + sortJE (in-memory, not scale-invariant)
      case "2"  => standardizeAndSort(params + ("inPath" -> inPath, "outPath" -> outPath, "year" -> year))
      case "3"  => post(outPath, outPath, year)   // input and output are the same location for posting
      case "4"  => contraCreation(outPath)       // must run LAST — after all balance-updating processes
      case "5"  => dataAggregation(outPath, inPath + "ViewSpec.csv")
                                                 // LDGR in outPath; ViewSpec + VendorMaster in inPath
      case "6"  => financialAllocation(outPath, params.getOrElse("allocationRules", inPath))  // allocates overhead to receivers; posts allocation SJEs
      case "7"  => consolidation(outPath)        // consolidates agencies; eliminates interagency transfers
      case "8"  => forecastingBudgeting(outPath) // projects actuals to budget; produces variance report
      case "9"  => arrangementReclass(outPath)   // reads/writes VendorMaster, VendorUpdate, LDGR files
      // Steps 1, 10, 11 (poToPayMatch, initFiles, DPBSpark) require Spark and are excluded from
      // the default build. To enable: add Spark dependency to build.sbt and move sources back to
      // src/main/scala/org/universalledger/foundation/va/ledger/ from
      // src/main/scala/org/universalledger/foundation/va/spark/
      case _    => println(s"Step '$step' is not yet implemented or requires Spark (steps 1, 10, 11).")
    }
  }

  // ── usage help ────────────────────────────────────────────────────────────
  def printUsage(): Unit = {
    println("")
    println("USAGE (CLI mode):")
    println("  sbt \"run --step <N> [--inPath <path>] [--outPath <path>] [--vendormaster <path>] [--year <YYYY>] [--quarter <Q>] [--type <E|R>]\"")
    println("")
    println("  --step         N     Required. 2=standardizeAndSort, 3=post, 4=contraCreation, 5=dataAggregation,")
    println("                                 6=financialAllocation, 7=consolidation, 8=forecastingBudgeting, 9=arrangementReclass")
    println(s"  --inPath       path  Optional. Raw file directory.  Default: $DefaultInPath")
    println(s"  --outPath      path  Optional. Output directory.    Default: $DefaultOutPath")
    println( "  --vendormaster path  Optional. VendorMaster CSV.    Default: data/VendorMaster_full.csv")
    println(s"  --year         YYYY  Optional. 4-digit fiscal year. Default: 20$DefaultYear")
    println(s"  --quarter      Q     Optional. 1-4=expense, 5=revenue. Default: $DefaultQuarter")
    println( "  --type         E|R   Optional. E=Expense, R=Revenue.  Default: E")
    println("")
    println("USAGE (interactive mode):")
    println("  sbt run         (no arguments)")
    println("")
  }
}
