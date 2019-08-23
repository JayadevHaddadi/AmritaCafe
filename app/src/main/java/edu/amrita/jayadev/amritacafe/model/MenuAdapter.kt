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

        val cell: View

        if (convertView == null)
            cell = inflater.inflate(R.layout.menu_item, null)
        else
            cell = convertView

        if (convertView?.name?.text != menuItems[position].name) {

            cell.name.text = menuItems[position].code
            if (menuItems[position].price == 0)
                cell.name.setTypeface(null, Typeface.BOLD)
            else
                cell.name.setTypeface(null, Typeface.NORMAL)

            if (!menuItems[position].category.equals(currentCategory)) {
                currentCategory = menuItems[position].category.name
                colorNumber++
            }
            if (menuItems[position].name.equals(""))
                cell.name.setBackgroundColor(Color.TRANSPARENT)
            else
                cell.name.setBackgroundColor(colors.getColor(colorNumber, 0))

        }

        return cell
    }

    fun setMenu(menuList: List<MenuItem>) {
        menuItems = menuList
        notifyDataSetChanged()
        colorNumber = -1
    }
}