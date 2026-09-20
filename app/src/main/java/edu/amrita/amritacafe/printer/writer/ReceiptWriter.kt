package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.activities.capitalizeWords
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration

import edu.amrita.amritacafe.printer.escpos.EscPosBuilder

class ReceiptWriter(private val orders: List<Order>, private val configuration: Configuration) {

    companion object : Writer {
        override fun writeToPrinter(
            orders: List<Order>,
            printer: Printer,
            configuration: Configuration
        ) {
            ReceiptWriter(orders, configuration).writeToPrinter(printer)
        }

        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration
        ): ByteArray {
            return ReceiptWriter(orders, configuration).writeToEscPos(42)
        }

        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration,
            columns: Int
        ): ByteArray {
            return ReceiptWriter(orders, configuration).writeToEscPos(columns)
        }

        fun orderItemsText(orderItems: List<RegularOrderItem>, totalColumns: Int = 32) =
            orderItems.joinToString("\n") {
                val priceStr = it.priceWithoutExtras.toString()
                val nameWidth = (totalColumns - priceStr.length).coerceAtLeast(10)
                "${it.quantity} ${it.code.capitalizeWords()}".padEnd(nameWidth, '.') +
                        priceStr +
                        if (it.comment.isBlank()) ""
                        else {
                            "\n * ${it.comment}"
                        } +
                        if (it.toppings.isNotEmpty()) {
                            "\n" + it.toppings.joinToString("\n") { topp ->
                                val toppPriceStr = topp.priceWithoutExtras.toString()
                                val toppNameWidth = (totalColumns - toppPriceStr.length).coerceAtLeast(10)
                                "${topp.quantity} ${topp.code}".padEnd(toppNameWidth, '.') +
                                        toppPriceStr +
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
            val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()

            val itemCount = orderItems.map { 1 }.sum()

            printer.addTextSize(titleSize, titleSize)
            val orderNumStr = orderNumber.toString().padStart(3, '0')
            printer.addText("$orderNumStr        $timeInHours")

            printer.addFeedLine(lineFeed)
            printer.addTextSize(textSize, textSize)
            printer.addFeedLine(lineFeed)
            printer.addText(orderItemsText(orderItems, 32))
            printer.addFeedLine(lineFeed)
            printer.addTextStyle(
                Printer.PARAM_DEFAULT,
                Printer.PARAM_DEFAULT,
                Printer.TRUE,
                Printer.PARAM_DEFAULT
            )

            printer.addFeedLine(lineFeed)
            printer.addText("TOTAL" + orderTotalText.padStart(15))
            if (configuration.printAmmaQuote) {
                val quoteLines = edu.amrita.amritacafe.quotes.AmmaQuotes.getFormattedLines(orderNumber, 36)
                printer.addFeedLine(1)
                printer.addTextAlign(Printer.ALIGN_CENTER)
                printer.addTextSize(1, 1)
                quoteLines.forEach { line ->
                    printer.addText(line + "\n")
                }
            }
            printer.addFeedLine(lineFeed)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    fun writeToEscPos(columns: Int = 42): ByteArray {
        val totalCols = if (columns > 0) columns else 42
        val doubleWidthCols = totalCols / 2
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        val builder = EscPosBuilder()

        orders.forEach { (orderNumber, orderItems, date, timeInHours) ->
            val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()
            val itemCount = orderItems.map { 1 }.sum()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            builder.alignLeft()
            val titleScale = if (titleSize > 1) 2 else 1
            builder.textSize(titleScale, titleScale)
            builder.bold(true)
            val headerPad = (doubleWidthCols - timeInHours.length).coerceAtLeast(orderNumStr.length)
            val headerText = orderNumStr.padEnd(headerPad) + timeInHours
            builder.line(headerText)
            builder.bold(false)

            builder.feedLines(lineFeed)
            builder.textSize(1, 1)
            builder.horizontalLine('-', totalCols)

            builder.line(orderItemsText(orderItems, totalCols))

            builder.horizontalLine('-', totalCols)

            builder.bold(true)
            builder.textSize(2, 2)
            val totalPrefix = "TOTAL"
            val dotCount = (doubleWidthCols - totalPrefix.length - orderTotalText.length).coerceAtLeast(1)
            val totalLine = totalPrefix + ".".repeat(dotCount) + orderTotalText
            builder.line(totalLine)
            builder.bold(false)
            builder.textSize(1, 1)

            if (configuration.printAmmaQuote) {
                val quoteChars = if (totalCols >= 42) 36 else 24
                val quoteLines = edu.amrita.amritacafe.quotes.AmmaQuotes.getFormattedLines(orderNumber, quoteChars)
                builder.feedLines(1)
                builder.alignCenter()
                quoteLines.forEach { line ->
                    builder.line(line)
                }
                builder.alignLeft()
            }

            builder.cut(1)
        }
        return builder.build()
    }
}