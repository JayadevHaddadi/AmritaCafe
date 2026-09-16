package edu.amrita.amritacafe.settings


import android.content.SharedPreferences
import androidx.core.content.edit

data class Configuration(private val preferences: SharedPreferences) {

    data class TextConfig(val titleSize: Int, val textSize: Int, val lineFeed: Int)

    var printToFile
        get() = preferences.getBoolean(DO_PRINT_CSV_HISTORY, false)
        set(value) {
            preferences.edit {
                putBoolean(DO_PRINT_CSV_HISTORY, value)
                apply()
            }
        }

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

    var betaUpdates
        get() = preferences.getBoolean(BETA_UPDATES, false)
        set(value) {
            preferences.edit {
                putBoolean(BETA_UPDATES, value)
                apply()
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
        get() = when (receiptPrinterTarget) {
            RECEIPT_TARGET_WIFI_2 -> "TCP:" + kitchenPrinterIP
            else -> "TCP:" + receiptPrinterIP
        }

    val kitchenPrinterConnStr
        get() = when (kitchenPrinterTarget) {
            KITCHEN_TARGET_WIFI_1 -> "TCP:" + receiptPrinterIP
            else -> "TCP:" + kitchenPrinterIP
        }

    val isReceiptWifi get() = receiptPrinterTarget == RECEIPT_TARGET_WIFI_1 || receiptPrinterTarget == RECEIPT_TARGET_WIFI_2
    val isReceiptBluetooth get() = receiptPrinterTarget == RECEIPT_TARGET_BLUETOOTH
    val isKitchenWifi get() = kitchenPrinterTarget == KITCHEN_TARGET_WIFI_2 || kitchenPrinterTarget == KITCHEN_TARGET_WIFI_1
    val isKitchenBluetooth get() = kitchenPrinterTarget == KITCHEN_TARGET_BLUETOOTH

    var tabletName
        get() = preferences.getString(TABLET_NAME_KEY, "Unnamed Tablet")!!
        set(value) {
            preferences.edit().putString(TABLET_NAME_KEY, value).apply()
        }
    var receiptPrinterIP
        get() = preferences.getString(IP_RECEIPT_PRINTER, "192.168.0.116")!!
        set(value) {
            preferences.edit().putString(IP_RECEIPT_PRINTER, value).apply()
        }

    var bluetoothName
        get() = preferences.getString(BLUETOOTH_NAME, "PT-280_2A69")!!
        set(value) {
            preferences.edit().putString(BLUETOOTH_NAME, value).apply()
        }

    var bluetoothAddress
        get() = preferences.getString(BLUETOOTH_ADDRESS, "")!!
        set(value) {
            preferences.edit().putString(BLUETOOTH_ADDRESS, value).apply()
        }

    var mode
        get() = preferences.getInt(MODE, 0)
        set(value) {
            preferences.edit().putInt(MODE, value).apply()
            println("JAYADEV set mode to " + value)
        }

    var wifiKeywords
        get() = preferences.getString(WIFI_KEYWORDS, "breakfast,lunch,dinner")!!
        set(value) {
            preferences.edit().putString(WIFI_KEYWORDS, value).apply()
        }

    var bluetoothKeywords
        get() = preferences.getString(BT_KEYWORDS, "cafe,canteen")!!
        set(value) {
            preferences.edit().putString(BT_KEYWORDS, value).apply()
        }

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

    var workflowMode: Int
        get() = preferences.getInt(WORKFLOW_MODE, preferences.getInt(MODE, MODE_ORDER_TAKER))
        set(value) {
            preferences.edit().putInt(WORKFLOW_MODE, value).putInt(MODE, value).apply()
        }

    var receiptPrinterTarget: Int
        get() = preferences.getInt(RECEIPT_PRINTER_TARGET, RECEIPT_TARGET_WIFI_1)
        set(value) {
            preferences.edit().putInt(RECEIPT_PRINTER_TARGET, value).apply()
        }

    var kitchenPrinterTarget: Int
        get() = preferences.getInt(KITCHEN_PRINTER_TARGET, KITCHEN_TARGET_WIFI_2)
        set(value) {
            preferences.edit().putInt(KITCHEN_PRINTER_TARGET, value).apply()
        }

    var useRawSocket
        get() = preferences.getBoolean(USE_RAW_SOCKET, true)
        set(value) {
            preferences.edit {
                putBoolean(USE_RAW_SOCKET, value)
                apply()
            }
        }

    var printAmmaQuote
        get() = preferences.getBoolean(PRINT_AMMA_QUOTE, false)
        set(value) {
            preferences.edit {
                putBoolean(PRINT_AMMA_QUOTE, value)
                apply()
            }
        }

    companion object {
        const val MODE_ORDER_TAKER = 0
        const val MODE_CASHIER = 1

        const val RECEIPT_TARGET_WIFI_1 = 0
        const val RECEIPT_TARGET_WIFI_2 = 1
        const val RECEIPT_TARGET_BLUETOOTH = 2
        const val RECEIPT_TARGET_NONE = 3

        const val KITCHEN_TARGET_WIFI_2 = 0
        const val KITCHEN_TARGET_WIFI_1 = 1
        const val KITCHEN_TARGET_BLUETOOTH = 2
        const val KITCHEN_TARGET_NONE = 3

        const val WORKFLOW_MODE = "workflow_mode"
        const val RECEIPT_PRINTER_TARGET = "receipt_printer_target"
        const val KITCHEN_PRINTER_TARGET = "kitchen_printer_target"

        const val TABLET_NAME_KEY = "tablet name"
        const val RANGE_FROM = "range from"
        const val DO_PRINT_CSV_HISTORY = "DO_PRINT_CSV_HISTORY"
        const val RANGE_FROM_DEFAULT = 1
        const val RANGE_TO = "range to"
        const val RANGE_TO_DEFAULT = 999
        const val SHOW_FULL_NAMES = "show_names"
        const val IP_KITCEN_PRINTER = "kitchen_printer_ip"
        const val IP_RECEIPT_PRINTER = "receipt_printer_ip"
        const val BLUETOOTH_NAME = "bluetooth name"
        const val BLUETOOTH_ADDRESS = "bluetooth address"
        const val MODE = "mode"
        const val WIFI_KEYWORDS = "wifi_keywords"
        const val BT_KEYWORDS = "bt_keywords"
        const val COLUMN_NUMBER_RANGE = "column_number_range"
        const val TESTING = "testing"
        const val BETA_UPDATES = "beta_updates"
        const val IS_BREAKFAST_MENU_KEY = "SHOW_BREAKFAST_MENU"
        const val COLUMNS_AMOUNT = "COLUMNS_AMOUNT"
        const val DEFAULT_COLUMNS_AMOUNT = 8 // TODO for tablet 11
        const val USE_RAW_SOCKET = "use_raw_socket"
        const val PRINT_AMMA_QUOTE = "print_amma_quote"
    }
}