package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem
import edu.amrita.jayadev.amritacafe.settings.Configuration

class WorkOrderWriter(val orders: List<Order>, private val configuration: Configuration) {

    private fun writeLine(index: Int, orderItem: OrderItem) =
        ( if (index == 0) orderItem.menuItem.location.name else "" ).padEnd(10) +
                orderItem.menuItem.code

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
            printer.addText("ORDER: $orderNumStr     ${startingLocation.code}")

            printer.addTextSize(textSize, textSize)
            printer.addFeedLine(lineFeed)
            printer.addText(orderItemsText)
            printer.addFeedLine(lineFeed)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    companion object : ReceiptWriter  {

        override fun writeToPrinter(orders: List<Order>, printer: Printer, configuration: Configuration) {
            WorkOrderWriter(orders, configuration).writeTo(printer)
        }

        private const val TITLE_SIZE = 1
        private const val TEXT_SIZE = 1
        private const val LINE_FEED = 0
    }
}