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

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.secu3.android.R
import org.secu3.android.utils.LogExportDestination

class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    private lateinit var sharedPref: SharedPreferences

    private val logExportDirectoryRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        val uri = result.data?.data ?: return@registerForActivityResult
        val flags = result.data?.flags ?: 0
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (takeFlags != 0) {
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
        sharedPref.edit {
            putString(getString(R.string.pref_log_export_directory_key), uri.toString())
        }
        updatePreferenceList()
    }

    private fun updatePreferenceList() {
        findPreference<Preference>(getString(R.string.pref_connection_retries_key))?.let {
            val maxConnRetries = sharedPref.getString(getString(R.string.pref_connection_retries_key), getString(R.string.defaultConnectionRetries))
            it.summary = getString(R.string.pref_connection_retries_summary, maxConnRetries)
        }

        findPreference<Preference>(getString(R.string.pref_log_export_directory_key))?.let {
            it.summary = logExportDestination().toSummary()
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

        findPreference<Preference>(getString(R.string.pref_log_export_directory_key))?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                openLogExportDirectoryPicker()
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
            getString(R.string.pref_log_export_directory_key) -> {
                updatePreferenceList()
            }
        }
    }

    private fun openLogExportDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )
        }
        logExportDirectoryRequest.launch(intent)
    }

    private fun String.toFolderSummary(): String {
        return runCatching {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(toUri())
            treeDocumentId.substringAfter(':').ifBlank { treeDocumentId }
        }.getOrDefault(getString(R.string.pref_log_export_directory_default))
    }

    private fun logExportDestination(): LogExportDestination {
        val uri = sharedPref.getString(getString(R.string.pref_log_export_directory_key), null)
        return if (uri == null) {
            LogExportDestination.DefaultDownloads
        } else {
            LogExportDestination.CustomTree(uri)
        }
    }

    private fun LogExportDestination.toSummary(): String {
        return when (this) {
            LogExportDestination.DefaultDownloads -> getString(R.string.pref_log_export_directory_default)
            is LogExportDestination.CustomTree -> uri.toFolderSummary()
        }
    }
}
