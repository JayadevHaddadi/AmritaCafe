package edu.amrita.jayadev.amritacafe.model

import edu.amrita.jayadev.amritacafe.menu.Location
import edu.amrita.jayadev.amritacafe.menu.OrderItem
import edu.amrita.jayadev.amritacafe.menu.RegularOrderItem
import edu.amrita.jayadev.amritacafe.menu.Topping
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderNumberService
import kotlinx.coroutines.runBlocking
import java.util.*

data class Order(val orderNumber: Int, val orderItems: List<OrderItem>, val orderTime : String = Calendar.getInstance(TimeZone.getDefault()).time.run {
    hours.toString().padStart(2) + ":" + minutes.toString().padStart(2, '0')
}) {
    private fun List<OrderItem>.collectToppings() : List<RegularOrderItem> {
        return filterIsInstance<RegularOrderItem>().map { orderItem ->
            orderItem.addToppings(
                filter {
                    it is Topping && it.toppingFor.id == orderItem.id
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
                orderItems.filter { it.menuItem.location != Location.Pizza }
            ),
            Order(
                runBlocking { orderNumberService.next() },
                orderItems.filter { it.menuItem.location == Location.Pizza }
            )
        )
    } else {
        listOf(this)
    }

    private val hasPizzaAndGrillItems get() = orderItems.map {
        it.menuItem.location
    }.containsAll(pizzaAndGrill)

    companion object {
        private val pizzaAndGrill = listOf(Location.Pizza, Location.Grill)
    }

}