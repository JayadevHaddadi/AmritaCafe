package edu.amrita.jayadev.amritacafe.receiptprinter

import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.settings.Configuration
import kotlinx.coroutines.runBlocking

class PrintService {
    fun print(vararg orders: Order, listener: PrintStatusListener) = runBlocking {

//        val receiptPrinter = ReceiptDispatch(
//            Configuration.current
//        )


    }
}