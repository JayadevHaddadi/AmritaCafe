package edu.amrita.amritacafe.printer.writer

import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration

interface Writer {
    fun writeToEscPos(orders: List<Order>, configuration: Configuration): ByteArray
    fun writeToEscPos(orders: List<Order>, configuration: Configuration, columns: Int): ByteArray =
        writeToEscPos(orders, configuration)
}