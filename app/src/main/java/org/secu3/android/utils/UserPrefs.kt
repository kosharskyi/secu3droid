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
package org.secu3.android.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import java.io.IOException
import org.secu3.android.R
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.SensorGaugeStyle

@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named(USER_PREFS_DATASTORE) private val dataStore: DataStore<Preferences>,
) {
    private val writeLogKey = booleanPreferencesKey(context.getString(R.string.pref_write_log_key))
    private val writeBinaryKey = booleanPreferencesKey(context.getString(R.string.pref_write_binary_log_key))
    private val csvTitleKey = booleanPreferencesKey(context.getString(R.string.pref_log_csv_write_title_key))
    private val csvDelimiterKey = stringPreferencesKey(context.getString(R.string.pref_log_csv_delimeter_key))
    private val keepScreenKey = booleanPreferencesKey(context.getString(R.string.pref_keep_screen_key))
    private val wakeLockKey = booleanPreferencesKey(context.getString(R.string.pref_wakelock_key))
    private val notificationPermissionRequestedKey = booleanPreferencesKey(context.getString(R.string.connection_notifications_permission_requested_key))
    private val legacyAppearanceKey = booleanPreferencesKey(context.getString(R.string.pref_night_mode_key))
    private val appearanceKey = stringPreferencesKey(context.getString(R.string.pref_appearance_mode_key))
    private val bluetoothNameKey = stringPreferencesKey(context.getString(R.string.pref_bluetooth_device_key))
    private val bluetoothAddressKey = stringPreferencesKey(context.getString(R.string.pref_bluetooth_device_address_key))
    private val connectionRetriesKey = stringPreferencesKey(context.getString(R.string.pref_connection_retries_key))
    private val logExportDirectoryKey = stringPreferencesKey(context.getString(R.string.pref_log_export_directory_key))
    private val oldSensorsViewKey = booleanPreferencesKey(OLD_SENSORS_VIEW_KEY)
    private val sensorGaugeStyleKey = stringPreferencesKey(SENSOR_GAUGE_STYLE_KEY)
    private val injectionTimingModeKey = stringPreferencesKey(INJECTION_TIMING_MODE_KEY)

    val settings: Flow<Settings> = dataStore.data
        .map(::readSettings)
        .distinctUntilChanged()

    fun observeSettings(onReadFailure: (IOException) -> Unit): Flow<Settings> {
        return flow {
            var failureReported = false
            emitAll(settings
                .onEach { failureReported = false }
                .retryWhen { failure, _ ->
                    if (failure !is IOException) return@retryWhen false
                    if (!failureReported) onReadFailure(failure)
                    failureReported = true
                    delay(1_000)
                    true
                })
        }
    }

    private fun readSettings(preferences: Preferences) = Settings(
        isSensorLoggerEnabled = preferences[writeLogKey] ?: false,
        isBinaryLogFormatEnabled = preferences[writeBinaryKey] ?: false,
        isCsvTitleEnabled = preferences[csvTitleKey] ?: false,
        csvDelimiter = preferences[csvDelimiterKey] ?: ";",
        isKeepScreenAliveActive = preferences[keepScreenKey] ?: false,
        isWakeLockEnabled = preferences[wakeLockKey] ?: false,
        hasRequestedConnectionNotificationPermission = preferences[notificationPermissionRequestedKey] ?: false,
        appearanceMode = readAppearanceMode(preferences),
        bluetoothDeviceName = preferences[bluetoothNameKey],
        bluetoothDeviceAddress = preferences[bluetoothAddressKey],
        connectionRetriesText = preferences[connectionRetriesKey] ?: context.getString(R.string.defaultConnectionRetries),
        logExportDirectoryUri = preferences[logExportDirectoryKey],
        oldSensorViewEnabled = preferences[oldSensorsViewKey] ?: false,
        sensorGaugeStyle = SensorGaugeStyle.fromPreference(preferences[sensorGaugeStyleKey]),
        injectionTimingMode = InjectionTimingMode.fromPreference(preferences[injectionTimingModeKey]),
    )

    private fun readAppearanceMode(preferences: Preferences): AppearanceMode {
        if (preferences.contains(appearanceKey)) return AppearanceMode.fromPreference(preferences[appearanceKey])
        return when (preferences[legacyAppearanceKey]) {
            true -> AppearanceMode.Dark
            false -> AppearanceMode.Light
            null -> AppearanceMode.System
        }
    }

    suspend fun setSensorLoggerEnabled(value: Boolean) = update { this[writeLogKey] = value }
    suspend fun setBinaryLogFormatEnabled(value: Boolean) = update { this[writeBinaryKey] = value }
    suspend fun setCsvTitleEnabled(value: Boolean) = update { this[csvTitleKey] = value }
    suspend fun setCsvDelimiter(value: String) = update { this[csvDelimiterKey] = value }
    suspend fun setKeepScreenAliveActive(value: Boolean) = update { this[keepScreenKey] = value }
    suspend fun setWakeLockEnabled(value: Boolean) = update { this[wakeLockKey] = value }
    suspend fun setConnectionNotificationPermissionRequested(value: Boolean) = update { this[notificationPermissionRequestedKey] = value }
    suspend fun setAppearanceMode(value: AppearanceMode) = update { this[appearanceKey] = value.preferenceValue }
    suspend fun setBluetoothDevice(address: String?, name: String?) = update {
        if (address == null) remove(bluetoothAddressKey) else this[bluetoothAddressKey] = address
        if (name == null) remove(bluetoothNameKey) else this[bluetoothNameKey] = name
    }

    suspend fun clearBluetoothDeviceAddress() = update { remove(bluetoothAddressKey) }
    suspend fun setConnectionRetries(value: Int) = update { this[connectionRetriesKey] = value.toString() }

    suspend fun setLogExportDirectoryUri(value: String?) = update {
        if (value == null) remove(logExportDirectoryKey) else this[logExportDirectoryKey] = value
    }

    suspend fun setOldSensorViewEnabled(value: Boolean) = update { this[oldSensorsViewKey] = value }
    suspend fun setSensorGaugeStyle(value: SensorGaugeStyle) = update { this[sensorGaugeStyleKey] = value.preferenceValue }
    suspend fun setInjectionTimingMode(value: InjectionTimingMode) = update { this[injectionTimingModeKey] = value.preferenceValue }

    fun parsePositiveConnectionRetries(value: String): Int? = value.toIntOrNull()?.takeIf { it > 0 }

    suspend fun saveConnectionRetries(value: String): Boolean {
        val retries = parsePositiveConnectionRetries(value) ?: return false
        setConnectionRetries(retries)
        return true
    }

    private suspend fun update(transform: MutablePreferences.() -> Unit) {
        dataStore.edit { it.transform() }
    }

    data class Settings(
        val isSensorLoggerEnabled: Boolean,
        val isBinaryLogFormatEnabled: Boolean,
        val isCsvTitleEnabled: Boolean,
        val csvDelimiter: String,
        val isKeepScreenAliveActive: Boolean,
        val isWakeLockEnabled: Boolean,
        val hasRequestedConnectionNotificationPermission: Boolean,
        val appearanceMode: AppearanceMode,
        val bluetoothDeviceName: String?,
        val bluetoothDeviceAddress: String?,
        val connectionRetriesText: String,
        val logExportDirectoryUri: String?,
        val oldSensorViewEnabled: Boolean,
        val sensorGaugeStyle: SensorGaugeStyle,
        val injectionTimingMode: InjectionTimingMode,
    ) {
        val connectionRetries: Int get() = connectionRetriesText.toIntOrNull()?.takeIf { it > 0 } ?: 5
        val logExportDestination: LogExportDestination
            get() = logExportDirectoryUri?.let(LogExportDestination::CustomTree) ?: LogExportDestination.DefaultDownloads
    }

    companion object {
        const val USER_PREFS_DATASTORE = "userPrefsDataStore"
        const val OLD_SENSORS_VIEW_KEY = "old_sensors_view"
        const val SENSOR_GAUGE_STYLE_KEY = "sensor_gauge_style"
        const val INJECTION_TIMING_MODE_KEY = "injection_timing_mode"

    }
}

sealed class LogExportDestination {
    data object DefaultDownloads : LogExportDestination()
    data class CustomTree(val uri: String) : LogExportDestination()
}
