package edu.amrita.amritacafe.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.epson.epos2.Epos2Exception
import com.example.hoinprinterlib.HoinPrinter
import com.example.hoinprinterlib.module.PrinterCallback
import com.example.hoinprinterlib.module.PrinterEvent
import edu.amrita.amritacafe.CloudStorage.sendToSheets
import edu.amrita.amritacafe.IO.createDefaultFilesIfNecessary
import edu.amrita.amritacafe.IO.getListOfMenu
import edu.amrita.amritacafe.IO.saveIfValidText
import edu.amrita.amritacafe.IO.writeToCSV
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.BuildConfig
import edu.amrita.amritacafe.databinding.ActivityMainBinding
import edu.amrita.amritacafe.databinding.DialogHistoryBinding
import edu.amrita.amritacafe.databinding.DialogPaymentBinding
import edu.amrita.amritacafe.databinding.DialogPrintBinding
import edu.amrita.amritacafe.menu.*
import edu.amrita.amritacafe.model.*
import edu.amrita.amritacafe.printer.ErrorStatus
import edu.amrita.amritacafe.printer.OrderNumberService
import edu.amrita.amritacafe.printer.PrintFailed
import edu.amrita.amritacafe.printer.PrintService
import edu.amrita.amritacafe.printer.bluetooth.bluetoothPrint
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*
import kotlin.math.max


fun String.capitalizeWords(): String =
    split(" ").map { it.toLowerCase().capitalize() }.joinToString(" ")


class MainActivity : AppCompatActivity() {

    private lateinit var backGround: View
    private lateinit var dialogView: View
    private lateinit var overlay: View
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

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        BREAKFAST_FILE = File(filesDir, "Breakfast.txt")
        LUNCH_DINNER_FILE = File(filesDir, "LunchDinner.txt")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.let { preferences ->
            configuration = Configuration(preferences)
            orderNumberService = OrderNumberService(preferences)
        }

        supportActionBar?.hide()

        orderAdapter = OrderAdapter(this)
        orderAdapter.orderChanged = {
            binding.totalCostTV.text =
                orderAdapter.orderItems.map { it.priceWithoutExtras }.sum().toString()
        }

        binding.orderListView.adapter = orderAdapter

        binding.orderButton.setOnClickListener {
            printOrder()
        }

        binding.menuGridView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view, _, _ ->
                when (val menuItem = view.tag) {
                    is MenuItem -> {
                        if (orderAdapter.add(menuItem) == -1) {
                            makeToast("Unsupported Action!")
                        }
                    }
                }
            }

        updateNameForToggleButton()
        tabletName = "Unknown Tablet"
        binding.userTV.text = "Amritapuri @ $tabletName"

        createDefaultFilesIfNecessary(baseContext)
//        loadMenu() //will load on resume

        binding.orderNumberET.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action === KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
//                Toast.makeText(this, , Toast.LENGTH_SHORT).show()
                makeToast(binding.orderNumberET.text.toString())
                orderNumberService.currentOrderNumber =
                    binding.orderNumberET.text.toString().toInt()
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

        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                BLUETOOTH_CONNECT_REQUEST_CODE
            )
        } else {
            // Permission has already been granted, proceed with accessing Bluetooth functionalities
            tryConnect()
        }

        mHoinPrinter = HoinPrinter.getInstance(this, 1, object : PrinterCallback {
            override fun onState(newStateCode: Int) {
                BT_STATE = newStateCode
                var message = when (newStateCode) {
                    BT_STATE_CONNECTING -> "Connecting... "
                    BT_STATE_CONNECTED -> "Connected!"
                    BT_STATE_LISTEN -> "Listening... "
                    BT_STATE_DISCONNECTED -> "Disconnected!"
                    else -> "STATUS $newStateCode"
                }
                makeToast(message)
                println(message)
            }

            override fun onError(p0: Int) {
                makeToast("onError " + p0)
                println("JAYADEV onError $p0")
            }

            override fun onEvent(p0: PrinterEvent?) {
                makeToast("onEvent $p0")
                println("JAYADEV onEvent $p0")
            }
        })
        mHoinPrinter.switchType(true);
    }

    private val BLUETOOTH_CONNECT_REQUEST_CODE = 101

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BLUETOOTH_CONNECT_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    tryConnect()
                } else {
                    makeToast("Bluetooth permission denied")
                }
            }
            2 -> { // REQUEST_EXTERNAL_STORAGE
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UpdateChecker.checkForUpdates(this)
                }
            }
        }
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
        binding.menuGridView.numColumns = configuration.columns
        menuAdapter =
            MenuAdapter(menu, applicationContext, configuration.showMenuItemNames, configuration) {
                runOnUiThread { menuAdapter.notifyDataSetChanged() }
            }
        runOnUiThread { binding.menuGridView.adapter = menuAdapter }
    }

    private fun makeToast(text: String) {
        myToast?.cancel()
        myToast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        myToast?.show()
    }

    private val APPS_SCRIPT_URL =
        "https://script.google.com/macros/s/AKfycbyb4ey0BF43Vuk4g4r4SGs-2NP4HEvNF0kn-pPEhsYtODwXKyp4G7P-1_Zhlmd1LrEB/exec" // <--- ****** PASTE YOUR URL HERE ******


    override fun onResume() {
        super.onResume()

        // First, load the saved (local) menu.
        loadMenu()

        // Then check if there's a preferred menu and try to update it.
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val selectedMenuName = pref.getString("selected_menu_name", null)
        if (selectedMenuName != null) {
            try {
                val encodedMenuName = URLEncoder.encode(selectedMenuName, "UTF-8")
                val url = "${BuildConfig.MENU_SCRIPT_URL}?menu=$encodedMenuName"
                Log.d("MainActivity", "Requesting update for menu: $selectedMenuName, URL: $url")

                val requestQueue = Volley.newRequestQueue(this)
                val stringRequest = object : StringRequest(
                    Request.Method.GET,
                    url,
                    { response ->
                        try {
                            val jsonResponse = JSONObject(response)
                            val status = jsonResponse.optString("status", "error")
                            val csvData = jsonResponse.optString("data", "")
                            
                            if (status == "success" && csvData.isNotEmpty()) {
                                // Save the updated CSV file using the new FileIO system.
                                val fileName = "$selectedMenuName.csv"
                                val saved = edu.amrita.amritacafe.IO.CSVFileManager.saveCSV(applicationContext, fileName, csvData)
                                if (saved) {
                                    Log.d("MainActivity", "Updated CSV saved successfully as $fileName")
                                    // Refresh the UI with the updated menu.
                                    loadMenu()
                                } else {
                                    Log.e("MainActivity", "Failed to save updated CSV as $fileName")
                                }
                            } else {
                                val msg = jsonResponse.optString("message", "Empty data received")
                                Log.e("MainActivity", "Update skipped: $msg")
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    },
                    { error ->
                        Log.e("MainActivity", "Error fetching updated menu: ${error.message}")
                        // Optionally, show a toast or log that the update failed.
                    }
                ) {
                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }
                }
                stringRequest.retryPolicy = DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
                requestQueue.add(stringRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.d("MainActivity", "No preferred menu name found in preferences.")
        }

        binding.tabletNameMainTV.text = configuration.tabletName
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             UpdateChecker.checkForUpdates(this)
        } else if (checkStoragePermission(this)) {
             UpdateChecker.checkForUpdates(this)
        }
    }

    private fun checkStoragePermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true
        
        val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                2 // REQUEST_EXTERNAL_STORAGE
            )
            return false
        }
        return true
    }



    private var received = 0f
    private var currentTotalCost: Float = 0f
    private var renunciate = false
    private var isGpay = false

    private fun openPaymentDialog(orders: List<Order>) {
        renunciate = false
        isGpay = false

        // Use the generated binding class for dialog_payment.xml
        val binding = DialogPaymentBinding.inflate(LayoutInflater.from(this))

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(binding.root) // Set the root view from the binding
            .setCancelable(true)
        val dialog = dialogBuilder.show()

        val checkDraw = ContextCompat.getDrawable(this, R.drawable.check_image)
        val renunciateDraw = ContextCompat.getDrawable(this, R.drawable.renunciate)
        val rupeeDraw = ContextCompat.getDrawable(this, R.drawable.rupee_image)

        binding.gpayButton.setOnClickListener {
            isGpay = !isGpay
            if (isGpay) {
                if (renunciate) {
                    binding.renunciateBotton.performClick()
                }
            }

            binding.gpayButton.setCompoundDrawablesWithIntrinsicBounds(
                null,
                rupeeDraw,
                if (isGpay) checkDraw else null,
                null
            )
        }

        binding.renunciateBotton.setOnClickListener {
            renunciate = !renunciate // Toggle the value of renunciate
            if (renunciate) {
                if (isGpay) {
                    isGpay = false
                    binding.gpayButton.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        rupeeDraw,
                        null,
                        null
                    )
                }
            }
            orderAdapter.orderItems.forEach {
                it.quantityAsRenounciate = it.quantity
            }

            // Print the current value of renounciate
            println("renunciate: $renunciate")

            // Set the appropriate drawable based on the value of renounciate
            binding.renunciateBotton.setCompoundDrawablesWithIntrinsicBounds(
                null, // left drawable
                renunciateDraw, // top drawable
                if (renunciate) checkDraw else null, // right drawable
                null // bottom drawable
            )

            var totalRenunciateItems = 0
            var hasMilkCurdEgg = false

            fun discountOne(string: String) {
                orderAdapter.orderItems.forEach {
                    if (it.menuItem.name == string && !hasMilkCurdEgg) {
                        hasMilkCurdEgg = true
                        it.quantityAsRenounciate = it.quantity - 1
                        it.renounciateEffected = true
                    }
                }
            }

            arrayOf("Curd", "Milk", "Egg").forEach {
                discountOne(it)
            }

            orderAdapter.orderItems.forEach {
                if (renunciate) {
                    if (it.menuItem.name.equals("Dressing") || it.menuItem.name.equals("Beschameal"))
                        it.quantityAsRenounciate = it.quantity - 1
                    else if (it.menuItem.name.equals("Bread")) {
                        it.quantityAsRenounciate = max(it.quantity - 2, 0)
                    } else if (it.menuItem.category.equals("LUNCH/DINNER (R)") && totalRenunciateItems < 3) {
                        totalRenunciateItems += 1
                        it.quantityAsRenounciate = it.quantity - 1
                    }
                    // 5 iddly or dosa are free for renunciates
                    else if (it.menuItem.name.equals("Iddly") || it.menuItem.name.equals("Dosa")) {
                        it.quantityAsRenounciate = max(it.quantity - 5, 0)
                    } else if (it.menuItem.name.equals("Sambar Only/Ex")) {
                        it.quantityAsRenounciate = 0
                    } else if (it.menuItem.name.equals("Upma")) {
                        it.quantityAsRenounciate = max(it.quantity - 2, 0)
                    } else if (it.menuItem.name.equals("Sprouts")) {
                        it.quantityAsRenounciate = max(it.quantity - 1, 0)
                    }
                    if (it.quantityAsRenounciate != it.quantity)
                        it.renounciateEffected = true
                } else {
                    it.quantityAsRenounciate = it.quantity
                    it.renounciateEffected = false
                }
                println("it.quantityAsRenounciate: ${it.quantityAsRenounciate}")
                println("it.renounciateEffected: ${it.renounciateEffected}")
                println("it.menuItem.name: ${it.menuItem.name}")
            }

            println("PRICE")
            // TODO STORE the renunciate indication on sheets
            currentTotalCost = orderAdapter.orderItems.map {
                it.totalPrice().toFloat()
            }.sum()

            binding.toPayTV.text = currentTotalCost.toString()
            binding.receivedTV.text = received.toString()
            binding.toReturnTV.text = (received - currentTotalCost).toString()
        }

        fun done() {
            orders.forEach { it.isGpay = isGpay }
            sendToSheets(orders, configuration, this)
            startNewOrder()
            orderDone(orders)
            dialog.dismiss()
        }

        binding.printBottom.setOnClickListener {
            bluetoothPrint(mHoinPrinter, orders)
            done()
        }

        binding.noPrintBotton.setOnClickListener {
            done()
        }

        received = 0f
        currentTotalCost = orderAdapter.orderItems.map { it.totalPrice().toFloat() }.sum()

        binding.toPayTV.text = currentTotalCost.toString()

        val onClickRecivedListener = View.OnClickListener { billButton ->
            billButton as Button
            when (billButton.text) {
                "500₹" -> received += 500
                "200₹" -> received += 200
                "100₹" -> received += 100
                "50₹" -> received += 50
                "20₹" -> received += 20
                "10₹" -> received += 10
                "5₹" -> received += 5f
                "1₹" -> received += 1f
                "Clear" -> received
            }
            binding.receivedTV.text = received.toString()
            binding.toReturnTV.text = (received - currentTotalCost).toString()
        }
        binding.received500Button.setOnClickListener(onClickRecivedListener)
        binding.received200Button.setOnClickListener(onClickRecivedListener)
        binding.received100Button.setOnClickListener(onClickRecivedListener)
        binding.received50Button.setOnClickListener(onClickRecivedListener)
        binding.received20Button.setOnClickListener(onClickRecivedListener)
        binding.received10Button.setOnClickListener(onClickRecivedListener)
        binding.received5Button.setOnClickListener(onClickRecivedListener)
        binding.recieved1Botton.setOnClickListener(onClickRecivedListener)
        binding.clearMoneyRecieved.setOnClickListener(onClickRecivedListener)
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
                if (!selection.isEmpty())
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
        // Retrieve the saved preferred menu name
        val selectedMenuName = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("selected_menu_name", null)
        if (selectedMenuName != null) {
            // Build the file name (e.g., "BreakfastMenu.csv")
            val fileName = "$selectedMenuName.csv"
            // Create a File object from internal storage using getFileStreamPath
            val file = getFileStreamPath(fileName)
            if (file.exists()) {
                // Parse the CSV file using your existing parser
                val list = getListOfMenu(file)
                // Build the list of unique categories
                allCurrentCategories = mutableListOf()
                list.forEach {
                    if (!allCurrentCategories.contains(it.category))
                        allCurrentCategories.add(it.category)
                }
                orderNumberService.updateRange()
                binding.orderNumberET.setText(orderNumberService.currentOrderNumber.toString())
                setMenuAdapter(list)
                Log.d("MainActivity", "Loaded menu from file: $fileName")
            } else {
                Toast.makeText(this, "Menu file not found: $fileName", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "File $fileName does not exist")
            }
        } else {
            Toast.makeText(this, "No preferred menu selected", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "Preferred menu name not set in preferences")
        }
    }


//    fun loadMenu() {
//        val file = if (configuration.isBreakfastTime) File(
//            baseContext.filesDir,
//            "Breakfast.txt"
//        ) else File(baseContext.filesDir, "LunchDinner.txt")
//        val list = getListOfMenu(file)
//
//        allCurrentCategories = mutableListOf()
//
//        list.forEach {
//            if (!allCurrentCategories.contains(it.category))
//                allCurrentCategories.add(it.category)
//        }
//
//        orderNumberService.updateRange()
//        binding.orderNumberET.setText(orderNumberService.currentOrderNumber.toString())
//
//        setMenuAdapter(list)
//    }

    private fun startNewOrder() {
        GlobalScope.launch {
//            orderNumberService.next()

            runOnUiThread {
                orderAdapter.clear()
                binding.orderNumberET.setText(orderNumberService.currentOrderNumber.toString())
            }
        }
    }

    private lateinit var currentDialog: AlertDialog

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this, R.style.DialogStyle)
        builder.setTitle("Close Amrita Cafe?")
        builder.setCancelable(true)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            super.onBackPressed()
            mHoinPrinter.destroy()
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which ->
        }

        builder.show()
    }

    fun orderButtonPressed(view: View) {
        printOrder()
    }


    private fun printOrder(printOrder: Boolean = true) {
        val orderItemsCopy = orderAdapter.orderItems.toMutableList()

        if (printOrder && BT_STATE != BT_STATE_CONNECTED)
            tryConnect()

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

        val orders = if (hasPizza && hasGrill) {
            listOf(
                Order(
                    orderNumberService.currentOrderNumber,
                    orderList.filter { it.menuItem.category != PIZZA }),
                Order(
                    runBlocking { orderNumberService.next() },
                    orderList.filter { it.menuItem.category == PIZZA })
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
            // Use the binding class generated for dialog_print.xml
            val dialogBinding = DialogPrintBinding.inflate(LayoutInflater.from(this))

            val dialog = AlertDialog.Builder(this)
                .setView(dialogBinding.root) // Set the root view from the binding
                .setCancelable(false)
                .show()
                .apply {
                    setCanceledOnTouchOutside(false)
                }

            currentDialog = dialog

            val listener = object : PrintService.PrintServiceListener {
                override fun kitchenPrinterFinished() = runOnUiThread {
                    histories.forEach {
                        it.KitchenPrinted = PrintStatus.SUCCESS_PRINT
                    }
                    dialogBinding.run {
                        include2.kitchenProgress.visibility = View.INVISIBLE
                        include2.kitchenError.visibility = View.INVISIBLE
                        include2.kitchenDone.visibility = View.VISIBLE
                        include2.kitchenRetryButton.visibility = View.INVISIBLE
                    }
                }

                override fun kitchenPrinterError(response: PrintFailed) = runOnUiThread {
                    histories.forEach {
                        it.KitchenPrinted = PrintStatus.FAILED_PRINT
                    }
                    dialogBinding.run {
                        include2.kitchenProgress.visibility = View.INVISIBLE
                        include2.kitchenError.visibility = View.VISIBLE
                        include2.kitchenRetryButton.visibility = View.VISIBLE
                    }
                }

                override fun kitchenPrinterError(
                    errorStatus: ErrorStatus,
                    exception: Epos2Exception
                ) = runOnUiThread {
                    histories.forEach {
                        it.KitchenPrinted = PrintStatus.FAILED_PRINT
                    }
                    dialogBinding.run {
                        include2.kitchenProgress.visibility = View.INVISIBLE
                        include2.kitchenError.visibility = View.VISIBLE
                        include2.kitchenRetryButton.visibility = View.VISIBLE
                    }
                }

                override fun receiptPrinterFinished() = runOnUiThread {
                    histories.forEach {
                        it.RecipePrinted = PrintStatus.SUCCESS_PRINT
                    }
                    dialogBinding.run {
                        include2.receiptProgress.visibility = View.INVISIBLE
                        include2.receiptError.visibility = View.INVISIBLE
                        include2.receiptDone.visibility = View.VISIBLE
                        include2.receiptRetryButton.visibility = View.INVISIBLE
                    }
                }

                override fun receiptPrinterError(response: PrintFailed) = runOnUiThread {
                    histories.forEach {
                        it.RecipePrinted = PrintStatus.FAILED_PRINT
                    }
                    dialogBinding.run {
                        include2.receiptProgress.visibility = View.INVISIBLE
                        include2.receiptError.visibility = View.VISIBLE
                        include2.receiptRetryButton.visibility = View.VISIBLE
                    }
                }

                override fun receiptPrinterError(
                    errorStatus: ErrorStatus,
                    exception: Epos2Exception
                ) = runOnUiThread {
                    histories.forEach {
                        it.RecipePrinted = PrintStatus.FAILED_PRINT
                    }
                    dialogBinding.run {
                        include2.receiptProgress.visibility = View.INVISIBLE
                        include2.receiptError.visibility = View.VISIBLE
                        include2.receiptRetryButton.visibility = View.VISIBLE
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

            dialogBinding.include2.kitchenRetryButton.setOnClickListener {
                histories.forEach {
                    it.KitchenPrinted = PrintStatus.PRINTING
                }
                printService.retry()
                it.visibility = View.INVISIBLE
                dialogBinding.include2.kitchenError.visibility = View.INVISIBLE
                dialogBinding.include2.kitchenProgress.visibility = View.VISIBLE
            }

            dialogBinding.include2.receiptRetryButton.setOnClickListener {
                histories.forEach {
                    it.RecipePrinted = PrintStatus.PRINTING
                }
                printService.retry()
                it.visibility = View.INVISIBLE
                dialogBinding.include2.receiptError.visibility = View.INVISIBLE
                dialogBinding.include2.receiptProgress.visibility = View.VISIBLE
            }

            orderDone(orders)
        } else if (configuration.mode == BLUETOOTH) {
            openPaymentDialog(orders)
        }
    }


    private fun orderDone(orders: List<Order>) {
        if (configuration.printToFile)
            writeToCSV(orders, configuration)

        GlobalScope.launch {
            orderNumberService.next()
        }
    }


    @SuppressLint("InflateParams")
    private fun printerDialog() =
        LayoutInflater.from(this).inflate(R.layout.dialog_print, null).let { view ->
            AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
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
        binding.shortLongToggleButton.text =
            if (configuration.showMenuItemNames) "Short Names" else "Long Names"
    }

    fun openSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun openHistoryDialog(historyButton: View) {
        // Inflate the dialog layout with view binding
        val binding = DialogHistoryBinding.inflate(LayoutInflater.from(this))

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .setCancelable(true)
            .show()

        // Set up the RecyclerView with the appropriate layout manager and adapter
        val layoutManager = LinearLayoutManager(this)
        binding.historyRV.layoutManager = layoutManager

        val historyAdapter = HistoryAdapter(orderHistory, configuration, this)
        binding.historyRV.adapter = historyAdapter

        // Scroll to the last position
        binding.historyRV.scrollToPosition(orderHistory.size - 1)
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

    fun cafeOrder(view: View) {
        showCostInputDialog(this)
    }


    private fun showCostInputDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val cafeOrderCostTV = TextView(context)
        cafeOrderCostTV.text = "Cafe Order Cost:"
        cafeOrderCostTV.setPadding(20, 10, 5, 5)
        cafeOrderCostTV.textSize = 30f
        cafeOrderCostTV.setTextColor(Color.BLACK)
        layout.addView(cafeOrderCostTV)

        val cafeOrderCostET = EditText(context)
        cafeOrderCostET.setPadding(20, 10, 5, 5)
        cafeOrderCostET.textSize = 30f
        cafeOrderCostET.inputType = InputType.TYPE_CLASS_NUMBER
        cafeOrderCostET.requestFocus()
        layout.addView(cafeOrderCostET)

        builder.setView(layout)
        builder.setPositiveButton("OK", null)
        builder.setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(Color.DKGRAY)
            negativeButton.setTextColor(Color.RED)

            positiveButton.setOnClickListener {
                try {
                    orderAdapter.add(
                        MenuItem(
                            "Cafe Order ", //+ cafeOrderNumberET.text,
                            "Cafe Order ", //+ cafeOrderNumberET.text,
                            cafeOrderCostET.text.toString().toFloat(),
                            "Cafe Order"
                        ),
                        uniqueItem = true
                    )
                    dialog.dismiss()
                } catch (e: Exception) {
                    makeToast("Not working")
                }
            }
        }

        dialog.show()

        cafeOrderCostET.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.performClick()
                true
            } else {
                false
            }
        }
    }
}