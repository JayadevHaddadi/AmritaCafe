package edu.amrita.amritacafe.model

import edu.amrita.amritacafe.menu.OrderItem
import java.util.*

data class Order(val orderNumber: Int, val orderItems: List<OrderItem>, val orderTime : String = Calendar.getInstance(TimeZone.getDefault()).time.run {
    hours.toString().padStart(2) + ":" + minutes.toString().padStart(2, '0')
}) {

    fun finalize() = listOf(this)

}