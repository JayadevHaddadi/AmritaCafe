package edu.amrita.amritacafe.activities

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.databinding.ItemHistoryBinding
import edu.amrita.amritacafe.model.HistoricalOrder
import edu.amrita.amritacafe.model.PrintStatus
import edu.amrita.amritacafe.printer.*
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

            view.include.kitchenProgress.visibility = View.INVISIBLE
            view.include.kitchenError.visibility = View.INVISIBLE
            view.include.kitchenDone.visibility = View.INVISIBLE
            view.include.kitchenRetryButton.visibility = View.VISIBLE

            view.include.receiptProgress.visibility = View.INVISIBLE
            view.include.receiptError.visibility = View.INVISIBLE
            view.include.receiptDone.visibility = View.INVISIBLE
            view.include.receiptRetryButton.visibility = View.VISIBLE

            when (historicalOrder.KitchenPrinted) {
                PrintStatus.SUCCESS_PRINT -> view.include.kitchenDone.visibility = View.VISIBLE
                PrintStatus.FAILED_PRINT -> view.include.kitchenError.visibility = View.VISIBLE
                PrintStatus.PRINTING -> view.include.kitchenProgress.visibility = View.VISIBLE
            }

            when (historicalOrder.RecipePrinted) {
                PrintStatus.SUCCESS_PRINT -> view.include.receiptDone.visibility = View.VISIBLE
                PrintStatus.FAILED_PRINT -> view.include.receiptError.visibility = View.VISIBLE
                PrintStatus.PRINTING -> view.include.receiptProgress.visibility = View.VISIBLE
            }

            view.historyItemSumTV.text = historicalOrder.order.sum.toString()

            view.include.kitchenRetryButton.setOnClickListener {
                view.include.kitchenError.visibility = View.INVISIBLE
                view.include.kitchenDone.visibility = View.INVISIBLE
                view.include.kitchenRetryButton.visibility = View.VISIBLE
                view.include.kitchenProgress.visibility = View.VISIBLE
                val printerDispatch = ReceiptDispatch(
                    configuration.kitchenPrinterConnStr,
                    KitchenWriter,
                    configuration,
                    object : PrintStatusListener {
                        override fun printComplete(status: PrintDispatchResponse) {
                            mainActivity.runOnUiThread {
                                view.include.kitchenProgress.visibility = View.INVISIBLE
                            }
                            if (status is PrintSuccess) {
                                historicalOrder.KitchenPrinted = PrintStatus.SUCCESS_PRINT
                                Log.d("kitchen_retry_button", "PrintSuccess")
                                mainActivity.runOnUiThread {
                                    view.include.kitchenDone.visibility = View.VISIBLE
                                }
                            } else if (status is PrintFailed) {
                                historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread {
                                    Log.d("kitchen_retry_button", "PrintFailed")
                                    view.include.kitchenError.visibility = View.VISIBLE
                                }
                            }
                        }

                        override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                            historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                            mainActivity.runOnUiThread {
                                view.include.kitchenProgress.visibility = View.INVISIBLE
                                Log.d("kitchen_retry_button", "error")
                                view.include.kitchenError.visibility = View.VISIBLE
                            }
                        }

                    }
                )

                printerDispatch.dispatchPrint(listOf(historicalOrder.order))
            }

            view.include.receiptRetryButton.setOnClickListener {
                view.include.receiptError.visibility = View.INVISIBLE
                view.include.receiptDone.visibility = View.INVISIBLE
                view.include.receiptRetryButton.visibility = View.VISIBLE
                view.include.receiptProgress.visibility = View.VISIBLE
                val receiptPrintDispatch = ReceiptDispatch(
                    configuration.receiptPrinterConnStr,
                    ReceiptWriter,
                    configuration,
                    object : PrintStatusListener {
                        override fun printComplete(status: PrintDispatchResponse) {
                            mainActivity.runOnUiThread {
                                view.include.receiptProgress.visibility = View.INVISIBLE
                            }
                            if (status is PrintSuccess) {
                                historicalOrder.RecipePrinted = PrintStatus.SUCCESS_PRINT
                                Log.d("receipt_retry_button", "PrintSuccess")
                                mainActivity.runOnUiThread {
                                    view.include.receiptDone.visibility = View.VISIBLE
                                }
                            } else if (status is PrintFailed) {
                                historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                                Log.d("receipt_retry_button", "PrintFailed")
                                mainActivity.runOnUiThread {
                                    view.include.receiptError.visibility = View.VISIBLE
                                }
                            }
                        }

                        override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                            historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                            mainActivity.runOnUiThread {
                                view.include.receiptProgress.visibility = View.INVISIBLE
                                Log.d("receipt_retry_button", "error")
                                view.include.receiptError.visibility = View.VISIBLE
                            }
                        }
                    }
                )

                receiptPrintDispatch.dispatchPrint(listOf(historicalOrder.order))
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
