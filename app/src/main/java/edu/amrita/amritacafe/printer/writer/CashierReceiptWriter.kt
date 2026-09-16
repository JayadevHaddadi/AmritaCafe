package edu.amrita.amritacafe.printer.writer

import android.os.Build
import com.epson.epos2.printer.Printer
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
        override fun writeToPrinter(
            orders: List<Order>,
            printer: Printer,
            configuration: Configuration
        ) {
            CashierReceiptWriter(orders, configuration).writeToPrinter(printer)
        }

        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration
        ): ByteArray {
            return CashierReceiptWriter(orders, configuration).writeToEscPos()
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

    private fun writeToPrinter(printer: Printer) {
        val totalCols = 42
        orders.forEach { (orderNumber, orderItems, _, _) ->
            val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()
            val time = getCurrentTime()
            val date = getCurrentDate()

            // Header (Centered)
            printer.addTextAlign(Printer.ALIGN_CENTER)
            printer.addTextSize(2, 2)
            printer.addTextStyle(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.TRUE, Printer.PARAM_DEFAULT)
            printer.addText("Western Cafe\n\n")

            // Address (Left) with Time & Date (Right) on 2 lines
            printer.addTextAlign(Printer.ALIGN_LEFT)
            printer.addTextSize(1, 1)
            printer.addTextStyle(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.FALSE, Printer.PARAM_DEFAULT)

            val line1Left = "Sree Bhadra Amrita"
            val pad1 = (totalCols - time.length).coerceAtLeast(line1Left.length)
            printer.addText(line1Left.padEnd(pad1) + time + "\n")

            val line2Left = "Amritapuri, Kollam-690546"
            val pad2 = (totalCols - date.length).coerceAtLeast(line2Left.length)
            printer.addText(line2Left.padEnd(pad2) + date + "\n")

            // Divider
            printer.addHLine(1, 2400, Printer.LINE_THIN)
            printer.addFeedLine(1)

            // Items
            printer.addText(ReceiptWriter.orderItemsText(orderItems, totalCols) + "\n\n")

            // Total (Bold, double size)
            printer.addTextSize(2, 2)
            printer.addTextStyle(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT, Printer.TRUE, Printer.PARAM_DEFAULT)
            val totalPrefix = "Total"
            val dotCount = (21 - totalPrefix.length - orderTotalText.length).coerceAtLeast(1)
            printer.addText(totalPrefix + ".".repeat(dotCount) + orderTotalText + "\n")

            // Optional Amma Quote
            if (configuration.printAmmaQuote) {
                val quoteLines = AmmaQuotes.getFormattedLines(orderNumber, 36)
                printer.addFeedLine(1)
                printer.addTextAlign(Printer.ALIGN_CENTER)
                printer.addTextSize(1, 1)
                quoteLines.forEach { line ->
                    printer.addText(line + "\n")
                }
            }

            printer.addFeedLine(1)
            printer.addCut(Printer.CUT_FEED)
        }
    }

    private fun writeToEscPos(): ByteArray {
        val builder = EscPosBuilder()
        val totalCols = 42

        orders.forEach { (orderNumber, orderItems, _, _) ->
            val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()
            val time = getCurrentTime()
            val date = getCurrentDate()

            // Header (Centered)
            builder.alignCenter()
            builder.textSize(2, 2)
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

            val line2Left = "Amritapuri, Kollam-690546"
            val pad2 = (totalCols - date.length).coerceAtLeast(line2Left.length)
            builder.line(line2Left.padEnd(pad2) + date)

            // Divider
            builder.horizontalLine('-', totalCols)
            builder.feedLines(1)

            // Items
            builder.line(ReceiptWriter.orderItemsText(orderItems, totalCols))
            builder.feedLines(1)

            // Total line (Bold, double size)
            builder.bold(true)
            builder.textSize(2, 2)
            val totalPrefix = "Total"
            val dotCount = (21 - totalPrefix.length - orderTotalText.length).coerceAtLeast(1)
            builder.line(totalPrefix + ".".repeat(dotCount) + orderTotalText)
            builder.bold(false)
            builder.textSize(1, 1)

            // Optional Amma Quote
            if (configuration.printAmmaQuote) {
                val quoteLines = AmmaQuotes.getFormattedLines(orderNumber, 36)
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
