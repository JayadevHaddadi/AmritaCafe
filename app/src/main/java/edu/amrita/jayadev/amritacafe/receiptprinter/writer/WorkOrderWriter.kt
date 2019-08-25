package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem

class WorkOrderWriter(orderNumber: Int, orderItems: List<OrderItem>) {

    private val orderItems = orderItems.groupBy { it.menuItem.location }.toSortedMap()
    private val startingLocation = orderItems.first().menuItem.location
    private val orderNumber = orderNumber.toString().padStart(3, '0')

    private val orderItemsText get() =
        orderItems.map { (_, orderItems) ->
            orderItems.mapIndexed(::writeLine)
        }.flatten().joinToString("\n")

    private fun writeLine(index: Int, orderItem: OrderItem) =
        ( if (index == 0) orderItem.menuItem.location.name else "" ).padEnd(10) +
                orderItem.menuItem.code

    private fun writeTo(printer: Printer) {
        printer.addTextSize(TITLE_SIZE, TITLE_SIZE)
        printer.addText("ORDER: $orderNumber     ${startingLocation.code}")

        printer.addTextSize(TEXT_SIZE, TEXT_SIZE)
        printer.addFeedLine(LINE_FEED)
        printer.addText(orderItemsText)
        printer.addFeedLine(LINE_FEED)
        printer.addCut(Printer.CUT_FEED)
    }

    companion object : ReceiptWriter  {
        override fun writeToPrinter(orderNumber: Int, orderItems: List<OrderItem>, printer: Printer) {
            WorkOrderWriter(orderNumber, orderItems).writeTo(printer)
        }

        private const val TITLE_SIZE = 3
        private const val TEXT_SIZE = 2
        private const val LINE_FEED = 1
    }
}