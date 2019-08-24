package edu.amrita.jayadev.amritacafe.fragments

import android.os.Bundle
import androidx.preference.*
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.settings.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainPreferencesFragment : PreferenceFragmentCompat() {
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

        println("THIS IS MY FPMOPPLE.")
        findPreference<EditTextPreference>("update_url")?.run {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<SwitchPreferenceCompat>("update_now")?.run {
            summaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> {
                "Updated: ${Configuration.lastUpdated(context!!)}"
            }
            setOnPreferenceChangeListener { _, newValue ->
                println("Changed")
                if (newValue == true) {
                    isEnabled = false
                    GlobalScope.launch(Dispatchers.IO) {
                        Configuration.refresh(context)
                        findPreference<SwitchPreferenceCompat>("update_now")?.run {
                            performClick()
                        }
                    }
                } else {
                    isEnabled = true
                }
                true
            }
        }


    }
}