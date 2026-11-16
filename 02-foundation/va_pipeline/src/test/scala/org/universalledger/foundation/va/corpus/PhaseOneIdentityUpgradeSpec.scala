package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.{ReportingSupport,SortEngine,post,GLPost}

class PhaseOneIdentityUpgradeSpec extends AnyFunSuite {
  val sortSpec=new File("../sort_specs/SortedJE.sortspec")
  def temp(f:File=>Unit):Unit={val d=Files.createTempDirectory("identity-upgrade-").toFile
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try f(d)finally remove(d)
  }
  def write(f:File,lines:Seq[String]):Unit={f.getParentFile.mkdirs();Files.write(f.toPath,(lines.mkString("\n")+"\n").getBytes("UTF-8"))}
  def text(f:File):String=new String(Files.readAllBytes(f.toPath),"UTF-8")
  test("opaque IDs preserve exact source grouping, sentinels and extension stability without numeric bunching") {
    val original=(1 to 10000).map(_.formatted("%010d"))
    val ids=original.map(StablePartyIdentity.observedVendor)
    assert(ids.distinct.size==original.size && ids.forall(_.matches("v[0-9]{36}")))
    assert(original.reverse.map(StablePartyIdentity.observedVendor).reverse==ids)
    assert((original:+"new-key").map(StablePartyIdentity.observedVendor).take(10000)==ids)
    Vector("0000000000","000000000","999999999999","UNMATCHED_A").foreach(x=>assert(StablePartyIdentity.observedVendor(x)==x))
    assert(StablePartyIdentity.observedVendor("1")!=StablePartyIdentity.observedVendor("0000000001"))
    val order=ids.zipWithIndex.sortBy(_._1).map(_._2)
    val n=order.size.toDouble;val mean=(n-1)/2
    val covariance=order.zipWithIndex.map{case(a,b)=>(a-mean)*(b-mean)}.sum
    val variance=(0 until order.size).map(i=>math.pow(i-mean,2)).sum
    assert(math.abs(covariance/variance)<0.05)
    val adjacent=order.sliding(2).count(p=>p(0)/20==p(1)/20).toDouble/(order.size-1)
    assert(adjacent<0.01)
  }
  test("registry rejects duplicate old identities and does not claim a completed map") {temp{d=>
    val c=new File(d,"coverage.csv");write(c,Vector("vendor_id","123","123"))
    val out=new File(d,"map");intercept[IllegalArgumentException](PhaseOneIdentityUpgrade.prepare(c,out,2))
    assert(!new File(out,"IDENTITY_MAP_COMPLETE").exists())
  }}
  test("actual post and GL consume rekeyed canonical SJE with unchanged financial projection") {temp{d=>
    val old=new File(d,"old");old.mkdirs()
    def pair(v:String,period:String,amount:String,seq:Int):Vector[String]={
      val a=Array.fill(39)("0");a(0)=v;a(3)="J"+seq;a(4)="1";a(5)="PAYMENT";a(6)="Payment Sys"
      a(9)="ACTUALS";a(10)="FIN";a(11)="BOOK";a(12)="A1";a(13)="F1";a(14)="301";a(16)="EXP301"
      a(18)="USD";a(19)="TXN";a(20)="USD";a(21)="BASE";a(22)=period;a(23)="01-01-14";a(24)="01-01-14"
      a(25)=amount;a(29)="test-rule";a(30)="test-v1";a(31)="D";a(34)="RULE1:MQ";a(35)="SRC1:test:"+seq
      val b=a.clone();b(4)="2";b(16)="0000";b(25)=(-BigDecimal(amount)).toString;b(31)="O"
      Vector(a.mkString(","),b.mkString(","))
    }
    val source=new File(old,"SortedJEFY14merged.csv")
    val rows=pair("0000000001","0014/07","100.00",1)++pair("0000000002","0014/07","37.00",2)++pair("0000000000","0014/07","12.00",3)
    write(source,SortEngine.sortChunk(rows,SortEngine.loadSpec(sortSpec.getPath)))
    val coverage=new File(d,"coverage.csv");write(coverage,Vector("vendor_id","0000000002","0000000000","0000000001"))
    val map=new File(d,"map");PhaseOneIdentityUpgrade.prepare(coverage,map,2)
    val fresh=new File(d,"fresh");PhaseOneIdentityUpgrade.applyMap(source,map,fresh,sortSpec,2)
    val baseline=text(source).split("\n").map(_.split(",",-1).toVector.updated(0,"").mkString(",")).sorted.toVector
    val upgraded=text(new File(fresh,source.getName)).split("\n").map(_.split(",",-1).toVector.updated(0,"").mkString(",")).sorted.toVector
    assert(baseline==upgraded)
    Vector(old,fresh).foreach{x=>post(x.getPath+"/",x.getPath+"/","14");GLPost(Map("inPath"->x.getPath,"outPath"->x.getPath,"year"->"14"))}
    assert(text(new File(old,"GL2014.csv"))==text(new File(fresh,"GL2014.csv")))
    val reverse=scala.collection.mutable.Map[String,String]()
    ConnectedViewsCheck.scan(new File(map,"identity_map.csv"))(r=>reverse(r("artificial_id"))=r("original_id"))
    def ledger(file:File,remap:Boolean):Vector[String]={
      val in=new ReportingSupport.Input(file,"ledger",true,literalCanonical=true);val b=Vector.newBuilder[String]
      try {var n=in.next();while(n.nonEmpty){val r=n.get;val id=if(remap)reverse(r(0))else r(0);b+=r.updated(0,id).updated(3,"physical-row-ordinal").mkString(",");n=in.next()}}finally in.close();b.result().sorted
    }
    assert(ledger(new File(old,"LDGR2014.csv"),false)==ledger(new File(fresh,"LDGR2014.csv"),true))
    val bad=new File(d,"bad");write(new File(bad,"SortedJEFY14merged.csv"),Vector(rows.head.replace("0000000001,","MISSING_KEY,")))
    intercept[IllegalArgumentException](PhaseOneIdentityUpgrade.applyMap(new File(bad,"SortedJEFY14merged.csv"),map,new File(d,"reject"),sortSpec,2))
  }}
}
