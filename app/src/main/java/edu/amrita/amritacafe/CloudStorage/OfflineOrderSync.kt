package edu.amrita.amritacafe.CloudStorage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import edu.amrita.amritacafe.activities.ConnectionIndicator
import org.json.JSONArray
import java.io.UnsupportedEncodingException

object OfflineOrderSync {
    private const val PREFS_KEY = "offline_orders_queue"

    private fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Synchronized
    fun addOrder(context: Context, jsonString: String) {
        try {
            val prefs = getPrefs(context)
            val queueStr = prefs.getString(PREFS_KEY, "[]") ?: "[]"
            val queue = JSONArray(queueStr)
            queue.put(jsonString)
            prefs.edit().putString(PREFS_KEY, queue.toString()).apply()
            Log.d("OfflineSync", "Added order to offline queue. Total queued: ${queue.length()}")
        } catch (e: Exception) {
            Log.e("OfflineSync", "Failed to add order to offline queue: ${e.message}")
        }
    }

    @Synchronized
    fun syncPendingOrders(context: Context, url: String) {
        try {
            val prefs = getPrefs(context)
            val queueStr = prefs.getString(PREFS_KEY, "[]") ?: "[]"
            val queue = JSONArray(queueStr)

            if (queue.length() == 0) {
                Log.d("OfflineSync", "No pending orders to sync.")
                return
            }

            Log.d("OfflineSync", "Attempting to sync ${queue.length()} pending orders...")
            val requestQueue = Volley.newRequestQueue(context)
            
            // Try to send the first one in the queue
            val pendingJsonString = queue.getString(0)

            val stringRequest = object : StringRequest(
                Method.POST, url,
                { response ->
                    Log.d("OfflineSync", "Successfully synced 1 offline order: $response")
                    ConnectionIndicator.setSheetsConnected(true)
                    
                    // Remove the synced order from the queue
                    removeOrderFromQueue(context, pendingJsonString)
                    
                    // Try next recursively
                    syncPendingOrders(context, url)
                },
                { error ->
                    Log.e("OfflineSync", "Failed to sync pending order: ${error.message}")
                }
            ) {
                override fun getBodyContentType(): String = "application/json; charset=utf-8"
                override fun getBody(): ByteArray = try {
                    pendingJsonString.toByteArray(Charsets.UTF_8)
                } catch (e: UnsupportedEncodingException) {
                    ByteArray(0)
                }
            }
            
            stringRequest.setRetryPolicy(DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
            requestQueue.add(stringRequest)
        } catch (e: Exception) {
            Log.e("OfflineSync", "Error during offline sync: ${e.message}")
        }
    }

    @Synchronized
    private fun removeOrderFromQueue(context: Context, jsonStringToRemove: String) {
        try {
            val prefs = getPrefs(context)
            val currentQueueStr = prefs.getString(PREFS_KEY, "[]") ?: "[]"
            val currentQueue = JSONArray(currentQueueStr)
            
            if (currentQueue.length() > 0 && currentQueue.getString(0) == jsonStringToRemove) {
                val updatedQueue = JSONArray()
                for (i in 1 until currentQueue.length()) {
                    updatedQueue.put(currentQueue.getString(i))
                }
                prefs.edit().putString(PREFS_KEY, updatedQueue.toString()).apply()
            }
        } catch (e: Exception) {
            Log.e("OfflineSync", "Failed to remove order from queue: ${e.message}")
        }
    }
}
