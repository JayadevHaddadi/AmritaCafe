package edu.amrita.amritacafe.activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.epson.epos2.Epos2Exception
import com.example.hoinprinterlib.HoinPrinter
import com.example.hoinprinterlib.module.PrinterCallback
import com.example.hoinprinterlib.module.PrinterEvent
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.*
import edu.amrita.amritacafe.model.*
import edu.amrita.amritacafe.printer.ErrorStatus
import edu.amrita.amritacafe.printer.OrderNumberService
import edu.amrita.amritacafe.printer.PrintFailed
import edu.amrita.amritacafe.printer.PrintService
import edu.amrita.amritacafe.printer.writer.ReceiptWriter
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_history.view.*
import kotlinx.android.synthetic.main.include_print.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*


fun String.capitalizeWords(): String =
    split(" ").map { it.toLowerCase().capitalize() }.joinToString(" ")


class MainActivity : AppCompatActivity() {

    val WIFI = 0
    val BLUETOOTH = 1

    val BT_STATE_DISCONNECTED = 0; //Bluetooth disconnected
    val BT_STATE_LISTEN = 1; //Bluetooth is listening
    val BT_STATE_CONNECTING = 2; //Bluetooth connecting
    val BT_STATE_CONNECTED = 3; //Bluetooth connected
    var BT_STATE = BT_STATE_DISCONNECTED

    private lateinit var mHoinPrinter: HoinPrinter
    private lateinit var devices: MutableSet<BluetoothDevice>
    private lateinit var allCurrentCategories: MutableList<String>
    private var myToast: Toast? = null

    private lateinit var tabletName: String
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration
    private lateinit var orderNumberService: OrderNumberService

    private val orderHistory = mutableListOf<HistoricalOrder>()

    companion object {
        lateinit var BREAKFAST_FILE: File
        lateinit var LUNCH_DINNER_FILE: File
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        BREAKFAST_FILE = File(filesDir, "Breakfast.txt")
        LUNCH_DINNER_FILE = File(filesDir, "LunchDinner.txt")

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.let { preferences ->
            configuration = Configuration(preferences)
            orderNumberService = OrderNumberService(preferences)
        }

        supportActionBar?.hide()

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE), 1)
//        }

        val androidId =
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        println("Unique device: $androidId")
        tabletName = when (androidId) {
            "f6ec19ab2b07a2f2" -> "Siva"
            "b3f76281fb4a42aa" -> "Amma"
            "3ebd272118138401" -> "Kali"
            "9dc83032a79a71a8" -> "Krishna"
            "c001d62ed3579582" -> "Shani"
            "3ec2fefa5a9e906e" -> "Saraswati"
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
                    if (orderAdapter.add(menuItem) == -1) {
                        makeToast("Unsupported Action!")
                    }
                }
            }
        }

        updateNameForToggleButton()
        user_TV.text = "Amritapuri @ $tabletName"

        println("Time: ${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}")
        configuration.isBreakfastTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 11

        createDefualtFilesIfNecessary(baseContext)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            println("Jayadev 2")
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }

        mHoinPrinter = HoinPrinter.getInstance(this, 1, object : PrinterCallback {
            override fun onState(p0: Int) {
                BT_STATE = p0
                var message = when (p0) {
                    BT_STATE_CONNECTING -> "Connecting... "
                    BT_STATE_CONNECTED -> "Connected!"
                    BT_STATE_LISTEN -> "Listening... "
                    BT_STATE_DISCONNECTED -> "Disconnected!"
                    else -> "STATUS $p0"
                }
                makeToast(message)
                println(message)
            }

            override fun onError(p0: Int) {
                makeToast("onError " + p0)
                println("JAYADEV onError $p0")
            }

            override fun onEvent(p0: PrinterEvent?) {
                makeToast("onEvent " + p0)
                println("JAYADEV onEvent $p0")
            }
        })
        mHoinPrinter.switchType(true);
        tryConnect()
    }

    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                //granted
            } else {
                //deny
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
//                Log.d("test006", "${it.key} = ${it.value}")
            }
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
//        if (configuration.mode == BLUETOOTH && BT_STATE == BT_STATE_DISCONNECTED)
//            tryConnect()
    }

    fun tryConnect() = runBlocking() {
        launch {
            delay(1000L)
            println("World")

            devices = mHoinPrinter.pairedDevice
            println("JAYADEV $devices")
            for (x in devices) {
                println(x)
                println("JAYADEV: ${x.name} ${x.address} ${x.alias} ${x.type} ")
            }

            println("JAYADEV MODE ${configuration.mode}")
            println("JAYADEV BT_STATE $BT_STATE")
            if (configuration.mode == BLUETOOTH && BT_STATE == BT_STATE_DISCONNECTED) {
                val selection =
                    devices.filter { bluetoothDevice -> bluetoothDevice.name == configuration.bluetoothName }
                println("JAYADEV SELECTION $selection")
                mHoinPrinter.connect(selection.first().address)
            }
        }

        println("JAYADEV MODE ${configuration.mode}")
        println("JAYADEV BT_STATE $BT_STATE")
        if (configuration.mode == BLUETOOTH && BT_STATE == BT_STATE_DISCONNECTED)
            mHoinPrinter.startBtDiscovery()
        println("Hello")

    }

    val BT_NOT_AVALIBLE = 1000; //Bluetooth is not available on this device
    val BT_UNABLE_CONNECT_TO_DEVICE = 1001; //Unable to connect to Bluetooth
    val BT_CONNECTION_LOST = 1002; //Unable to connect Bluetooth or Bluetooth disconnect
    val CONTEXT_ERROR = 1003; //Context error
    val WIFI_SEND_FAILED = 1004; //WIFI failed to send data
    val WIFI_CONNECT_ERROR = 1005; //WIFI connection failed
    val USB_NOT_FIND_DEVICE = 1006; //USB connection failed
    val USB_NO_PERMISSION = 1007; //USB does not have permission
    val BT_NO_PERMISSION = 1008; //Bluetooth has no permissions
    val DEVICE_NOT_CONNECTED = 1009; //Device not connected
    val IMAGE_NOT_FONUD = 1010; //Device not connected
    val NULL_POINTER_EXCEPTION = 9999; // null pointer exception

    fun loadMenu() {
        val file = if (configuration.isBreakfastTime) File(
            baseContext.filesDir,
            "Breakfast.txt"
        ) else File(baseContext.filesDir, "LunchDinner.txt")
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
        val builder = AlertDialog.Builder(this, R.style.DialogStyle)
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

        if (configuration.mode == WIFI) {
            val (dialog, view) = printerDialog()
            currentDialog = dialog

            val listener = object : PrintService.PrintServiceListener {
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

            }

            val printService = PrintService(orders, listener, configuration = configuration)
            printService.print()

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

        } else if (configuration.mode == BLUETOOTH) {
            orders.forEach { (orderNumber, orderItems, time) ->
                val orderTotalText = orderItems.map { it.priceWithToppings }.sum().toString()
                val orderNumStr = orderNumber.toString().padStart(3, '0')

//                val path = Uri.parse("android.resource://edu.amrita.amritacafe3/" + R.drawable.logo)
//                val otherPath = Uri.parse("android.resource://edu.amrita.amritacafe3/drawable/logo")
//                mHoinPrinter.printImage(otherPath.toString(), true)
                mHoinPrinter.printText(
                    "Amrita Cafe",
                    true, true, true, true
                )
                mHoinPrinter.printText(
                    "$orderNumStr        $time",
                    false, false, false, false
                )

                mHoinPrinter.printText(
                    ReceiptWriter.orderItemsText(orderItems),
                    false,
                    false,
                    false,
                    false
                )

                mHoinPrinter.printText(
                    "TOTAL" + orderTotalText.padStart(15),
                    false,
                    false,
                    false,
                    false
                )

                mHoinPrinter.printText("\n", false, false, false, false)
            }

            startNewOrder()
        }

        GlobalScope.launch {
            orderNumberService.next()
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