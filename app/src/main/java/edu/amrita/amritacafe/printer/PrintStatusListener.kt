package edu.amrita.amritacafe.printer

import java.util.EventListener

interface PrintStatusListener : EventListener {
    fun printComplete(status: PrintDispatchResponse)
    fun notifyPrinterStatus(status: List<PrinterStatus>) {}
    fun error(errorStatus: ErrorStatus, exception: Exception = Exception()) {}
}
