package edu.amrita.amritacafe.model

data class HistoricalOrder (
    val order: Order,
    var KitchenPrinted: PrintStatus = PrintStatus.PRINTING,
    var RecipePrinted: PrintStatus = PrintStatus.PRINTING
)

enum class PrintStatus {
    PRINTING, FAILED_PRINT, SUCCESS_PRINT
}