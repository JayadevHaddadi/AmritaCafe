package edu.amrita.jayadev.amritacafe.model

import edu.amrita.jayadev.amritacafe.menu.Location
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderNumberService
import kotlinx.coroutines.runBlocking

data class Order(val orderNumber: Int, val orderItems: List<OrderItem>) {
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