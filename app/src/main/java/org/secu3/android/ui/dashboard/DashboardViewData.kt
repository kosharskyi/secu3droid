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

package org.secu3.android.ui.dashboard

import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.utils.prefs.UserPrefs

data class DashboardViewData(
    var load: Float,
    var map: Float,
    var tps: Float,
    var temperature: Float,
    var voltage: Float,
    var currentAngle: Float,
    var afr: Float,
    var oilTemperature: Float,
    var fuelLevel: Float,
    var intakeAirTemperature: Float,
    var knockRetard: Float,

    var ledCheckEngine: Boolean,
    var ledGasoline: Boolean,
    var ledEco: Boolean,
    var ledPower: Boolean,
    var ledChoke: Boolean,
    var ledFan: Boolean,
    var ledRevLimit: Boolean,
    val primaryLeft: DashboardGaugeReading = DashboardGaugeReading(UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE, 0f),
    val primaryRight: DashboardGaugeReading = DashboardGaugeReading(UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE, 0f),
    val injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
) {

    companion object {

        fun inflate(
            packet: SensorsPacket,
            leftGauge: GaugeType = UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE,
            rightGauge: GaugeType = UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE,
            injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
        ): DashboardViewData {
            return DashboardViewData(
                load = packet.load,
                map = packet.map,
                tps = packet.tps,
                temperature = packet.temperature,
                voltage = packet.voltage,
                currentAngle = packet.currentAngle,
                afr = packet.afr,
                oilTemperature = packet.ots,
                fuelLevel = packet.ftls,
                intakeAirTemperature = packet.airtempSensor,
                knockRetard = packet.knockRetard,
                ledCheckEngine = packet.checkEngineBit > 0,
                ledGasoline = packet.gasBit > 0,
                ledEco = packet.ephhValveBit > 0,
                ledPower = packet.epmValveBit > 0,
                ledChoke = packet.carbBit > 0,
                ledFan = packet.coolFanBit > 0,
                ledRevLimit = packet.fc_revlim > 0,
                primaryLeft = DashboardGaugeReading(leftGauge, displayValue(leftGauge, packet, injectionTimingMode)),
                primaryRight = DashboardGaugeReading(rightGauge, displayValue(rightGauge, packet, injectionTimingMode)),
                injectionTimingMode = injectionTimingMode,
            )
        }

        private fun displayValue(
            gauge: GaugeType,
            packet: SensorsPacket,
            injectionTimingMode: InjectionTimingMode,
        ): Float {
            val rawValue = gauge.getGaugeItem(packet)
            return if (gauge == GaugeType.BEGIN_INJ_PHASE || gauge == GaugeType.END_INJ_PHASE) {
                injectionTimingMode.transform(rawValue)
            } else {
                rawValue
            }
        }
    }

}

data class DashboardGaugeReading(val type: GaugeType, val value: Float)
