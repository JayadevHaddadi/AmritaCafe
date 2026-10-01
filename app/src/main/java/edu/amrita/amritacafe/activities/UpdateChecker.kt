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
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import edu.amrita.amritacafe.CloudStorage.SharedRequestQueue
import edu.amrita.amritacafe.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object UpdateChecker {
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/JayadevHaddadi/AmritaCafe/releases?per_page=10"
    private val UPDATE_INFO_URL = BuildConfig.UPDATE_SCRIPT_URL
    
    private var isShowing = false
    private var downloadId: Long = -1
    private var lastCheckTime: Long = 0
    private const val CHECK_INTERVAL = 60 * 60 * 1000 // 1 hour

    fun checkForUpdates(context: Context, force: Boolean = false, onComplete: ((updateFound: Boolean) -> Unit)? = null) {
        val currentTime = System.currentTimeMillis()
        if (isShowing || (!force && (currentTime - lastCheckTime < CHECK_INTERVAL))) {
            onComplete?.invoke(false)
            return
        }

        lastCheckTime = currentTime
        val queue = SharedRequestQueue.get(context)
        
        // 1. Query GitHub Releases API directly
        val githubRequest = object : StringRequest(
            Request.Method.GET, GITHUB_RELEASES_URL,
            { response ->
                try {
                    val releases = org.json.JSONArray(response)
                    var standardVersionCode = 0
                    var standardUpdateUrl = ""
                    var betaVersionCode = 0
                    var betaUpdateUrl = ""

                    for (i in 0 until releases.length()) {
                        val rel = releases.getJSONObject(i)
                        val isPrerelease = rel.optBoolean("prerelease", false)
                        val isDraft = rel.optBoolean("draft", false)
                        if (isDraft) continue

                        val tag = rel.optString("tag_name", "")
                        val vCode = tag.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

                        var apkUrl = ""
                        val assets = rel.optJSONArray("assets")
                        if (assets != null) {
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val assetName = asset.optString("name", "")
                                if (assetName.endsWith(".apk", ignoreCase = true)) {
                                    val url = asset.optString("browser_download_url", "")
                                    if (assetName.equals("app-release.apk", ignoreCase = true)) {
                                        apkUrl = url
                                        break
                                    } else if (apkUrl.isEmpty()) {
                                        apkUrl = url
                                    }
                                }
                            }
                        }

                        if (apkUrl.isNotEmpty() && vCode > 0) {
                            if (isPrerelease && betaVersionCode == 0) {
                                betaVersionCode = vCode
                                betaUpdateUrl = apkUrl
                            } else if (!isPrerelease && standardVersionCode == 0) {
                                standardVersionCode = vCode
                                standardUpdateUrl = apkUrl
                            }
                        }

                        if (standardVersionCode > 0 && betaVersionCode > 0) {
                            break
                        }
                    }

                    val pref = PreferenceManager.getDefaultSharedPreferences(context)
                    val isBetaUser = pref.getBoolean("beta_updates", false)

                    var latestVersionCode = standardVersionCode
                    var updateUrl = standardUpdateUrl
                    var isFinalBeta = false

                    if (isBetaUser && betaVersionCode > standardVersionCode) {
                        latestVersionCode = betaVersionCode
                        updateUrl = betaUpdateUrl
                        isFinalBeta = true
                    }

                    if (latestVersionCode > 0 && updateUrl.isNotEmpty()) {
                        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode.toInt()
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode
                        }

                        if (latestVersionCode > currentVersionCode) {
                            showUpdateDialog(context, updateUrl, isFinalBeta)
                            onComplete?.invoke(true)
                        } else {
                            onComplete?.invoke(false)
                        }
                    } else if (UPDATE_INFO_URL.isNotEmpty()) {
                        // Fallback to Google Sheets update script if no GitHub APK found
                        checkForUpdatesFallback(context, onComplete)
                    } else {
                        onComplete?.invoke(false)
                    }
                } catch (e: Exception) {
                    Log.e("UpdateChecker", "Error parsing GitHub releases, trying fallback", e)
                    checkForUpdatesFallback(context, onComplete)
                }
            },
            { error ->
                Log.e("UpdateChecker", "GitHub API error: ${error.message}, trying fallback")
                checkForUpdatesFallback(context, onComplete)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "AmritaCafe-App"
                headers["Accept"] = "application/vnd.github.v3+json"
                return headers
            }
        }

        githubRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(githubRequest)
    }

    data class ReleaseInfo(
        val tag: String,
        val name: String,
        val isPrerelease: Boolean,
        val versionCode: Int,
        val apkUrl: String,
        val publishedAt: String
    )

    private fun githubHeaders(): MutableMap<String, String> {
        val headers = HashMap<String, String>()
        headers["User-Agent"] = "AmritaCafe-App"
        headers["Accept"] = "application/vnd.github.v3+json"
        return headers
    }

    /** Fetches the most recent GitHub releases (both pre-release/beta and standard). */
    fun fetchRecentReleases(
        context: Context,
        onResult: (List<ReleaseInfo>) -> Unit,
        onError: (String) -> Unit
    ) {
        val queue = SharedRequestQueue.get(context)
        val request = object : StringRequest(
            Request.Method.GET, GITHUB_RELEASES_URL,
            { response ->
                try {
                    val releases = org.json.JSONArray(response)
                    val list = mutableListOf<ReleaseInfo>()
                    for (i in 0 until releases.length()) {
                        val rel = releases.getJSONObject(i)
                        if (rel.optBoolean("draft", false)) continue

                        val tag = rel.optString("tag_name", "")
                        val name = rel.optString("name", tag).ifBlank { tag }
                        val isPrerelease = rel.optBoolean("prerelease", false)
                        val publishedAt = rel.optString("published_at", "")
                        val vCode = tag.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

                        var apkUrl = ""
                        val assets = rel.optJSONArray("assets")
                        if (assets != null) {
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val assetName = asset.optString("name", "")
                                if (assetName.endsWith(".apk", ignoreCase = true)) {
                                    val url = asset.optString("browser_download_url", "")
                                    if (assetName.equals("app-release.apk", ignoreCase = true)) {
                                        apkUrl = url
                                        break
                                    } else if (apkUrl.isEmpty()) {
                                        apkUrl = url
                                    }
                                }
                            }
                        }

                        if (apkUrl.isNotEmpty()) {
                            list.add(ReleaseInfo(tag, name, isPrerelease, vCode, apkUrl, publishedAt))
                        }
                    }
                    onResult(list)
                } catch (e: Exception) {
                    Log.e("UpdateChecker", "Error parsing releases list", e)
                    onError(e.message ?: "Failed to parse releases")
                }
            },
            { error ->
                Log.e("UpdateChecker", "Error fetching releases list: ${error.message}")
                onError(error.message ?: "Network error")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> = githubHeaders()
        }
        request.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(request)
    }

    /**
     * Checks for the latest standard (non-beta) release and offers to switch to it,
     * even if it means installing a build with a lower version code than a beta
     * the device may currently be running (e.g. right after Beta Updates is turned off).
     */
    fun checkForStandardVersion(context: Context, onComplete: ((offeredSwitch: Boolean) -> Unit)? = null) {
        val queue = SharedRequestQueue.get(context)
        val request = object : StringRequest(
            Request.Method.GET, GITHUB_RELEASES_URL,
            success@{ response ->
                try {
                    val releases = org.json.JSONArray(response)
                    var standardVersionCode = 0
                    var standardUpdateUrl = ""

                    for (i in 0 until releases.length()) {
                        val rel = releases.getJSONObject(i)
                        if (rel.optBoolean("draft", false) || rel.optBoolean("prerelease", false)) continue

                        val tag = rel.optString("tag_name", "")
                        val vCode = tag.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

                        var apkUrl = ""
                        val assets = rel.optJSONArray("assets")
                        if (assets != null) {
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val assetName = asset.optString("name", "")
                                if (assetName.endsWith(".apk", ignoreCase = true)) {
                                    val url = asset.optString("browser_download_url", "")
                                    if (assetName.equals("app-release.apk", ignoreCase = true)) {
                                        apkUrl = url
                                        break
                                    } else if (apkUrl.isEmpty()) {
                                        apkUrl = url
                                    }
                                }
                            }
                        }

                        if (apkUrl.isNotEmpty() && vCode > 0) {
                            standardVersionCode = vCode
                            standardUpdateUrl = apkUrl
                            break
                        }
                    }

                    if (standardVersionCode == 0 || standardUpdateUrl.isEmpty()) {
                        onComplete?.invoke(false)
                        return@success
                    }

                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    }

                    if (standardVersionCode != currentVersionCode) {
                        showSwitchToStandardDialog(context, standardUpdateUrl, standardVersionCode, currentVersionCode)
                        onComplete?.invoke(true)
                    } else {
                        onComplete?.invoke(false)
                    }
                } catch (e: Exception) {
                    Log.e("UpdateChecker", "Error checking standard version", e)
                    onComplete?.invoke(false)
                }
            },
            { error ->
                Log.e("UpdateChecker", "Error checking standard version: ${error.message}")
                onComplete?.invoke(false)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> = githubHeaders()
        }
        request.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(request)
    }

    private fun showSwitchToStandardDialog(context: Context, updateUrl: String, standardVersionCode: Int, currentVersionCode: Int) {
        if (isShowing) return
        isShowing = true

        val comparison = if (standardVersionCode > currentVersionCode) "newer" else "different (non-beta)"
        val dialog = AlertDialog.Builder(context)
            .setTitle("Switch to Standard Build")
            .setMessage("Beta Updates is now off. A $comparison standard build ($standardVersionCode) is available — you're currently on build $currentVersionCode.\n\nInstall the standard build now?")
            .setPositiveButton("Install") { _, _ ->
                isShowing = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    try {
                        val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(permIntent)
                        Toast.makeText(context, "Please enable 'Install unknown apps' to allow updates", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e("UpdateChecker", "Could not open unknown sources settings", e)
                    }
                }
                startDownload(context, updateUrl)
            }
            .setNegativeButton("Later") { _, _ ->
                isShowing = false
            }
            .setCancelable(true)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
    }

    private fun checkForUpdatesFallback(context: Context, onComplete: ((updateFound: Boolean) -> Unit)? = null) {
        if (UPDATE_INFO_URL.isEmpty()) {
            onComplete?.invoke(false)
            return
        }
        val queue = SharedRequestQueue.get(context)
        val stringRequest = StringRequest(
            Request.Method.GET, UPDATE_INFO_URL,
            { response ->
                try {
                    val json = JSONObject(response)
                    
                    val pref = PreferenceManager.getDefaultSharedPreferences(context)
                    val isBetaUser = pref.getBoolean("beta_updates", false)
                    
                    val standardVersionCode = json.optInt("versionCode", 0)
                    val standardUpdateUrl = json.optString("updateUrl", "")
                    
                    val betaVersionCode = json.optInt("betaVersionCode", 0)
                    val betaUpdateUrl = json.optString("betaUpdateUrl", "")

                    var latestVersionCode = standardVersionCode
                    var updateUrl = standardUpdateUrl
                    var isFinalBeta = false

                    if (isBetaUser && betaVersionCode >= standardVersionCode) {
                        latestVersionCode = betaVersionCode
                        updateUrl = betaUpdateUrl
                        isFinalBeta = true
                    }

                    if (latestVersionCode == 0) {
                        checkForUpdatesLegacy(context, json, onComplete)
                        return@StringRequest
                    }

                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    }

                    if (latestVersionCode > currentVersionCode) {
                        showUpdateDialog(context, updateUrl, isFinalBeta)
                        onComplete?.invoke(true)
                    } else {
                        onComplete?.invoke(false)
                    }
                } catch (e: Exception) {
                    Log.e("UpdateChecker", "Error parsing update info in fallback", e)
                    onComplete?.invoke(false)
                }
            },
            { error ->
                Log.e("UpdateChecker", "Error checking for updates in fallback", error)
                onComplete?.invoke(false)
            }
        )
        
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        
        queue.add(stringRequest)
    }

    private fun checkForUpdatesLegacy(context: Context, json: JSONObject, onComplete: ((updateFound: Boolean) -> Unit)? = null) {
        try {
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
                showUpdateDialog(context, updateUrl, false)
                onComplete?.invoke(true)
            } else {
                onComplete?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error in legacy update check", e)
            onComplete?.invoke(false)
        }
    }

    private fun showUpdateDialog(context: Context, updateUrl: String, isBeta: Boolean) {
        if (isShowing) return
        isShowing = true

        val title = if (isBeta) "Beta Update Available" else "Update Available"
        val message = if (isBeta) 
            "A newer beta version of the app is available. Install now?" 
            else "A newer version of the app is available. Download and install now?"

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Update") { _, _ ->
                isShowing = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    try {
                        val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(permIntent)
                        Toast.makeText(context, "Please enable 'Install unknown apps' to allow updates", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e("UpdateChecker", "Could not open unknown sources settings", e)
                    }
                }
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

    fun startDownload(context: Context, url: String) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AmritaCafe_update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(if (url.contains("beta", true)) "Downloading Amrita Cafe Beta" else "Downloading Amrita Cafe Update")
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
                    try {
                        context.applicationContext.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // ignore if already unregistered
                    }

                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            if (isValidApk(destination)) {
                                Log.d("UpdateChecker", "Download success. Size: ${destination.length()} bytes")
                                Toast.makeText(context, "Download complete. Starting update...", Toast.LENGTH_SHORT).show()
                                installApk(context, destination)
                            } else {
                                val size = destination.length()
                                showFallbackDialog(context, url, "Invalid file received ($size bytes). The link might not be a direct download.")
                            }
                        } else {
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                            showFallbackDialog(context, url, "Download failed (Error code: $reason).")
                        }
                    }
                    cursor?.close()
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.applicationContext.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
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
        if (!file.exists()) {
            Log.e("UpdateChecker", "APK file not found: ${file.absolutePath}")
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        // For Android 8.0+ we need to check if we can actually request installs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please allow 'Install unknown apps' and try again", Toast.LENGTH_LONG).show()
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            Log.d("UpdateChecker", "Launching installer for: ${file.absolutePath}")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error starting installation", e)
            Toast.makeText(context, "Failed to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun checkPendingInstall(context: Context) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AmritaCafe_update.apk")
        if (isValidApk(destination) && downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                    installApk(context, destination)
                }
            }
            cursor?.close()
        }
    }
}