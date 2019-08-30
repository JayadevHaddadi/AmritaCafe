package edu.amrita.amritacafe.menu

data class Topping(
    override val menuItem: MenuItem,
    override val quantity: Int = 1,
    override val comment: String = "",
    val toppingFor : RegularOrderItem
) : OrderItem {
    override fun increment() = copy(quantity = quantity + 1)

    override fun editComment(newComment: String) = copy(comment = newComment.trim())
}