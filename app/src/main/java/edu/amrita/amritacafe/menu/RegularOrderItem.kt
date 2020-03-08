package edu.amrita.amritacafe.menu

import java.util.*

data class RegularOrderItem(
    val menuItem: MenuItemUS,
    val quantity: Int = 1,
    var comment: String = "",
    var costMultiplier: Float = 1f,
    val id: UUID = UUID.randomUUID()
) {
    val totalPrice: Float
        get() = quantity * menuItem.price * costMultiplier
    fun editComment(newComment: String) = copy(comment = newComment)
    fun increment() = copy(quantity = quantity + 1)
}

