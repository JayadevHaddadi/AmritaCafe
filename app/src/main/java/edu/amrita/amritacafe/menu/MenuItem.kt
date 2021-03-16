package edu.amrita.amritacafe.menu

import kotlinx.serialization.Serializable

@Serializable
data class MenuItemUS(
    val name: String,
    val code: String,
    val price: Float,
    val category: String
)
//{
//    var code: String = category + code
//asdasd
//    sasd
//}
//sad