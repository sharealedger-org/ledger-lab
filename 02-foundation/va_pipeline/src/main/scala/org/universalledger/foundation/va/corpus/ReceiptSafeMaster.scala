package org.universalledger.foundation.va.corpus

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.universalledger.foundation.va.ledger.{LedgerApp, ReportingSupport}
import scala.collection.mutable

/** Versioned input policy: agency receipt aliases are not vendor identities. */
object ReceiptSafeMaster {
  import ReportingSupport._
  def prepare(source: File, agency: File, out: File): Unit = {
    require(source.isFile && source.length() <= 16L*1024*1024 && !out.exists())
    val policy = new File(out.getParentFile,"receipt_policy"); require(policy.mkdirs())
    val keys = mutable.HashSet[String]("0") // unresolved agency fallback must also miss
    val ref = new Input(agency,"agency_reference",true)
    val ki = ref.header.indexOf("AGY_AGENCY_KEY"); require(ki >= 0)
    try { var next=ref.next(); while(next.nonEmpty) {
      require(ref.count <= 100000 && next.get.size == ref.header.size)
      keys += next.get(ki).trim.toUpperCase(java.util.Locale.ROOT); next=ref.next()
    }} finally ref.close()
    val in = new Input(source,"original_master",true)
    val ni=in.header.indexOf("instHolderName"); val ti=in.header.indexOf("instTypeID"); val ii=in.header.indexOf("instID")
    require(ni>=0 && ti>=0 && ii>=0)
    val filtered=new File(policy,"ttl_master_source.csv")
    val keep=writer(policy,filtered.getName,in.header)
    val exclusions=writer(policy,"excluded_receipt_aliases.csv",Seq("source_record","original_id","holder_name","reason","original_record_csv"))
    val lineage=writer(policy,"included_master_lineage.csv",Seq("source_record","filtered_record","original_id"))
    var included=0L; var excluded=0L
    try { var next=in.next(); while(next.nonEmpty) {
      val r=next.get; require(in.count<=100000 && r.size==in.header.size)
      if(r(ti).trim.toUpperCase(java.util.Locale.ROOT)=="REV") {
        row(exclusions,in.count,r(ii),r(ni),"agency_receipt_alias_not_vendor_identity",csv(r)); excluded+=1
      } else {
        require(!keys(r(ni).trim.toUpperCase(java.util.Locale.ROOT)),s"Vendor name collides with receipt agency key: ${r(ni)}")
        keep.println(csv(r)); included+=1; row(lineage,in.count,included,r(ii))
      }
      next=in.next()
    }} finally { in.close(); Seq(keep,exclusions,lineage).foreach(IdentityAudit.close) }
    SyntheticReferenceBundle.prepareMaster(filtered,out)
    val manifest=writer(policy,"policy_manifest.csv",Seq("policy","source_bytes","source_sha256","included_rows","excluded_rev_rows"))
    val digest=java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source.toPath)).map(b=>f"${b&255}%02x").mkString
    try row(manifest,"agency_receipts_unattributed_v2",source.length(),digest,included,excluded) finally IdentityAudit.close(manifest)
    Files.write(new File(policy,"SOURCE_POLICY_COMPLETE").toPath,"agency_receipts_unattributed_v2\n".getBytes(StandardCharsets.UTF_8))
  }
  def main(args:Array[String]):Unit={ val p=LedgerApp.parseArgs(args); prepare(new File(p("source")),new File(p("agency")),new File(p("out"))) }
}
