package edu.amrita.jayadev.amritacafe

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.*

class SettingsRetriver(con: Context) {

    data class MenuItem(val name: String, val category: String, val price: Int)

    var printerOne = "192.168.0.10"
    var printerTwo = "192.168.0.11"
    var range = 100
    val menuList: MutableList<MenuItem> = mutableListOf()
    private val TAG = "debug"
    init {
        val dir = File(
            Environment.getExternalStorageDirectory().toString() + File.separator + "AmritaCafe"
        )

        Log.d(TAG, "Path: : " + dir.toString())
        Log.d(TAG, "Exists? : " + dir.exists())
        if (!dir.exists()) {
            Log.d(TAG, "Crated: " + dir.mkdirs())
        }

        val file = File(dir.toString() + File.separator + "Settings.txt")

        if (!file.isFile ) { // || true
            createDefaultFile(file, con)
        }

        val br = BufferedReader(FileReader(file))

        try {
            br.readLine()
            var line = br.readLine().trim()
            var currentCategory = "None"
            var currentNumberOfCat = 0

            //MENU
            while (!line.equals("")) {
                val split = line.split(",")
                val name = split[0].trim().toUpperCase()
                if (split.size == 1) {
                    if (!currentCategory.equals("None")) {
                        val missingFromFullRow = 10 - menuList.size % 10
                        if (missingFromFullRow != 10)
                            for (i in 1..missingFromFullRow) {
                                currentNumberOfCat++
                                menuList.add(MenuItem("", currentCategory, 0))
                            }
                    }
                    currentCategory = name
                } else {
                    currentNumberOfCat++
                    val price = split[1].trim()
                    menuList.add(MenuItem(name, currentCategory, price.toInt()))
                }
                line = br.readLine().trim()
            }

            //RANGE
            br.readLine().trim()
            range = br.readLine().trim().toInt()

            //PRINTER
            br.readLine().trim()
            br.readLine().trim()
            printerOne = br.readLine().trim()
            printerTwo = br.readLine().trim()

            br.close()
        } catch (e: IOException) {
        }
    }

    private fun createDefaultFile(file: File, con: Context) {
        //CREATE DEFAULT FILE
        file.createNewFile()
        val fos = FileOutputStream(file, false)
        val defaultText = Constants.defaultText
        fos.write(defaultText.toByteArray())
        fos.close()
//        MediaScannerConnection.scanFile(con, arrayOf(file.getAbsolutePath()), null, null)
    }
}

