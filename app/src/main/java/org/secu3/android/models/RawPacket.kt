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

data class RawPacket(val data: String, val notEscaped: IntArray)  {

    fun parse(firmwarePacket: FirmwareInfoPacket?): BaseSecu3Packet? {
        return try {
            when (data[1]) {
                SensorsPacket.DESCRIPTOR -> SensorsPacket.parse(data)
                FirmwareInfoPacket.DESCRIPTOR -> FirmwareInfoPacket.parse(data)
                AdcRawDatPacket.DESCRIPTOR -> AdcRawDatPacket.parse(data, firmwarePacket)
                CheckEngineErrorsPacket.DESCRIPTOR -> CheckEngineErrorsPacket.parse(data)
                CheckEngineSavedErrorsPacket.DESCRIPTOR -> CheckEngineSavedErrorsPacket.parse(data)
                DiagInputPacket.DESCRIPTOR -> DiagInputPacket.parse(data)

                StarterParamPacket.DESCRIPTOR -> StarterParamPacket.parse(data)
                AnglesParamPacket.DESCRIPTOR -> AnglesParamPacket.parse(data)
                IdlingParamPacket.DESCRIPTOR -> IdlingParamPacket.parse(data)
                FunSetParamPacket.DESCRIPTOR -> FunSetParamPacket.parse(data)
                TemperatureParamPacket.DESCRIPTOR -> TemperatureParamPacket.parse(data)
                CarburParamPacket.DESCRIPTOR -> CarburParamPacket.parse(data)
                AdcCorrectionsParamPacket.DESCRIPTOR -> AdcCorrectionsParamPacket.parse(data)
                CkpsParamPacket.DESCRIPTOR -> CkpsParamPacket.parse(data)
                KnockParamPacket.DESCRIPTOR -> KnockParamPacket.parse(data)
                MiscellaneousParamPacket.DESCRIPTOR -> MiscellaneousParamPacket.parse(data)
                ChokeControlParPacket.DESCRIPTOR -> ChokeControlParPacket.parse(data)
                SecurityParamPacket.DESCRIPTOR -> SecurityParamPacket.parse(data)
                UniOutParamPacket.DESCRIPTOR -> UniOutParamPacket.parse(data)
                InjctrParPacket.DESCRIPTOR -> InjctrParPacket.parse(data)
                LambdaParamPacket.DESCRIPTOR -> LambdaParamPacket.parse(data)
                AccelerationParamPacket.DESCRIPTOR -> AccelerationParamPacket.parse(data)
                GasDoseParamPacket.DESCRIPTOR -> GasDoseParamPacket.parse(data)

                FnNameDatPacket.DESCRIPTOR -> FnNameDatPacket.parse(data)
                OpCompNc.DESCRIPTOR -> OpCompNc.parse(data)

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