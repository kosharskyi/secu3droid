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
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import org.secu3.android.R
import org.secu3.android.ui.dashboard.DashboardConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPrefs @Inject constructor(@ApplicationContext private val ctx: Context) {

    private var mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    private val gson = Gson()

    var isSensorLoggerEnabled: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_write_log_key), false)
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_write_log_key), value).apply()

    var isBinaryLogFormatEnabled: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_write_binary_log_key), false)
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_write_binary_log_key), value).apply()

    var isCsvTitleEnabled: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_log_csv_write_title_key), false)
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_log_csv_write_title_key), value).apply()

    var CSVDelimeter: String
        get() {
            return mPrefs.getString(ctx.getString(R.string.pref_log_csv_delimeter_key),";")!!
        }
        set(value) = mPrefs.edit().putString(ctx.getString(R.string.pref_log_csv_delimeter_key), value).apply()

    var isKeepScreenAliveActive: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_keep_screen_key), false);
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_keep_screen_key), value).apply()

    var isWakeLockEnabled: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_wakelock_key), false);
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_wakelock_key), value).apply()

    var isDarkTheme: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_night_mode_key), false);
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_night_mode_key), value).apply()

    var bluetoothDeviceName: String?
        get() = mPrefs.getString(ctx.getString(R.string.pref_bluetooth_device_key), null);
        set(value) = mPrefs.edit().putString(ctx.getString(R.string.pref_bluetooth_device_key), value).apply()

    var connectionRetries: Int
        get() = mPrefs.getString(ctx.getString(R.string.pref_connection_retries_key), ctx.getString(R.string.defaultConnectionRetries))!!.toInt()
        set(value) = mPrefs.edit().putString(ctx.getString(R.string.pref_connection_retries_key), value.toString()).apply()







    var oldSensorViewEnabled: Boolean
        get() = mPrefs.getBoolean("old_sensors_view", false)
        set(value) = mPrefs.edit().putBoolean("old_sensors_view", value).apply()

    var columnsCount: Int
        get() = mPrefs.getInt("columns_count", 2)
        set(value) = mPrefs.edit().putInt("columns_count", value).apply()


    var dashboardConfig: DashboardConfig?
        get() {
            return mPrefs.getString("dashboard_config", null)?.let {
                gson.fromJson(it, DashboardConfig::class.java)
            }
        }
        set(value) {
            value ?: return
            val json = gson.toJson(value)
            mPrefs.edit().putString("dashboard_config", json).apply()
        }

}