package edu.amrita.amritacafe.menu

import java.util.*

data class RegularOrderItem(
    override val menuItem: MenuItem,
    override val quantity: Int = 1,
    override var comment: String = "",
    override var costMultiplier: Float = 1f,
    val id: UUID = UUID.randomUUID()
) : OrderItem {
    override val totalPrice: Int
        get() = Math.round(quantity * menuItem.price * costMultiplier)
    override fun editComment(newComment: String) = copy(comment = newComment)
    override fun increment() = copy(quantity = quantity + 1)
}

