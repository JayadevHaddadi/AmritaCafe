package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem

class WorkOrderWriter(vararg val orders: Order) {

    private fun writeLine(index: Int, orderItem: OrderItem) =
        ( if (index == 0) orderItem.menuItem.location.name else "" ).padEnd(10) +
                orderItem.menuItem.code

    private fun writeTo(printer: Printer) {
        orders.forEach { (orderNumber, itemList) ->
            val itemsMap = itemList.groupBy { it.menuItem.location }.toSortedMap()
            val startingLocation = itemsMap.firstKey()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            val orderItemsText = itemsMap.map { (_, orderItems) ->
                orderItems.mapIndexed(::writeLine)
            }.flatten().joinToString("\n")

            printer.addTextSize(TITLE_SIZE, TITLE_SIZE)
            printer.addText("ORDER: $orderNumStr     ${startingLocation.code}")

            printer.addTextSize(TEXT_SIZE, TEXT_SIZE)
            printer.addFeedLine(LINE_FEED)
            printer.addText(orderItemsText)
            printer.addFeedLine(LINE_FEED)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    companion object : ReceiptWriter  {
        override fun writeToPrinter(vararg orders: Order, printer: Printer) {
            WorkOrderWriter(*orders).writeTo(printer)
        }

        private const val TITLE_SIZE = 1
        private const val TEXT_SIZE = 1
        private const val LINE_FEED = 0
    }
}