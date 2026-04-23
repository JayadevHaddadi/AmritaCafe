# Amrita Cafe App Usage Guide

This app is designed to cater to two primary workflows: **Cafe Order Takers** and **Cashiers**. The UI automatically adapts based on the selected menu.

## Workflow Modes

### 1. Cafe Order Taker (WIFI Printing)
Used for taking orders at tables or the counter where receipts need to be printed in both the kitchen and for the customer via WIFI.
- **Trigger Menus:** Any menu containing "breakfast", "lunch", or "dinner".
- **Printer Mode:** Automatically switches to **WIFI**.
- **Process:**
  1. Select the appropriate menu (e.g., "Breakfast") from the top spinner.
  2. Add items to the order.
  3. Click **ORDER**.
  4. Kitchen and Customer receipts are printed automatically over the network.

### 2. Cashier (Bluetooth Printing)
Used for handling direct payments and quick takeaways.
- **Trigger Menus:** Any menu containing "cafe" or "canteen".
- **Printer Mode:** Automatically switches to **Bluetooth**.
- **Process:**
  1. Select the appropriate menu (e.g., "Cafe Drinks") from the top spinner.
  2. Add items to the order.
  3. Click **ORDER**.
  4. A **Payment Dialog** appears to handle cash/GPay and calculate change.
  5. Customer receipt is printed via a paired Bluetooth printer.

## Key UI Elements
- **Menu Spinner (Top Left):** Quickly switch between different menus. This also sets the printer mode.
- **Settings (Gear Icon, Top Right):** Access configuration such as Printer IP, Bluetooth device selection, and Order Number ranges.
- **History (Clock Icon, Top Right):** View and re-print previous orders.
- **Short/Long Names:** Toggle between display names for menu items.
- **Clear Order (Trash Icon):** Clears the current pending order.

## Admin Configuration
Printer modes are assigned based on keywords found in the selected menu's name. These keywords can be configured in the **Settings** screen.

- **WIFI Mode Keywords (Default):** `breakfast`, `lunch`, `dinner`
- **Bluetooth Mode Keywords (Default):** `cafe`, `canteen`

If a menu name contains keywords from both lists, Bluetooth takes precedence. If no keywords match, the previous mode is retained.
