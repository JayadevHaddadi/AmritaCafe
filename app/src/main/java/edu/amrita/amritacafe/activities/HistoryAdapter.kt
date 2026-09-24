package edu.amrita.amritacafe.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.amrita.amritacafe.databinding.ItemHistoryBinding
import edu.amrita.amritacafe.history.HistoryPersistence
import edu.amrita.amritacafe.model.HistoricalOrder
import edu.amrita.amritacafe.model.PrintStatus
import edu.amrita.amritacafe.printer.*
import edu.amrita.amritacafe.printer.writer.KitchenWriter
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.printer.writer.CashierReceiptWriter
import edu.amrita.amritacafe.settings.Configuration

class HistoryAdapter(
    val orders: MutableList<HistoricalOrder>,
    val configuration: Configuration,
    val mainActivity: MainActivity
) : RecyclerView.Adapter<HistoryAdapter.HistoryHolder>() {

    inner class HistoryHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(historicalOrder: HistoricalOrder) {
            val view = binding
            view.historyOrderNrTV.text = "Order ${historicalOrder.order.orderNumber}"
            view.historyOrderTV.text = ReceiptWriter.orderItemsText(historicalOrder.order.orderItems)
            view.historyTimeTV.text = historicalOrder.order.orderTime

            fun updateGPayUI() {
                if (historicalOrder.order.isGpay) {
                    view.gpayIndicator.visibility = View.VISIBLE
                    view.gpayIndicator.alpha = 1.0f
                } else {
                    view.gpayIndicator.visibility = View.VISIBLE
                    view.gpayIndicator.alpha = 0.2f
                }
            }

            fun updateRenunciateUI() {
                if (historicalOrder.order.isRenunciate) {
                    view.renunciateIndicator.visibility = View.VISIBLE
                    view.renunciateIndicator.alpha = 1.0f
                } else {
                    view.renunciateIndicator.visibility = View.VISIBLE
                    view.renunciateIndicator.alpha = 0.2f
                }
            }

            updateGPayUI()
            updateRenunciateUI()

            view.gpayIndicator.setOnClickListener {
                historicalOrder.order.isGpay = !historicalOrder.order.isGpay
                updateGPayUI()
                HistoryPersistence.saveHistory(mainActivity, orders)
                edu.amrita.amritacafe.CloudStorage.updateGPayOnSheets(
                    historicalOrder,
                    configuration,
                    mainActivity
                )
            }

            view.renunciateIndicator.setOnClickListener {
                historicalOrder.order.isRenunciate = !historicalOrder.order.isRenunciate
                updateRenunciateUI()
                HistoryPersistence.saveHistory(mainActivity, orders)
            }

            val isOrderTaker = configuration.workflowMode == Configuration.MODE_ORDER_TAKER

            fun updateKitchenPrintUI() {
                if (isOrderTaker) {
                    view.include.kitchenLayout.visibility = View.VISIBLE
                    when (historicalOrder.KitchenPrinted) {
                        PrintStatus.SUCCESS_PRINT -> {
                            view.include.kitchenProgress.visibility = View.GONE
                            view.include.kitchenStatusTV.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.text = "PRINTED ✓"
                            view.include.kitchenStatusTV.setTextColor(Color.parseColor("#4CAF50"))
                            view.include.kitchenRetryButton.visibility = View.VISIBLE
                            view.include.kitchenRetryButton.text = "Re-print"
                        }
                        PrintStatus.FAILED_PRINT, PrintStatus.NONE -> {
                            view.include.kitchenProgress.visibility = View.GONE
                            view.include.kitchenStatusTV.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.text = "NOT PRINTED ✗"
                            view.include.kitchenStatusTV.setTextColor(Color.parseColor("#FF3B30"))
                            view.include.kitchenRetryButton.visibility = View.VISIBLE
                            view.include.kitchenRetryButton.text = "Retry"
                        }
                        PrintStatus.PRINTING -> {
                            view.include.kitchenProgress.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.text = "Printing..."
                            view.include.kitchenStatusTV.setTextColor(Color.parseColor("#FFC107"))
                            view.include.kitchenRetryButton.visibility = View.GONE
                        }
                    }
                } else {
                    view.include.kitchenLayout.visibility = View.GONE
                }
            }

            fun updateReceiptPrintUI() {
                when (historicalOrder.RecipePrinted) {
                    PrintStatus.SUCCESS_PRINT -> {
                        view.include.receiptProgress.visibility = View.GONE
                        view.include.receiptStatusTV.visibility = View.VISIBLE
                        view.include.receiptStatusTV.text = "PRINTED ✓"
                        view.include.receiptStatusTV.setTextColor(Color.parseColor("#4CAF50"))
                        view.include.receiptRetryButton.visibility = View.VISIBLE
                        view.include.receiptRetryButton.text = if (isOrderTaker) "Re-print" else "PRINT NEW"
                    }
                    PrintStatus.FAILED_PRINT -> {
                        view.include.receiptProgress.visibility = View.GONE
                        view.include.receiptStatusTV.visibility = View.VISIBLE
                        view.include.receiptStatusTV.text = "NOT PRINTED ✗"
                        view.include.receiptStatusTV.setTextColor(Color.parseColor("#FF3B30"))
                        view.include.receiptRetryButton.visibility = View.VISIBLE
                        view.include.receiptRetryButton.text = "Retry"
                    }
                    PrintStatus.PRINTING -> {
                        view.include.receiptProgress.visibility = View.VISIBLE
                        view.include.receiptStatusTV.visibility = View.VISIBLE
                        view.include.receiptStatusTV.text = "Printing..."
                        view.include.receiptStatusTV.setTextColor(Color.parseColor("#FFC107"))
                        view.include.receiptRetryButton.visibility = View.GONE
                    }
                    PrintStatus.NONE -> {
                        view.include.receiptProgress.visibility = View.GONE
                        view.include.receiptStatusTV.visibility = View.VISIBLE
                        view.include.receiptStatusTV.text = "NOT PRINTED ✗"
                        view.include.receiptStatusTV.setTextColor(Color.parseColor("#FF3B30"))
                        view.include.receiptRetryButton.visibility = View.VISIBLE
                        view.include.receiptRetryButton.text = "Print"
                    }
                }
            }

            updateKitchenPrintUI()
            updateReceiptPrintUI()

            view.historyItemSumTV.text = historicalOrder.order.sum.toString()

            fun retryKitchenPrint() {
                historicalOrder.KitchenPrinted = PrintStatus.PRINTING
                mainActivity.runOnUiThread { updateKitchenPrintUI() }

                if (configuration.isKitchenBluetooth) {
                    try {
                        edu.amrita.amritacafe.printer.bluetooth.bluetoothPrintKitchen(
                            mainActivity.mHoinPrinter,
                            listOf(historicalOrder.order),
                            configuration
                        )
                        historicalOrder.KitchenPrinted = PrintStatus.SUCCESS_PRINT
                        mainActivity.runOnUiThread {
                            updateKitchenPrintUI()
                            HistoryPersistence.saveHistory(mainActivity, orders)
                        }
                    } catch (e: Exception) {
                        historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                        mainActivity.runOnUiThread {
                            updateKitchenPrintUI()
                            HistoryPersistence.saveHistory(mainActivity, orders)
                        }
                    }
                } else {
                    val printerDispatch = ReceiptDispatch(
                        configuration.kitchenPrinterConnStr,
                        KitchenWriter,
                        configuration,
                        object : PrintStatusListener {
                            override fun printComplete(status: PrintDispatchResponse) {
                                historicalOrder.KitchenPrinted = if (status is PrintSuccess) {
                                    PrintStatus.SUCCESS_PRINT
                                } else {
                                    PrintStatus.FAILED_PRINT
                                }
                                mainActivity.runOnUiThread {
                                    updateKitchenPrintUI()
                                    HistoryPersistence.saveHistory(mainActivity, orders)
                                }
                            }

                            override fun error(errorStatus: ErrorStatus, exception: Exception) {
                                historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread {
                                    updateKitchenPrintUI()
                                    HistoryPersistence.saveHistory(mainActivity, orders)
                                }
                            }
                        }
                    )
                    printerDispatch.dispatchPrint(listOf(historicalOrder.order))
                }
            }

            fun retryReceiptPrint() {
                historicalOrder.RecipePrinted = PrintStatus.PRINTING
                mainActivity.runOnUiThread { updateReceiptPrintUI() }

                if (configuration.isReceiptBluetooth) {
                    try {
                        edu.amrita.amritacafe.printer.bluetooth.bluetoothPrintReceipt(
                            mainActivity.mHoinPrinter,
                            listOf(historicalOrder.order),
                            configuration
                        )
                        historicalOrder.RecipePrinted = PrintStatus.SUCCESS_PRINT
                        mainActivity.runOnUiThread {
                            updateReceiptPrintUI()
                            HistoryPersistence.saveHistory(mainActivity, orders)
                        }
                    } catch (e: Exception) {
                        historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                        mainActivity.runOnUiThread {
                            updateReceiptPrintUI()
                            HistoryPersistence.saveHistory(mainActivity, orders)
                        }
                    }
                } else {
                    val writer = if (configuration.workflowMode == Configuration.MODE_CASHIER) {
                        CashierReceiptWriter
                    } else {
                        ReceiptWriter
                    }
                    val receiptPrintDispatch = ReceiptDispatch(
                        configuration.receiptPrinterConnStr,
                        writer,
                        configuration,
                        object : PrintStatusListener {
                            override fun printComplete(status: PrintDispatchResponse) {
                                historicalOrder.RecipePrinted = if (status is PrintSuccess) {
                                    PrintStatus.SUCCESS_PRINT
                                } else {
                                    PrintStatus.FAILED_PRINT
                                }
                                mainActivity.runOnUiThread {
                                    updateReceiptPrintUI()
                                    HistoryPersistence.saveHistory(mainActivity, orders)
                                }
                            }

                            override fun error(errorStatus: ErrorStatus, exception: Exception) {
                                historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread {
                                    updateReceiptPrintUI()
                                    HistoryPersistence.saveHistory(mainActivity, orders)
                                }
                            }
                        }
                    )
                    receiptPrintDispatch.dispatchPrint(listOf(historicalOrder.order))
                }
            }

            view.include.kitchenRetryButton.setOnClickListener {
                retryKitchenPrint()
            }

            view.include.receiptRetryButton.setOnClickListener {
                retryReceiptPrint()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryHolder(binding)
    }

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        val item = orders[position]
        holder.bind(item)
    }
}
