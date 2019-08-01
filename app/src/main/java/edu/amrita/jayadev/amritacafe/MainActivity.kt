package edu.amrita.jayadev.amritacafe

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    data class OrderItem(val name: String, val amount: Int, val totPrice: Int)

    private var orderList: MutableList<OrderItem> = mutableListOf()
    internal lateinit var gridView: GridView
    val TAG = "debug"

    var adapter2: RecipeAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridView = findViewById(R.id.gridView) as GridView


        adapter2 = RecipeAdapter(
            this, orderList
        )

        val menuList = SettingsRetriver.getList(this)
        val adapter = AlphabetAdapter(applicationContext, menuList)

        gridView.adapter = adapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, v, position, id ->
            orderList.add(OrderItem(menuList[position].name,1,menuList[position].price))
            Log.d(TAG, "adding1")
            adapter2?.notifyDataSetChanged()
        }

        order_ListView.adapter = adapter2

        order_ListView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
                orderList.removeAt(i)
                adapter2?.notifyDataSetChanged()
            }

    }

    class RecipeAdapter(
        private val context: Context,
        private var orderList: MutableList<OrderItem>
    ) : BaseAdapter() {

        private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getCount(): Int {
            return orderList.size
        }

        override fun getItem(position: Int): Any {
            return orderList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView = inflater.inflate(R.layout.order_list, parent, false)

            val label = rowView.findViewById<TextView>(R.id.label)
            val amount = rowView.findViewById<TextView>(R.id.amount_TV)
            val price = rowView.findViewById<TextView>(R.id.price_TV)
            label.text = orderList[position].name
            amount.text = orderList[position].amount.toString()
            price.text = orderList[position].totPrice.toString()

            return rowView
        }

    }
}
