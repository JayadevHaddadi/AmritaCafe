package edu.amrita.amritacafe.history

import android.content.Context
import android.util.Log
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.HistoricalOrder
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.model.PrintStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HistoryPersistence {
    private const val TAG = "HistoryPersistence"
    private const val FILE_NAME = "order_history.json"
    private const val MAX_HISTORY_ITEMS = 20

    @Synchronized
    fun saveHistory(context: Context, history: List<HistoricalOrder>) {
        try {
            val jsonArray = JSONArray()
            val listToSave = if (history.size > MAX_HISTORY_ITEMS) {
                history.takeLast(MAX_HISTORY_ITEMS)
            } else {
                history
            }

            listToSave.forEach { hist ->
                val orderObj = JSONObject().apply {
                    put("orderNumber", hist.order.orderNumber)
                    put("orderLongTime", hist.order.orderLongTime)
                    put("orderTime", hist.order.orderTime)
                    put("isGpay", hist.order.isGpay)
                    put("isRenunciate", hist.order.isRenunciate)
                    put("kitchenPrinted", hist.KitchenPrinted.name)
                    put("recipePrinted", hist.RecipePrinted.name)

                    val itemsArray = JSONArray()
                    hist.order.orderItems.forEach { item ->
                        val itemObj = JSONObject().apply {
                            put("name", item.menuItem.name)
                            put("code", item.menuItem.code)
                            put("price", item.menuItem.price.toDouble())
                            put("category", item.menuItem.category)
                            put("quantity", item.quantity)
                            put("comment", item.comment)
                            put("quantityAsRenounciate", item.quantityAsRenounciate)
                            put("renounciateEffected", item.renounciateEffected)

                            if (item.toppings.isNotEmpty()) {
                                val toppingsArray = JSONArray()
                                item.toppings.forEach { top ->
                                    toppingsArray.put(JSONObject().apply {
                                        put("name", top.menuItem.name)
                                        put("code", top.menuItem.code)
                                        put("price", top.menuItem.price.toDouble())
                                        put("category", top.menuItem.category)
                                        put("quantity", top.quantity)
                                        put("comment", top.comment)
                                    })
                                }
                                put("toppings", toppingsArray)
                            }
                        }
                        itemsArray.put(itemObj)
                    }
                    put("orderItems", itemsArray)
                }
                jsonArray.put(orderObj)
            }

            val file = File(context.filesDir, FILE_NAME)
            val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
            tempFile.writeText(jsonArray.toString())
            if (!tempFile.renameTo(file)) {
                if (file.delete()) {
                    tempFile.renameTo(file)
                } else {
                    file.writeText(jsonArray.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save history", e)
        }
    }

    @Synchronized
    fun loadHistory(context: Context): MutableList<HistoricalOrder> {
        val result = mutableListOf<HistoricalOrder>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return result

            val content = file.readText()
            if (content.isBlank()) return result

            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val orderObj = jsonArray.getJSONObject(i)
                val orderNumber = orderObj.optInt("orderNumber", 0)
                val orderLongTime = orderObj.optLong("orderLongTime", System.currentTimeMillis())
                val orderTime = orderObj.optString("orderTime", "")
                val isGpay = orderObj.optBoolean("isGpay", false)
                val isRenunciate = orderObj.optBoolean("isRenunciate", false)
                val kitchenPrintedStr = orderObj.optString("kitchenPrinted", PrintStatus.NONE.name)
                val recipePrintedStr = orderObj.optString("recipePrinted", PrintStatus.NONE.name)

                val kitchenStatus = try {
                    PrintStatus.valueOf(kitchenPrintedStr)
                } catch (_: Exception) {
                    PrintStatus.NONE
                }
                val recipeStatus = try {
                    PrintStatus.valueOf(recipePrintedStr)
                } catch (_: Exception) {
                    PrintStatus.NONE
                }

                val itemsArray = orderObj.optJSONArray("orderItems") ?: JSONArray()
                val orderItems = mutableListOf<RegularOrderItem>()

                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val name = itemObj.optString("name", "")
                    val code = itemObj.optString("code", "")
                    val price = itemObj.optDouble("price", 0.0).toFloat()
                    val category = itemObj.optString("category", "")
                    val quantity = itemObj.optInt("quantity", 1)
                    val comment = itemObj.optString("comment", "")
                    val quantityAsRenounciate = itemObj.optInt("quantityAsRenounciate", 1)
                    val renounciateEffected = itemObj.optBoolean("renounciateEffected", false)

                    val menuItem = MenuItem(name, code, price, category)
                    val orderItem = RegularOrderItem(
                        menuItem = menuItem,
                        quantity = quantity,
                        comment = comment,
                        quantityAsRenounciate = quantityAsRenounciate,
                        renounciateEffected = renounciateEffected
                    )

                    val toppingsArray = itemObj.optJSONArray("toppings")
                    if (toppingsArray != null) {
                        for (k in 0 until toppingsArray.length()) {
                            val topObj = toppingsArray.getJSONObject(k)
                            val topItem = RegularOrderItem(
                                menuItem = MenuItem(
                                    topObj.optString("name", ""),
                                    topObj.optString("code", ""),
                                    topObj.optDouble("price", 0.0).toFloat(),
                                    topObj.optString("category", "")
                                ),
                                quantity = topObj.optInt("quantity", 1),
                                comment = topObj.optString("comment", "")
                            )
                            orderItem.addTopping(topItem)
                        }
                    }
                    orderItems.add(orderItem)
                }

                val order = Order(
                    orderNumber = orderNumber,
                    orderItems = orderItems,
                    orderLongTime = orderLongTime,
                    orderTime = orderTime,
                    isGpay = isGpay,
                    isRenunciate = isRenunciate
                )
                result.add(HistoricalOrder(order, kitchenStatus, recipeStatus))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load history", e)
        }
        return result
    }
}
