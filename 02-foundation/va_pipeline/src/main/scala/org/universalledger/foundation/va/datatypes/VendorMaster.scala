package org.universalledger.foundation.va.datatypes

//***************************************************************************************************************
//
//  Structure:  VendorMaster.scala — the Vendor Master (Instrument Master) record
//
//  One record per unique vendor. This is the Arrangement Master / Instrument Master in the
//  Universal Ledger model.  Each vendor is assigned a unique instID by the poVendorID or
//  transStandardize process.
//
//  The instTypeID field drives the Arrangement Reclass process: when it changes from EXP to REV
//  (or vice versa), reclassification journal entries must be generated for all affected balances.
//
//  The effective-dating fields (instEffectDate / instEffectEndDate) support the full date-effective
//  lookup described in the POC VA Data Layouts and Specs document — a join to an instID with a
//  given effective date should return the type that was in force on that date.
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  2025 - Added for Arrangement Reclass (Option 9) (Bob AI)
//***************************************************************************************************************

case class VendorMaster(
  var instID:               String,   // Unique vendor/instrument ID (e.g. "1229771")
  var instEffectDate:       String,   // Record effective start date (e.g. "2003-01-01")
  var instEffectEndDate:    String,   // Record effective end date   (e.g. "9999-99-99")
  var instHolderName:       String,   // Vendor name
  var instTypeID:           String,   // "EXP" (expense) or "REV" (revenue) — drives reclass
  var instVendorAddress:    String,
  var instVendorCity:       String,
  var instVendorState:      String,
  var instVendorPostalCode: String,
  var instAuditTrail:       String,   // Timestamp of last update
  var instNIGPClass:        String = "", // NIGP commodity class (e.g. "918")
  var instNIGPDesc:         String = "", // NIGP commodity description
  var instGeoRegion:        String = ""  // Geographic region (e.g. "SE", "VA")
)
