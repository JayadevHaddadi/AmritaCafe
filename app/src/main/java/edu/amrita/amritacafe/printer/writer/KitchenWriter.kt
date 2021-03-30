package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration

class KitchenWriter(private val orders: List<Order>, private val configuration: Configuration) {

    init {
        require(orders.isNotEmpty()) { "An order needs at least one order item." }
    }

    private fun writeLine(orderItem: RegularOrderItem) =
        if (orderItem.quantity == 1) {
            "  "
        } else {
            orderItem.quantity.toString().padEnd(2)
        } + orderItem.code +
                if (orderItem.comment.isBlank()) {
                    ""
                } else {
                    "\n  * ${orderItem.comment}"
                } +

                if (orderItem.toppings.isNotEmpty()) {
                    "\n" + orderItem.toppings.joinToString("\n") {
                        if (it.quantity == 1) {
                            "  "
                        } else {
                            it.quantity.toString().padEnd(2)
                        }  + it.menuItem.code //+ "+ "
                    }
                } else {
                    ""
                }


    private fun writeTo(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        orders.forEach { (orderNumber, itemList, time) ->

            val orderItemsText =
                itemList.map (::writeLine).joinToString("\n")
            val itemCount =
                itemList.map { 1 }.sum()

            printer.addTextSize(titleSize, titleSize)
            //total length of this size is 16
//            printer.addText("$orderNumber${time.padStart(16 - orderNumber.toString().length, ' ')}")
            val orderNumStr = orderNumber.toString().padStart(3, '0')
            printer.addText("$orderNumStr        $time")

            printer.addFeedLine(lineFeed)
            printer.addHLine(1, 2400, Printer.LINE_THICK_DOUBLE)

            printer.addFeedLine(lineFeed)

            printer.addTextSize(textSize, textSize)
            printer.addText(orderItemsText)
            printer.addFeedLine(lineFeed)
            printer.addFeedLine(lineFeed)
            printer.addFeedLine((6 - itemCount).let { if (it < 0) 0 else it })
            printer.addCut(Printer.CUT_FEED)
        }
    }

    private fun printItem(orderItem: RegularOrderItem): String {
        val toppingsString = StringBuffer()
        orderItem.toppings.forEach {
            toppingsString.append("\n  + " + it.quantity + " " + it.code)
        }

        return if (orderItem.quantity == 1) {
            "  "
        } else {
            orderItem.quantity.toString().padEnd(2)
        } + orderItem.code +
                if (orderItem.comment.isBlank()) {
                    ""
                } else {
                    "\n  * ${orderItem.comment}"
                } + toppingsString.toString()
    }

    companion object : Writer {

        override fun writeToPrinter(
            orders: List<Order>,
            printer: Printer,
            configuration: Configuration
        ) {
            KitchenWriter(orders, configuration).writeTo(printer)
        }
    }
}