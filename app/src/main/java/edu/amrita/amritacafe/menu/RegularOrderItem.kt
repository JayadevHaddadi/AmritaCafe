package edu.amrita.amritacafe.menu

data class RegularOrderItem(
    val menuItem: MenuItemUS,
    var quantity: Int = 1,
    var comment: String = "",
    var toppings: MutableList<RegularOrderItem> = mutableListOf()
) {
    val priceWithoutToppings: Int by lazy {
        (quantity * menuItem.price).toInt()
    }

    val priceWithToppings: Int by lazy {
        priceWithoutToppings + if (toppings.isEmpty()) 0 else toppings.map { it.priceWithoutToppings }.sum()
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

