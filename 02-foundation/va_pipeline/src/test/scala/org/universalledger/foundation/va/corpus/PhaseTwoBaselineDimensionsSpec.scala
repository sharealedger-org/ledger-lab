package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite

class PhaseTwoBaselineDimensionsSpec extends AnyFunSuite {
  private def write(file:File,text:String):Unit=Files.write(file.toPath,text.getBytes("UTF-8"))
  def temp(f:(File,File,File,File)=>Unit):Unit={val root=Files.createTempDirectory("baseline-dimensions-").toFile
    def write(file:File,text:String):Unit={file.getParentFile.mkdirs();Files.write(file.toPath,text.getBytes("UTF-8"))}
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try{
      val h=new File(root,"historical");val g=new File(root,"generated");val n=new File(root,"normalized")
      write(new File(h,"VendorMaster.csv"),"instID,instHolderName,instEffectDate,instEffectEndDate\n123,Original,2003-01-01,9999-99-99\n")
      write(new File(g,"VendorMaster.csv"),"instID,instHolderName,instEffectDate,instEffectEndDate\nwhole,Wholly fictional,2031-01-01,9999-99-99\nreuse,Sample supplier,2031-01-01,2032-01-01\n")
      write(new File(h,"synthetic_assignments.csv"),"vendor_id,template_instrument_id\n123,SIM_I000001\n")
      write(new File(g,"synthetic_assignments.csv"),"vendor_id,template_instrument_id\nwhole,SIM_I000002\nreuse,SIM_I000003\n")
      write(new File(g,"private/reassigned_actor_definitions.csv"),"vendor_id\nreuse\n")
      write(new File(n,"NORMALIZATION_COMPLETE"),"fixture\n")
      write(new File(n,"actor_candidates.csv"),"vendor_id,original_vendor,generation_class,private_actor_key,template_instrument_id\nreuse,0000000000,reassigned_activity,actor,SIM_I000003\nreuse,0000000000,reassigned_activity,actor,SIM_I000003\n")
      SyntheticReferenceBundle.specs.foreach{case(name,_,_)=>
        write(new File(h,name),"key,label\n1,Same\n2,Historical\n")
        write(new File(g,name),"key,label\n1,Same\n2,Fictional\n")
      }
      f(h,g,n,new File(root,"out"))
    }finally remove(root)
  }
  test("metadata rekeys and shifts originals, replaces sampled supplier masters, and preserves reference ambiguity") {temp{(h,g,n,out)=>
    PhaseTwoBaselineDimensions.prepare(h,g,Vector(n),out,2)
    val vendors=scala.collection.mutable.Map[String,Map[String,String]]()
    ConnectedViewsCheck.scan(new File(out,"VendorMaster.csv")){r=>assert(!vendors.contains(r("instID")));vendors(r("instID"))=r}
    assert(vendors.size==3)
    assert(vendors(StablePartyIdentity.observedVendor("123"))("instEffectDate")=="2031-01-01")
    assert(vendors("reuse")("instEffectEndDate")=="9999-99-99")
    var references=0
    ConnectedViewsCheck.scan(new File(out,"aRefFundCSV.txt")){_=>references+=1}
    assert(references==3)
  }}
  test("real five-column historical and three-column generated fund references retain their actual fields") {temp{(h,g,n,out)=>
    val prefix="FNDDTL_FUND_DETAIL_KEY,FNDDTL_FUND_DETAIL_CODE,FNDDTL_FUND_DETAIL_NAME"
    write(new File(h,"aRefFundCSV.txt"),prefix+",FND_FUND_CODE,FND_FUND_NAME\n201,201,Historical,10,GENERAL\n")
    write(new File(g,"aRefFundCSV.txt"),prefix+"\n201,201,Fictional\n")
    PhaseTwoBaselineDimensions.prepare(h,g,Vector(n),out,2)
    val rows=scala.collection.mutable.ArrayBuffer[Map[String,String]]()
    ConnectedViewsCheck.scan(new File(out,"aRefFundCSV.txt"))(rows+=_)
    assert(rows.size==2)
    assert(rows.find(_("FNDDTL_FUND_DETAIL_NAME")=="Historical").exists(r=>r("FND_FUND_CODE")=="10" && r("FND_FUND_NAME")=="GENERAL"))
    assert(rows.find(_("FNDDTL_FUND_DETAIL_NAME")=="Fictional").exists(r=>r("FND_FUND_CODE").isEmpty && r("FND_FUND_NAME").isEmpty))
  }}
  test("incompatible reference column order fails instead of assigning values to another field") {temp{(h,g,n,out)=>
    write(new File(h,"aRefFundCSV.txt"),"key,label,code\n1,Historical,10\n")
    write(new File(g,"aRefFundCSV.txt"),"label,key\nFictional,1\n")
    intercept[IllegalArgumentException]{PhaseTwoBaselineDimensions.prepare(h,g,Vector(n),out,2)}
    assert(!new File(out,"DIMENSIONS_COMPLETE").exists())
  }}
}
