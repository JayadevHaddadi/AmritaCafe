package edu.amrita.amritacafe.printer.bluetooth

import android.os.Build
import com.example.hoinprinterlib.HoinPrinter
import edu.amrita.amritacafe.activities.capitalizeWords
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.settings.Configuration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BluetoothPrinter {

}


fun bluetoothPrint(mHoinPrinter: HoinPrinter, orders: List<Order>, configuration: Configuration? = null) {
    bluetoothPrintReceipt(mHoinPrinter, orders, configuration)
}

fun bluetoothPrintReceipt(mHoinPrinter: HoinPrinter, orders: List<Order>, configuration: Configuration? = null) {
    val isCashier = configuration?.workflowMode == Configuration.MODE_CASHIER
    orders.forEach { order ->
        val (orderNumber, orderItems, _, timeInHours) = order
        val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()
        val orderNumStr = orderNumber.toString().padStart(3, '0')

        if (isCashier) {
            // Cashier Mode Receipt
            // Header (Centered, double size, no trailing newline)
            mHoinPrinter.printText("Western Cafe", true, true, false, true)

            // Address (Left) with Time & Date (Right) on 2 lines (30 chars max)
            val time = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            } else {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
            }
            val date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            } else {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
            }

            val line1Left = "Sree Bhadra Amrita"
            val pad1 = (30 - time.length).coerceAtLeast(line1Left.length)
            mHoinPrinter.printText(line1Left.padEnd(pad1) + time, false, false, false, false)

            val line2Left = "Amritapuri, Kollam"
            val pad2 = (30 - date.length).coerceAtLeast(line2Left.length)
            mHoinPrinter.printText(line2Left.padEnd(pad2) + date, false, false, false, false)

            // Divider
            mHoinPrinter.printText("-".repeat(30), false, false, false, false)

            // Items (30 chars max per line to avoid accidental wrapping)
            ReceiptWriter.orderItemsText(orderItems, 30).split("\n").forEach {
                if (it.isNotBlank()) {
                    mHoinPrinter.printText(it, false, false, false, false)
                }
            }

            // Total (Bold, double size on 58mm roll: 14 chars * 2 = 28 columns max)
            mHoinPrinter.printText("Total" + orderTotalText.padStart(9, '.'), true, true, true, false)
        } else {
            // Order Mode Receipt (Compact Customer Order Ticket)
            // Order Number & Time Header (double height/width, 14 chars max = 28 cols)
            val headerText = orderNumStr.padEnd(7) + timeInHours.padStart(7)
            mHoinPrinter.printText(headerText, true, true, true, false)

            // Divider
            mHoinPrinter.printText("-".repeat(30), false, false, false, false)

            // Items
            ReceiptWriter.orderItemsText(orderItems, 30).split("\n").forEach {
                if (it.isNotBlank()) {
                    mHoinPrinter.printText(it, false, false, false, false)
                }
            }

            // Divider
            mHoinPrinter.printText("-".repeat(30), false, false, false, false)

            // Total (14 chars * 2 = 28 cols)
            mHoinPrinter.printText("TOTAL" + orderTotalText.padStart(9), true, true, true, false)
        }

        // Optional Amma Quote (Hardware centered, 24-char wrap, no extra blank lines)
        if (configuration?.printAmmaQuote == true) {
            val quoteLines = edu.amrita.amritacafe.quotes.AmmaQuotes.getFormattedLines(orderNumber, 24)
            quoteLines.forEach { line ->
                mHoinPrinter.printText(line, false, false, false, true)
            }
        }

        // Auto-cut: safely ignored by non-cutters, cuts on KP307 etc.
        try {
            mHoinPrinter.testCutting()
        } catch (e: Exception) {
            // Ignored if not supported
        }
    }
}

fun bluetoothPrintKitchen(mHoinPrinter: HoinPrinter, orders: List<Order>, configuration: Configuration? = null) {
    orders.forEach { (orderNumber, orderItems, _, time) ->
        val orderNumStr = orderNumber.toString().padStart(3, '0')

        // Kitchen Header: Order Number & Time (Large, bold, 14 chars * 2 = 28 cols max)
        val headerText = orderNumStr.padEnd(7) + time.padStart(7)
        mHoinPrinter.printText(headerText, true, true, true, false)

        // Heavy Divider
        mHoinPrinter.printText("=".repeat(30), false, false, false, false)

        // Kitchen Items (Bold font for easy reading by kitchen staff)
        orderItems.forEach { item ->
            val qtyStr = if (item.quantity == 1) "  " else item.quantity.toString().padEnd(2)
            var itemLine = "$qtyStr ${item.code}"
            if (item.comment.isNotBlank()) {
                itemLine += "\n  * ${item.comment}"
            }
            if (item.toppings.isNotEmpty()) {
                itemLine += "\n" + item.toppings.joinToString("\n") { top ->
                    val tQty = if (top.quantity == 1) "  " else top.quantity.toString().padEnd(2)
                    "  +$tQty ${top.menuItem.code}"
                }
            }
            itemLine.split("\n").forEach { l ->
                if (l.isNotBlank()) {
                    mHoinPrinter.printText(l, false, false, true, false)
                }
            }
        }

        // Auto-cut
        try {
            mHoinPrinter.testCutting()
        } catch (e: Exception) {
            // Ignored if not supported
        }
    }
}