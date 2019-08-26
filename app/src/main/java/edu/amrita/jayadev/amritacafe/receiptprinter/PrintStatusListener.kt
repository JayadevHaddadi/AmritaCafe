package edu.amrita.jayadev.amritacafe.receiptprinter

import com.epson.epos2.Epos2Exception

import java.util.EventListener

interface PrintStatusListener : EventListener {
    fun printComplete(status: PrintDispatchResponse)
    fun notifyPrinterStatus(status: List<PrinterStatus>)
    fun error(errorStatus: ErrorStatus, exception: Epos2Exception)
    fun busy()
}
