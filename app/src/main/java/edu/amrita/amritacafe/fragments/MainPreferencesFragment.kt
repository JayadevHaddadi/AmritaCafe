package edu.amrita.amritacafe.fragments

import android.os.Bundle
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.menu.defaultMenu
import edu.amrita.amritacafe.settings.Configuration
import edu.amrita.amritacafe.settings.Configuration.Companion.MENU_JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import java.lang.Exception

class MainPreferencesFragment : PreferenceFragmentCompat() {
    private val mutex = Mutex()
    /**
     * Called during [.onCreate] to supply the preferences for this fragment.
     * Subclasses are expected to call [.setPreferenceScreen] either
     * directly or via helper methods such as [.addPreferencesFromResource].
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     * this is the state.
     * @param rootKey            If non-null, this preference fragment should be rooted at the
     * [PreferenceScreen] with this key.
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)

        findPreference<EditTextPreference>("update_url")?.apply {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        }

        val preference = PreferenceManager.getDefaultSharedPreferences(this.context)
        val configuration = Configuration(preference)

        findPreference<Preference>(Configuration.MEAL)?.run {
            setOnPreferenceClickListener {
                configuration.toggleMeal()
                true
            }
            summaryProvider = Preference.SummaryProvider<Preference> {
                configuration.currentMeal.name
            }
        }

        findPreference<Preference>(Configuration.MENU_RESET)?.run {
            setOnPreferenceClickListener {
                preference.edit {
                    putString(MENU_JSON, Json(JsonConfiguration.Stable.copy(prettyPrint = true))
                        .stringify(MenuItem.serializer().list, defaultMenu))
                    apply()
                }
                true
            }
        }

        findPreference<EditTextPreference>(MENU_JSON)?.run {
            setOnPreferenceChangeListener { _, newValue ->
                try {
                    Json(JsonConfiguration.Stable).parse(MenuItem.serializer().list, newValue.toString())
                    true
                } catch (_: Exception) {
                    Toast.makeText(this.context, "Invalid format. Changes were rejected.", Toast.LENGTH_LONG)
                        .show()
                    false
                }
            }
        }

        findPreference<Preference>(Configuration.UPDATE_NOW)?.run {
            summaryProvider = Preference.SummaryProvider<Preference> {
                "Updated: ${preference.getLong(Configuration.UPDATE_NOW, 0)}"
            }
            setOnPreferenceClickListener {
                val url = preference.getString("update_url", "")!!

                if (!mutex.isLocked) {
                    GlobalScope.launch(Dispatchers.IO) {
                        mutex.withLock {
                            val (_, response, result) = Fuel.get(url)
                                .awaitObjectResponseResult(kotlinxDeserializerOf(loader = MenuItem.serializer().list))
                            println(response.responseMessage)
                            result.fold(
                                success = {
                                    preference.edit {
                                        Json(JsonConfiguration.Stable).stringify(MenuItem.serializer().list, it).also { jsonStr ->
                                            putString("menu_json", jsonStr)
                                        }

                                        putLong("update_now", System.currentTimeMillis())
                                        apply()
                                    }
                                },
                                failure = {
                                    println("I made a boo boo")
                                }
                            )

                        }
                    }
                }

                true
            }
        }
    }
}