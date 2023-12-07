package edu.amrita.amritacafe.menu

import android.content.Context
import edu.amrita.amritacafe.activities.MainActivity.Companion.BREAKFAST_FILE
import edu.amrita.amritacafe.activities.MainActivity.Companion.LUNCH_DINNER_FILE
import edu.amrita.amritacafe.activities.capitalizeWords
import java.io.File
import java.io.FileOutputStream

fun createDefualtFilesIfNecessary(context: Context) {

    if (!BREAKFAST_FILE.isFile) { // TODO || true
        createMenuFileFromMenuList(BREAKFAST_FILE, DEFAULT_BREAKFAST_MENU)
    }
    if (!LUNCH_DINNER_FILE.isFile) { // TODO || true
        createMenuFileFromMenuList(LUNCH_DINNER_FILE, DEFAULT_LUNCH_DINNER_MENU)
    }

    overrideFile(
        """MORNING
Curd,Curd,50
Granola,Granola,30
Cornflakes,Cornflakes,30
Gomasio,Gomasio,25
Breakfast Bakery,Break Bakery,50
Butter,Butter,15


COUNTER GOODS
Cakes/Cookies,Cakes/Cookies,40
Energy Balls, E Balls,20
Raw Pie,Raw Pie,100
Granola Bars,Gran Bars,50
Spring Rolls,Spring Rolls,100
SR Extra Sauce,SR Ex Sauce,20


HOT DRINKS SMALL
Coffee Small,Coffee Sm,20
Chai Small,Chai Small,15
Masala Chai Small,Masala Chai Sm,20
Espresso Small,Espresso Sm,25
Cappuccino Small,Cap Sm,30
Americano Small,Americano Sm,30
Macchiato Small,Macchiato Sm,30
Mocha Small,Mocha Sm,50
Ceral Coffee Small, Ceral Sm, 30
Sattu Drink Small, Sattu Sm, 30
Ex Masala,Ex Masala,5
Tulasi Tea Small,Tulasi Sm,15
Milk Small,Milk Sm,20
Almond Milk Small,Almond Mlk Sm,50
Soy Milk Small,Soy Mlk Sm,50
Hot Chocolate Small,Hot Choc Sm,30
Add Almond/Soy Small,Add Alm/Soy Sm,25


HOT DRINKS LARGE
Coffee Large,Coffee Lg,40
Chai Large,Chai Lg,30
Masala Chai Large,Masala Chai Lg,40
Espresso Large,Espresso Lg,50
Cappuccino Large,Cap Lg,60
Americano Large,Ameri Lg,60
Macchiato Large,Macchiato Lg,60
Mocha Large,Mocha Lg,100
Ceral Coffee Large, Ceral Lg,60
Sattu Drink Large, Sattu Lg,60
Tulasi Tea Large,Tulasi Lg,30
Black Tea,Black Tea,15
Ginger Tea,Ginger Tea,30
Green Tea,Green Tea,15
Peppermint Tea,Pepmnt Tea,30
Milk Large,Mlk Lg,40
Almond Milk Large,Almond Mlk Lg,100
Soy Milk Large,Soy Mlk Lg,100
Hot Chocolate Large,Hot Choco Lg,60
Hot Water,Hot Water,5
Add Almond/Soy Large,Add Alm/Soy Lg,50


COLD DRINKS SMALL
Iced Coffee Small,Iced Coffee Sm,20
Lemonade Small,Lemonade Sm,15
Lemongrass Small,Lemongrass Sm,15
Mango Small,Mango Sm,30
Green Drink Small,Green Sm,30
Berry Drink Small,Berry Sm,50


COLD DRINKS LARGE
Iced Coffee Large,Iced Coffee Lg,40
Lemonade Large,Lemonade Lg,30
Lemongrass Small,Lemongrass Lg,30
Mango Large,Mango Lg,60
Green Drink Large,Green Lg,60
Berry Drink Large,Berry Lg,100


SODA
Plain Soda,Plain,20
Lemon Soda,Lemon,30
Lemon Soda No Sugar,Lemon No Sgr,30
Pepsi Soda,Pepsi,30
Diet Pepsi Soda,Diet Pepsi,30
Ginger Soda,Ginger,30
Black Kombucca,Kombucca,50
Green Kombucha,Grn Bry Kombucha,50


BAKERY/OTHER
40,40,40
50,50,50
60,60,60
80,80,80
100,100,100""", LUNCH_DINNER_FILE
    )

    overrideFile(
        """BREAKFAST
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
80,80""", BREAKFAST_FILE
    )
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
            val columns = line.split(",").map { it.trim() }
            if (columns.size == 1) {
                if (columns[0].isEmpty()) //skip empty lines
                    continue
                category = columns[0].toUpperCase()
                itemNr = 1
            } else if (columns.size == 3)
                menu.add(
                    MenuItem(
                        columns[0].capitalizeWords(),
                        columns[1].uppercase(),
                        columns[2].toFloat(),
                        category
                    )
                )
            else if (columns.size == 2)
                menu.add(
                    MenuItem(
                        columns[0].capitalizeWords(),
                        columns[0].capitalizeWords(),
                        columns[1].toFloat(),
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


