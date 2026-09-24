package edu.amrita.amritacafe.printer.writer

import edu.amrita.amritacafe.activities.capitalizeWords
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration
import edu.amrita.amritacafe.printer.escpos.EscPosBuilder

class ReceiptWriter(private val orders: List<Order>, private val configuration: Configuration) {

    companion object : Writer {
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

    fun writeToEscPos(columns: Int = 42): ByteArray {
        val totalCols = if (columns > 0) columns else 42
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        val builder = EscPosBuilder()

        orders.forEach { (orderNumber, orderItems, date, timeInHours) ->
            val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            if (configuration.receiptMarginFeedBefore > 0) {
                builder.feedLines(configuration.receiptMarginFeedBefore)
            }

            builder.alignLeft()
            val titleScale = configuration.receiptLargeTextScale
            val effectiveHeaderCols = totalCols / titleScale
            builder.textSize(titleScale, titleScale)
            builder.bold(true)
            val headerPad = (effectiveHeaderCols - timeInHours.length).coerceAtLeast(orderNumStr.length)
            val headerText = orderNumStr.padEnd(headerPad) + timeInHours
            builder.line(headerText)
            builder.bold(false)

            builder.feedLines(lineFeed)
            builder.textSize(1, 1)
            builder.horizontalLine('-', totalCols)

            val smallScale = configuration.receiptSmallTextScale
            builder.textSize(smallScale, smallScale)
            val effectiveCols = totalCols / smallScale
            builder.line(orderItemsText(orderItems, effectiveCols))

            builder.textSize(1, 1)
            builder.horizontalLine('-', totalCols)

            builder.bold(true)
            val totalScale = configuration.receiptLargeTextScale
            val effectiveTotalCols = totalCols / totalScale
            builder.textSize(totalScale, totalScale)
            val totalPrefix = "TOTAL"
            val dotCount = (effectiveTotalCols - totalPrefix.length - orderTotalText.length).coerceAtLeast(1)
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

            val feedAfter = configuration.receiptMarginFeedAfter.coerceAtLeast(1)
            builder.cut(feedAfter)
        }
        return builder.build()
    }
}