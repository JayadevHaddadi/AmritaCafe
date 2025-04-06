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
import edu.amrita.amritacafe.IO.DEFAULT_BREAKFAST_CSV
import edu.amrita.amritacafe.IO.DEFAULT_LUNCH_CSV
import edu.amrita.amritacafe.IO.overrideFile
import edu.amrita.amritacafe.IO.saveIfValidText
import edu.amrita.amritacafe.activities.MainActivity.Companion.BREAKFAST_FILE
import edu.amrita.amritacafe.activities.MainActivity.Companion.LUNCH_DINNER_FILE
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
import java.net.UnknownHostException // Keep this if you want specific check

class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var pref: SharedPreferences
    private lateinit var configuration: Configuration
    private lateinit var binding: ActivitySettingsBinding


    private lateinit var sheetSpinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewError: TextView
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    // IMPORTANT: Replace this with YOUR deployed Apps Script Web App URL
    private val APPS_SCRIPT_URL =
        "https://script.google.com/macros/s/AKfycbyb4ey0BF43Vuk4g4r4SGs-2NP4HEvNF0kn-pPEhsYtODwXKyp4G7P-1_Zhlmd1LrEB/exec" // <--- ****** PASTE YOUR URL HERE ******


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        configuration = Configuration(pref)

        with(binding) {
            // *** FIX: Initialize views using binding ***
            this@SettingsActivity.sheetSpinner =
                sheetSpinner // Assigns the Spinner from the layout (binding.sheetSpinner) to your variable
            this@SettingsActivity.progressBar = progressBar   // Assigns the ProgressBar
            this@SettingsActivity.textViewError = textViewError // Assigns the TextView
            // ******************************************


            // Initialize the adapter (Use this@SettingsActivity for context clarity inside 'with')
            spinnerAdapter = ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_spinner_item,
                mutableListOf<String>()
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Now this line should work because sheetSpinner is initialized:
            this@SettingsActivity.sheetSpinner.adapter = spinnerAdapter

            // --- Set the ItemSelectedListener for the sheetSpinner ---
            this@SettingsActivity.sheetSpinner.onItemSelectedListener = sheetSpinnerListener
            // ---------------------------------------------------------

            // Set spinner visibility initially to invisible until data loads
            this@SettingsActivity.sheetSpinner.visibility = View.INVISIBLE

            // *** FIX: Correct URL Check Logic ***
            // Check if the URL IS the placeholder OR blank
            if (APPS_SCRIPT_URL == "YOUR_APPS_SCRIPT_WEB_APP_URL" || APPS_SCRIPT_URL.isBlank()) {
                showError("Apps Script URL is not set correctly in SettingsActivity.")
                this@SettingsActivity.progressBar.visibility =
                    View.GONE // Hide progress bar if URL is missing
            } else {
                Log.d("Get Menu", "CALLING FETCHING NAMES")
                fetchSheetNames()
            }
            // ************************************
            //GEMINI END


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
        }
    }

    private fun updateSpinner(sheetNames: List<String>) {
        Log.d(
            "Get Menu",
            "SettingsActivity: updateSpinner started with sheetNames: $sheetNames"
        ) // Added log
        spinnerAdapter.clear()
        if (sheetNames.isEmpty()) {
            spinnerAdapter.add("No sheets found")
            sheetSpinner.isEnabled = false
        } else {
            spinnerAdapter.addAll(sheetNames)
            sheetSpinner.isEnabled = true
        }
        spinnerAdapter.notifyDataSetChanged()
        Log.d(
            "Get Menu",
            "SettingsActivity: updateSpinner finished, spinnerAdapter count: ${spinnerAdapter.count}"
        ) // Added log
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
            if (parent != null && position > 0) { // Check parent is not null and avoid acting on the initial prompt ("Select a Sheet")
                val selectedSheetName = parent.getItemAtPosition(position) as String
                if (selectedSheetName != "No sheets found") { // Also avoid acting on error messages
                    Toast.makeText(
                        this@SettingsActivity,
                        "Fetching content for: $selectedSheetName",
                        Toast.LENGTH_SHORT
                    ).show()
                    fetchSheetContent(selectedSheetName) // Call the new function
                    binding.menuET.visibility = View.GONE // Hide old content while loading
                    binding.menuET.setText("") // Clear old content
                    textViewError.visibility = View.GONE // Hide previous errors
                }
            } else {
                // Item at position 0 ("Select a Sheet") or null parent, do nothing or hide EditText
                binding.menuET.visibility = View.GONE
                binding.menuET.setText("")
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // Optional: Handle case where nothing is selected
            binding.menuET.visibility = View.GONE
            binding.menuET.setText("")
        }
    }
    // --- End of Listener ---


    private fun fetchSheetNames() {
        Log.d("Get Menu", "FETCHING SHEETS!")
        Log.d("Get Menu", "SettingsActivity: fetchSheetNames started") // Added log
        showLoading(true)
        textViewError.visibility = View.GONE
        sheetSpinner.visibility = View.INVISIBLE // Keep spinner visible, just update content

        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET,
            APPS_SCRIPT_URL, // URL WITHOUT parameters fetches all names
            { response ->
                Log.d(
                    "Get Menu",
                    "SettingsActivity: fetchSheetNames success response: $response"
                ) // Added log
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "error")
                    if (status == "success") {
                        val sheetNamesArray = jsonResponse.optJSONArray("sheetNames")
                        if (sheetNamesArray != null) {
                            val sheetNamesList =
                                mutableListOf<String>("Select a Sheet") // Add prompt at the start
                            for (i in 0 until sheetNamesArray.length()) {
                                sheetNamesList.add(sheetNamesArray.getString(i))
                            }
                            Log.d(
                                "Get Menu",
                                "SettingsActivity: fetchSheetNames sheetNamesList: $sheetNamesList"
                            ) // Added log
                            updateSpinner(sheetNamesList)
                            sheetSpinner.visibility = View.VISIBLE // Ensure spinner is visible
                        } else {
                            Log.d(
                                "Get Menu",
                                "SettingsActivity: fetchSheetNames error: missing 'sheetNames'"
                            ) // Added log
                            showError("Script Error: Response format incorrect (missing 'sheetNames').")
                            updateSpinner(listOf("Select a Sheet", "Error loading"))
                        }
                    } else {
                        val message = jsonResponse.optString("message", "Unknown script error")
                        Log.d(
                            "Get Menu",
                            "SettingsActivity: fetchSheetNames error: script returned error - $message"
                        ) // Added log
                        showError("Script Error: $message")
                        updateSpinner(listOf("Select a Sheet", "Error loading"))
                    }
                } catch (e: JSONException) {
                    Log.d(
                        "Get Menu",
                        "SettingsActivity: fetchSheetNames error: JSON parsing - ${e.message}"
                    ) // Added log
                    showError("Error: Could not parse sheet list response.")
                    updateSpinner(listOf("Select a Sheet", "Error loading"))
                } finally {
                    showLoading(false)
                }
            },
            { error ->
                Log.d(
                    "Get Menu",
                    "SettingsActivity: fetchSheetNames error: Volley - ${error.message}"
                ) // Added log
                showError("Network Error fetching sheet list: ${error.message}")
                updateSpinner(listOf("Select a Sheet", "Network Error"))
                showLoading(false)
            })
        stringRequest.setRetryPolicy(
            DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        )
        requestQueue.add(stringRequest)
        Log.d("Get Menu", "SettingsActivity: fetchSheetNames finished") // Added log
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
                        val processedCsv = processCsv(csvData)
                        binding.menuET.setText(processedCsv)
//                        binding.menuET.setText(csvData) // Set the text in the EditText
                        binding.menuET.visibility = View.VISIBLE // Show the EditText
                        Log.d("Get Menu", "Successfully got content for $sheetName") // Use Log.d
                    } else {
                        // Handle error status from the script (e.g., sheet not found)
                        val message = jsonResponse.optString(
                            "message",
                            "Unknown script error fetching content"
                        )
                        showError("Script Error: $message")
                        binding.menuET.visibility = View.GONE // Hide EditText on error
                    }
                } catch (e: JSONException) {
                    Log.d(
                        "Get Menu",
                        "BAD: JSON Parsing Error (Content): ${e.message}"
                    ) // Use Log.e
                    showError("Error: Could not parse content response from server.")
                    binding.menuET.visibility = View.GONE
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
                binding.menuET.visibility = View.GONE
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
    // --- End of New Function ---
//    private fun processCsv(csv: String): String {
//        // Split into lines
//        val lines = csv.split("\n")
//        // Process each line: remove trailing commas.
//        val processedLines = lines.map { line ->
//            // Remove trailing commas. (If the line consists solely of commas, this returns an empty string.)
//            line.trimEnd(',')
//        }
//        return processedLines.joinToString("\n")
//    }
    private fun processCsv(csv: String): String {
        return csv.split("\n")
            .map { it.trimEnd(',') }
            .filter { it.isNotEmpty() }  // This line drops lines that become empty.
            .joinToString("\n")
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
        val sheetName = sheetSpinner.selectedItem?.toString()
        if (sheetName == null || sheetName == "Select a Sheet" || sheetName == "No sheets found") {
            Toast.makeText(applicationContext, "Please select a valid sheet name before saving.", Toast.LENGTH_LONG).show()
            return
        }
        val fileName = "$sheetName.csv"
        val response = saveIfValidText(binding.menuET.text.toString(), applicationContext, fileName)
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
