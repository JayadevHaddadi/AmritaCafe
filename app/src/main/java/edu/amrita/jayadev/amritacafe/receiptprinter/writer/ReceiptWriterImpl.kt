package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem

class ReceiptWriterImpl(val orderNumber: Int, val orderItems: List<OrderItem>) {
    private val TITLE_SIZE = 3
    private val TEXT_SIZE = 2
    private val LINE_FEED = 1

    companion object : ReceiptWriter {
        override fun writeToPrinter(orderNumber: Int, orderItems: List<OrderItem>, printer: Printer) {
            ReceiptWriterImpl(orderNumber, orderItems).writeToPrinter(printer)
        }
    }

    private fun writeToPrinter(printer: Printer) {
        printer.addTextSize(TITLE_SIZE, TITLE_SIZE)
        printer.addText("ORDER: $orderNumber")

        printer.addTextSize(TEXT_SIZE, TEXT_SIZE)
        printer.addFeedLine(LINE_FEED)
        printer.addText(orderItemsText)
        printer.addFeedLine(LINE_FEED)
        printer.addText("TOTAL" + orderTotalText.padStart(15))
        printer.addFeedLine(LINE_FEED)
        printer.addCut(Printer.CUT_FEED)
    }

    private val orderItemsText get() =
        orderItems.map {
            "${it.quantity} ${it.name}".padEnd(17) +
                    it.totalPrice.toString().padStart(3) +
                    if (it.comment.isBlank()) ""
                    else "\n * ${it.comment}"
        }.joinToString("\n")

    private val orderTotalText get() = orderItems.map { it.totalPrice }.sum().toString()
}