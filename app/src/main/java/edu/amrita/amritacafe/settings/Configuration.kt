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

    var columns
        get() = preferences.getInt(COLUMNS_AMOUNT, DEFAULT_COLUMNS_AMOUNT)
        set(value) {
            preferences.edit {
                putInt(COLUMNS_AMOUNT, value)
                apply()
            }
        }

    var testing
        get() = preferences.getBoolean(TESTING, false)
        set(value) {
            preferences.edit {
                putBoolean(TESTING, value)
            }
        }

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

    val receiptPrinterConnStr
        get() = "TCP:" + receiptPrinterIP

    var receiptPrinterIP
        get() = preferences.getString(IP_RECEIPT_PRINTER, "192.168.0.102")!!
        set(value) {
            preferences.edit().putString(IP_RECEIPT_PRINTER, value).apply()
        }

    val kitchenPrinterConnStr
        get() = "TCP:" + kitchenPrinterIP

    var kitchenPrinterIP
        get() = preferences.getString(IP_KITCEN_PRINTER, "192.168.0.11")!!
        set(value) {
            preferences.edit().putString(IP_KITCEN_PRINTER, value).apply()
        }

    var rangeFrom
        get() = preferences.getInt(RANGE_FROM, RANGE_FROM_DEFAULT)
        set(value) {
            preferences.edit().putInt(RANGE_FROM, value).apply()
        }

    var rangeTo
        get() = preferences.getInt(RANGE_TO, RANGE_TO_DEFAULT)
        set(value) {
            preferences.edit().putInt(RANGE_TO, value).apply()
        }

    companion object {
        const val RANGE_FROM = "range from"
        const val RANGE_FROM_DEFAULT = 1
        const val RANGE_TO = "range to"
        const val RANGE_TO_DEFAULT = 999
        const val SHOW_FULL_NAMES = "show_names"
        const val IP_KITCEN_PRINTER = "kitchen_printer_ip"
        const val IP_RECEIPT_PRINTER = "receipt_printer_ip"
        const val COLUMN_NUMBER_RANGE = "column_number_range"
        const val TESTING = "testing"
        const val IS_BREAKFAST_MENU_KEY = "SHOW_BREAKFAST_MENU"
        const val COLUMNS_AMOUNT = "COLUMNS_AMOUNT"
        const val DEFAULT_COLUMNS_AMOUNT = 11
    }
}