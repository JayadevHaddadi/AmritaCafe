package edu.amrita.amritacafe.printer

import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.printer.writer.KitchenWriter
import edu.amrita.amritacafe.settings.Configuration

class PrintService(private val orders: List<Order>, private val listener: PrintServiceListener, private val configuration: Configuration) {
    interface PrintServiceListener {
        fun kitchenPrinterFinished()
        fun receiptPrinterFinished()
        fun receiptPrinterError(response: PrintFailed)
        fun receiptPrinterError(errorStatus: ErrorStatus, exception: Exception = Exception())
        fun kitchenPrinterError(response: PrintFailed)
        fun kitchenPrinterError(errorStatus: ErrorStatus, exception: Exception = Exception())
        fun printingComplete()
    }

    private var receiptFinished = false
    private var kitchenFinished = false

    fun print() {
        receiptFinished = !configuration.isReceiptWifi
        kitchenFinished = !configuration.isKitchenWifi

        if (!receiptFinished) {
            receiptPrintDispatch.dispatchPrint(orders)
        } else {
            listener.receiptPrinterFinished()
        }

        if (!kitchenFinished) {
            kitchenPrintDispatch.dispatchPrint(orders)
        } else {
            listener.kitchenPrinterFinished()
        }

        if (receiptFinished && kitchenFinished) {
            listener.printingComplete()
        }
    }

    fun retry() {
        if (!receiptFinished && configuration.isReceiptWifi) {
            receiptPrintDispatch.dispatchPrint(orders)
        }
        if (!kitchenFinished && configuration.isKitchenWifi) {
            kitchenPrintDispatch.dispatchPrint(orders)
        }
    }

    private val kitchenPrintDispatch = ReceiptDispatch(
        configuration.kitchenPrinterConnStr,
        KitchenWriter,
        configuration,
        object : PrintStatusListener {
            override fun printComplete(status: PrintDispatchResponse) {
                if (status is PrintSuccess) {
                    listener.kitchenPrinterFinished()
                    kitchenFinished = true
                    if (receiptFinished) {
                        listener.printingComplete()
                    }

                } else if (status is PrintFailed) {
                    listener.kitchenPrinterError(status)
                }
            }

            override fun error(errorStatus: ErrorStatus, exception: Exception) {
                listener.kitchenPrinterError(errorStatus, exception)
            }
        }
    )

    private val receiptPrintDispatch = ReceiptDispatch(
        configuration.receiptPrinterConnStr,
        ReceiptWriter,
        configuration,
        object : PrintStatusListener {
            override fun printComplete(status: PrintDispatchResponse) {
                if (status is PrintSuccess) {
                    listener.receiptPrinterFinished()
                    receiptFinished = true
                    if (kitchenFinished) {
                        listener.printingComplete()
                    }
                } else if (status is PrintFailed) {
                    listener.receiptPrinterError(status)
                }
            }

            override fun error(errorStatus: ErrorStatus, exception: Exception) {
                listener.receiptPrinterError(errorStatus, exception)
            }
        }
    )
}