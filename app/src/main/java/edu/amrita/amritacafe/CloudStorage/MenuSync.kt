package edu.amrita.amritacafe.CloudStorage

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import edu.amrita.amritacafe.BuildConfig
import edu.amrita.amritacafe.IO.CSVFileManager
import org.json.JSONObject
import java.net.URLEncoder

object MenuSync {
    private const val TAG = "MenuSync"

    data class MenuSyncResult(
        val totalMenus: Int,
        val successCount: Int,
        val failedMenus: List<String>
    )

    fun forceUpdateAllMenus(context: Context, onComplete: (MenuSyncResult) -> Unit) {
        val appContext = context.applicationContext
        val listUrl = BuildConfig.MENU_SCRIPT_URL
        val requestQueue = Volley.newRequestQueue(appContext)

        val listRequest = StringRequest(
            com.android.volley.Request.Method.GET,
            listUrl,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "error")
                    if (status != "success") {
                        Log.e(TAG, "Menu list fetch returned status: $status")
                        onComplete(MenuSyncResult(0, 0, emptyList()))
                        return@StringRequest
                    }

                    val sheetNamesArray = jsonResponse.optJSONArray("sheetNames")
                    val names = mutableListOf<String>()
                    if (sheetNamesArray != null) {
                        for (i in 0 until sheetNamesArray.length()) {
                            names.add(sheetNamesArray.getString(i))
                        }
                    }

                    if (names.isEmpty()) {
                        onComplete(MenuSyncResult(0, 0, emptyList()))
                        return@StringRequest
                    }

                    PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                        .putString("cached_sheet_names", names.joinToString(","))
                        .apply()

                    var remaining = names.size
                    var successCount = 0
                    val failedMenus = mutableListOf<String>()

                    names.forEach { name ->
                        fetchSingleMenu(appContext, name) { ok ->
                            synchronized(this) {
                                if (ok) successCount++ else failedMenus.add(name)
                                remaining--
                                if (remaining == 0) {
                                    onComplete(MenuSyncResult(names.size, successCount, failedMenus))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing menu list during force update", e)
                    onComplete(MenuSyncResult(0, 0, emptyList()))
                }
            },
            { error ->
                Log.e(TAG, "Error fetching menu list during force update: ${error.message}")
                onComplete(MenuSyncResult(0, 0, emptyList()))
            }
        )
        listRequest.retryPolicy = DefaultRetryPolicy(
            20000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(listRequest)
    }

    private fun fetchSingleMenu(context: Context, sheetName: String, onDone: (Boolean) -> Unit) {
        try {
            val encodedMenuName = URLEncoder.encode(sheetName, "UTF-8")
            val url = "${BuildConfig.MENU_SCRIPT_URL}?sheetName=$encodedMenuName"
            val requestQueue = Volley.newRequestQueue(context)

            val stringRequest = StringRequest(
                com.android.volley.Request.Method.GET,
                url,
                { response ->
                    try {
                        val jsonResponse = JSONObject(response)
                        val status = jsonResponse.optString("status", "error")
                        val csvData = jsonResponse.optString("data", "")
                        if (status == "success" && csvData.isNotEmpty()) {
                            val fileName = "${sheetName.replace("[\\\\/]".toRegex(), "_")}.csv"
                            val saved = CSVFileManager.saveCSV(context, fileName, csvData)
                            onDone(saved)
                        } else {
                            Log.w(TAG, "Force update failed for $sheetName: $status")
                            onDone(false)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing menu update for $sheetName", e)
                        onDone(false)
                    }
                },
                { error ->
                    Log.e(TAG, "Error fetching menu $sheetName: ${error.message}")
                    onDone(false)
                }
            )
            stringRequest.retryPolicy = DefaultRetryPolicy(15000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            requestQueue.add(stringRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching menu $sheetName", e)
            onDone(false)
        }
    }
}
