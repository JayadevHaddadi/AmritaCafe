package edu.amrita.amritacafe.menu

import org.junit.Assert.assertEquals
import org.junit.Test

class RenunciateRulesTest {

    private fun item(name: String, price: Float, qty: Int = 1) =
        RegularOrderItem(MenuItem(name, name, price, "EXTRAS"), quantity = qty)

    private fun total(items: List<RegularOrderItem>) = items.sumOf { it.totalPrice() }

    @Test
    fun mostExpensiveOfCurdMilkEggIsFree_evenWhenEggComesFirst() {
        val items = listOf(item("Egg", 20f, 2), item("Curd", 50f), item("Milk", 30f))
        assertEquals(120, total(items))
        RenunciateRules.apply(items, true)
        assertEquals(70, total(items)) // curd (50) removed, not egg (20)
    }

    @Test
    fun onlyOneFreeExtra() {
        val items = listOf(item("Milk", 30f), item("Egg", 20f))
        RenunciateRules.apply(items, true)
        assertEquals(20, total(items))
    }

    @Test
    fun togglingOffRestoresFullPrice() {
        val items = listOf(item("Egg", 20f, 2), item("Curd", 50f))
        RenunciateRules.apply(items, true)
        RenunciateRules.apply(items, false)
        assertEquals(90, total(items))
    }

    @Test
    fun applyingTwiceDoesNotDoubleDiscount() {
        val items = listOf(item("Curd", 50f, 2))
        RenunciateRules.apply(items, true)
        RenunciateRules.apply(items, true)
        assertEquals(50, total(items))
    }
}
