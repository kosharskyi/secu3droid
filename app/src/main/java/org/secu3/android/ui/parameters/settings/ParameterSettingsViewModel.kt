/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */

package org.secu3.android.ui.parameters.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.IOException
import org.secu3.android.utils.prefs.FuelDensities
import org.secu3.android.utils.prefs.ParameterSettingsPrefs

internal data class ParameterSettingsUiState(
    val densities: FuelDensities? = null,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
internal class ParameterSettingsViewModel @Inject constructor(
    private val prefs: ParameterSettingsPrefs,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ParameterSettingsUiState())
    val state: StateFlow<ParameterSettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.settings.collect { densities ->
                if (densities != null) mutableState.value = mutableState.value.copy(densities = densities)
            }
        }
        viewModelScope.launch {
            prefs.readFailed.collect { failed ->
                mutableState.value = mutableState.value.copy(loadFailed = failed)
            }
        }
    }

    fun parseDensity(text: String) = prefs.parseDensity(text)

    fun save(primary: Float, secondary: Float) {
        if (mutableState.value.saving || mutableState.value.saved || mutableState.value.densities == null) return
        mutableState.value = mutableState.value.copy(saving = true, saveFailed = false, saved = false)
        viewModelScope.launch {
            try {
                prefs.save(primary, secondary)
                mutableState.value = mutableState.value.copy(saving = false, saved = true)
            } catch (_: IOException) {
                mutableState.value = mutableState.value.copy(saving = false, saveFailed = true)
            }
        }
    }
}
