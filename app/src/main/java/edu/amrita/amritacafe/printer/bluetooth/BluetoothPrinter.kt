package edu.amrita.amritacafe.printer.bluetooth

import android.os.Build
import android.util.Log
import com.example.hoinprinterlib.HoinPrinter
import edu.amrita.amritacafe.AmritaCafeApp
import edu.amrita.amritacafe.activities.capitalizeWords
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.writer.CashierReceiptWriter
import edu.amrita.amritacafe.printer.writer.KitchenWriter
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.quotes.AmmaQuotes
import edu.amrita.amritacafe.settings.Configuration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BluetoothPrinter

fun HoinPrinter.sendRawData(data: ByteArray): Boolean {
    return try {
        val field = HoinPrinter::class.java.getDeclaredField("mPrinterModule")
        field.isAccessible = true
        val module = field.get(this) as? com.example.hoinprinterlib.module.PrinterModule
        if (module != null) {
            // Chunk data in 256-byte blocks with short pauses to prevent Bluetooth SPP buffer overflows
            val chunks = data.toList().chunked(256)
            for (chunk in chunks) {
                module.sendData(chunk.toByteArray())
                Thread.sleep(15)
            }
            true
        } else {
            false
        }
    } catch (e: Exception) {
        Log.e("BluetoothPrinter", "Failed to send raw ESC/POS data via reflection: ${e.message}")
        false
    }
}

fun bluetoothPrint(mHoinPrinter: HoinPrinter, orders: List<Order>, configuration: Configuration? = null) {
    bluetoothPrintReceipt(mHoinPrinter, orders, configuration)
}

fun bluetoothPrintReceipt(mHoinPrinter: HoinPrinter, orders: List<Order>, configuration: Configuration? = null) {
    val cfg = configuration ?: Configuration(androidx.preference.PreferenceManager.getDefaultSharedPreferences(AmritaCafeApp.appContext))
    val is80mm = cfg.isReceiptBluetooth80mm
    val cols = if (is80mm) 42 else 30
    val isCashier = cfg.workflowMode == Configuration.MODE_CASHIER

    // 1. Primary: Send raw ESC/POS byte stream (fixes double spacing, alignment, and auto-cuts)
    val rawData = if (isCashier) {
        CashierReceiptWriter(orders, cfg).writeToEscPos(cols)
    } else {
        ReceiptWriter(orders, cfg).writeToEscPos(cols)
    }

    if (cfg.isReceiptBluetooth2) {
        val address = cfg.receiptBluetoothAddress
        if (address.isNotEmpty()) {
            BluetoothRawPrinter.print(address, rawData)
        }
        return
    }

    // Target is Bluetooth 1:
    if (mHoinPrinter.sendRawData(rawData)) {
        return
    }

    // If Hoin SDK reflection failed or disconnected, try direct RFCOMM on Bluetooth 1 address
    val bt1Address = cfg.bluetoothAddress
    if (bt1Address.isNotEmpty() && BluetoothRawPrinter.print(bt1Address, rawData)) {
        return
    }

    // 2. Fallback: Hoin SDK high-level API if reflection and direct RFCOMM fail
    orders.forEach { order ->
        val (orderNumber, orderItems, _, timeInHours) = order
        val orderTotalText = orderItems.map { it.totalPrice() }.sum().toString()
        val orderNumStr = orderNumber.toString().padStart(3, '0')
        val doubleWidthCols = cols / 2

        if (isCashier) {
            mHoinPrinter.printText("Western Cafe", true, true, false, true)

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
            val pad1 = (cols - time.length).coerceAtLeast(line1Left.length)
            mHoinPrinter.printText(line1Left.padEnd(pad1) + time, false, false, false, false)

            val line2Left = if (cols >= 42) "Amritapuri, Kollam-690546" else "Amritapuri, Kollam"
            val pad2 = (cols - date.length).coerceAtLeast(line2Left.length)
            mHoinPrinter.printText(line2Left.padEnd(pad2) + date, false, false, false, false)

            mHoinPrinter.printText("-".repeat(cols), false, false, false, false)

            ReceiptWriter.orderItemsText(orderItems, cols).split("\n").forEach {
                if (it.isNotBlank()) {
                    mHoinPrinter.printText(it, false, false, false, false)
                }
            }

            val totalPrefix = "Total"
            val dotCount = (doubleWidthCols - totalPrefix.length - orderTotalText.length).coerceAtLeast(1)
            mHoinPrinter.printText(totalPrefix + ".".repeat(dotCount) + orderTotalText, true, true, true, false)
        } else {
            val headerPad = (doubleWidthCols - timeInHours.length).coerceAtLeast(orderNumStr.length)
            val headerText = orderNumStr.padEnd(headerPad) + timeInHours
            mHoinPrinter.printText(headerText, true, true, true, false)

            mHoinPrinter.printText("-".repeat(cols), false, false, false, false)

            ReceiptWriter.orderItemsText(orderItems, cols).split("\n").forEach {
                if (it.isNotBlank()) {
                    mHoinPrinter.printText(it, false, false, false, false)
                }
            }

            mHoinPrinter.printText("-".repeat(cols), false, false, false, false)

            val totalPrefix = "TOTAL"
            val dotCount = (doubleWidthCols - totalPrefix.length - orderTotalText.length).coerceAtLeast(1)
            mHoinPrinter.printText(totalPrefix + ".".repeat(dotCount) + orderTotalText, true, true, true, false)
        }

        if (cfg.printAmmaQuote) {
            val quoteChars = if (cols >= 42) 36 else 24
            val quoteLines = AmmaQuotes.getFormattedLines(orderNumber, quoteChars)
            quoteLines.forEach { line ->
                mHoinPrinter.printText(line, false, false, false, true)
            }
        }

        try {
            mHoinPrinter.testCutting()
        } catch (e: Exception) {
            // Ignored if not supported
        }
    }
}

fun bluetoothPrintKitchen(mHoinPrinter: HoinPrinter, orders: List<Order>, configuration: Configuration? = null) {
    val cfg = configuration ?: Configuration(androidx.preference.PreferenceManager.getDefaultSharedPreferences(AmritaCafeApp.appContext))
    val is80mm = cfg.isKitchenBluetooth80mm
    val cols = if (is80mm) 42 else 30

    // 1. Primary: Send raw ESC/POS byte stream
    val rawData = KitchenWriter(orders, cfg).writeToEscPos(cols)

    if (cfg.isKitchenBluetooth2) {
        val address = cfg.kitchenBluetoothAddress
        if (address.isNotEmpty()) {
            BluetoothRawPrinter.print(address, rawData)
        }
        return
    }

    // Target is Bluetooth 1:
    if (mHoinPrinter.sendRawData(rawData)) {
        return
    }

    // If Hoin SDK reflection failed or disconnected, try direct RFCOMM on Bluetooth 1 address
    val bt1Address = cfg.bluetoothAddress
    if (bt1Address.isNotEmpty() && BluetoothRawPrinter.print(bt1Address, rawData)) {
        return
    }

    // 2. Fallback: Hoin SDK high-level API
    orders.forEach { (orderNumber, orderItems, _, time) ->
        val orderNumStr = orderNumber.toString().padStart(3, '0')
        val doubleWidthCols = cols / 2

        val headerPad = (doubleWidthCols - time.length).coerceAtLeast(orderNumStr.length)
        val headerText = orderNumStr.padEnd(headerPad) + time
        mHoinPrinter.printText(headerText, true, true, true, false)

        mHoinPrinter.printText("=".repeat(cols), false, false, false, false)

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

        try {
            mHoinPrinter.testCutting()
        } catch (e: Exception) {
            // Ignored if not supported
        }
    }
}