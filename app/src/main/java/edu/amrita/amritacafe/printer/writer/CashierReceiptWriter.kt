package edu.amrita.amritacafe.printer.writer

import android.os.Build
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.escpos.EscPosBuilder
import edu.amrita.amritacafe.quotes.AmmaQuotes
import edu.amrita.amritacafe.settings.Configuration
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class CashierReceiptWriter(
    private val orders: List<Order>,
    private val configuration: Configuration
) {

    companion object : Writer {
        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration
        ): ByteArray {
            return CashierReceiptWriter(orders, configuration).writeToEscPos(42)
        }

        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration,
            columns: Int
        ): ByteArray {
            return CashierReceiptWriter(orders, configuration).writeToEscPos(columns)
        }
    }

    private fun getCurrentTime(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            SimpleDateFormat("HH:mm", Locale.US).format(Date())
        }
    }

    private fun getCurrentDate(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } else {
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        }
    }

    fun writeToEscPos(columns: Int = 42): ByteArray {
        val builder = EscPosBuilder()
        val totalCols = if (columns > 0) columns else 42

        orders.forEach { (orderNumber, orderItems, _, _) ->
            val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()
            val time = getCurrentTime()
            val date = getCurrentDate()

            if (configuration.receiptMarginFeedBefore > 0) {
                builder.feedLines(configuration.receiptMarginFeedBefore)
            }

            // Header (Centered)
            builder.alignCenter()
            val headerScale = configuration.receiptLargeTextScale
            builder.textSize(headerScale, headerScale)
            builder.bold(true)
            builder.line("Western Cafe")
            builder.textSize(1, 1)
            builder.bold(false)
            builder.feedLines(1)

            // Address (Left) with Time & Date (Right) on 2 lines
            builder.alignLeft()
            val line1Left = "Sree Bhadra Amrita"
            val pad1 = (totalCols - time.length).coerceAtLeast(line1Left.length)
            builder.line(line1Left.padEnd(pad1) + time)

            val line2Left = if (totalCols >= 42) "Amritapuri, Kollam-690546" else "Amritapuri, Kollam"
            val pad2 = (totalCols - date.length).coerceAtLeast(line2Left.length)
            builder.line(line2Left.padEnd(pad2) + date)

            // Divider
            builder.horizontalLine('-', totalCols)
            builder.feedLines(1)

            // Items
            val smallScale = configuration.receiptSmallTextScale
            builder.textSize(smallScale, smallScale)
            val effectiveCols = totalCols / smallScale
            builder.line(ReceiptWriter.orderItemsText(orderItems, effectiveCols))
            builder.feedLines(1)

            // Total line
            builder.bold(true)
            val totalScale = configuration.receiptLargeTextScale
            val effectiveTotalCols = totalCols / totalScale
            builder.textSize(totalScale, totalScale)
            val totalPrefix = "Total"
            val dotCount = (effectiveTotalCols - totalPrefix.length - orderTotalText.length).coerceAtLeast(1)
            builder.line(totalPrefix + ".".repeat(dotCount) + orderTotalText)
            builder.bold(false)
            builder.textSize(1, 1)

            // Optional Amma Quote
            if (configuration.printAmmaQuote) {
                val quoteChars = if (totalCols >= 42) 36 else 24
                val quoteLines = AmmaQuotes.getFormattedLines(orderNumber, quoteChars)
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
