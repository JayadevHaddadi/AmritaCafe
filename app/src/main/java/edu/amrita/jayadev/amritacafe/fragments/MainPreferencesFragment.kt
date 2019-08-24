package edu.amrita.jayadev.amritacafe.fragments

import android.os.Bundle
import androidx.preference.*
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.settings.Configuration

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

        val updateUrlPref = findPreference<EditTextPreference>("update_url")
        updateUrlPref?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            "Updated: ${Configuration.lastUpdated(context!!)}"
        }

        findPreference<SwitchPreferenceCompat>("update_now")?.run {
            setOnPreferenceChangeListener { _, newValue ->
                println("The new value is $newValue")
//                this.isEnabled = newValue != true
                true
            }

        }

    }
}