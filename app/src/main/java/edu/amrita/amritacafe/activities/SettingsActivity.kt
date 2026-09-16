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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.net.InetSocketAddress
import edu.amrita.amritacafe.printer.escpos.EscPosBuilder

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
            rawSocketCheckbox.isChecked = configuration.useRawSocket
            rawSocketCheckbox.setOnCheckedChangeListener { _, isChecked ->
                configuration.useRawSocket = isChecked
            }

            testingCheckBox.isChecked = configuration.testing
            testingCheckBox.setOnCheckedChangeListener { _, isChecked ->
                configuration.testing = isChecked
            }

            rangeFromET.setText(configuration.rangeFrom.toString())
            rangeToET.setText(configuration.rangeTo.toString())
            columnNumbersET.setText(configuration.columns.toString())

            betaUpdatesCheckBox.isChecked = configuration.betaUpdates
            betaUpdatesCheckBox.setOnCheckedChangeListener { _, isChecked ->
                configuration.betaUpdates = isChecked
            }

            printAmmaQuoteCheckBox.isChecked = configuration.printAmmaQuote
            printAmmaQuoteCheckBox.setOnCheckedChangeListener { _, isChecked ->
                configuration.printAmmaQuote = isChecked
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

            pairButton.setOnClickListener {
                showBluetoothDeviceSelector()
            }

            testBluetoothButton.setOnClickListener {
                testBluetoothPrinterConnection(testBluetoothButton)
            }

            // Destination Spinners
            updateReceiptSpinner()
            updateKitchenSpinner()

            // Workflow Mode Spinner setup
            val modeOptions = listOf(
                "Order Mode (Table/Counter)",
                "Cashier Mode (Payments & Change)"
            )
            val modeAdapter = ArrayAdapter(
                this@SettingsActivity,
                R.layout.spinner_item,
                modeOptions
            )
            modeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            modeSpinner.adapter = modeAdapter
            modeSpinner.setSelection(configuration.workflowMode)
            updateKitchenVisibility(configuration.workflowMode)
            modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    configuration.workflowMode = position
                    configuration.mode = position
                    updateKitchenVisibility(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            testWifi1Button.setOnClickListener {
                val ip = receiptIpET.text.toString().trim()
                testPrinterConnection(ip, testWifi1Button, "Wi-Fi / LAN 1")
            }

            testWifi2Button.setOnClickListener {
                val ip = kitchenIpET.text.toString().trim()
                testPrinterConnection(ip, testWifi2Button, "Wi-Fi / LAN 2")
            }
        }
    }

    private fun updateReceiptSpinner() {
        val btLabel = if (configuration.bluetoothName.isNotEmpty()) {
            "Bluetooth (${configuration.bluetoothName})"
        } else {
            "Bluetooth"
        }
        val receiptOptions = listOf(
            "Wi-Fi / LAN 1",
            "Wi-Fi / LAN 2",
            btLabel,
            "None (Disabled)"
        )
        val receiptAdapter = ArrayAdapter(
            this@SettingsActivity,
            R.layout.spinner_item,
            receiptOptions
        )
        receiptAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.receiptDestinationSpinner.adapter = receiptAdapter
        binding.receiptDestinationSpinner.setSelection(configuration.receiptPrinterTarget)
        binding.receiptDestinationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                configuration.receiptPrinterTarget = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateKitchenSpinner() {
        val btLabel = if (configuration.bluetoothName.isNotEmpty()) {
            "Bluetooth (${configuration.bluetoothName})"
        } else {
            "Bluetooth"
        }
        val kitchenOptions = listOf(
            "Wi-Fi / LAN 2",
            "Wi-Fi / LAN 1",
            btLabel,
            "None (Disabled)"
        )
        val kitchenAdapter = ArrayAdapter(
            this@SettingsActivity,
            R.layout.spinner_item,
            kitchenOptions
        )
        kitchenAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.kitchenDestinationSpinner.adapter = kitchenAdapter
        binding.kitchenDestinationSpinner.setSelection(configuration.kitchenPrinterTarget)
        binding.kitchenDestinationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                configuration.kitchenPrinterTarget = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateKitchenVisibility(mode: Int) {
        val isOrderMode = (mode == Configuration.MODE_ORDER_TAKER)
        binding.kitchenPrinterLabel.visibility = if (isOrderMode) View.VISIBLE else View.GONE
        binding.kitchenDestinationSpinner.visibility = if (isOrderMode) View.VISIBLE else View.GONE
    }

    private fun testPrinterConnection(ipAddress: String, button: android.widget.Button, printerLabel: String) {
        val cleanIp = ipAddress.removePrefix("TCP:").trim()
        val parts = cleanIp.split(":")
        val host = parts[0].trim()
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 9100 else 9100

        if (host.isEmpty()) {
            Toast.makeText(this, "Please enter an IP address first", Toast.LENGTH_SHORT).show()
            return
        }

        button.isEnabled = false
        button.text = "Testing..."

        lifecycleScope.launch(Dispatchers.IO) {
            var success = false
            var errorMsg = ""
            val startTime = System.currentTimeMillis()

            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 2500)
                    success = socket.isConnected
                }
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: e.message ?: "Connection timed out"
            }

            val elapsed = System.currentTimeMillis() - startTime

            withContext(Dispatchers.Main) {
                button.isEnabled = true
                button.text = "Test"

                if (success) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("$printerLabel Online! \u2705")
                        .setMessage("Successfully connected to $host:$port in ${elapsed}ms.\n\nWould you like to print a test ticket?")
                        .setPositiveButton("Print Test") { _, _ ->
                            printTestTicket(host, port)
                        }
                        .setNegativeButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("$printerLabel Unreachable \u274C")
                        .setMessage("Could not connect to $host:$port.\n\nError: $errorMsg\n\nPlease check:\n1. Tablet is connected to router Wi-Fi\n2. Printer is turned ON\n3. IP address matches printer self-test sheet")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun printTestTicket(host: String, port: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val data = EscPosBuilder()
                    .alignCenter()
                    .textSize(2, 2)
                    .bold(true)
                    .line("AMRITA CAFE")
                    .textSize(1, 1)
                    .bold(false)
                    .line("Printer Test Successful")
                    .line("IP: $host:$port")
                    .horizontalLine('-', 42)
                    .cut(1)
                    .build()

                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 3000)
                    socket.getOutputStream().use { out ->
                        out.write(data)
                        out.flush()
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Test ticket printed!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Print failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun testBluetoothPrinterConnection(button: android.widget.Button) {
        val address = configuration.bluetoothAddress
        val name = configuration.bluetoothName.ifEmpty { "Bluetooth Printer" }

        if (address.isEmpty()) {
            Toast.makeText(this, "Please select a Bluetooth printer first", Toast.LENGTH_SHORT).show()
            return
        }

        button.isEnabled = false
        button.text = "Testing..."

        lifecycleScope.launch(Dispatchers.IO) {
            var success = false
            var errorMsg = ""
            val startTime = System.currentTimeMillis()

            try {
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                if (adapter == null || !adapter.isEnabled) {
                    throw Exception("Bluetooth is disabled on tablet")
                }
                val device = adapter.getRemoteDevice(address)
                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                adapter.cancelDiscovery()
                socket.connect()
                success = socket.isConnected
                socket.close()
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: e.message ?: "Connection failed"
            }

            val elapsed = System.currentTimeMillis() - startTime

            withContext(Dispatchers.Main) {
                button.isEnabled = true
                button.text = "Test"

                if (success) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("$name Online! \u2705")
                        .setMessage("Successfully connected to $name ($address) in ${elapsed}ms.\n\nWould you like to print a test ticket?")
                        .setPositiveButton("Print Test") { _, _ ->
                            printBluetoothTestTicket(address, name)
                        }
                        .setNegativeButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("$name Unreachable \u274C")
                        .setMessage("Could not connect to $name ($address).\n\nError: $errorMsg\n\nPlease check:\n1. Printer is turned ON and nearby\n2. Bluetooth is paired in Android Settings\n3. No other device is currently connected to the printer")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun printBluetoothTestTicket(address: String, name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter ?: throw Exception("Bluetooth not available")
                val device = adapter.getRemoteDevice(address)
                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                adapter.cancelDiscovery()
                socket.connect()

                val data = EscPosBuilder()
                    .alignCenter()
                    .textSize(2, 2)
                    .bold(true)
                    .line("AMRITA CAFE")
                    .textSize(1, 1)
                    .bold(false)
                    .line("Bluetooth Test Successful")
                    .line(name)
                    .line(address)
                    .horizontalLine('-', 30)
                    .cut(1)
                    .build()

                socket.outputStream.write(data)
                socket.outputStream.flush()
                delay(500L)
                socket.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Test ticket printed!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Print failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
                updateReceiptSpinner()
                updateKitchenSpinner()
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
                updateReceiptSpinner()
                updateKitchenSpinner()
                
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
                useRawSocket = rawSocketCheckbox.isChecked
                rangeFrom = rangeFromET.text.toString().trim().toIntOrNull() ?: 1
                rangeTo = rangeToET.text.toString().trim().toIntOrNull() ?: 999
                columns = columnNumbersET.text.toString().trim().toIntOrNull() ?: 8
            }
        }
    }

    fun tryConnect(view: View?) {
        // Handled in MainActivity on mode switch
    }
}
