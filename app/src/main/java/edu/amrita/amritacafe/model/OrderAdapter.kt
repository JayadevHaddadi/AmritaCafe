package edu.amrita.amritacafe.model

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.menu.OrderItem
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.menu.Topping
import kotlinx.android.synthetic.main.order_item.view.*


class OrderAdapter(context: Context) : BaseAdapter() {


    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val orderList: MutableList<OrderItem> = mutableListOf()

    val orderItems : List<OrderItem> = orderList

    var orderChanged : () -> Unit = {}

    fun isNotEmpty() = count > 0

    override fun getCount(): Int {
        return orderList.size
    }

    override fun getItem(position: Int): Any {
        return orderList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun remove(item: OrderItem) {
        orderList.removeAll { it == item || (it is Topping && it.toppingFor == item) }
        notifyDataSetChanged()
        orderChanged()
    }

    fun add(item: MenuItem) {
        orderList.add(RegularOrderItem(item))
        notifyDataSetChanged()
        orderChanged()
    }

    fun addTopping(menuItem: MenuItem): Boolean {
        val lastAddedOrderItem = orderItems.findLast { it is RegularOrderItem }
        return lastAddedOrderItem is RegularOrderItem && true.apply {
            orderList.add(Topping(menuItem, toppingFor = lastAddedOrderItem))
            notifyDataSetChanged()
            orderChanged()
        }
    }

    fun clear() {
        orderList.clear()
        notifyDataSetChanged()
        orderChanged()
    }

    private fun reuseOrInflate(view: View?, parent: ViewGroup) =
        view ?: inflater.inflate(R.layout.order_item, parent, false).apply {

            val orderItemView = this
            amount_TV.setOnClickListener {

                val position = tag as Int

                orderList[position] = orderList[position].increment()
                notifyDataSetChanged()
                orderChanged()
            }

            setOnClickListener {
                comment_ET.run {
                    visibility = View.VISIBLE
                    requestFocus()
                    context.getSystemService(Context.INPUT_METHOD_SERVICE).also {
                        (it as InputMethodManager).showSoftInput(this, 0)
                    }
                }
            }

            cancel_button.setOnClickListener {
                remove(orderList[tag as Int])
            }

            comment_ET.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    val position = orderItemView.tag as Int
                    orderList[position] = orderItems[position].editComment(p0.toString())
                }
            })
        }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return reuseOrInflate(convertView, parent).apply {
            tag = position

            orderList[position].let { orderItem ->
                label.text = orderItem.menuItem.code
                amount_TV.text = orderItem.quantity.toString()
                price_TV.text = orderItem.totalPrice.toString()
                comment_ET.setText(orderItem.comment)
                comment_ET.visibility =
                    if (orderItem.comment.isNotBlank()) View.VISIBLE
                    else View.GONE
            }
        }
    }
}