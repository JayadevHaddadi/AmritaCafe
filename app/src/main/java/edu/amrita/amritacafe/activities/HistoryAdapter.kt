package edu.amrita.amritacafe.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.databinding.ItemHistoryBinding
import edu.amrita.amritacafe.model.HistoricalOrder
import edu.amrita.amritacafe.model.PrintStatus
import edu.amrita.amritacafe.printer.*
import edu.amrita.amritacafe.printer.bluetooth.bluetoothPrint
import edu.amrita.amritacafe.printer.writer.KitchenWriter
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.settings.Configuration

class HistoryAdapter(
    val orders: MutableList<HistoricalOrder>,
    val configuration: Configuration,
    val mainActivity: MainActivity
) : RecyclerView.Adapter<HistoryAdapter.HistoryHolder>() {

    inner class HistoryHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(historicalOrder: HistoricalOrder) {
            val view = binding

            view.historyTimeTV.text = historicalOrder.order.orderTime
            view.historyOrderNrTV.text = historicalOrder.order.orderNumber.toString()

            view.historyOrderTV.text =
                ReceiptWriter.orderItemsText(historicalOrder.order.orderItems)

            fun updateGPayUI() {
                if (historicalOrder.order.isGpay) {
                    view.gpayIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                    view.gpayIndicator.alpha = 1.0f
                } else {
                    view.gpayIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#808080"))
                    view.gpayIndicator.alpha = 0.2f
                }
            }

            fun updateRenunciateUI() {
                if (historicalOrder.order.isRenunciate) {
                    view.renunciateIndicator.visibility = View.VISIBLE
                    view.renunciateIndicator.alpha = 1.0f
                } else {
                    view.renunciateIndicator.visibility = View.VISIBLE // Make always visible as requested?
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

            // User didn't ask for retrospective Renunciate toggle yet, but let's keep it consistent
            view.renunciateIndicator.setOnClickListener {
                // For now just toggle UI, or maybe do nothing if not supported by backend
                // historicalOrder.order.isRenunciate = !historicalOrder.order.isRenunciate
                // updateRenunciateUI()
            }

            val isBluetooth = configuration.mode == mainActivity.BLUETOOTH

            val kitchenLayout = binding.root.findViewById<View>(R.id.kitchen_layout)
            if (isBluetooth) {
                kitchenLayout?.visibility = View.GONE
                view.include.receiptTextTV.text = "Printer:"
                view.include.receiptRetryButton.text = "PRINT NEW"
            } else {
                kitchenLayout?.visibility = View.VISIBLE
                view.include.receiptTextTV.text = "Receipt Printer:"
                view.include.receiptRetryButton.text = "Retry"
            }

            // Reset visibilities - ensure progress is GONE by default
            view.include.kitchenProgress.visibility = View.GONE
            view.include.kitchenError.visibility = View.GONE
            view.include.kitchenDone.visibility = View.GONE
            view.include.kitchenRetryButton.visibility = if (isBluetooth) View.GONE else View.VISIBLE

            view.include.receiptProgress.visibility = View.GONE
            view.include.receiptError.visibility = View.GONE
            view.include.receiptDone.visibility = View.GONE
            view.include.receiptRetryButton.visibility = View.VISIBLE

            if (!isBluetooth) {
                when (historicalOrder.KitchenPrinted) {
                    PrintStatus.SUCCESS_PRINT -> view.include.kitchenDone.visibility = View.VISIBLE
                    PrintStatus.FAILED_PRINT -> view.include.kitchenError.visibility = View.VISIBLE
                    PrintStatus.PRINTING -> view.include.kitchenProgress.visibility = View.VISIBLE
                    PrintStatus.NONE -> {}
                }
            }

            when (historicalOrder.RecipePrinted) {
                PrintStatus.SUCCESS_PRINT -> view.include.receiptDone.visibility = View.VISIBLE
                PrintStatus.FAILED_PRINT -> view.include.receiptError.visibility = View.VISIBLE
                PrintStatus.PRINTING -> view.include.receiptProgress.visibility = View.VISIBLE
                PrintStatus.NONE -> {}
            }

            view.historyItemSumTV.text = historicalOrder.order.sum.toString()

            view.include.kitchenRetryButton.setOnClickListener {
                view.include.kitchenError.visibility = View.GONE
                view.include.kitchenDone.visibility = View.GONE
                view.include.kitchenProgress.visibility = View.VISIBLE
                val printerDispatch = ReceiptDispatch(
                    configuration.kitchenPrinterConnStr,
                    KitchenWriter,
                    configuration,
                    object : PrintStatusListener {
                        override fun printComplete(status: PrintDispatchResponse) {
                            mainActivity.runOnUiThread {
                                view.include.kitchenProgress.visibility = View.GONE
                            }
                            if (status is PrintSuccess) {
                                historicalOrder.KitchenPrinted = PrintStatus.SUCCESS_PRINT
                                mainActivity.runOnUiThread {
                                    view.include.kitchenDone.visibility = View.VISIBLE
                                }
                            } else if (status is PrintFailed) {
                                historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread {
                                    view.include.kitchenError.visibility = View.VISIBLE
                                }
                            }
                        }

                        override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                            historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                            mainActivity.runOnUiThread {
                                view.include.kitchenProgress.visibility = View.GONE
                                view.include.kitchenError.visibility = View.VISIBLE
                            }
                        }

                    }
                )

                printerDispatch.dispatchPrint(listOf(historicalOrder.order))
            }

            view.include.receiptRetryButton.setOnClickListener {
                view.include.receiptError.visibility = View.GONE
                view.include.receiptDone.visibility = View.GONE
                view.include.receiptProgress.visibility = View.VISIBLE

                if (isBluetooth) {
                    try {
                        bluetoothPrint(mainActivity.mHoinPrinter, listOf(historicalOrder.order))
                        historicalOrder.RecipePrinted = PrintStatus.SUCCESS_PRINT
                        mainActivity.runOnUiThread {
                            view.include.receiptProgress.visibility = View.GONE
                            view.include.receiptDone.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                        mainActivity.runOnUiThread {
                            view.include.receiptProgress.visibility = View.GONE
                            view.include.receiptError.visibility = View.VISIBLE
                        }
                    }
                } else {
                    val receiptPrintDispatch = ReceiptDispatch(
                        configuration.receiptPrinterConnStr,
                        ReceiptWriter,
                        configuration,
                        object : PrintStatusListener {
                            override fun printComplete(status: PrintDispatchResponse) {
                                mainActivity.runOnUiThread {
                                    view.include.receiptProgress.visibility = View.GONE
                                }
                                if (status is PrintSuccess) {
                                    historicalOrder.RecipePrinted = PrintStatus.SUCCESS_PRINT
                                    mainActivity.runOnUiThread {
                                        view.include.receiptDone.visibility = View.VISIBLE
                                    }
                                } else if (status is PrintFailed) {
                                    historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                                    mainActivity.runOnUiThread {
                                        view.include.receiptError.visibility = View.VISIBLE
                                    }
                                }
                            }

                            override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                                historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread {
                                    view.include.receiptProgress.visibility = View.GONE
                                    view.include.receiptError.visibility = View.VISIBLE
                                }
                            }
                        }
                    )

                    receiptPrintDispatch.dispatchPrint(listOf(historicalOrder.order))
                }
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
