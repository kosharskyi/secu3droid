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
package org.secu3.android.utils

import org.secu3.android.models.packets.*

sealed class Task {

    object Secu3ReadSensors : Task()

    object Secu3ReadRawSensors : Task()

    object Secu3ReadFirmwareInfo : Task()

    object Secu3ReadEcuErrors : Task()

    object Secu3ReadEcuSavedErrors : Task()

    object Secu3DiagInput : Task()

    object Secu3ReadStarterParam : Task()

    object Secu3ReadAnglesParam : Task()

    object Secu3ReadIdlingParam : Task()

    object Secu3ReadFunsetParam : Task()

    object Secu3ReadTemperatureParam : Task()

    object Secu3ReadCarburParam : Task()

    object Secu3ReadAdcErrorsCorrectionsParam : Task()

    object Secu3ReadCkpsParam : Task()

    object Secu3ReadKnockParam : Task()

    object Secu3ReadMiscellaneousParam : Task()

    object Secu3ReadChokeControlParam : Task()

    object Secu3ReadSecurityParam : Task()

    object Secu3ReadUniversalOutputsParam : Task()

    object Secu3ReadFuelInjectionParam : Task()

    object Secu3ReadLambdaParam : Task()

    object Secu3ReadAccelerationParam : Task()

    object Secu3ReadGasDoseParam : Task()

    object Secu3ReadFnNameDat : Task()



    object Secu3EnterDiagnostics : Task()
    object Secu3LeaveDiagnostics : Task()




    fun getPacket(): BaseOutputPacket {
        return when (this) {
            Secu3ReadFirmwareInfo, Secu3ReadSensors, Secu3ReadRawSensors, Secu3ReadEcuErrors, Secu3ReadEcuSavedErrors,
            Secu3DiagInput, Secu3ReadStarterParam, Secu3ReadAnglesParam, Secu3ReadIdlingParam, Secu3ReadFunsetParam,
            Secu3ReadTemperatureParam, Secu3ReadCarburParam, Secu3ReadAdcErrorsCorrectionsParam, Secu3ReadCkpsParam,
            Secu3ReadKnockParam, Secu3ReadMiscellaneousParam, Secu3ReadChokeControlParam, Secu3ReadSecurityParam,
            Secu3ReadUniversalOutputsParam, Secu3ReadFuelInjectionParam, Secu3ReadLambdaParam, Secu3ReadAccelerationParam,
            Secu3ReadGasDoseParam,Secu3ReadFnNameDat -> ChangeModePacket.getPacket(this)


            Secu3EnterDiagnostics -> OpCompNc.getEnterDiagPacket()
            Secu3LeaveDiagnostics -> OpCompNc.getLeaveDiagPacket()
        }
    }

}