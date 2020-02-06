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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.epson.epos2.Epos2Exception
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.email.Mailer
import edu.amrita.amritacafe.menu.Category
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

    private var printMode = true
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

        val root = File(Environment.getExternalStorageDirectory(), "AmritaCafe");
        if (!root.exists()) {
            root.mkdirs();
        }

        val currentTime = Calendar.getInstance().getTime()
        val DATE_FORMAT_2 = "dd-MMM-yyyy kk-mm EEEE"; // hh-mm a for pm/am
        val dateFormat = SimpleDateFormat(DATE_FORMAT_2, Locale.US);
        val fileDate = dateFormat.format(currentTime)
        val user = "Jayadev"
        val fileName = "${fileDate} - $user.txt"

        file = File(root, fileName)

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


        printMode = false

        order_button.setOnClickListener {
            val orders = Order(currentOrderNumber, orderAdapter.orderItems)
                .collectToppings().split(orderNumberService)

            if (printMode) {
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


    private fun sendEmail() {
        Mailer.sendMail("jayadev.haddadi@gmail.com", file.name, file.readText())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Toast.makeText(this, "Mail send check e-mail", Toast.LENGTH_SHORT).show() },
                { Log.e("BlackMailin", "error") }
            )
    }

    override fun onBackPressed() {
        println("dialog open: $dialogOpen")
        if (!dialogOpen) {
            if (!printMode) {
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
                root.button_finish.setOnClickListener {
                    if (isOnline()) {
                        root.email_progress.visibility = View.VISIBLE
                        Mailer.sendMail("jayadev.haddadi@gmail.com", file.name, file.readText())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                {
                                    root.email_TV.text = "Done!"
                                    root.email_progress.visibility = View.GONE
                                    root.button_cancel.visibility = View.GONE
                                    root.button_finish.text = "FINISH"
                                    root.button_finish.setOnClickListener { finish() }
                                    Toast.makeText(this, "Mail send check e-mail", Toast.LENGTH_SHORT).show() },
                                {
                                    root.email_TV.text = "Error while sending!"
                                    root.email_error_IV.visibility = View.VISIBLE
                                    root.email_progress.visibility = View.GONE
                                    root.button_finish.visibility = View.GONE
                                    Log.e("BlackMailin", "error") }
                            )
                    } else {
                        root.email_TV.text = "No internet connection!"
                        root.button_finish.visibility = View.INVISIBLE
                        root.email_error_IV.visibility = View.VISIBLE
                        Toast.makeText(this, "No internet connection!!", Toast.LENGTH_SHORT).show()
                    }
                }
                root.button_cancel.setOnClickListener { dialog.dismiss() }
            } else
                finish()
        }
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
            if (printMode)
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
        startNewOrder()

        menuAdapter = MenuAdapter(configuration, applicationContext) {
            runOnUiThread { menuAdapter.notifyDataSetChanged() }
        }
        gridView.adapter = menuAdapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItem -> {
                    orderAdapter.add(menuItem)
                }
            }
        }
        gridView.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, view, _, _ ->
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
            R.id.history_menu -> {
                if (isOnline()) {
                    sendEmail()
                } else {
                    Toast.makeText(this, "No internet connection!!", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

