# Amrita Cafe Printer & Network Configuration Guide

This guide documents the setup, configuration, and troubleshooting procedures for thermal printers (KP307, Epson TM-m30, Hoin, and generic ESC/POS printers) and the Tenda Wi-Fi router used during major events (such as Amritapuri Birthday celebrations).

---

## 1. KP307 Thermal Printer Setup (Wi-Fi Web Portal)

The KP307 thermal receipt printer includes an embedded Wi-Fi module that hosts a built-in web management interface when in Access Point (AP) mode or when connected directly to your computer/phone.

### Accessing the Web Interface
1. **Default AP IP Address:** `http://192.168.223.1` (or the IP assigned by your router in STA mode).
2. Connect your phone, tablet, or laptop to the printer's Wi-Fi network (or route to its IP).
3. Open a browser and navigate to `http://192.168.223.1`.

### Web Interface Tabs & Settings

#### **Status Tab**
- **PrintSelfTestPage:** Prints a hardware self-test ticket showing firmware version, current IP, MAC address, baud rate, and Wi-Fi mode.
- **Font Size:** `0 FontA(12x24)` (Default 12×24 dot font for 80mm/58mm standard ESC/POS).
- **Print Speed:** `120 mm/s` (Recommended: 100–120 mm/s for thermal head longevity and crisp barcodes/text).
- **Print Density:** `8` (Standard thermal contrast. Range is typically 1–8 or 1–15; set to 8 for clear, dark prints without overheating).
- **Buzzer On/Off:** `1 On` (Audio chime when an order finishes printing or when paper runs out — very useful in busy cafe/kitchen setups).
- **Cut Feed Back:** `0 Off`.
- **Auto Cut:** `0.0 Off` (Leave software-controlled; the app sends ESC/POS cut command `GS V 66 0` after printing).

#### **WiFi Tab**
- Used to switch the printer from Access Point (AP) mode to Station (STA) client mode.
- Enter your Tenda router's **SSID** and **WPA2 Pre-Shared Key (Password)**.
- Once saved and rebooted, the KP307 connects to your Tenda router and obtains an IP on your main cafe network.

#### **Network Tab**
- View or assign a **Static IP address** (e.g., `192.168.0.10` or `192.168.0.11`).
- Subnet mask: `255.255.255.0`
- Gateway: `192.168.0.1` (Tenda router's IP).

---

## 2. Printer Compatibility: KP307 vs. Epson TM-m30

### Will the app print with Epson printers or will there be an issue?
**Yes, it will print with Epson printers smoothly!**

In the Amrita Cafe app (`ReceiptDispatch.kt`), there are two printing backends:
1. **Raw TCP (Port 9100)** (`useRawSocket = true` in Settings):
   - Streams standard ESC/POS command bytes (`EscPosBuilder`) over socket port 9100.
   - **ESC/POS was created by Epson**, and virtually all Epson network printers (TM-m30, TM-T88, TM-T20) have standard RAW/JetDirect Port 9100 enabled by default.
   - **Recommendation:** Keep **"Use Raw TCP (Port 9100)" CHECKED** in the app Settings. Raw TCP works universally across **both Epson and KP307/Hoin** printers without requiring vendor-specific drivers or SDKs.

2. **Epson ePOS2 SDK Mode** (`useRawSocket = false` in Settings):
   - Uses Epson's proprietary Android SDK (`com.epson.epos2.printer.Printer`).
   - **Crucial Warning:** The ePOS2 SDK uses proprietary Epson handshakes and will **FAIL** if connected to non-Epson printers like the KP307.
   - Only turn OFF Raw TCP if you are using genuine Epson printers AND specifically need bidirectional status feedback (e.g. paper out detection via SDK).

### Can the tablet auto-detect which printer is connected?
**No, and it should NOT rely on auto-detection.**

- **Why:** Port 9100 is a raw unidirectional data stream. Budget Chinese thermal printers (KP307, Hoin) do not support standardized network discovery (such as mDNS, UPnP, or SNMP) in a unified way. Polling bidirectional ESC/POS status over noisy Wi-Fi introduces latency and packet timeouts that can freeze order submission.
- **How it is handled:** The tablet's **Settings screen** allows you to explicitly state:
  - **Kitchen Printer IP** (e.g., `192.168.0.11`)
  - **Receipt Printer IP** (e.g., `192.168.0.10`)
  - **"Use Raw TCP (Port 9100)"** checkbox
- **Formatting Compatibility (Columns & Line Width):**
  - The app's `EscPosBuilder` formats text using 32 columns for dividing lines (`length = 32`) and compact item name + price columns (17 + 4 characters).
  - Because 32 columns fit within both **58mm** (32 columns) and **80mm** (42/48 columns) paper sizes, the receipt layout prints neatly on both Epson and KP307 without wrapping or clipping text.

---

## 3. Tenda Router Settings: Best Configuration for Amritapuri Birthday

### Current Configured Tenda Router Setup (Saved)
- **SSID (Wi-Fi Name):** `Amrita cafe tenda` *(Note: SSID Broadcast is HIDDEN, must type manually when joining!)*
- **Password:** `ammaamma`
- **Wireless Channel:** `11` (Fixed non-overlapping channel)
- **IP Addressing:** DHCP (Dynamic IP used across varying routers)

During major festival events like Amma's Birthday, the 2.4 GHz RF environment has thousands of active smartphones, personal hotspots, and high background interference. A poorly configured router will suffer disconnects and dropped orders.

### Optimal Settings

| Setting | Recommended Value | Reason / Explanation |
| :--- | :--- | :--- |
| **Network Option** | **`11b/g/n` mixed** | Thermal printers (KP307, Hoin) use low-cost 2.4 GHz Wi-Fi chipsets. Forcing `11n only` can cause connection or association failures with legacy chipsets. `11b/g/n` guarantees 100% compatibility. |
| **Wireless Channel** | **Fixed: Channel 1, 6, or 11**<br>*(NEVER set to `Auto`)* | **CRITICAL:** When set to `Auto`, the router constantly detects crowd interference and performs channel hopping. **Every channel hop drops all Wi-Fi connections for 5–15 seconds**, causing POS prints to fail or timeout right when queues are longest. Lock to one non-overlapping channel (1, 6, or 11). |
| **Channel Width** | **`20 MHz` ONLY**<br>*(NEVER set to `40 MHz`)* | **CRITICAL:** 40 MHz occupies over two-thirds of the entire 2.4 GHz spectrum. In a crowded festival hall, 40 MHz suffers severe packet collisions, massive retransmissions, and high packet loss. 20 MHz has a +3 dB higher signal-to-noise ratio, much greater interference rejection, and more than enough speed for receipt printing (which transmits only a few kilobytes). |

### Additional Battle-Tested Best Practices for the Event

1. **DHCP Static IP Reservation:**
   - In the Tenda router management page under **DHCP / Static IP Reservation**, bind the MAC addresses of your printers to fixed IP addresses:
     - `192.168.0.10` -> Receipt Printer
     - `192.168.0.11` -> Kitchen Printer
   - This guarantees that if a printer or router reboots, the IP addresses never change or clash.
2. **Hidden or Private SSID:**
   - Do NOT share the cafe Wi-Fi password with guests or devotees.
   - If devotees connect their smartphones to the cafe Wi-Fi, the router's small NAT translation table will quickly saturate, causing printer sockets to be refused.
3. **Turn Off WPS:**
   - Disable Wi-Fi Protected Setup (WPS) on the router to prevent connection freezes and security vulnerabilities.
4. **Router Placement:**
   - Place the Tenda router elevated above table level (at head height or higher), within direct line of sight to the order tablets and printers. Crowds of human bodies absorb 2.4 GHz radio signals heavily.

---

## 4. Hardware Inventory & Roles

| Device | Brand / Model | Primary Interface | Auto-Cutter | Paper Size | Current / Planned Role |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Printer 1** | Shreyans KP307 | Wi-Fi / LAN (or BT) | Yes | 80mm | Wi-Fi / LAN 1 (Receipt or Kitchen) |
| **Printer 2** | Shreyans KP307 (New) | Wi-Fi / LAN (or BT) | Yes | 80mm | Wi-Fi / LAN 2 (Receipt or Kitchen) |
| **Printer 3** | Shreyans 80mm (Legacy) | USB / LAN | Yes | 80mm | Backup 80mm Station / Kitchen |
| **Printer 4** | Epson TM Series | Ethernet / Wi-Fi | Yes | 80mm | Backup 80mm Station |
| **Printer 5** | Hoin / PT-280 | Bluetooth | No (Tear bar) | 58mm | Mobile Cashier / Counter |

---

## 5. Universal Auto-Cut Command Implementation

### Is sending the cut command safe for printers without a cutter?
**Yes, 100% safe.**
* In the standard ESC/POS protocol, the paper cutting sequence (`GS V 0` or `0x1D 0x56 0x00`) is parsed only by printers equipped with a motorized cutter blade.
* Any thermal printer lacking a physical cutter mechanism (such as portable 58mm printers like the PT-280) simply discards the byte sequence without error, jam, or interruption.

### Bluetooth Auto-Cut Activation
* Previously, Bluetooth printing only sent plain text lines and never issued an auto-cut command.
* In [BluetoothPrinter.kt](file:///d:/GitHub/AmritaCafe/app/src/main/java/edu/amrita/amritacafe/printer/bluetooth/BluetoothPrinter.kt), we have now added `printer.testCutting()` (which sends `[0x1D, 0x56, 0x00]`) preceded by 3 feed lines.
* **Result:**
  * When the **KP307** is connected in Bluetooth mode, it feeds past the blade and **automatically cuts**.
  * When a portable **PT-280** is connected, it feeds the paper for a clean manual tear without throwing any error.
  * All **Wi-Fi / LAN** print jobs (via `EscPosBuilder.cut(1)`) already execute the cut command on all tickets.

---

## 6. Migration Roadmap: Retiring the Epson SDK

* **Current Architecture:**
  * The app provides a setting `useRawSocket` ("Use Raw TCP (Universal ESC/POS)"), enabled by default.
  * `ReceiptDispatch.kt` routes print jobs either via direct TCP socket (port 9100) or through Epson's `com.epson.epos2.printer.Printer`.
* **Testing Milestone:**
  * Test the remaining Epson printer using Raw TCP (port 9100). Because ESC/POS was created by Epson, all network Epson printers natively listen on port 9100.
* **Final Cleanup:**
  * Once the Epson printer confirms flawless printing and cutting via Raw TCP, we can remove:
    1. The `com.epson.epos2` proprietary SDK (`ePOS2.jar`).
    2. The "Use Raw TCP" toggle in Settings and Configuration.
    3. All legacy Epson wrapper classes and error handling.
  * This eliminates ~200 KB of binary bloat, prevents vendor lock-in, and standardizes the entire codebase on 100% open, universal ESC/POS.

