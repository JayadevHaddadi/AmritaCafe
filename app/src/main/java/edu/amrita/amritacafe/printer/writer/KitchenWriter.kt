package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.escpos.EscPosBuilder
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
                        } + it.menuItem.code //+ "+ "
                    }
                } else {
                    ""
                }


    private fun writeTo(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        orders.forEach { (orderNumber, itemList, date, time) ->

            val orderItemsText =
                itemList.map(::writeLine).joinToString("\n")
            val itemCount =
                itemList.map { 1 }.sum()

            printer.addTextSize(titleSize, titleSize)
            val orderNumStr = orderNumber.toString().padStart(3, '0')
            printer.addText("$orderNumStr        $time")

            printer.addFeedLine(lineFeed)
            printer.addHLine(1, 2400, Printer.LINE_THICK_DOUBLE)

            printer.addFeedLine(lineFeed)

            printer.addTextSize(textSize, textSize)
            printer.addText(orderItemsText)
            printer.addFeedLine(lineFeed)
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

        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration
        ): ByteArray {
            return KitchenWriter(orders, configuration).writeToEscPos(42)
        }

        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration,
            columns: Int
        ): ByteArray {
            return KitchenWriter(orders, configuration).writeToEscPos(columns)
        }
    }

    fun writeToEscPos(columns: Int = 42): ByteArray {
        val totalCols = if (columns > 0) columns else 42
        val doubleWidthCols = totalCols / 2
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        val builder = EscPosBuilder()

        orders.forEach { (orderNumber, itemList, date, time) ->
            val orderItemsText = itemList.map(::writeLine).joinToString("\n")
            val itemCount = itemList.map { 1 }.sum()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            builder.alignLeft()
            val titleScale = if (titleSize > 1) 2 else 1
            builder.textSize(titleScale, titleScale)
            builder.bold(true)
            val headerPad = (doubleWidthCols - time.length).coerceAtLeast(orderNumStr.length)
            val headerText = orderNumStr.padEnd(headerPad) + time
            builder.line(headerText)
            builder.bold(false)

            builder.feedLines(lineFeed)
            // Crucial: reset text size to 1x1 before drawing separator line to prevent wrapping
            builder.textSize(1, 1)
            builder.horizontalLine('=', totalCols)

            // Large, bold font for kitchen staff (2x2 scale in normal mode)
            val textScale = if (configuration.testing) 1 else 2
            builder.textSize(textScale, textScale)
            builder.bold(true)
            builder.line(orderItemsText)
            builder.bold(false)
            builder.textSize(1, 1)
            builder.cut(1)
        }
        return builder.build()
    }
}