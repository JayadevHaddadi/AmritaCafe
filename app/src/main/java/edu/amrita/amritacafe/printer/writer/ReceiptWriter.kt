package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration

class ReceiptWriter(private val orders: List<Order>, private val configuration: Configuration) {

    companion object : Writer {
        override fun writeToPrinter(
            orders: List<Order>,
            printer: Printer,
            configuration: Configuration
        ) {
            ReceiptWriter(orders, configuration).writeToPrinter(printer)
        }
    }

    private fun writeToPrinter(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig

        orders.forEach { (orderNumber, orderItems, time) ->
            val myOrderItems = orderItems.map {
                listOf(it)
            }.flatten()
            val orderTotalText = orderItems.map { it.finalPrice }.sum().toString()

            val itemCount = orderItems.map { 1 }.sum()

            printer.addTextSize(titleSize, titleSize)
            //total length of this size is 16
            printer.addText("$orderNumber${time.padStart(16 - orderNumber.toString().length, ' ')}")

            printer.addFeedLine(lineFeed)
            printer.addTextSize(textSize, textSize)
            printer.addFeedLine(lineFeed)
            printer.addText(orderItemsText(myOrderItems))
            printer.addFeedLine(lineFeed)
            printer.addTextStyle(
                Printer.PARAM_DEFAULT,
                Printer.PARAM_DEFAULT,
                Printer.TRUE,
                Printer.PARAM_DEFAULT
            )

            printer.addFeedLine(lineFeed)
            printer.addText("TOTAL" + orderTotalText.padStart(15))
            printer.addFeedLine(lineFeed)
            printer.addFeedLine((4 - itemCount).let { if (it < 0) 0 else it })
            printer.addFeedLine(lineFeed)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    private fun orderItemsText(orderItems: List<RegularOrderItem>) =
        orderItems.joinToString("\n") {
            "${it.quantity} ${it.code}".padEnd(17) +
                    it.finalPrice.toString().padStart(3) +
                    if (it.comment.isBlank()) ""
                    else "\n * ${it.comment}"
        }
}