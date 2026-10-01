package org.universalledger.foundation.va.datatypes

//***************************************************************************************************************
//
//  Structure:  UnviJournalAllEntity.scala - the journal entry structure
//
//  This structure is produced by the transStandardize module, and posted to ledger by the Post module
//
//(c) Copyright IBM Corporation. 2019
// SPDX-License-Identifier: Apache-2.0
// By Kip Twitchell
//
//  Created August 2019
//
//  Change Log:
//***************************************************************************************************************


// Changed from case class to class: Scala 2 case classes are limited to 22 fields (tuple arity limit).
// UnviJournalAllEntity has 92 fields. The class is only used in the Step 1 / PO path which is
// currently excluded from the pipeline. toString replicates case-class format expected by callers.
class UnviJournalAllEntity (
                                    //Universal Journal
                                    var ujIPID: String,
                                    var ujInstrumentID: String,
                                    var ujCommitmentID: String,
                                    var ujJrnlID: String,
                                    var ujJrnlLineID: String,
                                    var ujBusEventCD: String,
                                    var ulSourceID: String,
                                    var ujProcessID: String,
                                    var ujOriginalDocID: String,
                                    var ujJrnlDescript: String,
                                    ujLedgerID: String      = "ACTUALS",
                                    ujJrnlType: String      = "FIN",
                                    ujBookCodeID: String    = "SHRD-3RD-PARTY",
                                    var ujLegalEntityID: String,
                                    var ujCenterID: String,
                                    var ujAffliateEntityID: String,
                                    var ujAffliateCenterID: String,
                                    var ujProjectID: String,
                                    ujProductID: String  = "0",
                                    var ujAccountID: String,
                                    ujAltAccountID: String                 = "0",
                                    ujCurrencyCodeSourceID: String         = "USD",
                                    ujCurrencyTypeCodeSourceID: String     = "TXN",
                                    ujCurrencyCodeTargetID: String         = "USD",
                                    ujCurrencyTypeCodeTargetID: String     = "BASE-LE",
                                    var ujFiscalPeriod: String,
                                    var ujAcctDate: String,
                                    var ujTransDate: String,
                                    var ujMovementFlg: Boolean,
                                    var ujDirVsOffsetFlg: Boolean,
                                    ujReconcileFlg: Boolean = false,
                                    var ujAdjustFlg: Boolean,
                                    var ujUnitOfMeasure: String,
                                    var ujUnitPrice: BigDecimal,
                                    var ujTransAmount: BigDecimal,
                                    var ujStatisticAmount: BigDecimal,
                                    ujRuleSetID: String = " ",
                                    ujRuleID: String = " ",
                                    var ujExtensionIDSource: String,
                                    var ujExtnesionSourceType: String,
                                    ujExtensionIDAuditTrail: String = " ",
                                    ujExtensionIDClass: String = " ",
                                    ujExtensionIDDates: String = " ",
                                    var ujExtensionIDCustom: String = " ",

                                    //Universal Header
                                    var uhIPID: String,
                                    var uhInstrumentID: String,
                                    var uhCommitmentID: String,
                                    var uhHeaderID: String,
                                    var uhEntryDate: String,
                                    var uhCreateDate: String,
                                    uhUpdateDate: String = " ",
                                    uhPostedDate: String = " ",
                                    uhApproverID: String = " ",
                                    uhCreatorID: String = " ",
                                    uhCreditHashTotal: BigDecimal = 0,
                                    uhDebitHashTotal: BigDecimal = 0,

                                    //Commitment
                                    var ucIPID: String,
                                    var ucInstrumentID: String,
                                    var ucCommitmentID: String,
                                    var ucContractType: String,
                                    var ucContractStattDate: String,

                                    //Instrument/Contract
                                    var uiIPID: String,
                                    var uiInstrumentID: String,
                                    var uiInstrumentType: String,
                                    var uiInstrumentStattDate: String,

                                    //Invovled Party
                                    var ipIPID: String,
                                    var ipName: String,
                                    var ipAddress: String,
                                    var ipCity: String,
                                    var ipState: String,
                                    var ipCountry: String,
                                    var ipPostalCode: String,
                                    var ipEmail: String,
                                    var ipExternalID: String,
                                    ipSourceSystemID: String = " ",
                                    ipUnintID: String = " ",
                                    ipDistrictID: String = " ",


                                    //Trading Partners
                                    var tpIPID: String,
                                    var tpRelatedIPID: String,

                                    //Universal Journal Extension
                                    //Original PO Transaction
                                    var ujepoIPID: String,
                                    var ujepoInstrumentID: String,
                                    var ujepoCommitmentID: String,
                                    var ujepoInstID: String,
                                    var ujepoJrnlID: String,
                                    var ujepoJrnlLineID: String,
                                    var ujepoExtensionIDSource: String,
                                    var ujepoPONumber: String,
                                    var ujepoAgency: String,
                                    var ujepoOrderedDate: String,
                                    var ujepoVendorCommodityDesc: String,
                                    var ujepoQuantityOrdered: BigDecimal,
                                    var ujepoPrice: BigDecimal,
                                    var ujepoVendorID: String,
                                    var ujepoVendorName: String,
                                    var ujepoVendorAddress: String,
                                    var ujepoVendorCity: String,
                                    var ujepoVendorState: String,
                                    var ujepoVendorPostalCode: String,
                                    var ujepoVendorLocEmailAddress: String,
                                    var ujepoNIGPcode: String,
                                    var ujepoNIGPDescription: String,
                                    var ujepoUnitOfMeasureCode: String,
                                    var ujepoUnitOfMeasureDesc: String,
                                    var VendorPartNumber: String,
                                    var ujepoManPartNumber: String,

                                    //Original Payment Tran
                                    var ujepyIPID: String,
                                    var ujepyInstrumentID: String,
                                    var ujepyCommitmentID: String,
                                    var ujepyJrnlID: String,
                                    var ujepyJrnlLineID: String,
                                    var ujepyExtensionIDSource: String,
                                    var ujepyAgencyKey: String,
                                    var ujepyFundDetailKey: String,
                                    var ujepyObjectKey: String,
                                    var ujepySubProgramKey: String,
                                    var ujepyVendorName: String,
                                    var ujepyAmount: BigDecimal) {

 override def toString: String =
   s"UnviJournalAllEntity($ujIPID,$ujInstrumentID,$ujCommitmentID,$ujJrnlID,$ujJrnlLineID," +
   s"$ujBusEventCD,$ulSourceID,$ujProcessID,$ujOriginalDocID,$ujJrnlDescript," +
   s"$ujLedgerID,$ujJrnlType,$ujBookCodeID,$ujLegalEntityID,$ujCenterID," +
   s"$ujAffliateEntityID,$ujAffliateCenterID,$ujProjectID,$ujProductID,$ujAccountID," +
   s"$ujAltAccountID,$ujCurrencyCodeSourceID,$ujCurrencyTypeCodeSourceID,$ujCurrencyCodeTargetID,$ujCurrencyTypeCodeTargetID," +
   s"$ujFiscalPeriod,$ujAcctDate,$ujTransDate,$ujMovementFlg,$ujDirVsOffsetFlg," +
   s"$ujReconcileFlg,$ujAdjustFlg,$ujUnitOfMeasure,$ujUnitPrice,$ujTransAmount," +
   s"$ujStatisticAmount,$ujRuleSetID,$ujRuleID,$ujExtensionIDSource,$ujExtnesionSourceType," +
   s"$ujExtensionIDAuditTrail,$ujExtensionIDClass,$ujExtensionIDDates,$ujExtensionIDCustom," +
   s"$uhIPID,$uhInstrumentID,$uhCommitmentID,$uhHeaderID,$uhEntryDate,$uhCreateDate," +
   s"$uhUpdateDate,$uhPostedDate,$uhApproverID,$uhCreatorID,$uhCreditHashTotal,$uhDebitHashTotal," +
   s"$ucIPID,$ucInstrumentID,$ucCommitmentID,$ucContractType,$ucContractStattDate," +
   s"$uiIPID,$uiInstrumentID,$uiInstrumentType,$uiInstrumentStattDate," +
   s"$ipIPID,$ipName,$ipAddress,$ipCity,$ipState,$ipCountry,$ipPostalCode,$ipEmail,$ipExternalID," +
   s"$ipSourceSystemID,$ipUnintID,$ipDistrictID," +
   s"$tpIPID,$tpRelatedIPID," +
   s"$ujepoIPID,$ujepoInstrumentID,$ujepoCommitmentID,$ujepoInstID,$ujepoJrnlID,$ujepoJrnlLineID," +
   s"$ujepoExtensionIDSource,$ujepoPONumber,$ujepoAgency,$ujepoOrderedDate,$ujepoVendorCommodityDesc," +
   s"$ujepoQuantityOrdered,$ujepoPrice,$ujepoVendorID,$ujepoVendorName,$ujepoVendorAddress," +
   s"$ujepoVendorCity,$ujepoVendorState,$ujepoVendorPostalCode,$ujepoVendorLocEmailAddress," +
   s"$ujepoNIGPcode,$ujepoNIGPDescription,$ujepoUnitOfMeasureCode,$ujepoUnitOfMeasureDesc," +
   s"$VendorPartNumber,$ujepoManPartNumber," +
   s"$ujepyIPID,$ujepyInstrumentID,$ujepyCommitmentID,$ujepyJrnlID,$ujepyJrnlLineID," +
   s"$ujepyExtensionIDSource,$ujepyAgencyKey,$ujepyFundDetailKey,$ujepyObjectKey,$ujepySubProgramKey," +
   s"$ujepyVendorName,$ujepyAmount)"
}

object UnviJournalAllEntity {
  /** Returns a blank instance with all fields set to defaults.
   *  Use this instead of the constructor when all or most fields are left at default values,
   *  since Scala 2 limits constructor argument lists to 22. Mutate fields as needed after construction.
   */
  def empty: UnviJournalAllEntity = new UnviJournalAllEntity(
    ujIPID = " ", ujInstrumentID = " ", ujCommitmentID = " ", ujJrnlID = " ", ujJrnlLineID = " ",
    ujBusEventCD = " ", ulSourceID = " ", ujProcessID = " ", ujOriginalDocID = " ", ujJrnlDescript = " ",
    ujLegalEntityID = " ", ujCenterID = " ", ujAffliateEntityID = " ", ujAffliateCenterID = " ",
    ujProjectID = " ", ujAccountID = " ", ujFiscalPeriod = " ", ujAcctDate = " ", ujTransDate = " ",
    ujMovementFlg = true, ujDirVsOffsetFlg = false, ujAdjustFlg = false,
    ujUnitOfMeasure = " ", ujUnitPrice = 0, ujTransAmount = 0, ujStatisticAmount = 0,
    ujExtensionIDSource = " ", ujExtnesionSourceType = " ",
    uhIPID = " ", uhInstrumentID = " ", uhCommitmentID = " ", uhHeaderID = " ",
    uhEntryDate = " ", uhCreateDate = " ",
    ucIPID = " ", ucInstrumentID = " ", ucCommitmentID = " ", ucContractType = " ", ucContractStattDate = " ",
    uiIPID = " ", uiInstrumentID = " ", uiInstrumentType = " ", uiInstrumentStattDate = " ",
    ipIPID = " ", ipName = " ", ipAddress = " ", ipCity = " ", ipState = " ",
    ipCountry = " ", ipPostalCode = " ", ipEmail = " ", ipExternalID = " ",
    tpIPID = " ", tpRelatedIPID = " ",
    ujepoIPID = " ", ujepoInstrumentID = " ", ujepoCommitmentID = " ", ujepoInstID = " ",
    ujepoJrnlID = " ", ujepoJrnlLineID = " ", ujepoExtensionIDSource = " ",
    ujepoPONumber = " ", ujepoAgency = " ", ujepoOrderedDate = " ", ujepoVendorCommodityDesc = " ",
    ujepoQuantityOrdered = 0, ujepoPrice = 0, ujepoVendorID = " ", ujepoVendorName = " ",
    ujepoVendorAddress = " ", ujepoVendorCity = " ", ujepoVendorState = " ",
    ujepoVendorPostalCode = " ", ujepoVendorLocEmailAddress = " ",
    ujepoNIGPcode = " ", ujepoNIGPDescription = " ", ujepoUnitOfMeasureCode = " ",
    ujepoUnitOfMeasureDesc = " ", VendorPartNumber = " ", ujepoManPartNumber = " ",
    ujepyIPID = " ", ujepyInstrumentID = " ", ujepyCommitmentID = " ",
    ujepyJrnlID = " ", ujepyJrnlLineID = " ", ujepyExtensionIDSource = " ",
    ujepyAgencyKey = " ", ujepyFundDetailKey = " ", ujepyObjectKey = " ",
    ujepySubProgramKey = " ", ujepyVendorName = " ", ujepyAmount = 0
  )
}

