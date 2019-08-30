package edu.amrita.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.menu.OrderItem
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.menu.Topping
import edu.amrita.amritacafe.settings.Configuration

class ReceiptWriterImpl(private val orders: List<Order>, private val configuration: Configuration) {


    companion object : ReceiptWriter {
        override fun writeToPrinter(orders: List<Order>, printer: Printer, configuration: Configuration) {
            ReceiptWriterImpl(orders, configuration).writeToPrinter(printer)
        }
    }

    private fun writeToPrinter(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig

        orders.forEach { (orderNumber, orderItems, time) ->
            val myOrderItems = orderItems.map {
                listOf(it) +
                        if (it is RegularOrderItem) {
                            it.toppings
                        } else {
                            emptyList()
                        }
            }.flatten()
            val orderTotalText = orderItems.map { it.totalPrice }.sum().toString()

            val itemCount = orderItems.map { it as RegularOrderItem }.map { 1 + it.toppings.size }.sum()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            printer.addTextSize(titleSize, titleSize)
            printer.addText("$orderNumStr        $time")

            printer.addFeedLine(lineFeed)
            printer.addTextSize(textSize, textSize)
            printer.addFeedLine(lineFeed)
            printer.addText(orderItemsText(myOrderItems))
            printer.addFeedLine(lineFeed)
            printer.addTextStyle(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.TRUE, Printer.PARAM_DEFAULT)

            printer.addFeedLine(lineFeed)
            printer.addText("TOTAL" + orderTotalText.padStart(15))
            printer.addFeedLine(lineFeed)
            printer.addFeedLine( (4 - itemCount).let { if (it < 0) 0 else it } )
            printer.addFeedLine(lineFeed)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    private fun orderItemsText(orderItems : List<OrderItem>) =
        orderItems.joinToString("\n") {
            "${it.quantity} ${if (it is Topping) " with" else ""} ${it.menuItem.code}".padEnd(17) +
                    it.totalPrice.toString().padStart(3) +
                    if (it.comment.isBlank()) ""
                    else "\n * ${it.comment}"
        }
}