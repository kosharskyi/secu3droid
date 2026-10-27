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
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.R
import org.secu3.android.utils.AppearanceMode
import org.secu3.android.utils.UserPrefs

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPrefs,
) : ViewModel() {
    private val _state = MutableStateFlow(readSettings())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    private val _dialog = MutableStateFlow<SettingsDialog?>(null)
    val dialog: StateFlow<SettingsDialog?> = _dialog.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.preferenceChanges.collect { _state.value = readSettings() }
        }
    }

    fun setBooleanPreference(keyResource: Int, value: Boolean) {
        when (keyResource) {
            R.string.pref_write_log_key -> userPrefs.isSensorLoggerEnabled = value
            R.string.pref_log_csv_write_title_key -> userPrefs.isCsvTitleEnabled = value
            R.string.pref_write_binary_log_key -> userPrefs.isBinaryLogFormatEnabled = value
            R.string.pref_keep_screen_key -> userPrefs.isKeepScreenAliveActive = value
            R.string.pref_wakelock_key -> userPrefs.isWakeLockEnabled = value
        }
    }

    fun setDelimiter(value: String) {
        userPrefs.CSVDelimeter = value
        dismissDialog()
    }

    fun saveRetries(value: String) {
        if (!userPrefs.saveConnectionRetries(value)) return
        dismissDialog()
    }

    fun isValidRetries(value: String): Boolean = userPrefs.parsePositiveConnectionRetries(value) != null

    fun saveExportFolder(uri: String) {
        userPrefs.logExportDirectoryUri = uri
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        userPrefs.appearanceMode = mode
        dismissDialog()
    }

    fun showDialog(dialog: SettingsDialog?) {
        _dialog.value = dialog
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    private fun readSettings(): SettingsUiState {
        val exportFolder = userPrefs.logExportDirectoryUri
        return SettingsUiState(
            writeLog = userPrefs.isSensorLoggerEnabled,
            writeCsvHeader = userPrefs.isCsvTitleEnabled,
            csvDelimiter = userPrefs.CSVDelimeter,
            writeBinaryLog = userPrefs.isBinaryLogFormatEnabled,
            exportFolder = exportFolder?.let(::folderSummary) ?: context.getString(R.string.pref_log_export_directory_default),
            connectionRetries = userPrefs.connectionRetriesText,
            keepScreenOn = userPrefs.isKeepScreenAliveActive,
            wakeLock = userPrefs.isWakeLockEnabled,
            appearanceMode = userPrefs.appearanceMode,
        )
    }

    private fun folderSummary(uri: String): String = runCatching {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(uri.toUri())
        treeDocumentId.substringAfter(':').ifBlank { treeDocumentId }
    }.getOrDefault(context.getString(R.string.pref_log_export_directory_default))
}
