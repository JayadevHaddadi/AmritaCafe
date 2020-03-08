package edu.amrita.amritacafe.menu

import kotlinx.serialization.Serializable

@Serializable
data class MenuItem(
    val name: String,
    val code: String,
    val price: Float,
    val availability: Availability,
    val category: Category
)

@Serializable
data class MenuItemUS(
    val name: String,
    val code: String,
    val price: Float,
    val category: String
)
