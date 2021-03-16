package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
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
                }

//    private fun orderItemsText(orderItems: List<RegularOrderItem>) =
//        orderItems.joinToString("\n") {
//            "${it.quantity} ${it.code}".padEnd(17) +
//                    it.finalPrice.toString().padStart(3) +
//                    if (it.comment.isBlank()) ""
//                    else "\n * ${it.comment}"
//        }

    private fun writeTo(printer: Printer) {
        val (titleSize, textSize, lineFeed) = configuration.textConfig
        orders.forEach { (orderNumber, itemList, time) ->
//            val myOrderItems = itemList.map {
//                listOf(it)
//            }.flatten()

            val itemsMap = itemList.groupBy {
//                Pizza,
//                Burger,
//                Fries,
//                Pasta,
//                Eggs,
//                Salad,
//                Toast,
//                Breakfast("Break fast"),
//                Topping("Top with"),
//                Kitcheri("Kitch"),
//                Side;

                //{if(it.menuItem.name)}
                when (it.menuItem.category) {
                    "PIZZA" -> 1
                    "EGGS" -> 2
                    "BURGER" -> 2
                    "TOAST" -> 2
                    "SALAD" -> 3
                    "SIDE" -> 3
                    "KITCHERI" -> 3
                    "BREAKFAST" -> {
                        if (it.menuItem.name == "Oatmeal" ||
                            it.menuItem.name == "Ragi Porridge"
                        ) 3 else 2
                    }
                    "PASTA" -> 3
                    "FRIES" -> 3
                    else -> 3
                }
//                it.menuItem.category


            }.toSortedMap()
            val orderNumStr = orderNumber.toString().padStart(3, '0')

            val orderItemsText = itemsMap.map { (_, orderItems) ->
                orderItems.map(::writeLine)
            }.flatten().joinToString("\n")
            val itemCount =
                itemList.map { 1 }.sum()

            printer.addTextSize(titleSize, titleSize)
            printer.addText("$orderNumStr        $time")
            printer.addFeedLine(lineFeed)
            printer.addHLine(1, 2400, Printer.LINE_THICK_DOUBLE)

            printer.addFeedLine(lineFeed)

            printer.addTextSize(textSize, textSize)
            printer.addText(orderItemsText)
            printer.addFeedLine(lineFeed)
            printer.addFeedLine(lineFeed)
            printer.addFeedLine((6 - itemCount).let { if (it < 0) 0 else it })
            printer.addCut(Printer.CUT_FEED)
        }
    }

    companion object : Writer {

        override fun writeToPrinter(
            orders: List<Order>,
            printer: Printer,
            configuration: Configuration
        ) {
            KitchenWriter(orders, configuration).writeTo(printer)
        }
    }
}