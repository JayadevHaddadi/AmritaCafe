package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem

class ReceiptWriterImpl(private vararg val orders: Order) {

    companion object : ReceiptWriter {
        override fun writeToPrinter(vararg orders: Order, printer: Printer) {
            ReceiptWriterImpl(*orders).writeToPrinter(printer)
        }
        private const val TITLE_SIZE = 3
        private const val TEXT_SIZE = 2
        private const val LINE_FEED = 1
    }

    private fun writeToPrinter(printer: Printer) {
        orders.forEach { (orderNumber, orderItems) ->
            val orderTotalText = orderItems.map { it.totalPrice }.sum().toString()

            printer.addTextSize(TITLE_SIZE, TITLE_SIZE)
            printer.addText("ORDER: $orderNumber")

            printer.addTextSize(TEXT_SIZE, TEXT_SIZE)
            printer.addFeedLine(LINE_FEED)
            printer.addText(orderItemsText(orderItems))
            printer.addFeedLine(LINE_FEED)
            printer.addText("TOTAL" + orderTotalText.padStart(15))
            printer.addFeedLine(LINE_FEED)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    private fun orderItemsText(orderItems : List<OrderItem>) =
        orderItems.joinToString("\n") {
            "${it.quantity} ${it.menuItem.code}".padEnd(17) +
                    it.totalPrice.toString().padStart(3) +
                    if (it.comment.isBlank()) ""
                    else "\n * ${it.comment}"
        }
}