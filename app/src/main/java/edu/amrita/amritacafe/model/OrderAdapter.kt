package edu.amrita.amritacafe.model

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import edu.amrita.amritacafe.databinding.ItemOrderBinding
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.menu.TOPPING

class OrderAdapter(context: Context) : BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val orderList: MutableList<RegularOrderItem> = mutableListOf()

    val orderItems: List<RegularOrderItem> = orderList

    var orderChanged: () -> Unit = {} // Replaced by correct callback in MainActivity

    override fun getCount(): Int {
        return orderList.size
    }

    override fun getItem(position: Int): Any {
        return orderList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun remove(item: RegularOrderItem) {
        orderList.removeAll { it == item }
        updateAll()
    }

    fun add(addItem: MenuItem, uniqueItem: Boolean = false): Int {
        try {
            var found = false
            var listToCheck = orderList

            if (addItem.category == TOPPING) {
                listToCheck = listToCheck.subList(
                    listToCheck.indexOfLast { it.menuItem.category != TOPPING },
                    listToCheck.size
                )
            }

            for (existingItem in listToCheck) {
                if (addItem.code == existingItem.menuItem.code &&
                    addItem.category == existingItem.menuItem.category &&
                    existingItem.comment == "" && !uniqueItem
                ) {
                    existingItem.quantity++
                    found = true
                    break
                }
            }

            if (!found)
                orderList.add(RegularOrderItem(addItem))
            else
                orderList[0].increment()

            updateAll()
            return 1
        } catch (e: Exception) {
            return -1
        }
    }

    fun clear() {
        orderList.clear()
        updateAll()
    }

    private fun updateAll() {
        notifyDataSetChanged()
        orderChanged()
    }

    private fun reuseOrInflate(view: View?, parent: ViewGroup): View {
        val binding: ItemOrderBinding = if (view == null) {
            // Inflate the layout with ViewBinding
            ItemOrderBinding.inflate(inflater, parent, false)
        } else {
            ItemOrderBinding.bind(view)
        }

        with(binding) {
            amountTV.setOnClickListener {
                val position = root.tag as Int
                orderList[position].increment()
                notifyDataSetChanged()
                orderChanged()
            }

            root.setOnClickListener {
                commentET.run {
                    visibility = View.VISIBLE
                    requestFocus()
                    context.getSystemService(Context.INPUT_METHOD_SERVICE)?.let {
                        (it as InputMethodManager).showSoftInput(this, 0)
                    }
                }
            }

            cancelButton.setOnClickListener {
                remove(orderList[root.tag as Int])
            }

            commentET.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    val position = root.tag as Int
                    orderList[position] = orderItems[position].editComment(p0.toString())
                }
            })
        }

        return binding.root
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return reuseOrInflate(convertView, parent).apply {
            tag = position

            val orderItem = orderList[position]
            val binding = ItemOrderBinding.bind(this)

            with(binding) {
                label.text = orderItem.menuItem.code
                amountTV.text = orderItem.quantity.toString()
                priceTV.text = orderItem.priceWithoutExtras.toString()
                commentET.setText(orderItem.comment)
                commentET.visibility = if (orderItem.comment.isNotBlank()) View.VISIBLE else View.GONE
            }
        }
    }
}
