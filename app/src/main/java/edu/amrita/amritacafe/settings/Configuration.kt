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

    var columnsLandscape: Int
        get() = preferences.getInt(COLUMNS_AMOUNT, DEFAULT_COLUMNS_AMOUNT)
        set(value) {
            preferences.edit {
                putInt(COLUMNS_AMOUNT, value)
                apply()
            }
        }

    var columnsPortrait: Int
        get() = preferences.getInt(COLUMNS_AMOUNT_PORTRAIT, DEFAULT_COLUMNS_AMOUNT_PORTRAIT)
        set(value) {
            preferences.edit {
                putInt(COLUMNS_AMOUNT_PORTRAIT, value)
                apply()
            }
        }

    var columns: Int
        get() = columnsLandscape
        set(value) {
            columnsLandscape = value
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
    val isReceiptBluetooth1 get() = receiptPrinterTarget == RECEIPT_TARGET_BLUETOOTH_1
    val isReceiptBluetooth2 get() = receiptPrinterTarget == RECEIPT_TARGET_BLUETOOTH_2
    val isReceiptBluetooth get() = isReceiptBluetooth1 || isReceiptBluetooth2

    val isKitchenWifi get() = kitchenPrinterTarget == KITCHEN_TARGET_WIFI_2 || kitchenPrinterTarget == KITCHEN_TARGET_WIFI_1
    val isKitchenBluetooth1 get() = kitchenPrinterTarget == KITCHEN_TARGET_BLUETOOTH_1
    val isKitchenBluetooth2 get() = kitchenPrinterTarget == KITCHEN_TARGET_BLUETOOTH_2
    val isKitchenBluetooth get() = isKitchenBluetooth1 || isKitchenBluetooth2

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

    var printer1PaperSize: Int
        get() = preferences.getInt(BLUETOOTH_PAPER_SIZE, PAPER_SIZE_80MM)
        set(value) {
            preferences.edit().putInt(BLUETOOTH_PAPER_SIZE, value).apply()
        }

    var bluetoothPaperSize: Int
        get() = printer1PaperSize
        set(value) { printer1PaperSize = value }

    val isPrinter180mm get() = printer1PaperSize == PAPER_SIZE_80MM
    val isBluetooth80mm get() = isPrinter180mm

    var bluetooth2Name
        get() = preferences.getString(BLUETOOTH_2_NAME, "")!!
        set(value) {
            preferences.edit().putString(BLUETOOTH_2_NAME, value).apply()
        }

    var bluetooth2Address
        get() = preferences.getString(BLUETOOTH_2_ADDRESS, "")!!
        set(value) {
            preferences.edit().putString(BLUETOOTH_2_ADDRESS, value).apply()
        }

    var printer2PaperSize: Int
        get() = preferences.getInt(BLUETOOTH_2_PAPER_SIZE, PAPER_SIZE_80MM)
        set(value) {
            preferences.edit().putInt(BLUETOOTH_2_PAPER_SIZE, value).apply()
        }

    var bluetooth2PaperSize: Int
        get() = printer2PaperSize
        set(value) { printer2PaperSize = value }

    val isPrinter280mm get() = printer2PaperSize == PAPER_SIZE_80MM
    val isBluetooth280mm get() = isPrinter280mm

    val receiptBluetoothAddress get() = if (isReceiptBluetooth2) bluetooth2Address else bluetoothAddress
    val receiptBluetoothPaperSize get() = if (isReceiptBluetooth2) printer2PaperSize else printer1PaperSize
    val isReceiptBluetooth80mm get() = receiptBluetoothPaperSize == PAPER_SIZE_80MM

    val kitchenBluetoothAddress get() = if (isKitchenBluetooth2) bluetooth2Address else bluetoothAddress
    val kitchenBluetoothPaperSize get() = if (isKitchenBluetooth2) printer2PaperSize else printer1PaperSize
    val isKitchenBluetooth80mm get() = kitchenBluetoothPaperSize == PAPER_SIZE_80MM

    fun getPaperSizeForTarget(target: Int): Int {
        return when (target) {
            RECEIPT_TARGET_WIFI_2, RECEIPT_TARGET_BLUETOOTH_2,
            KITCHEN_TARGET_WIFI_2, KITCHEN_TARGET_BLUETOOTH_2 -> printer2PaperSize
            else -> printer1PaperSize
        }
    }

    fun getColumnsForTarget(target: Int): Int {
        return if (getPaperSizeForTarget(target) == PAPER_SIZE_80MM) 42 else 32
    }

    // Receipt font scaling (backwards compat: reads from old shared keys)
    var receiptLargeTextScale: Int
        get() = preferences.getInt(PRINT_LARGE_TEXT_SCALE, 2)
        set(value) {
            preferences.edit().putInt(PRINT_LARGE_TEXT_SCALE, value.coerceIn(1, 4)).apply()
        }

    var receiptSmallTextScale: Int
        get() = preferences.getInt(PRINT_SMALL_TEXT_SCALE, 1)
        set(value) {
            preferences.edit().putInt(PRINT_SMALL_TEXT_SCALE, value.coerceIn(1, 3)).apply()
        }

    // Kitchen font scaling
    var kitchenLargeTextScale: Int
        get() = preferences.getInt(KITCHEN_LARGE_TEXT_SCALE, 2)
        set(value) {
            preferences.edit().putInt(KITCHEN_LARGE_TEXT_SCALE, value.coerceIn(1, 4)).apply()
        }

    var kitchenSmallTextScale: Int
        get() = preferences.getInt(KITCHEN_SMALL_TEXT_SCALE, 1)
        set(value) {
            preferences.edit().putInt(KITCHEN_SMALL_TEXT_SCALE, value.coerceIn(1, 3)).apply()
        }

    // Receipt margins
    var receiptMarginFeedBefore: Int
        get() = preferences.getInt(RECEIPT_MARGIN_FEED_BEFORE, 0)
        set(value) {
            preferences.edit().putInt(RECEIPT_MARGIN_FEED_BEFORE, value.coerceIn(0, 10)).apply()
        }

    var receiptMarginFeedAfter: Int
        get() = preferences.getInt(RECEIPT_MARGIN_FEED_AFTER, 2)
        set(value) {
            preferences.edit().putInt(RECEIPT_MARGIN_FEED_AFTER, value.coerceIn(0, 10)).apply()
        }

    // Kitchen margins
    var kitchenMarginFeedBefore: Int
        get() = preferences.getInt(KITCHEN_MARGIN_FEED_BEFORE, 0)
        set(value) {
            preferences.edit().putInt(KITCHEN_MARGIN_FEED_BEFORE, value.coerceIn(0, 10)).apply()
        }

    var kitchenMarginFeedAfter: Int
        get() = preferences.getInt(KITCHEN_MARGIN_FEED_AFTER, 2)
        set(value) {
            preferences.edit().putInt(KITCHEN_MARGIN_FEED_AFTER, value.coerceIn(0, 10)).apply()
        }

    // Deprecated aliases for backward compatibility with printer writers
    @Deprecated("Use receiptLargeTextScale or kitchenLargeTextScale", ReplaceWith("receiptLargeTextScale"))
    var printLargeTextScale: Int
        get() = receiptLargeTextScale
        set(value) { receiptLargeTextScale = value }

    @Deprecated("Use receiptSmallTextScale or kitchenSmallTextScale", ReplaceWith("receiptSmallTextScale"))
    var printSmallTextScale: Int
        get() = receiptSmallTextScale
        set(value) { receiptSmallTextScale = value }

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

    var currentOrderNumber: Int
        get() = preferences.getInt(edu.amrita.amritacafe.printer.OrderNumberService.LAST_ORDER_NUMBER, RANGE_FROM_DEFAULT)
        set(value) {
            preferences.edit().putInt(edu.amrita.amritacafe.printer.OrderNumberService.LAST_ORDER_NUMBER, value).apply()
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

    var useRawSocket: Boolean
        get() = true
        set(_) {}

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
        const val RECEIPT_TARGET_BLUETOOTH_1 = 2
        const val RECEIPT_TARGET_BLUETOOTH_2 = 3
        const val RECEIPT_TARGET_NONE = 4

        const val KITCHEN_TARGET_WIFI_2 = 0
        const val KITCHEN_TARGET_WIFI_1 = 1
        const val KITCHEN_TARGET_BLUETOOTH_1 = 2
        const val KITCHEN_TARGET_BLUETOOTH_2 = 3
        const val KITCHEN_TARGET_NONE = 4

        // Backwards compatibility alias
        const val RECEIPT_TARGET_BLUETOOTH = RECEIPT_TARGET_BLUETOOTH_1
        const val KITCHEN_TARGET_BLUETOOTH = KITCHEN_TARGET_BLUETOOTH_1

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
        const val PAPER_SIZE_80MM = 0
        const val PAPER_SIZE_58MM = 1
        const val BLUETOOTH_PAPER_SIZE = "bluetooth_paper_size"
        const val BLUETOOTH_2_NAME = "bluetooth_2_name"
        const val BLUETOOTH_2_ADDRESS = "bluetooth_2_address"
        const val BLUETOOTH_2_PAPER_SIZE = "bluetooth_2_paper_size"
        const val MODE = "mode"
        const val WIFI_KEYWORDS = "wifi_keywords"
        const val BT_KEYWORDS = "bt_keywords"
        const val COLUMN_NUMBER_RANGE = "column_number_range"
        const val TESTING = "testing"
        const val BETA_UPDATES = "beta_updates"
        const val IS_BREAKFAST_MENU_KEY = "SHOW_BREAKFAST_MENU"
        const val COLUMNS_AMOUNT = "COLUMNS_AMOUNT"
        const val DEFAULT_COLUMNS_AMOUNT = 8 // TODO for tablet 11
        const val COLUMNS_AMOUNT_PORTRAIT = "COLUMNS_AMOUNT_PORTRAIT"
        const val DEFAULT_COLUMNS_AMOUNT_PORTRAIT = 4
        const val USE_RAW_SOCKET = "use_raw_socket"
        const val PRINT_AMMA_QUOTE = "print_amma_quote"
        const val PRINTER_1_PAPER_SIZE = "bluetooth_paper_size"
        const val PRINTER_2_PAPER_SIZE = "bluetooth_2_paper_size"
        const val PRINT_LARGE_TEXT_SCALE = "print_large_text_scale"
        const val PRINT_SMALL_TEXT_SCALE = "print_small_text_scale"
        const val KITCHEN_LARGE_TEXT_SCALE = "kitchen_large_text_scale"
        const val KITCHEN_SMALL_TEXT_SCALE = "kitchen_small_text_scale"
        const val KITCHEN_MARGIN_FEED_BEFORE = "kitchen_margin_feed_before"
        const val KITCHEN_MARGIN_FEED_AFTER = "kitchen_margin_feed_after"
        const val RECEIPT_MARGIN_FEED_BEFORE = "receipt_margin_feed_before"
        const val RECEIPT_MARGIN_FEED_AFTER = "receipt_margin_feed_after"
    }
}