package edu.amrita.jayadev.amritacafe.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.menu.MenuItem
import edu.amrita.jayadev.amritacafe.model.MenuAdapter
import edu.amrita.jayadev.amritacafe.model.OrderAdapter
import edu.amrita.jayadev.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import android.view.MenuItem as ViewMenuItem


class MainActivity : AppCompatActivity() {

    private var dialogOpen = false

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configuration = Configuration(PreferenceManager.getDefaultSharedPreferences(this))

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

        orderAdapter = OrderAdapter(this)
        orderAdapter.orderChanged = {
            total_cost_TV.text = orderAdapter.orderItems.map { it.totalPrice }.sum().toString()
        }

        order_ListView.adapter = orderAdapter

        order_ListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, _, _ -> }


        order_button.setOnClickListener {
            dialogOpen = true

            val mDialogView =
                LayoutInflater.from(this).inflate(edu.amrita.jayadev.amritacafe.R.layout.response_dialog, null)
            //AlertDialogBuilder
            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)

            val mAlertDialog = mBuilder.show()
            mAlertDialog.setCanceledOnTouchOutside(false)

            mAlertDialog.setOnKeyListener { _, _, _ ->
                println("key")
                true
            }
            //show dialog
            //login button click of custom layout
//            currentReceiptTV = mDialogView.receipt_TV


//            mDialogView.kitchen_button.setOnClickListener {
//                startAPrintJob(
//                    Configuration.current.kitchenPrinterIP, mDialogView.kitchen_TV2, mDialogView.kitchen_progress2,
//                    finalOrder,
//                    finalOrderNumber,
//                    finalTotalToPay
//                )
//            }
//            mDialogView.receipt_button.setOnClickListener {
//                startAPrintJob(
//                    Configuration.current.receiptPrinterIP, mDialogView.receipt_TV, mDialogView.receipt_progress,
//                    finalOrder,
//                    finalOrderNumber,
//                    finalTotalToPay
//                )
//            }
//            //cancel button click of custom layout
//            mDialogView.next_button.setOnClickListener {
//                dialogOpen = false
//                currentOrderNumber++
//                updateOrderNumber()
//
//                orderAdapter.clear()
//                totalToPay = 0
//                updateOrderList(0)
//                mAlertDialog.dismiss()
//            }
        }
    }

    override fun onBackPressed() {
        println("dialog open: $dialogOpen")
        if (!dialogOpen)
            finish()
    }

    fun startNewOrder() {
        orderAdapter.clear()
    }


    override fun onStart() {
        super.onStart()
        menuAdapter = MenuAdapter(applicationContext, configuration.currentMenu)
        gridView.adapter = menuAdapter

        configuration.registerMealChangedListener {
            menuAdapter.setMenu(configuration.currentMenu)
        }

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            when (val menuItem = view.tag) {
                is MenuItem -> {
                    orderAdapter.add( menuItem )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.switch_menu).title = configuration.currentMeal.name
        return true
    }

    override fun onOptionsItemSelected(item: ViewMenuItem): Boolean {
        return when (item.itemId) {
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}

