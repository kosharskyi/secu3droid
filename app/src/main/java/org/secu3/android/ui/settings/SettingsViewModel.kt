/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.provider.DocumentsContract
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.R

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _state = MutableStateFlow(readSettings())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    private val _dialog = MutableStateFlow<SettingsDialog?>(null)
    val dialog: StateFlow<SettingsDialog?> = _dialog.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        _state.value = readSettings()
    }

    fun setBooleanPreference(keyResource: Int, value: Boolean) {
        preferences.edit { putBoolean(context.getString(keyResource), value) }
    }

    fun setDelimiter(value: String) {
        preferences.edit { putString(context.getString(R.string.pref_log_csv_delimeter_key), value) }
        dismissDialog()
    }

    fun saveRetries(value: String) {
        if (parsePositiveRetries(value) == null) return
        preferences.edit { putString(context.getString(R.string.pref_connection_retries_key), value) }
        dismissDialog()
    }

    fun saveExportFolder(uri: String) {
        preferences.edit { putString(context.getString(R.string.pref_log_export_directory_key), uri) }
    }

    fun showDialog(dialog: SettingsDialog?) {
        _dialog.value = dialog
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun readSettings(): SettingsUiState {
        val delimiterKey = context.getString(R.string.pref_log_csv_delimeter_key)
        val retriesKey = context.getString(R.string.pref_connection_retries_key)
        val exportFolder = preferences.getString(context.getString(R.string.pref_log_export_directory_key), null)
        return SettingsUiState(
            writeLog = preferences.getBoolean(context.getString(R.string.pref_write_log_key), false),
            writeCsvHeader = preferences.getBoolean(context.getString(R.string.pref_log_csv_write_title_key), false),
            csvDelimiter = preferences.getString(delimiterKey, ";") ?: ";",
            writeBinaryLog = preferences.getBoolean(context.getString(R.string.pref_write_binary_log_key), false),
            exportFolder = exportFolder?.let(::folderSummary) ?: context.getString(R.string.pref_log_export_directory_default),
            connectionRetries = preferences.getString(retriesKey, context.getString(R.string.defaultConnectionRetries))
                ?: context.getString(R.string.defaultConnectionRetries),
            keepScreenOn = preferences.getBoolean(context.getString(R.string.pref_keep_screen_key), false),
            wakeLock = preferences.getBoolean(context.getString(R.string.pref_wakelock_key), false),
        )
    }

    private fun folderSummary(uri: String): String = runCatching {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(uri.toUri())
        treeDocumentId.substringAfter(':').ifBlank { treeDocumentId }
    }.getOrDefault(context.getString(R.string.pref_log_export_directory_default))
}
