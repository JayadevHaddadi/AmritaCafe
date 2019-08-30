
package edu.amrita.amritacafe.settings


import edu.amrita.amritacafe.menu.MenuItem
import android.content.SharedPreferences
import androidx.core.content.edit
import edu.amrita.amritacafe.menu.Availability
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list

val List<MenuItem>.breakfastMenu get() = this.filter { it.availability != Availability.LunchDinner }
val List<MenuItem>.lunchDinnerMenu get() = this.filter { it.availability != Availability.Breakfast }
/**
 * Maintains configuration for the application.
 *
 * Grabs JSON from remote.
 * Deserializes into a configuration object.
 * Stores a copy locally.
 * On HTTP Error, returns the locally cached copy.
 * Defaults to the defaultConfiguration for testing.
 *
 */
data class Configuration (private val preferences: SharedPreferences) {

    data class TextConfig(val titleSize : Int, val textSize : Int, val lineFeed : Int)


    val testing get() = preferences.getBoolean(TESTING, false)
    val textConfig get() = if (testing) {
        TextConfig(1, 1, 0)
    } else {
        TextConfig(3, 2, 1)
    }

    private val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))
    private fun buildMenu() = json.parse(
        MenuItem.serializer().list,
        preferences.getString(MENU_JSON, "[]")!!
    )

    private var menuChangedListeners = mutableListOf<() -> Unit>()

    fun registerMenuChangedListener(block : () -> Unit) {
        menuChangedListeners.add(block)
    }

    private val poopSauce = SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences, name: String ->
        println("$name value changed")
        if (name in listOf(MEAL, MENU_JSON, SHOW_FULL_NAMES)) {
            GlobalScope.launch {
                menuChangedListeners.forEach { fn -> fn() }
            }
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(poopSauce)
    }

    var fullMenu : List<MenuItem> = buildMenu()
        private set

    fun toggleMeal() {
        currentMeal =
            if (currentMeal == Availability.Breakfast) Availability.LunchDinner
            else Availability.Breakfast
    }

    var currentMeal get() = Availability.valueOf(preferences.getString(MEAL, "Breakfast")!!)
        set(value) {
            preferences.edit {
                putString(MEAL, value.name)
                apply()
            }
        }

    val currentMenu get() = fullMenu.filter {
        it.availability in listOf(Availability.All, currentMeal)
    }

    val showMenuItemNames get() = preferences.getBoolean(SHOW_FULL_NAMES, false)

    val receiptPrinterConnStr
        get() = "TCP:" + preferences.getString(IP_RECEIPT_PRINTER, "")!!

    val kitchenPrinterConnStr
        get() = "TCP:" + preferences.getString(IP_KITCEN_PRINTER, "")!!

    init {
        preferences.registerOnSharedPreferenceChangeListener { _, string ->
            if (string == MENU_JSON) {
                fullMenu = buildMenu()
            }
        }
    }

    companion object {
        const val SHOW_FULL_NAMES = "show_names"
        const val MENU_RESET = "menu_reset"
        const val MENU_JSON = "menu_json"
        const val UPDATE_NOW = "update_now"
        const val UPDATE_URL = "update_url"
        const val IP_KITCEN_PRINTER = "kitchen_printer_ip"
        const val IP_RECEIPT_PRINTER = "receipt_printer_ip"
        const val MEAL = "meal"
        const val ORDER_NUMBER_RANGE = "order_number_range"
        const val TESTING = "testing"
    }
}

