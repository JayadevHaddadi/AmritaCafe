package edu.amrita.amritacafe.printer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.Socket

object SocketHelper {

    private const val TAG = "SocketHelper"

    /**
     * Creates a Socket that is explicitly bound to the Wi-Fi network interface (if available).
     * This ensures local LAN/Wi-Fi communication (e.g. 192.168.0.x) succeeds even when
     * Mobile Data is turned on and the Wi-Fi network has no internet connection.
     */
    @Suppress("DEPRECATION")
    fun createBoundSocket(context: Context?): Socket {
        val socket = Socket()
        if (context == null) return socket

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val wifiNetwork = cm.allNetworks.firstOrNull { network ->
                    val caps = cm.getNetworkCapabilities(network)
                    caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
                if (wifiNetwork != null) {
                    wifiNetwork.bindSocket(socket)
                    Log.d(TAG, "Successfully bound socket to Wi-Fi network interface: $wifiNetwork")
                } else {
                    Log.d(TAG, "No active Wi-Fi network found to bind socket; using default routing")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bind socket to Wi-Fi network: ${e.message}")
        }
        return socket
    }
}
