package edu.amrita.amritacafe.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import edu.amrita.amritacafe.IO.DEFAULT_BREAKFAST_CSV
import edu.amrita.amritacafe.IO.DEFAULT_LUNCH_CSV
import edu.amrita.amritacafe.IO.overrideFile
import edu.amrita.amritacafe.IO.saveIfValidText
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.activities.MainActivity.Companion.BREAKFAST_FILE
import edu.amrita.amritacafe.activities.MainActivity.Companion.LUNCH_DINNER_FILE
import edu.amrita.amritacafe.databinding.ActivitySettingsBinding
import edu.amrita.amritacafe.settings.Configuration
import edu.amrita.amritacafe.settings.Configuration.Companion.COLUMN_NUMBER_RANGE
import java.io.BufferedReader
import java.io.FileReader

class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var pref: SharedPreferences
    private lateinit var configuration: Configuration
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        configuration = Configuration(pref)

        // Bind Views
        with(binding) {
            receiptIpET.setText(configuration.receiptPrinterIP)
            kitchenIpET.setText(configuration.kitchenPrinterIP)
            tabletNameET.setText(configuration.tabletName)

            val column = pref.getString(COLUMN_NUMBER_RANGE, "10")
            columnNumbersET.setText(column)

            menuToggleButton.isChecked = configuration.isBreakfastTime
            loadCurrentMenu()

            menuToggleButton.setOnCheckedChangeListener { _, isBreakfastTime ->
                configuration.isBreakfastTime = isBreakfastTime
                loadCurrentMenu()
            }

            testingCheckBox.isChecked = configuration.testing
            testingCheckBox.setOnCheckedChangeListener { _, isChecked ->
                configuration.testing = isChecked
            }

            printToCSVCheckBox.isChecked = configuration.printToFile
            printToCSVCheckBox.setOnCheckedChangeListener { _, isChecked ->
                configuration.printToFile = isChecked
            }

            rangeFromET.setText(configuration.rangeFrom.toString())
            rangeToET.setText(configuration.rangeTo.toString())
            columnNumbersET.setText(configuration.columns.toString())

            bluetoothET.setText(configuration.bluetoothName)
            bluetoothET.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    configuration.bluetoothName = s.toString()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Spinner setup
            val spinner = modeSpinner
            spinner.onItemSelectedListener = this@SettingsActivity
            val categories = listOf("Wifi", "Bluetooth")
            val dataAdapter = ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_spinner_item,
                categories
            )
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = dataAdapter
            spinner.setSelection(configuration.mode)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val item = parent.getItemAtPosition(position).toString()
        Toast.makeText(parent.context, "Selected: $item", Toast.LENGTH_LONG).show()
        configuration.mode = position
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    private fun loadCurrentMenu() {
        println("Loading is breakfast: ${configuration.isBreakfastTime}")
        val file = if (configuration.isBreakfastTime) BREAKFAST_FILE else LUNCH_DINNER_FILE
        val br = BufferedReader(FileReader(file))
        binding.menuET.setText(br.readText())
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            configuration.apply {
                tabletName = tabletNameET.text.toString()
                kitchenPrinterIP = kitchenIpET.text.toString()
                receiptPrinterIP = receiptIpET.text.toString()
                rangeFrom = rangeFromET.text.toString().toInt()
                rangeTo = rangeToET.text.toString().toInt()
                columns = columnNumbersET.text.toString().toInt()
            }
        }
    }

    fun saveSettings(view: View) {
        val file = if (configuration.isBreakfastTime) BREAKFAST_FILE else LUNCH_DINNER_FILE
        val response = saveIfValidText(binding.menuET.text.toString(), applicationContext, file)
        Toast.makeText(applicationContext, response, Toast.LENGTH_LONG).show()
    }

    fun resetMenu(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset current menu?")
        builder.setPositiveButton(android.R.string.yes) { _, _ ->
            if (configuration.isBreakfastTime)
                overrideFile(DEFAULT_BREAKFAST_CSV, BREAKFAST_FILE)
            else
                overrideFile(DEFAULT_LUNCH_CSV, LUNCH_DINNER_FILE)

            loadCurrentMenu()
            Toast.makeText(
                applicationContext,
                "Current menu reset to default", Toast.LENGTH_SHORT
            ).show()
        }
        builder.setNegativeButton(android.R.string.no, null)
        builder.show()
    }

    fun tryConnect(view: View) {
        // Connection logic here
    }
}
