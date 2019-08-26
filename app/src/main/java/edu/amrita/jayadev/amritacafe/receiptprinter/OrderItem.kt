package edu.amrita.jayadev.amritacafe.receiptprinter

import edu.amrita.jayadev.amritacafe.menu.MenuItem


data class OrderItem(
    val menuItem: MenuItem,
    val quantity: Int,
    var comment: String = ""
) {
    fun increment() = copy(quantity = quantity + 1)

    val totalPrice get() = menuItem.price * quantity
}

