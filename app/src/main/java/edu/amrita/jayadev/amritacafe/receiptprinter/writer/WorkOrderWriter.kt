package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.menu.OrderItem
import edu.amrita.jayadev.amritacafe.menu.RegularOrderItem
import edu.amrita.jayadev.amritacafe.settings.Configuration

class WorkOrderWriter(private val orders: List<Order>, private val configuration: Configuration) {

    init {
        require(orders.isNotEmpty()) { "An order needs at least one order item." }
    }

    private fun writeLine(orderItem: RegularOrderItem) =
        if (orderItem.quantity == 1) {
            "  "
        } else {
            orderItem.quantity.toString().padEnd(2)
        } + orderItem.menuItem.code +
                if (orderItem.comment.isBlank()) {""}
                else {"\n  * ${orderItem.comment}"} +

                if (orderItem.toppings.isNotEmpty()) {
                    "\n" + orderItem.toppings.joinToString("\n") {
                        if (it.quantity == 1) {"  "} else { it.quantity.toString().padEnd(2) } + "  " + it.menuItem.code
                    }
                } else {
                    ""
                }

    private fun writeTo(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        orders.forEach { (orderNumber, itemList, time) ->
            val itemsMap = itemList.groupBy { it.menuItem.location }.toSortedMap()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            val orderItemsText = itemsMap.map { (_, orderItems) ->
                orderItems.map {it as RegularOrderItem}.map(::writeLine)
            }.flatten().joinToString("\n")

            printer.addTextSize(titleSize, titleSize)
            printer.addText("$orderNumStr         $time")
            printer.addFeedLine(lineFeed)
            printer.addHLine(1, 2400, Printer.LINE_THICK_DOUBLE)

            printer.addFeedLine(lineFeed)

            printer.addTextSize(textSize, textSize)
            printer.addText(orderItemsText)
            printer.addFeedLine(lineFeed)
            printer.addFeedLine(lineFeed)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    companion object : ReceiptWriter  {

        override fun writeToPrinter(orders: List<Order>, printer: Printer, configuration: Configuration) {
            WorkOrderWriter(orders, configuration).writeTo(printer)
        }
    }
}