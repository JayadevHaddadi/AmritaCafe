package edu.amrita.amritacafe.CloudStorage

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import edu.amrita.amritacafe.BuildConfig
import edu.amrita.amritacafe.activities.ConnectionIndicator
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

fun getOrderScriptUrl(): String {
    return if (BuildConfig.ORDER_SCRIPT_URL.isNotEmpty()) {
        BuildConfig.ORDER_SCRIPT_URL
    } else {
        "https://script.google.com/macros/s/AKfycbz9Jbpdz8VVG8Yo23F0-ti5xuUflFmEOugdV8sVyVtlGyjlNyD5R1HwFfLwAwoWqd26Xg/exec" // Replace with your actual URL
    }
}

fun sendToSheets(
    orders: List<Order>, configuration: Configuration, context: Context
) {
    val url = getOrderScriptUrl()

    orders.forEach { order ->
        val jsonData = JSONObject()
        val jsonArray = JSONArray()

        order.orderItems.forEach {
            val jsonItem = JSONObject()
//                val r = if(it.renounciateEffected)  "R" else ""
            jsonItem.put("name", it.menuItem.name)
            jsonItem.put("quantity", it.quantity)
            jsonItem.put("total", it.totalPrice())
            jsonItem.put("cost", it.menuItem.price)
            jsonItem.put("renounciate", if (it.renounciateEffected) "R" else "normal")
            jsonArray.put(jsonItem)
        }

        try {
            jsonData.put("time", order.orderLongTime)
            jsonData.put("tablet", configuration.tabletName)
            jsonData.put("order", order.orderNumber.toString())
            jsonData.put("isGpay", order.isGpay)
            jsonData.put("items", jsonArray)
            jsonData.put("appVersion", BuildConfig.VERSION_CODE.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val jsonString = jsonData.toString()
        
        // Add to offline queue first for safety
        OfflineOrderSync.addOrder(context, jsonString)
    }
    
    // Trigger sync
    OfflineOrderSync.syncPendingOrders(context, url)
}

fun updateGPayOnSheets(
    historicalOrder: edu.amrita.amritacafe.model.HistoricalOrder,
    configuration: Configuration,
    context: Context
) {
    val jsonData = JSONObject()
    val url = getOrderScriptUrl()

    try {
        jsonData.put("action", "updateGPay")
        jsonData.put("time", historicalOrder.order.orderLongTime)
        jsonData.put("tablet", configuration.tabletName)
        jsonData.put("order", historicalOrder.order.orderNumber.toString())
        jsonData.put("isGpay", historicalOrder.order.isGpay)
        jsonData.put("appVersion", BuildConfig.VERSION_CODE.toString())
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    
    val jsonString = jsonData.toString()
    val requestQueue = Volley.newRequestQueue(context)
    val stringRequest = object : StringRequest(
        Method.POST,
        url,
        { response ->
            Log.d("Connection", "Update GPay Response: $response")
            ConnectionIndicator.setSheetsConnected(true)
        },
        { error ->
            Log.e("Connection", "Update GPay Error: ${error.message}")
            ConnectionIndicator.setSheetsConnected(false)
            // Note: We might want an offline queue for updates too, 
            // but starting with immediate sync for simplicity.
        }) {
        override fun getBodyContentType(): String = "application/json; charset=utf-8"
        override fun getBody(): ByteArray = jsonString.toByteArray(Charsets.UTF_8)
    }
    
    stringRequest.setRetryPolicy(DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
    requestQueue.add(stringRequest)
}