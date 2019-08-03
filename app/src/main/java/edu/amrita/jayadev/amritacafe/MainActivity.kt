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

    private val MAX_RANGE = 100
    private var orderNumber: Int = 100

    private val PREFERENCE_KEY: String = "PREFERENCE_NAME"
    private val ORDER_NR_KEY: String = "ORDER_NR"
    private lateinit var sharedPreference: SharedPreferences

    private lateinit var orderAdapter: OrderAdapter
    private lateinit var gridView: GridView

    val settings = SettingsRetriver(this)
    var totalToPay = 0

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
            this, this::updateOrderList
        )

        val menuList = settings.menuList
        val adapter = MenuAdapter(applicationContext, menuList)
        gridView.adapter = adapter
        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->
            if (menuList[position].name == "")
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
            orderAdapter.add(OrderAdapter.OrderItem(menuList[position].name, 1, menuList[position].price))
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
            orderAdapter.clear()
            totalToPay = 0
            updateOrderList(0)

            orderNumber++
            updateOrderNumber()

            println("Printing to: ${settings.printerOne}")

            try {
                val mPrinter = Printer(Printer.TM_T82, Printer.MODEL_ANK, this) // TM_M30, MODEL_ANK correct
                mPrinter.connect("TCP:" + settings.printerOne, Printer.PARAM_DEFAULT);//param_default correct
                mPrinter.beginTransaction()
                mPrinter.addTextAlign(1);
                mPrinter.addFeedLine(1);
                mPrinter.addText("AMMMMAAAAA!!!!!")
                mPrinter.addFeedLine(1);
                mPrinter.addCut(Printer.CUT_FEED)
                mPrinter.sendData(Printer.PARAM_DEFAULT)
                mPrinter.endTransaction()
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
        totalToPay += cost
        total_cost_TV.setText(totalToPay.toString())
        orderAdapter.notifyDataSetChanged()
    }
}