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
import android.provider.Settings
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
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.epson.epos2.Epos2Exception
import com.example.hoinprinterlib.HoinPrinter
import com.example.hoinprinterlib.module.PrinterCallback
import com.example.hoinprinterlib.module.PrinterEvent
import edu.amrita.amritacafe.IO.createDefaultFilesIfNecessary
import edu.amrita.amritacafe.IO.getListOfMenu
import edu.amrita.amritacafe.IO.saveIfValidText
import edu.amrita.amritacafe.IO.writeToCSV
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.*
import edu.amrita.amritacafe.model.*
import edu.amrita.amritacafe.printer.ErrorStatus
import edu.amrita.amritacafe.printer.OrderNumberService
import edu.amrita.amritacafe.printer.PrintFailed
import edu.amrita.amritacafe.printer.PrintService
import edu.amrita.amritacafe.printer.bluetooth.bluetoothPrint
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_history.view.*
import kotlinx.android.synthetic.main.dialog_payment.view.clear_money_recieved
import kotlinx.android.synthetic.main.dialog_payment.view.recieved_1_botton
import kotlinx.android.synthetic.main.dialog_payment.view.no_print_botton
import kotlinx.android.synthetic.main.dialog_payment.view.print_bottom
import kotlinx.android.synthetic.main.dialog_payment.view.received_500_button
import kotlinx.android.synthetic.main.dialog_payment.view.received_50_button
import kotlinx.android.synthetic.main.dialog_payment.view.received_10_button
import kotlinx.android.synthetic.main.dialog_payment.view.received_100_button
import kotlinx.android.synthetic.main.dialog_payment.view.received_5_button
import kotlinx.android.synthetic.main.dialog_payment.view.received_200_button
import kotlinx.android.synthetic.main.dialog_payment.view.received_20_button
import kotlinx.android.synthetic.main.dialog_payment.view.received_TV
import kotlinx.android.synthetic.main.dialog_payment.view.renunciate_botton
import kotlinx.android.synthetic.main.dialog_payment.view.to_pay_TV
import kotlinx.android.synthetic.main.dialog_payment.view.to_return_TV
import kotlinx.android.synthetic.main.include_print.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.math.max


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

        val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(
            this,
            PERMISSIONS_STORAGE,
            PackageManager.PERMISSION_GRANTED
        );

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
                orderAdapter.orderItems.map { it.priceWithoutExtras }.sum().toString()
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

        createDefaultFilesIfNecessary(baseContext)
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

        print("PERMISSION " + checkStoragePermission(this))
    }

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun checkStoragePermission(activity: Activity): Boolean {
        val permission =
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                print("WE HAVE PERMISSION")
                // Permission granted, you can now proceed with writing to external storage
            } else {
                print("WE DOOOONT HAVE PERMISSION")
                // Permission denied, handle accordingly
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

        val menu = if (configuration.isBreakfastTime) "Cafe Drinks" else "Canteen Menu"
        val file = if (configuration.isBreakfastTime) BREAKFAST_FILE else LUNCH_DINNER_FILE
        val url =
            "https://script.google.com/macros/s/AKfycbzYTDthdR5kebKwuqz7M2IOB_" +
                    "TqauCRcxTs8vtb7rt8giHhhVTNqgYe5aSpzMQX6-fTOQ/exec?menu=" + menu

        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(
            Method.GET,
            url, // Replace with your actual URL
            { response ->
                // Handle successful response
                Log.d("Get Menu", "Response: $response")

                val response2 = saveIfValidText(response, applicationContext, file)
                if (response2.startsWith("Successfully")) {
                    loadMenu()
                }
                Log.d("Get Menu", "Response2: $response2")
            },
            { error ->
                // Handle error
                Log.e("Get Menu", "Error: ${error.message}")
                makeToast("Sending data error: ${error.message}")
            }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                return try {
                    "Helloooo".toByteArray(Charsets.UTF_8)
                } catch (e: UnsupportedEncodingException) {
                    Log.e("TAG", "Error encoding JSON: $e")
                    return ByteArray(0)
                }
            }
        }
        stringRequest.setRetryPolicy(
            DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )

        Log.d("Get Menu", "Response2: $stringRequest")

        // Add the request to the queue
        requestQueue.add(stringRequest)

        tabletNameMainTV.text = configuration.tabletName
    }


    var received = 0f
    var currentTotalCost = 0f
    var renounciate = false

    private fun openPaymentDialog(orders: List<Order>) {
        renounciate = false

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.show()
        val check_draw = ContextCompat.getDrawable(this, R.drawable.check_image)
        val renunciate_draw = ContextCompat.getDrawable(this, R.drawable.renunciate)

        dialogView.renunciate_botton.setOnClickListener {
            renounciate = !renounciate // Toggle the value of renounciate

            // Print the current value of renounciate
            println("renounciate: $renounciate")

            // Set the appropriate drawable based on the value of renounciate
            dialogView.renunciate_botton.setCompoundDrawablesWithIntrinsicBounds(
                null, // left drawable
                renunciate_draw, // top drawable
                if (renounciate) check_draw else null, // right drawable
                null // bottom drawable
            )

            var totalRenunciateItems = 0
            var hasMilkCurdEgg = false

            // TODO STORE the renunciate indication on sheets
            currentTotalCost = orderAdapter.orderItems.map {
                if (renounciate) {
                    if (it.menuItem.name.equals("Dressing") || it.menuItem.name.equals("Beschameal"))
                        ((it.quantity - 1) * it.menuItem.price)
                    else if (it.menuItem.category.equals("LUNCH/DINNER (R)") && totalRenunciateItems < 3) {
                        totalRenunciateItems += 1
                        (it.quantity - 1) * it.menuItem.price
                    }
                    // 5 iddly or dosa are free for renunciates
                    else if ((it.menuItem.name.equals("Iddly") || it.menuItem.name.equals("Dosa")) && totalRenunciateItems < 3) {
                        (max(it.quantity - 5, 0)) * it.menuItem.price
                    } else if (it.menuItem.name.equals("Sambar Only/Ex")) {
                        0f
                    } else if (it.menuItem.name.equals("Upma")) {
                        (max(it.quantity - 2, 0)) * it.menuItem.price
                    } else if (it.menuItem.name.equals("Sprouts")) {
                        (max(it.quantity - 1, 0)) * it.menuItem.price
                    } else if ((it.menuItem.name.equals("Milk") || it.menuItem.name.equals("Curd") ||
                                it.menuItem.name.equals("Egg")) && !hasMilkCurdEgg
                    ) {
                        hasMilkCurdEgg = true
                        (it.quantity - 1) * it.menuItem.price
                    } else {
                        it.totalPrice.toFloat()
                    }
                } else {
                    it.totalPrice.toFloat()
                }
            }.sum()


            dialogView.to_pay_TV.text = currentTotalCost.toString()
            dialogView.received_TV.text = received.toString()
            dialogView.to_return_TV.text = (received - currentTotalCost).toString()
        }

        dialogView.print_bottom.setOnClickListener {
            sendToSheetAndPrint(orders, true)
            orderDone(orders)
            dialog.dismiss()
        }

        dialogView.no_print_botton.setOnClickListener {
            sendToSheetAndPrint(orders, false)
            orderDone(orders)
            dialog.dismiss()
        }

        received = 0f
        currentTotalCost = orderAdapter.orderItems.map { it.totalPrice.toFloat() }.sum()

        dialogView.to_pay_TV.text = currentTotalCost.toString()

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
                "Clear" -> received = 0f
            }
            dialogView.received_TV.text = received.toString()
            dialogView.to_return_TV.text = (received - currentTotalCost).toString()
        }
        dialogView.received_500_button.setOnClickListener(onClickRecivedListener)
        dialogView.received_200_button.setOnClickListener(onClickRecivedListener)
        dialogView.received_100_button.setOnClickListener(onClickRecivedListener)
        dialogView.received_50_button.setOnClickListener(onClickRecivedListener)
        dialogView.received_20_button.setOnClickListener(onClickRecivedListener)
        dialogView.received_10_button.setOnClickListener(onClickRecivedListener)
        dialogView.received_5_button.setOnClickListener(onClickRecivedListener)
        dialogView.recieved_1_botton.setOnClickListener(onClickRecivedListener)
        dialogView.clear_money_recieved.setOnClickListener(onClickRecivedListener)
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

        if (printOrder)
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

            orderDone(orders)
        } else if (configuration.mode == BLUETOOTH) {
            openPaymentDialog(orders)
        }

    }

    private fun orderDone(orders: List<Order>) {
        if(configuration.printToFile)
            writeToCSV(orders, configuration)

        GlobalScope.launch {
            orderNumberService.next()
        }
    }

    private fun sendToSheetAndPrint(
        orders: List<Order>,
        printOrder: Boolean
    ) {
        val jsonData = JSONObject()
        val jsonArray = JSONArray()
        var orderTime = 0L
        var myOrderNumber = 0

        orders.forEach { (orderNumber, orderItems, time) ->
            val orderTotalText = orderItems.map { it.totalPrice }.sum().toString()
            val orderNumStr = orderNumber.toString().padStart(3, '0')
            orderTime = time
            myOrderNumber = orderNumber

            orderItems.forEach {
                val jsonItem = JSONObject()
                jsonItem.put("name", it.menuItem.name)
                jsonItem.put("quantity", it.quantity)
                jsonItem.put("total", it.totalPrice)
                jsonItem.put("cost", it.menuItem.price)
                jsonArray.put(jsonItem)
            }
            if (printOrder) {
                bluetoothPrint(mHoinPrinter, orderNumStr, orderItems, orderTotalText)
            }
        }

        val url =
            "https://script.google.com/macros/s/AKfycbzYTDthdR5kebKwuqz7M2IOB_TqauCRcxTs8vtb7rt8giHhhVTNqgYe5aSpzMQX6-fTOQ/exec" // Replace with your actual URL

        jsonData.put("items", jsonArray)
        try {
            jsonData.put("time", orderTime)
            jsonData.put("tablet", configuration.tabletName)
            jsonData.put("order", myOrderNumber.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val jsonString = jsonData.toString()
        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(
            Method.POST,
            url, // Replace with your actual URL
            { response ->
                // Handle successful response
                Log.d("Connection", "Response: $response")
            },
            { error ->
                // Handle error
                Log.e("Connection", "Error: ${error.message}")
                makeToast("Sending data error: ${error.message}")
            }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                return try {
                    jsonString.toByteArray(Charsets.UTF_8)
                } catch (e: UnsupportedEncodingException) {
                    Log.e("TAG", "Error encoding JSON: $e")
                    return ByteArray(0)
                }
            }
        }
        stringRequest.setRetryPolicy(
            DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )

        // Add the request to the queue
        requestQueue.add(stringRequest)

        startNewOrder()
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

    fun cafeOrder(view: View) {
        showCostInputDialog(this)
    }


    private fun showCostInputDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        val cafeOrderNumberTV = TextView(context)
        cafeOrderNumberTV.text = "Cafe Order Number:"
        cafeOrderNumberTV.setPadding(20, 10, 5, 5)
        cafeOrderNumberTV.textSize = 30f
        cafeOrderNumberTV.setTextColor(Color.BLACK)
        layout.addView(cafeOrderNumberTV)

        val cafeOrderNumberET = EditText(context)
        cafeOrderNumberET.setPadding(20, 10, 5, 5)
        cafeOrderNumberET.textSize = 30f
        cafeOrderNumberET.inputType = InputType.TYPE_CLASS_NUMBER
        cafeOrderNumberET.requestFocus()
        layout.addView(cafeOrderNumberET)

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
                            "Cafe Order " + cafeOrderNumberET.text,
                            "Cafe Order " + cafeOrderNumberET.text,
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