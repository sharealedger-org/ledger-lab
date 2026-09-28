package org.universalledger.foundation.va.datatypes

//***************************************************************************************************************
//
//  Structure:  VendorUpdate.scala — a changed Vendor Master record
//
//  A VendorUpdate record represents a new version of a VendorMaster record where one or more
//  attributes have changed since the last processing cycle.  Only vendors whose instTypeID
//  has changed trigger the Arrangement Reclass process.
//
//  The instEffectDate on this record is the effective date of the change.  If it is backdated
//  to a prior period, the reclass process must generate reclassification SJEs for every
//  affected accounting period back to that date.
//
//  (c) Copyright IBM Corporation. 2018
//  SPDX-License-Identifier: Apache-2.0
//  By Kip Twitchell
//
//  Change Log:
//  2025 - Added for Arrangement Reclass (Option 9) (Bob AI)
//***************************************************************************************************************

case class VendorUpdate(
  var instID:            String,  // Vendor ID — matches VendorMaster.instID
  var instTypeID:        String,  // New type: "EXP" or "REV"
  var instEffectDate:    String   // Effective date of the change (may be backdated)
)
