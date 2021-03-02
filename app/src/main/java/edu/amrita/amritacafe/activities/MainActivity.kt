package edu.amrita.amritacafe.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import edu.amrita.amritacafe.BuildConfig
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.*
import edu.amrita.amritacafe.model.MenuAdapter
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.model.OrderAdapter
import edu.amrita.amritacafe.printer.ErrorStatus
import edu.amrita.amritacafe.printer.OrderNumberService
import edu.amrita.amritacafe.printer.PrintFailed
import edu.amrita.amritacafe.printer.PrintService
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_finish.view.receipt_progress
import kotlinx.android.synthetic.main.dialog_finish.view.receipt_retry_button
import kotlinx.android.synthetic.main.dialog_history.view.*
import kotlinx.android.synthetic.main.include_print.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


fun String.capitalizeWords(): String =
    split(" ").map { it.toLowerCase().capitalize() }.joinToString(" ")

class MainActivity : AppCompatActivity() {
    private var myToast: Toast? = null
    private lateinit var tabletName: String
    private var modeAmritapuri: Boolean = false
    private lateinit var currentHistoryFile: File

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration
    private lateinit var orderNumberService: OrderNumberService
    private var currentOrderNumber = 0
    private var sessionCash = 0f
    private var sessionCredit = 0f
    private var sessionRefund = 0f
    private var sessionDiscount = 0f
    private var currentOrderSum = 0f

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        modeAmritapuri = BuildConfig.FLAVOR == "amritapuri"
        println("Build flavor: ${BuildConfig.FLAVOR}, is amritapuri: $modeAmritapuri")

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
            "cb41899147fbbee7" -> "Amma"
            "3ebd272118138401" -> "Kali"
            "9dc83032a79a71a8" -> "Krishna"
            "c001d62ed3579582" -> "Shani"
            else -> androidId
        }
        user_TV.text = "$tabletName"

        orderAdapter = OrderAdapter(this)
        orderAdapter.orderChanged = {
            currentOrderSum = orderAdapter.orderItems.map { it.finalPrice }.sum()
            total_cost_TV.text = currentOrderSum.toString()
        }

        order_ListView.adapter = orderAdapter

        order_button.setOnClickListener {
            val order = Order(currentOrderNumber, orderAdapter.orderItems.toList())
            val orders = listOf(order)

                printOrder(orders) // just for testing history
        }

        menuGridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItemUS -> {
                    orderAdapter.add(menuItem)
                }
            }
        }

        println("STARTED?")

        updateNameForToggleButton()

        setAmritapuriMode()

    }

    private val orderHistory = mutableListOf<Order>()

    private fun storeOrders(order: Order) {
        orderHistory.add(order)
    }

    private fun setMenuAdapter(menu: List<MenuItemUS>) {
        menuAdapter =
            MenuAdapter(menu, applicationContext, configuration.showMenuItemNames) {
                runOnUiThread { menuAdapter.notifyDataSetChanged() }
            }
        runOnUiThread { menuGridView.adapter = menuAdapter }
    }

    private fun finishOrder(cash: Boolean = false) {
        currentHistoryFile.appendText("Order: $currentOrderNumber\n")
        var orderDiscount = 0f
        var orderRefund = 0f
        for (item in orderAdapter.orderItems) {
            orderDiscount += item.discount
            orderRefund += item.refund
            currentHistoryFile.appendText(
                "${item.quantity} ${item.menuItem.name}: ${item.finalPrice}$" +
                        "${if (item.comment.length > 0) " (" + item.comment + ")" else ""}\n"
            )
        }
        currentHistoryFile.appendText("Order   total: ${currentOrderSum}$ ${if (cash) "cash" else "credit"}\n")
        if (cash)
            sessionCash += currentOrderSum
        else
            sessionCredit += currentOrderSum

        sessionRefund += orderRefund
        sessionDiscount += orderDiscount
        currentHistoryFile.appendText("Session Discount: ${sessionDiscount}$, Refund: ${sessionRefund}$\n")
        currentHistoryFile.appendText("Session total: ${sessionCash}$ cash, ${sessionCredit}$ credit\n\n")

        println("Text in file : \n" + currentHistoryFile.readText())

        startNewOrder()
    }

    private fun makeToast(text: String) {
        myToast?.cancel()
        myToast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        myToast?.show()
    }

    override fun onResume() {
        super.onResume()
        if (modeAmritapuri) loadAmritapuriMenu()
    }

    fun loadAmritapuriMenu() {
        val file = if (configuration.isBreakfastTime) BREAKFAST_FILE else LUNCH_DINNER_FILE
        val list = getListOfMenu(file)
        setMenuAdapter(list)
    }

    private fun setAmritapuriMode() {
        modeAmritapuri = true
        order_button.text = "Order"
        user_TV.text = "Amritapuri @ $tabletName"
        refund_button.visibility = View.GONE
        discount_100_button.visibility = View.GONE
        discount_50_button.visibility = View.GONE
        discount_25_button.visibility = View.GONE
        finish_button.visibility = View.GONE

        println("Time: ${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}")
        configuration.isBreakfastTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 11

        createDefualtFilesIfNecessary()
        loadAmritapuriMenu() //will load on resume

        startNewOrder()
    }

    private fun createHistoryFile(user: String) {
        println("Logged in as: $user")
        startNewOrder()
        val root = File(Environment.getExternalStorageDirectory(), "Amrita Cafe")
        if (!root.exists())
            root.mkdirs()

        val currentTime = Calendar.getInstance().time
        val dateFormatString = "yyyy-MM-dd kk-mm" // hh-mm a for pm/am
        val dateFormat = SimpleDateFormat(dateFormatString, Locale.US)
        val fileDate = dateFormat.format(currentTime)
        val fileName = "${fileDate} - $user.txt"

        currentHistoryFile = File(root, fileName)
        currentHistoryFile.appendText(
            "Session: ${fileDate}\n" +
                    "User: $user\n" +
                    "Tablet: $tabletName\n"
//                    + "App Version: ${BuildConfig.VERSION_NAME}\n\n"
        )
    }

//    override fun onBackPressed() {
//        println("dialog open: $dialogOpen")
//        if (!dialogOpen) {
//            if (!modeAmritapuri) {
//                openExitDialog()
//            } else
//                finish()
//        }
//    }

    private fun startNewOrder() {
        GlobalScope.launch {
            println("New order, printMode: $modeAmritapuri")
            if (modeAmritapuri)
                currentOrderNumber = orderNumberService.next()
            else
                currentOrderNumber++

            val order = Order(currentOrderNumber, orderAdapter.orderItems.toList())
            storeOrders(order)

            runOnUiThread {
                orderAdapter.clear()
                order_number_TV.text = currentOrderNumber.toString()
            }
        }
    }

    private lateinit var currentDialog: AlertDialog

    private fun printOrder(orders: List<Order>) {
        val (dialog, view) = printerDialog()
        currentDialog = dialog

        val printService = PrintService(
            orders, configuration = configuration,
            listener = object : PrintService.PrintServiceListener {
                override fun kitchenPrinterFinished() = runOnUiThread {
                    view.run {
                        kitchen_progress.visibility = View.INVISIBLE
                        image_kitchen_error.visibility = View.INVISIBLE
                        image_kitchen_done.visibility = View.VISIBLE
                        kitchen_retry_button.visibility = View.INVISIBLE
                    }
                }

                override fun receiptPrinterFinished() = runOnUiThread {
                    view.run {
                        receipt_progress.visibility = View.INVISIBLE
                        image_receipt_error.visibility = View.INVISIBLE
                        image_receipt_done.visibility = View.VISIBLE
                        receipt_retry_button.visibility = View.INVISIBLE
                    }
                }

                override fun receiptPrinterError(response: PrintFailed) = runOnUiThread {
                    view.run {
                        receipt_progress.visibility = View.INVISIBLE
                        image_receipt_error.visibility = View.VISIBLE
                        receipt_retry_button.visibility = View.VISIBLE
                    }
                }

                override fun receiptPrinterError(
                    errorStatus: ErrorStatus,
                    exception: Epos2Exception
                ) = runOnUiThread {
                    view.run {
                        receipt_progress.visibility = View.INVISIBLE
                        image_receipt_error.visibility = View.VISIBLE
                        receipt_retry_button.visibility = View.VISIBLE
                    }
                }

                override fun kitchenPrinterError(response: PrintFailed) = runOnUiThread {
                    view.run {
                        kitchen_progress.visibility = View.INVISIBLE
                        image_kitchen_error.visibility = View.VISIBLE
                        kitchen_retry_button.visibility = View.VISIBLE
                    }
                }

                override fun kitchenPrinterError(
                    errorStatus: ErrorStatus,
                    exception: Epos2Exception
                ) = runOnUiThread {
                    view.run {
                        kitchen_progress.visibility = View.INVISIBLE
                        image_kitchen_error.visibility = View.VISIBLE
                        kitchen_retry_button.visibility = View.VISIBLE
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

        view.kitchen_retry_button.setOnClickListener {
            printService.retry()
            it.visibility = View.INVISIBLE
            view.image_kitchen_error.visibility = View.INVISIBLE
            view.kitchen_progress.visibility = View.VISIBLE
        }

        view.receipt_retry_button.setOnClickListener {
            printService.retry()
            it.visibility = View.INVISIBLE
            view.image_receipt_error.visibility = View.INVISIBLE
            view.receipt_progress.visibility = View.VISIBLE
        }

        println("Started Print Job")
    }

    @SuppressLint("InflateParams")
    private fun printerDialog() =
        LayoutInflater.from(this).inflate(R.layout.dialog_print, null).let { view ->
            AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
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
        makeToast("So many old orders: ${orderHistory.size}")
        val (dialog, root) =
            LayoutInflater.from(this).inflate(R.layout.dialog_history, null).let { view ->
                AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(true)
                    .show()
                    .to(view)
            }

        root.history_RV.layoutManager = LinearLayoutManager(this)
        val historyAdapter = HistoryAdapter(orderHistory,configuration)
        root.history_RV.adapter = historyAdapter
    }

    fun nextOrder(view: View) {
        runOnUiThread {
            currentDialog.dismiss()
        }
        startNewOrder()
    }
}