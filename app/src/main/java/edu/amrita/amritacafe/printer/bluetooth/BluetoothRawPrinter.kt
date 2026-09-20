package edu.amrita.amritacafe.printer.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

object BluetoothRawPrinter {

    private const val TAG = "BluetoothRawPrinter"
    // Standard Serial Port Profile (SPP) UUID used by all ESC/POS Bluetooth printers
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun print(macAddress: String, data: ByteArray): Boolean {
        if (macAddress.isBlank()) {
            Log.w(TAG, "Cannot print: Bluetooth MAC address is empty")
            return false
        }

        var socket: BluetoothSocket? = null
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                Log.w(TAG, "Bluetooth adapter is disabled or not supported")
                return false
            }

            val device = adapter.getRemoteDevice(macAddress)
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }

            Log.d(TAG, "Connecting to Bluetooth printer: ${device.name ?: macAddress} ($macAddress)...")
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            Log.d(TAG, "Connected. Sending ${data.size} bytes...")
            val out = socket.outputStream
            val chunks = data.toList().chunked(256)
            for (chunk in chunks) {
                out.write(chunk.toByteArray())
                try {
                    Thread.sleep(15)
                } catch (e: InterruptedException) {
                    // Ignore interruption
                }
            }
            out.flush()
            try {
                Thread.sleep(150) // Allow printer hardware buffer to process before tearing down connection
            } catch (e: InterruptedException) {
                // Ignore interruption
            }
            Log.d(TAG, "Print data sent successfully to $macAddress")
            true
        } catch (e: IOException) {
            Log.e(TAG, "IO error printing to $macAddress: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error printing to $macAddress: ${e.message}")
            false
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore socket close errors
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printAsync(macAddress: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        print(macAddress, data)
    }
}
