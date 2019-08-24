package edu.amrita.jayadev.amritacafe.model


import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Typeface.BOLD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.menu.Category
import edu.amrita.jayadev.amritacafe.menu.MenuItem
import kotlinx.android.synthetic.main.menu_item.view.*

class MenuAdapter(private val mContext: Context, menu: List<MenuItem>) :
    BaseAdapter() {

    private fun setTheItems(menuItems: List<MenuItem>) : List<Any>
        = menuItems.groupBy {
           it.category
        }.toSortedMap(Comparator { left, right -> left.ordinal.compareTo(right.ordinal) })
            .map<Category, List<MenuItem>, List<Any>> { (category, items) ->
                listOf(category) + items + Array(10 - (items.size + 1) % 10) {Unit}
        }.flatten()

    private var menuItems = setTheItems(menu)

    var colorNumber = -1
    val colors: TypedArray = mContext.resources.obtainTypedArray(R.array.colors)

    override fun getCount(): Int {
        return menuItems.size
    }

    override fun getItem(position: Int): Any? {
        return menuItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        return convertView ?: inflater.inflate(R.layout.menu_item, parent, false).apply {
            val menuItem = menuItems[position]

            tag = menuItem

            when (menuItem) {
                is Category -> {
                    name.setTypeface(null, BOLD)
                    name.text = menuItem.name
                    colorNumber++
                }
                is MenuItem -> {
                    name.text = menuItem.code
                    name.setTypeface(null, Typeface.NORMAL)
                }
            }

            if (menuItem is Unit) {
                name.setBackgroundColor(Color.TRANSPARENT)
            } else {
                name.setBackgroundColor(colors.getColor(colorNumber, 0))
            }
        }
    }

    fun setMenu(menuList: List<MenuItem>) {
        menuItems = setTheItems(menuList)
        notifyDataSetChanged()
        colorNumber = -1
    }
}