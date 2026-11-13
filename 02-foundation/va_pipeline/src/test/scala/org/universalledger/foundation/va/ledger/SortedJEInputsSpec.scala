package org.universalledger.foundation.va.ledger

import java.io.File

import org.scalatest.funsuite.AnyFunSuite

class SortedJEInputsSpec extends AnyFunSuite {

  private def files(names: String*): Seq[File] = names.map(new File(_))
  private def names(fs: Seq[File]): Seq[String] = fs.map(_.getName)

  test("year with a merged file: 2m inputs (monthly, quarterly, PO) are skipped; merged and derived files are kept") {
    val in = files(
      "SortedJEFY03_0003_07_exp.csv", "SortedJEFY03_0003_08_exp.csv", "SortedJEFY03q1PO.csv", "SortedJEFY03_rev.csv",
      "SortedJEFY03merged.csv", "SortedJEFY03_FXR.csv", "SortedJEFY03_ALLOC.csv")
    assert(names(SortedJEInputs.oneRead(in)) ==
      Seq("SortedJEFY03merged.csv", "SortedJEFY03_FXR.csv", "SortedJEFY03_ALLOC.csv"))
  }

  test("year without a merged file is unchanged (source files are read as before)") {
    val in = files("SortedJEFY04q1exp.csv", "SortedJEFY04q1PO.csv")
    assert(names(SortedJEInputs.oneRead(in)) == names(in))
  }

  test("merged file of one year does not hide another year's source files") {
    val in = files("SortedJEFY03merged.csv", "SortedJEFY03q1exp.csv", "SortedJEFY04q1exp.csv")
    assert(names(SortedJEInputs.oneRead(in)) == Seq("SortedJEFY03merged.csv", "SortedJEFY04q1exp.csv"))
  }
}
