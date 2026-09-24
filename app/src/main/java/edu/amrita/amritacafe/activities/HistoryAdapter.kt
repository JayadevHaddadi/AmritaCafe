package edu.amrita.amritacafe.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.databinding.ItemHistoryBinding
import edu.amrita.amritacafe.model.HistoricalOrder
import edu.amrita.amritacafe.model.PrintStatus
import edu.amrita.amritacafe.printer.*
import edu.amrita.amritacafe.printer.bluetooth.bluetoothPrint
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
                edu.amrita.amritacafe.CloudStorage.updateGPayOnSheets(
                    historicalOrder,
                    configuration,
                    mainActivity
                )
            }

            val isOrderTaker = configuration.workflowMode == Configuration.MODE_ORDER_TAKER

            fun updateKitchenPrintUI() {
                if (isOrderTaker) {
                    view.kitchenStatusBadge.visibility = View.VISIBLE
                    when (historicalOrder.KitchenPrinted) {
                        PrintStatus.SUCCESS_PRINT -> {
                            view.kitchenStatusBadge.text = "🍳 KITCHEN ✓"
                            view.kitchenStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                            view.kitchenStatusBadge.alpha = 0.9f

                            view.include.kitchenProgress.visibility = View.GONE
                            view.include.kitchenDone.visibility = View.VISIBLE
                            view.include.kitchenError.visibility = View.GONE
                            view.include.kitchenStatusTV.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.text = "PRINTED ✓"
                            view.include.kitchenStatusTV.setTextColor(Color.parseColor("#4CAF50"))
                            view.include.kitchenRetryButton.visibility = View.VISIBLE
                            view.include.kitchenRetryButton.text = "Re-print"
                        }
                        PrintStatus.FAILED_PRINT, PrintStatus.NONE -> {
                            view.kitchenStatusBadge.text = "🍳 KITCHEN FAILED ✗"
                            view.kitchenStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F"))
                            view.kitchenStatusBadge.alpha = 1.0f

                            view.include.kitchenProgress.visibility = View.GONE
                            view.include.kitchenDone.visibility = View.GONE
                            view.include.kitchenError.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.text = "NOT PRINTED ✗"
                            view.include.kitchenStatusTV.setTextColor(Color.parseColor("#FF3B30"))
                            view.include.kitchenRetryButton.visibility = View.VISIBLE
                            view.include.kitchenRetryButton.text = "Retry"
                        }
                        PrintStatus.PRINTING -> {
                            view.kitchenStatusBadge.text = "🍳 KITCHEN ⏳"
                            view.kitchenStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F57C00"))
                            view.kitchenStatusBadge.alpha = 1.0f

                            view.include.kitchenProgress.visibility = View.VISIBLE
                            view.include.kitchenDone.visibility = View.GONE
                            view.include.kitchenError.visibility = View.GONE
                            view.include.kitchenStatusTV.visibility = View.VISIBLE
                            view.include.kitchenStatusTV.text = "Printing..."
                            view.include.kitchenStatusTV.setTextColor(Color.parseColor("#FFC107"))
                            view.include.kitchenRetryButton.visibility = View.GONE
                        }
                    }
                } else {
                    view.kitchenStatusBadge.visibility = View.GONE
                    view.include.kitchenLayout.visibility = View.GONE
                }
            }

            fun updateReceiptPrintUI() {
                when (historicalOrder.RecipePrinted) {
                    PrintStatus.SUCCESS_PRINT -> {
                        view.receiptStatusBadge.visibility = View.VISIBLE
                        view.receiptStatusBadge.text = "🧾 RECEIPT ✓"
                        view.receiptStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                        view.receiptStatusBadge.alpha = 0.9f

                        view.include.receiptProgress.visibility = View.GONE
                        view.include.receiptDone.visibility = View.VISIBLE
                        view.include.receiptError.visibility = View.GONE
                        view.include.receiptStatusTV.visibility = View.VISIBLE
                        view.include.receiptStatusTV.text = "PRINTED ✓"
                        view.include.receiptStatusTV.setTextColor(Color.parseColor("#4CAF50"))
                        view.include.receiptRetryButton.visibility = View.VISIBLE
                        view.include.receiptRetryButton.text = if (isOrderTaker) "Re-print" else "PRINT NEW"
                    }
                    PrintStatus.FAILED_PRINT -> {
                        view.receiptStatusBadge.visibility = View.VISIBLE
                        view.receiptStatusBadge.text = "🧾 RECEIPT FAILED ✗"
                        view.receiptStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F"))
                        view.receiptStatusBadge.alpha = 1.0f

                        view.include.receiptProgress.visibility = View.GONE
                        view.include.receiptDone.visibility = View.GONE
                        view.include.receiptError.visibility = View.VISIBLE
                        view.include.receiptStatusTV.visibility = View.VISIBLE
                        view.include.receiptStatusTV.text = "NOT PRINTED ✗"
                        view.include.receiptStatusTV.setTextColor(Color.parseColor("#FF3B30"))
                        view.include.receiptRetryButton.visibility = View.VISIBLE
                        view.include.receiptRetryButton.text = "Retry"
                    }
                    PrintStatus.PRINTING -> {
                        view.receiptStatusBadge.visibility = View.VISIBLE
                        view.receiptStatusBadge.text = "🧾 RECEIPT ⏳"
                        view.receiptStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F57C00"))
                        view.receiptStatusBadge.alpha = 1.0f

                        view.include.receiptProgress.visibility = View.VISIBLE
                        view.include.receiptDone.visibility = View.GONE
                        view.include.receiptError.visibility = View.GONE
                        view.include.receiptStatusTV.visibility = View.VISIBLE
                        view.include.receiptStatusTV.text = "Printing..."
                        view.include.receiptStatusTV.setTextColor(Color.parseColor("#FFC107"))
                        view.include.receiptRetryButton.visibility = View.GONE
                    }
                    PrintStatus.NONE -> {
                        view.receiptStatusBadge.visibility = View.GONE
                        view.include.receiptProgress.visibility = View.GONE
                        view.include.receiptDone.visibility = View.GONE
                        view.include.receiptError.visibility = View.GONE
                        view.include.receiptStatusTV.visibility = View.GONE
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
                        mainActivity.runOnUiThread { updateKitchenPrintUI() }
                    } catch (e: Exception) {
                        historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                        mainActivity.runOnUiThread { updateKitchenPrintUI() }
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
                                mainActivity.runOnUiThread { updateKitchenPrintUI() }
                            }

                            override fun error(errorStatus: ErrorStatus, exception: Exception) {
                                historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread { updateKitchenPrintUI() }
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
                        mainActivity.runOnUiThread { updateReceiptPrintUI() }
                    } catch (e: Exception) {
                        historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                        mainActivity.runOnUiThread { updateReceiptPrintUI() }
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
                                mainActivity.runOnUiThread { updateReceiptPrintUI() }
                            }

                            override fun error(errorStatus: ErrorStatus, exception: Exception) {
                                historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread { updateReceiptPrintUI() }
                            }
                        }
                    )
                    receiptPrintDispatch.dispatchPrint(listOf(historicalOrder.order))
                }
            }

            view.kitchenStatusBadge.setOnClickListener {
                if (historicalOrder.KitchenPrinted != PrintStatus.SUCCESS_PRINT) {
                    retryKitchenPrint()
                }
            }

            view.receiptStatusBadge.setOnClickListener {
                if (historicalOrder.RecipePrinted != PrintStatus.SUCCESS_PRINT) {
                    retryReceiptPrint()
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
