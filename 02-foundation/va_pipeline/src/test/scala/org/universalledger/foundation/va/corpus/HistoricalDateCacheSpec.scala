package org.universalledger.foundation.va.corpus

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.scalatest.funsuite.AnyFunSuite
import scala.util.Try

class HistoricalDateCacheSpec extends AnyFunSuite {
  // Frozen pre-optimization parser precedence, deliberately independent of the
  // new ISO dispatch and memoization. SMART US formats and canonical yy/MM/dd
  // ambiguities must retain their original meanings.
  private val canonical=DateTimeFormatter.ofPattern("yy/MM/dd")
  private def previousDate(s:String):Option[LocalDate]=
    if(s.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}"))
      try Some(LocalDate.parse(s,canonical))catch{case _:java.time.DateTimeException=>None}
    else ReassignedActivity.sourceDate(s).orElse(try Some(LocalDate.parse(s))catch{case _:java.time.DateTimeException=>None})
  private def previousShift(s:String):String=previousDate(s).map{d=>
    val target=d.plusYears(28);require(target.minusYears(28)==d,"Non-reversible synthetic calendar shift")
    if(s.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}"))target.format(canonical)
    else if(s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))target.toString
    else target.format(DateTimeFormatter.ofPattern("M/d/uuuu"))
  }.getOrElse(s)
  private def outcome(f: => String):Either[(String,String),String]=
    Try(Right(f)).recover{case e:Exception=>Left((e.getClass.getName,e.getMessage))}.get

  test("ISO fast path preserves legacy parsing and shifting across valid ambiguous and invalid dates") {
    val calendar=for{
      year<-Vector(2003,2004,2016,2031,2044)
      month<-1 to 12;day<-Vector(1,28,29,30,31)
      text<-Vector(f"$year%04d-$month%02d-$day%02d",s"$month/$day/$year",s"$month-$day-$year",
        f"${year%100}%02d/$month%02d/$day%02d",f"$month%02d/$day%02d/${year%100}%02d")
    }yield text
    val edge=Vector(""," ","not-a-date","13-45-03","2003-01-01T00:00:00"," 5/20/2003","5/20/2003 ",
      "0000-02-29","0001-02-03","0012-02-03","0013-02-03","0200-02-29","1899-12-31",
      "1900-02-29","2000-02-29","2100-02-29","2072-02-29","2003-02-30","2/30/2003","03/07/01")
    (calendar++edge).foreach{raw=>
      assert(PhaseTwoBaselineNormalize.date(raw)==previousDate(raw),raw)
      assert(outcome(PhaseTwoBaselineNormalize.shiftedDate(raw))==outcome(previousShift(raw)),raw)
    }
  }
  test("bounded positive and negative cache preserves narrower source eligibility and eviction semantics") {
    val cache=new HistoricalDateCache(2,128);val disabled=new HistoricalDateCache(0,128)
    val dates=Vector("2003-05-20","5/20/2003","03/07/01","bad-date","", "5/21/2003")
    (dates++dates.reverse).foreach{raw=>
      Vector(raw,raw).foreach{s=>
        val a=cache(s);val b=disabled(s)
        assert(a.parsed==b.parsed && a.shifted==b.shifted && a.sourceDate==b.sourceDate)
        assert(cache.size<=2 && cache.peak<=2)
      }
    }
    assert(cache.hits>0 && disabled.hits==0 && disabled.size==0)
    assert(cache("2003-05-20").parsed.nonEmpty && cache("2003-05-20").sourceDate.isEmpty)
    assert(cache("03/07/01").parsed.contains(LocalDate.of(2003,7,1)))
    assert(cache("03/07/01").sourceDate==ReassignedActivity.sourceDate("03/07/01"))
    val before=cache.misses;val long="x"*129
    assert(cache(long).parsed.isEmpty && cache(long).parsed.isEmpty)
    assert(cache.misses==before+2 && cache.size<=2)
  }
}
