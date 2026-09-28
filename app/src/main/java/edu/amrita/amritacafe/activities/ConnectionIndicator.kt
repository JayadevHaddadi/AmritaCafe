package edu.amrita.amritacafe.activities

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import android.widget.Toast
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.Socket

object ConnectionIndicator {
    private var printerIndicatorRef: WeakReference<ImageView>? = null
    private var sheetsIndicatorRef: WeakReference<ImageView>? = null
    @Volatile
    var isPrinterConnected: Boolean = false
        private set

    fun init(printer: ImageView, sheets: ImageView) {
        printerIndicatorRef = WeakReference(printer)
        sheetsIndicatorRef = WeakReference(sheets)
    }

    fun setPrinterConnected(connected: Boolean) {
        isPrinterConnected = connected
        printerIndicatorRef?.get()?.post {
            printerIndicatorRef?.get()?.setColorFilter(if (connected) Color.GREEN else Color.RED)
        }
    }

    fun setSheetsConnected(connected: Boolean) {
        sheetsIndicatorRef?.get()?.post {
            sheetsIndicatorRef?.get()?.setColorFilter(if (connected) Color.GREEN else Color.RED)
        }
    }

    fun checkPrinters(
        context: Context,
        configuration: Configuration,
        scope: CoroutineScope,
        showToast: Boolean = false
    ) {
        scope.launch(Dispatchers.IO) {
            val results = mutableListOf<String>()
            var anyConnected = false
            var anyConfigured = false

            // Check Kitchen Printer
            when (configuration.kitchenPrinterTarget) {
                Configuration.KITCHEN_TARGET_WIFI_1, Configuration.KITCHEN_TARGET_WIFI_2 -> {
                    anyConfigured = true
                    val ip = if (configuration.kitchenPrinterTarget == Configuration.KITCHEN_TARGET_WIFI_1) {
                        configuration.receiptPrinterIP
                    } else {
                        configuration.kitchenPrinterIP
                    }
                    val reachable = testTcpSocket(ip, 9100)
                    if (reachable) anyConnected = true
                    results.add("Kitchen WiFi ($ip): ${if (reachable) "Connected ✓" else "Offline ✗"}")
                }
                Configuration.KITCHEN_TARGET_BLUETOOTH_1, Configuration.KITCHEN_TARGET_BLUETOOTH_2 -> {
                    anyConfigured = true
                    val addr = if (configuration.kitchenPrinterTarget == Configuration.KITCHEN_TARGET_BLUETOOTH_1) {
                        configuration.bluetoothAddress
                    } else {
                        configuration.bluetooth2Address
                    }
                    val name = if (configuration.kitchenPrinterTarget == Configuration.KITCHEN_TARGET_BLUETOOTH_1) {
                        configuration.bluetoothName
                    } else {
                        configuration.bluetooth2Name
                    }
                    val ready = testBluetooth(addr)
                    if (ready) anyConnected = true
                    results.add("Kitchen BT ($name): ${if (ready) "Paired & Ready ✓" else "Not Ready ✗"}")
                }
            }

            // Check Receipt Printer
            when (configuration.receiptPrinterTarget) {
                Configuration.RECEIPT_TARGET_WIFI_1, Configuration.RECEIPT_TARGET_WIFI_2 -> {
                    anyConfigured = true
                    val ip = if (configuration.receiptPrinterTarget == Configuration.RECEIPT_TARGET_WIFI_2) {
                        configuration.kitchenPrinterIP
                    } else {
                        configuration.receiptPrinterIP
                    }
                    val reachable = testTcpSocket(ip, 9100)
                    if (reachable) anyConnected = true
                    results.add("Receipt WiFi ($ip): ${if (reachable) "Connected ✓" else "Offline ✗"}")
                }
                Configuration.RECEIPT_TARGET_BLUETOOTH_1, Configuration.RECEIPT_TARGET_BLUETOOTH_2 -> {
                    anyConfigured = true
                    val addr = if (configuration.receiptPrinterTarget == Configuration.RECEIPT_TARGET_BLUETOOTH_1) {
                        configuration.bluetoothAddress
                    } else {
                        configuration.bluetooth2Address
                    }
                    val name = if (configuration.receiptPrinterTarget == Configuration.RECEIPT_TARGET_BLUETOOTH_1) {
                        configuration.bluetoothName
                    } else {
                        configuration.bluetooth2Name
                    }
                    val ready = testBluetooth(addr)
                    if (ready) anyConnected = true
                    results.add("Receipt BT ($name): ${if (ready) "Paired & Ready ✓" else "Not Ready ✗"}")
                }
            }

            val finalConnected = if (anyConfigured) anyConnected else true
            setPrinterConnected(finalConnected)

            if (showToast) {
                withContext(Dispatchers.Main) {
                    val msg = if (results.isEmpty()) "No printers configured" else results.joinToString("\n")
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun testTcpSocket(ip: String, port: Int): Boolean {
        if (ip.isBlank()) return false
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 1200)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun testBluetooth(address: String): Boolean {
        if (address.isBlank()) return false
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        if (!adapter.isEnabled) return false
        return adapter.bondedDevices?.any { it.address.equals(address, ignoreCase = true) } ?: false
    }
}
