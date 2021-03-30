package edu.amrita.amritacafe.activities

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.printer.*
import edu.amrita.amritacafe.printer.writer.KitchenWriter
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.include_print.view.*
import kotlinx.android.synthetic.main.item_history.view.*

class HistoryAdapter(
    val orders: MutableList<Order>,
    val configuration: Configuration
) :
    RecyclerView.Adapter<HistoryAdapter.HistoryHolder>() {
    inner class HistoryHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(order: Order) {
            view.history_time_TV.text = order.orderTime
            view.history_order_nr_TV.text = order.orderNumber.toString()

//            val orderText = StringBuffer()
//            println("size: ${order.orderItems.size}")
//
//            for (item in order.orderItems) {
//                println("order: ${item.menuItem.name}")
//                orderText.append(
//                    "${item.quantity} ${item.menuItem.code}".padEnd(17) +
//                            item.priceWithoutToppings.toString().padStart(3) +
//                            if (item.comment.isBlank()) "\n"
//                            else "\n * ${item.comment}\n"
//                )
//            }
            view.history_order_TV.text = ReceiptWriter.orderItemsText(order.orderItems)//orderText.toString()

            view.kitchen_progress.visibility = View.INVISIBLE
            view.image_kitchen_error.visibility = View.INVISIBLE
            view.image_kitchen_done.visibility = View.INVISIBLE
            view.kitchen_retry_button.visibility = View.VISIBLE

            view.receipt_progress.visibility = View.INVISIBLE
            view.image_receipt_error.visibility = View.INVISIBLE
            view.image_receipt_done.visibility = View.INVISIBLE
            view.receipt_retry_button.visibility = View.VISIBLE

            view.history_item_sum_TV.text = order.sum.toString()

            view.kitchen_retry_button.setOnClickListener {
                view.image_kitchen_error.visibility = View.INVISIBLE
                view.image_kitchen_done.visibility = View.INVISIBLE
                view.kitchen_retry_button.visibility = View.VISIBLE
                view.kitchen_progress.visibility = View.VISIBLE
                val receiptPrintDispatch = ReceiptDispatch(
                    configuration.kitchenPrinterConnStr,
                    KitchenWriter,
                    configuration,
                    //TODO: RUN ON UI THREAD ISSUE
                    object : PrintStatusListener {
                        override fun printComplete(status: PrintDispatchResponse) {
//                            view.kitchen_progress.visibility = View.INVISIBLE
                            if (status is PrintSuccess) {
                                Log.d("kitchen_retry_button","PrintSuccess")
//                                view.image_kitchen_done.visibility = View.VISIBLE
                            } else if (status is PrintFailed) {
                                Log.d("kitchen_retry_button","PrintFailed")
//                                view.image_kitchen_error.visibility = View.VISIBLE
                            }
                        }

                        override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
//                            view.kitchen_progress.visibility = View.INVISIBLE
                            Log.d("kitchen_retry_button","error")
//                            view.image_kitchen_error.visibility = View.VISIBLE
                        }

                    }
                )

                receiptPrintDispatch.dispatchPrint(listOf(order))
            }
            view.receipt_retry_button.setOnClickListener {
                view.image_receipt_error.visibility = View.INVISIBLE
                view.image_receipt_done.visibility = View.INVISIBLE
                view.receipt_retry_button.visibility = View.VISIBLE
                view.receipt_progress.visibility = View.VISIBLE
                val receiptPrintDispatch = ReceiptDispatch(
                    configuration.receiptPrinterConnStr,
                    ReceiptWriter,
                    configuration,
                    object : PrintStatusListener {
                        override fun printComplete(status: PrintDispatchResponse) {
//                            view.receipt_progress.visibility = View.INVISIBLE
                            if (status is PrintSuccess) {
                                Log.d("receipt_retry_button","PrintSuccess")
//                                view.image_receipt_done.visibility = View.VISIBLE
                            } else if (status is PrintFailed) {
                                Log.d("receipt_retry_button","PrintFailed")
//                                view.image_receipt_error.visibility = View.VISIBLE
                            }
                        }

                        override fun error(errorStatus: ErrorStatus, exception: Epos2Exception) {
//                            view.receipt_progress.visibility = View.INVISIBLE
                            Log.d("receipt_retry_button","error")
//                            view.image_receipt_error.visibility = View.VISIBLE
                        }

                    }
                )

                receiptPrintDispatch.dispatchPrint(listOf(order))
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
