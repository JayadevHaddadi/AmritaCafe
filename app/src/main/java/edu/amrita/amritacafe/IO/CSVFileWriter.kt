package edu.amrita.amritacafe.IO

import android.os.Environment
import android.util.Log
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private fun isExternalStorageWritable(): Boolean {
    val state = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED == state
}

fun writeToCSV(orders: List<Order>, configuration: Configuration) {
    val lineToWrite = StringBuffer()
    orders.forEach { (orderNumber, orderItems, time) ->
        val date = Date(time)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val stringDate = dateFormat.format(date)

        orderItems.forEach {
            lineToWrite.append(
                "$stringDate, ${configuration.tabletName}, " +
                        "${orderNumber}, ${it.quantity}, ${it.menuItem.name}, " +
                        "${it.menuItem.price.toInt()}, ${it.totalPrice()}\n"
            )
        }
    }

    val text = lineToWrite.toString()
    val parentFolderName = "Amrita Cafe"
    val subFolderName = "History"

    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val fileName = "Amrita Cafe History - $currentDate.txt"

    if (isExternalStorageWritable()) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

// Create the parent folder if it doesn't exist
        val parentFolder = File(documentsDir, parentFolderName)
        if (!parentFolder.exists()) {
            val worked = parentFolder.mkdirs()
            if (worked) {
                Log.d("FileCreation", "File created successfully")
            } else {
                Log.d("FileCreation", "Failed to create file")
            }
        }

// Create the subfolder if it doesn't exist
        val subFolder = File(parentFolder, subFolderName)
        if (!subFolder.exists()) {
            subFolder.mkdirs()
        }

// Create or open the file within the specified folder.
        val file = File(subFolder, fileName)

        try {
            // Create the file if it doesn't exist
            if (!file.exists()) {
                file.createNewFile()
            }

            // Open the file in append mode
            val fos = FileOutputStream(file, true)
            // Write data to the file if needed
            fos.write(text.toByteArray())
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    } else {
        println("no write permissions")
        // External storage not writable, handle accordingly
    }
}