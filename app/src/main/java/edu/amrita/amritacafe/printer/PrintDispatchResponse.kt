package edu.amrita.amritacafe.printer

open class PrintDispatchResponse(val printerStatus: List<PrinterStatus> = emptyList())

class PrintSuccess(printerStatus: List<PrinterStatus> = emptyList()) :
    PrintDispatchResponse(printerStatus)

class PrintFailed(val status: CompletedJobStatus, printerStatus: List<PrinterStatus> = emptyList()) :
    PrintDispatchResponse(printerStatus)