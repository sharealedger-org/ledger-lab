package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter}
import java.util.Calendar
import scala.io.Source

//***************************************************************************************************************
//
//  Program:  sortJE — External Sort Step (between transStandardize and post)
//
//  GenevaERS architecture: the sort utility is a separate step between standardize and post.
//  It reads all JE{year}.csv files in the output directory, sorts each on the 14-field
//  composite posting key, and writes SortedJE{year}.csv — the pre-sorted input that
//  post.scala reads in a single sequential O(M) pass.
//
//  Sort key (14 fields, cols 9–22 of the JE CSV, concatenated for lexicographic order):
//    col 9:  transLedgerID          col 16: transNominalAccountID
//    col 10: transJrnlType          col 17: transAltAccountID
//    col 11: transBookCodeID        col 18: transCurrencyCodeSourceID
//    col 12: transLegalEntityID     col 19: transCurrencyTypeCodeSourceID
//    col 13: transCenterID          col 20: transCurrencyCodeTargetID
//    col 14: transProjectID         col 21: transCurrencyTypeCodeTargetID
//    col 15: transProductID         col 22: transFiscalPeriod
//
//  This is intentionally a simple in-process sort (not a shell-out) so it runs on any OS.
//  For production volumes (>100M records), replace with OS sort or an external merge sort.
//
//  *(c) Copyright IBM Corporation. 2018
//  * SPDX-License-Identifier: Apache-2.0
//  * By Kip Twitchell
//
//***************************************************************************************************************

object sortJE {

  // Column indices of the 14-field composite sort key within a JE CSV row
  private val KEY_COLS = Array(9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)

  def compositeKey(cols: Array[String]): String = {
    val sb = new StringBuilder
    for (i <- KEY_COLS) {
      if (i < cols.length) sb.append(cols(i).trim)
    }
    sb.toString
  }

  def apply(fileInLocation: String, fileOutLocation: String): Unit = {

    println("*" * 100)
    println("                                Sort Step (JE → SortedJE)")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)

    val outDir = new File(fileOutLocation)
    // Match JE*.csv but exclude JEHeader.csv (the schema reference file written by transStandardize)
    val jeFiles = outDir.listFiles().filter(f =>
      f.getName.startsWith("JE") && f.getName.endsWith(".csv") && f.getName != "JEHeader.csv"
    )

    if (jeFiles == null || jeFiles.isEmpty) {
      println("  No JE*.csv files found in " + fileOutLocation + " — nothing to sort.")
      return
    }

    var totalFilesProcessed = 0
    var totalRowsRead = 0
    var totalRowsWritten = 0

    for (jeFile <- jeFiles.sorted) {
      val lines = Source.fromFile(jeFile).getLines().toArray
      if (lines.isEmpty) {
        println("  " + jeFile.getName + ": empty — skipped")
      } else {
        // JE files have no header row — all lines are data records
        val dataLines = lines.filter(_.trim.nonEmpty)

        // Sort on composite key (lexicographic — matches CKB sorted-merge contract)
        val sorted = dataLines.sortBy(line => compositeKey(line.split(",")))

        val outName = jeFile.getName.replace("JE", "SortedJE")
        val outFile = new File(fileOutLocation + outName)
        val pw = new PrintWriter(outFile)
        sorted.foreach(pw.println)
        pw.close()

        totalRowsRead += dataLines.length
        totalRowsWritten += sorted.length
        totalFilesProcessed += 1
        println("  " + jeFile.getName + " → " + outName + "  (" + dataLines.length + " rows)")
      }
    }

    println("*" * 100)
    println(f"Files Processed:   $totalFilesProcessed")
    println(f"Rows Read:         $totalRowsRead")
    println(f"Rows Written:      $totalRowsWritten")
    println("Process End Time:  " + Calendar.getInstance.getTime)
    println("*" * 100)
  }
}
