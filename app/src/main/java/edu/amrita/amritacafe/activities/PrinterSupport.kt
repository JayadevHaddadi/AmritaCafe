package edu.amrita.amritacafe.activities
//
//import android.annotation.SuppressLint
//import android.view.LayoutInflater
//import android.view.View
//import androidx.appcompat.app.AlertDialog
//import com.epson.epos2.Epos2Exception
//import edu.amrita.amritacafe.R
//import edu.amrita.amritacafe.model.Order
//import edu.amrita.amritacafe.receiptprinter.ErrorStatus
//import edu.amrita.amritacafe.receiptprinter.PrintFailed
//import edu.amrita.amritacafe.receiptprinter.PrintService
//import kotlinx.android.synthetic.main.dialog_print.view.*
//
///**
// * Returns a tuple containing the dialog, and the view.
// */
//@SuppressLint("InflateParams")
//private fun openDialog() =
//    LayoutInflater.from(this).inflate(R.layout.dialog_print, null).let { view ->
//        AlertDialog.Builder(this)
//            .setView(view).setTitle("Printing")
//            .setCancelable(true)
//            .setIcon(R.drawable.ic_print_black_24dp)
//            .show()
//            .apply {
//                setCanceledOnTouchOutside(false)
//            }.to(view)
//    }
//fun printOrder(orders: List<Order>) {
//    val (dialog, view) = openDialog()
//
//    val printService = PrintService(
//        orders, configuration = configuration,
//        listener = object : PrintService.PrintServiceListener {
//            override fun kitchenPrinterFinished() = runOnUiThread {
//                view.run {
//                    kitchen_progress.visibility = View.INVISIBLE
//                    image_kitchen_error.visibility = View.INVISIBLE
//                    image_kitchen_done.visibility = View.VISIBLE
//                    button_finish.visibility = View.INVISIBLE
//                }
//            }
//
//            override fun receiptPrinterFinished() = runOnUiThread {
//                view.run {
//                    email_progress.visibility = View.INVISIBLE
//                    image_receipt_error.visibility = View.INVISIBLE
//                    image_receipt_done.visibility = View.VISIBLE
//                    button_cancel.visibility = View.INVISIBLE
//                }
//            }
//
//            override fun receiptPrinterError(response: PrintFailed) = runOnUiThread {
//                view.run {
//                    email_progress.visibility = View.INVISIBLE
//                    image_receipt_error.visibility = View.VISIBLE
//                    button_cancel.visibility = View.VISIBLE
//                }
//            }
//
//            override fun receiptPrinterError(
//                errorStatus: ErrorStatus,
//                exception: Epos2Exception
//            ) = runOnUiThread {
//                view.run {
//                    email_progress.visibility = View.INVISIBLE
//                    image_receipt_error.visibility = View.VISIBLE
//                    button_cancel.visibility = View.VISIBLE
//                }
//            }
//
//            override fun kitchenPrinterError(response: PrintFailed) = runOnUiThread {
//                view.run {
//                    kitchen_progress.visibility = View.INVISIBLE
//                    image_kitchen_error.visibility = View.VISIBLE
//                    button_finish.visibility = View.VISIBLE
//                }
//            }
//
//            override fun kitchenPrinterError(
//                errorStatus: ErrorStatus,
//                exception: Epos2Exception
//            ) = runOnUiThread {
//                view.run {
//                    kitchen_progress.visibility = View.INVISIBLE
//                    image_kitchen_error.visibility = View.VISIBLE
//                    button_finish.visibility = View.VISIBLE
//                }
//            }
//
//            override fun printingComplete() {
//                runOnUiThread {
//                    dialog.dismiss()
//                }
//                startNewOrder()
//            }
//
//        })
//
//    printService.print()
//
//    view.button_finish.setOnClickListener {
//        printService.retry()
//        it.visibility = View.INVISIBLE
//        view.image_kitchen_error.visibility = View.INVISIBLE
//        view.kitchen_progress.visibility = View.VISIBLE
//    }
//
//    view.button_cancel.setOnClickListener {
//        printService.retry()
//        it.visibility = View.INVISIBLE
//        view.image_receipt_error.visibility = View.INVISIBLE
//        view.email_progress.visibility = View.VISIBLE
//    }
//
//    println("Started Print Job")
//}