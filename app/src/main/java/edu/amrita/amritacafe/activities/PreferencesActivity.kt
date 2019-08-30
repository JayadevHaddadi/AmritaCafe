package edu.amrita.amritacafe.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.fragments.MainPreferencesFragment

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MainPreferencesFragment())
            .commit()
    }

}