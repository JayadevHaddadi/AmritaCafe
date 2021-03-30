package edu.amrita.amritacafe.activities

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.model.HistoricalOrder
import edu.amrita.amritacafe.model.PrintStatus
import edu.amrita.amritacafe.printer.*
import edu.amrita.amritacafe.printer.writer.KitchenWriter
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.include_print.view.*
import kotlinx.android.synthetic.main.item_history.view.*

class HistoryAdapter(
    val orders: MutableList<HistoricalOrder>,
    val configuration: Configuration,
    val mainActivity: MainActivity
) :
    RecyclerView.Adapter<HistoryAdapter.HistoryHolder>() {
    inner class HistoryHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(historicalOrder: HistoricalOrder) {
            view.history_time_TV.text = historicalOrder.order.orderTime
            view.history_order_nr_TV.text = historicalOrder.order.orderNumber.toString()

            view.history_order_TV.text =
                ReceiptWriter.orderItemsText(historicalOrder.order.orderItems)//orderText.toString()

            view.kitchen_progress.visibility = View.INVISIBLE
            view.kitchen_error.visibility = View.INVISIBLE
            view.kitchen_done.visibility = View.INVISIBLE
            view.kitchen_retry_button.visibility = View.VISIBLE

            view.receipt_progress.visibility = View.INVISIBLE
            view.receipt_error.visibility = View.INVISIBLE
            view.receipt_done.visibility = View.INVISIBLE
            view.receipt_retry_button.visibility = View.VISIBLE

            when (historicalOrder.KitchenPrinted) {
                PrintStatus.SUCCESS_PRINT -> view.kitchen_done.visibility = View.VISIBLE
                PrintStatus.FAILED_PRINT -> view.kitchen_error.visibility = View.VISIBLE
                PrintStatus.PRINTING -> view.kitchen_progress.visibility = View.VISIBLE
            }
            when (historicalOrder.RecipePrinted) {
                PrintStatus.SUCCESS_PRINT -> view.receipt_done.visibility = View.VISIBLE
                PrintStatus.FAILED_PRINT -> view.receipt_error.visibility = View.VISIBLE
                PrintStatus.PRINTING -> view.receipt_progress.visibility = View.VISIBLE
            }

            view.history_item_sum_TV.text = historicalOrder.order.sum.toString()

            view.kitchen_retry_button.setOnClickListener {
                view.kitchen_error.visibility = View.INVISIBLE
                view.kitchen_done.visibility = View.INVISIBLE
                view.kitchen_retry_button.visibility = View.VISIBLE
                view.kitchen_progress.visibility = View.VISIBLE
                val printerDispatch = ReceiptDispatch(
                    configuration.kitchenPrinterConnStr,
                    KitchenWriter,
                    configuration,
                    //TODO: RUN ON UI THREAD ISSUE
                    object : PrintStatusListener {
                        override fun printComplete(status: PrintDispatchResponse) {
                            mainActivity.runOnUiThread {
                                view.kitchen_progress.visibility = View.INVISIBLE
                            }
                            if (status is PrintSuccess) {
                                historicalOrder.KitchenPrinted = PrintStatus.SUCCESS_PRINT
                                Log.d("kitchen_retry_button", "PrintSuccess")
                                mainActivity.runOnUiThread {
                                    view.kitchen_done.visibility = View.VISIBLE
                                }
                            } else if (status is PrintFailed) {
                                historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                                mainActivity.runOnUiThread {
                                    Log.d("kitchen_retry_button", "PrintFailed")
                                    view.kitchen_error.visibility = View.VISIBLE
                                }
                            }
                        }

                        override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                            historicalOrder.KitchenPrinted = PrintStatus.FAILED_PRINT
                            mainActivity.runOnUiThread {
                                view.kitchen_progress.visibility = View.INVISIBLE
                                Log.d("kitchen_retry_button", "error")
                                view.kitchen_error.visibility = View.VISIBLE
                            }
                        }

                    }
                )

                printerDispatch.dispatchPrint(listOf(historicalOrder.order))
            }

            view.receipt_retry_button.setOnClickListener {
                view.receipt_error.visibility = View.INVISIBLE
                view.receipt_done.visibility = View.INVISIBLE
                view.receipt_retry_button.visibility = View.VISIBLE
                view.receipt_progress.visibility = View.VISIBLE
                val receiptPrintDispatch = ReceiptDispatch(
                    configuration.receiptPrinterConnStr,
                    ReceiptWriter,
                    configuration,
                    object : PrintStatusListener {
                        override fun printComplete(status: PrintDispatchResponse) {
                            mainActivity.runOnUiThread {
                                view.receipt_progress.visibility = View.INVISIBLE
                            }
                            if (status is PrintSuccess) {
                                historicalOrder.RecipePrinted = PrintStatus.SUCCESS_PRINT
                                Log.d("receipt_retry_button", "PrintSuccess")
                                mainActivity.runOnUiThread {
                                    view.receipt_done.visibility = View.VISIBLE
                                }
                            } else if (status is PrintFailed) {
                                historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                                Log.d("receipt_retry_button", "PrintFailed")
                                mainActivity.runOnUiThread {
                                    view.receipt_error.visibility = View.VISIBLE
                                }
                            }
                        }

                        override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
                            historicalOrder.RecipePrinted = PrintStatus.FAILED_PRINT
                            mainActivity.runOnUiThread {
                                view.receipt_progress.visibility = View.INVISIBLE
                                Log.d("receipt_retry_button", "error")
                                view.receipt_error.visibility = View.VISIBLE
                            }
                        }
                    }
                )

                receiptPrintDispatch.dispatchPrint(listOf(historicalOrder.order))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        println("creating new holder")
        val inflatedView = parent.inflate(R.layout.item_history, false)
        return HistoryHolder(inflatedView)
    }

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        println("Binding $position")
        val item = orders[position]
        holder.bind(item)
    }

}
