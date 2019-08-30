package edu.amrita.amritacafe.receiptprinter

import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.receiptprinter.writer.ReceiptWriter
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.coroutines.*
import java.util.logging.Logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ReceiptDispatch(
    private val connectionString: String,
    private val receiptWriter: ReceiptWriter,
    private val configuration: Configuration,
    private val listener: PrintStatusListener
) {

    private val logger = Logger.getLogger("ReceiptDispatch")
    private data class CallbackData(val code: Int, val status: PrinterStatusInfo?)
    companion object {
        private val locks = mutableMapOf<String, Any>()
        private val locksLock = Any()
        private fun getLock(connectionString: String) = synchronized(locksLock) {
            locks.getOrPut(connectionString, { Any() })
        }
    }
    /***
     *  Print algorithm:
     *
     *  * Open a suspend coroutine (executePrintJob)
     *    * Perform connect and execute the print transaction.
     *    * Listener object created by *buildListener* will resume the continuation.
     *    * If an error is caught, clean up and resume the continuation, with feedback.
     *  * Make calls to *listener* object, to give feedback to the app.
     */
    private suspend fun print(orders: List<Order>) = synchronized(getLock(connectionString)) {
        try {

            val (code, status) = runBlocking { executePrintJob(orders) }
            logger.fine("Executed.")

            val response = PrintDispatchResponse.fromPrinterCallback(code, status)
            logger.fine("Notify")
            listener.printComplete(response)
            listener.notifyPrinterStatus(response.printerStatus)
            ("Did notify")
        } catch (exception: Epos2Exception) {
            logger.warning("Caught error ${ErrorStatus.fromCode(exception.errorStatus)}")
            listener.error(ErrorStatus.fromCode(exception.errorStatus), exception)
        }
    }

    fun dispatchPrint(orders: List<Order>): Job = GlobalScope.launch(Dispatchers.IO) {
        print(orders)
    }

    private suspend fun executePrintJob(orders: List<Order>) = buildPrinter().let { printer ->
        try {
            logger.fine("executePrintJob")
            printer.connect(connectionString, Printer.PARAM_DEFAULT)

            return@let withTimeout(5000) { suspendCancellableCoroutine<CallbackData> { continuation ->
                logger.fine("inside coroutine")
                printer.setReceiveEventListener(buildListener(continuation))
                logger.fine("Begin Transaction")
                printer.beginTransaction()
                logger.fine("Transaction open.")
                printer.addTextSmooth(Printer.TRUE)

                receiptWriter.writeToPrinter(orders, printer, configuration)

                GlobalScope.launch(Dispatchers.IO) {
                    withTimeout(2000) {

                        logger.fine("Send Data")
                        printer.sendData(5000)
                        logger.fine("Sent.")

                    }
                }
            }}
        } catch (exception : Exception) {
            logger.fine("Exception is $exception")
            throw exception
        } finally {
            logger.fine("Will disconnect")
            try {
                printer.endTransaction()
                logger.fine("Wrote.  End Transaction.")
                printer.disconnect()
                logger.fine("Disconnected")
            } catch(e : Exception) {
                logger.fine("Timeout disconnecting.")
            }
            logger.warning("Clear Command Buffer")
            printer.clearCommandBuffer()
            printer.setReceiveEventListener(null)
            logger.fine("Cleared")
        }
    }

    private fun buildPrinter() = Printer(Printer.TM_M30, Printer.MODEL_ANK, null)

    /**
     * Returns a ReceiveListener object that simply calls *resume* on @param Continuation
     * passing the code and printer status arguments given from the printer driver.
     */
    private fun buildListener(continuation: Continuation<CallbackData>)
        = ReceiveListener { _, p1, p2, _ ->
        logger.fine("Inside Receive Listener")
        continuation.resume(CallbackData(p1, p2))
        logger.fine("Resumed continuation.")
    }

    /**
     * True if the printer is not marked as offline or unknown status.
     */
    private fun connected(printer: Printer) = printer.status.run {
        connection != Printer.FALSE && online != Printer.FALSE
    }
}