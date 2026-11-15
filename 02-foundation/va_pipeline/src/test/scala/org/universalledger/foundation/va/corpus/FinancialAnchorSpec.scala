package org.universalledger.foundation.va.corpus

import java.io.File
import org.scalatest.funsuite.AnyFunSuite

/** Offline checks of prepared summary inputs; not an APA corpus tie-out or posting engine. */
class FinancialAnchorSpec extends AnyFunSuite {
  private val dir=new File("../../data/acfr_anchors")
  private def rows(name: String): Vector[Map[String,String]] = {
    val out=scala.collection.mutable.ArrayBuffer[Map[String,String]]()
    ConnectedViewsCheck.scan(new File(dir,name)) { r=>require(out.size<64); out+=r }
    out.toVector
  }
  private val anchors=rows("FY2012_general_fund_verified_anchors_v1.csv")
  private val bridge=rows("FY2012_general_fund_verified_bridge_v1.csv")
  private def scope(rs: Vector[Map[String,String]]): Unit = rs.foreach { r=>
    require(r("fiscal_year")=="2012" && r("fund")=="GENERAL_FUND" && r("unit")=="THOUSAND" && r("currency")=="USD")
    require(r("status")=="VERIFIED_TABLE" && r("source_sha256").matches("[0-9a-f]{64}"))
  }
  private def value(basis: String, measure: String): BigDecimal = {
    val selected=anchors.filter(r=>r("basis")==basis && r("measure")==measure); require(selected.size==1)
    BigDecimal(selected.head("amount"))
  }
  private def closing(rs: Vector[Map[String,String]]): BigDecimal = {
    scope(rs)
    require(rs.size==6 && rs.map(_("bridge_id")).distinct.size==6)
    rs.foreach(r=>require(r("from_basis")=="BUDGET" && r("to_basis")=="MODIFIED_ACCRUAL" && r("measure")=="closing_fund_balance"))
    value("BUDGET","closing_fund_balance")+rs.map(r=>BigDecimal(r("signed_amount"))).sum
  }
  test("published General Fund flow identities and six named closing adjustments reconcile offline") {
    scope(anchors); assert(anchors.size==12)
    Vector("BUDGET","MODIFIED_ACCRUAL").foreach { basis=>
      val change=value(basis,"total_revenues")-value(basis,"total_expenditures")+value(basis,"other_financing_net")
      assert(change==value(basis,"net_change_fund_balance"))
      assert(value(basis,"opening_fund_balance_restated")+change==value(basis,"closing_fund_balance"))
    }
    assert(closing(bridge)==value("MODIFIED_ACCRUAL","closing_fund_balance"))
    assert(bridge.map(r=>BigDecimal(r("signed_amount"))).sum==BigDecimal(-1171053))
    val fraction=BigDecimal("0.01"); val toUsd=BigDecimal(1000)
    assert(value("BUDGET","total_revenues")*toUsd*fraction==BigDecimal(171019590))
    assert(value("BUDGET","total_expenditures")*toUsd*fraction==BigDecimal(168940140))
    assert(closing(bridge)*toUsd*fraction==BigDecimal(5123590))
  }
  test("fund-scope, measure and sign mistakes do not pass the prepared bridge controls") {
    intercept[IllegalArgumentException](closing(bridge.updated(0,bridge.head.updated("fund","FEDERAL_TRUST_FUND"))))
    intercept[IllegalArgumentException](closing(bridge.updated(0,bridge.head.updated("measure","total_revenues"))))
    val wrongSign=bridge.updated(1,bridge(1).updated("signed_amount","390947"))
    assert(closing(wrongSign)!=value("MODIFIED_ACCRUAL","closing_fund_balance"))
    assert(!bridge.exists(_("category").contains("food")))
  }

  test("prepared scale ladder counts and calibration configuration are internally consistent") {
    val input=new File("../../data/synthetic/component_anchor_2012_v1")
    val config=scala.collection.mutable.HashMap[String,String]()
    ConnectedViewsCheck.scan(new File(input,"test_config.csv")) { r=>
      require(config.size<64 && !config.contains(r("key"))); config(r("key"))=r("value")
    }
    assert(config("state")=="prepared_not_executed" && config("accounting_basis")=="BUDGET")
    assert(config("population_policy")=="synthetic_only_no_addition_to_observed_totals")
    val fraction=BigDecimal(config("synthetic_fraction")); assert(fraction==BigDecimal("0.01"))
    assert(value(config("accounting_basis"),"total_revenues")*1000*fraction==BigDecimal(171019590))
    var cases=0
    ConnectedViewsCheck.scan(new File(input,"scale_cases.csv")) { r=>
      cases+=1; require(cases<=3)
      val components=BigInt(r("components"))
      val financial=Seq("expense_events_per_component","revenue_events_per_component","po_events_per_component").map(k=>BigInt(r(k))).sum
      assert(components*(financial+BigInt(r("employee_events_per_component")))==BigInt(r("total_events")))
      assert(components*financial*2==BigInt(r("financial_sje_rows")))
    }
    assert(cases==3 && config("max_live_sort_readers")=="16")
  }
}
