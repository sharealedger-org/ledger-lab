package org.universalledger.foundation.va.corpus

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

/** Offline identity contract, independent of scenario and stochastic recipe seed. */
object StablePartyIdentity {
  val Version="stable-party-identity-v1"
  private val Modulus=BigInt(10).pow(36)
  def opaque(namespace:String,privateKey:String,prefix:String="v"):String={
    require(namespace.nonEmpty && privateKey.nonEmpty && prefix.matches("[A-Za-z]{1,3}"))
    val payload=Version+"\u0000"+namespace+"\u0000"+privateKey
    val bytes=MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))
    val digits=(BigInt(1,bytes)%Modulus).toString
    prefix+("0"*(36-digits.length))+digits
  }
  def unresolved(id:String):Boolean=id.isEmpty || id.forall(_=='0') || id=="999999999999" || id.startsWith("UNMATCHED_")
  def observedVendor(original:String):String=if(unresolved(original))original else opaque("observed-canonical-vendor",original)
  def generatedVendor(population:String,key:String):String=opaque("synthetic-vendor",population+"\u0000"+key)
}
