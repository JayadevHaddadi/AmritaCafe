package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.activities.capitalizeWords
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

        fun orderItemsText(orderItems: List<RegularOrderItem>) =
            orderItems.joinToString("\n") {
                "${it.quantity} ${it.code.capitalizeWords()}".padEnd(17, '.') +//28
                        it.priceWithoutExtras.toString().padStart(4, '.') +
                        if (it.comment.isBlank()) ""
                        else {
                            "\n * ${it.comment}"
                        } +
                        if (it.toppings.isNotEmpty()) {
                            "\n" + it.toppings.joinToString("\n") { topp ->
                                "${topp.quantity} ${topp.code}".padEnd(17) +
                                        topp.priceWithoutExtras.toString().padStart(3) +
                                        if (topp.comment.isBlank()) ""
                                        else {
                                            "\n * ${topp.comment}"
                                        }
                            }
                        } else {
                            ""
                        }
            }
    }


    private fun writeToPrinter(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig

        orders.forEach { (orderNumber, orderItems, date, timeInHours) ->
            val orderTotalText = orderItems.map { it.totalPrice }.sum().toString()

            val itemCount = orderItems.map { 1 }.sum()

            printer.addTextSize(titleSize, titleSize)
            val orderNumStr = orderNumber.toString().padStart(3, '0')
            printer.addText("$orderNumStr        $timeInHours")

            printer.addFeedLine(lineFeed)
            printer.addTextSize(textSize, textSize)
            printer.addFeedLine(lineFeed)
            printer.addText(orderItemsText(orderItems))
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
}