/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
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
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorType
import org.threeten.bp.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPrefs @Inject constructor(@ApplicationContext private val ctx: Context) {

    private var mPrefs: SharedPreferences = ctx.getSharedPreferences("app_shared_prefs", MODE_PRIVATE)


    var lastAppVersionCheck: LocalDateTime
        get() {
            return mPrefs.getString("last_app_version_check_stamp", LocalDateTime.MIN.toString()).let { LocalDateTime.parse(it) }
        }
        set(value) = mPrefs.edit().putString("last_app_version_check_stamp", value.toString()).apply()

    var indicatorsEnabled: List<IndicatorType>
        get() {
            val indicators = mPrefs.getString("indicators_enabled", defaultIndicators)?.split(",") ?: defaultIndicators.split(",")

            return indicators.filter { it.isNotEmpty() }.map { IndicatorType.valueOf(it) }
        }

        set(values) {
            val indicators = values.map { it.toString() }.joinToString(",")

            mPrefs.edit().putString("indicators_enabled", indicators).apply()
        }

    private val defaultIndicators: String = listOf(
        IndicatorType.GAS_VALVE, IndicatorType.THROTTLE, IndicatorType.FI_FUEL
        , IndicatorType.POWER_VALVE, IndicatorType.STARTER_BLOCKING, IndicatorType.AE, IndicatorType.COOLING_FAN,
        IndicatorType.CHECK_ENGINE, IndicatorType.REV_LIM_FUEL_CUT, IndicatorType.FLOOD_CLEAR_MODE)
        .map { it.toString() }.joinToString(",")
}