/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.logger

import android.content.Intent
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.LogExportDestination
import org.secu3.android.utils.LogDeletionResult
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.UserPrefs
import org.secu3.android.utils.logFileMimeType
import java.io.File

@HiltViewModel
class SecuLogsViewModel @Inject constructor(
    private val fileHelper: FileHelper,
    private val userPrefs: UserPrefs,
    private val savedStateHandle: SavedStateHandle,
    private val secuLogger: SecuLogger,
) : ViewModel() {
    private val _state = MutableStateFlow(SecuLogsUiState())
    val state: StateFlow<SecuLogsUiState> = _state.asStateFlow()
    val pendingDeletePath: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_PENDING_DELETE, null)
    private val effectsChannel = Channel<SecuLogsEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    init {
        refreshFiles()
    }

    fun refreshFiles() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { fileHelper.listOfLogs }
            _state.value = _state.value.copy(files = files)
        }
    }

    fun requiresDefaultDownloadsPermission(): Boolean =
        userPrefs.logExportDestination is LogExportDestination.DefaultDownloads && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    fun requestDelete(file: File) {
        savedStateHandle[KEY_PENDING_DELETE] = file.absolutePath
    }

    fun cancelDelete() {
        savedStateHandle[KEY_PENDING_DELETE] = null
    }

    fun confirmDelete() {
        val path = pendingDeletePath.value ?: return
        cancelDelete()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { secuLogger.deleteLogFile(File(path)) }
            refreshFiles()
            val failureEffect = when (result) {
                LogDeletionResult.Deleted -> null
                LogDeletionResult.Active -> SecuLogsEffect.ActiveLogDeleteRejected
                LogDeletionResult.Failed -> SecuLogsEffect.DeleteFailed
            }
            failureEffect?.let { effectsChannel.send(it) }
        }
    }

    fun requestDownloadPermission(file: File) {
        savedStateHandle[KEY_PENDING_EXPORT] = file.absolutePath
    }

    fun onDownloadPermissionResult(granted: Boolean) {
        consumePendingExportPath(
            savedStateHandle = savedStateHandle,
            granted = granted,
            onGranted = ::exportFile,
            onDenied = { effectsChannel.trySend(SecuLogsEffect.PermissionDenied) },
        )
    }

    fun exportFile(file: File) {
        viewModelScope.launch {
            val result = saveLogFile(file)
            effectsChannel.send(SecuLogsEffect.ExportFinished(result))
        }
    }

    suspend fun saveLogFile(file: File): SaveLogResult = withContext(Dispatchers.IO) {
        runCatching {
            val destination = fileHelper.saveLog(file, userPrefs.logExportDestination)
            SaveLogResult(isSuccess = true, destination = destination)
        }.getOrElse {
            SaveLogResult(isSuccess = false, destination = "")
        }
    }

    fun getShareIntent(file: File): Intent {
        val fileUri = fileHelper.getFileUri(file)
        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = logFileMimeType(file)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    companion object {
        internal const val KEY_PENDING_EXPORT = "pending_log_export_path"
        internal const val KEY_PENDING_DELETE = "pending_log_delete_path"
    }
}

internal fun consumePendingExportPath(
    savedStateHandle: SavedStateHandle,
    granted: Boolean,
    onGranted: (File) -> Unit,
    onDenied: () -> Unit,
) {
    val path = savedStateHandle.get<String>(SecuLogsViewModel.KEY_PENDING_EXPORT) ?: return
    savedStateHandle[SecuLogsViewModel.KEY_PENDING_EXPORT] = null
    if (granted) onGranted(File(path)) else onDenied()
}

data class SecuLogsUiState(val files: List<File> = emptyList())

sealed interface SecuLogsEffect {
    data object PermissionDenied : SecuLogsEffect
    data object ActiveLogDeleteRejected : SecuLogsEffect
    data object DeleteFailed : SecuLogsEffect
    data class ExportFinished(val result: SaveLogResult) : SecuLogsEffect
}

data class SaveLogResult(
    val isSuccess: Boolean,
    val destination: String,
)
