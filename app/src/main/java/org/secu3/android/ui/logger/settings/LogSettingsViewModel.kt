/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.logger.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.prefs.UserPrefs

internal data class LogSettingsUiState(
    val showLogControls: Boolean,
    val alwaysWriteLog: Boolean,
    val writeCsvHeader: Boolean,
    val csvDelimiter: String,
    val writeBinaryLog: Boolean,
    val exportFolder: String,
)

@HiltViewModel
internal class LogSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPrefs,
    private val fileHelper: FileHelper,
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {
    private val _state = MutableStateFlow<LogSettingsUiState?>(null)
    val state = _state.asStateFlow()
    private val _showDelimiter = MutableStateFlow(false)
    val showDelimiter = _showDelimiter.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.observeSettings { reportStorageError() }.collect { settings ->
                _state.value = LogSettingsUiState(
                    showLogControls = settings.showLogControls,
                    alwaysWriteLog = settings.isAlwaysWriteLogEnabled,
                    writeCsvHeader = settings.isCsvTitleEnabled,
                    csvDelimiter = settings.csvDelimiter,
                    writeBinaryLog = settings.isBinaryLogFormatEnabled,
                    exportFolder = folderSummary(settings.logExportDirectoryUri),
                )
            }
        }
    }

    fun setBooleanPreference(keyResource: Int, value: Boolean) {
        persist {
            when (keyResource) {
                R.string.pref_write_log_key -> userPrefs.setShowLogControls(value)
                R.string.pref_always_write_log_key -> userPrefs.setAlwaysWriteLogEnabled(value)
                R.string.pref_log_csv_write_title_key -> userPrefs.setCsvTitleEnabled(value)
                R.string.pref_write_binary_log_key -> userPrefs.setBinaryLogFormatEnabled(value)
            }
        }
    }

    fun openDelimiter() {
        _showDelimiter.value = true
    }

    fun dismissDelimiter() {
        _showDelimiter.value = false
    }

    fun setDelimiter(value: String) {
        persist {
            userPrefs.setCsvDelimiter(value)
            dismissDelimiter()
        }
    }

    fun saveExportFolder(uri: Uri, resultFlags: Int) {
        if (fileHelper.retainExportDirectoryPermission(uri, resultFlags)) {
            persist { userPrefs.setLogExportDirectoryUri(uri.toString()) }
        }
    }

    private fun persist(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (failure: IOException) {
                reportStorageError()
            }
        }
    }

    private fun reportStorageError() {
        uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG)
    }

    private fun folderSummary(uri: String?): String {
        val folder = uri?.let {
            runCatching {
                val treeDocumentId = DocumentsContract.getTreeDocumentId(it.toUri())
                treeDocumentId.substringAfter(':').ifBlank { treeDocumentId }
            }.getOrNull()
        }
        return folder ?: context.getString(R.string.pref_log_export_directory_default)
    }
}
