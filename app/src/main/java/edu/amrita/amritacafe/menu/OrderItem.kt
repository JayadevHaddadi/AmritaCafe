package edu.amrita.amritacafe.menu

interface OrderItem {
    val menuItem : MenuItem
    val quantity : Int
    val comment : String
    val totalPrice get() = menuItem.price * quantity
    fun increment() : OrderItem
    fun editComment(newComment : String) : OrderItem
}
