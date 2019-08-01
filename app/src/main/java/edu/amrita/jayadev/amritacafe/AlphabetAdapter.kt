package edu.amrita.jayadev.amritacafe


import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class AlphabetAdapter(private val mContext: Context, private val mLetters: MutableList<SettingsRetriver.MenuItem>) : BaseAdapter() {

    override fun getCount(): Int {
        return mLetters.size
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
            // get layout from xml file
            gridView = inflater.inflate(R.layout.alphabet_grid_item, null)

            // pull views
            val letterView = gridView
                .findViewById<View>(R.id.grid_item_letter) as TextView

            // set values into views
            letterView.text = mLetters[position].name  // using dummy data for now
            if(mLetters[position].equals("Burgers")) {
                letterView.setBackgroundColor(Color.RED)
                letterView.setTextSize(25f)
            }
            else
                letterView.setBackgroundColor(Color.YELLOW)

        } else {
            gridView = convertView
        }
        return gridView
    }
}