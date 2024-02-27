package edu.amrita.amritacafe.menu

data class RegularOrderItem(
    val menuItem: MenuItem,
    var quantity: Int = 1,
    var comment: String = "",
    var toppings: MutableList<RegularOrderItem> = mutableListOf()
) {
    val priceWithoutExtras: Int by lazy {
        (quantity * menuItem.price).toInt()
    }

    val extrasPrice: Int by lazy {
        toppings.map { it.priceWithoutExtras }.sum()
    }

    val totalPrice: Int by lazy {
        priceWithoutExtras + extrasPrice// if (toppings.isEmpty()) 0 else toppingsPrice
    }

//    val totalForRenunciates: Int by lazy {
//        if(menuItem.category.equals("LUNCH/DINNER",true)) ((quantity-1) * menuItem.price).toInt() + toppingsPrice else totalPrice
//    }

    val code: String by lazy {
         menuItem.code
    }

    fun editComment(newComment: String) = copy(comment = newComment)
    fun increment() = copy(quantity = quantity + 1)
    fun addTopping(topping: RegularOrderItem) {
        toppings.add(topping)
    }
}

