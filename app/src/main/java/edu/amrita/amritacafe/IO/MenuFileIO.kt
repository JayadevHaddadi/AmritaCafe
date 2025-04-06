package edu.amrita.amritacafe.IO

import android.content.Context
import edu.amrita.amritacafe.activities.MainActivity.Companion.BREAKFAST_FILE
import edu.amrita.amritacafe.activities.MainActivity.Companion.LUNCH_DINNER_FILE
import edu.amrita.amritacafe.activities.capitalizeWords
import edu.amrita.amritacafe.menu.DEFAULT_BREAKFAST_MENU
import edu.amrita.amritacafe.menu.DEFAULT_LUNCH_DINNER_MENU
import edu.amrita.amritacafe.menu.MenuItem
import java.io.File
import java.io.FileOutputStream

val DEFAULT_BREAKFAST_CSV = """BREAKFAST
Iddli ,15
Dosa,15
Sambar Only/Ex,10
Upma,30
Milk,30
Curd,40
Egg,20
Sprouts,10
Granola,30
Cornflakes,30
Butter,15
Bakery,50

LUNCH/DINNER
Main Item,40
Salad,30
Dressing,10
Bread,15
Butter,15
Pasta,30
Beschameal,20

SPECIAL
50,50
60,60
70,70
80,80
90,90
100,100
120,120
150,150

BAKERY
40,40
50,50
60,60
70,70
80,80"""

val DEFAULT_LUNCH_CSV =
    """MORNING
Curd,Curd,50
Granola,Gran,30
Cornflakes,Corn flks,30
Gomasio,Gomas,25
Breakfast Bakery,Break Bkry,50
Butter,Butter,15


COUNTER GOODS
Cakes/Cookies,Cakes/Cookies,40
Energy Balls, E Balls,20
Raw Pie,Raw Pie,100
Gran Bars, Bars,50
Spring Rolls,Rolls,100
SR Extra Sauce,SR Ex Sauce,20


HOT DRINKS SMALL
Coffee Sm,Coffee Sm,20
Chai Sm,Chai Sm,15
Masala Chai Sm,Msla Chai Sm,20
Espresso Sm,Espresso Sm,25
Cappuccino Sm,Cap Sm,30
Americano Sm,Americano Sm,30
Macchiato Sm,Macchiato Sm,30
Mocha Sm,Mocha Sm,50
Ceral Coffee Sm, Ceral Sm, 30
Sattu Drink Sm, Sattu Sm, 30
Tulasi Sm,Tulasi Sm,15
Milk Sm,Milk Sm,20
Alm Milk Sm,Alm Mlk Sm,50
Soy Milk Sm,Soy Mlk Sm,50
Hot Chocolate Sm,Hot Choc Sm,30
+ Alm/Soy Sm,+ Alm/Soy Sm,25
Ex Masala,Ex Msla,5

HOT DRINKS LARGE
Coffee Lg,Coffee Lg,40
Chai Lg,Chai Lg,30
Masala Chai Lg,Msla Chai Lg,40
Espresso Lg,Espresso Lg,50
Cappuccino Lg,Cap Lg,60
Americano Lg,Ameri Lg,60
Macchiato Lg,Macchiato Lg,60
Mocha Lg,Mocha Lg,100
Ceral Coffee Lg, Ceral Lg,60
Sattu Drink Lg, Sattu Lg,60
Tulasi Tea Lg,Tulasi Lg,30
Milk Lg,Mlk Lg,40
Almond Milk Lg,Almond Mlk Lg,100
Soy Milk Lg,Soy Mlk Lg,100
Hot Chocolate Lg,Hot Choco Lg,60
+ Almond/Soy Lg,+ Alm/Soy Lg,50
Black Tea,Black Tea,15
Ginger Tea,Ginger Tea,30
Green Tea,Green Tea,15
Peppermint Tea,Pepmnt Tea,30
Hot Water,Hot Water,5


COLD DRINKS SMALL
Iced Coffee Sm,Iced Coffee Sm,20
Lemonade Sm,Lemon Sm,15
Lemon No Sugar Sm,Lemon N/S Sm,15
Lemongrass Sm,Lemongrass Sm,15
Mango Sm,Mango Sm,30
Green Sm,Green Sm,30
Berry Sm,Berry Sm,50


COLD DRINKS LARGE
Iced Coffee Lg,Iced Coffee Lg,40
Lemononade Lg,Lemon Lg,30
Lemon No Sugar Lg, Lemon N/S Lg, 30
Lemongrass Lg,Lemongrass Lg,30
Mango Lg,Mango Lg,60
Green  Lg,Green Lg,60
Berry Lg,Berry Lg,100


SODA
Plain Soda,Plain,20
Lemon Soda,Lemon,30
Lemon Soda No Sugar,Lemon No Sgr,30
Pepsi Soda,Pepsi,30
Diet Pepsi Soda,Diet Pepsi,30
Ginger Soda,Ginger,30
Kombucca,Kombu,50
Grn Bry Kombu,Grn Bry Kombu,50


BAKERY/OTHER
50,50,50
60,60,60
Cups,Cups,100
Kisses,Kisses,200"""



fun createDefaultFilesIfNecessary(context: Context) {

    if (!BREAKFAST_FILE.isFile) { // TODO || true
        createMenuFileFromMenuList(BREAKFAST_FILE, DEFAULT_BREAKFAST_MENU)
    }
    if (!LUNCH_DINNER_FILE.isFile) { // TODO || true
        createMenuFileFromMenuList(LUNCH_DINNER_FILE, DEFAULT_LUNCH_DINNER_MENU)
    }
}

fun createMenuFileFromMenuList(file: File, list: List<MenuItem>) {
    if (!file.getParentFile().exists())
        file.getParentFile().mkdirs();
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

fun overrideFile(text: String, file: File) {
    val fos = FileOutputStream(file, false)
    fos.write(text.toByteArray())
    fos.close()
}

fun getListOfMenu(file: File): List<MenuItem> {
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
): Pair<String, List<MenuItem>> {
    val lineByLine = allText.split("\n")
    val menu = mutableListOf<MenuItem>()
    var category = ""
    var itemNr = 1
    try {
        for (line in lineByLine) {
            // Trim the line and skip if it's empty
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            // Split by comma, trim each cell, and filter out empties
            val columns = line.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (columns.isEmpty()) continue

            // If there's only one non-empty value, treat it as a category row
            if (columns.size == 1) {
                category = columns[0].toUpperCase()
                itemNr = 1
            } else if (columns.size == 3) {
                menu.add(
                    MenuItem(
                        columns[0].capitalizeWords(),
                        columns[1].uppercase(),
                        columns[2].toFloat(),
                        category
                    )
                )
            } else if (columns.size == 2) {
                menu.add(
                    MenuItem(
                        columns[0].capitalizeWords(),
                        columns[0].capitalizeWords(),
                        columns[1].toFloat(),
                        category
                    )
                )
            }
            itemNr++
        }
        return Pair("Successfully saved", menu.toList())
    } catch (e: Exception) {
        throw BadMenuException("Save failed at:\nCategory: ${category}\nItem in category: ${itemNr}")
    }
}


class BadMenuException(message: String) : Throwable(message)


