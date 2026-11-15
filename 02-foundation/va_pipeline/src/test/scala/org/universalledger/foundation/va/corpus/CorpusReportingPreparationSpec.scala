package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.ReportingSupport
import scala.collection.mutable

class CorpusReportingPreparationSpec extends AnyFunSuite {
  test("literal ID distinct merge survives multiple levels, duplicate blocks and reordered inputs") {
    val ids = Vector("c1246", "c12464", "000000000", "0000000000", "9", "10", "UNMATCHED_X") ++
      (0 until 100).map(n => s"SIM_$n")
    def sorted(rows: Seq[String], capacity: Int, fan: Int): Vector[String] = {
      val work = new File(Files.createTempDirectory("assignment-sort-").toFile, "work")
      val sorter = new CorpusReportingPreparation.IdSort(work, capacity, fan)
      rows.foreach(sorter.add)
      val out = mutable.ArrayBuffer[String](); sorter.finish(out += _)
      assert(sorter.peak <= capacity)
      if (capacity == 2) assert(sorter.levels >= 3)
      out.toVector
    }
    val expected = ids.distinct.sorted
    assert(sorted(ids ++ ids.reverse, 2, 2) == expected)
    assert(sorted((ids ++ ids).reverse, 7, 3) == expected)
    assert(expected.filter(ReportingSupport.attributed).contains("c12464"))
    assert(!expected.filter(ReportingSupport.attributed).exists(_.forall(_ == '0')))
  }
  test("invalid ID and unbounded merge fan-in fail before publication") {
    def dir = new File(Files.createTempDirectory("assignment-bound-").toFile, "work")
    intercept[IllegalArgumentException](new CorpusReportingPreparation.IdSort(dir, 2, 17))
    val sorter = new CorpusReportingPreparation.IdSort(dir, 2, 2)
    intercept[IllegalArgumentException](sorter.add("broken,id"))
    intercept[IllegalArgumentException](sorter.add("x" * 257))
  }
}
