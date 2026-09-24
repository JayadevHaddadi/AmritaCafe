# Amrita Cafe — Build 80105

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

---

# Previous Changes — Build 80104

### 🖨️ Raw TCP ESC/POS Universal Default
- **Standardized on Raw TCP (Port 9100)**: Direct socket communication is now the standard network printing engine across all Wi-Fi/LAN printers.
- **Settings UI Cleanup**: Removed the "Use Raw TCP" checkbox.
- **Real-Time IP Saving**: Changes made to printer IP addresses in Settings are synchronized immediately for test prints.
