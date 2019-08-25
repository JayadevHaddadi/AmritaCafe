package edu.amrita.jayadev.amritacafe.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.menu.Availability
import edu.amrita.jayadev.amritacafe.menu.Category
import edu.amrita.jayadev.amritacafe.menu.Location
import edu.amrita.jayadev.amritacafe.menu.MenuItem
import edu.amrita.jayadev.amritacafe.model.EditMenuItemListener
import kotlinx.android.synthetic.main.fragment_edit_menu_item.view.*

class EditMenuItemFragment(private val listener: EditMenuItemListener,
                           menuItem: MenuItem = MenuItem.default.copy(),
                           val position: Int?
                           ) : DialogFragment() {

    private var menuItem = menuItem.copy()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_edit_menu_item, container, false).apply {

        editText_Code.setText(menuItem.code)
        editText_Name.setText(menuItem.name)
        editText_Price.setText(menuItem.price)
        spinner_Availability.setSelection(menuItem.availability.ordinal)
        spinner_Category.setSelection(menuItem.category.ordinal)
        spinner_Location.setSelection(menuItem.location.ordinal)

        editText_Name.setOnClickListener {
            menuItem = menuItem.copy(name = (it as TextView).text.toString())
        }
        editText_Code.setOnClickListener {
            menuItem = menuItem.copy(code = (it as TextView).text.toString())
        }
        editText_Price.setOnClickListener {
            menuItem = menuItem.copy(price = (it as TextView).text.toString().toInt())
        }
        spinner_Availability.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                menuItem = menuItem.copy(availability = Availability.values()[position])
            }
        }
        spinner_Category.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                menuItem = menuItem.copy(category = Category.values()[position])
            }
        }
        spinner_Location.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                menuItem = menuItem.copy(location = Location.values()[position])
            }
        }
    }


    override fun onDismiss(dialog: DialogInterface) {
        listener.save(menuItem, position)
        super.onDismiss(dialog)
    }

}