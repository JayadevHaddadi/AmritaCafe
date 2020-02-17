package edu.amrita.amritacafe.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.email.Mailer
import edu.amrita.amritacafe.email.allUsers
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.model.MenuAdapter
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.model.OrderAdapter
import edu.amrita.amritacafe.receiptprinter.*
import edu.amrita.amritacafe.settings.Configuration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.finish_dialog.view.*
import kotlinx.android.synthetic.main.login_dialog.view.*
import kotlinx.android.synthetic.main.response_dialog.view.*
import kotlinx.android.synthetic.main.response_dialog.view.button_cancel
import kotlinx.android.synthetic.main.response_dialog.view.button_finish
import kotlinx.android.synthetic.main.response_dialog.view.email_TV
import kotlinx.android.synthetic.main.response_dialog.view.email_progress
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.view.MenuItem as ViewMenuItem


class MainActivity : AppCompatActivity() {

    private var nextItemCostMultiplier = 100
    private lateinit var actionBarMenu: Menu
    private lateinit var userEmail: String
    private var modeAmritapuri = true
    private lateinit var file: File
    private var dialogOpen = false

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration
    private lateinit var orderNumberService: OrderNumberService
    private var currentOrderNumber = 0
    private var totalIncomeThisSession = 0
    private var currentOrderSum = 0

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
            currentOrderSum = orderAdapter.orderItems.map { it.totalPrice }.sum()
            total_cost_TV.text = currentOrderSum.toString()
        }

        order_ListView.adapter = orderAdapter

        button_cancel.setOnClickListener {
            orderAdapter.clear()
        }

        openLoginDialog()

        order_button.setOnClickListener {
            val orders = listOf(Order(currentOrderNumber, orderAdapter.orderItems))

            if (modeAmritapuri) {
                val (dialog, view) = openDialog()

                val printService = PrintService(orders, configuration = configuration,
                    listener = object : PrintService.PrintServiceListener {
                        override fun kitchenPrinterFinished() = runOnUiThread {
                            view.run {
                                kitchen_progress.visibility = View.INVISIBLE
                                image_kitchen_error.visibility = View.INVISIBLE
                                image_kitchen_done.visibility = View.VISIBLE
                                button_finish.visibility = View.INVISIBLE
                            }
                        }

                        override fun receiptPrinterFinished() = runOnUiThread {
                            view.run {
                                email_progress.visibility = View.INVISIBLE
                                image_receipt_error.visibility = View.INVISIBLE
                                image_receipt_done.visibility = View.VISIBLE
                                button_cancel.visibility = View.INVISIBLE
                            }
                        }

                        override fun receiptPrinterError(response: PrintFailed) = runOnUiThread {
                            view.run {
                                email_progress.visibility = View.INVISIBLE
                                image_receipt_error.visibility = View.VISIBLE
                                button_cancel.visibility = View.VISIBLE
                            }
                        }

                        override fun receiptPrinterError(
                            errorStatus: ErrorStatus,
                            exception: Epos2Exception
                        ) = runOnUiThread {
                            view.run {
                                email_progress.visibility = View.INVISIBLE
                                image_receipt_error.visibility = View.VISIBLE
                                button_cancel.visibility = View.VISIBLE
                            }
                        }

                        override fun kitchenPrinterError(response: PrintFailed) = runOnUiThread {
                            view.run {
                                kitchen_progress.visibility = View.INVISIBLE
                                image_kitchen_error.visibility = View.VISIBLE
                                button_finish.visibility = View.VISIBLE
                            }
                        }

                        override fun kitchenPrinterError(
                            errorStatus: ErrorStatus,
                            exception: Epos2Exception
                        ) = runOnUiThread {
                            view.run {
                                kitchen_progress.visibility = View.INVISIBLE
                                image_kitchen_error.visibility = View.VISIBLE
                                button_finish.visibility = View.VISIBLE
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

                view.button_finish.setOnClickListener {
                    printService.retry()
                    it.visibility = View.INVISIBLE
                    view.image_kitchen_error.visibility = View.INVISIBLE
                    view.kitchen_progress.visibility = View.VISIBLE
                }

                view.button_cancel.setOnClickListener {
                    printService.retry()
                    it.visibility = View.INVISIBLE
                    view.image_receipt_error.visibility = View.INVISIBLE
                    view.email_progress.visibility = View.VISIBLE
                }

                println("Started Print Job")
            }

            file.appendText("Order: $currentOrderNumber\n")
            for (item in orderAdapter.orderItems) {
                file.appendText(
                    "Item:${item.menuItem.name} Quantity:${item.quantity} " +
                            "Sum:${item.totalPrice} Comment:${item.comment} \n"
                )
            }
            file.appendText("Order total:${currentOrderSum}\n")
            totalIncomeThisSession += currentOrderSum
            file.appendText("Session total:${totalIncomeThisSession}\n\n")

            println("Text in file : \n" + file.readText())

            startNewOrder()
        }
    }

    private fun openLoginDialog() {
        val (dialog, root) =
            LayoutInflater.from(this).inflate(R.layout.login_dialog, null).let { view ->
                AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(true)
                    .show()
                    .to(view)
            }

        dialog.setOnCancelListener { createHistoryFile(dialog) }
        val namesOnly = allUsers.map {
            it.name
        }
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, namesOnly)
        // Set layout to use when the list of choices appear
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Set Adapter to Spinner
        root.user_spinner.setAdapter(aa)
        root.cancel_login_button.setOnClickListener {
            createHistoryFile(dialog)
        }
        root.login_button.setOnClickListener {
            val user = allUsers[root.user_spinner.selectedItemPosition]
            if (root.password_ET.text.toString() == user.password) {
                modeAmritapuri = false
                userEmail = user.email
                Toast.makeText(
                    this,
                    "Succesfully logged as ${user.name}\n${userEmail}",
                    Toast.LENGTH_SHORT
                ).show()
                createHistoryFile(dialog, user.name)
            } else {
                Toast.makeText(
                    this,
                    "Failed login, you have infinite attempts left",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createHistoryFile(dialog: AlertDialog?, user: String? = "Amritapuri") {
        println("Logged in as: $user")
        startNewOrder()
        val root = File(Environment.getExternalStorageDirectory(), "Amrita Cafe");
        if (!root.exists())
            root.mkdirs();

        if (modeAmritapuri)
            actionBarMenu.findItem(R.id.finish_menu).setVisible(false)

        title = "Amrita Cafe - $user"

        val currentTime = Calendar.getInstance().getTime()
        val DATE_FORMAT_2 = "dd-MMM-yyyy kk-mm EEE"; // hh-mm a for pm/am
        val dateFormat = SimpleDateFormat(DATE_FORMAT_2, Locale.US);
        val fileDate = dateFormat.format(currentTime)
        val fileName = "${fileDate} - $user.txt"

        file = File(root, fileName)
        file.appendText("")
        dialog?.dismiss()
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

    private fun openExitDialog() {
        val (dialog, root) =
            LayoutInflater.from(this).inflate(R.layout.finish_dialog, null).let { view ->
                AlertDialog.Builder(this)
                    .setView(view)
//                            .setTitle("Finished session")
//                            .setIcon(R.drawable.ic_done_black_24dp)
                    .setCancelable(false)
                    .show()
                    .apply {
                        setCanceledOnTouchOutside(false)
                    }.to(view)
            }
        root.email_TV.text = "Finished with current session?\nEmail will be sent to: ${userEmail}"
        root.button_finish.setOnClickListener {
            root.final_comments_ET.visibility = View.GONE
            root.email_progress.visibility = View.VISIBLE
            root.button_finish.visibility = View.GONE
            if (isOnline()) {
                root.email_TV.text = "Sending email to $userEmail..."
                val emailBody = "${file.readText()}Final Comments:\n${root.final_comments_ET.text}"
                Mailer.sendMail(userEmail, file.name, emailBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            fun close() {
                                dialog.dismiss()
                                println("finishing")
                                finish()
                            }
                            root.email_progress.visibility = View.GONE
                            root.button_cancel.visibility = View.GONE
                            root.button_finish.visibility = View.VISIBLE
                            root.email_TV.text = "Mail successfully sent! Thank you!"
                            root.button_finish.text = "close"
                            root.button_finish.setOnClickListener { close() }
                            if(!dialog.isShowing) {
                                Toast.makeText(this, "Mail successfully sent! Thank you!", Toast.LENGTH_SHORT)
                                    .show()
                                close()
                            }

                        },
                        {
                            root.email_progress.visibility = View.GONE
                            root.email_error_IV.visibility = View.VISIBLE
                            root.email_TV.text = "Error while sending!"
                            Log.e("BlackMailin", "error")
                            it.printStackTrace()
                        }
                    )
            } else {
                root.email_TV.text = "No internet connection!"
                root.email_error_IV.visibility = View.VISIBLE
                Toast.makeText(this, "No internet connection!!", Toast.LENGTH_SHORT).show()
            }
        }
        root.button_cancel.setOnClickListener { dialog.dismiss() }
    }

    fun isOnline(): Boolean {
        val cm =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return if (netInfo != null && netInfo.isConnectedOrConnecting) {
            true
        } else false
    }

    private fun startNewOrder() {
        GlobalScope.launch {
            println("New order, printMode: $modeAmritapuri")
            if (modeAmritapuri)
                currentOrderNumber = orderNumberService.next()
            else
                currentOrderNumber++

            runOnUiThread {
                orderAdapter.clear()
                order_number_TV.text = currentOrderNumber.toString()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        menuAdapter = MenuAdapter(configuration, applicationContext) {
            runOnUiThread { menuAdapter.notifyDataSetChanged() }
        }
        menuGridView.adapter = menuAdapter

        menuGridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItem -> {
                    orderAdapter.add(menuItem)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        actionBarMenu = menu
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.switch_menu).title = configuration.currentMeal.name
        menu.findItem(R.id.names_menu).title = getToggleNameString()
        return true
    }

    fun getToggleNameString(): String {
        return if (configuration.showMenuItemNames) "Short name" else "Full name"
    }

    override fun onOptionsItemSelected(item: ViewMenuItem): Boolean {
        return when (item.itemId) {

            R.id.names_menu -> {
                configuration.toggleName()
                item.title = getToggleNameString()
                true
            }

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
            R.id.finish_menu -> {
                openExitDialog()
                true
            }
            R.id.discount_100 -> {
                nextItemCostMultiplier(0f)
                true
            }
            R.id.discount_50 -> {
                nextItemCostMultiplier(0.5F)
                true
            }
            R.id.discount_25 -> {
                nextItemCostMultiplier(0.75f)
                true
            }
            R.id.refund -> {
                nextItemCostMultiplier(-1f)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun nextItemCostMultiplier(percentCost: Float) {
        orderAdapter.lastItemCostMultiplier(percentCost)
        Toast.makeText(this,"Last item reduced to ${percentCost*100}% of normal price",Toast.LENGTH_SHORT).show()
    }

    /**
     * Returns a tuple containing the dialog, and the view.
     */
    @SuppressLint("InflateParams")
    private fun openDialog() =
        LayoutInflater.from(this).inflate(R.layout.response_dialog, null).let { view ->
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

