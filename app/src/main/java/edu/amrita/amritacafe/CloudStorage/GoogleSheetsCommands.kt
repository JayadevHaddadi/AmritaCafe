package edu.amrita.amritacafe.CloudStorage

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

fun sendToSheets(
    orders: List<Order>, configuration: Configuration, context: Context
) {
    val jsonData = JSONObject()
    val jsonArray = JSONArray()
    var orderTime = 0L
    var myOrderNumber = 0

    orders.forEach { (orderNumber, orderItems, time) ->
        orderTime = time
        myOrderNumber = orderNumber

        orderItems.forEach {
            val jsonItem = JSONObject()
//                val r = if(it.renounciateEffected)  "R" else ""
            jsonItem.put("name", it.menuItem.name)
            jsonItem.put("quantity", it.quantity)
            jsonItem.put("total", it.totalPrice())
            jsonItem.put("cost", it.menuItem.price)
            jsonItem.put("renounciate", if (it.renounciateEffected) "R" else "normal")
            jsonArray.put(jsonItem)
        }
    }

    val url =
        "https://script.google.com/macros/s/AKfycbz9Jbpdz8VVG8Yo23F0-ti5xuUflFmEOugdV8sVyVtlGyjlNyD5R1HwFfLwAwoWqd26Xg/exec" // Replace with your actual URL

    jsonData.put("items", jsonArray)
    try {
        jsonData.put("time", orderTime)
        jsonData.put("tablet", configuration.tabletName)
        jsonData.put("order", myOrderNumber.toString())
        jsonData.put("isGpay", orders.any { it.isGpay })
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    val jsonString = jsonData.toString()
    val requestQueue = Volley.newRequestQueue(context)
    val stringRequest = object : StringRequest(
        Method.POST,
        url, // Replace with your actual URL
        { response ->
            // Handle successful response
            Log.d("Connection", "Response: $response")
        },
        { error ->
            // Handle error
            Log.e("Connection", "Error: ${error.message}")
        }) {
        override fun getBodyContentType(): String {
            return "application/json; charset=utf-8"
        }

        override fun getBody(): ByteArray {
            return try {
                jsonString.toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "Error encoding JSON: $e")
                return ByteArray(0)
            }
        }
    }
    stringRequest.setRetryPolicy(
        DefaultRetryPolicy(
            0,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
    )

    // Add the request to the queue
    requestQueue.add(stringRequest)

}