
package edu.amrita.jayadev.amritacafe.menu

enum class Category(private val altName : String? = null) {
    Pizza,
    Burger,
    Sandwich("Sand wich"),
    Fries,
    Pasta,
    Eggs,
    Salad,
    Toast,
    Breakfast("Break fast"),
    Topping("Top with"),
    Kitcheri("Kitch"),
    Side;

    val displayName get() = altName ?: name
}