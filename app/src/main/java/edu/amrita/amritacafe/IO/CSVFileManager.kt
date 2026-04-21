package edu.amrita.amritacafe.IO

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object CSVFileManager {
    fun saveCSV(context: Context, fileName: String, csvData: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            val fos = FileOutputStream(file)
            fos.write(csvData.toByteArray())
            fos.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun readCSV(context: Context, fileName: String): String? {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
