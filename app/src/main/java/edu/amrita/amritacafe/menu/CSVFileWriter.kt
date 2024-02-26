package edu.amrita.amritacafe.menu

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CSVFileWriter {
}

private fun isExternalStorageWritable(): Boolean {
    val state = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED == state
}
fun addOrderToTodaysCSV(text: String) {
    val parentFolderName = "Amrita Cafe"
    val subFolderName = "History"

    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val fileName = "Amrita Cafe History - $currentDate.txt"

    if (isExternalStorageWritable()) {
        // Get the directory for the user's public directory.
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        // Create the parent folder if it doesn't exist
        val parentFolder = File(documentsDir, parentFolderName)
//        if (!parentFolder.exists()) {
//            parentFolder.mkdirs()
//        }
//
//        // Create the subfolder if it doesn't exist
        val subFolder = File(parentFolder, subFolderName)
        if (!subFolder.exists()) {
            subFolder.mkdirs()
        }

        // Create or open the file within the specified folder.
        val file = File(subFolder, fileName)

        try {
            // Open the file in append mode
            val fos = FileOutputStream(file, true)
            fos.write(text.toByteArray())
//                fos.write("\n".toByteArray()) // Add a newline after appending the text
            fos.close()
            // Content successfully appended
        } catch (e: IOException) {
            e.printStackTrace()
            // Error occurred while appending to the file
        }
    } else {
        // External storage not writable, handle accordingly
    }
}