# Amrita Cafe — Build 80107

### 💾 Persistent Order History Across Tablet Rotation & App Restarts
- **Orientation Rotation Resilient**: Moved history data storage to static companion storage so rotating between portrait and landscape never clears or resets historical orders.
- **Disk Persistence**: Added `HistoryPersistence` that automatically serializes and restores historical orders and their print statuses to internal storage (`order_history.json`).

### 🎯 Clean Text-Based Print Status & No Overlapping Icons
- **Dialog & History Refinements**:
  - Removed duplicate icon overlays in the print dialog and history rows.
  - Simplified failure message to clean, bold **`FAILED ✗`** (print dialog) and **`NOT PRINTED ✗`** (history) in bright red (`#FF3B30`), and **`PRINTED ✓`** in green (`#4CAF50`).
  - Removed the bulky `(Out of Paper / Power Cut)` and `(Check Printer)` parentheticals so the text fits comfortably without squeezing adjacent controls.
  - Placed a clean spacer so `Retry` and `Re-print` buttons are neatly right-aligned and fully visible.

### 📐 Full-Width Dialog Sizing & Clean Vertical Mode Layout
- **Full Width with Dismiss Margin**:
  - Dialogs now expand horizontally to ~94–96% of the screen width.
  - History dialog height is constrained to a maximum of 85% of screen height so workers can always tap outside the dialog or press back to dismiss it.
  - In vertical (portrait) mode, order items (e.g. `1 Med Pza...................200`) now fit comfortably on a single line instead of wrapping onto two lines.
- **Uncluttered History Cards**:
  - Removed top badge tabs from the header of history cards, leaving room for Order Number, GPay/Renunciate toggles, and timestamp.
  - Kitchen and receipt print status and retry buttons are cleanly positioned in the dedicated print section at the bottom of each card.

---

# Previous Changes — Build 80106

### 🚨 Kitchen & Receipt Print Status
- Initial introduction of high-visibility print status indicators and one-tap retry in order dialog and history.

---

# Previous Changes — Build 80105

### 🧹 Complete Removal of Legacy Epson SDK & Native Libraries
- Deleted `ePOS2.jar` & `libepos2.so`, removed ABI filtering, and switched exclusively to raw TCP ESC/POS printing.
