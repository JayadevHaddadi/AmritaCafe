package edu.amrita.jayadev.amritacafe.receiptprinter

import android.content.Context
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.writer.ReceiptWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeoutException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReceiptDispatch(private val connectionString: String,
                      private val listener: PrintStatusListener,
                      private val receiptWriter: ReceiptWriter) {

    companion object {
        private val mutex = Mutex()
    }

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
        if (mutex.isLocked) {
            listener.busy()
        } else {
            mutex.withLock {
                try {
                    println("Wanna print")
                    val (code, status) = executePrintJob(*orders)
                    println("Printed shit")

                    val response = PrintDispatchResponse.fromPrinterCallback(code, status)
                    println("Notify")
                    listener.printComplete(response)
                    listener.notifyPrinterStatus(response.printerStatus)
                    println("Did notify")
                } catch (exception: Epos2Exception) {
                    println("Erorr")
                    listener.error(ErrorStatus.fromCode(exception.errorStatus), exception)
                }
            }
        }
    }

    fun dispatchPrint(vararg orders: Order) = GlobalScope.launch(Dispatchers.IO) {
        print(*orders)
    }

    private suspend fun executePrintJob(vararg orders: Order) = buildPrinter().let { printer ->
        try {
            println("POOPFUCKFACE")
            printer.connect(connectionString, 1000)

            return@let withTimeout(5000) { suspendCancellableCoroutine<CallbackData> { continuation ->
                println("inside coroutine")
                printer.setReceiveEventListener(buildListener(continuation))
                println("Begin Transaction")
                printer.beginTransaction()
                println("Write that shit")

                receiptWriter.writeToPrinter(*orders, printer = printer)

                try {
                    GlobalScope.launch(Dispatchers.IO) {
                        withTimeout(2000) {

                            println("Send Data")
                            printer.sendData(5000)
                            println("Sent.")

                        }
                    }
                } catch (poop : Exception) {
                    println("Caught exception")
                    println(poop)
                    continuation.cancel(poop)
                }
            }}
        } catch (boop : Exception) {
            println("That is fucking stupid $boop")
            throw boop
        } finally {
            println("Will disconnect")
            try {
                withTimeout(1000) {
                    printer.endTransaction()
                    println("Wrote.  End Transaction.")
                    printer.disconnect()
                    println("Disconnected")
                }
            } catch(e : Exception) {
                println("Timeout disconnecting.")
            }
            println("Clear Command Buffer")
            printer.clearCommandBuffer()
            printer.setReceiveEventListener(null)
            println("Cleared")
        }
    }

    private fun buildPrinter() = Printer(Printer.TM_M30, Printer.MODEL_ANK, null)

    /**
     * Returns a ReceiveListener object that simply calls *resume* on @param Continuation
     * passing the code and printer status arguments given from the printer driver.
     */
    private fun buildListener(continuation: Continuation<CallbackData>)
        = ReceiveListener { _, p1, p2, _ ->
        println("Inside Receive Listener")
        continuation.resume(CallbackData(p1, p2))
        println("Resumed continuation.")
    }

    /**
     * True if the printer is not marked as offline or unknown status.
     */
    private fun connected(printer: Printer) = printer.status.run {
        connection != Printer.FALSE && online != Printer.FALSE
    }
}