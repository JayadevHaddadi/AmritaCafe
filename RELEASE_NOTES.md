# Amrita Cafe — Build 80106

### 🚨 Prominent Kitchen & Receipt Print Status Indicators
- **High-Visibility Status in Order Dialog**:
  - Clear **green checkmark (`#4CAF50`)** and `"PRINTED ✓"` label on success.
  - High-visibility **bright red warning icon (`#FF3B30`)** and `"FAILED ✗ (Out of Paper / Power Cut)"` label when a printer is offline, out of paper, or disconnected.
  - One-tap `"Retry"` button right alongside the status message.
- **Instant Identification in Order History**:
  - Added prominent print status badges directly in the header of every order card next to the order number:
    - **`🍳 KITCHEN FAILED ✗`** in bold red if the kitchen ticket failed or was not printed.
    - **`🍳 KITCHEN ✓`** in green when successfully printed to the kitchen.
    - **`🧾 RECEIPT FAILED ✗`** in bold red if the receipt print failed.
    - **`🧾 RECEIPT ✓`** in green when receipt printed.
  - Tapping any failed badge directly triggers an immediate re-print without opening submenus.
  - Bottom print detail section now uses colored icons (`#4CAF50` green and `#FF3B30` red) and status text.

---

# Previous Changes — Build 80105

### 🧹 Complete Removal of Legacy Epson SDK & Native Libraries
- **Deleted `ePOS2.jar` & `libepos2.so`**: Completely purged all proprietary Epson SDK binaries and native `.so` files from the project.
- **Removed ABI Filtering Constraints**: Removed legacy 32-bit `ndk { abiFilters "armeabi", "x86" }`, allowing modern 64-bit Android tablets to run natively with full performance.
- **Decoupled Status & Exception Handlers**: Refactored `CompletedJobStatus`, `ErrorStatus`, `PrinterStatus`, `PrintStatusListener`, and `PrintService` to standard Java/Kotlin exceptions with zero vendor library coupling.
- **Cleaned Gradle & ProGuard**: Removed obsolete Epson ProGuard rules and dependency declarations.

### 🍳 Kitchen Printing Formatting
- **Clean Ticket Output**: Removed the horizontal separator line (`====`) above the kitchen items so tickets flow cleanly and compactly.

### 🌿 Git Branch Consolidation
- **Master-Only Repository**: Consolidated all active development into `master`. Removed obsolete branches (`main`, `7008`, `v5`) both locally and remotely.
- **CI/CD Workflow**: Updated GitHub Actions release workflow to trigger exclusively on `master` pushes and release tags.
