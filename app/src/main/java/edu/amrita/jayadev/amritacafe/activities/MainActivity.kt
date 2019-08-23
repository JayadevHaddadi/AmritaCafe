package edu.amrita.jayadev.amritacafe.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.model.MenuAdapter
import edu.amrita.jayadev.amritacafe.model.OrderAdapter
import edu.amrita.jayadev.amritacafe.printer.Printer
import edu.amrita.jayadev.amritacafe.settings.Configuration
import edu.amrita.jayadev.amritacafe.settings.breakfastMenu
import edu.amrita.jayadev.amritacafe.settings.lunchDinnerMenu
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.response_dialog.view.*


class MainActivity : AppCompatActivity() {

    private var dialogOpen = false
    private var currentReceiptTV: TextView? = null
    private val MAX_RANGE = 100
    private var currentOrderNumber = 100
    private var orderRange = 100

    object DbConstants {
        const val PREFERENCE_KEY = "PREFERENCE_KEY"
        const val ORDER_NR_KEY = "ORDER_KEY"
        const val RANGE_KEY = "RANGE_KEY"
        lateinit var sharedPreference: SharedPreferences
    }

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter

    var totalToPay = 0

    private val LUNCH_DINNER = "Lunch/Dinner"
    private val BREAKFAST = "Breakfast"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        try {
//            this.supportActionBar!!.hide()
//        } catch (e: NullPointerException) {
//        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)

        orderAdapter = OrderAdapter(
            this, this::updateOrderList
        )

        order_ListView.adapter = orderAdapter

        order_ListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, _, _ -> }

        DbConstants.sharedPreference = getSharedPreferences(DbConstants.PREFERENCE_KEY, Context.MODE_PRIVATE)
        currentOrderNumber = DbConstants.sharedPreference.getInt(DbConstants.ORDER_NR_KEY, currentOrderNumber)



        order_button.setOnClickListener {
            dialogOpen = true
            val finalOrder = orderAdapter.orderList.toMutableList()
            val finalOrderNumber = currentOrderNumber
            val finalTotalToPay = totalToPay

            val mDialogView =
                LayoutInflater.from(this).inflate(edu.amrita.jayadev.amritacafe.R.layout.response_dialog, null)
            //AlertDialogBuilder
            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)

            val mAlertDialog = mBuilder.show()
            mAlertDialog.setCanceledOnTouchOutside(false)
//            mAlertDialog.onBackPressed {
//
//            }
//            mAlertDialog.setOnDismissListener {
//                println("dismiss1")
//            }
//            mAlertDialog.setOnCancelListener {
//                println("cancel")
//            }
            mAlertDialog.setOnKeyListener { _, _, _ ->
                println("key")
                true
            }
            //show dialog
            //login button click of custom layout
            currentReceiptTV = mDialogView.receipt_TV

            startAPrintJob(
                Configuration.current.kitchenPrinterIP, mDialogView.kitchen_TV2, mDialogView.kitchen_progress2,
                finalOrder,
                finalOrderNumber,
                finalTotalToPay
            )
            startAPrintJob(
                Configuration.current.receiptPrinterIP, mDialogView.receipt_TV, mDialogView.receipt_progress,
                finalOrder,
                finalOrderNumber,
                finalTotalToPay
            )
            mDialogView.kitchen_button.setOnClickListener {
                startAPrintJob(
                    Configuration.current.kitchenPrinterIP, mDialogView.kitchen_TV2, mDialogView.kitchen_progress2,
                    finalOrder,
                    finalOrderNumber,
                    finalTotalToPay
                )
            }
            mDialogView.receipt_button.setOnClickListener {
                startAPrintJob(
                    Configuration.current.receiptPrinterIP, mDialogView.receipt_TV, mDialogView.receipt_progress,
                    finalOrder,
                    finalOrderNumber,
                    finalTotalToPay
                )
            }
//            //cancel button click of custom layout
            mDialogView.next_button.setOnClickListener {
                dialogOpen = false
                currentOrderNumber++
                updateOrderNumber()

                orderAdapter.clear()
                totalToPay = 0
                updateOrderList(0)
                mAlertDialog.dismiss()
            }
        }
    }

    override fun onBackPressed() {
        println("dilaogopen: $dialogOpen")
        if (!dialogOpen)
            finish()
    }

    private fun startAPrintJob(
        receiptPrinterIP: String,
        receiptTv: TextView,
        receiptProgress: ProgressBar,
        finalOrder: MutableList<OrderAdapter.OrderItem>,
        finalOrderNumber: Int,
        finalTotalToPay: Int
    ) {
        Thread(Runnable {
            val tempPrinter =
                Printer(this, receiptPrinterIP, receiptTv, receiptProgress)
            tempPrinter.runPrintReceiptSequence(
                finalOrder,
                finalOrderNumber,
                finalTotalToPay
            )
        }).start()
    }

    override fun onStart() {
        super.onStart()
        Configuration.loadLocal(this)
        menuAdapter = MenuAdapter(applicationContext, Configuration.current.menu)
        gridView.adapter = menuAdapter
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val menuList = menuAdapter.menuItems
            if (menuList[position].name == "" || menuList[position].price == 0)
                return@OnItemClickListener

            //Not perfect code, All interactions with orderlist should be done by one class onlu and not
            // in two as below 2 examples
            for (orderItem in orderAdapter.orderList) {
                if (menuList[position].name == orderItem.name) {
                    orderItem.amount++
                    orderItem.totPrice += menuList[position].price
                    updateOrderList(menuList[position].price)
                    return@OnItemClickListener
                }
            }
            orderAdapter.add(
                OrderAdapter.OrderItem(
                    menuList[position].name,
                    1,
                    menuList[position].price
                )
            )
            updateOrderList(menuList[position].price)
        }
        orderRange = DbConstants.sharedPreference.getInt(DbConstants.RANGE_KEY, 100)
        updateOrderNumber()
    }

    private fun updateOrderNumber() {
        println("OrderNr:$currentOrderNumber Range:${orderRange}")
        if (currentOrderNumber < orderRange || currentOrderNumber >= (orderRange + MAX_RANGE))
            currentOrderNumber = orderRange

        val editor = DbConstants.sharedPreference.edit()
        editor.putInt(DbConstants.ORDER_NR_KEY, currentOrderNumber)
        editor.apply()

        order_numberr_TV.text = currentOrderNumber.toString()
    }

    private fun updateOrderList(cost: Int) {
        totalToPay += cost
        total_cost_TV.text = totalToPay.toString()
        orderAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.switch_menu -> {
                if (item.title == LUNCH_DINNER) {
                    menuAdapter.setMenu(Configuration.current.menu.lunchDinnerMenu)
                    item.title = BREAKFAST
                } else if (item.title == BREAKFAST) {
                    menuAdapter.setMenu(Configuration.current.menu.breakfastMenu)
                    item.title = LUNCH_DINNER
                }
                true
            }
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

