package edu.amrita.amritacafe.settings


import android.content.SharedPreferences
import androidx.core.content.edit

data class Configuration(private val preferences: SharedPreferences) {

    data class TextConfig(val titleSize: Int, val textSize: Int, val lineFeed: Int)

    var isBreakfastTime
        get() = preferences.getBoolean(IS_BREAKFAST_MENU_KEY, true)
        set(value) {
            preferences.edit {
                putBoolean(IS_BREAKFAST_MENU_KEY, value)
                apply()
            }
        }

    val testing get() = preferences.getBoolean(TESTING, false)

    val textConfig
        get() = if (testing) {
            TextConfig(1, 1, 0)
        } else {
            TextConfig(3, 2, 1)
        }

    fun toggleName() {
        preferences.edit {
            putBoolean(SHOW_FULL_NAMES, !showMenuItemNames)
            apply()
        }
    }

    val showMenuItemNames get() = preferences.getBoolean(SHOW_FULL_NAMES, false)

    var receiptPrinterConnStr
        get() = "TCP:" + preferences.getString(IP_RECEIPT_PRINTER, "192.168.0.116")!!
        set(value) {
            preferences.edit().putString(IP_RECEIPT_PRINTER, value)
        }

    var kitchenPrinterConnStr
        get() = "TCP:" + preferences.getString(IP_KITCEN_PRINTER, "192.168.0.11")!!
        set(value) {
            preferences.edit().putString(IP_KITCEN_PRINTER, value)
        }

    companion object {
        const val MENU_US_KEY = "menu_us_key"
        const val USERS_KEY = "users_json"
        const val SHOW_FULL_NAMES = "show_names"
        const val MENU_RESET = "menu_reset"
        const val MENU_JSON = "menu_json"
        const val UPDATE_NOW = "update_now"
        const val IP_KITCEN_PRINTER = "kitchen_printer_ip"
        const val IP_RECEIPT_PRINTER = "receipt_printer_ip"
        const val MEAL = "meal"
        const val ORDER_NUMBER_RANGE = "order_number_range"
        const val COLUMN_NUMBER_RANGE = "column_number_range"
        const val TESTING = "testing"
        const val IS_BREAKFAST_MENU_KEY = "SHOW_BREAKFAST_MENU"
    }
}