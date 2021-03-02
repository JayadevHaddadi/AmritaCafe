package edu.amrita.amritacafe.menu

import android.content.SharedPreferences
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import edu.amrita.amritacafe.email.User
import edu.amrita.amritacafe.settings.Configuration.Companion.MENU_US_KEY
import edu.amrita.amritacafe.settings.Configuration.Companion.USERS_KEY
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList


fun readSheets(
    pref: SharedPreferences,
    myCallback: (menuList: ArrayList<MenuItemUS>, users: ArrayList<User>) -> Unit
) {
    val google_api_key = "AIzaSyBYZ2FSarNqtbsuZYDYO5i7DcysQtfNlBE"
    val spreadsheet_id =
        "1mUen3oNFWpkkdxLcivkAB2ba2wS4rvZut2ODE8NkIPk"

    println("reading sheets!")
    val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    val factory: JsonFactory = JacksonFactory.getDefaultInstance()
    val sheetsService: Sheets = Sheets.Builder(transport, factory, null)
        .setApplicationName("My Awesome App")
        .build()
    val spreadsheetId = spreadsheet_id
//    pref.edit().putString()

    object : Thread() {
        override fun run() {
            try {
                val menuSheet = "Menu"
                val userSheet = "Users"

                val ranges: List<String> = Arrays.asList(menuSheet, userSheet)
                val result =
                    sheetsService.spreadsheets()
                        .values()
                        .batchGet(spreadsheetId)
                        .setKey(google_api_key)
                        .setRanges(ranges).execute()
                System.out.printf("%d ranges retrieved.", result.valueRanges.size)

                val menuRawList = result.valueRanges[0].getValues()
                val usersRawList = result.valueRanges[1].getValues()

                val userList = createUsersList(usersRawList)
                val toJson2 = Gson().toJson(usersRawList)
//                println("toJson: $toJson")
                pref.edit().putString(USERS_KEY, toJson2).apply()
                println("userlist: $userList")

                val menuList = createMenuList(menuRawList)
//                println("sheets: $menuRawList")
                println("Created list: $menuList")
                val toJson = Gson().toJson(menuRawList)
//                println("toJson: $toJson")
                pref.edit().putString(MENU_US_KEY, toJson).apply()
                val string = pref.getString(MENU_US_KEY, "[[]]")
                println("just got $string")

                myCallback(menuList, userList)
            } catch (e: Exception) {
                println("sheets: failed")
                e.printStackTrace()
            }
        }
    }.start()
}

private fun createUsersList(usersRawList: List<List<Any>>): ArrayList<User> {
    val userList = ArrayList<User>()
    if (usersRawList.size < 2) // only a empty list inside the list
        return userList
    for (user in usersRawList) {
        if (user[0] == "User Name") {
            println("First one is header2")
            continue
        }
        userList.add(
            User(
                user[0].toString(),
                user[1].toString(),
                user[2].toString()
            )
        )
    }
    return userList
}

fun loadLastUsers(pref: SharedPreferences): ArrayList<User> {
    val loadOldPref = pref.getString(USERS_KEY, "[[]]")
    println("Old pref2: $loadOldPref")
    val type = object : TypeToken<java.util.ArrayList<java.util.ArrayList<String>>>() {}.type
//                val type2 = object : TypeToken<Array<Array<String>>>(){}.type

    val fromJson =
        Gson().fromJson<java.util.ArrayList<java.util.ArrayList<String>>>(loadOldPref, type)
    println("fromJson: $fromJson")

    return createUsersList(fromJson)
}

private fun createMenuList(itemList: List<List<Any>>): ArrayList<MenuItemUS> {
    val arrayList = ArrayList<MenuItemUS>()
    if (itemList.size < 2)
        return arrayList
    var skipFirst = false
    for (item in itemList) {
        if (!skipFirst) {
            skipFirst = true
            println("First one is header")
            continue
        }
        arrayList.add(
            MenuItemUS(
                item[1].toString(),
                item[2].toString(),
                item[3].toString().toFloat(),
                item[4].toString()
            )
        )
    }
    return arrayList
}

fun loadLastMenu(pref: SharedPreferences): ArrayList<MenuItemUS> {
    val loadOldPref = pref.getString(MENU_US_KEY, "[[]]")
    println("Old pref: $loadOldPref")
    val type = object : TypeToken<ArrayList<ArrayList<String>>>() {}.type
    val fromJson =
        Gson().fromJson<ArrayList<ArrayList<String>>>(loadOldPref, type)
    println("fromJson: $fromJson")
    return createMenuList(fromJson)
}