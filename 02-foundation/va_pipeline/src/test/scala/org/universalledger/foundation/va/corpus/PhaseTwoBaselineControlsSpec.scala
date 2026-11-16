package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite

class PhaseTwoBaselineControlsSpec extends AnyFunSuite {
  def fixture(f:(File,File,File)=>Unit):Unit={val root=Files.createTempDirectory("baseline-controls-").toFile
    def write(file:File,text:String):Unit={file.getParentFile.mkdirs();Files.write(file.toPath,text.getBytes("UTF-8"))}
    def remove(x:File):Unit={if(x.isDirectory)x.listFiles().foreach(remove);Files.delete(x.toPath)}
    try{
      val header="agency,nominal_account_id,fund,object_id,fiscal_year,dir_flag,total_amount\n"
      val old=header+"A1,EXP301,F1,301,0003/06,D,100\nA1,0000,F1,0,0003/06,O,-100\n"
      write(new File(root,"retained/financial_controls/FY03/GL2003.csv"),old)
      write(new File(root,"run/native/FY03/GL2003.csv"),old)
      write(new File(root,"run/wholly/FY31/GL2031.csv"),header+"A1,EXP301,F1,301,0031/06,D,20\nA1,0000,F1,0,0031/06,O,-20\n")
      write(new File(root,"run/output/FY31/GL2031.csv"),header+"A1,EXP301,F1,301,0031/06,D,120\nA1,0000,F1,0,0031/06,O,-120\n")
      Vector("native/FY03"->("2m",2),"wholly/FY31"->("2m",2),"output/FY31"->("3",4)).foreach{case(path,(pass,n))=>
        write(new File(root,s"run/$path/metrics/process_metrics.csv"),s"pass_id,status,input_rows,output_rows\n$pass,complete,$n,$n\n")
      }
      f(new File(root,"run"),new File(root,"retained"),new File(root,"controls"))
    }finally remove(root)
  }
  test("shifted historical plus wholly GL and exact journal counts reconcile") {fixture{(run,retained,out)=>
    PhaseTwoBaselineControls.check(run,retained,Vector(2003),out,false)
    assert(new File(out,"BASELINE_FINANCIAL_CONTROLS_COMPLETE").isFile)
  }}
  test("balanced but duplicated addition fails the GL conservation check") {fixture{(run,retained,out)=>
    val file=new File(run,"output/FY31/GL2031.csv")
    val changed=new String(Files.readAllBytes(file.toPath),"UTF-8").replace("120","140")
    Files.write(file.toPath,changed.getBytes("UTF-8"))
    intercept[IllegalArgumentException]{PhaseTwoBaselineControls.check(run,retained,Vector(2003),out,false)}
    assert(!out.exists())
  }}
}
