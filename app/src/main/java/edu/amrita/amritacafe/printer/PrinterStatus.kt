package edu.amrita.amritacafe.printer

enum class PrinterStatus(val message: String) {
    Ok("Printer OK."),
    Unknown("No status was given."),
    NoConnection("Check power and communication to printer."),
    Offline("Printer offline. Check cover/paper/etc."),
    StatusUnknown("Can't establish connection."),
    CoverOpen("Cover is open"),
    PaperNearEnd("Printer is short on paper. Please replace paper soon."),
    PaperIsOut("Printer out of paper. Please replace paper."),
    PaperFeed("Paper is being fed manually."),
    PanelSwitch("A panel switch is being used"),
    RecoverableError("Check printer and try again"),
    UnrecoverableError("Check and reboot printer.");

    companion object {
        fun fromPrinterStatusInfo(printerStatusInfo: Any? = null): List<PrinterStatus> =
            listOf(Ok)
    }
}