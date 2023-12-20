package edu.amrita.amritacafe.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.activities.MainActivity.Companion.BREAKFAST_FILE
import edu.amrita.amritacafe.activities.MainActivity.Companion.LUNCH_DINNER_FILE
import edu.amrita.amritacafe.menu.DEFAULT_BREAKFAST_CSV
import edu.amrita.amritacafe.menu.DEFAULT_BREAKFAST_MENU
import edu.amrita.amritacafe.menu.DEFAULT_LUNCH_CSV
import edu.amrita.amritacafe.menu.DEFAULT_LUNCH_DINNER_MENU
import edu.amrita.amritacafe.menu.createMenuFileFromMenuList
import edu.amrita.amritacafe.menu.overrideFile
import edu.amrita.amritacafe.menu.saveIfValidText
import edu.amrita.amritacafe.settings.Configuration
import edu.amrita.amritacafe.settings.Configuration.Companion.COLUMN_NUMBER_RANGE
import kotlinx.android.synthetic.main.activity_settings.bluetooth_ET
import kotlinx.android.synthetic.main.activity_settings.column_numbers_ET
import kotlinx.android.synthetic.main.activity_settings.kitchen_ip_ET
import kotlinx.android.synthetic.main.activity_settings.menu_ET
import kotlinx.android.synthetic.main.activity_settings.menu_toggle_button
import kotlinx.android.synthetic.main.activity_settings.mode_spinner
import kotlinx.android.synthetic.main.activity_settings.range_from_ET
import kotlinx.android.synthetic.main.activity_settings.range_to_ET
import kotlinx.android.synthetic.main.activity_settings.receipt_ip_ET
import kotlinx.android.synthetic.main.activity_settings.testingCheckBox
import java.io.BufferedReader
import java.io.FileReader


class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var pref: SharedPreferences
    private lateinit var configuration: Configuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.hide()

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.let { preferences ->
            configuration = Configuration(preferences)
        }

        receipt_ip_ET.setText(configuration.receiptPrinterIP)
        kitchen_ip_ET.setText(configuration.kitchenPrinterIP)

        val column = pref.getString(COLUMN_NUMBER_RANGE, "10")
        column_numbers_ET.setText(column)

        menu_toggle_button.isChecked = configuration.isBreakfastTime
        loadCurrentMenu()

        menu_toggle_button.setOnCheckedChangeListener { compoundButton, isBreakfastTime ->
            configuration.isBreakfastTime = isBreakfastTime
            loadCurrentMenu()
        }

        testingCheckBox.isChecked = configuration.testing
        testingCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            configuration.testing = isChecked
        }

        range_from_ET.setText(configuration.rangeFrom.toString())
        range_to_ET.setText(configuration.rangeTo.toString())
        column_numbers_ET.setText(configuration.columns.toString())

        bluetooth_ET.setText(configuration.bluetoothName)
        bluetooth_ET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                configuration.bluetoothName = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        val spinner: Spinner = mode_spinner
        spinner.setOnItemSelectedListener(this)
        val categories: MutableList<String> = ArrayList()
        categories.add("Wifi")
        categories.add("Bluetooth")
        val dataAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = dataAdapter
        spinner.setSelection(configuration.mode)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // On selecting a spinner item
        val item = parent.getItemAtPosition(position).toString()

        // Showing selected spinner item
        Toast.makeText(parent.context, "Selected: $item", Toast.LENGTH_LONG).show()
        configuration.mode = position
    }

    override fun onNothingSelected(arg0: AdapterView<*>?) {
        // TODO Auto-generated method stub
    }

    fun loadCurrentMenu() {
        println("Loading is breakfast: ${configuration.isBreakfastTime}")
        val file = if (configuration.isBreakfastTime) BREAKFAST_FILE else LUNCH_DINNER_FILE
        val br = BufferedReader(FileReader(file))
        menu_ET.setText(br.readText())
    }

    override fun onPause() {
        super.onPause()
        configuration.kitchenPrinterIP = kitchen_ip_ET.text.toString()
        configuration.receiptPrinterIP = receipt_ip_ET.text.toString()
        configuration.rangeFrom = range_from_ET.text.toString().toInt()
        configuration.rangeTo = range_to_ET.text.toString().toInt()
        configuration.columns = column_numbers_ET.text.toString().toInt()
    }

    fun saveSettings(view: View) {
        val file = if (configuration.isBreakfastTime) BREAKFAST_FILE else LUNCH_DINNER_FILE
        val response = saveIfValidText(menu_ET.text.toString(), applicationContext, file)

        Toast.makeText(
            applicationContext,
            response,
            Toast.LENGTH_LONG
        ).show()
    }

    fun resetMenu(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset current menu?")

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            if (configuration.isBreakfastTime)
                overrideFile(DEFAULT_BREAKFAST_CSV,BREAKFAST_FILE)
//                createMenuFileFromMenuList(BREAKFAST_FILE, DEFAULT_BREAKFAST_MENU)
            else
                overrideFile(DEFAULT_LUNCH_CSV,LUNCH_DINNER_FILE)
//                createMenuFileFromMenuList(LUNCH_DINNER_FILE, DEFAULT_LUNCH_DINNER_MENU)

            loadCurrentMenu()
            Toast.makeText(
                applicationContext,
                "Current menu reset to default", Toast.LENGTH_SHORT
            ).show()
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which ->
//            Toast.makeText(applicationContext,
//                android.R.string.no, Toast.LENGTH_SHORT).show()
        }

        builder.show()
    }

    fun tryConnect(view: View) {
    }
}