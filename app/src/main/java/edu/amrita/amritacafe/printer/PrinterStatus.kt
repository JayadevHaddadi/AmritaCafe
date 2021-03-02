package edu.amrita.amritacafe.printer

import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo

enum class PrinterStatus(val message: String, val selector: (PrinterStatusInfo) -> Boolean) {
    Ok("Printer OK.", { false }),
    Unknown("No status was given.", { false }),
    NoConnection("Check power and communication to printer.",
        { it.connection == Printer.FALSE }),
    Offline("Printer offline.  Check cover/paper/etc.",
        { it.online == Printer.FALSE }),
    StatusUnknown("Can't establish connection.",
        { it.online == Printer.UNKNOWN }),
    CoverOpen("Cover is open", { it.coverOpen == Printer.TRUE }),
    PaperNearEnd("Printer is short on paper.  Please replace paper soon.",
        { it.paper == Printer.PAPER_NEAR_END }),
    PaperIsOut("Printer out of paper.  Please replace paper.",
        { it.paper == Printer.PAPER_EMPTY} ),
    PaperFeed("Paper is being fed manually.", {it.paperFeed == Printer.TRUE}),
    PanelSwitch("A panel switch is being used", {it.panelSwitch == Printer.TRUE}),
    RecoverableError("Check printer and try again", { false && it.autoRecoverError != Printer.NO_ERR }),
    UnrecoverableError("Check and reboot printer.", {it.errorStatus != Printer.NO_ERR});

    companion object {
        fun fromPrinterStatusInfo(printerStatusInfo: PrinterStatusInfo?) =
            if (printerStatusInfo == null) {
                listOf(Unknown)
            } else {
                values().filter { it.selector(printerStatusInfo) }
                    .let { if (it.isEmpty()) listOf(Ok) else it }
            }
    }


}