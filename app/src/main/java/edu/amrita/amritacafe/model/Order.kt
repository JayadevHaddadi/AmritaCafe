package edu.amrita.amritacafe.model

import edu.amrita.amritacafe.menu.RegularOrderItem
import java.util.*

data class Order(
    val orderNumber: Int, var orderItems: List<RegularOrderItem>,
    val orderTime: String = Calendar.getInstance(TimeZone.getDefault()).time.run {
        hours.toString().padStart(2) + ":" + minutes.toString().padStart(2, '0')
    }, val sum: Double = orderItems.map { it.priceWithoutToppings }.sum().toDouble()
)