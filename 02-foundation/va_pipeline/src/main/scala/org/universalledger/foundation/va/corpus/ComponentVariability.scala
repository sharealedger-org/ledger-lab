package org.universalledger.foundation.va.corpus

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.LocalDate

/** Bounded source recipes. Distributions are declared assumptions, not fitted risk probabilities. */
object ComponentVariability {
  val Version="component-variability-v2"
  def u(seed:String,key:String,purpose:String):Double={
    val b=MessageDigest.getInstance("SHA-256").digest((Version+"|"+seed+"|"+key+"|"+purpose).getBytes("UTF-8"))
    ((ByteBuffer.wrap(b).getLong >>> 11).toDouble+0.5)/9007199254740992.0
  }
  def normal(seed:String,key:String,purpose:String):Double={
    val z=StrictMath.sqrt(-2*StrictMath.log(u(seed,key,purpose+"-a")))*StrictMath.cos(2*StrictMath.PI*u(seed,key,purpose+"-b"))
    math.max(-4,math.min(4,z))
  }
  def weight(c:PhaseTwoCorpus.Config,index:Int,year:Int,family:String):Long={
    if(!c.variable)1L else {
      val persistent=normal(c.seed,index.toString,"size-"+family)
      val annual=normal(c.seed,index+"|"+year,"annual-"+family)
      math.max(1L,StrictMath.round(1000000*StrictMath.exp(0.9*persistent+0.25*annual)))
    }
  }
  /** Integer-shape gamma-Poisson mixture, with a minimum of one and a cap of 24. */
  def count(c:PhaseTwoCorpus.Config,index:Int,year:Int,family:String):Int={
    if(!c.variable)Map("EXP"->12,"REV"->12,"PO"->4,"EMP"->2)(family)
    else if(family=="EMP")2
    else {
      val mean=if(family=="REV")3.0 else if(family=="PO")4.0 else Vector(10.0,5.0,18.0)(index%3)
      val key=index+"|"+year+"|"+family
      val lambda= -mean/2*(StrictMath.log(u(c.seed,key,"gamma-a"))+StrictMath.log(u(c.seed,key,"gamma-b")))
      val stop=StrictMath.exp(-lambda);var product=1.0;var k=0
      while(k<24 && product>stop){product*=u(c.seed,key,"poisson-"+k);k+=1}
      if(k==24 && product>stop)24 else math.max(1,k-1)
    }
  }
  case class Event(slot:Int,day:LocalDate,available:LocalDate,cents:Long)
  def events(c:PhaseTwoCorpus.Config,index:Int,year:Int,family:String,total:Long):Vector[Event]={
    val n=count(c,index,year,family)
    val weights=(0 until n).map(s=>if(!c.variable)1L else math.max(1L,StrictMath.round(1000000*StrictMath.exp(normal(c.seed,s"$index|$year|$family|$s","amount")))))
    val sum=weights.sum;var used=0L
    (0 until n).map {s=>
      val key=s"$index|$year|$family|$s"
      val day=if(!c.variable || family=="EMP"){val m=if(family=="PO")s*3 else if(family=="EMP")s*6 else s;PhaseTwoCorpus.eventDay(year,m,index)}else {
        val season=Vector(0.8,1.0,1.0,1.0,1.0,1.3,1.0,1.0,1.0,1.0,1.0,2.0)
        val draw=u(c.seed,key,"month")*season.sum;var m=0;var cumulative=season(0)
        while(m<11 && draw>cumulative){m+=1;cumulative+=season(m)}
        val first=LocalDate.of(year-1,7,1).plusMonths(m)
        first.withDayOfMonth(1+(u(c.seed,key,"day")*first.lengthOfMonth()).toInt)
      }
      val amount=if(s==n-1)total-used else (BigInt(total)*weights(s)/sum).toLong;used+=amount
      val delay=if(!c.variable){if((index+s)%7==0)20 else 1}else math.min(90,math.max(1,StrictMath.round(StrictMath.exp(1.0+normal(c.seed,key,"arrival-delay"))).toInt))
      Event(s,day,day.plusDays(delay),amount)
    }.toVector
  }
}
