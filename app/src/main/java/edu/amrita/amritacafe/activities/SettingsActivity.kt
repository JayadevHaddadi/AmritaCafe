package edu.amrita.amritacafe.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import edu.amrita.amritacafe.databinding.ActivitySettingsBinding
import edu.amrita.amritacafe.settings.Configuration

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

        with(binding) {
            // Hide the Menu Selection UI as it is now in MainActivity
            textViewLabel.visibility = View.GONE
            sheetSpinner.visibility = View.GONE
            refreshButton.visibility = View.GONE
            progressBar.visibility = View.GONE
            textViewError.visibility = View.GONE

            // Bind Views
            receiptIpET.setText(configuration.receiptPrinterIP)
            kitchenIpET.setText(configuration.kitchenPrinterIP)

            testingCheckBox.isChecked = configuration.testing
            testingCheckBox.setOnCheckedChangeListener { _, isChecked ->
                configuration.testing = isChecked
            }

            rangeFromET.setText(configuration.rangeFrom.toString())
            rangeToET.setText(configuration.rangeTo.toString())
            columnNumbersET.setText(configuration.columns.toString())

            wifiKeywordsET.setText(configuration.wifiKeywords)
            btKeywordsET.setText(configuration.bluetoothKeywords)

            betaUpdatesCheckBox.isChecked = configuration.betaUpdates
            betaUpdatesCheckBox.setOnCheckedChangeListener { _, isChecked ->
                configuration.betaUpdates = isChecked
            }

            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
                val vName = packageInfo.versionName
                versionInfoTV.text = "Current Version: $vName ($vCode)"
            } catch (e: Exception) {
                versionInfoTV.text = "Version: Unknown"
            }

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
        Toast.makeText(parent.context, "Selected: $item", Toast.LENGTH_SHORT).show()
        configuration.mode = position
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onPause() {
        super.onPause()
        with(binding) {
            configuration.apply {
                kitchenPrinterIP = kitchenIpET.text.toString().trim()
                receiptPrinterIP = receiptIpET.text.toString().trim()
                rangeFrom = rangeFromET.text.toString().trim().toIntOrNull() ?: 1
                rangeTo = rangeToET.text.toString().trim().toIntOrNull() ?: 999
                columns = columnNumbersET.text.toString().trim().toIntOrNull() ?: 8
                wifiKeywords = wifiKeywordsET.text.toString().trim()
                bluetoothKeywords = btKeywordsET.text.toString().trim()
            }
        }
    }

    fun tryConnect(view: View?) {
        // Handled in MainActivity on mode switch
    }
}
