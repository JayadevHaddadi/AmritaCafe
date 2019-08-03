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

            if (!file.isFile || true) { // TODO FORCE CREATE
                createDefaultFile(file, con)
            }

            val br = BufferedReader(FileReader(file))

            val menuList: MutableList<MenuItem> = mutableListOf()

            try {
                br.readLine()
                var line = br.readLine().trim()
                var currentCategory = "None"
                var currentNumberOfCat = 0
                while (!line.equals("")) {
                    println(line)
                    val split = line.split(",")
                    val name = split[0].trim().toUpperCase()
                    if(split.size == 1) {
                        if(!currentCategory.equals("None")) {
                            val missingFromFullRow = 10 - menuList.size % 10
                            if(missingFromFullRow!=10)
                                for (i in 1..missingFromFullRow) {
                                    menuList.add(MenuItem("", currentCategory, 0))
                                }
                        }
                        currentCategory = name
                    } else {
                        currentNumberOfCat++
                        val price = split[1].trim()
                        menuList.add(MenuItem(name,currentCategory,price.toInt()))
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

            val fos = FileOutputStream(file, false)

            val defaultText =
                """MENU ITEMS:
BURGER
VB, 40
CB, 60
Patty, 20
PSTOB, 65
DLXB, 80
Bun, 10
PIZZA
VN VEG PZA,100
VN GM PZA,100
VEG PZA,125
PSTO PZA,100
pan pza, 150
olv pza,135
med pza,125
gm pza,150
ch pza,10
PASTA
vn gm past,100
psto past,60
past tom ch,80
past all soy,90
dlx past,115
SALAD
SPRSAL, 50
sal,30
psto sal,75
dxl sal, 90
OMELETE
ON OM, 35
GM OM,95
veg ch om,70
olv om,65
psto om,55
frt om,35
dxl om,105
veg om,50
vn om,40
FRIES
SMSAFF,35
LGSAFF,70
LGFF,60
SMFF,30
BREAKFAST
ft,30
oat,25
por,25
hash,20
pan,30
rag pan,30
ft,30
vn om,40
SANDWICH
oms,45
ecs,55
gc frt,45
dxl gc,75
gc,40

ORDER RANGE: 
100

PRINTER IP: 
192.168.0.10
192.168.0.11
""".trimMargin()

            fos.write(defaultText.toByteArray())
            fos.close()
            MediaScannerConnection.scanFile(con, arrayOf(file.getAbsolutePath()), null, null)
        }

    }

}
