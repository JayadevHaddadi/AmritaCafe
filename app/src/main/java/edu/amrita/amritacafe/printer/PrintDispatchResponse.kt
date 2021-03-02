package edu.amrita.amritacafe.printer

import com.epson.epos2.printer.PrinterStatusInfo

open class PrintDispatchResponse(val printerStatus: List<PrinterStatus>) {
    companion object {
        fun fromPrinterCallback(code: Int, status: PrinterStatusInfo?) : PrintDispatchResponse {
            return if (code == CompletedJobStatus.Success.code) {
                PrintSuccess(PrinterStatus.fromPrinterStatusInfo(status))
            } else {
                PrintFailed(
                    CompletedJobStatus.fromCode(code),
                    PrinterStatus.fromPrinterStatusInfo(status))
            }
        }
    }
}

class PrintSuccess(printerStatus: List<PrinterStatus>) :
    PrintDispatchResponse(printerStatus)

class PrintFailed(val status: CompletedJobStatus, printerStatus: List<PrinterStatus>) :
    PrintDispatchResponse(printerStatus)