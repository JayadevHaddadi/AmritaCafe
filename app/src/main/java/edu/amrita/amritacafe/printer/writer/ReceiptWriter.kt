package edu.amrita.amritacafe.printer.writer

import com.epson.epos2.printer.Printer
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration

interface ReceiptWriter {
    fun writeToPrinter(orders: List<Order>, printer: Printer, configuration: Configuration)
}