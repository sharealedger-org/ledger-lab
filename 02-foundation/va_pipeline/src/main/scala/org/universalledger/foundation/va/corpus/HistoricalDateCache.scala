package org.universalledger.foundation.va.corpus

import java.time.LocalDate

/** Exact bounded memoization, including unsuccessful parses. Source eligibility
  * deliberately retains its narrower legacy parser; ISO date recognition does
  * not make a previously ineligible source eligible for fictional attribution.
  */
final class HistoricalDateCache(capacity:Int=4096,maxKeyChars:Int=128) {
  require(capacity>=0 && maxKeyChars>=0)
  case class Value(parsed:Option[LocalDate],shifted:String,sourceDate:Option[LocalDate])
  private val cache=new java.util.LinkedHashMap[String,Value](16,0.75f,true)
  var hits=0L;var misses=0L;var peak=0
  def size:Int=cache.size()
  def apply(raw:String):Value={
    val retain=capacity>0 && raw.length<=maxKeyChars
    val found=if(retain)cache.get(raw)else null
    if(found!=null){hits+=1;found}
    else{
      misses+=1
      val value=Value(PhaseTwoBaselineNormalize.date(raw),PhaseTwoBaselineNormalize.shiftedDate(raw),ReassignedActivity.sourceDate(raw))
      if(retain){
        if(cache.size()>=capacity){val it=cache.keySet().iterator();it.next();it.remove()}
        cache.put(raw,value);peak=peak.max(cache.size())
      }
      value
    }
  }
}
