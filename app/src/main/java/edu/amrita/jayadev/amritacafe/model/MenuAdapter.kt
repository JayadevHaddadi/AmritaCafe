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
import java.util.*
import kotlin.Comparator

class MenuAdapter(private val mContext: Context, menu: List<MenuItem>) :
    BaseAdapter() {

    init {
        setTheItems(menu)
    }

    private fun setTheItems(items: List<MenuItem>) {

        items.groupBy {
            it.category
        }.toSortedMap(
            Comparator { left, right ->
                left.ordinal.compareTo(right.ordinal) }
        ).let { menuByCategory ->
            menuItems = menuByCategory.map { (category, items) ->
                listOf(category) + items + Array((10 - (items.size + 1) % 10) % 10) {Unit}
            }.flatten()

            val colors: TypedArray = mContext.resources.obtainTypedArray(R.array.colors)
            colorMap = menuByCategory.keys.mapIndexed { idx, cat ->
                cat to colors.getColor(idx, 0)
            }.toMap()
            colors.recycle()
        }
    }

    private lateinit var menuItems : List<Any>
    private lateinit var colorMap : Map<Category, Int>

    override fun getCount(): Int {
        return menuItems.size
    }

    override fun getItem(position: Int): Any? {
        return menuItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View
    = convertOrInflate(convertView, parent).apply {

        val menuItem = menuItems[position]

        tag = menuItem

        when (menuItem) {
            is Category -> {
                name.setTypeface(null, BOLD)
                name.text = menuItem.name
                name.setBackgroundColor(colorMap.getValue(menuItem))
            }
            is MenuItem -> {
                name.text = menuItem.code
                name.setTypeface(null, Typeface.NORMAL)
                name.setBackgroundColor(colorMap.getValue(menuItem.category))
            }
            is Unit -> {
                name.setBackgroundColor(Color.TRANSPARENT)
                name.text = ""
            }

        }
    }

    private fun convertOrInflate(view: View?, parent: ViewGroup) =
        view ?: mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
        (it as LayoutInflater).inflate(R.layout.menu_item, parent, false)
    }

    fun setMenu(menuList: List<MenuItem>) {
        setTheItems(menuList)
        notifyDataSetChanged()
    }
}