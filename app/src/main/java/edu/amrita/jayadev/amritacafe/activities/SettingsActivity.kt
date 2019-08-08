package edu.amrita.jayadev.amritacafe.activities

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.settings.Constants
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val dir = File(
            Environment.getExternalStorageDirectory().toString() + File.separator + "AmritaCafe"
        )
        file = File(dir.toString() + File.separator + "Settings.txt")
    }

    override fun onResume() {
        super.onResume()

        loadFromFile()
//        val sharedPreference = getSharedPreferences(MainActivity.DbConstants.PREFERENCE_KEY, Context.MODE_PRIVATE)
        range_ET.setText(MainActivity.DbConstants.sharedPreference.getInt("RANGE_KEY",100).toString())
    }

    private fun loadFromFile() {
        if (!file.isFile) { // TODO || true
            createDefaultFile(null)
        }

        val br = BufferedReader(FileReader(file))

        var text = ""
        try {
            text = br.readText()
            br.close()
        } catch (e: IOException) {
        }

        menu_settings_ET.setText(text)
    }

    override fun onPause() {
        super.onPause()
        file.createNewFile()
        val fos = FileOutputStream(file, false)
        fos.write(menu_settings_ET.text.toString().toByteArray())
        fos.close()

//        val sharedPreference = getSharedPreferences(MainActivity.DbConstants.PREFERENCE_KEY, Context.MODE_PRIVATE)
        val editor = MainActivity.DbConstants.sharedPreference.edit()
        editor.putInt("RANGE_KEY", range_ET.text.toString().toInt())
        editor.apply()


        println("onPause")
    }

    fun createDefaultFile(view: View?) {
        val dir = File(
            Environment.getExternalStorageDirectory().toString() + File.separator + "AmritaCafe"
        )

        val TAG = "default file"
        Log.d(TAG, "Path: : " + dir.toString())
        Log.d(TAG, "Exists? : " + dir.exists())
        if (!dir.exists()) {
            Log.d(TAG, "Crated: " + dir.mkdirs())
        }

        val file = File(dir.toString() + File.separator + "Settings.txt")
        file.createNewFile()
        val fos = FileOutputStream(file, false)
        val defaultText = Constants.defaultText
        fos.write(defaultText.toByteArray())
        fos.close()

        loadFromFile()
    }
}
