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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.R
import org.secu3.android.utils.prefs.AppearanceMode
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.utils.prefs.UserPrefs
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.analytics.UsageAnalytics

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    private val uiMessageQueue: UiMessageQueue,
    private val usageAnalytics: UsageAnalytics,
) : ViewModel() {
    private val _state = MutableStateFlow<SettingsUiState?>(null)
    val state: StateFlow<SettingsUiState?> = _state.asStateFlow()
    private val _dialog = MutableStateFlow<SettingsDialog?>(null)
    val dialog: StateFlow<SettingsDialog?> = _dialog.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.observeSettings { uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG) }
                .collect { settings -> _state.value = readSettings(settings) }
        }
    }

    fun setBooleanPreference(keyResource: Int, value: Boolean) {
        persist(write = {
            when (keyResource) {
                R.string.pref_keep_screen_key -> userPrefs.setKeepScreenAliveActive(value)
                R.string.pref_usage_analytics_key -> usageAnalytics.setUserConsent(value)
            }
        })
    }

    fun saveRetries(value: String) {
        if (userPrefs.parsePositiveConnectionRetries(value) == null) return
        persist(write = { userPrefs.saveConnectionRetries(value) }, onSuccess = ::dismissDialog)
    }

    fun isValidRetries(value: String): Boolean = userPrefs.parsePositiveConnectionRetries(value) != null

    fun saveFuelFlowPulsesPerLitre(value: String) {
        if (userPrefs.parseFuelFlowPulsesPerLitre(value) == null) return
        persist(
            write = { userPrefs.saveFuelFlowPulsesPerLitre(value) },
            onSuccess = ::dismissDialog,
        )
    }

    fun isValidFuelFlowPulsesPerLitre(value: String): Boolean =
        userPrefs.parseFuelFlowPulsesPerLitre(value) != null

    fun setAppearanceMode(mode: AppearanceMode) {
        persist(write = { userPrefs.setAppearanceMode(mode) }, onSuccess = ::dismissDialog)
    }

    fun setInjectionTimingMode(mode: InjectionTimingMode) {
        persist(write = { userPrefs.setInjectionTimingMode(mode) }, onSuccess = ::dismissDialog)
    }

    fun showDialog(dialog: SettingsDialog?) {
        _dialog.value = dialog
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    private fun readSettings(settings: UserPrefs.Settings): SettingsUiState =
        SettingsUiState(
            connectionRetries = settings.connectionRetriesText,
            keepScreenOn = settings.isKeepScreenAliveActive,
            wakeLock = settings.isWakeLockEnabled,
            appearanceMode = settings.appearanceMode,
            fuelFlowPulsesPerLitre = settings.fuelFlowPulsesPerLitre.toString(),
            injectionTimingMode = settings.injectionTimingMode,
            usageAnalyticsEnabled = settings.isUsageAnalyticsEnabled,
        )

    private fun persist(write: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                write()
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IOException) {
                uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG)
            }
        }
    }
}
