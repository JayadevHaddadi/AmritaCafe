package edu.amrita.amritacafe.IO

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object CSVFileManager {
    /**
     * Saves the CSV content to a file named [fileName] in the internal storage.
     *
     * @param context The context.
     * @param fileName The name of the file to save (e.g., "BreakfastMenu.csv").
     * @param csvContent The CSV string to save.
     * @return True if the file was saved successfully; otherwise false.
     */
    fun saveCSV(context: Context, fileName: String, csvContent: String): Boolean {
        return try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                outputStream.write(csvContent.toByteArray())
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads the CSV content from a file named [fileName] in the internal storage.
     *
     * @param context The context.
     * @param fileName The name of the file to read.
     * @return The CSV string if available, or null if an error occurred.
     */
    fun readCSV(context: Context, fileName: String): String? {
        return try {
            context.openFileInput(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
