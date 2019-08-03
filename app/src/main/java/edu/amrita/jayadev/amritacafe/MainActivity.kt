package edu.amrita.jayadev.amritacafe

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.epson.epos2.printer.Printer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    data class OrderItem(
        val name: String,
        var amount: Int,
        var totPrice: Int,
        var comment: String = "",
        var commentOn: Boolean = false
    )

    val TAG = "debug"
    private val MAX_RANGE = 100
    private var orderNumber: Int = 100

    private val PREFERENCE_KEY: String = "PREFERENCE_NAME"
    private val ORDER_NR_KEY: String = "ORDER_NR"
    private lateinit var sharedPreference: SharedPreferences

    private var orderList: MutableList<OrderItem> = mutableListOf()
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var gridView: GridView

    val settings = SettingsRetriver(this)
    var totalCost = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            this.supportActionBar!!.hide()
        } catch (e: NullPointerException) {
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main)

        gridView = findViewById(R.id.gridView) as GridView

        orderAdapter = OrderAdapter(
            this, orderList
        )

        val menuList = settings.menuList

        val adapter = MenuAdapter(applicationContext, menuList)

        gridView.adapter = adapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->
            if (menuList[position].name == "")
                return@OnItemClickListener

            for (orderItem in orderList) {
                if (menuList[position].name == orderItem.name) {
                    orderItem.amount++
                    orderItem.totPrice += menuList[position].price
                    updateOrderList(menuList[position].price)
                    return@OnItemClickListener
                }
            }
            orderList.add(OrderItem(menuList[position].name, 1, menuList[position].price))
            updateOrderList(menuList[position].price)
        }

        order_ListView.adapter = orderAdapter

        order_ListView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            }

        sharedPreference = getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
        orderNumber = sharedPreference.getInt(ORDER_NR_KEY, orderNumber)
        updateOrderNumber()

        order_button.setOnClickListener {
            orderList.clear()
            totalCost = 0
            updateOrderList(0)

            orderNumber++
            updateOrderNumber()

            println("Printing to: ${settings.printerOne}")

            try {
                val mPrinter = Printer(Printer.TM_T82, Printer.MODEL_ANK, this) // TM_M30
                mPrinter.connect("TCP:" + settings.printerOne, Printer.PARAM_DEFAULT);
                val textData = StringBuilder()
                mPrinter.addFeedLine(1)
                mPrinter.addText(textData.toString())
                textData.append("MAAAIN STRING")
                mPrinter.addText(textData.toString())
                mPrinter.addFeedLine(2)
                mPrinter.addCut(Printer.CUT_FEED)
                mPrinter.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "errrroorrr: $e", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateOrderNumber() {
        println("OrderNr:$orderNumber Range:${settings.range}")
        if (orderNumber < settings.range || orderNumber >= (settings.range + MAX_RANGE))
            orderNumber = settings.range

        val editor = sharedPreference.edit()
        editor.putInt(ORDER_NR_KEY, orderNumber)
        editor.apply()

        order_numberr_TV.text = orderNumber.toString()
    }

    private fun updateOrderList(cost: Int) {
        totalCost += cost
        total_cost_TV.setText(totalCost.toString())
        orderAdapter.notifyDataSetChanged()
    }
}