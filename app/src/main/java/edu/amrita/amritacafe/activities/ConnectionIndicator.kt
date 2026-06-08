package edu.amrita.amritacafe.activities

import android.graphics.Color
import android.widget.ImageView
import java.lang.ref.WeakReference

object ConnectionIndicator {
    private var printerIndicatorRef: WeakReference<ImageView>? = null
    private var sheetsIndicatorRef: WeakReference<ImageView>? = null

    fun init(printer: ImageView, sheets: ImageView) {
        printerIndicatorRef = WeakReference(printer)
        sheetsIndicatorRef = WeakReference(sheets)
    }

    fun setPrinterConnected(connected: Boolean) {
        printerIndicatorRef?.get()?.setColorFilter(if (connected) Color.GREEN else Color.RED)
    }

    fun setSheetsConnected(connected: Boolean) {
        sheetsIndicatorRef?.get()?.setColorFilter(if (connected) Color.GREEN else Color.RED)
    }
}
