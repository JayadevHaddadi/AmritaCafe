package edu.amrita.jayadev.amritacafe.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.epson.epos2.Epos2Exception
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.menu.MenuItem
import edu.amrita.jayadev.amritacafe.model.MenuAdapter
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.model.OrderAdapter
import edu.amrita.jayadev.amritacafe.receiptprinter.*
import edu.amrita.jayadev.amritacafe.receiptprinter.writer.ReceiptWriterImpl
import edu.amrita.jayadev.amritacafe.receiptprinter.writer.WorkOrderWriter
import edu.amrita.jayadev.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.MenuItem as ViewMenuItem


class MainActivity : AppCompatActivity(), PrintService.PrintServiceListener {

    private var dialogOpen = false

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration
    private lateinit var orderNumberService: OrderNumberService
    private var currentOrderNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.getDefaultSharedPreferences(this).let { pref ->
            configuration = Configuration(pref)
            orderNumberService = OrderNumberService(pref)
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

        orderAdapter = OrderAdapter(this)
        orderAdapter.orderChanged = {
            total_cost_TV.text = orderAdapter.orderItems.map { it.totalPrice }.sum().toString()
        }

        order_ListView.adapter = orderAdapter

        button_cancel.setOnClickListener {
            orderAdapter.clear()
        }

        order_button.setOnClickListener {
            val orders = Order(currentOrderNumber, orderAdapter.orderItems).split(orderNumberService)
            val printService = PrintService(orders, this, configuration)
            openDialog()
            printService.print()
            println("Started Print Job")
        }
    }

    override fun onBackPressed() {
        println("dialog open: $dialogOpen")
        if (!dialogOpen)
            finish()
    }

    private fun startNewOrder() {
        GlobalScope.launch {
            currentOrderNumber = orderNumberService.next()
            runOnUiThread {
                orderAdapter.clear()
                order_number_TV.text = currentOrderNumber.toString()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startNewOrder()

        menuAdapter = MenuAdapter(applicationContext, configuration.currentMenu)
        gridView.adapter = menuAdapter

        configuration.registerMealChangedListener {
            menuAdapter.setMenu(configuration.currentMenu)
        }

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItem -> {
                    orderAdapter.add( menuItem )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.switch_menu).title = configuration.currentMeal.name
        return true
    }

    override fun onOptionsItemSelected(item: ViewMenuItem): Boolean {
        return when (item.itemId) {
            R.id.switch_menu -> {
                configuration.toggleMeal()
                item.title = configuration.currentMeal.name
                true
            }
            R.id.settings -> {
                val intent = Intent(this, PreferencesActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun kitchenPrinterFinished() {
    }

    override fun receiptPrinterFinished() {
    }

    override fun receiptPrinterError(response: PrintFailed) {
    }

    override fun receiptPrinterError(errorStatus: ErrorStatus, exception: Epos2Exception) {
    }

    override fun kitchenPrinterError(response: PrintFailed) {
    }

    override fun kitchenPrinterError(errorStatus: ErrorStatus, exception: Epos2Exception) {
    }

    override fun printingComplete() {
        startNewOrder()
    }

    fun openDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(
                LayoutInflater.from(this).inflate(R.layout.response_dialog, null)
            ).setTitle("Printing")
            .setCancelable(true)
            .setPositiveButton("SHUTUP", {_, _ ->})
            .setNegativeButton("FUCKIT", {int, poop -> int.dismiss()})
            .setIcon(R.drawable.ic_print_black_24dp)
            .show()
        dialog.setCanceledOnTouchOutside(false)

    }
//    83     ┆   ┆   val mDialogView =
//        84     ┆   ┆   ┆   LayoutInflater.from(this).inflate(edu.amrita.jayadev.amritacafe.R.layout.response_dialog, null)
//    85     ┆   ┆   //AlertDialogBuilder
//    86     ┆   ┆   val mBuilder = AlertDialog.Builder(this)
//    87     ┆   ┆   ┆   .setView(mDialogView)
//    88
//    89     ┆   ┆   val mAlertDialog = mBuilder.show()
//    90     ┆   ┆   mAlertDialog.setCanceledOnTouchOutside(false)
//    91 //            mAlertDialog.onBackPressed {
//    92 //
//    93 //            }
//    94 //            mAlertDialog.setOnDismissListener {
//    95 //                println("dismiss1")
//    96 //            }
//    97 //            mAlertDialog.setOnCancelListener {
//    98 //                println("cancel")
//    99 //            }
//    100     ┆   ┆   mAlertDialog.setOnKeyListener { dialogInterface, i, keyEvent ->
//        101     ┆   ┆   ┆   println("key")
//        102     ┆   ┆   ┆   true
//        103     ┆   ┆   }
//    104     ┆   ┆   //show dialog
//    105     ┆   ┆   //login button click of custom layout
//    106     ┆   ┆   currentReceiptTV = mDialogView.receipt_TV
//    107
//
}

