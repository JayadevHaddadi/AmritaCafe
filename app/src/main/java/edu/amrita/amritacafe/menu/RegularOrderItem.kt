package edu.amrita.amritacafe.menu

data class RegularOrderItem(
    val menuItem: MenuItem,
    var quantity: Int = 1,
    var comment: String = "",
    var toppings: MutableList<RegularOrderItem> = mutableListOf(),
    var quantityAsRenounciate: Int = 1,
    var renounciateEffected: Boolean = false
) {
//    var quantity: Int = 1
//        get() = field        // getter
//        set(value) {         // setter
//            field = value
//        }

    val priceWithoutExtras: Int by lazy {
//        (quantityAsRenounciate * menuItem.price).toInt()
        println("renounciateEffected ${renounciateEffected}")
        if(renounciateEffected) {
            println("quantityAsRenounciate ${quantityAsRenounciate}")
            println("menuItem.price ${menuItem.price}")
            (quantityAsRenounciate * menuItem.price).toInt()
        }
        else
            (quantity * menuItem.price).toInt()
    }

    fun totalPrice(): Int {
        val price = if(renounciateEffected)
            (quantityAsRenounciate * menuItem.price).toInt()
        else
            (quantity * menuItem.price).toInt()
        return price + toppings.map { it.priceWithoutExtras }.sum()// if (toppings.isEmpty()) 0 else toppingsPrice
    }

    val code: String by lazy {
        menuItem.code
    }

    fun editComment(newComment: String) = copy(comment = newComment)
    fun increment() = copy(quantity = quantity + 1)
    fun addTopping(topping: RegularOrderItem) {
        toppings.add(topping)
    }
}

