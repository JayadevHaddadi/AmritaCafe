package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem
import edu.amrita.jayadev.amritacafe.settings.Configuration
import kotlinx.coroutines.sync.Mutex

interface ReceiptWriter {
    fun writeToPrinter(orders: List<Order>, printer: Printer, configuration: Configuration)
}