package edu.amrita.amritacafe.printer.writer

import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.escpos.EscPosBuilder
import edu.amrita.amritacafe.settings.Configuration

class KitchenWriter(private val orders: List<Order>, private val configuration: Configuration) {

    init {
        require(orders.isNotEmpty()) { "An order needs at least one order item." }
    }

    private fun writeLine(orderItem: RegularOrderItem) =
        if (orderItem.quantity == 1) {
            "  "
        } else {
            orderItem.quantity.toString().padEnd(2)
        } + orderItem.code +
                if (orderItem.comment.isBlank()) {
                    ""
                } else {
                    "\n  * ${orderItem.comment}"
                } +

                if (orderItem.toppings.isNotEmpty()) {
                    "\n" + orderItem.toppings.joinToString("\n") {
                        if (it.quantity == 1) {
                            "  "
                        } else {
                            it.quantity.toString().padEnd(2)
                        } + it.menuItem.code
                    }
                } else {
                    ""
                }

    companion object : Writer {
        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration
        ): ByteArray {
            return KitchenWriter(orders, configuration).writeToEscPos(42)
        }

        override fun writeToEscPos(
            orders: List<Order>,
            configuration: Configuration,
            columns: Int
        ): ByteArray {
            return KitchenWriter(orders, configuration).writeToEscPos(columns)
        }
    }

    fun writeToEscPos(columns: Int = 42): ByteArray {
        val totalCols = if (columns > 0) columns else 42
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        val builder = EscPosBuilder()

        orders.forEach { (orderNumber, itemList, date, time) ->
            val orderItemsText = itemList.map(::writeLine).joinToString("\n")
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            if (configuration.kitchenMarginFeedBefore > 0) {
                builder.feedLines(configuration.kitchenMarginFeedBefore)
            }

            builder.alignLeft()
            val titleScale = configuration.kitchenLargeTextScale
            val effectiveHeaderCols = totalCols / titleScale
            builder.textSize(titleScale, titleScale)
            builder.bold(true)
            val headerPad = (effectiveHeaderCols - time.length).coerceAtLeast(orderNumStr.length)
            val headerText = orderNumStr.padEnd(headerPad) + time
            builder.line(headerText)
            builder.bold(false)

            builder.feedLines(lineFeed)

            // Large, bold font for kitchen staff
            val textScale = if (configuration.testing) 1 else configuration.kitchenSmallTextScale
            builder.textSize(textScale, textScale)
            builder.bold(true)
            builder.line(orderItemsText)
            builder.bold(false)
            builder.textSize(1, 1)

            val feedAfter = configuration.kitchenMarginFeedAfter.coerceAtLeast(1)
            builder.cut(feedAfter)
        }
        return builder.build()
    }
}