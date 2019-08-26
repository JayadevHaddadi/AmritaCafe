package edu.amrita.jayadev.amritacafe.receiptprinter

import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.writer.ReceiptWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReceiptDispatch(private val connectionString: String,
                      private val listener: PrintStatusListener,
                      private val receiptWriter: ReceiptWriter) {

    private data class CallbackData(val code: Int, val status: PrinterStatusInfo?)

    /***
     *  Print algorithm:
     *
     *  * Open a suspend coroutine (executePrintJob)
     *    * Perform connect and execute the print transaction.
     *    * Listener object created by *buildListener* will resume the continuation.
     *    * If an error is caught, clean up and resume the continuation, with feedback.
     *  * Make calls to *listener* object, to give feedback to the app.
     */
    suspend fun print(vararg orders: Order) {
        try {

            val (code, status) = executePrintJob(*orders)

            val response = PrintDispatchResponse.fromPrinterCallback(code, status)
            listener.printComplete(response)
            listener.notifyPrinterStatus(response.printerStatus)
        } catch (exception: Epos2Exception) {
            listener.error(ErrorStatus.fromCode(exception.errorStatus), exception)
        }
    }

    fun dispatchPrint(vararg orders: Order) = runBlocking {
        launch(Dispatchers.IO) {
            print(*orders)
        }
    }

    private suspend fun executePrintJob(vararg orders: Order) = suspendCoroutine<CallbackData> { continuation ->
        val printer = Printer(Printer.TM_M30, Printer.MODEL_ANK, null)
        printer.setReceiveEventListener(buildListener(continuation))
        try {
            printer.connect(connectionString, Printer.PARAM_DEFAULT)
            printer.beginTransaction()

            receiptWriter.writeToPrinter(*orders, printer = printer)

            printer.endTransaction()
            printer.sendData(Printer.PARAM_DEFAULT)
        } finally {
            if (connected(printer)) {
                printer.disconnect()
            }
        }
    }

    /**
     * Returns a ReceiveListener object that simply calls *resume* on @param Continuation
     * passing the code and printer status arguments given from the printer driver.
     */
    private fun buildListener(continuation: Continuation<CallbackData>)
        = ReceiveListener { _, p1, p2, _ -> continuation.resume(CallbackData(p1, p2)) }

    /**
     * True if the printer is not marked as offline or unknown status.
     */
    private fun connected(printer: Printer) = printer.status.run {
        connection != Printer.FALSE && online != Printer.FALSE && online != Printer.UNKNOWN
    }
}