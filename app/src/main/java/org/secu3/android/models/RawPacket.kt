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

import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.base.BaseSecu3Packet
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.CheckEngineSavedErrorsPacket
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket

data class RawPacket(val data: IntArray)  {

    fun parse(firmwarePacket: FirmwareInfoPacket?): BaseSecu3Packet? {
        return try {
            val packetData = data.sliceArray(0 until data.size - 2)

            return when (packetData[1].toChar()) {
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
                LtftParamPacket.DESCRIPTOR -> LtftParamPacket.parse(packetData)

                FnNameDatPacket.DESCRIPTOR -> FnNameDatPacket.parse(packetData)
                OpCompNc.DESCRIPTOR -> OpCompNc.parse(packetData)

                else -> null
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        } catch (e: StringIndexOutOfBoundsException) {
            e.printStackTrace()
            null
        }
    }
}