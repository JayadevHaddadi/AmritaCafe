package edu.amrita.amritacafe.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.epson.epos2.Epos2Exception
import com.google.firebase.firestore.FirebaseFirestore
import edu.amrita.amritacafe.BuildConfig
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.email.Mailer
import edu.amrita.amritacafe.email.User
import edu.amrita.amritacafe.email.adminEmail
import edu.amrita.amritacafe.email.allUsers
import edu.amrita.amritacafe.menu.*
import edu.amrita.amritacafe.model.MenuAdapter
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.model.OrderAdapter
import edu.amrita.amritacafe.printer.ErrorStatus
import edu.amrita.amritacafe.printer.OrderNumberService
import edu.amrita.amritacafe.printer.PrintFailed
import edu.amrita.amritacafe.printer.PrintService
import edu.amrita.amritacafe.settings.Configuration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_finish.view.*
import kotlinx.android.synthetic.main.dialog_finish.view.receipt_progress
import kotlinx.android.synthetic.main.dialog_finish.view.receipt_retry_button
import kotlinx.android.synthetic.main.dialog_finish.view.receipt_text_TV
import kotlinx.android.synthetic.main.dialog_history.view.*
import kotlinx.android.synthetic.main.dialog_login.view.*
import kotlinx.android.synthetic.main.dialog_payment.view.*
import kotlinx.android.synthetic.main.include_print.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


fun String.capitalizeWords(): String =
    split(" ").map { it.toLowerCase().capitalize() }.joinToString(" ")

class MainActivity : AppCompatActivity() {
    private lateinit var sheetsMenu: ArrayList<MenuItemUS>
    private var myToast: Toast? = null
    private lateinit var tabletName: String
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUser: User
    private var modeAmritapuri: Boolean = false
    private lateinit var currentHistoryFile: File
    private var dialogOpen = false

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
    var received = 0f
    var currentTotalCost = 0f

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

//        Security.insertProviderAt(Conscrypt.newProvider(), 1)
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

        //todo bring back for other release
        db = FirebaseFirestore.getInstance()

        orderAdapter = OrderAdapter(this)
        orderAdapter.orderChanged = {
            currentOrderSum = orderAdapter.orderItems.map { it.finalPrice }.sum()
            total_cost_TV.text = currentOrderSum.toString()
        }

        order_ListView.adapter = orderAdapter

        order_button.setOnClickListener {
            val order = Order(currentOrderNumber, orderAdapter.orderItems.toList())
            val orders = listOf(order)

            if (modeAmritapuri) {
                printOrder(orders) // just for testing history
//                startNewOrder() // same TODO remove both

//                finishOrder()
            } else {
                openPaymentDialog()
            }
        }

        val latestMenu = loadLastMenu(pref)
        println("latest menu: $latestMenu")
//        setMenuAdapter(latestMenu)
        readSheets(pref) { menuList, users ->
            println("CALLBACK BABY")
            sheetsMenu = menuList
//            setMenuAdapter(menuList)

            allUsers = users
            if (!allUsers.isEmpty())
                adminEmail = allUsers.last().email
            runOnUiThread {
                dialogRoot?.let { setupSpinner(it) }
            }
        }

        menuGridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItemUS -> {
                    orderAdapter.add(menuItem)
                }
            }
        }

        val latestUsers = loadLastUsers(pref)
        allUsers = latestUsers
        if (!allUsers.isEmpty())
            adminEmail = allUsers.last().email

        println("STARTED?")

        updateNameForToggleButton()

        if (!modeAmritapuri)
            openLoginDialog()
        else {
            setAmritapuriMode()
        }
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

    private fun setupSpinner(root: View) {
        val namesOnly = allUsers.map {
            "${it.name} - ${it.email}"
        }
        val userNameAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, namesOnly)
        userNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        root.user_spinner.adapter = userNameAdapter
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

    private fun openPaymentDialog() {
        val (dialog, root) =
            LayoutInflater.from(this).inflate(R.layout.dialog_payment, null).let { view ->
                AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(true)
                    .show()
                    .to(view)
            }

        fun allItemsToDatabase() {
            orderAdapter.orderItems.forEach {
                val item = hashMapOf(
                    "time" to Date(),
                    "cashier" to currentUser.name,
                    "order_number" to currentOrderNumber,
                    "item_name" to it.menuItem.name,
                    "quantity" to it.quantity,
                    "discount_factor" to it.priceMultiplier,
                    "original_price" to it.originalPrice,
                    "final_price" to it.finalPrice
                )

                // Add a new document with a generated ID
                db.collection("sold_item")
                    .add(item)
                    .addOnSuccessListener { documentReference ->
                        makeToast("Success: ${it.menuItem.name} added to database")
                        Log.d("hhallo", "DocumentSnapshot added with ID: ${documentReference.id}")
                    }
                    .addOnFailureListener { e ->
                        makeToast(
                            "Failure: ${it.menuItem.name} not added to database\nWill upload once " +
                                    "internet is reestablished"
                        )
                        Log.w("hhallo", "Error adding document", e)
                    }
            }
        }

        root.cash_received_button.setOnClickListener {
            allItemsToDatabase()
            finishOrder(true)
            dialog.dismiss()
        }
        root.credit_received_button.setOnClickListener {
            allItemsToDatabase()
            finishOrder(false)
            dialog.dismiss()
        }

        received = 0f
        currentTotalCost = orderAdapter.orderItems.map { it.finalPrice }.sum()
        root.to_pay_TV.text = currentTotalCost.toString()
        val onClickRecivedListener = View.OnClickListener { billButton ->
            billButton as Button
            when (billButton.text) {
                "100$" -> received += 100
                "50$" -> received += 50
                "20$" -> received += 20
                "10$" -> received += 10
                "5$" -> received += 5
                "1$" -> received += 1
                ".25$" -> received += .25f
                "CLEAR" -> received = 0f
            }
            root.received_TV.text = received.toString()
            root.to_return_TV.text = (received - currentTotalCost).toString()
        }
        root.received_100_button.setOnClickListener(onClickRecivedListener)
        root.received_50_button.setOnClickListener(onClickRecivedListener)
        root.received_20_button.setOnClickListener(onClickRecivedListener)
        root.received_10_button.setOnClickListener(onClickRecivedListener)
        root.received_5_button.setOnClickListener(onClickRecivedListener)
        root.received_1_button.setOnClickListener(onClickRecivedListener)
        root.received_25cent_button.setOnClickListener(onClickRecivedListener)
        root.clear_received_button.setOnClickListener(onClickRecivedListener)
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

    var dialogRoot: View? = null
    private fun openLoginDialog() {
        val (dialog, root) =
            LayoutInflater.from(this).inflate(R.layout.dialog_login, null).let { view ->
                AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .show()
                    .to(view)
            }
        dialogRoot = root

        setupSpinner(root)

        root.amritapuri_button.setOnClickListener {
            dialog.dismiss()
        }
        root.login_button.setOnClickListener {
            currentUser = allUsers[root.user_spinner.selectedItemPosition]
            if (root.password_ET.text.toString() == currentUser.password) {
                modeAmritapuri = false
                Toast.makeText(
                    this,
                    "Succesfully logged as ${currentUser.name}\n${currentUser.email}",
                    Toast.LENGTH_SHORT
                ).show()
                createHistoryFile(currentUser.name)
                order_button.text = "Pay"
                settings_button.visibility = View.GONE
                dialog.dismiss()
                user_TV.text = "$currentUser.name @ $tabletName"
                setMenuAdapter(sheetsMenu)
            } else {
                Toast.makeText(
                    this,
                    "Failed login, you have infinite attempts left",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialogRoot = null
        }
    }

    private fun setAmritapuriMode() {
        modeAmritapuri = true
        order_button.text = "Order"
        dialogRoot = null
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
                    "Email: ${currentUser.email}\n" +
                    "Tablet: $tabletName\n"
//                    + "App Version: ${BuildConfig.VERSION_NAME}\n\n"
        )
    }

    override fun onBackPressed() {
        println("dialog open: $dialogOpen")
        if (!dialogOpen) {
            if (!modeAmritapuri) {
                openExitDialog()
            } else
                finish()
        }
    }

    var myFinalComment = ""

    @SuppressLint("CheckResult")
    private fun openExitDialog() {
        if (!isOnline()) {
            makeToast("No internet connection!!! Git some!!!")
            return
        }

        val (dialog, root) =
            LayoutInflater.from(this).inflate(R.layout.dialog_finish, null).let { view ->
                AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .show()
                    .apply {
                        setCanceledOnTouchOutside(false)
                    }.to(view)
            }
        root.final_comments_ET.setText(myFinalComment)
        root.final_comments_ET.doOnTextChanged { text, start, count, after ->
            myFinalComment = text.toString()
        }

        root.receipt_text_TV.text = "Session record will be sent to ${currentUser.email}"
        root.email_TV2.text = "Session record will be sent to ${adminEmail}"
        root.kitchen_retry_button.setOnClickListener {
            root.final_comments_ET.visibility = View.GONE
            root.receipt_progress.visibility = View.VISIBLE
            root.email_progress2.visibility = View.VISIBLE
            root.kitchen_retry_button.visibility = View.GONE
            if (isOnline()) {
                root.receipt_text_TV.text = "Sending email to ${currentUser.email}..."
                root.email_TV2.text = "Sending email to ${adminEmail}..."
                val emailBody =
                    "${currentHistoryFile.readText()}Final Comments:\n${root.final_comments_ET.text}"
                val subject =
                    "${currentUser.name} - Cash: $sessionCash, Credit: $sessionCredit"
                println("Sending first mail")
                Mailer.sendMail(currentUser.email, subject, emailBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            fun close() {
                                dialog.dismiss()
                                println("finishing")
                                finish()
                            }
                            if (!dialog.isShowing) {
                                Toast.makeText(
                                    this,
                                    "Mail successfully sent! Thank you!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                close()
                            } else {
                                root.receipt_progress.visibility = View.GONE
                                root.receipt_retry_button.visibility = View.GONE
                                root.kitchen_retry_button.visibility = View.VISIBLE
                                root.email_success_IV.visibility = View.VISIBLE
                                root.receipt_text_TV.text =
                                    "Mail successfully sent ${currentUser.email}!"
                                root.kitchen_retry_button.text = "close"
                                root.kitchen_retry_button.setOnClickListener { close() }
                            }

                        },
                        {
                            root.receipt_progress.visibility = View.GONE
                            root.email_error_IV.visibility = View.VISIBLE
                            root.receipt_text_TV.text = "Error while sending!"
                            Log.e("BlackMailin", "error")
                            it.printStackTrace()
                        }
                    )
                println("Sending second mail")
                Mailer.sendMail(
                    adminEmail,
                    subject,
                    emailBody
                ).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            root.email_progress2.visibility = View.GONE
                            root.email_TV2.text = "Mail successfully sent ${adminEmail}!"
                            root.email_success_IV2.visibility = View.VISIBLE
                            Toast.makeText(this, "success", Toast.LENGTH_SHORT).show()
                        },
                        {
                            root.email_progress2.visibility = View.GONE
                            root.email_error_IV2.visibility = View.VISIBLE
                            root.email_TV2.text = "Error while sending!"
                            Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show()
                            it.printStackTrace()
                        }
                    )
            } else {
                root.receipt_progress.visibility = View.INVISIBLE
                root.email_progress2.visibility = View.INVISIBLE
                root.receipt_text_TV.text = "No internet connection!"
                root.email_TV2.text = ""
                root.email_error_IV.visibility = View.VISIBLE
                Toast.makeText(this, "No internet connection!!", Toast.LENGTH_SHORT).show()
            }
        }
        root.receipt_retry_button.setOnClickListener { dialog.dismiss() }
    }

    private fun isOnline(): Boolean {
        val cm =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

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

    fun lastItemCostMultiplier(view: View) {
        when (view.id) {
            R.id.discount_25_button -> lastItemCostMultiplier(0.75f)
            R.id.discount_50_button -> lastItemCostMultiplier(0.5f)
            R.id.discount_100_button -> lastItemCostMultiplier(0f)
            R.id.refund_button -> lastItemCostMultiplier(-1f)
        }
    }

    private fun lastItemCostMultiplier(percentCost: Float) {
        orderAdapter.lastItemCostMultiplier(percentCost)
    }

    private fun printOrder(orders: List<Order>) {
        val (dialog, view) = printerDialog()

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

    fun finishSession(view: View) {
        openExitDialog()
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
        val historyAdapter = HistoryAdapter(orderHistory)
        root.history_RV.adapter = historyAdapter
    }
}