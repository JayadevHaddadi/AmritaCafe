package edu.amrita.amritacafe.menu

data class RegularOrderItem(
    val menuItem: MenuItemUS,
    var quantity: Int = 1,
    var comment: String = "",
    var toppings: MutableList<RegularOrderItem> = mutableListOf()
) {
    val finalPrice: Float by lazy {
        quantity * menuItem.price
    }

    val priceWithToppings: Float by lazy {
        finalPrice + if (toppings.isEmpty()) 0f else toppings.map { it.finalPrice }.sum()
    }

    val code: String by lazy {
//        (if(menuItem.category.equals(TOPPING,true)) " + " else "") +
         menuItem.code
    }

    fun editComment(newComment: String) = copy(comment = newComment)
    fun increment() = copy(quantity = quantity + 1)
    fun addTopping(topping: RegularOrderItem) {
        toppings.add(topping)
    }
}

