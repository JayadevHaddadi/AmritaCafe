package edu.amrita.amritacafe.menu

import kotlin.math.max

/** Free items a renunciate gets. Pure logic so it can be unit tested. */
object RenunciateRules {

    /** Renunciates get ONE of these free — always the most expensive one in the order. */
    val FREE_EXTRA_CHOICES = setOf("Curd", "Milk", "Egg")

    fun apply(items: List<RegularOrderItem>, isRenunciate: Boolean) {
        if (!isRenunciate) {
            items.forEach {
                it.quantityAsRenounciate = it.quantity
                it.renounciateEffected = false
            }
            return
        }

        items.forEach { it.quantityAsRenounciate = it.quantity }

        // Only one of Curd / Milk / Egg is free: pick the priciest so the discount is maximal.
        items.filter { it.menuItem.name in FREE_EXTRA_CHOICES && it.quantity > 0 }
            .maxByOrNull { it.menuItem.price }
            ?.let {
                it.quantityAsRenounciate = it.quantity - 1
                it.renounciateEffected = true
            }

        var totalRenunciateItems = 0
        items.forEach {
            if (it.menuItem.name == "Dressing" || it.menuItem.name == "Beschameal")
                it.quantityAsRenounciate = it.quantity - 1
            else if (it.menuItem.name == "Bread") {
                it.quantityAsRenounciate = max(it.quantity - 2, 0)
            } else if (it.menuItem.category == "LUNCH/DINNER (R)" && totalRenunciateItems < 3) {
                totalRenunciateItems += 1
                it.quantityAsRenounciate = it.quantity - 1
            }
            // 5 iddly or dosa are free for renunciates
            else if (it.menuItem.name == "Iddly" || it.menuItem.name == "Dosa") {
                it.quantityAsRenounciate = max(it.quantity - 5, 0)
            } else if (it.menuItem.name == "Sambar Only/Ex") {
                it.quantityAsRenounciate = 0
            } else if (it.menuItem.name == "Upma") {
                it.quantityAsRenounciate = max(it.quantity - 2, 0)
            } else if (it.menuItem.name == "Sprouts") {
                it.quantityAsRenounciate = max(it.quantity - 1, 0)
            }
            if (it.quantityAsRenounciate != it.quantity)
                it.renounciateEffected = true
        }
    }
}
