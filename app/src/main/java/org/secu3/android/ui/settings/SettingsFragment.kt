/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.secu3.android.R

class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {


    private lateinit var sharedPref: SharedPreferences

    private fun updatePreferenceList() {
        findPreference<Preference>(getString(R.string.pref_connection_retries_key))?.let {
            val maxConnRetries = sharedPref.getString(getString(R.string.pref_connection_retries_key), getString(R.string.defaultConnectionRetries))
            it.summary = getString(R.string.pref_connection_retries_summary, maxConnRetries)
        }

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        findPreference<Preference>(getString(R.string.pref_about_key))?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AboutDialog().show(childFragmentManager, "About dialog")
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPref.registerOnSharedPreferenceChangeListener(this)
        updatePreferenceList()
    }

    override fun onPause() {
        super.onPause()
        sharedPref.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.pref_connection_retries_key) -> {
                updatePreferenceList()
            }
        }
    }
}