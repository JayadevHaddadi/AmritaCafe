package edu.amrita.jayadev.amritacafe.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.model.MenuAdapter
import edu.amrita.jayadev.amritacafe.model.OrderAdapter
import edu.amrita.jayadev.amritacafe.printer.Printer
import edu.amrita.jayadev.amritacafe.settings.SettingsRetriver
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val MAX_RANGE = 100
    private var currentOrderNumber = 100
    private var orderRange = 100

    object DbConstants {
        val PREFERENCE_KEY = "PREFERENCE_KEY"
        val ORDER_NR_KEY = "ORDER_KEY"
        val RANGE_KEY = "RANGE_KEY"
        lateinit var sharedPreference: SharedPreferences
    }

    private lateinit var kitchenPrinter: Printer
    private lateinit var receiptPrinter: Printer
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter

    var settings = SettingsRetriver(this)
    var totalToPay = 0

    val LUNCH_DINNER = "Lunch/Dinner"
    val BREAKFAST = "Breakfast"
    var currentMenu = LUNCH_DINNER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        try {
//            this.supportActionBar!!.hide()
//        } catch (e: NullPointerException) {
//        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main)

//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)

        orderAdapter = OrderAdapter(
            this, this::updateOrderList
        )

        order_ListView.adapter = orderAdapter

        order_ListView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            }

        DbConstants.sharedPreference = getSharedPreferences(DbConstants.PREFERENCE_KEY, Context.MODE_PRIVATE)
        currentOrderNumber = DbConstants.sharedPreference.getInt(DbConstants.ORDER_NR_KEY, currentOrderNumber)



        order_button.setOnClickListener {
            val finalOrder = orderAdapter.orderList.toMutableList()
            val finalOrderNumber = currentOrderNumber
            val finalTotalToPay = totalToPay

            Thread(Runnable {
                kitchenPrinter.runPrintReceiptSequence(
                    finalOrder,
                    finalOrderNumber,
                    finalTotalToPay
                )
            }).start()
            Thread(Runnable {
                receiptPrinter.runPrintReceiptSequence(
                    finalOrder,
                    finalOrderNumber,
                    finalTotalToPay
                )
            }).start()

            currentOrderNumber++
            updateOrderNumber()

            orderAdapter.clear()
            totalToPay = 0
            updateOrderList(0)
        }
    }

    override fun onResume() {
        super.onResume()
        settings.readSettings()
        kitchenPrinter = Printer(this, settings.kitchenPrinterIP)
        receiptPrinter = Printer(this, settings.receiptPrinterIP)
        menuAdapter = MenuAdapter(applicationContext, settings.dinnerLunchMenu)
        gridView.adapter = menuAdapter
        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->
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
        total_cost_TV.setText(totalToPay.toString())
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
                    menuAdapter.setMenu(settings.breakfastMenu)
                    item.setTitle(BREAKFAST)
                } else if (item.title == BREAKFAST) {
                    menuAdapter.setMenu(settings.dinnerLunchMenu)
                    item.setTitle(LUNCH_DINNER)
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