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
}
