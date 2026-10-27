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
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.secu3.android.R

@Singleton
class PreferencesMigrations @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun userPrefsMigration(): DataMigration<Preferences> {
        val legacyName = "${context.packageName}_preferences"
        val migratedKeys = setOf(
            context.getString(R.string.pref_write_log_key),
            context.getString(R.string.pref_write_binary_log_key),
            context.getString(R.string.pref_log_csv_write_title_key),
            context.getString(R.string.pref_log_csv_delimeter_key),
            context.getString(R.string.pref_keep_screen_key),
            context.getString(R.string.pref_wakelock_key),
            context.getString(R.string.connection_notifications_permission_requested_key),
            context.getString(R.string.pref_night_mode_key),
            context.getString(R.string.pref_appearance_mode_key),
            context.getString(R.string.pref_bluetooth_device_key),
            context.getString(R.string.pref_bluetooth_device_address_key),
            context.getString(R.string.pref_connection_retries_key),
            context.getString(R.string.pref_log_export_directory_key),
            UserPrefs.OLD_SENSORS_VIEW_KEY,
            UserPrefs.SENSOR_GAUGE_STYLE_KEY,
            UserPrefs.INJECTION_TIMING_MODE_KEY,
        )
        return SharedPreferencesMigration(context, legacyName, migratedKeys)
    }

    fun appPrefsMigration(): DataMigration<Preferences> = SharedPreferencesMigration(
        context,
        "app_shared_prefs",
        setOf(AppPrefs.LAST_APP_VERSION_CHECK_KEY),
    )
}
