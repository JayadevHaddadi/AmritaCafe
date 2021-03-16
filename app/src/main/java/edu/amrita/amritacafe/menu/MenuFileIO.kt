package edu.amrita.amritacafe.menu

import android.content.Context
import android.os.Environment
import edu.amrita.amritacafe.activities.capitalizeWords
import java.io.File
import java.io.FileOutputStream

val dir = File(
    Environment.getExternalStorageDirectory().toString() + File.separator + "Amrita Cafe"
            + File.separator + "Menus"
)
val BREAKFAST_FILE = File(dir.toString() + File.separator + "Breakfast.txt")
val LUNCH_DINNER_FILE = File(dir.toString() + File.separator + "LunchDinner.txt")

fun createDefualtFilesIfNecessary() {
    println("Path: : $dir")
    println("Exists? : " + dir.exists())
    if (!dir.exists()) {
        println("Crated: " + dir.mkdirs())
    }

    if (!BREAKFAST_FILE.isFile) { // TODO || true
        createMenuFileFromMenuList(BREAKFAST_FILE, DEFAULT_BREAKFAST_MENU)
    }
    if (!LUNCH_DINNER_FILE.isFile) { // TODO || true
        createMenuFileFromMenuList(LUNCH_DINNER_FILE, DEFAULT_LUNCH_DINNER_MENU)
    }

}

public fun createMenuFileFromMenuList(file: File, list: List<MenuItemUS>) {
    file.createNewFile()
    val fos = FileOutputStream(file, false)
    var category = ""

    for (item in list) {
        val nextCategory = item.category.toUpperCase()
        if (category != nextCategory) {
            category = nextCategory
            fos.write("\n$category\n".toByteArray())
        }

        fos.write("${item.name.capitalizeWords()}, ${item.code.toUpperCase()}, ${item.price.toInt()}\n".toByteArray())
    }

    fos.close()
}

fun overrideFile(text: String, file: File, con: Context) {
//    file.createNewFile()
    val fos = FileOutputStream(file, false)
    fos.write(text.toByteArray())
    fos.close()
}

fun getListOfMenu(file: File): List<MenuItemUS> {
//    val fileInputStream = FileInputStream(file)
    val readLines = file.readText()
    val (_, list) = readMenuFromText(readLines)
    return list
}

fun saveIfValidText(text: String, context: Context, file: File): String {
    try {
        var (message, list) = readMenuFromText(text)
        createMenuFileFromMenuList(file, list)
//        val fos = FileOutputStream(file, false)
//        fos.write(text.toByteArray())
//        fos.close()
    } catch (e: BadMenuException) {
        return e.message ?: "Bad menu"
    } catch (e: Exception) {
        return "Failed on saving file"
    }
    return "Successfully saved ${file.name}"
}

private fun readMenuFromText(
    allText: String
): Pair<String, List<MenuItemUS>> {
    val lineByLine = allText.split("\n")

    val menu = mutableListOf<MenuItemUS>()

    var category = ""
    var itemNr = 1
    try {
        for (line in lineByLine) {
            val columns = line.split(",").map { it.trim() }
            if (columns.size == 1) {
                if (columns[0].isEmpty()) //skip empty lines
                    continue
                category = columns[0].toUpperCase()
                itemNr = 1
            } else
                menu.add(
                    MenuItemUS(
                        columns[0].capitalizeWords(),
                        columns[1].toUpperCase(),
                        columns[2].toFloat(),
                        category
                    )
                )

            itemNr++
        }
        for (item in menu) {
            println("${item.name},${item.code},${item.price},${item.category}")
        }
        return Pair("Successfully saved", menu.toList())
    } catch (e: Exception) {
        throw BadMenuException("Save failed at:\nCategory: ${category}\nItem in category: ${itemNr}")
//        return Pair(
//            ,
//            menu.toList()
//        )
    }
}

class BadMenuException(message: String) : Throwable(message)


