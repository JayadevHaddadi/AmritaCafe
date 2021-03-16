package edu.amrita.amritacafe.menu

import java.util.*

data class RegularOrderItem(
    val menuItem: MenuItemUS,
    var quantity: Int = 1,
    var comment: String = "",
    var priceMultiplier: Float = 1f,
    var toppings: MutableList<RegularOrderItem> = mutableListOf(),
    val id: UUID = UUID.randomUUID()
) {
    val finalPrice: Float by lazy {
        originalPrice * priceMultiplier
    }

    val originalPrice: Float by lazy {
        quantity * menuItem.price
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

