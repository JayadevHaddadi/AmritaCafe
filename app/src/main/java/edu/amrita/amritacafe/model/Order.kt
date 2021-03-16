package edu.amrita.amritacafe.model

import edu.amrita.amritacafe.menu.RegularOrderItem
import kotlinx.coroutines.runBlocking
import java.util.*

data class Order(
    val orderNumber: Int, val orderItems: List<RegularOrderItem>,
    val orderTime: String = Calendar.getInstance(TimeZone.getDefault()).time.run {
        hours.toString().padStart(2) + ":" + minutes.toString().padStart(2, '0')
    }, val sum: Double = orderItems.map { it.finalPrice }.sum().toDouble()
) {

//    fun finalize() = listOf(this)

    private fun List<OrderItem>.collectToppings() : List<RegularOrderItem> {
        return filterIsInstance<RegularOrderItem>().map { orderItem ->
            orderItem.addToppings(
                filter {
                    it is Topping && it.toppidngFor.id == orderItem.id
                }.map {
                    it as Topping
                }
            )
        }
    }

    fun collectToppings() = copy(orderItems = orderItems.collectToppings())

    fun split(orderNumberService: OrderNumberService) = if (hasPizzaAndGrillItems) {
        listOf(
            Order(
                orderNumber,
                orderItems.filter { it.menuItem.category != "Pizza" }
            ),
            Order(
                runBlocking { orderNumberService.next() },
                orderItems.filter { it.menuItem.category == "Pizza" }
            )
        )
    } else {
        listOf(this)
    }
    private val hasPizzaAndGrillItems get() = orderItems.map {
        it.menuItem.category
    }.containsAll(pizzaAndGrill)

    companion object {
        private val pizzaAndGrill = listOf("Pizza", "Grill")
    }

}