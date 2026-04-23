package edu.amrita.amritacafe.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import edu.amrita.amritacafe.BuildConfig
import edu.amrita.amritacafe.databinding.ActivitySettingsBinding
import edu.amrita.amritacafe.settings.Configuration
import edu.amrita.amritacafe.settings.Configuration.Companion.COLUMN_NUMBER_RANGE
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.net.UnknownHostException 
import edu.amrita.amritacafe.R
class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var pref: SharedPreferences
    private lateinit var configuration: Configuration
    private lateinit var binding: ActivitySettingsBinding


    private lateinit var sheetSpinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewError: TextView
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    // IMPORTANT: Replace this with YOUR deployed Apps Script Web App URL
    private val APPS_SCRIPT_URL = BuildConfig.MENU_SCRIPT_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        configuration = Configuration(pref)

        with(binding) {
            this@SettingsActivity.sheetSpinner = sheetSpinner 
            this@SettingsActivity.progressBar = progressBar   
            this@SettingsActivity.textViewError = textViewError 

            spinnerAdapter = ArrayAdapter(
                this@SettingsActivity,
                R.layout.spinner_item,
                mutableListOf<String>()
            )
            spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
            this@SettingsActivity.sheetSpinner.adapter = spinnerAdapter
            this@SettingsActivity.sheetSpinner.onItemSelectedListener = sheetSpinnerListener

            // --- SEAMLESS MENU LOADING ---
            val cachedNames = pref.getString("cached_sheet_names", "")
            if (!cachedNames.isNullOrBlank()) {
                val list = cachedNames.split(",").filter { it.isNotBlank() }
                updateSpinner(list)
            } else {
                updateSpinner(emptyList())
            }

            // Fetch fresh names in background
            Log.d("SettingsActivity", "Refreshing sheet names in background")
            fetchSheetNames()

            refreshButton.setOnClickListener {
                Toast.makeText(this@SettingsActivity, "Refreshing menu list...", Toast.LENGTH_SHORT).show()
                fetchSheetNames(true)
            }
            // -----------------------------

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

            bluetoothET.setText(configuration.bluetoothName)
            bluetoothET.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    configuration.bluetoothName = s.toString()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

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

    private fun updateSpinner(sheetNames: List<String>) {
        spinnerAdapter.clear()
        spinnerAdapter.add("Select a Sheet")
        if (sheetNames.isEmpty()) {
            spinnerAdapter.add("No sheets found")
            sheetSpinner.isEnabled = false
        } else {
            spinnerAdapter.addAll(sheetNames)
            sheetSpinner.isEnabled = true
        }
        spinnerAdapter.notifyDataSetChanged()
        
        // Cache the names for next time
        pref.edit().putString("cached_sheet_names", sheetNames.joinToString(",")).apply()

        // Restore selection if it exists in the list
        val currentSelection = pref.getString("selected_menu_name", null)
        if (currentSelection != null) {
            val index = sheetNames.indexOf(currentSelection)
            if (index != -1) {
                sheetSpinner.setSelection(index + 1)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        textViewError.text = message
        textViewError.visibility = View.VISIBLE
        sheetSpinner.visibility = View.INVISIBLE // Hide spinner on error
        Toast.makeText(this, message, Toast.LENGTH_LONG).show() // Optional Toast
        Log.d("Get Menu", "BAD: Displaying Error: $message")

    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val item = parent.getItemAtPosition(position).toString()
        Toast.makeText(parent.context, "Selected: $item", Toast.LENGTH_LONG).show()
        configuration.mode = position
    }

    // --- Listener for the Sheet Name Spinner ---
    private val sheetSpinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (parent != null && position > 0) { // Skip the initial prompt
                val selectedSheetName = parent.getItemAtPosition(position) as String
                if (selectedSheetName != "No sheets found") {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Fetching content for: $selectedSheetName",
                        Toast.LENGTH_SHORT
                    ).show()
                    fetchSheetContent(selectedSheetName)
                    textViewError.visibility = View.GONE // Hide previous errors
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    // --- End of Listener ---


    private fun fetchSheetNames(showFeedback: Boolean = false) {
        Log.d("Get Menu", "SettingsActivity: fetchSheetNames started (Background)")
        // Seamless background update: don't show loading or hide spinner
        textViewError.visibility = View.GONE

        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET,
            APPS_SCRIPT_URL,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "error")
                    if (status == "success") {
                        val sheetNamesArray = jsonResponse.optJSONArray("sheetNames")
                        if (sheetNamesArray != null) {
                            val sheetNamesList = mutableListOf<String>()
                            for (i in 0 until sheetNamesArray.length()) {
                                sheetNamesList.add(sheetNamesArray.getString(i))
                            }
                            updateSpinner(sheetNamesList)
                            
                            if (showFeedback) {
                                Toast.makeText(this, "Menu list updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("Get Menu", "JSON parsing error in background fetch", e)
                }
            },
            { error ->
                Log.e("Get Menu", "Volley error in background fetch", error)
            })
        stringRequest.setRetryPolicy(
            DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )
        requestQueue.add(stringRequest)
    }

    private fun fetchSheetContent(sheetName: String) {
        showLoading(true)
        textViewError.visibility = View.GONE

        val encodedSheetName = try {
            URLEncoder.encode(sheetName, "UTF-8")
        } catch (e: Exception) {
            showError("Error: Could not encode sheet name.")
            showLoading(false)
            return
        }

        val url = "$APPS_SCRIPT_URL?sheetName=$encodedSheetName"
        Log.d("Get Menu", "Fetching content from URL: $url")

        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET,
            url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "error")

                    if (status == "success") {
                        val csvData = jsonResponse.optString("data", "")
                        pref.edit().putString("selected_menu_name", sheetName).apply()

                        val fileName = "$sheetName.csv"
                        val saved = edu.amrita.amritacafe.IO.CSVFileManager.saveCSV(applicationContext, fileName, csvData)
                        if (saved) {
                            Log.d("Get Menu", "CSV saved successfully as $fileName")
                        }
                    } else {
                        val message = jsonResponse.optString("message", "Unknown script error")
                        showError("Script Error: $message")
                    }
                } catch (e: JSONException) {
                    showError("Error: Could not parse response.")
                } finally {
                    showLoading(false)
                }
            },
            { error ->
                showError("Network Error: ${error.message}")
                showLoading(false)
            }
        )

        stringRequest.setRetryPolicy(DefaultRetryPolicy(10000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
        requestQueue.add(stringRequest)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onPause() {
        super.onPause()
        with(binding) {
            configuration.apply {
                kitchenPrinterIP = kitchenIpET.text.toString()
                receiptPrinterIP = receiptIpET.text.toString()
                rangeFrom = rangeFromET.text.toString().toIntOrNull() ?: 1
                rangeTo = rangeToET.text.toString().toIntOrNull() ?: 999
                columns = columnNumbersET.text.toString().toIntOrNull() ?: 8
                wifiKeywords = wifiKeywordsET.text.toString()
                bluetoothKeywords = btKeywordsET.text.toString()
            }
        }
    }

    fun tryConnect(view: View?) {
        // Handled in MainActivity on mode switch
    }
}
