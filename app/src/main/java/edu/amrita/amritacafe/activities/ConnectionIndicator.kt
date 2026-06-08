package edu.amrita.amritacafe.activities

import android.graphics.Color
import android.view.View
import java.lang.ref.WeakReference

object ConnectionIndicator {
    private var printerIndicatorRef: WeakReference<View>? = null
    private var sheetsIndicatorRef: WeakReference<View>? = null

    fun init(printer: View, sheets: View) {
        printerIndicatorRef = WeakReference(printer)
        sheetsIndicatorRef = WeakReference(sheets)
    }

    fun setPrinterConnected(connected: Boolean) {
        printerIndicatorRef?.get()?.setBackgroundColor(if (connected) Color.GREEN else Color.RED)
    }

    fun setSheetsConnected(connected: Boolean) {
        sheetsIndicatorRef?.get()?.setBackgroundColor(if (connected) Color.GREEN else Color.RED)
    }
}
