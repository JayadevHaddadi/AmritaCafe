# Amrita Cafe — Build 80103

### 🏷️ Branding & Package
- **App Name**: Renamed to **"Amrita Cafe"** (removed outdated "v5" suffix).
- **Application ID**: Updated to `edu.amrita.amritacafe` (removed trailing `3`).
- **Icon Refresh**: Foreground coffee mug restyled to vibrant DeepPink (`#FF1493`) on brown background (`#5D4037`).

### ⚙️ Independent & Compact Printer Settings
- **Separate Controls for Kitchen & Receipt**:
  - Independent **Large Text** and **Small Text** scaling controls for receipt and kitchen printers.
  - Independent **Feed Before** and **Feed After** margin controls for receipt and kitchen printers.
  - Dedicated **Test Receipt Print** and **Test Kitchen Print** buttons right under their respective sections.
- **Space-Saving Compact UI**:
  - Both text size controls share the same row (`Large` and `Small`).
  - Both margin controls share the same row (`Before` and `After`).
  - Optimized for both portrait and landscape tablet orientations.
- **Printer Writers**: Updated `ReceiptWriter`, `CashierReceiptWriter`, and `KitchenWriter` to respect per-printer scaling and margins.

### ⚡ Simultaneous Printing (Concurrent Dispatch)
- Bluetooth print commands for kitchen and receipt are now dispatched simultaneously using asynchronous coroutines (`Dispatchers.IO`).
- Prevents UI freezing and sends print data concurrently to both printers without waiting for each other.

### 📱 UI & Layout Fixes
- **Tablet Name Display**: Fixed an issue where the tablet name was squeezed into a vertical 1-character-wide column. The name now appears cleanly directly underneath the "Amrita Cafe" title without wrapping.
- **AMMA Easter Egg**: Fixed dismissal behavior — removed the double-tap close handler. Tapping or holding on the photo streams hearts continuously, and it now only closes when tapping outside the picture or on the close button.

### ☁️ Cloud & Google Sheets
- **Order Upload Fix**: Restored missing `items` array payload in Google Sheets upload.
- **Dynamic Script URL**: Added dynamic URL support with fallback via `BuildConfig.ORDER_SCRIPT_URL`.
- **App Version Tracking**: Added dynamic `appVersion` (`versionCode`) reporting to the "APP VERSION" column in Google Sheets via `OrderEntry.gs`.
