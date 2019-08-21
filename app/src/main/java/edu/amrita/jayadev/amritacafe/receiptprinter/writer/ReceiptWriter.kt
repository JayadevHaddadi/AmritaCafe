package edu.amrita.jayadev.amritacafe.receiptprinter.writer

import com.epson.epos2.printer.Printer
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderItem

interface ReceiptWriter {
    fun writeToPrinter(orderNumber: Int, orderItems: List<OrderItem>, printer: Printer)
}