# Amrita Cafe — Build 80113

### 📊 Direct Google Sheets Sync for All Modes
- **Universal Order Syncing**: Orders completed in both **Order Taker** mode and **Cashier** mode now automatically push to Google Sheets.
- **Auto-Crash Uploading**: The app now silently transmits pending offline crash logs directly to a dedicated `"Crash Logs"` tab on Google Sheets as soon as internet connectivity is available.

### 🖨️ Robust Printer Connection Indicator
- **Active Printer Monitoring**: The printer indicator dot dynamically tests configured Wi-Fi printers (via instant TCP socket verification) and Bluetooth printers (paired & adapter state).
- **Interactive Printer Status**: Tapping the printer indicator on the main screen tests all configured printers (Kitchen & Receipt) and displays their real-time connection status in a prompt.
- **Live Print Feedback**: Indicator automatically updates to green when print jobs complete successfully.

### 📜 Compact & Dismissable History Dialog
- **Compact Tablet Layout**: Refined history dialog width on landscape tablets to 70% (capped at 720dp) for a clean, centered appearance.
- **Outside Touch Dismissal**: Clicking anywhere outside the history dialog now immediately cancels and closes it.
