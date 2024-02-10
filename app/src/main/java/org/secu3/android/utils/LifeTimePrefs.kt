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
import dagger.hilt.android.qualifiers.ApplicationContext
import org.secu3.android.R
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeTimePrefs @Inject constructor(@ApplicationContext private val ctx: Context) {

    private var mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

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

    var uploadImmediately: Boolean
        get() = mPrefs.getBoolean(ctx.getString(R.string.pref_upload_immediately_key), false);
        set(value) = mPrefs.edit().putBoolean(ctx.getString(R.string.pref_upload_immediately_key), value).apply()

    var connectionRetries: Int
        get() = mPrefs.getString(ctx.getString(R.string.pref_connection_retries_key), ctx.getString(R.string.defaultConnectionRetries))!!.toInt()
        set(value) = mPrefs.edit().putString(ctx.getString(R.string.pref_connection_retries_key), value.toString()).apply()

    var gaugesEnabled: List<GaugeType>
        get() {
            val gauges = mPrefs.getString("gauges_enabled", defaultGauges)?.split(",") ?: defaultGauges.split(",")

            return gauges.filter { it.isNotEmpty() }.map { GaugeType.valueOf(it) }
        }

        set(values) {
            val gauges = values.map { it.toString() }.joinToString(",")

            mPrefs.edit().putString("gauges_enabled", gauges).apply()
        }

    private val defaultGauges: String = listOf(GaugeType.RPM, GaugeType.MAP, GaugeType.VOLTAGE, GaugeType.TEMPERATURE)
        .map { it.toString() }.joinToString(",")

    var indicatorsEnabled: List<IndicatorType>
        get() {
            val indicators = mPrefs.getString("indicators_enabled", defaultIndicators)?.split(",") ?: defaultIndicators.split(",")

            return indicators.map { IndicatorType.valueOf(it) }
        }

        set(values) {
            val indicators = values.map { it.toString() }.joinToString(",")

            mPrefs.edit().putString("indicators_enabled", indicators).apply()
        }

    private val defaultIndicators: String = listOf(IndicatorType.GAS_VALVE, IndicatorType.THROTTLE, IndicatorType.FI_FUEL
        , IndicatorType.POWER_VALVE, IndicatorType.STARTER_BLOCKING, IndicatorType.AE, IndicatorType.COOLING_FAN,
        IndicatorType.CHECK_ENGINE, IndicatorType.REV_LIM_FUEL_CUT, IndicatorType.FLOOD_CLEAR_MODE)
        .map { it.toString() }.joinToString(",")
}