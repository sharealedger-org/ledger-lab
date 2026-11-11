package org.universalledger.foundation.va.ledger

import java.io.{File, PrintWriter}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import java.util.Calendar
import sys.process._
import org.universalledger.foundation.va.datatypes.Ledger
import org.universalledger.foundation.va.datatypes.Transaction
import org.universalledger.foundation.va.datatypes.UnviJournalAllEntity
import java.security.MessageDigest


//***************************************************************************************************************
//
//  Program:  dataStandarize - Standardize Data into a common journal entry
//
//  This program reads transaction data and produces standardized journals
//
//
//  *(c) Copyright IBM Corporation. 2018
//  * SPDX-License-Identifier: Apache-2.0
//  * By Kip Twitchell
//  Created July 2018
//
//  Change Log:
//  To Do:
//         - Fix Direct VS. Offset Flag
//         - ReConsider Business Event File
//***************************************************************************************************************


object transStandardize {
  // ── Load VendorMaster: instHolderName.toUpperCase → instID ──────────────────
  private def loadVendorMap(basePath: String): Map[String, String] = {
    val candidates = Seq(
      basePath + "VendorMaster.csv",
      basePath + "../data/VendorMaster.csv",
      "data/VendorMaster.csv"
    )
    val path = candidates.find(p => new File(p).exists()).getOrElse("")
    if (path.isEmpty) {
      System.err.println("WARNING: VendorMaster not found; VENDORID lookups will produce UNMATCHED_ keys")
      return Map.empty
    }
    val vm = mutable.HashMap[String, String]()
    implicit val codec = scala.io.Codec.UTF8
      .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
      .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
    val src = Source.fromFile(path)(codec)
    try {
      val lines = src.getLines()
      if (!lines.hasNext) return vm.toMap
      val headerRaw = lines.next().stripPrefix("\uFEFF")
      val header = headerRaw.split(",").map(_.trim.stripPrefix("\"").stripSuffix("\""))
      val nameIdx = header.indexOf("instHolderName")
      val idIdx   = header.indexOf("instID")
      if (nameIdx < 0 || idIdx < 0) {
        System.err.println("WARNING: VendorMaster missing instHolderName or instID column")
        return vm.toMap
      }
      for (line <- lines) {
        val cols = line.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
        if (cols.length > math.max(nameIdx, idIdx)) {
          val name  = cols(nameIdx).toUpperCase
          val rawId = cols(idIdx)
          val instId = try { rawId.toLong.formatted("%010d") }
                       catch { case _: NumberFormatException => rawId }
          if (name.nonEmpty && rawId.nonEmpty) vm(name) = instId
        }
      }
    } finally {
      src.close()
    }
    vm.toMap
  }

  // ── Stable unmatched key: "UNMATCHED_" + first 8 hex chars of SHA-256 ───────
  private def unmatchedKey(vendorName: String): String = {
    val bytes = MessageDigest.getInstance("SHA-256")
      .digest(vendorName.toUpperCase.getBytes("UTF-8"))
    val hex = bytes.take(4).map("%02X".format(_)).mkString
    "UNMATCHED_" + hex
  }

  def apply(fileInLocation: String, fileOutLocation: String): Unit = {

    println("*" * 100)
    println("                                Data Transformation Module")
    println("Start Time: " + Calendar.getInstance.getTime)
    println("*" * 100)


    if (fileInLocation.length == 0) {
      println("Invalid input file invocation parameters.  Program abort.")
      sys.exit(0)
    }
    if (fileOutLocation.length == 0) {
      println("Invalid output file invocation parameters.  Program abort.")
      sys.exit(0)
    }

    val VAInDataPath = fileInLocation
    val VAOutDataPath = fileOutLocation

    // Load VendorMaster once as a bounded side-input for payment vendor-name lookup
    val vendorMap = loadVendorMap(VAInDataPath)

    // Accumulator for unmatched payment vendors: normalized name → count
    val unmatchedVendors = mutable.HashMap[String, Int]()

    //*****************************************************************************************
    // Open File Name Table
    //*****************************************************************************************
    //
    val fileNames = Source.fromFile(VAInDataPath + "FileList.csv").getLines
    fileNames.drop(1) //Drop header record
    // Schema:  POFileName,PayFileName,Contents,RecFormat,Delimiter,FileCount,FileYear,FileQtr,RecCount,ControlAmtSum
    println("FileName,Type (PO or Purchase),Contents(Expense vs Revenue),RecFormat(VA Format),Delimiter(Text vs Comma),FileCount,FileYear,FileQtr,RecCount,ControlAmtSum")

    // Open Output File and Write Header Record
    val fileName = VAOutDataPath + "JEHeader.csv"
    val outHeader: PrintWriter = new PrintWriter(new File(fileName))

    tranHeader(outHeader)
    outHeader.close()

    var transRead = 0 //counter for number of records read
    var poFilesProcessed = 0 //counter for PO files processed
    var payFilesProcessed = 0 //counter for Payment files processed
    var journalsLinesWritten = 0
    var journalsWritten = 0

    //*****************************************************************************************
    // Process Loop
    //*****************************************************************************************
    //

    // Process each entry in the file name listing
    for (fn <- fileNames) {
      val cols = fn.split(",").map(_.trim)
      val FileName = cols(0)
      val Type = cols(1)
      val Contents = cols(2)
      val RecFormat = cols(3)
      val Delimiter = cols(4)
      val FileCount = cols(5)
      val FileYear = cols(6)
      val FileQtr = cols(7)
      val RecCount = cols(8)
      val ControlAmtSum = cols(9)
      payFilesProcessed += 1
      println("processing file: " + fn)

      var transOutRec: Any = null  // holds either UnviJournalAllEntity (PO path) or Transaction (Payment path)
      val JrnlDate = (FileQtr.toInt * 3).toString + "/" + "01" + "/" + FileYear
      val fileDelimiter = Delimiter match {
        case "T" => "\t";
        case "C" => ","
      }


      Contents match {
        // Contents: E=Expense payments, R=Revenue payments, PO=Purchase Orders
        case "E" | "R" => processPayments()
        case "PO"      => processPOs()
        case other     => println(s"  Skipping unknown file type: $other")
      }

      def processPOs(): Unit = {
        //*****************************************************************************************
        // Process PO Data
        //*****************************************************************************************
        println("PO File")

        // Open Output File and Write Header Record
        val fileName = VAOutDataPath + "PO_" + FileYear + ".csv"
        val outPOs: PrintWriter = new PrintWriter(new File(fileName))

        // Expense type record structure
        case class poCLS(
                          poNumber: String,
                          poAgency: String,
                          poOrderedDate: String,
                          poJrnlDate: String,
                          poVendorCommodityDesc: String,
                          poQuantityOrdered: BigDecimal,
                          poPrice: BigDecimal,
                          poVendorID: String,
                          poVendorName: String,
                          poVendorAddress: String,
                          poVendorCity: String,
                          poVendorState: String,
                          poVendorPostalCode: String,
                          poVendorLocEmailAddress: String,
                          poNIGPcode: String,
                          poNIGPDescription: String,
                          poUnitOfMeasureCode: String,
                          poUnitOfMeasureDesc: String,
                          poVendorPartNumber: String,
                          poManPartNumber: String,
                                            )
        val poRaw = ArrayBuffer[poCLS]()

        // Read input records
        val lineIn: Seq[String] = Source.fromFile(VAInDataPath + FileName).getLines.toList.drop(1)
        for (i <- lineIn) {
          val cols = i.split(fileDelimiter).map(_.trim)
          poRaw.+=(poCLS(cols(0), cols(1), cols(2), JrnlDate, cols(3), BigDecimal(cols(4)), BigDecimal(cols(5)),
            cols(6),cols(7),cols(8),cols(9), cols(10), cols(11), cols(12),cols(13),cols(14),cols(15), cols(16),cols(17),cols(18) ))
        }
        //Format output records
        var jrnlID = 0
        for (rec <- poRaw) {
          jrnlID += 1
          journalsWritten += 1

          // Journal Line 1 ("debit" side or original entry)
          // All fields default to " " / 0 via UnviJournalAllEntity.empty; mutate only the
          // fields actually populated by this PO path. (Scala 2 limits constructor arg lists
          // to 22; the full 92-field form is not usable directly.)
          val poRec = UnviJournalAllEntity.empty
          poRec.ujepoPONumber          = rec.poNumber
          poRec.ujepoAgency            = rec.poAgency
          poRec.ujepoOrderedDate       = rec.poOrderedDate
          poRec.ujepoVendorCommodityDesc = rec.poVendorCommodityDesc
          poRec.ujepoQuantityOrdered   = rec.poQuantityOrdered
          poRec.ujepoPrice             = rec.poPrice
          poRec.ujepoVendorID          = rec.poVendorID
          poRec.ujepoVendorName        = rec.poVendorName
          poRec.ujepoVendorAddress     = rec.poVendorAddress
          poRec.ujepoVendorCity        = rec.poVendorCity
          poRec.ujepoVendorState       = rec.poVendorState
          poRec.ujepoVendorPostalCode  = rec.poVendorPostalCode
          poRec.ujepoVendorLocEmailAddress = rec.poVendorLocEmailAddress
          poRec.ujepoNIGPcode          = rec.poNIGPcode
          poRec.ujepoNIGPDescription   = rec.poNIGPDescription
          poRec.ujepoUnitOfMeasureCode = rec.poUnitOfMeasureCode
          poRec.ujepoUnitOfMeasureDesc = rec.poUnitOfMeasureDesc
          poRec.VendorPartNumber       = rec.poVendorPartNumber
          poRec.ujepoManPartNumber     = rec.poManPartNumber

          outPOs.write(poRec.toString.substring(12, poRec.toString.length() - 1) + "\n")
          journalsLinesWritten += 1


          // Journal line 2 ("credit" side or offset)
          // transIPID set from VENDORID — the nine-digit eVA vendor identifier is the
          // natural instrument key for commitment events (Sub-Task 1 fix).
          transOutRec = Transaction(
            transIPID = rec.poVendorID,
            transContractID = "0",
            transCommitmentID = "0",
            transJrnlID = "ID" + jrnlID.toString + JrnlDate,
            transJrnlLineID = "2", //create journal line 2
            transBusinessEventCode = "Cash Disbursement",
            transSourceSystemID = "\"Payment Sys\"",
            transOriginalDocID = "Unknown",
            transJrnlDescript = "Check to vendor",
            transLegalEntityID = rec.poAgency,
            transCenterID = "0000",
            transProjectID = "0000",
            transNominalAccountID = "0000",
            transFiscalPeriod = FileYear,
            transAcctDate = JrnlDate,
            transTransDate = rec.poOrderedDate,
            transTransAmount = rec.poPrice * rec.poQuantityOrdered * -1, //reverse sign on entry
            transDirVsOffsetFlg = "D",
            transAdjustFlg = "N"
          )
          outPOs.write(transOutRec.toString.substring(12, transOutRec.toString.length() - 1) + "\n")
          journalsLinesWritten += 1

        }
        outPOs.close()
      }


      def processPayments(): Unit = {
        //*****************************************************************************************
        // Process Payments
        //*****************************************************************************************

        Contents match {
          // Test for File types to parse correctly

          case "E" =>
            // Process Expense files
            println("Expense File")

            // Open Output File and Write Header Record
            val payFile = Type  // Type = cols(1) = PaymentFileName
            val fileName = VAOutDataPath + "JE" + payFile.substring(0, math.min(9, payFile.length)) + ".csv"
            val outJournals: PrintWriter = new PrintWriter(new File(fileName))

            // Expense type record structure
            case class transECLS(tranAgency: String, tranFund: String, tranObj: String, tranProg: String,
                                 tranVendorName: String, tranDate: String, tranAmt: BigDecimal)
            val transRaw = ArrayBuffer[transECLS]()

            // Read input records
            val lineIn = Source.fromFile(VAInDataPath + payFile).getLines.toList.drop(1)
            for (i <- lineIn) {
              val cols = i.split(fileDelimiter).map(_.trim.stripPrefix("\"").stripSuffix("\""))
              RecFormat match {
                case "A" => transRaw.+=(transECLS(cols(0), cols(1), cols(2), cols(3), cols(4), JrnlDate, BigDecimal(cols(5)))) // assign years 03 - 11 to Expense type A record structure
                case "B" => transRaw.+=(transECLS(cols(0), cols(1), cols(2), cols(3), cols(4), cols(6), BigDecimal(cols(5)))) // assign years 12 - 13 to Expense type B record structure
                case "C" => transRaw.+=(transECLS(cols(0), cols(2), cols(3), cols(4), cols(5), cols(6), BigDecimal(cols(1)))) // assign years 14 - 16 to Expense type C record structure
              }
            }

            // Aggregate input data keyed on (agency × fund × obj × prog × vendorName × date).
            // vendorName is included in the key so instrument identity (transIPID) can be
            // looked up from VendorMaster on the output side (Sub-Task 1 fix).
            case class aggTrans(tranAgency: String, tranFund: String, tranObj: String, tranProg: String,
                                tranVendorName: String, tranDate: String)
            implicit object OrderAggTrans extends Ordering[aggTrans] {
              def compare(a: aggTrans, b: aggTrans): Int = {
                if (a.tranAgency == b.tranAgency) {
                  if (b.tranFund == b.tranFund) {
                    if (a.tranObj == b.tranObj) {
                      if (a.tranProg == b.tranProg) {
                        if (a.tranVendorName == b.tranVendorName) {
                          if (a.tranDate == b.tranDate) {
                            0
                          }
                          a.tranDate compare b.tranDate
                        }
                        a.tranVendorName compare b.tranVendorName
                      }
                      a.tranProg compare b.tranProg
                    }
                    a.tranObj compare b.tranObj
                  }
                  a.tranFund compare b.tranFund
                }
                a.tranAgency compare b.tranAgency
              }
            }
            var aggAllSmallTBL: mutable.Map[aggTrans, BigDecimal] = mutable.Map.empty

            for (i <- transRaw) {
              transRead += 1
              val w = aggTrans(i.tranAgency, i.tranFund, i.tranObj, i.tranProg, i.tranVendorName, i.tranDate)
              if (aggAllSmallTBL contains w) {
                aggAllSmallTBL.+=(w -> (aggAllSmallTBL(w) + i.tranAmt))
              }
              else aggAllSmallTBL.+=(w -> i.tranAmt)
            }

            //sort the aggregated journal entries
            val sortedAggAllSmallTBL = aggAllSmallTBL.toList.
              sortBy(_._1.tranDate).
              sortBy(_._1.tranVendorName).
              sortBy(_._1.tranProg).
              sortBy(_._1.tranObj).
              sortBy(_._1.tranFund).
              sortBy(_._1.tranAgency)

            //Format output records
            var jrnlID = 0
            for (rec <- sortedAggAllSmallTBL) {
              jrnlID += 1
              journalsWritten += 1

              // Look up transIPID from VendorMaster using normalized vendor name.
              // Unmatched vendors receive a stable UNMATCHED_<hex8> key and are recorded
              // in the unmatchedVendors control total (Sub-Task 1 fix).
              val vendorNameNorm = rec._1.tranVendorName.toUpperCase
              val ipid = vendorMap.getOrElse(vendorNameNorm, {
                val key = unmatchedKey(vendorNameNorm)
                unmatchedVendors(vendorNameNorm) = unmatchedVendors.getOrElse(vendorNameNorm, 0) + 1
                key
              })

              // Journal Line 1 ("debit" side or original entry)
              transOutRec = Transaction(
                transIPID = ipid,
                transContractID = "0",
                transCommitmentID = "0",
                transJrnlID = "ID" + jrnlID.toString + JrnlDate,
                transJrnlLineID = "1",
                transBusinessEventCode = "Procurement",
                transSourceSystemID = "\"Payment Sys\"",
                transOriginalDocID = "Unknown",
                transJrnlDescript = "Procurement for goods or services", //tranVendorName in universal journal
                transLegalEntityID = rec._1.tranAgency,
                transCenterID = rec._1.tranFund,
                transProjectID = rec._1.tranObj,
                transNominalAccountID = "EXP" + rec._1.tranProg,
                transFiscalPeriod = FileYear,
                transAcctDate = JrnlDate,
                transTransDate = rec._1.tranDate,
                transTransAmount = rec._2,
                transDirVsOffsetFlg = "D",
                transAdjustFlg = "N"
              )
              outJournals.write(transOutRec.toString.substring(12, transOutRec.toString.length() - 1) + "\n")
              journalsLinesWritten += 1


              // Journal line 2 ("credit" side or offset)
              transOutRec = Transaction(
                transIPID = ipid,
                transContractID = "0",
                transCommitmentID = "0",
                transJrnlID = "ID" + jrnlID.toString + JrnlDate,
                transJrnlLineID = "2", //create journal line 2
                transBusinessEventCode = "Cash Disbursement",
                transSourceSystemID = "\"Payment Sys\"",
                transOriginalDocID = "Unknown",
                transJrnlDescript = "Check to vendor",
                transLegalEntityID = rec._1.tranAgency,
                transCenterID = "0000",
                transProjectID = "0000",
                transNominalAccountID = "0000",
                transFiscalPeriod = FileYear,
                transAcctDate = JrnlDate,
                transTransDate = rec._1.tranDate,
                transTransAmount = rec._2 * -1, //reverse sign on entry
                transDirVsOffsetFlg = "D",
                transAdjustFlg = "N"
              )
              outJournals.write(transOutRec.toString.substring(12, transOutRec.toString.length() - 1) + "\n")
              journalsLinesWritten += 1

            }
            outJournals.close()


          case "R" =>
            // process Revenue Records
            println("Revenue File")

            // Open Output File and Write Header Record
            val payFile = Type  // Type = cols(1) = PaymentFileName
            val fileName = VAOutDataPath + "JE" + payFile.substring(0, math.min(7, payFile.length)) + ".csv"
            val outJournals: PrintWriter = new PrintWriter(new File(fileName))

            // Revenue type record structure
            case class transRCLS(tranAgency: String, tranFund: String, tranSrc: String, tranDate: String, tranAmt: BigDecimal)

            val transRaw = ArrayBuffer[transRCLS]()
            //          var transRaw: Array[transECLS.type] = Array(transECLS)

            // Read input records
            val lineIn = Source.fromFile(VAInDataPath + payFile).getLines.toList.drop(1)
            for (i <- lineIn) {
              val cols = i.split(fileDelimiter).map(_.trim)
              RecFormat match {
                case "A" => transRaw.+=(transRCLS(cols(0), cols(1), cols(2), JrnlDate, BigDecimal(cols(3)))) // assign years 03 - 13 to Revenue type A record structure
                case "C" => transRaw.+=(transRCLS(cols(0), cols(2), cols(3), cols(4), BigDecimal(cols(1)))) // assign years 14 - 16 to Revenue type C record structure
              }
            }

            //Aggregate input data based upon four of five fields
            case class aggTrans(tranAgency: String, tranFund: String, tranSrc: String, tranDate: String)
            implicit object OrderAggTrans extends Ordering[aggTrans] {
              def compare(a: aggTrans, b: aggTrans): Int = {
                if (a.tranAgency == b.tranAgency) {
                  if (b.tranFund == b.tranFund) {
                    if (a.tranSrc == b.tranSrc) {
                      if (a.tranDate == b.tranDate) {
                        0
                      }
                      a.tranDate compare b.tranDate
                    }
                    a.tranSrc compare b.tranSrc
                  }
                  a.tranFund compare b.tranFund
                }
                a.tranAgency compare b.tranAgency
              }
            }
            //          val aggTransOut: mutable.Map[aggTrans] = aggTrans(transRaw.tranAgency,transRaw.tranFund,transRaw.tranObj,transRaw.tranProg,transRaw.tranDate)
            var aggAllSmallTBL: mutable.Map[aggTrans, BigDecimal] = mutable.Map.empty

            for (i <- transRaw) {
              transRead += 1
              val w = aggTrans(i.tranAgency, i.tranFund, i.tranSrc, i.tranDate)
              if (aggAllSmallTBL contains w) {
                aggAllSmallTBL.+=(w -> (aggAllSmallTBL(w) + i.tranAmt))
              }
              else aggAllSmallTBL.+=(w -> i.tranAmt)
            }

            //sort the aggregated journal entries
            val sortedAggAllSmallTBL = aggAllSmallTBL.toList.
              sortBy(_._1.tranDate).
              sortBy(_._1.tranSrc).
              sortBy(_._1.tranFund).
              sortBy(_._1.tranAgency)

            //Format output records
            var jrnlID = 0
            for (rec <- sortedAggAllSmallTBL) {
              jrnlID += 1
              journalsWritten += 1

              // Journal Line 1 ("debit" side or original entry)
              transOutRec = Transaction(
                transIPID = "0",
                transContractID = "0",
                transCommitmentID = "0",
                transJrnlID = "ID" + jrnlID.toString + JrnlDate + "REV",
                transJrnlLineID = "1",
                transBusinessEventCode = "Tax Receipt",
                transSourceSystemID = "\"Revenue Sys\"",
                transOriginalDocID = "Unknown",
                transJrnlDescript = "Tax Receipt from Taxpayer", //tranVendorName in universal journal
                transLegalEntityID = rec._1.tranAgency,
                transCenterID = rec._1.tranFund, // assign years 03 - 9 to Expense type A record structure
                transProjectID = " ",
                transNominalAccountID = "REV" + rec._1.tranSrc,
                transFiscalPeriod = FileYear,
                transAcctDate = JrnlDate,
                transTransDate = rec._1.tranDate,
                transTransAmount = rec._2,
                transDirVsOffsetFlg = "D",
                transAdjustFlg = "N"
              )
              outJournals.write(transOutRec.toString.substring(12, transOutRec.toString.length() - 1) + "\n")
              journalsLinesWritten += 1

              // Journal line 2 ("credit" side or offset)
              transOutRec = Transaction(
                transIPID = "0",
                transContractID = "0",
                transCommitmentID = "0",
                transJrnlID = "ID" + jrnlID.toString + JrnlDate + "REV",
                transJrnlLineID = "2", //create journal line 2
                transBusinessEventCode = "Cash Receipt",
                transSourceSystemID = "\"Revenue Sys\"",
                transOriginalDocID = "Unknown",
                transJrnlDescript = "Tax Cash or Check",
                transLegalEntityID = rec._1.tranAgency,
                transCenterID = "0000",
                transProjectID = "0000",
                transNominalAccountID = "0000",
                transFiscalPeriod = FileYear,
                transAcctDate = JrnlDate,
                transTransDate = rec._1.tranDate,
                transTransAmount = rec._2 * -1, //reverse sign on entry
                transDirVsOffsetFlg = "O", // offset line
                transAdjustFlg = "N"
              )
              outJournals.write(transOutRec.toString.substring(12, transOutRec.toString.length() - 1) + "\n")
              journalsLinesWritten += 1

            }
            outJournals.close()
        }
        //*****************************************************************************************
        // End of Payment process, before loop to next file
        //*****************************************************************************************
      }
      //*****************************************************************************************
      // End of file process, before loop to next file
      //*****************************************************************************************
    }


    //*****************************************************************************************
    // Write UNMATCHED_VENDORS.csv control total
    //*****************************************************************************************
    if (unmatchedVendors.nonEmpty) {
      val unmatchedFile = VAOutDataPath + "UNMATCHED_VENDORS.csv"
      val unmatchedOut = new PrintWriter(new File(unmatchedFile))
      unmatchedOut.write("vendor_name_normalized,unmatched_key,occurrence_count\n")
      for ((name, count) <- unmatchedVendors.toSeq.sortBy(_._1)) {
        val key = unmatchedKey(name)
        unmatchedOut.write(s"${name.replace(",", " ")},$key,$count\n")
      }
      unmatchedOut.close()
      println("UNMATCHED_VENDORS: " + unmatchedFile + " (" + unmatchedVendors.size + " distinct unmatched vendor names)")
    }

    //*****************************************************************************************
    // Print Control Reports
    //*****************************************************************************************

    println("*" * 100)
    println("Files Processed:       " + payFilesProcessed)
    println("Records Read:          " + transRead)
    println("Journal Lines Written: " + journalsLinesWritten)
    println("Journals Written:      " + journalsWritten)
    println("Process End Time:      " + Calendar.getInstance.getTime)

    //*****************************************************************************************
    // End of Program
    //*****************************************************************************************


    //    var addFileCnt = 0

    //*****************************************************************************************
    // Prompt about additional business event transactions
    //
    // Although this prompt could be useful if a user interface was developed to allow
    // users to create new business events; it probably isn't that important to create.
    // The Video and explain for what is produced by the user interace can simply point
    // to existing VA data outputs as examples.  THus this code is probably not needed.
    //*****************************************************************************************
    //    println("Additional Transactions:  Would you like to process your added transaction? (y/n)")
    //    var fileAddTrans: String = "AddTrans.txt"
    //    val procTrans = scala.io.StdIn.readLine().trim.toUpperCase()
    //    if (procTrans.substring(0, 1) == "Q") sys.exit(0)
    //    if (procTrans.substring(0, 1) == "Y") {
    //      addFileCnt += 1
    //      println("The default file name for these transaction is AddTrans.txt")
    //      println("Would you like to change this? (y/n)")
    //      val addTranFiles = scala.io.StdIn.readLine().trim.toUpperCase()
    //      if (addTranFiles.substring(0, 1) == "Q") sys.exit(0)
    //      if (addTranFiles.substring(0, 1) == "Y") {
    //        fileAddTrans = scala.io.StdIn.readLine().trim
    //      }
    //    }
    //*****************************************************************************************
    // Prompt about adjustment file
    //
    // Although I coded this section, I have decided not to use it, as there really isn't much
    // that this program could add to the process.  The records will already be in the
    // journal entry format.  So this code is probably unneeded.
    //*****************************************************************************************

    //    println("Adjustments:  Would you like to process your added adjustments? (y/n)")
    //    var fileAddAdj: String = "AddAdj.txt"
    //    val procAdjs = scala.io.StdIn.readLine().trim.toUpperCase()
    //    if (procAdjs.substring(0, 1) == "Q") sys.exit(0)
    //    if (procAdjs.substring(0, 1) == "Y") {
    //      addFileCnt += 1
    //      println("The default file name for these transaction is Adjustments.txt")
    //      println("Would you like to change this? (y/n)")
    //      val AddAdjFiles = scala.io.StdIn.readLine().trim.toUpperCase()
    //      if (AddAdjFiles.substring(0, 1) == "Q") sys.exit(0)
    //      if (AddAdjFiles.substring(0, 1) == "Y") {
    //        fileAddAdj = scala.io.StdIn.readLine().trim
    //      }
    //    }


    //*****************************************************************************************
    // File name construction
    // this set of collections constructs the file names for the VA data
    // This code was deemed too complex to finish; opted for a simple table of file names.
    //*****************************************************************************************

    //    val qtr = Array("1", "2", "3", "4")
    //    val yrs = Array("03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16")
    //    val fileType = Array("exp", "rev")
    //    val fileNameArray = Array.ofDim[String](
    //      yrs.length * 5 + addFileCnt,           // File Name
    //      yrs.length * 5 + addFileCnt,           // File Type (EA, EB, EC, or RA, RC)
    //      yrs.length * 5 + addFileCnt,           // Fiscal Period (Quarter)
    //      yrs.length * 5 + addFileCnt            // Accounting and Transaction Date (CCYY/MM/01)
    //    )
    //
    //    for (fileCnt <- 0 to (yrs.length * 5)) {
    //      for (yrCnt <- 0 to yrs.length) {
    //        val (quotient, remainder) = fileCnt /% 5
    //        if (remainder != 0) {
    //          // create all quarterly expense entries
    //          fileNameArray(fileCnt)(0) = Array("FY" + yrs(yrCnt) + fileType(1) + ".txt")
    //          // The following adds another dimension to the table for the format of the records to be read
    //          yrCnt match {
    //            // assign years 03 - 9 to Expense type A record structure
    //            case 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 => fileNameArray(fileCnt)(1) = "EA"
    //            // assign years 10 - 11 to Expense type B record structure
    //            case 10 | 11 => fileNameArray(fileCnt)(1) = "EB"
    //            // assign years 12 - 16 to Expense type C record structure
    //            case 12 | 13 | 14 => fileNameArray(fileCnt)(1) = "EC"
    //          }
    //          fileNameArray(fileCnt)(2) = qtr
    //          fileNameArray(fileCnt)(3) = "20" + yrs(yrCnt) + {qtr match {case "1" }}
    //        }
    //        else {
    //          // create revenue entry every 5th entry
    //          for (qtrCnt <- 0 to qtr.length) {
    //            fileNameArray(fileCnt)(0) = "FY" + yrs(yrCnt) + "q" + qtr(qtrCnt) + fileType(0) + ".txt"
    //          }
    //          yrCnt match {
    //            // assign years 03 - 11 to Revenue type A record structure
    //            case 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 => fileNameArray(fileCnt)(1) = "RA"
    //            // assign years 12 - 16 to Revenue type C record structure
    //            case 12 | 13 | 14 | 15 | 16 => fileNameArray(fileCnt)(1) = "RC"
    //          }
    //        }
    //      }
    //    }
    //    // Add Additional Transaction and Adjustment Files to list if Requested above.
    //    var subscript: Int = yrs.length * 5
    //    if (procTrans.substring(0, 1) == "Y") {
    //      fileNameArray(subscript + 1)(0) = fileAddTrans
    //      fileNameArray(subscript + 1)(1) = "TA"
    //    }
    //    if (procAdjs.substring(0, 1) == "Y") {
    //      fileNameArray(subscript + 1)(0) = fileAddAdj
    //      fileNameArray(subscript + 1)(1) = "AA"
    //    }
    //

    //*****************************************************************************************
    // This code can be used to read text files in a Scala Terminal (paste in code), and it will print the line
    // number of the data having the problem.  The problem may be a few rows lower if it is a buffer problem
    //*****************************************************************************************
//
//    import scala.io.Source
//    val instX = Source.fromFile("FY09q1exp.txt").getLines()
//    val instIDLine = instX.buffered
//    var instRead = 0
//
//    def readInst(): Unit = {
//      val e: Array[String] = instIDLine.next.split(',')
//      // if (debugPrint == "Y") println("inst vendor: " + instIDRec.instIDVendorName + " inst row: " + instIDRec)
//      instRead +=1
//      println("line count: " + instRead)
//    }
//    var instEOF = "N"
//
//    while (instEOF == "N") {
//      if (instIDLine.isEmpty) {
//        // if (debugPrint == "Y") println("inst high values. EOF Inst")
//        instEOF = "Y"
//      }
//      else readInst()
//    }
//
//  //*****************************************************************************************
    // These characters were located in the later data files from VA, using the above code.
    // //list was kept to be able to search
    // for new instances of them in later files.
    //*****************************************************************************************
    //+¨
    //«÷
    //«Ù
    //-·
    //†
    //¨
    //¶+
    //*****************************************************************************************

  }
  def tranHeader(outJournals: PrintWriter): Unit = {
    outJournals.write(
      "transIPID," +
        "transContractID," +
        "transCommitmentID," +
        "transJrnlID," +
        "transJrnlLineID," +
        "transBusinessEventCode," +
        "transSourceSystemID," +
        "transOriginalDocID," +
        "transJrnlDescript," +
        "transLedgerID,"  +
        "transJrnlType,"  +
        "transBookCodeID," +
        "transLegalEntityID," +
        "transCenterID," +
        "transProjectID," +
        "transProductID," +
        "transNominalAccountID," +
        "transAltAccountID,"  +
        "transCurrencyCodeSourceID,"  +
        "transCurrencyTypeCodeSourceID," +
        "transCurrencyCodeTargetID," +
        "transCurrencyTypeCodeTargetID," +
        "transFiscalPeriod," +
        "transAcctDate," +
        "transTransDate," +
        "transTransAmount,"  +
        "transUnitOfMeasure," +
        "transUnitPrice," +
        "transStatisticAmount," +
        "tranRuleSetID," +
        "transRuleID," +
        "transDirVsOffsetFlg," +
        "transReconcileFlg," +
        "transAdjustFlg," +
        "transExtensionIDAuditTrail," +
        "transExtensionIDSource,"  +
        "transExtensionIDClass," +
        "transExtensionIDDates,"  +
        "transExtensionIDCustom"
        + "\n")
  }

}
