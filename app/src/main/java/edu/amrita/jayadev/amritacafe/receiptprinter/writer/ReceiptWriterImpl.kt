package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem
import edu.amrita.jayadev.amritacafe.settings.Configuration

class ReceiptWriterImpl(private val orders: List<Order>, private val configuration: Configuration) {


    companion object : ReceiptWriter {
        override fun writeToPrinter(orders: List<Order>, printer: Printer, configuration: Configuration) {
            ReceiptWriterImpl(orders, configuration).writeToPrinter(printer)
        }
    }

    private fun writeToPrinter(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig

        orders.forEach { (orderNumber, orderItems) ->
            val orderTotalText = orderItems.map { it.totalPrice }.sum().toString()

            printer.addTextSize(titleSize, titleSize)
            printer.addText("ORDER  $orderNumber")

            printer.addTextSize(textSize, textSize)
            printer.addFeedLine(lineFeed)
            printer.addText(orderItemsText(orderItems))
            printer.addFeedLine(lineFeed)
            printer.addTextStyle(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.TRUE, Printer.PARAM_DEFAULT)
            printer.addText("TOTAL" + orderTotalText.padStart(15))
            printer.addFeedLine(lineFeed)
            printer.addFeedLine(lineFeed)
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