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
package org.secu3.android.models.packets

import org.secu3.android.models.packets.params.*
import org.secu3.android.utils.Task

data class ChangeModePacket(
    val descriptor: Char
) : BaseOutputPacket() {

    override fun pack(): String {
        return "${OUTPUT_PACKET_SYMBOL}h$descriptor"
    }


    companion object {

        fun getPacket(task: Task): ChangeModePacket {

            return when (task) {
                Task.Secu3ReadFirmwareInfo -> ChangeModePacket(FirmwareInfoPacket.DESCRIPTOR)
                Task.Secu3ReadSensors -> ChangeModePacket(SensorsPacket.DESCRIPTOR)
                Task.Secu3ReadRawSensors -> ChangeModePacket(AdcRawDatPacket.DESCRIPTOR)
                Task.Secu3ReadEcuErrors -> ChangeModePacket(CheckEngineErrorsPacket.DESCRIPTOR)
                Task.Secu3ReadEcuSavedErrors -> ChangeModePacket(CheckEngineErrorsPacket.DESCRIPTOR)
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

                Task.Secu3ReadFnNameDat -> ChangeModePacket(FnNameDatPacket.DESCRIPTOR)
                else -> ChangeModePacket(SensorsPacket.DESCRIPTOR)
            }

        }

    }
}