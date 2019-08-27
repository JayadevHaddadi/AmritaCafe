package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem
import kotlinx.coroutines.sync.Mutex

interface ReceiptWriter {
    fun writeToPrinter(vararg orders: Order, printer: Printer)
    val mutex: Mutex
}