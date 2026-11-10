package org.universalledger.foundation.va.ledger

import java.io.File

//***************************************************************************************************************
//
//  Object:  SortedJEInputs — one-read rule for SortedJE partitions
//
//  Step 2m merges a year's payment SortedJE files and its PO SortedJE file into SortedJEFY{yy}merged.csv.
//  The merged file holds exactly the rows of its inputs. A step that selects every file starting with
//  SortedJEFY{yy} therefore reads each row twice once the merged file exists (steps 3, 20, 21 and 5v did).
//
//  Rule: for a year whose merged file is among the candidates, read the merged file and the derived files
//  (_FXR, _ALLOC, _CONTRA, _RECLASS — not 2m inputs) and skip the 2m inputs (payment/revenue/PO files).
//  A year with no merged file is unchanged: its source files are read as before.
//
//  The 2m-input predicate matches LedgerApp.runMerge: every SortedJEFY{yy}*.csv that is not the merged file
//  and not a derived file.
//***************************************************************************************************************

object SortedJEInputs {

  private val DerivedMarkers = Seq("_FXR", "_ALLOC", "_CONTRA", "_RECLASS")

  /** Two-digit year in "SortedJEFY{yy}...", or "" when the name does not follow the pattern. */
  def yearOf(name: String): String =
    if (name.startsWith("SortedJEFY") && name.length >= 12) name.substring(10, 12) else ""

  def isMerged(name: String): Boolean = name.startsWith("SortedJEFY") && name.endsWith("merged.csv")

  /** True for the files step 2m reads: SortedJEFY{yy}*.csv that is neither merged nor derived. */
  def isMergeInput(name: String): Boolean =
    name.startsWith("SortedJEFY") && name.endsWith(".csv") && !isMerged(name) && !DerivedMarkers.exists(name.contains)

  /** Drop each year's 2m inputs when that year's merged file is present; keep everything else in order. */
  def oneRead(files: Seq[File]): Seq[File] = {
    val mergedYears = files.map(_.getName).filter(isMerged).map(yearOf).toSet
    files.filterNot(f => isMergeInput(f.getName) && mergedYears.contains(yearOf(f.getName)))
  }
}
