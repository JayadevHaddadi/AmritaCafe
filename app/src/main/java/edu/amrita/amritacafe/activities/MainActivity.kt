package edu.amrita.amritacafe.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.*
import edu.amrita.amritacafe.model.*
import edu.amrita.amritacafe.printer.ErrorStatus
import edu.amrita.amritacafe.printer.OrderNumberService
import edu.amrita.amritacafe.printer.PrintFailed
import edu.amrita.amritacafe.printer.PrintService
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_history.view.*
import kotlinx.android.synthetic.main.include_print.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*


fun String.capitalizeWords(): String =
    split(" ").map { it.toLowerCase().capitalize() }.joinToString(" ")

class MainActivity : AppCompatActivity() {
    private lateinit var allCurrentCategories: MutableList<String>
    private var myToast: Toast? = null

    private lateinit var tabletName: String
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration
    private lateinit var orderNumberService: OrderNumberService

    private val orderHistory = mutableListOf<HistoricalOrder>()

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.let { preferences ->
            configuration = Configuration(preferences)
            orderNumberService = OrderNumberService(preferences)
        }

        supportActionBar?.hide()

        val androidId =
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        println("Unique device: $androidId")
        tabletName = when (androidId) {
            "f6ec19ab2b07a2f2" -> "Siva"
            "b3f76281fb4a42aa" -> "Amma"
            "3ebd272118138401" -> "Kali"
            "9dc83032a79a71a8" -> "Krishna"
            "c001d62ed3579582" -> "Shani"
            else -> androidId
        }
        user_TV.text = "$tabletName"

        orderAdapter = OrderAdapter(this)
        orderAdapter.orderChanged = {
            total_cost_TV.text =
                orderAdapter.orderItems.map { it.priceWithoutToppings }.sum().toString()
        }

        order_ListView.adapter = orderAdapter

        order_button.setOnClickListener {
            printOrder() // just for testing history
        }

        menuGridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItem -> {
                    if(orderAdapter.add(menuItem) == -1)
                    {
                        makeToast("Unsupported Action!")
                    }
                }
            }
        }

        updateNameForToggleButton()
        user_TV.text = "Amritapuri @ $tabletName"

        println("Time: ${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}")
        configuration.isBreakfastTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 11

        createDefualtFilesIfNecessary()
        loadMenu() //will load on resume

        order_number_ET.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action === KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
//                Toast.makeText(this, , Toast.LENGTH_SHORT).show()
                makeToast(order_number_ET.text.toString())
                orderNumberService.currentOrderNumber = order_number_ET.text.toString().toInt()
                return@OnKeyListener true
            }
            false
        })
    }

    private fun setMenuAdapter(menu: List<MenuItem>) {
        menuGridView.numColumns = configuration.columns
        menuAdapter =
            MenuAdapter(menu, applicationContext, configuration.showMenuItemNames, configuration) {
                runOnUiThread { menuAdapter.notifyDataSetChanged() }
            }
        runOnUiThread { menuGridView.adapter = menuAdapter }
    }

    private fun makeToast(text: String) {
        myToast?.cancel()
        myToast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        myToast?.show()
    }

    override fun onResume() {
        super.onResume()
        loadMenu()
    }

    fun loadMenu() {
        val file = if (configuration.isBreakfastTime) BREAKFAST_FILE else LUNCH_DINNER_FILE
        val list = getListOfMenu(file)

        allCurrentCategories = mutableListOf()

        list.forEach {
            if (!allCurrentCategories.contains(it.category))
                allCurrentCategories.add(it.category)
        }

        orderNumberService.updateRange()
        order_number_ET.setText(orderNumberService.currentOrderNumber.toString())

        setMenuAdapter(list)
    }

    private fun startNewOrder() {
        GlobalScope.launch {
//            orderNumberService.next()

            runOnUiThread {
                orderAdapter.clear()
                order_number_ET.setText(orderNumberService.currentOrderNumber.toString())
            }
        }
    }

    private lateinit var currentDialog: AlertDialog

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this,R.style.DialogStyle)
        builder.setTitle("Close Amrita Cafe?")
        builder.setCancelable(true)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            super.onBackPressed()
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which ->
        }

        builder.show()
    }

    private fun printOrder() {
        val orderItemsCopy = orderAdapter.orderItems.toMutableList()

        var pos = 0
        orderItemsCopy.forEach {
            if (it.menuItem.category == TOPPING) {
                for (i in pos downTo 0) {
                    if (orderItemsCopy[i].menuItem.category != TOPPING) {
                        orderItemsCopy[i].addTopping(it)
                        break
                    }
                }
            }
            pos++
        }
        val orderList = orderItemsCopy.filter {
            it.menuItem.category != TOPPING
        }

        var hasPizza = false
        var hasGrill = false
        orderList.forEach {
            when (it.menuItem.category) {
                PIZZA -> hasPizza = true
                BREAKFAST, SANDWICH, EGGS, TOAST, BURGER -> hasGrill = true
            }
        }

        val orders = if (hasPizza and hasGrill) {
            listOf(
                Order(
                    orderNumberService.currentOrderNumber,
                    orderList.filter { it.menuItem.category != PIZZA }
                ),
                Order(
                    runBlocking { orderNumberService.next() },
                    orderList.filter { it.menuItem.category == PIZZA }
                )
            )
        } else {
            listOf(Order(orderNumberService.currentOrderNumber, orderList))
        }

        val histories = mutableListOf<HistoricalOrder>()

        orders.forEach {
            it.orderItems = it.orderItems.sortedBy {
                allCurrentCategories.indexOf(it.menuItem.category)
            }

            val historicalOrder = HistoricalOrder(it)
            histories.add(historicalOrder)
            orderHistory.add(historicalOrder)
        }

        val (dialog, view) = printerDialog()
        currentDialog = dialog

        val printService = PrintService(
            orders, configuration = configuration,
            listener = object : PrintService.PrintServiceListener {
                override fun kitchenPrinterFinished() = runOnUiThread {
                    histories.forEach {
                        it.KitchenPrinted = PrintStatus.SUCCESS_PRINT
                    }
                    view.run {
                        kitchen_progress.visibility = View.INVISIBLE
                        kitchen_error.visibility = View.INVISIBLE
                        kitchen_done.visibility = View.VISIBLE
                        kitchen_retry_button.visibility = View.INVISIBLE
                    }
                }

                override fun kitchenPrinterError(response: PrintFailed) = runOnUiThread {
                    histories.forEach {
                        it.KitchenPrinted = PrintStatus.FAILED_PRINT
                    }
                    view.run {
                        kitchen_progress.visibility = View.INVISIBLE
                        kitchen_error.visibility = View.VISIBLE
                        kitchen_retry_button.visibility = View.VISIBLE
                    }
                }

                override fun kitchenPrinterError(
                    errorStatus: ErrorStatus,
                    exception: Epos2Exception
                ) = runOnUiThread {
                    histories.forEach {
                        it.KitchenPrinted = PrintStatus.FAILED_PRINT
                    }
                    view.run {
                        kitchen_progress.visibility = View.INVISIBLE
                        kitchen_error.visibility = View.VISIBLE
                        kitchen_retry_button.visibility = View.VISIBLE
                    }
                }

                override fun receiptPrinterFinished() = runOnUiThread {
                    histories.forEach {
                        it.RecipePrinted = PrintStatus.SUCCESS_PRINT
                    }
                    view.run {
                        receipt_progress.visibility = View.INVISIBLE
                        receipt_error.visibility = View.INVISIBLE
                        receipt_done.visibility = View.VISIBLE
                        receipt_retry_button.visibility = View.INVISIBLE
                    }
                }

                override fun receiptPrinterError(response: PrintFailed) = runOnUiThread {
                    histories.forEach {
                        it.RecipePrinted = PrintStatus.FAILED_PRINT
                    }
                    view.run {
                        receipt_progress.visibility = View.INVISIBLE
                        receipt_error.visibility = View.VISIBLE
                        receipt_retry_button.visibility = View.VISIBLE
                    }
                }

                override fun receiptPrinterError(
                    errorStatus: ErrorStatus,
                    exception: Epos2Exception
                ) = runOnUiThread {
                    histories.forEach {
                        it.RecipePrinted = PrintStatus.FAILED_PRINT
                    }
                    view.run {
                        receipt_progress.visibility = View.INVISIBLE
                        receipt_error.visibility = View.VISIBLE
                        receipt_retry_button.visibility = View.VISIBLE
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
        GlobalScope.launch {
            orderNumberService.next()

//            runOnUiThread {
//                orderAdapter.clear()
//                order_number_ET.setText(orderNumberService.currentOrderNumber.toString())
//            }
        }

        view.kitchen_retry_button.setOnClickListener {
            histories.forEach {
                it.KitchenPrinted = PrintStatus.PRINTING
            }
            printService.retry()
            it.visibility = View.INVISIBLE
            view.kitchen_error.visibility = View.INVISIBLE
            view.kitchen_progress.visibility = View.VISIBLE
        }

        view.receipt_retry_button.setOnClickListener {
            histories.forEach {
                it.RecipePrinted = PrintStatus.PRINTING
            }
            printService.retry()
            it.visibility = View.INVISIBLE
            view.receipt_error.visibility = View.INVISIBLE
            view.receipt_progress.visibility = View.VISIBLE
        }

        println("Started Print Job")
    }

    @SuppressLint("InflateParams")
    private fun printerDialog() =
        LayoutInflater.from(this).inflate(R.layout.dialog_print, null).let { view ->
            AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
//                .setIcon(R.drawable.ic_print_black_24dp)
                .show()
                .apply {
                    setCanceledOnTouchOutside(false)
                }.to(view)
        }

    fun toggleShortLongName(view: View) {
        configuration.toggleName()
        updateNameForToggleButton()
        menuAdapter.showFullName = configuration.showMenuItemNames
    }

    private fun updateNameForToggleButton() {
        short_long_toggle_button.text =
            if (configuration.showMenuItemNames) "Short Names" else "Long Names"
    }

    fun openSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun openHistoryDialog(historyButton: View) {
//        makeToast("So many old orders: ${orderHistory.size}")
        val (dialog, root) =
            LayoutInflater.from(this).inflate(R.layout.dialog_history, null).let { view ->
                AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(true)
                    .show()
                    .to(view)
            }

        val layoutManager = LinearLayoutManager(this)
//        layoutManager.reverseLayout = true
        root.history_RV.layoutManager = layoutManager

        val historyAdapter = HistoryAdapter(orderHistory, configuration, this)
        root.history_RV.adapter = historyAdapter
        root.history_RV.scrollToPosition(orderHistory.size - 1)
    }

    fun nextOrder(view: View) {
        runOnUiThread {
            currentDialog.dismiss()
        }
        startNewOrder()
    }

    fun deleteOrder(view: View) {
        orderAdapter.clear()
    }
}