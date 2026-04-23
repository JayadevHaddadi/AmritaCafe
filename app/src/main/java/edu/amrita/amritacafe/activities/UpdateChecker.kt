package edu.amrita.amritacafe.activities

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import edu.amrita.amritacafe.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object UpdateChecker {
    private val UPDATE_INFO_URL = BuildConfig.UPDATE_SCRIPT_URL
    
    private var isShowing = false
    private var downloadId: Long = -1
    private var lastCheckTime: Long = 0
    private const val CHECK_INTERVAL = 60 * 60 * 1000 // 1 hour

    fun checkForUpdates(context: Context) {
        val currentTime = System.currentTimeMillis()
        if (isShowing || (currentTime - lastCheckTime < CHECK_INTERVAL)) {
            return
        }
        
        lastCheckTime = currentTime
        val queue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(
            Request.Method.GET, UPDATE_INFO_URL,
            { response ->
                try {
                    val json = JSONObject(response)
                    val latestVersionCode = json.getInt("versionCode")
                    val updateUrl = json.getString("updateUrl")
                    
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    }

                    if (latestVersionCode > currentVersionCode) {
                        showUpdateDialog(context, updateUrl)
                    }
                } catch (e: Exception) {
                    Log.e("UpdateChecker", "Error parsing update info", e)
                }
            },
            { error ->
                Log.e("UpdateChecker", "Error checking for updates", error)
            }
        )
        
        stringRequest.retryPolicy = DefaultRetryPolicy(
            15000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        
        queue.add(stringRequest)
    }

    private fun showUpdateDialog(context: Context, updateUrl: String) {
        if (isShowing) return
        isShowing = true

        val dialog = AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("A newer version of the app is available. Download and install now?")
            .setPositiveButton("Update") { _, _ ->
                isShowing = false
                startDownload(context, updateUrl)
            }
            .setNegativeButton("Later") { _, _ ->
                isShowing = false
            }
            .setCancelable(false)
            .create()

        dialog.show()
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
    }

    private fun startDownload(context: Context, url: String) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AmritaCafe_update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Amrita Cafe Update")
            .setDescription("Please wait...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                                                        if (isValidApk(destination)) {
                                Log.d("UpdateChecker", "Download success. Size: ${destination.length()} bytes")
                                context.unregisterReceiver(this)
                                installApk(context, destination)
                            } else {
                                val size = destination.length()
                                context.unregisterReceiver(this)
                                showFallbackDialog(context, url, "Invalid file received ($size bytes). The link might not be a direct download.")
                            }
                        } else {
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = cursor.getInt(reasonIndex)
                            context.unregisterReceiver(this)
                            showFallbackDialog(context, url, "Download failed (Error code: $reason).")
                        }
                    }
                    cursor.close()
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun showFallbackDialog(context: Context, url: String, reason: String) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Auto-Update Failed")
            .setMessage("$reason\n\nWould you like to try downloading manually via the browser?")
            .setPositiveButton("Open Browser") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        
        // Force button colors for visibility
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
    }

    private fun isValidApk(file: File): Boolean {
        if (!file.exists() || file.length() < 1000) return false
        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                if (fis.read(header) == 4) {
                    // ZIP/APK files start with PK (0x50 0x4B 0x03 0x04)
                    return header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                           header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error validating file", e)
        }
        return false
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error starting installation", e)
            Toast.makeText(context, "Failed to launch installer", Toast.LENGTH_SHORT).show()
        }
    }
}