package edu.amrita.amritacafe.printer

import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.writer.Writer
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.coroutines.*
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.logging.Logger

class ReceiptDispatch(
    private val connectionString: String,
    private val receiptWriter: Writer,
    private val configuration: Configuration,
    private val listener: PrintStatusListener
) {

    private val logger = Logger.getLogger("ReceiptDispatch")

    companion object {
        private val locks = mutableMapOf<String, Any>()
        private val locksLock = Any()
        private fun getLock(connectionString: String) = synchronized(locksLock) {
            locks.getOrPut(connectionString, { Any() })
        }
    }

    /***
     *  Print algorithm:
     *  * Connect to printer using Raw TCP (ESC/POS) on port 9100.
     *  * Transmit ESC/POS byte payload.
     *  * Listener object will be notified with PrintSuccess or PrintFailed.
     */
    private suspend fun print(orders: List<Order>) = synchronized(getLock(connectionString)) {
        try {
            println("Printing from: $connectionString (Raw TCP)")
            val response = executeRawPrintJob(orders)
            logger.fine("Raw socket print success")
            listener.printComplete(response)
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
}