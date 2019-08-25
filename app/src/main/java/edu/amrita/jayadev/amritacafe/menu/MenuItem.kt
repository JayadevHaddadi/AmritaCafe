
package edu.amrita.jayadev.amritacafe.menu

import kotlinx.serialization.Serializable

@Serializable
data class MenuItem(val name: String,
                    val code: String,
                    val price: Int,
                    val availability: Availability,
                    val location: Location,
                    val category: Category) {
    companion object {
        val default = MenuItem(
            "",
            "", 0,
            Availability.All,
            Location.Grill,
            Category.Burger)
    }
}
