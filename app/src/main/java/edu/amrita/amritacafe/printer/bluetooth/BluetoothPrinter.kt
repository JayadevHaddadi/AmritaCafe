package edu.amrita.amritacafe.printer.bluetooth

import android.os.Build
import com.example.hoinprinterlib.HoinPrinter
import edu.amrita.amritacafe.activities.capitalizeWords
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BluetoothPrinter {

}


fun bluetoothPrint(
    printer: HoinPrinter,
    orderNumStr: String,
    orderItems: List<RegularOrderItem>,
    orderTotalText: String
) {
    //                val path = Uri.parse("android.resource://edu.amrita.amritacafe3/" + R.drawable.logo)
    //                val otherPath = Uri.parse("android.resource://edu.amrita.amritacafe3/drawable/logo")
    //                mHoinPrinter.printImage(otherPath.toString(), true)

    printer.printText(
        "Western Cafe",
        true, true, false, true
    )

    printer.printText(
        "Sree Bhadra Amrita\nSelf Help Group\n" +
                "AMRITAPURI\n".capitalizeWords() +
                "KOLLAM-690546".capitalizeWords(),
        false,
        false,
        false,
        false
    )
    printer.printText(
        "${"".padEnd(32, '-')}",
        false, false, false, false
    )

    val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
    } else {
        TODO("VERSION.SDK_INT < O")
    }
    val current = LocalDateTime.now().format(formatter)
    printer.printText(
        "${("Order " + orderNumStr).padEnd(16)}${current.padStart(16)}",
        false, false, false, false
    )

    printer.printText(
        ReceiptWriter.orderItemsText(orderItems),
        false,
        false,
        false,
        false
    )

    printer.printText(
        "Total" + orderTotalText.padStart(11, '.'),
        true,
        true,
        true,
        false
    )

    printer.printText(
        "${"".padEnd(32, '-')}",
        false, false, false, false
    )

    printer.printText(
        "Thank You",
        true, true, false, true
    )
}