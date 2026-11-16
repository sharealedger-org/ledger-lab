package org.universalledger.foundation.va.corpus

import java.io.{File,PrintWriter}
import java.time.{Instant,LocalDate,ZoneOffset}
import org.universalledger.foundation.va.ledger.ReportingSupport

/** Explicit display axis; ISO backing dates are adapter encoding, not future forecasts. */
object SyntheticCalendar {
  val FirstFiscalYear=2031
  val Version="synthetic-calendar-v1-july-june"
  def year(fiscalYear:Int):String={val index=fiscalYear-FirstFiscalYear+1;require(index>=0 && index<=99);f"SY$index%02d"}
  def fiscalYear(day:LocalDate):Int=day.getYear+(if(day.getMonthValue>=7)1 else 0)
  def label(day:LocalDate):String=year(fiscalYear(day))+f"-${day.getMonthValue}%02d-${day.getDayOfMonth}%02d"
  def parseDate(value:String):LocalDate={
    if(value.matches("SY[0-9]{2}-[0-9]{2}-[0-9]{2}")){
      val fy=FirstFiscalYear+value.substring(2,4).toInt-1;val month=value.substring(5,7).toInt
      LocalDate.of(fy-(if(month>=7)1 else 0),month,value.substring(8,10).toInt)
    }else LocalDate.parse(value)
  }
  def parseInstant(value:String):Instant={
    if(value.startsWith("SY"))Instant.parse(parseDate(value.take(10)).toString+value.drop(10))
    else Instant.parse(value)
  }
  def value(x:Any):Any=x match {
    case d:LocalDate=>label(d)
    case s:String if s.matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}")=>label(LocalDate.parse(s))
    case s:String if s.matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}T.*Z")=>label(LocalDate.parse(s.take(10)))+s.drop(10)
    case _=>x
  }
  def row(p:PrintWriter,x:Any*):Unit=ReportingSupport.row(p,x.map(value):_*)
  def manifest(out:File,first:Int,last:Int):Unit={
    val p=ReportingSupport.writer(out,"calendar.csv",Seq("synthetic_year","internal_fiscal_year","internal_start","internal_end_exclusive","month_convention","calendar_version"))
    try { (first to last).foreach(y=>ReportingSupport.row(p,year(y),y,s"${y-1}-07-01",s"$y-07-01","calendar_months_07_to_06_fiscal_order",Version)) }finally IdentityAudit.close(p)
  }
}
