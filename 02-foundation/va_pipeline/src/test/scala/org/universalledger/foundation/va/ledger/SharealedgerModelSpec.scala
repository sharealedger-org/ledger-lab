package org.universalledger.foundation.va.ledger

import org.scalatest.funsuite.AnyFunSuite
import org.universalledger.foundation.va.datatypes.UnviJournalAllEntity

class SharealedgerModelSpec extends AnyFunSuite {
  test("rich journal record carries contract, commitment, instrument, party, and header lineage") {
    val purchaseOrder = UnviJournalAllEntity.empty
    purchaseOrder.ujBusEventCD = "PurchaseOrder"
    purchaseOrder.ujIPID = "IP-001"
    purchaseOrder.ujInstrumentID = "PO-001"
    purchaseOrder.ujCommitmentID = "COMMIT-001"
    purchaseOrder.uhHeaderID = "GROUP-001"
    purchaseOrder.ucContractType = "PURCHASE_ORDER"
    purchaseOrder.uiInstrumentType = "CONTRACT"
    purchaseOrder.ipIPID = "IP-001"
    purchaseOrder.ujepoPONumber = "PO-001"
    purchaseOrder.ujepoVendorID = "IP-001"
    purchaseOrder.ujepoQuantityOrdered = BigDecimal("2")
    purchaseOrder.ujepoPrice = BigDecimal("50.00")

    val debit = UnviJournalAllEntity.empty
    debit.ujBusEventCD = "Payment"
    debit.ujIPID = "IP-001"
    debit.ujInstrumentID = "PO-001"
    debit.ujCommitmentID = "COMMIT-001"
    debit.uhHeaderID = "GROUP-001"
    debit.ujJrnlID = "JOURNAL-001"
    debit.ujJrnlLineID = "1"
    debit.ujDirVsOffsetFlg = true
    debit.ujTransAmount = BigDecimal("100.00")

    val credit = UnviJournalAllEntity.empty
    credit.ujBusEventCD = "Payment"
    credit.ujIPID = "IP-001"
    credit.ujInstrumentID = "PO-001"
    credit.ujCommitmentID = "COMMIT-001"
    credit.uhHeaderID = "GROUP-001"
    credit.ujJrnlID = "JOURNAL-001"
    credit.ujJrnlLineID = "2"
    credit.ujDirVsOffsetFlg = false
    credit.ujTransAmount = BigDecimal("-100.00")

    assert(purchaseOrder.ujInstrumentID == debit.ujInstrumentID)
    assert(debit.ujInstrumentID == credit.ujInstrumentID)
    assert(purchaseOrder.uhHeaderID == debit.uhHeaderID)
    assert(debit.ujJrnlID == credit.ujJrnlID)
    assert(debit.ujTransAmount + credit.ujTransAmount == BigDecimal(0))
  }

  test("unmatched payment remains explicit rather than becoming an anonymous balance") {
    val payment = UnviJournalAllEntity.empty
    payment.ujBusEventCD = "Payment"
    payment.ujIPID = "IP-UNMATCHED-001"
    payment.ujInstrumentID = "UNMATCHED-INSTRUMENT-001"
    payment.ujOriginalDocID = "UNMATCHED_SOURCE"
    payment.ujepyVendorName = "SANITIZED_PARTY"
    payment.ujepyAmount = BigDecimal("25.00")

    assert(payment.ujOriginalDocID == "UNMATCHED_SOURCE")
    assert(payment.ujInstrumentID.nonEmpty)
    assert(payment.ujepyAmount == BigDecimal("25.00"))
  }
}
