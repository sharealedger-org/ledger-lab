package org.universalledger.foundation.va.ledger

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

import org.apache.commons.csv.CSVFormat
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.JavaConverters._

class FinancialTupleExplorerSpec extends AnyFunSuite {
  test("the interactive explorer switches measures, handles ranges, and hides the numeric target") {
    val csv = Files.createTempFile("q2-relationships", ".csv")
    val exportDirectory = Files.createTempDirectory("q2-export")
    val examples = exportDirectory.resolve("paired examples.csv")
    val report =
      """agency,fiscal_year,vendor_id,event,currency,fund,commodity,contract_id,amount,rows,absolute_amount,contract_status
        |"Agency, East",2044,001,COMMITMENT,USD,0000,45000,0,0.00,16,0.00,unattributed
        |Agency West,2044,001,COMMITMENT,USD,0000,45000,0,123.45,5,123.45,unattributed
        |Agency North,2044,001,EXPENSE_PAYMENT,USD,0000,45000,0,-21.50,2,21.50,unattributed
        |Other vendor,2044,1,COMMITMENT,USD,0000,45000,0,9000,20,9000,unattributed
        |""".stripMargin
    val commands =
      s"""where event COMMITMENT
        |rank amount
        |mask 1 amount
        |rank rows
        |range amount 0 123.45
        |range amount 200 300
        |export $examples
        |export $examples
        |clear event
        |range amount -30 0
        |quit
        |""".stripMargin
    val bytes = new ByteArrayOutputStream()
    val output = new PrintStream(bytes, true, UTF_8.name())
    try {
      Files.write(csv, report.getBytes(UTF_8))
      Console.withIn(new ByteArrayInputStream(commands.getBytes(UTF_8))) {
        Console.withOut(output) {
          FinancialTupleExplorer.main(Array(csv.toString, "001", "2044"))
        }
      }
      val lines = new String(bytes.toByteArray, UTF_8).split("\\R").toVector
      val resultLines = lines.filter(_.startsWith("1,"))
      assert(resultLines.size == 4)
      assert(resultLines(0).startsWith("1,Agency West,"))
      assert(resultLines(1).startsWith("1,\"Agency, East\","))
      assert(resultLines(2).endsWith(",0.00,16,0.00"))
      assert(resultLines(3).endsWith(",-21.50,2,21.50"))
      assert(lines.contains("No report cells match this scope."))
      val masked = lines.dropWhile(!_.startsWith("In fiscal year")).take(4)
      assert(masked.size == 4)
      assert(masked.forall(!_.contains("123.45")))
      assert(masked.exists(_.contains("amount=<MASK>")))
      assert(masked.last == "Numeric payload in marker order: 5")
      assert(!lines.exists(_.contains("Other vendor")))
      assert(lines.count(_.startsWith("Exported ")) == 1)
      val parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
        .parse(Files.newBufferedReader(examples, UTF_8))
      try {
        val cases = parser.iterator().asScala.toVector
        assert(cases.size == 6)
        assert(cases.groupBy(_.get("cell_id")).values.forall(_.size == 3))
        val observedZero = cases.find(r => r.get("target") == "amount" && r.get("reference_answer") == "0.00").get
        assert(observedZero.get("numeric_payload") == "16")
        assert(observedZero.get("typed_tuple").contains("amount=<MASK>"))
        assert(observedZero.get("typed_tuple").contains("agency=Agency, East"))
      } finally parser.close()
    } finally {
      output.close()
      Files.deleteIfExists(csv)
      Files.deleteIfExists(examples)
      Files.deleteIfExists(exportDirectory)
    }
  }
}
