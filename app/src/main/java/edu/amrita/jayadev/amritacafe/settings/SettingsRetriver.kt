package edu.amrita.jayadev.amritacafe.settings

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.*

class SettingsRetriver(val con: Context) {

    data class MenuItem(val name: String, val category: String, val price: Int)

    var kitchenPrinterIP = "192.168.0.10"
    var receiptPrinterIP = "192.168.0.11"
    val dinnerLunchMenu: MutableList<MenuItem> = mutableListOf()
    val breakfastMenu: MutableList<MenuItem> = mutableListOf()
    private val TAG = "debug"

    fun readSettings() {
        dinnerLunchMenu.clear()
        breakfastMenu.clear()
        val dir = File(
            Environment.getExternalStorageDirectory().toString() + File.separator + "AmritaCafe"
        )

        Log.d(TAG, "Path: : " + dir.toString())
        Log.d(TAG, "Exists? : " + dir.exists())
        if (!dir.exists()) {
            Log.d(TAG, "Crated: " + dir.mkdirs())
        }

        val file = File(dir.toString() + File.separator + "Settings.txt")

        if (!file.isFile) { // TODO || true
            createDefaultFile(file, con)
        }

        val br = BufferedReader(FileReader(file))

        try {
            br.readLine()
            var line = br.readLine().trim()
            var currentCategory = "None"
            var currentNumberOfDinnerLunch = 0
            var currentNumberOfBreakfast = 0

            //MENU
            while (!line.equals("")) {
                val split = line.split(",")
                val name = split[0].trim().toUpperCase()
                if (split.size == 1) {
                    //NEW CATEGORY
                    if (!currentCategory.equals("None")) {
                        if (currentNumberOfDinnerLunch == 0) {
                            dinnerLunchMenu.removeAt(dinnerLunchMenu.size - 1)
                        } else {
                            val missingFromFullRow = 10 - dinnerLunchMenu.size % 10
                            if (missingFromFullRow != 10)
                                for (i in 1..missingFromFullRow) {
                                    dinnerLunchMenu.add(
                                        MenuItem(
                                            "",
                                            currentCategory,
                                            0
                                        )
                                    )
                                }
                            currentNumberOfDinnerLunch = 0
                        }
                        if (currentNumberOfBreakfast == 0) {
                            breakfastMenu.removeAt(breakfastMenu.size - 1)
                        } else {
                            val missingFromFullRow = 10 - breakfastMenu.size % 10
                            if (missingFromFullRow != 10)
                                for (i in 1..missingFromFullRow) {
                                    breakfastMenu.add(
                                        MenuItem(
                                            "",
                                            currentCategory,
                                            0
                                        )
                                    )
                                }
                            currentNumberOfBreakfast = 0
                        }
                    }
                    currentCategory = name
                    breakfastMenu.add(
                        MenuItem(
                            currentCategory,
                            currentCategory,
                            0
                        )
                    )
                    dinnerLunchMenu.add(
                        MenuItem(
                            currentCategory,
                            currentCategory,
                            0
                        )
                    )
                } else {
                    val price = split[1].trim()
                    if (split.size == 3) {
                        val toCategory = split[2].trim().toLowerCase()
                        if (toCategory == "b") {
                            breakfastMenu.add(
                                MenuItem(
                                    name,
                                    currentCategory,
                                    price.toInt()
                                )
                            )
                            currentNumberOfBreakfast++
                        } else if (toCategory == "a") {
                            currentNumberOfDinnerLunch++
                            currentNumberOfBreakfast++
                            dinnerLunchMenu.add(
                                MenuItem(
                                    name,
                                    currentCategory,
                                    price.toInt()
                                )
                            )
                            breakfastMenu.add(
                                MenuItem(
                                    name,
                                    currentCategory,
                                    price.toInt()
                                )
                            )
                        }
                    } else {
                        currentNumberOfDinnerLunch++
                        dinnerLunchMenu.add(
                            MenuItem(
                                name,
                                currentCategory,
                                price.toInt()
                            )
                        )
                    }
                }
                line = br.readLine().trim()
            }

            br.readLine().trim() // KITCHEN PRINTER:
            kitchenPrinterIP = br.readLine().trim()
            br.readLine().trim() // RECEIPT PRINTER:
            receiptPrinterIP = br.readLine().trim()
            br.close()
        } catch (e: IOException) {
        }
    }

    fun createDefaultFile(file: File, con: Context) {
        //CREATE DEFAULT FILE
        file.createNewFile()
        val fos = FileOutputStream(file, false)
        val defaultText = Constants.defaultText
        fos.write(defaultText.toByteArray())
        fos.close()
//        MediaScannerConnection.scanFile(con, arrayOf(file.getAbsolutePath()), null, null)
    }
}

