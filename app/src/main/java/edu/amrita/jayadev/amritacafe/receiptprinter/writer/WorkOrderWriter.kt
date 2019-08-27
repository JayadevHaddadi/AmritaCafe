package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem
import edu.amrita.jayadev.amritacafe.settings.Configuration

class WorkOrderWriter(val orders: List<Order>, private val configuration: Configuration) {

    init {
        if (orders.isEmpty())
            throw IllegalArgumentException("An order needs at least one order item.")
    }

    private fun writeLine(index: Int, orderItem: OrderItem) =
        ( if (index == 0) orderItem.menuItem.location.code else "" ).padEnd(5) +
                "${orderItem.quantity}x".padStart(3).padEnd(4) +
                orderItem.menuItem.code + if (orderItem.comment.isNotBlank()) {
            "\n" + "* ${orderItem.comment}".padEnd(12).padStart(20)
        } else ""

    private fun writeTo(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        orders.forEach { (orderNumber, itemList) ->
            val itemsMap = itemList.groupBy { it.menuItem.location }.toSortedMap()
            val startingLocation = itemsMap.firstKey()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            val orderItemsText = itemsMap.map { (_, orderItems) ->
                orderItems.mapIndexed(::writeLine)
            }.flatten().joinToString("\n")

            printer.addTextSize(titleSize, titleSize)
            printer.addText("ORDER  $orderNumStr     ${startingLocation.code}")
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