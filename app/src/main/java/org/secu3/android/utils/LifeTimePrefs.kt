/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.secu3.android.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeTimePrefs @Inject constructor(@ApplicationContext private val ctx: Context) {

    private var mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isSensorLoggerEnabled: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_write_log_key), false)
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_write_log_key), value).apply()

    var CSVDelimeter: String
        get() {
            val delimeter = mPrefs.getString(
                ctx.getString(R.string.pref_log_csv_delimeter_key),
                ctx.getString(R.string.defaultCsvDelimeter)
            )

            try {
                val idx = delimeter!!.indexOf("\"")
                return delimeter.substring(idx + 1, idx + 2)
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
            return ";"
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

    var uploadImmediately: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_upload_immediately_key), false);
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_upload_immediately_key), value).apply()

    var connectionRetries: Int
        get() = mPrefs.getString(ctx.getString(R.string.pref_connection_retries_key), ctx.getString(R.string.defaultConnectionRetries))!!.toInt()
        set(value) = mPrefs.edit().putString(ctx.getString(R.string.pref_connection_retries_key), value.toString()).apply()

    val speedPulses: Int
        get() = mPrefs.getString(ctx.getString(R.string.pref_speed_pulse_key), ctx.getString(R.string.defaultSpeedPulse))!!.toInt()
}