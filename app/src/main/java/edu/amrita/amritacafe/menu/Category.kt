
package edu.amrita.amritacafe.menu

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
    Side,
    
    Indian_Snacks,
    Curry_Corner,
    Dosa_and_More,
    Pizza_and_More,
    Raw,
    Sweets_Homemade,
    Hot_Drinks,
    Juice_and_Smoothie,
    Cold_Drinks,
    Cash_Register_Shelf;

    val displayName get() = altName ?: name
}