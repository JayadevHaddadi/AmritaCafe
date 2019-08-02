package edu.amrita.jayadev.amritacafe

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.order_item.view.*

class OrderAdapter(
    private val context: Context,
    private var orderList: MutableList<MainActivity.OrderItem>
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
        val rowView = inflater.inflate(R.layout.order_item, parent, false)

        val label = rowView.findViewById<TextView>(R.id.label)
        val amount = rowView.findViewById<TextView>(R.id.amount_TV)
        val price = rowView.findViewById<TextView>(R.id.price_TV)
        label.text = orderList[position].name
        amount.text = orderList[position].amount.toString()
        price.text = orderList[position].totPrice.toString()

        val cancelButton = rowView.findViewById<ImageView>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            orderList.removeAt(position)
            notifyDataSetChanged()
        }

        if(orderList[position].commentOn) {
            rowView.comment_ET.visibility = View.VISIBLE
            rowView.comment_ET.requestFocus()
//                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(rowView.comment_ET,0)
        }

        rowView.setOnClickListener {
            rowView.comment_ET.visibility = View.VISIBLE
            orderList[position].commentOn = true
        }

        return rowView
    }

}