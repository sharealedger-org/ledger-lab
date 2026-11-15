package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite
import scala.collection.mutable

class ReceiptSafeMasterSpec extends AnyFunSuite {
  def put(root:File,name:String,text:String):File={ val f=new File(root,name); Files.write(f.toPath,text.getBytes(StandardCharsets.UTF_8)); f }
  test("receipt alias exclusion preserves original snapshot and observed vendor projection") {
    val root=Files.createTempDirectory("receipt-policy-").toFile
    val text="instID,instHolderName,instTypeID\n10,ACTUAL VENDOR,EXP\n900,101,REV\n"
    val source=put(root,"original.csv",text)
    val agency=put(root,"agency.csv","AGY_AGENCY_KEY\n101\n")
    val out=new File(root,"VendorMaster.csv")
    ReceiptSafeMaster.prepare(source,agency,out)
    val rows=mutable.ArrayBuffer[Map[String,String]](); ConnectedViewsCheck.scan(out)(rows+=_)
    assert(rows.toVector==Vector(Map("instID"->"0000000010","instHolderName"->"ACTUAL VENDOR","instTypeID"->"EXP")))
    assert(new String(Files.readAllBytes(source.toPath),StandardCharsets.UTF_8)==text)
    var exclusions=0L
    ConnectedViewsCheck.scan(new File(root,"receipt_policy/excluded_receipt_aliases.csv")){ r=> assert(r("original_id")=="900"); exclusions+=1 }
    assert(exclusions==1 && new File(root,"receipt_policy/SOURCE_POLICY_COMPLETE").isFile)
  }
  test("a remaining vendor name matching an agency key blocks input publication") {
    val root=Files.createTempDirectory("receipt-collision-").toFile
    val source=put(root,"original.csv","instID,instHolderName,instTypeID\n10,101,EXP\n")
    val agency=put(root,"agency.csv","AGY_AGENCY_KEY\n101\n")
    val out=new File(root,"VendorMaster.csv")
    intercept[IllegalArgumentException](ReceiptSafeMaster.prepare(source,agency,out))
    assert(!out.exists() && !new File(root,"receipt_policy/SOURCE_POLICY_COMPLETE").exists())
  }
}
