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
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.out.CheckEngineSavedErrorsPacket
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
import org.secu3.android.models.packets.out.params.DbwParamPacket
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

    fun parse(firmwarePacket: FirmwareInfoPacket?): InputPacket? {
        return try {
            val packetData = data.sliceArray(0 until data.size - 2)

            return when (packetData[1].toChar()) {
                SensorsPacket.DESCRIPTOR -> SensorsPacket()
                FirmwareInfoPacket.DESCRIPTOR -> FirmwareInfoPacket()
                AdcRawDatPacket.DESCRIPTOR -> AdcRawDatPacket()
                CheckEngineErrorsPacket.DESCRIPTOR -> CheckEngineErrorsPacket()
                CheckEngineSavedErrorsPacket.DESCRIPTOR -> CheckEngineSavedErrorsPacket()
                DiagInputPacket.DESCRIPTOR -> DiagInputPacket()

                StarterParamPacket.DESCRIPTOR -> StarterParamPacket()
                AnglesParamPacket.DESCRIPTOR -> AnglesParamPacket()
                IdlingParamPacket.DESCRIPTOR -> IdlingParamPacket()
                FunSetParamPacket.DESCRIPTOR -> FunSetParamPacket()
                TemperatureParamPacket.DESCRIPTOR -> TemperatureParamPacket()
                CarburParamPacket.DESCRIPTOR -> CarburParamPacket()
                AdcCorrectionsParamPacket.DESCRIPTOR -> AdcCorrectionsParamPacket()
                CkpsParamPacket.DESCRIPTOR -> CkpsParamPacket()
                KnockParamPacket.DESCRIPTOR -> KnockParamPacket()
                MiscellaneousParamPacket.DESCRIPTOR -> MiscellaneousParamPacket()
                ChokeControlParPacket.DESCRIPTOR -> ChokeControlParPacket()
                SecurityParamPacket.DESCRIPTOR -> SecurityParamPacket()
                UniOutParamPacket.DESCRIPTOR -> UniOutParamPacket()
                InjctrParPacket.DESCRIPTOR -> InjctrParPacket()
                LambdaParamPacket.DESCRIPTOR -> LambdaParamPacket()
                AccelerationParamPacket.DESCRIPTOR -> AccelerationParamPacket()
                GasDoseParamPacket.DESCRIPTOR -> GasDoseParamPacket()
                LtftParamPacket.DESCRIPTOR -> LtftParamPacket()
                DbwParamPacket.DESCRIPTOR -> DbwParamPacket()

                FnNameDatPacket.DESCRIPTOR -> FnNameDatPacket()
                OpCompNc.DESCRIPTOR -> OpCompNc()

                else -> null
            }?.parse(packetData)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        } catch (e: StringIndexOutOfBoundsException) {
            e.printStackTrace()
            null
        }
    }
}