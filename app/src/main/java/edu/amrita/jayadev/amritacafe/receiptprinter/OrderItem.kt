package edu.amrita.jayadev.amritacafe.receiptprinter

data class OrderItem(
    val name: String,
    var quantity: Int,
    var totalPrice: Int,
    var comment: String = ""
)

