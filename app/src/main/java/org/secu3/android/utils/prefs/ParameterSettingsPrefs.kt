/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */

package org.secu3.android.utils.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException

data class FuelDensities(val primary: Float, val secondary: Float)

@Singleton
class ParameterSettingsPrefs @Inject constructor(
    @Named(PARAMETER_SETTINGS_DATASTORE) private val dataStore: DataStore<Preferences>,
    @Named(PREFERENCE_DATASTORE_SCOPE) scope: CoroutineScope,
) {
    private val primaryDensityKey = floatPreferencesKey("primary_fuel_density")
    private val secondaryDensityKey = floatPreferencesKey("secondary_fuel_density")

    private val mutex = Mutex()
    private val mutableSettings = MutableStateFlow<FuelDensities?>(null)
    val settings: StateFlow<FuelDensities?> = mutableSettings.asStateFlow()
    private val mutableReadFailed = MutableStateFlow(false)
    val readFailed: StateFlow<Boolean> = mutableReadFailed.asStateFlow()
    init {
        scope.launch {
            val preferences = dataStore.data
                .retryWhen { failure, _ ->
                    if (failure !is IOException) return@retryWhen false
                    mutableReadFailed.value = true
                    delay(1_000)
                    true
                }
                .first()
            mutex.withLock {
                if (mutableSettings.value == null) mutableSettings.value = readSettings(preferences)
                mutableReadFailed.value = false
            }
        }
    }

    fun parseDensity(text: String): Float? = text.replace(',', '.')
        .toFloatOrNull()
        ?.takeIf(::isValidDensity)

    suspend fun save(primary: Float, secondary: Float) {
        require(isValidDensity(primary) && isValidDensity(secondary))
        withContext(NonCancellable) {
            mutex.withLock {
                val savedPreferences = dataStore.edit { preferences ->
                    preferences[primaryDensityKey] = primary
                    preferences[secondaryDensityKey] = secondary
                }
                mutableSettings.value = readSettings(savedPreferences)
                mutableReadFailed.value = false
            }
        }
    }

    private fun readSettings(preferences: Preferences) = FuelDensities(
        primary = preferences[primaryDensityKey].validOrDefault(DEFAULT_PRIMARY_DENSITY),
        secondary = preferences[secondaryDensityKey].validOrDefault(DEFAULT_SECONDARY_DENSITY),
    )

    private fun Float?.validOrDefault(default: Float): Float =
        this?.takeIf(::isValidDensity) ?: default

    private fun isValidDensity(value: Float): Boolean = value.isFinite() && value in MIN_DENSITY..MAX_DENSITY

    companion object {
        const val PARAMETER_SETTINGS_DATASTORE = "parameterSettingsDataStore"
        const val PREFERENCE_DATASTORE_SCOPE = "preferenceDataStoreScope"
        const val MIN_DENSITY = 0.2f
        const val MAX_DENSITY = 2.0f
        const val DEFAULT_PRIMARY_DENSITY = 0.710f
        const val DEFAULT_SECONDARY_DENSITY = 0.536f
    }
}
