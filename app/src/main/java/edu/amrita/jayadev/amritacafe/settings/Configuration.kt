
package edu.amrita.jayadev.amritacafe.settings


import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import edu.amrita.jayadev.amritacafe.menu.MenuItem
import kotlinx.serialization.Serializable
import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import edu.amrita.jayadev.amritacafe.menu.Availability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.parse
import java.io.File

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

    private val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))
    private fun buildMenu() = json.parse(
        MenuItem.serializer().list,
        preferences.getString(MENU_JSON, "[]")!!
    )

    private var mealChangedListeners = mutableListOf<() -> Unit>()

    fun registerMealChangedListener(block : () -> Unit) {
        mealChangedListeners.add(block)
    }

    private val poopSauce = SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences, name: String ->
        println("$name value changed")
        if (name == MEAL) {
            GlobalScope.launch {
                mealChangedListeners.forEach { fn -> fn() }
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

    val receiptPrinterConnStr
        get() = "TCP:" + preferences.getString(IP_RECEIPT_PRINTER, "")!!

    val kitchenPrinterConnStr
        get() = "TCP:" + preferences.getString(IP_KITCEN_PRINTER, "")!!

    init {
        preferences.registerOnSharedPreferenceChangeListener { preferences, string ->
            if (string == MENU_JSON) {
                fullMenu = buildMenu()
            }
        }
    }

    companion object {
        const val MENU_RESET = "menu_reset"
        const val MENU_JSON = "menu_json"
        const val UPDATE_NOW = "update_now"
        const val UPDATE_URL = "update_url"
        const val IP_KITCEN_PRINTER = "kitchen_printer_ip"
        const val IP_RECEIPT_PRINTER = "receipt_printer_ip"
        const val MEAL = "meal"
        const val ORDER_NUMBER_RANGE = "order_number_range"
    }
}

