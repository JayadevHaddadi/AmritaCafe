package edu.amrita.jayadev.amritacafe.menu

data class RegularOrderItem(
    override val menuItem: MenuItem,
    override val quantity: Int = 1,
    override val comment: String = "",
    val toppings : List<Topping> = emptyList()
) : OrderItem {
    override val totalPrice: Int
        get() = quantity * menuItem.price + toppings.map { it.totalPrice }.sum()
    override fun editComment(newComment: String) = copy(comment = newComment)
    override fun increment() = copy(quantity = quantity + 1)
    fun addToppings(newToppings : List<Topping>) = copy(toppings = toppings + newToppings)
}

