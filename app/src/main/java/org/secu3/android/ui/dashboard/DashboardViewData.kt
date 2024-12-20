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

import org.secu3.android.models.packets.input.SensorsPacket

data class DashboardViewData(
    var topLeft: Float,
    var topRight: Float,
    var center: Float,
    var bottomLeft: Float,
    var bottomRight: Float,

    var ledCheckEngine: Boolean,
    var ledGasoline: Boolean,
    var ledEco: Boolean,
    var ledPower: Boolean,
    var ledChoke: Boolean,
    var ledFan: Boolean,
) {

    companion object {

        fun inflate(dashboardConfig: DashboardConfig, packet: SensorsPacket): DashboardViewData {
            val center = dashboardConfig.center.type.getGaugeItem(packet)
            val topLeft = dashboardConfig.topLeft.type.getGaugeItem(packet)
            val topRight = dashboardConfig.topRight.type.getGaugeItem(packet)
            val bottomLeft = dashboardConfig.bottomLeft.type.getGaugeItem(packet)
            val bottomRight = dashboardConfig.bottomRight.type.getGaugeItem(packet)

            return DashboardViewData(
                topLeft, topRight, center, bottomLeft, bottomRight,
                packet.checkEngineBit > 0,
                packet.gasBit > 0,
                packet.ephhValveBit > 0,
                packet.epmValveBit > 0,
                packet.carbBit > 0,
                packet.coolFanBit > 0,

                )
        }

    }

}
