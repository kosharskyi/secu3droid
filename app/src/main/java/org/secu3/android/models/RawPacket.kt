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

package org.secu3.android.models

import android.util.Log
import org.secu3.android.models.packets.AdcRawDatPacket
import org.secu3.android.models.packets.BaseSecu3Packet
import org.secu3.android.models.packets.CheckEngineErrorsPacket
import org.secu3.android.models.packets.CheckEngineSavedErrorsPacket
import org.secu3.android.models.packets.DiagInputPacket
import org.secu3.android.models.packets.FirmwareInfoPacket
import org.secu3.android.models.packets.FnNameDatPacket
import org.secu3.android.models.packets.OpCompNc
import org.secu3.android.models.packets.SensorsPacket
import org.secu3.android.models.packets.params.AccelerationParamPacket
import org.secu3.android.models.packets.params.AdcCorrectionsParamPacket
import org.secu3.android.models.packets.params.AnglesParamPacket
import org.secu3.android.models.packets.params.CarburParamPacket
import org.secu3.android.models.packets.params.ChokeControlParPacket
import org.secu3.android.models.packets.params.CkpsParamPacket
import org.secu3.android.models.packets.params.FunSetParamPacket
import org.secu3.android.models.packets.params.GasDoseParamPacket
import org.secu3.android.models.packets.params.IdlingParamPacket
import org.secu3.android.models.packets.params.InjctrParPacket
import org.secu3.android.models.packets.params.KnockParamPacket
import org.secu3.android.models.packets.params.LambdaParamPacket
import org.secu3.android.models.packets.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.params.SecurityParamPacket
import org.secu3.android.models.packets.params.StarterParamPacket
import org.secu3.android.models.packets.params.TemperatureParamPacket
import org.secu3.android.models.packets.params.UniOutParamPacket
import org.secu3.android.utils.PacketUtils

data class RawPacket(val data: String)  {

    fun parse(firmwarePacket: FirmwareInfoPacket?): BaseSecu3Packet? {
        return try {
            val packetData = data.substring(0, data.length - 2)

            val packet = when (packetData[1]) {
                SensorsPacket.DESCRIPTOR -> SensorsPacket.parse(packetData)
                FirmwareInfoPacket.DESCRIPTOR -> FirmwareInfoPacket.parse(packetData)
                AdcRawDatPacket.DESCRIPTOR -> AdcRawDatPacket.parse(data, firmwarePacket)
                CheckEngineErrorsPacket.DESCRIPTOR -> CheckEngineErrorsPacket.parse(packetData)
                CheckEngineSavedErrorsPacket.DESCRIPTOR -> CheckEngineSavedErrorsPacket.parse(packetData)
                DiagInputPacket.DESCRIPTOR -> DiagInputPacket.parse(packetData)

                StarterParamPacket.DESCRIPTOR -> StarterParamPacket.parse(packetData)
                AnglesParamPacket.DESCRIPTOR -> AnglesParamPacket.parse(packetData)
                IdlingParamPacket.DESCRIPTOR -> IdlingParamPacket.parse(packetData)
                FunSetParamPacket.DESCRIPTOR -> FunSetParamPacket.parse(packetData)
                TemperatureParamPacket.DESCRIPTOR -> TemperatureParamPacket.parse(packetData)
                CarburParamPacket.DESCRIPTOR -> CarburParamPacket.parse(packetData)
                AdcCorrectionsParamPacket.DESCRIPTOR -> AdcCorrectionsParamPacket.parse(packetData)
                CkpsParamPacket.DESCRIPTOR -> CkpsParamPacket.parse(packetData)
                KnockParamPacket.DESCRIPTOR -> KnockParamPacket.parse(packetData)
                MiscellaneousParamPacket.DESCRIPTOR -> MiscellaneousParamPacket.parse(packetData)
                ChokeControlParPacket.DESCRIPTOR -> ChokeControlParPacket.parse(packetData)
                SecurityParamPacket.DESCRIPTOR -> SecurityParamPacket.parse(packetData)
                UniOutParamPacket.DESCRIPTOR -> UniOutParamPacket.parse(packetData)
                InjctrParPacket.DESCRIPTOR -> InjctrParPacket.parse(packetData)
                LambdaParamPacket.DESCRIPTOR -> LambdaParamPacket.parse(packetData)
                AccelerationParamPacket.DESCRIPTOR -> AccelerationParamPacket.parse(packetData)
                GasDoseParamPacket.DESCRIPTOR -> GasDoseParamPacket.parse(packetData)

                FnNameDatPacket.DESCRIPTOR -> FnNameDatPacket.parse(packetData)
                OpCompNc.DESCRIPTOR -> OpCompNc.parse(packetData)

                else -> null
            }

            packet?.apply {
                packetCrc[0] = data[data.lastIndex - 1].code.toUByte()
                packetCrc[1] = data[data.lastIndex].code.toUByte()
            }

            if (packet != null) {
                val checksum = PacketUtils.calculateChecksum(data.substring(2, data.length - 2))

                if (packet.packetCrc[0] == checksum[1] && packet.packetCrc[1] == checksum[0]) {
                    return packet
                } else {
                    Log.e("RawPacket", "checksum is not valid")
                }
            }

            return null
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        } catch (e: StringIndexOutOfBoundsException) {
            e.printStackTrace()
            null
        }
    }
}