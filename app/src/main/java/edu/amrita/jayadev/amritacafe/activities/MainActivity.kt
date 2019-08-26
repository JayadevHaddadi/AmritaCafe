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
import edu.amrita.jayadev.amritacafe.model.Order
import edu.amrita.jayadev.amritacafe.model.OrderAdapter
import edu.amrita.jayadev.amritacafe.receiptprinter.OrderNumberService
import edu.amrita.jayadev.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.MenuItem as ViewMenuItem


class MainActivity : AppCompatActivity() {

    private var dialogOpen = false

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var configuration: Configuration
    private lateinit var orderNumberService: OrderNumberService
    private var currentOrderNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.getDefaultSharedPreferences(this).let { pref ->
            configuration = Configuration(pref)
            orderNumberService = OrderNumberService(pref)
        }

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
            
        }
    }

    override fun onBackPressed() {
        println("dialog open: $dialogOpen")
        if (!dialogOpen)
            finish()
    }

    private fun startNewOrder() {
        orderAdapter.clear()
        GlobalScope.launch {
            currentOrderNumber = orderNumberService.next()
            runOnUiThread {
                order_number_TV.text = currentOrderNumber.toString()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startNewOrder()

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

