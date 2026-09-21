package edu.amrita.amritacafe.printer

import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.writer.Writer
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.coroutines.*
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.logging.Logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ReceiptDispatch(
    private val connectionString: String,
    private val receiptWriter: Writer,
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
     *  * Open a suspend coroutine (executePrintJob or executeRawPrintJob)
     *    * Perform connect and execute the print transaction.
     *    * Listener object will be notified with PrintSuccess or PrintFailed.
     *  * Make calls to *listener* object, to give feedback to the app.
     */
    private suspend fun print(orders: List<Order>) = synchronized(getLock(connectionString)) {
        try {
            println("Printing from: $connectionString (rawSocket: ${configuration.useRawSocket})")
            if (configuration.useRawSocket) {
                val response = executeRawPrintJob(orders)
                logger.fine("Raw socket print success")
                listener.printComplete(response)
            } else {
                val (code, status) = runBlocking { executePrintJob(orders) }
                logger.fine("Executed.")

                val response = PrintDispatchResponse.fromPrinterCallback(code, status)
                logger.fine("Notify")
                listener.printComplete(response)
                listener.notifyPrinterStatus(response.printerStatus)
                ("Did notify")
            }
        } catch (exception: Epos2Exception) {
            logger.warning("Caught error ${ErrorStatus.fromCode(exception.errorStatus)}")
            listener.error(ErrorStatus.fromCode(exception.errorStatus), exception)
        } catch (e: SocketTimeoutException) {
            logger.warning("Socket timeout connecting to $connectionString: ${e.message}")
            listener.printComplete(PrintFailed(CompletedJobStatus.TimeoutError, emptyList()))
        } catch (e: ConnectException) {
            logger.warning("Connection refused / not found for $connectionString: ${e.message}")
            listener.printComplete(PrintFailed(CompletedJobStatus.NotFoundError, emptyList()))
        } catch (e: IOException) {
            logger.warning("IO error on $connectionString: ${e.message}")
            listener.printComplete(PrintFailed(CompletedJobStatus.PortError, emptyList()))
        } catch (e: Exception) {
            logger.severe("Print failed on $connectionString: ${e.message}")
            listener.printComplete(PrintFailed(CompletedJobStatus.FailureError, emptyList()))
        }
    }

    private fun executeRawPrintJob(orders: List<Order>): PrintDispatchResponse {
        val cleanIp = connectionString.removePrefix("TCP:").trim()
        val parts = cleanIp.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 9100 else 9100

        val isPrinter2 = host.equals(configuration.kitchenPrinterIP.split(":")[0], ignoreCase = true) &&
                         (configuration.kitchenPrinterTarget == Configuration.KITCHEN_TARGET_WIFI_2 || configuration.receiptPrinterTarget == Configuration.RECEIPT_TARGET_WIFI_2)
        val paperSize = if (isPrinter2) configuration.printer2PaperSize else configuration.printer1PaperSize
        val columns = if (paperSize == Configuration.PAPER_SIZE_80MM) 42 else 32

        val data = receiptWriter.writeToEscPos(orders, configuration, columns)

        SocketHelper.createBoundSocket(edu.amrita.amritacafe.AmritaCafeApp.appContext).use { socket ->
            socket.connect(InetSocketAddress(host, port), 4000)
            socket.soTimeout = 4000
            socket.getOutputStream().use { out ->
                out.write(data)
                out.flush()
            }
        }
        return PrintSuccess(emptyList())
    }

    fun dispatchPrint(orders: List<Order>): Job = CoroutineScope(Dispatchers.IO).launch {
        try {
            print(orders)
        } catch (e: Exception) {
            logger.severe("Dispatch print failed: ${e.message}")
        }
    }

    private suspend fun executePrintJob(orders: List<Order>) = buildPrinter().let { printer ->
        try {
            logger.fine("executePrintJob")
            logger.fine("connectionString: " + connectionString)
            printer.connect(connectionString, Printer.PARAM_DEFAULT)

            return@let withTimeout(5000) { suspendCancellableCoroutine<CallbackData> { continuation ->
                logger.fine("inside coroutine")
                printer.setReceiveEventListener(buildListener(continuation))
                logger.fine("Begin Transaction")
                printer.beginTransaction()
                logger.fine("Transaction open.")
                printer.addTextSmooth(Printer.TRUE)

                receiptWriter.writeToPrinter(orders, printer, configuration)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        withTimeout(2000) {
                            logger.fine("Send Data")
                            printer.sendData(5000)
                            logger.fine("Sent.")
                        }
                    } catch (e: Exception) {
                        logger.warning("Error sending data: ${e.message}")
                    }
                }
            }}
        } catch (exception : Exception) {
            logger.fine("Exception is $exception")
            exception.printStackTrace()
            println("ERROR, NO CONNECTION TO PRINTER")
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
}