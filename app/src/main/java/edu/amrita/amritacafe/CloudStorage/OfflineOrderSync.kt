package edu.amrita.amritacafe.CloudStorage

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import edu.amrita.amritacafe.activities.ConnectionIndicator
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object OfflineOrderSync {
    private const val TAG = "OfflineOrderSync"
    private const val QUEUE_FILE_NAME = "google_sheets_pending_queue.json"
    private const val LEGACY_PREFS_KEY = "offline_orders_queue"

    private val lock = Any()
    @Volatile
    private var isSyncing = false

    data class PendingSheetsRequest(
        val id: String = UUID.randomUUID().toString(),
        val url: String,
        val payload: String,
        val description: String = "",
        val queuedTime: Long = System.currentTimeMillis(),
        var attempts: Int = 0
    ) {
        fun toJsonObject(): JSONObject = JSONObject().apply {
            put("id", id)
            put("url", url)
            put("payload", payload)
            put("description", description)
            put("queuedTime", queuedTime)
            put("attempts", attempts)
        }

        companion object {
            fun fromJsonObject(obj: JSONObject): PendingSheetsRequest {
                return PendingSheetsRequest(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    url = obj.optString("url", getOrderScriptUrl()),
                    payload = obj.optString("payload", "{}"),
                    description = obj.optString("description", ""),
                    queuedTime = obj.optLong("queuedTime", System.currentTimeMillis()),
                    attempts = obj.optInt("attempts", 0)
                )
            }
        }
    }

    private fun getQueueFile(context: Context): File {
        return File(context.filesDir, QUEUE_FILE_NAME)
    }

    private fun readQueueFromDisk(context: Context): MutableList<PendingSheetsRequest> {
        val list = mutableListOf<PendingSheetsRequest>()
        val file = getQueueFile(context)

        // 1. Check legacy SharedPreferences queue and migrate if present
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val legacyStr = prefs.getString(LEGACY_PREFS_KEY, null)
            if (!legacyStr.isNullOrBlank() && legacyStr != "[]") {
                val legacyArray = JSONArray(legacyStr)
                for (i in 0 until legacyArray.length()) {
                    val payload = legacyArray.getString(i)
                    list.add(
                        PendingSheetsRequest(
                            url = getOrderScriptUrl(),
                            payload = payload,
                            description = "Migrated order"
                        )
                    )
                }
                prefs.edit().remove(LEGACY_PREFS_KEY).apply()
                Log.d(TAG, "Migrated ${legacyArray.length()} orders from SharedPreferences to disk queue.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking legacy queue: ${e.message}")
        }

        // 2. Read persistent file from disk
        if (file.exists()) {
            try {
                val content = file.readText(Charsets.UTF_8)
                if (content.isNotBlank()) {
                    val array = JSONArray(content)
                    for (i in 0 until array.length()) {
                        list.add(PendingSheetsRequest.fromJsonObject(array.getJSONObject(i)))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading queue file: ${e.message}")
            }
        }

        return list
    }

    private fun writeQueueToDisk(context: Context, queue: List<PendingSheetsRequest>) {
        val file = getQueueFile(context)
        val tmp = File(context.filesDir, "$QUEUE_FILE_NAME.tmp")
        try {
            val array = JSONArray()
            queue.forEach { array.put(it.toJsonObject()) }

            FileOutputStream(tmp).use { fos ->
                fos.write(array.toString().toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync() // Ensure physical flush to flash memory (power cut safe)
            }

            if (!tmp.renameTo(file)) {
                if (file.delete()) {
                    tmp.renameTo(file)
                } else {
                    file.writeText(array.toString(), Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write queue to disk: ${e.message}", e)
        }
    }

    fun getPendingCount(context: Context): Int = synchronized(lock) {
        return readQueueFromDisk(context).size
    }

    fun addOrder(
        context: Context,
        jsonString: String,
        url: String = getOrderScriptUrl(),
        description: String = ""
    ) = synchronized(lock) {
        try {
            val queue = readQueueFromDisk(context)
            val request = PendingSheetsRequest(
                url = url,
                payload = jsonString,
                description = description
            )
            queue.add(request)
            writeQueueToDisk(context, queue)
            Log.d(TAG, "Safely saved order to offline queue on disk. Total pending: ${queue.size}")
            ConnectionIndicator.setSheetsConnected(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist order to offline queue: ${e.message}", e)
        }
    }

    fun syncPendingOrders(context: Context, targetUrl: String = getOrderScriptUrl()) {
        if (isSyncing) {
            Log.d(TAG, "Sync already in progress, skipping duplicate run.")
            return
        }

        val nextItem: PendingSheetsRequest? = synchronized(lock) {
            val queue = readQueueFromDisk(context)
            if (queue.isEmpty()) {
                ConnectionIndicator.setSheetsConnected(true)
                null
            } else {
                queue.first()
            }
        }

        if (nextItem == null) return

        isSyncing = true
        sendSingleRequest(context, nextItem, targetUrl)
    }

    private fun sendSingleRequest(
        context: Context,
        item: PendingSheetsRequest,
        fallbackUrl: String
    ) {
        val url = if (item.url.isNotBlank()) item.url else fallbackUrl
        val requestQueue = Volley.newRequestQueue(context)

        val stringRequest = object : StringRequest(
            Method.POST, url,
            { response ->
                val trimmed = response.trim()
                // Verify confirmation: Must not be HTML error or captive portal login page
                val isHtml = trimmed.startsWith("<", ignoreCase = true) ||
                        trimmed.contains("<html>", ignoreCase = true) ||
                        trimmed.contains("<!DOCTYPE", ignoreCase = true)
                val hasSyntaxError = trimmed.contains("SyntaxError", ignoreCase = true)

                if (isHtml || hasSyntaxError) {
                    Log.w(TAG, "Response from $url was HTML / Error (possible captive portal or script error), retaining order in queue: $trimmed")
                    ConnectionIndicator.setSheetsConnected(false)
                    isSyncing = false
                } else {
                    Log.d(TAG, "Confirmed delivery to Google Sheets for item ${item.id}: $trimmed")
                    var remainingCount = 0
                    synchronized(lock) {
                        val currentQueue = readQueueFromDisk(context)
                        val iterator = currentQueue.iterator()
                        while (iterator.hasNext()) {
                            if (iterator.next().id == item.id) {
                                iterator.remove()
                                break
                            }
                        }
                        writeQueueToDisk(context, currentQueue)
                        remainingCount = currentQueue.size
                    }

                    if (remainingCount == 0) {
                        Log.d(TAG, "All pending Google Sheets orders confirmed and synced!")
                        ConnectionIndicator.setSheetsConnected(true)
                        isSyncing = false
                    } else {
                        Log.d(TAG, "$remainingCount orders remaining in queue, processing next...")
                        isSyncing = false
                        // Process next pending item
                        syncPendingOrders(context, fallbackUrl)
                    }
                }
            },
            { error ->
                Log.w(TAG, "Failed to reach Google Sheets (${error.message}). Order kept safely on disk.")
                ConnectionIndicator.setSheetsConnected(false)
                isSyncing = false
            }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = item.payload.toByteArray(Charsets.UTF_8)
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            15000, // 15s timeout
            0,     // 0 internal retries so queue maintains strict control
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(stringRequest)
    }
}
