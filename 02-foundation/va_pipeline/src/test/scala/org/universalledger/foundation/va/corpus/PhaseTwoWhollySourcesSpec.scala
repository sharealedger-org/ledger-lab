package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.ledger.ReportingSupport._

class PhaseTwoWhollySourcesSpec extends AnyFunSuite {
  def fixture(f:(File,File)=>Unit):Unit={
    val root=Files.createTempDirectory("wholly-sources-").toFile
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try{
      val input=new File(root,"input");input.mkdirs()
      val path="VARawFiles/FY 2031 Expenses/Expenses Jul 2030.csv"
      val raw=new File(input,path);raw.getParentFile.mkdirs()
      Files.write(raw.toPath,"header\nwhole-a\nreused\nwhole-b\n".getBytes("UTF-8"))
      val p=writer(input,"source_lineage.csv",Seq("source_file","source_row","locator","generation_class","event_id"))
      try Vector(3,1,2).foreach{n=>row(p,path,n,PhaseTwoWhollySources.locator(path,n),if(n==2)"reassigned_activity"else"wholly_generated","event"+n)}finally IdentityAudit.close(p)
      f(input,new File(root,"subset"))
    }finally remove(root)
  }
  test("reused financial copies are excluded and shifted raw ordinals retain event lineage") {fixture{(input,out)=>
    PhaseTwoWhollySources.prepare(input,out,1)
    val path="VARawFiles/FY 2031 Expenses/Expenses Jul 2030.csv"
    assert(new String(Files.readAllBytes(new File(out,path).toPath),"UTF-8")=="header\nwhole-a\nwhole-b\n")
    var count=0
    ConnectedViewsCheck.scan(new File(out,"source_subset_map.csv")){r=>
      count+=1
      assert(r("event_id")== (if(count==1)"event1"else"event3"))
      assert(r("subset_locator")==PhaseTwoWhollySources.locator(path,count))
    }
    assert(count==2 && new File(out,"WHOLLY_SOURCE_SUBSET_COMPLETE").isFile)
  }}
  test("an untracked raw record prevents publication") {fixture{(input,out)=>
    val raw=new File(input,"VARawFiles/FY 2031 Expenses/Expenses Jul 2030.csv")
    Files.write(raw.toPath,"extra\n".getBytes("UTF-8"),java.nio.file.StandardOpenOption.APPEND)
    intercept[IllegalArgumentException]{PhaseTwoWhollySources.prepare(input,out,2)}
    assert(!out.exists())
  }}
}
