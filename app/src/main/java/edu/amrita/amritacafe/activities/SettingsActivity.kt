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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import edu.amrita.amritacafe.BuildConfig
import edu.amrita.amritacafe.IO.overrideFile
import edu.amrita.amritacafe.databinding.ActivitySettingsBinding
import edu.amrita.amritacafe.settings.Configuration
import edu.amrita.amritacafe.settings.Configuration.Companion.COLUMN_NUMBER_RANGE
import java.io.BufferedReader
import java.io.FileReader
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
                android.R.layout.simple_spinner_item,
                mutableListOf<String>()
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
            tabletNameET.setText(configuration.tabletName)

            val column = pref.getString(COLUMN_NUMBER_RANGE, "10")
            columnNumbersET.setText(column)

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

            loadCurrentMenu()
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
                    if (isUsingLocalFallback) {
                        loadLocalSheetContent(selectedSheetName)
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Fetching content for: $selectedSheetName",
                            Toast.LENGTH_SHORT
                        ).show()
                        fetchSheetContent(selectedSheetName)
                    }
                    textViewError.visibility = View.GONE // Hide previous errors
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            binding.menuET.setText("")
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
                    if (showFeedback) Toast.makeText(this, "Update failed: Invalid response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Get Menu", "Volley error in background fetch", error)
                if (showFeedback) Toast.makeText(this, "Update failed: Network error", Toast.LENGTH_SHORT).show()
            })
        stringRequest.setRetryPolicy(
            DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )
        requestQueue.add(stringRequest)
    }

    // --- New Function to Fetch Content of a Specific Sheet ---
    private fun fetchSheetContent(sheetName: String) {
        showLoading(true) // Show progress bar while fetching content
        textViewError.visibility = View.GONE // Hide previous errors

        // URL Encode the sheet name to handle spaces, etc.
        val encodedSheetName = try {
            URLEncoder.encode(sheetName, "UTF-8")
        } catch (e: Exception) {
            showError("Error: Could not encode sheet name.")
            showLoading(false)
            return // Stop if encoding fails
        }

        // Construct the URL with the sheetName parameter
        val url = "$APPS_SCRIPT_URL?sheetName=$encodedSheetName"
        Log.d("Get Menu", "Fetching content from URL: $url") // Use Log.d ideally

        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET,
            url,
            { response -> // Success listener
                Log.d("Get Menu", "Raw Content Response: $response") // Use Log.d ideally
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "error")

                    if (status == "success") {
                        val csvData = jsonResponse.optString("data", "") // Get the CSV data string
//                        val processedCsv = processCsv(csvData)
                        val editor = pref.edit()
                        editor.putString("selected_menu_name", sheetName)
                        editor.apply()

                        binding.menuET.setText(csvData)
//                        binding.menuET.setText(csvData) // Set the text in the EditText
                        // binding.menuET.visibility = View.VISIBLE // Show the EditText

                        val fileName = "$sheetName.csv"

                        // Save the CSV content using the new FileIO system.
                        val saved = edu.amrita.amritacafe.IO.CSVFileManager.saveCSV(applicationContext, fileName, csvData)
                        if (saved) {
                            Log.d("Get Menu", "CSV saved successfully as $fileName")
                        } else {
                            Log.d("Get Menu", "Failed to save CSV as $fileName")
                        }
                        Log.d("Get Menu", "Successfully got content for $sheetName") // Use Log.d
                    } else {
                        // Handle error status from the script (e.g., sheet not found)
                        val message = jsonResponse.optString(
                            "message",
                            "Unknown script error fetching content"
                        )
                        showError("Script Error: $message")
                        // binding.menuET.visibility = View.GONE // Hide EditText on error
                    }
                } catch (e: JSONException) {
                    Log.d(
                        "Get Menu",
                        "BAD: JSON Parsing Error (Content): ${e.message}"
                    ) // Use Log.e
                    showError("Error: Could not parse content response from server.")
                    // binding.menuET.visibility = View.GONE
                } finally {
                    showLoading(false) // Hide loading indicator
                }
            },
            { error -> // Error listener (Volley network errors)
                Log.d("Get Menu", "BAD: Volley Error (Content): ${error.toString()}") // Use Log.e
                if (error.networkResponse == null) {
                    if (error.cause is UnknownHostException) {
                        showError("Network Error: Cannot reach host. Check internet connection.")
                    } else {
                        showError("Network Error: No connection or timeout.")
                    }
                } else {
                    showError("Network Error: ${error.message} (Code: ${error.networkResponse.statusCode})")
                }
                showLoading(false) // Hide loading indicator
                // binding.menuET.visibility = View.GONE
            }
        )

        // Set Retry Policy
        stringRequest.setRetryPolicy(
            DefaultRetryPolicy(
                10000, // Increase timeout slightly for potentially larger data? (10 seconds)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )

        // Add request to queue
        requestQueue.add(stringRequest)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}


    private fun loadCurrentMenu() {
        // Check if a preferred menu was saved in SharedPreferences
        val selectedMenuName = pref.getString("selected_menu_name", null)
        if (selectedMenuName != null) {
            // Construct the file name based on the saved menu name.
            val fileName = "$selectedMenuName.csv"
            // Read the CSV content from local storage using CSVFileManager.
            val csvContent = edu.amrita.amritacafe.IO.CSVFileManager.readCSV(applicationContext, fileName)
            if (csvContent != null) {
                // Set the CSV content into the EditText without changing its visibility.
                binding.menuET.setText(csvContent)
                Log.d("Get Menu", "Loaded CSV content from file: $fileName")
            } else {
                Log.d("Get Menu", "No CSV file found for: $fileName")
            }
        } else {
            Log.d("Get Menu", "No preferred menu selected in preferences.")
        }
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

    private var isUsingLocalFallback = false

    private fun fallbackLocalSheetNames() {
        // List all files in internal storage ending with ".csv"
        Log.d("Get Menu", "Fallback to local files")
        val localFiles = applicationContext.filesDir.listFiles { file ->
            file.name.endsWith(".csv")
        }?.map { it.name.removeSuffix(".csv") } ?: emptyList()

        if (localFiles.isEmpty()) {
            updateSpinner(listOf("No sheets found"))
            sheetSpinner.isEnabled = false
        } else {
            val sheetNamesList = mutableListOf("Select a Sheet")
            sheetNamesList.addAll(localFiles)
            updateSpinner(sheetNamesList)
            sheetSpinner.visibility = View.VISIBLE
            isUsingLocalFallback = true  // Mark that we are using the fallback
            Log.d("Get Menu", "Using local fallback with files: $localFiles")
        }
    }

    private fun loadLocalSheetContent(sheetName: String) {
        // Save the selected name as a preference
        pref.edit().putString("selected_menu_name", sheetName).apply()
        val fileName = "$sheetName.csv"
        val csvData = edu.amrita.amritacafe.IO.CSVFileManager.readCSV(applicationContext, fileName)
        if (csvData != null) {
            binding.menuET.setText(csvData)
            Log.d("Get Menu", "Loaded local CSV content for $sheetName from file: $fileName")
        } else {
            showError("Local CSV file not found for: $sheetName")
        }
    }


    fun tryConnect(view: View) {
        // Connection logic here
    }
}
