package edu.amrita.amritacafe.model


import android.content.Context

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface.*
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.Category
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.android.synthetic.main.item_menu.view.*
import kotlin.Comparator

class MenuAdapter(
    private val configuration: Configuration,
    private val context: Context,
    private val onChanged: () -> Unit
) :
    BaseAdapter() {

    private var menuItemDisplayNameHandler: (MenuItem) -> String = { "" }
    private var showItemNames: Boolean = configuration.showMenuItemNames
        set(value) {
            field = value
            menuItemDisplayNameHandler =
                if (value) {
                    { it.name }
                } else {
                    { it.code }
                }
        }

    init {
        setTheItems(configuration.currentMenu)
        showItemNames = configuration.showMenuItemNames

        configuration.registerMenuChangedListener {
            println("I swear I can change.")
            showItemNames = configuration.showMenuItemNames
            setTheItems(configuration.currentMenu)
            onChanged()
        }
    }

    private fun setTheItems(items: List<MenuItem>) {
        items.groupBy {
            it.category
        }.toSortedMap(
            Comparator { left, right ->
                left.ordinal.compareTo(right.ordinal)
            }
        ).let { menuByCategory ->
            val colors: TypedArray = context.resources.obtainTypedArray(R.array.colors)
            colorMap = menuByCategory.keys.mapIndexed { idx, cat ->
                cat to colors.getColor(idx + 3, 0)
            }.toMap()

            colors.recycle()
            menuItems = menuByCategory.map { (category, items) ->
                listOf(category) + items.sortedBy(menuItemDisplayNameHandler) + Array((10 - (items.size + 1) % 10) % 10) { Unit }
            }.flatten()
        }
    }

    private lateinit var menuItems: List<Any>
    private lateinit var colorMap: Map<Category, Int>

    override fun getCount(): Int {
        return menuItems.size
    }

    override fun getItem(position: Int): Any? {
        return menuItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        convertOrInflate(convertView, parent).apply {

            val menuItem = menuItems[position]

            tag = menuItem

            when (menuItem) {
                is Category -> {
                    name.setTypeface(SANS_SERIF, BOLD)
                    name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                    name.text = menuItem.displayName
                    name.setBackgroundColor(colorMap.getValue(menuItem))
                }
                is MenuItem -> {
                    name.text = menuItemDisplayNameHandler(menuItem)
                    name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)

                    name.setTypeface(SERIF, NORMAL)
                    name.setBackgroundColor(colorMap.getValue(menuItem.category))
                }
                is Unit -> {
                    name.setBackgroundColor(Color.TRANSPARENT)
                    name.text = ""
                }

            }
        }

    private fun convertOrInflate(view: View?, parent: ViewGroup) =
        view ?: context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            (it as LayoutInflater).inflate(R.layout.item_menu, parent, false)
        }
}