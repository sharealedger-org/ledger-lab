package org.universalledger.foundation.va.corpus
import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
class ReassignedActivitySpec extends AnyFunSuite {
  def temp(f:File=>Unit):Unit={val d=Files.createTempDirectory("reassigned-test-").toFile
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try f(d)finally remove(d)
  }
  def write(f:File,lines:Seq[String]):Unit={f.getParentFile.mkdirs();Files.write(f.toPath,(lines.mkString("\n")+"\n").getBytes("UTF-8"))}
  test("selection is order-independent, retains posting period and excludes summary/known/receipt records") {temp{d=>
    def row(i:Int,name:String="Unresolved Named Supplier",kind:String="EXP",vendor:String="0000000000"):String={
      val a=Array.fill(39)("0");a(0)=vendor;a(6)=if(kind=="EXP")"Payment Sys"else"Revenue Sys";a(8)=name
      a(12)="A1";a(13)="F1";a(14)="301";a(16)=kind+"301";a(18)="USD";a(22)="0003/06";a(24)="5/20/2003";a(25)=i.toString;a(31)="D";a(35)="SRC1:test:"+i;a.mkString(",")
    }
    val refs=new File(d,"objects.csv");write(refs,Vector("OBJ_OBJECT_KEY,4-DIGIT OBJECT NAME","301,SERVICES"))
    val rows=(1 to 20).map(i=>row(i))++Vector(row(21,"PAYROLL INTERFACE"),row(22,"MULTIPLE VENDORS"),row(23,kind="REV"),row(24,vendor="123"))
    Vector("a"->rows,"b"->rows.reverse).foreach{case(name,data)=>val r=new File(d,name);write(new File(r,"FY03/SortedJEFY03merged.csv"),data);write(new File(r,"FY03/metrics/run_manifest.csv"),Vector("status","complete"))}
    val a=new File(d,"out-a");val b=new File(d,"out-b")
    ReassignedActivity.extract(new File(d,"a"),Vector(2003),refs,a,"seed",5)
    ReassignedActivity.extract(new File(d,"b"),Vector(2003),refs,b,"seed",5)
    def selected(root:File):Vector[String]={val result=Vector.newBuilder[String];ConnectedViewsCheck.scan(new File(root,"selected.csv")){r=>assert(r("source_period")=="0003/06");assert(r("source_date")=="2003-05-20");result+=r("source_locator")};result.result()}
    assert(selected(a)==selected(b) && selected(a).size==5)
    var eligible=0L;ConnectedViewsCheck.scan(new File(a,"selection_controls.csv"))(r=>if(r("reason")=="eligible")eligible=r("rows").toLong);assert(eligible==20)
  }}
}
