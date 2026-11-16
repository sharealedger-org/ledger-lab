package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.{SortEngine,post,GLPost}

class PhaseTwoBaselineNormalizeSpec extends AnyFunSuite {
  val spec=new File("../sort_specs/SortedJE.sortspec")
  def temp(f:File=>Unit):Unit={val root=Files.createTempDirectory("baseline-normalize-").toFile
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try f(root)finally remove(root)
  }
  def write(f:File,s:String):Unit={f.getParentFile.mkdirs();Files.write(f.toPath,s.getBytes("UTF-8"))}
  def pair(id:String,amount:String,seq:Int,receipt:Boolean=false):Vector[String]={
    val r=Array.fill(39)("0");r(0)=id;r(3)="ID"+seq;r(4)="1";r(5)="PAYMENT";r(6)=if(receipt)"Revenue Sys"else"Payment Sys";r(8)="Named supplier example"
    r(9)="ACTUALS";r(10)="FIN";r(11)="BOOK";r(12)="A1";r(13)="F1";r(14)="301";r(16)=if(receipt)"REV301"else"EXP301"
    r(18)="USD";r(19)="TXN";r(20)="USD";r(21)="BASE";r(22)="0003/06";r(23)="03/06/01";r(24)="5/20/2003";r(25)=amount
    r(29)="base-rule";r(30)="rule-v1";r(31)="D";r(34)="RULE1:MQ";r(35)="SRC1:test:"+seq
    val o=r.clone();o(4)="2";o(8)="CASH_CLEARING";o(13)="0000";o(14)="0000";o(16)="0000";o(25)=(-BigDecimal(amount)).toString;o(31)="O"
    Vector(r.mkString(","),o.mkString(","))
  }
  test("paired attribution, known rekey, unresolved receipts and dates reach actual posting") {temp{root=>
    val original=pair("0000000000","-12.50",1)++pair("0000000123","50.00",2)++pair("0000000000","9.00",3,true)
    val source=new File(root,"source.csv");write(source,original.reverse.mkString("\n")+"\n")
    val objects=new File(root,"objects.csv");write(objects,"OBJ_OBJECT_KEY,2-DIGIT OBJECT CODE,4-DIGIT OBJECT NAME\n301,23,SUPPLIES\n")
    val out=new File(root,"normalized")
    PhaseTwoBaselineNormalize.prepare(source,objects,spec,out,2003,chunk=2)
    val canonical=new File(out,"SortedJEFY31merged.csv")
    val in=scala.io.Source.fromFile(canonical,"UTF-8");val rows=try in.getLines().map(_.split(",",-1).toVector).toVector finally in.close()
    assert(rows.size==6)
    rows.groupBy(_(35)).foreach{case(locator,group)=>
      assert(group.map(_(0)).distinct.size==1)
      assert(group.forall(r=>r(22)=="0031/06" && r(23)=="31/06/01" && r(24)=="5/20/2031"))
      assert(group.map(r=>BigDecimal(r(25))).sum==0)
      if(locator.endsWith(":1"))assert(group.head(0)==ReassignedRouting.supplier("phase2-v1",PhaseTwoCorpus.DefaultPopulation,"A1","23","Named supplier example")._1)
      if(locator.endsWith(":2"))assert(group.head(0)==StablePartyIdentity.observedVendor("0000000123"))
      if(locator.endsWith(":3"))assert(group.head(0)=="0000000000")
      group.foreach{r=>val old=original.map(_.split(",",-1).toVector).find(x=>x(35)==locator && x(31)==r(31)).get
        assert(old.indices.filterNot(Set(0,8,22,23,24)).forall(i=>old(i)==r(i)))
      }
    }
    assert(SortEngine.sortChunk(rows.map(_.mkString(",")),SortEngine.loadSpec(spec.getPath))==rows.map(_.mkString(",")))
    post(out.getPath+"/",out.getPath+"/","31")
    GLPost(Map("inPath"->out.getPath,"outPath"->out.getPath,"year"->"31"))
    assert(new File(out,"LDGR2031.csv").isFile && new File(out,"GL2031.csv").isFile)
  }}
  test("duplicate source journals fail the current-group bound without publishing") {temp{root=>
    val source=new File(root,"source.csv");write(source,(pair("0000000000","1",1)++pair("0000000000","1",1)).mkString("\n")+"\n")
    val objects=new File(root,"objects.csv");write(objects,"OBJ_OBJECT_KEY,2-DIGIT OBJECT CODE,4-DIGIT OBJECT NAME\n301,23,SUPPLIES\n")
    val out=new File(root,"bad")
    intercept[IllegalArgumentException]{PhaseTwoBaselineNormalize.prepare(source,objects,spec,out,2003,chunk=2)}
    assert(!out.exists())
  }}
}
