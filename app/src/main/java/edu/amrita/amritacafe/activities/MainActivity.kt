package edu.amrita.amritacafe.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.Category
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.model.MenuAdapter
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.model.OrderAdapter
import edu.amrita.amritacafe.receiptprinter.*
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.response_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.MenuItem as ViewMenuItem


class MainActivity : AppCompatActivity() {

    private var dialogOpen = false

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration
    private lateinit var orderNumberService: OrderNumberService
    private var currentOrderNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
            val orders = Order(currentOrderNumber, orderAdapter.orderItems)
                .collectToppings().split(orderNumberService)

            val (dialog, view) = openDialog()

            val printService = PrintService(orders, configuration = configuration,
                listener = object : PrintService.PrintServiceListener {
                    override fun kitchenPrinterFinished() = runOnUiThread {
                        view.run {
                            kitchen_progress.visibility = View.INVISIBLE
                            image_kitchen_error.visibility = View.INVISIBLE
                            image_kitchen_done.visibility = View.VISIBLE
                            button_retry_kitchen.visibility = View.INVISIBLE
                        }
                    }

                    override fun receiptPrinterFinished() = runOnUiThread {
                        view.run {
                            receipt_progress.visibility = View.INVISIBLE
                            image_receipt_error.visibility = View.INVISIBLE
                            image_receipt_done.visibility = View.VISIBLE
                            button_retry_receipt.visibility = View.INVISIBLE
                        }
                    }

                    override fun receiptPrinterError(response: PrintFailed) = runOnUiThread {
                        view.run {
                            receipt_progress.visibility = View.INVISIBLE
                            image_receipt_error.visibility = View.VISIBLE
                            button_retry_receipt.visibility = View.VISIBLE
                        }
                    }

                    override fun receiptPrinterError(
                        errorStatus: ErrorStatus,
                        exception: Epos2Exception
                    ) = runOnUiThread {
                        view.run {
                            receipt_progress.visibility = View.INVISIBLE
                            image_receipt_error.visibility = View.VISIBLE
                            button_retry_receipt.visibility = View.VISIBLE
                        }
                    }

                    override fun kitchenPrinterError(response: PrintFailed) = runOnUiThread {
                        view.run {
                            kitchen_progress.visibility = View.INVISIBLE
                            image_kitchen_error.visibility = View.VISIBLE
                            button_retry_kitchen.visibility = View.VISIBLE
                        }
                    }

                    override fun kitchenPrinterError(
                        errorStatus: ErrorStatus,
                        exception: Epos2Exception
                    ) = runOnUiThread {
                        view.run {
                            kitchen_progress.visibility = View.INVISIBLE
                            image_kitchen_error.visibility = View.VISIBLE
                            button_retry_kitchen.visibility = View.VISIBLE
                        }
                    }

                    override fun printingComplete() {
                        runOnUiThread {
                            dialog.dismiss()
                        }
                        startNewOrder()
                    }

                })
            printService.print()

            view.button_retry_kitchen.setOnClickListener {
                printService.retry()
                it.visibility = View.INVISIBLE
                view.image_kitchen_error.visibility = View.INVISIBLE
                view.kitchen_progress.visibility = View.VISIBLE
            }

            view.button_retry_receipt.setOnClickListener {
                printService.retry()
                it.visibility = View.INVISIBLE
                view.image_receipt_error.visibility = View.INVISIBLE
                view.receipt_progress.visibility = View.VISIBLE
            }

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

        menuAdapter = MenuAdapter(configuration, applicationContext) {
            runOnUiThread { menuAdapter.notifyDataSetChanged() }
        }
        gridView.adapter = menuAdapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItem -> {
                    orderAdapter.add( menuItem )
                }
            }
        }
        gridView.onItemLongClickListener = AdapterView.OnItemLongClickListener {_, view, _, _ ->
            val menuItem = view.tag
            menuItem is MenuItem
                    && menuItem.category == Category.Topping
                    && orderAdapter.isNotEmpty()
                    && orderAdapter.addTopping(menuItem).let {
                println("I added it.")
                true
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

    /**
     * Returns a tuple containing the dialog, and the view.
     */
    @SuppressLint("InflateParams")
    private fun openDialog() = LayoutInflater.from(this).inflate(R.layout.response_dialog, null).let { view ->
        AlertDialog.Builder(this)
        .setView(view).setTitle("Printing")
        .setCancelable(true)
        .setIcon(R.drawable.ic_print_black_24dp)
        .show()
        .apply {
            setCanceledOnTouchOutside(false)
        }.to(view)
    }
}

