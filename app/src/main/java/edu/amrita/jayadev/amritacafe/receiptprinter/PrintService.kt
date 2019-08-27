package edu.amrita.jayadev.amritacafe.receiptprinter

import com.epson.epos2.Epos2Exception
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.receiptprinter.writer.ReceiptWriterImpl
import edu.amrita.jayadev.amritacafe.receiptprinter.writer.WorkOrderWriter
import edu.amrita.jayadev.amritacafe.settings.Configuration

class PrintService(private val orders: List<Order>, private val listener: PrintServiceListener, private val configuration: Configuration) {
    interface PrintServiceListener {
        fun kitchenPrinterFinished()
        fun receiptPrinterFinished()
        fun receiptPrinterError(response: PrintFailed)
        fun receiptPrinterError(errorStatus: ErrorStatus, exception: Epos2Exception)
        fun kitchenPrinterError(response: PrintFailed)
        fun kitchenPrinterError(errorStatus: ErrorStatus, exception: Epos2Exception)
        fun printingComplete()
    }

    private var receiptFinished = false
    private var kitchenFinished = false


    fun print() {
        receiptFinished = false
        kitchenFinished = false

        receiptPrintDispatch.dispatchPrint(orders)
        kitchenPrintDispatch.dispatchPrint(orders)
    }

    fun retry() {
        if (!receiptFinished) {
            receiptPrintDispatch.dispatchPrint(orders)
        }
        if (!kitchenFinished) {
            kitchenPrintDispatch.dispatchPrint(orders)
        }
    }

    private val kitchenPrintDispatch = ReceiptDispatch(
        configuration.kitchenPrinterConnStr,
        WorkOrderWriter,
        configuration,
        object : PrintStatusListener {
            override fun printComplete(response: PrintDispatchResponse) {
                if (response is PrintSuccess) {
                    listener.kitchenPrinterFinished()
                    kitchenFinished = true
                    if (receiptFinished) {
                        listener.printingComplete()
                    }

                } else if (response is PrintFailed) {
                    listener.kitchenPrinterError(response)
                }
            }

            override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                listener.kitchenPrinterError(errorStatus, exception)
            }
        }
    )

    private val receiptPrintDispatch = ReceiptDispatch(
        configuration.receiptPrinterConnStr,
        ReceiptWriterImpl,
        configuration,
        object : PrintStatusListener {
            override fun printComplete(response: PrintDispatchResponse) {
                if (response is PrintSuccess) {
                    listener.receiptPrinterFinished()
                    receiptFinished = true
                    if (kitchenFinished) {
                        listener.printingComplete()
                    }
                } else if (response is PrintFailed) {
                    listener.receiptPrinterError(response)
                }
            }

            override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                listener.receiptPrinterError(errorStatus, exception)
            }

        }
    )

}