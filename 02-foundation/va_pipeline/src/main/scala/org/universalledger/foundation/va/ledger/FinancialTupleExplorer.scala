package org.universalledger.foundation.va.ledger

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths}

import org.apache.commons.csv.CSVFormat

import scala.collection.JavaConverters._
import scala.io.StdIn
import scala.util.control.NonFatal

/** Read-only development access to accepted relationship cells. */
object FinancialTupleExplorer {
  private val dimensions = Vector("agency", "event", "currency", "fund", "commodity", "contract_id", "contract_status")
  private val measures = Vector("amount", "rows", "absolute_amount")
  private val limit = 20000
  private val pageSize = 20

  private val help =
    """show                         Show matching report cells
      |values FIELD                 List next values in a dimension
      |where FIELD VALUE            Select a dimension value (spaces allowed)
      |clear FIELD                  Remove a dimension selection
      |rank MEASURE                 Rank matching cells by a measure
      |range MEASURE LOW HIGH       Show cells in a half-open interval
      |example ROW                  Render a displayed row as prose and tuples
      |mask ROW FIELD               Hide agency, amount, or rows in a rendering
      |export PATH                  Write paired masked examples for this scope
      |reset                        Clear selections
      |help                         Show commands
      |quit                         End the session
      |Dimensions: agency, event, currency, fund, commodity, contract_id, contract_status
      |Measures: amount, rows, absolute_amount""".stripMargin

  private case class Cell(fields: Map[String, String], amount: BigDecimal, rows: Long, gross: BigDecimal) {
    def value(measure: String): BigDecimal = measure match {
      case "amount" => amount
      case "rows" => BigDecimal(rows)
      case "absolute_amount" => gross
      case _ => throw new IllegalArgumentException(s"Unknown measure: $measure")
    }
  }

  private val targets = Vector("agency", "amount", "rows")

  private case class Rendering(prose: String, tuple: String, typedTuple: String, payload: Vector[String])

  private def load(path: Path, vendor: String, year: String): Vector[Cell] = {
    val reader = Files.newBufferedReader(path, UTF_8)
    try {
      val parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)
      try {
        val required = Vector("vendor_id", "fiscal_year") ++ dimensions ++ measures
        require(required.forall(parser.getHeaderMap.containsKey), "Expected a relationships.csv header")
        val vendorColumn = parser.getHeaderMap.get("vendor_id").intValue()
        val yearColumn = parser.getHeaderMap.get("fiscal_year").intValue()
        val matches = parser.iterator().asScala
          .filter(r => r.get(vendorColumn) == vendor && r.get(yearColumn) == year)
          .take(limit + 1)
          .map { r =>
            Cell(dimensions.map(d => d -> r.get(d)).toMap,
              BigDecimal(r.get("amount")), r.get("rows").toLong, BigDecimal(r.get("absolute_amount")))
          }.toVector
        require(matches.size <= limit, s"Vendor/year exceeds the $limit cell session limit")
        matches
      } finally parser.close()
    } finally reader.close()
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      Console.err.println("Usage: FinancialTupleExplorer RELATIONSHIPS_CSV VENDOR_ID FISCAL_YEAR")
      sys.exit(2)
    }

    val Array(path, vendor, year) = args
    try {
      require(year.matches("[0-9]{4}"), "Use a four-digit fiscal year")
      val cells = load(Paths.get(path), vendor, year)
      val attribution = if (ReportingSupport.attributed(vendor)) "attributed" else "unattributed"
      println(s"Vendor $vendor | FY$year | $attribution | ${cells.size} report cells")
      println("One result row is a direct movement report cell. Enter help for commands.")
      interact(cells, vendor, year, attribution)
    } catch {
      case NonFatal(error) =>
        Console.err.println(error.getMessage)
        sys.exit(1)
    }
  }

  private def interact(cells: Vector[Cell], vendor: String, year: String, attribution: String): Unit = {
    var selections = Map.empty[String, String]
    var displayed = Vector.empty[Cell]

    def matching: Vector[Cell] = cells.filter(cell => selections.forall { case (field, value) => cell.fields(field) == value })

    def show(results: Vector[Cell]): Unit = {
      displayed = results.take(pageSize)
      if (results.isEmpty) println("No report cells match this scope.")
      else {
        println(s"Showing ${displayed.size} of ${results.size} matching cells")
        println(ReportingSupport.csv(Vector("row") ++ dimensions ++ measures))
        displayed.zipWithIndex.foreach { case (cell, index) =>
          println(ReportingSupport.csv(Vector(index + 1) ++ dimensions.map(cell.fields) ++
            measures.map(m => cell.value(m).bigDecimal.toPlainString)))
        }
      }
    }

    def render(cell: Cell, target: Option[String]): Rendering = {
      val context = Vector("vendor_id" -> vendor, "fiscal_year" -> year, "attribution" -> attribution) ++
        dimensions.map(d => d -> cell.fields(d))
      // Amount and gross can be equal. Exclude gross from the paired pilot inputs.
      val exampleMeasures = Vector("amount", "rows")
      val values = exampleMeasures.map(m => m -> cell.value(m).bigDecimal.toPlainString)
      def hide(pair: (String, String)): (String, String) =
        if (target.contains(pair._1)) pair._1 -> "<MASK>" else pair
      val f = (context ++ values).map(hide).toMap
      val prose = s"""In fiscal year $year, vendor key $vendor ($attribution) had a signed amount of ${f("amount")} """ +
        s"""${f("currency")} across ${f("rows")} direct movement rows. """ +
        s"""Event "${f("event")}", agency "${f("agency")}", fund "${f("fund")}", """ +
        s"""commodity "${f("commodity")}", contract "${f("contract_id")}" (${f("contract_status")})."""
      def fields(pairs: Vector[(String, String)]): Vector[String] = pairs.map { case (k, v) => s"$k=$v" }
      val markers = values.map { case (k, _) => k -> (if (target.contains(k)) "<MASK>" else "<NUM>") }
      Rendering(prose, ReportingSupport.csv(fields((context ++ values).map(hide))),
        ReportingSupport.csv(fields(context.map(hide) ++ markers)),
        values.filterNot(p => target.contains(p._1)).map(_._2))
    }

    def example(row: Int, target: Option[String]): Unit = {
      require(row > 0 && row <= displayed.size, "Choose a row from the last displayed results")
      val rendered = render(displayed(row - 1), target)
      println(rendered.prose)
      println("Tuple (numbers as text): " + rendered.tuple)
      println("Tuple (numeric markers): " + rendered.typedTuple)
      println("Numeric payload in marker order: " + rendered.payload.mkString(", "))
    }

    def export(path: Path): Unit = {
      import java.nio.file.StandardOpenOption.{CREATE_NEW, WRITE}
      val results = matching
      require(results.nonEmpty, "Select a scope containing report cells before exporting")
      val writer = Files.newBufferedWriter(path, UTF_8, CREATE_NEW, WRITE)
      def write(fields: Seq[String]): Unit = {
        writer.write(ReportingSupport.csv(fields))
        writer.newLine()
      }
      try {
        write(Vector("cell_id", "target", "reference_answer", "prose", "tuple", "typed_tuple", "numeric_payload"))
        results.foreach { cell =>
          val id = ReportingSupport.key(Vector(vendor, year) ++ dimensions.map(cell.fields))
          targets.foreach { target =>
            val rendered = render(cell, Some(target))
            val answer = cell.fields.getOrElse(target, cell.value(target).bigDecimal.toPlainString)
            write(Vector(id, target, answer, rendered.prose, rendered.tuple, rendered.typedTuple, rendered.payload.mkString(",")))
          }
        }
      } finally writer.close()
      println(s"Exported ${results.size * targets.size} paired development examples to $path")
    }

    Iterator.continually {
      if (System.console() != null) {
        print("q2> ")
        Console.flush()
      }
      StdIn.readLine()
    }.takeWhile(line => line != null && line.trim != "quit").foreach { line =>
      try {
        val input = line.trim
        val command = line.trim.split("\\s+", 3)
        command match {
          case Array("") =>
          case Array("help") => println(help)
          case Array("show") => show(matching)
          case Array("reset") => selections = Map.empty; displayed = Vector.empty
          case Array("where", field, value) if dimensions.contains(field) =>
            selections += field -> value
            displayed = Vector.empty
          case Array("clear", field) if dimensions.contains(field) =>
            selections -= field
            displayed = Vector.empty
          case Array("values", field) if dimensions.contains(field) =>
            val candidates = matching.map(_.fields(field)).distinct.sorted
            println(s"Showing ${math.min(pageSize, candidates.size)} of ${candidates.size} distinct values")
            candidates.take(pageSize).foreach(println)
          case Array("rank", measure) if measures.contains(measure) =>
            show(matching.sortBy(cell => -cell.value(measure)))
          case Array("range", measure, bounds) if measures.contains(measure) =>
            val limits = bounds.split("\\s+")
            require(limits.length == 2, "Use range MEASURE LOW HIGH")
            val lower = BigDecimal(limits(0))
            val upper = BigDecimal(limits(1))
            require(lower < upper, "The lower bound must be smaller than the upper bound")
            show(matching.filter { cell =>
              val value = cell.value(measure)
              lower <= value && value < upper
            })
          case Array("example", index) => example(index.toInt, None)
          case Array("mask", index, field) if targets.contains(field) =>
            example(index.toInt, Some(field))
          case Array("export", _*) if input.startsWith("export ") =>
            export(Paths.get(input.stripPrefix("export ").trim))
          case _ => println("Unknown command or field. Enter help for commands.")
        }
      } catch {
        case NonFatal(error) => println(s"Query error: ${error.getMessage}")
      }
    }
  }
}
