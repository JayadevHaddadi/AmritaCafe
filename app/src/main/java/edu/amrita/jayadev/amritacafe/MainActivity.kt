package edu.amrita.jayadev.amritacafe

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    data class OrderItem(val name: String, var amount: Int, var totPrice: Int, var comment: String = "", var commentOn: Boolean = false)

    private var orderList: MutableList<OrderItem> = mutableListOf()
    internal lateinit var gridView: GridView
    val TAG = "debug"
    var totalCost = 0

    var orderAdapter: OrderAdapter? = null

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
                if(menuList[position].name == orderItem.name) {
                    orderItem.amount++
                    orderItem.totPrice += menuList[position].price
                    updateOrderList(menuList[position].price)
                    return@OnItemClickListener
                }
            }
            orderList.add(OrderItem(menuList[position].name,1,menuList[position].price))
            updateOrderList(menuList[position].price)
        }

        order_ListView.adapter = orderAdapter

        order_ListView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            }



    }

    private fun updateOrderList(cost: Int) {
        totalCost += cost
        total_cost_TV.setText(totalCost.toString())
        orderAdapter?.notifyDataSetChanged()
    }


}
