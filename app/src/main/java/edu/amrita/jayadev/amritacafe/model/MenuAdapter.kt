package edu.amrita.jayadev.amritacafe.model


import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.menu.MenuItem
import kotlinx.android.synthetic.main.menu_item.view.*

class MenuAdapter(private val mContext: Context, var menuItems: List<MenuItem>) :
    BaseAdapter() {

    var colorNumber = -1
    var currentCategory = ""
    val colors: TypedArray = mContext.resources.obtainTypedArray(R.array.colors)

    override fun getCount(): Int {
        return menuItems.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        return convertView ?: inflater.inflate(R.layout.menu_item, parent, false).apply {

            name.text = menuItems[position].code
            if (menuItems[position].price == 0)
                name.setTypeface(null, Typeface.BOLD)
            else
                name.setTypeface(null, Typeface.NORMAL)

            if (menuItems[position].category.name != currentCategory) {
                currentCategory = menuItems[position].category.name
                colorNumber++
            }
            if (menuItems[position].name == "")
                name.setBackgroundColor(Color.TRANSPARENT)
            else
                name.setBackgroundColor(colors.getColor(colorNumber, 0))
        }
    }

    fun setMenu(menuList: List<MenuItem>) {
        menuItems = menuList
        notifyDataSetChanged()
        colorNumber = -1
    }
}