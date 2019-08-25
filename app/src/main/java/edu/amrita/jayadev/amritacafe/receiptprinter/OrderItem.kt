package edu.amrita.jayadev.amritacafe.receiptprinter

import edu.amrita.jayadev.amritacafe.menu.MenuItem


data class OrderItem(
    val menuItem: MenuItem,
    val quantity: Int,
    var comment: String = ""
) {
    val totalPrice get() = menuItem.price * quantity
}

