package edu.amrita.amritacafe.model

data class HistoricalOrder (
    val order: Order,
    var KitchenPrinted: PrintStatus = PrintStatus.NONE,
    var RecipePrinted: PrintStatus = PrintStatus.NONE
)

enum class PrintStatus {
    PRINTING, FAILED_PRINT, SUCCESS_PRINT, NONE
}