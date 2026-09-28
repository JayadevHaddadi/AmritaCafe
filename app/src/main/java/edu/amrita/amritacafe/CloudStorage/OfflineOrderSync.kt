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
                    val rawItem = legacyArray.get(i)
                    val payload = rawItem.toString()
                    val orderNum = try { JSONObject(payload).optString("order", "") } catch (e: Exception) { "" }
                    val tabletName = try { JSONObject(payload).optString("tablet", "") } catch (e: Exception) { "" }
                    val desc = if (orderNum.isNotBlank()) "Order $orderNum ($tabletName)" else "Migrated order"
                    list.add(
                        PendingSheetsRequest(
                            url = getOrderScriptUrl(),
                            payload = payload,
                            description = desc
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

    fun getPendingQueueSnapshot(context: Context): List<PendingSheetsRequest> = synchronized(lock) {
        return readQueueFromDisk(context)
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

    private const val BATCH_SIZE = 25

    fun syncPendingOrders(context: Context, targetUrl: String = getOrderScriptUrl()) {
        if (isSyncing) {
            Log.d(TAG, "Sync already in progress, skipping duplicate run.")
            return
        }

        val batchItems: List<PendingSheetsRequest> = synchronized(lock) {
            val queue = readQueueFromDisk(context)
            if (queue.isEmpty()) {
                ConnectionIndicator.setSheetsConnected(true)
                emptyList()
            } else {
                val first = queue.first()
                if (first.payload.contains("\"action\"")) {
                    listOf(first)
                } else {
                    val batch = mutableListOf<PendingSheetsRequest>()
                    for (item in queue) {
                        if (item.payload.contains("\"action\"")) break
                        batch.add(item)
                        if (batch.size >= BATCH_SIZE) break
                    }
                    if (batch.isEmpty()) listOf(first) else batch
                }
            }
        }

        if (batchItems.isEmpty()) return

        isSyncing = true
        sendBatchRequest(context, batchItems, targetUrl)
    }

    private fun sendBatchRequest(
        context: Context,
        batch: List<PendingSheetsRequest>,
        fallbackUrl: String
    ) {
        val url = if (batch.first().url.isNotBlank()) batch.first().url else fallbackUrl
        val requestQueue = Volley.newRequestQueue(context)

        val payloadBytes: ByteArray = if (batch.size == 1) {
            batch.first().payload.toByteArray(Charsets.UTF_8)
        } else {
            val array = JSONArray()
            batch.forEach {
                try {
                    array.put(JSONObject(it.payload))
                } catch (e: Exception) {
                    // ignore
                }
            }
            val root = JSONObject()
            root.put("action", "batchOrders")
            root.put("orders", array)
            root.toString().toByteArray(Charsets.UTF_8)
        }

        val stringRequest = object : StringRequest(
            Method.POST, url,
            { response ->
                val trimmed = response.trim()
                val isHtml = trimmed.startsWith("<", ignoreCase = true) ||
                        trimmed.contains("<html>", ignoreCase = true) ||
                        trimmed.contains("<!DOCTYPE", ignoreCase = true)
                val hasSyntaxError = trimmed.contains("SyntaxError", ignoreCase = true)

                if (isHtml || hasSyntaxError) {
                    Log.w(TAG, "Response from $url was HTML / Error, retaining orders in queue: $trimmed")
                    ConnectionIndicator.setSheetsConnected(false)
                    isSyncing = false
                } else {
                    Log.d(TAG, "Confirmed delivery of ${batch.size} orders to Google Sheets: $trimmed")
                    var remainingCount = 0
                    val batchIds = batch.map { it.id }.toSet()
                    synchronized(lock) {
                        val currentQueue = readQueueFromDisk(context)
                        currentQueue.removeAll { it.id in batchIds }
                        writeQueueToDisk(context, currentQueue)
                        remainingCount = currentQueue.size
                    }

                    if (remainingCount == 0) {
                        Log.d(TAG, "All pending Google Sheets orders confirmed and synced!")
                        ConnectionIndicator.setSheetsConnected(true)
                        isSyncing = false
                    } else {
                        Log.d(TAG, "$remainingCount orders remaining in queue, processing next batch...")
                        isSyncing = false
                        syncPendingOrders(context, fallbackUrl)
                    }
                }
            },
            { error ->
                Log.w(TAG, "Failed to reach Google Sheets (${error.javaClass.simpleName}: ${error.message}). Orders kept safely on disk.")
                ConnectionIndicator.setSheetsConnected(false)
                isSyncing = false
            }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = payloadBytes
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            60000, // 60s timeout for batch requests
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(stringRequest)
    }
}
