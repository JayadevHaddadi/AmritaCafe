package edu.amrita.jayadev.amritacafe

import android.content.Context
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

    private var orderNumber: Int = 100
    private val PREFERENCE_KEY: String = "PREFERENCE_NAME"
    private val ORDER_NR_KEY: String = "ORDER_NR"

    private var orderList: MutableList<OrderItem> = mutableListOf()
    private var orderAdapter: OrderAdapter? = null
    internal lateinit var gridView: GridView
    val TAG = "debug"

    var totalCost = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridView = findViewById(R.id.gridView) as GridView


        orderAdapter = OrderAdapter(
            this, orderList
        )

        val menuList = SettingsRetriver.getList(this)
        val adapter = MenuAdapter(applicationContext, menuList)

        gridView.adapter = adapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->
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

        val sharedPreference = getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
        orderNumber = sharedPreference.getInt(ORDER_NR_KEY, orderNumber)
        order_numberr_TV.text = orderNumber.toString()

        order_button.setOnClickListener {
            orderList.clear()
            totalCost = 0
            updateOrderList(0)

            orderNumber++
            val editor = sharedPreference.edit()
            editor.putInt(ORDER_NR_KEY, orderNumber)
            editor.apply()

            order_numberr_TV.text = orderNumber.toString()

            try {
                val mPrinter = Printer(Printer.TM_M30,Printer.MODEL_ANK,this)

                mPrinter.connect("TCP:192.168.0.10", Printer.PARAM_DEFAULT);

                var method = ""
                val textData = StringBuilder()

                method = "addTextAlign"
                method = "addFeedLine"
                mPrinter.addFeedLine(1)

                method = "addText"
                mPrinter.addText(textData.toString())
                textData.append("MAAAIN STRING")
                method = "addText"
                mPrinter.addText(textData.toString())
                method = "addFeedLine"
                mPrinter.addFeedLine(2)

                method = "addCut"
                mPrinter.addCut(Printer.CUT_FEED)

                mPrinter.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this,  "errrroorrr: ${e.toString()}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateOrderList(cost: Int) {
        totalCost += cost
        total_cost_TV.setText(totalCost.toString())
        orderAdapter?.notifyDataSetChanged()
    }
}