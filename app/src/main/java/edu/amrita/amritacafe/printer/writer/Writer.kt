package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration

interface Writer {
    fun writeToPrinter(orders: List<Order>, printer: Printer, configuration: Configuration)
    fun writeToEscPos(orders: List<Order>, configuration: Configuration): ByteArray
    fun writeToEscPos(orders: List<Order>, configuration: Configuration, columns: Int): ByteArray =
        writeToEscPos(orders, configuration)
}