# Amrita Cafe — Build 80108

### 🔒 Ultra-Reliable Google Sheets Offline Sync (Multi-Day Resilience)
- **Power-Cut & Crash Safe Storage**: Replaced asynchronous `SharedPreferences` with a dedicated disk queue (`google_sheets_pending_queue.json`) using atomic temporary file swaps and physical hardware flush (`fsync`). Orders are physically committed to flash memory immediately upon order placement, ensuring zero data loss even if the battery dies or power is abruptly cut.
- **Strict Delivery Confirmation**: Orders remain in the queue until a genuine confirmation response is received from Google Sheets. Responses containing captive portal logins (hotel/cafe Wi-Fi redirect pages) or HTML script errors are detected and rejected from clearing the queue.
- **Automatic Reconnection & Sync**:
  - Registered an OS-level `ConnectivityManager.NetworkCallback` that immediately begins syncing pending orders as soon as Wi-Fi/Internet connectivity is re-established.
  - Added a background sync ticker (every 30 seconds) while pending orders exist.
  - Queued GPay status updates (`updateGPayOnSheets`) through the same durable offline queue so payment method edits made while offline are never lost.
- **Live Sync Indicator**: Tapping the Google Sheets indicator dot on the main screen shows the exact count of pending orders and triggers an instant sync attempt.

### ✂️ History Trimmed to Last 20 Orders
- **Lean Memory & Storage**: Capped order history to a strict maximum of the last 20 entries both in memory and on disk. History remains resilient across screen rotations and recent restarts without piling up endlessly over time.

---

# Previous Changes — Build 80107

### 🎯 Clean Print Dialogs & Rotation Persistence
- Rotation-safe order history, clean text-based print status without overlapping icons, and full-width dialog styling.
