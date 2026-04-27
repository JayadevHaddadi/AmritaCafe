package edu.amrita.amritacafe.activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import edu.amrita.amritacafe.databinding.ActivitySettingsBinding
import edu.amrita.amritacafe.settings.Configuration
import java.util.ArrayList
import java.util.HashSet
import java.util.Collections
import android.util.Log
import edu.amrita.amritacafe.R
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var pref: SharedPreferences
    private lateinit var configuration: Configuration
    private lateinit var binding: ActivitySettingsBinding
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val discoveredDevices = HashSet<BluetoothDevice>()
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    @Suppress("DEPRECATION")
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        Log.d("BT_SCAN", "Found: ${device.name ?: "Unknown"} (${device.address})")
                        discoveredDevices.add(device)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BT_SCAN", "Discovery Finished")
                }
            }
        }
    }

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

            pairButton.setOnClickListener {
                showBluetoothDeviceSelector()
            }

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

    @SuppressLint("MissingPermission")
    private fun showBluetoothDeviceSelector() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 123)
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Enable Bluetooth first", Toast.LENGTH_SHORT).show()
            return
        }

        val bondedDevices: Set<BluetoothDevice> = bluetoothAdapter!!.bondedDevices
        val initialList = ArrayList<BluetoothDevice>(bondedDevices)
        val initialStrings = initialList.map { "${it.name ?: "Unknown"}\n${it.address} (Paired)" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Bluetooth Printer")
            .setItems(initialStrings) { _, which ->
                val selectedDevice = initialList[which]
                binding.bluetoothET.setText(selectedDevice.name ?: "Unknown")
                configuration.bluetoothName = selectedDevice.name ?: "Unknown"
                configuration.bluetoothAddress = selectedDevice.address
            }
            .setNeutralButton("Scan for New Devices") { _, _ ->
                startDiscoveryFlow()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startDiscoveryFlow() {
        if (bluetoothAdapter == null) return

        // 1. Check Permissions
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
            return
        }

        // 2. Check if Enabled
        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Bluetooth is disabled. Please enable it.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. System Location check
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }

        if (!isLocationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // On Android 12+, we might not need GPS if using neverForLocation flag
                Log.d("BT_SCAN", "Location disabled, but continuing on API 31+")
            } else {
                Toast.makeText(this, "Please pull down the notification bar and ENABLE Location/GPS to scan.", Toast.LENGTH_LONG).show()
                return
            }
        }

        // 4. Start Scan
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }

        discoveredDevices.clear()
        discoveredDevices.addAll(bluetoothAdapter!!.bondedDevices)
        
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
        
        val success = bluetoothAdapter?.startDiscovery() ?: false
        if (!success) {
            Toast.makeText(this, "Failed to start scanning. Is Bluetooth on?", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Scanning for Printers...")
            .setMessage("Looking for nearby Bluetooth devices. This will take 10 seconds.")
            .setNegativeButton("Cancel") { _, _ ->
                bluetoothAdapter?.cancelDiscovery()
            }
            .create()

        progressDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                progressDialog.dismiss()
                bluetoothAdapter?.cancelDiscovery()
                try {
                    unregisterReceiver(receiver)
                } catch (e: Exception) {}
                showResultsDialog()
            }
        }, 10000)
    }

    @SuppressLint("MissingPermission")
    private fun showResultsDialog() {
        val deviceList = ArrayList<BluetoothDevice>(discoveredDevices)
        if (deviceList.size > 1) {
            Collections.sort(deviceList) { a: BluetoothDevice, b: BluetoothDevice ->
                val aBonded = if (a.bondState == BluetoothDevice.BOND_BONDED) 0 else 1
                val bBonded = if (b.bondState == BluetoothDevice.BOND_BONDED) 0 else 1
                aBonded - bBonded
            }
        }

        if (deviceList.isEmpty()) {
            Toast.makeText(this, "No devices found. Make sure the printer is in pairing mode.", Toast.LENGTH_LONG).show()
            return
        }

        val deviceStrings = deviceList
            .filter { it.name != null && it.name.isNotEmpty() }
            .map { 
                val status = when(it.bondState) {
                    BluetoothDevice.BOND_BONDED -> "(Paired)"
                    BluetoothDevice.BOND_BONDING -> "(Pairing...)"
                    else -> "(Unpaired)"
                }
                "${it.name}\n${it.address} $status"
            }.toTypedArray()

        if (deviceStrings.isEmpty()) {
            Toast.makeText(this, "No named devices found. Ensure the printer is on and nearby.", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Found Bluetooth Printers")
            .setItems(deviceStrings) { _, which ->
                // We need the original device from the filtered list
                val filteredList = deviceList.filter { it.name != null && it.name.isNotEmpty() }
                val selectedDevice = filteredList[which]
                
                if (selectedDevice.bondState == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(this, "Initiating pairing with ${selectedDevice.name}...", Toast.LENGTH_SHORT).show()
                    selectedDevice.createBond()
                }

                binding.bluetoothET.setText(selectedDevice.name ?: "Unknown")
                configuration.bluetoothName = selectedDevice.name ?: "Unknown"
                configuration.bluetoothAddress = selectedDevice.address
                
                Toast.makeText(this, "Saved: ${selectedDevice.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {}
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // val item = parent.getItemAtPosition(position).toString()
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
