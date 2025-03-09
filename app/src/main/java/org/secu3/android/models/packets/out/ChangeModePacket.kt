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
package org.secu3.android.models.packets.out

import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.CheckEngineErrorsPacket
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.packets.base.OutputPacket
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
import org.secu3.android.utils.Task

data class ChangeModePacket(
    val nextDescriptor: Char
) : Secu3Packet(), OutputPacket {

    override fun pack(): IntArray {
        val stubByte = 0.toUByte().toInt()

        val data = intArrayOf(
            DESCRIPTOR.code,
            nextDescriptor.code,
            stubByte
        )
        return data
    }


    companion object {

        internal const val DESCRIPTOR = 'h'

        fun getPacket(task: Task): ChangeModePacket {

            return when (task) {
                Task.Secu3ReadFirmwareInfo -> ChangeModePacket(FirmwareInfoPacket.DESCRIPTOR)
                Task.Secu3ReadSensors -> ChangeModePacket(SensorsPacket.DESCRIPTOR)
                Task.Secu3ReadRawSensors -> ChangeModePacket(AdcRawDatPacket.DESCRIPTOR)
                Task.Secu3ReadEcuErrors -> ChangeModePacket(CheckEngineErrorsPacket.DESCRIPTOR)
                Task.Secu3ReadEcuSavedErrors -> ChangeModePacket(CheckEngineSavedErrorsPacket.DESCRIPTOR)
                Task.Secu3DiagInput -> ChangeModePacket(DiagInputPacket.DESCRIPTOR)

                Task.Secu3ReadStarterParam -> ChangeModePacket(StarterParamPacket.DESCRIPTOR)
                Task.Secu3ReadAnglesParam -> ChangeModePacket(AnglesParamPacket.DESCRIPTOR)
                Task.Secu3ReadIdlingParam -> ChangeModePacket(IdlingParamPacket.DESCRIPTOR)
                Task.Secu3ReadFunsetParam -> ChangeModePacket(FunSetParamPacket.DESCRIPTOR)
                Task.Secu3ReadTemperatureParam -> ChangeModePacket(TemperatureParamPacket.DESCRIPTOR)
                Task.Secu3ReadCarburParam -> ChangeModePacket(CarburParamPacket.DESCRIPTOR)
                Task.Secu3ReadAdcErrorsCorrectionsParam -> ChangeModePacket(AdcCorrectionsParamPacket.DESCRIPTOR)
                Task.Secu3ReadCkpsParam -> ChangeModePacket(CkpsParamPacket.DESCRIPTOR)
                Task.Secu3ReadKnockParam -> ChangeModePacket(KnockParamPacket.DESCRIPTOR)
                Task.Secu3ReadMiscellaneousParam -> ChangeModePacket(MiscellaneousParamPacket.DESCRIPTOR)
                Task.Secu3ReadChokeControlParam -> ChangeModePacket(ChokeControlParPacket.DESCRIPTOR)
                Task.Secu3ReadSecurityParam -> ChangeModePacket(SecurityParamPacket.DESCRIPTOR)
                Task.Secu3ReadUniversalOutputsParam -> ChangeModePacket(UniOutParamPacket.DESCRIPTOR)
                Task.Secu3ReadFuelInjectionParam -> ChangeModePacket(InjctrParPacket.DESCRIPTOR)
                Task.Secu3ReadLambdaParam -> ChangeModePacket(LambdaParamPacket.DESCRIPTOR)
                Task.Secu3ReadAccelerationParam -> ChangeModePacket(AccelerationParamPacket.DESCRIPTOR)
                Task.Secu3ReadGasDoseParam -> ChangeModePacket(GasDoseParamPacket.DESCRIPTOR)
                Task.Secu3ReadLtftParam -> ChangeModePacket(LtftParamPacket.DESCRIPTOR)
                Task.Secu3ReadDbwParam -> ChangeModePacket(DbwParamPacket.DESCRIPTOR)

                Task.Secu3ReadFnNameDat -> ChangeModePacket(FnNameDatPacket.DESCRIPTOR)
                else -> ChangeModePacket(SensorsPacket.DESCRIPTOR)
            }
        }
    }
}