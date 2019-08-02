package edu.amrita.jayadev.amritacafe

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.*


class SettingsRetriver {

    data class MenuItem(val name: String, val category: String, val price: Int)

    companion object {

        fun getList(con: Context): MutableList<MenuItem> {
            val dir = File(
                Environment.getExternalStorageDirectory().toString() + File.separator + "AmritaCafe"
            )

            Log.d(TAG, "Path: : " + dir.toString())
            Log.d(TAG, "Exists? : " + dir.exists())
            if (!dir.exists()) {
                Log.d(TAG, "Crated: " + dir.mkdirs())
            }

            val file = File(dir.toString() + File.separator + "Settings.txt")

            if (!file.isFile) {
                createDefaultFile(file, con)
            }

            val br = BufferedReader(FileReader(file))

            val menuList: MutableList<MenuItem> = mutableListOf()

            try {
                br.readLine()
                var line = br.readLine().trim()
                var currentCat = "None"
                while (!line.equals("")) {
                    println(line)
                    val split = line.split(",")
                    val name = split[0].trim()
                    val price = split[1].trim()
                    if(price.equals("")){
                        currentCat = name
                    } else {
                        menuList.add(MenuItem(name,currentCat,price.toInt()))
                    }
                    line = br.readLine().trim()
                }

                br.close()
            } catch (e: IOException) {

            }

            return menuList
        }

        val TAG = "debug"

        private fun createDefaultFile(file: File, con: Context) {
            //CREATE DEFAULT FILE
            file.createNewFile()

            val fos = FileOutputStream(file, true)

            val defaultText = ("MENU:\n" +
                    "Burger, \n" +
                    "VB, 30\n" +
                    "CB,60\n" +
                    "DB,  80\n" +
                    "PP, 30\n" +
                    "Topping,\n" +
                    "Honey, 10 \n" +
                    "Sides, \n" +
                    "Tomato, 10\n" +
                    "\n" +
                    "RANGE: 100\n" +
                    "\n" +
                    "IP: 192.168.0.1")

            fos.write(defaultText.toByteArray())
            fos.close()
            MediaScannerConnection.scanFile(con, arrayOf(file.getAbsolutePath()), null, null)
        }

    }

}
