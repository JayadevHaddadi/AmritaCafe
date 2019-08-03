package edu.amrita.jayadev.amritacafe


import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class MenuAdapter(private val mContext: Context, private val menuItems: MutableList<SettingsRetriver.MenuItem>) : BaseAdapter() {

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

        val gridView: View

        if (convertView == null) {
            gridView = inflater.inflate(R.layout.menu_item, null)

            val nameView = gridView
                .findViewById<View>(R.id.grid_item_letter) as TextView

            nameView.text = menuItems[position].name

            if(!menuItems[position].category.equals(currentCategory)) {
                currentCategory = menuItems[position].category
                colorNumber++
            }
            if(menuItems[position].name.equals(""))
                nameView.setBackgroundColor(Color.WHITE)
            else
                nameView.setBackgroundColor(colors.getColor(colorNumber,0))

        } else {
            gridView = convertView
        }
        return gridView
    }
}