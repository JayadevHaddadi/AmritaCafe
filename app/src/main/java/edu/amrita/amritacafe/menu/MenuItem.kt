
package edu.amrita.amritacafe.menu

import kotlinx.serialization.Serializable

@Serializable
data class MenuItem(val name: String,
                    val code: String,
                    val price: Int,
                    val availability: Availability,
                    val category: Category) {
}
