package edu.amrita.amritacafe.crash

import android.content.Context
import android.os.Build
import android.util.Log
import edu.amrita.amritacafe.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {
    private const val TAG = "CrashHandler"
    private const val CRASH_FILE_NAME = "crash_logs.txt"
    private const val MAX_LOG_SIZE = 100 * 1024 // 100 KB max

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(context.applicationContext, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record crash: ${e.message}", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val crashReport = buildString {
            append("========================================\n")
            append("TIME: $time\n")
            append("APP VERSION: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("DEVICE: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})\n")
            append("THREAD: ${thread.name} (id: ${thread.id})\n")
            append("EXCEPTION: ${throwable.javaClass.name}: ${throwable.message}\n")
            append("STACKTRACE:\n$stackTrace\n")
            append("========================================\n\n")
        }

        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            val existing = if (file.exists()) file.readText() else ""
            val combined = (crashReport + existing).take(MAX_LOG_SIZE)
            file.writeText(combined, Charsets.UTF_8)
            Log.e(TAG, "Recorded crash report to $file")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing crash to file: ${e.message}")
        }
    }

    fun getCrashLogs(context: Context): String {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists()) file.readText(Charsets.UTF_8).trim() else ""
    }

    fun clearCrashLogs(context: Context): Boolean {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists()) file.delete() else true
    }

    fun uploadPendingCrashLogs(context: Context) {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (!file.exists() || file.length() == 0L) return

        val content = try {
            file.readText(Charsets.UTF_8).trim()
        } catch (e: Exception) {
            return
        }

        if (content.isEmpty()) return

        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val tabletName = prefs.getString("tablet name", "Unknown") ?: "Unknown"

        val json = org.json.JSONObject().apply {
            put("action", "reportCrash")
            put("tablet", tabletName)
            put("appVersion", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            put("deviceInfo", "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            put("stackTrace", content)
        }

        val url = edu.amrita.amritacafe.CloudStorage.getOrderScriptUrl()
        val requestQueue = edu.amrita.amritacafe.CloudStorage.SharedRequestQueue.get(context)
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val res = org.json.JSONObject(response)
                    if (res.optString("status") == "success") {
                        file.delete()
                        Log.d(TAG, "Crash logs successfully uploaded to Google Sheets and cleared locally.")
                    }
                } catch (e: Exception) {
                    // Not parsed or error, leave on disk
                }
            },
            { error ->
                Log.w(TAG, "Could not upload crash logs to Google Sheets (${error.message}). Will retry next launch.")
            }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = json.toString().toByteArray(Charsets.UTF_8)
        }

        stringRequest.retryPolicy = com.android.volley.DefaultRetryPolicy(
            30000,
            0,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(stringRequest)
    }
}
