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

In the Amrita Cafe app (`ReceiptDispatch.kt`), network printing is standardized on **Raw TCP (Port 9100)**:
- Streams standard ESC/POS command bytes (`EscPosBuilder`) over socket port 9100.
- **ESC/POS was created by Epson**, and virtually all Epson network printers (TM-m30, TM-T88, TM-T20) have standard RAW/JetDirect Port 9100 enabled by default.
- Raw TCP works universally across **both Epson and KP307/Hoin** printers without requiring vendor-specific drivers or SDKs.
- The legacy proprietary Epson ePOS2 SDK and the "Use Raw TCP" toggle have been retired to guarantee consistent cuts and margins.

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

## 6. Migration Status: Epson SDK Retired
* **Completed in Build 80104:**
  * Network printing has been unified on standard Raw TCP (Port 9100) ESC/POS.
  * The "Use Raw TCP" checkbox has been removed from Settings.
  * The app directly formats ESC/POS commands with customizable font sizes, line feeds, and auto-cuts.

---

## 7. Wi-Fi Hotspots: Why Printers Only Support 2.4 GHz (Not 5 GHz)

### The Hardware Reality of POS Printers
* **Why did the printer fail to connect to a 5 GHz phone hotspot?**
  * Thermal POS receipt printers (including KP307, Xprinter, Shreyans, Netum, and budget Epson models) use low-cost embedded IoT Wi-Fi microcontrollers (such as Espressif ESP8266, ESP32-C3/S2, or Realtek RTL8711AM).
  * These embedded chips **only have a 2.4 GHz radio (802.11 b/g/n)**. They physically lack the 5 GHz RF receiver, power amplifier, and ceramic filter required to detect or connect to 5 GHz (802.11 a/ac/ax) networks.
  * In commercial kitchen and restaurant environments, 2.4 GHz is the industry standard because 2.4 GHz radio waves penetrate kitchen walls, metal counters, and human bodies significantly better than 5 GHz (which has very weak obstacle penetration).

### How to Configure Your Phone's Hotspot for Cafe Use
Modern smartphones default to 5 GHz for hotspot sharing. To allow the KP307 and other printers to connect:
1. Open phone **Settings** \u2192 **Portable Hotspot / Tethering**.
2. Look for **"Extend compatibility"** (Samsung Galaxy / Google Pixel) and toggle it **ON**.
   - OR look for **AP Band / Wi-Fi Frequency** and change from **5 GHz Band** to **2.4 GHz Band**.
3. Now the KP307 printer, kitchen tablets, and order-taker phones can all connect to the same hotspot simultaneously.

---

## 8. Offline Local Wi-Fi Printing with Mobile Data Enabled

### The Problem
* When phones or tablets connect to the cafe router (e.g. Tenda) that has **no internet connection** (no WAN uplink, IPs like `192.168.0.x`), Android detects "No Internet Access".
* To keep internet working for apps (WhatsApp, Chrome), Android leaves **Mobile Data (Cellular)** as the *default network*.
* In standard Java networking, `Socket.connect()` routes packets through the default network (Cellular). The cellular carrier cannot route private RFC 1918 IPs (`192.168.0.x`), causing the connection to fail or timeout unless Mobile Data was manually turned off.

### The App Solution (`SocketHelper.kt`)
* The app now uses Android's `ConnectivityManager` and `Network.bindSocket()` to explicitly bind raw TCP printer sockets to the Wi-Fi network interface (`TRANSPORT_WIFI`).
* **Benefits:**
  1. Print sockets travel directly over Wi-Fi (`wlan0`) straight to the printer at `192.168.0.x`.
  2. **Mobile Data can stay ON at all times!**
  3. Orders can post to Google Sheets in the cloud via Mobile Data while simultaneously printing locally to the kitchen printer over offline Wi-Fi!

---

## 9. Testing Summary & Verified Matrix

| Setup | Printer Model | Protocol | Verified Behavior |
| :--- | :--- | :--- | :--- |
| **Wi-Fi / LAN** | Shreyans KP307 (New) | Raw TCP (Port 9100) | \u2705 Flawless printing, clean spacing, auto-cut working. |
| **Wi-Fi / LAN** | Epson TM Series | Raw TCP (Port 9100) | \u2705 Confirmed working with Raw TCP (bypassing ePOS SDK). |
| **Wi-Fi / LAN** | Shreyans Legacy 80mm | Raw TCP (Port 9100) | \u2705 Confirmed working with Raw TCP. |
| **Bluetooth** | Shreyans KP307 | SPP (Raw ESC/POS) | \u2705 Full 80mm width (42 cols), auto-cut working, no double line spacing. |
| **Bluetooth** | Hoin / PT-280 | SPP (Raw ESC/POS) | \u2705 58mm width (30 cols), clean tear-off feed. |
| **Phone Hotspot** | KP307 + Tablets | 2.4 GHz Wi-Fi Hotspot | ✅ Both phone and tablet can print simultaneously. Requires 2.4 GHz band. |
| **Offline Router** | Kitchen Tenda (No WAN) | Raw TCP with Mobile Data | ✅ Sockets bound to Wi-Fi; no need to disable cellular data. |

---

## 10. RF Interference at the Birthday Venue: Bluetooth vs. Wi-Fi

During Amma's Birthday celebrations, with thousands of devotees packed into the ashram carrying smartphones, smartwatches, and wireless earbuds, the 2.4 GHz spectrum becomes heavily congested. Understanding the physics behind Bluetooth and Wi-Fi helps prevent dropped orders.

### 1. What Band Does Bluetooth Use?
* **Bluetooth uses the exact same 2.4 GHz ISM band** (2.402 GHz to 2.480 GHz) as 2.4 GHz Wi-Fi.
* They share the exact same radio frequency airspace.

### 2. How Each Technology Fights Congestion

| Feature | 2.4 GHz Wi-Fi (Tenda Router) | Bluetooth (KP307 / PT-280) |
| :--- | :--- | :--- |
| **Frequency Strategy** | Parks on **one static 20 MHz channel** (e.g. Channel 11). | **Frequency Hopping (FHSS):** Hops across 79 distinct 1-MHz channels **1,600 times per second**. |
| **Transmit Power** | **High** (~100–200 mW / +20 dBm) with external high-gain antennas. | **Low** (~2.5 mW / +4 dBm) with tiny internal PCB trace antennas. |
| **Range Through Crowds** | **Long (20–50 meters)**. High power punches through air and around obstacles. | **Short (3–8 meters max)**. Human bodies are 70% water, which strongly absorbs 2.4 GHz radio waves. |
| **Interference Behavior** | Contends for airtime if other routers/hotspots share Channel 11. | **Adaptive Frequency Hopping (AFH):** Automatically detects busy Wi-Fi channels and skips around them. |

### 3. Which Suffers More Interference at the Venue?

The winner depends entirely on **distance**:

* **Close-Range (< 3 to 5 meters, e.g. Cashier Counter):**
  * **Winner: Bluetooth.**
  * Because the tablet is sitting right next to the printer, signal strength is high. Bluetooth's rapid 1,600 hops/second effectively dodges surrounding Wi-Fi traffic, giving an exceptionally clean, reliable connection at the counter.
* **Medium-to-Long Range (> 5 to 20 meters, e.g. Kitchen Printing):**
  * **Winner: Wi-Fi.**
  * The low-power (2.5 mW) Bluetooth signal will be completely smothered by crowds of standing devotees absorbing the signal. Connections will drop, timeout, or fail to pair.
  * The Tenda Wi-Fi router (100–200 mW) placed above crowd height will easily broadcast through the hall and kitchen.

### 4. Golden Architecture for Festival Operations

1. **Kitchen Order Printer (Long Range / Walls / Distance):**
   * **Always use Wi-Fi.**
   * Elevate the Tenda router at head height or higher.
   * Lock router to **Channel 11** at **20 MHz** width (prevents channel-hopping drops).
2. **Cashier / Counter Receipt Printer (Short Range):**
   * **Use Bluetooth OR Wi-Fi.**
   * If the cashier tablet is within 1–2 meters of the printer, Bluetooth is rock-solid and completely immune to Wi-Fi traffic.
   * If using Wi-Fi, it shares the same high-speed network.

---

## 11. Dual Bluetooth Printer Architecture (Bluetooth 1 & Bluetooth 2)

As of v7.2, the app supports configuring **two independent Bluetooth printers**:

* **Bluetooth Printer 1:** Connected via Hoin SDK / direct RFCOMM socket. Has its own paired device selector, Test button, and Paper Size setting (58mm or 80mm).
* **Bluetooth Printer 2:** Connected on-demand via direct Android RFCOMM socket (`BluetoothRawPrinter`). Has its own paired device selector, Test button, and Paper Size setting (58mm or 80mm).

### Destination Routing
In Settings, you can independently assign:
* **Receipt Printer:** Wi-Fi 1, Wi-Fi 2, `Bluetooth 1 (<name>)`, `Bluetooth 2 (<name>)`, or None.
* **Kitchen Printer:** Wi-Fi 2, Wi-Fi 1, `Bluetooth 1 (<name>)`, `Bluetooth 2 (<name>)`, or None.

This allows setups such as:
1. **Receipt on Bluetooth 1** (e.g., 58mm mobile belt printer or 80mm counter printer) + **Kitchen on Wi-Fi 2** (KP307 80mm in kitchen).
2. **Receipt on Bluetooth 1** + **Kitchen on Bluetooth 2** (both printing via Bluetooth without dropping connections).
3. **Receipt on Wi-Fi 1** + **Kitchen on Bluetooth 2**.



