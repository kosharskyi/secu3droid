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

data class OpCompNc(

    val opData: Int,

    val opCode: Int

) : BaseOutputPacket() {
    
    override fun pack(): String {
        return "${OUTPUT_PACKET_SYMBOL}$DESCRIPTOR${opData.toChar()}${opCode.toChar()}"
    }


    companion object {

        private const val EEPROM_PARAM_SAVE: Int    = 1;
        private const val CE_SAVE_ERRORS: Int       = 2;
        private const val READ_FW_SIG_INFO: Int     = 3;
        private const val LOAD_TABLSET: Int         = 4;  //realtime tables
        private const val SAVE_TABLSET: Int         = 5;  //realtime tables
        private const val DIAGNOST_ENTER: Int       = 6;  //enter diagnostic mode
        private const val DIAGNOST_LEAVE: Int       = 7;  //leave diagnostic mode
        private const val RESET_EEPROM: Int         = 0xCF;//reset EEPROM
        private const val BL_CONFIRM: Int           = 0xCB; //boot loader entering confirmation

        internal const val DESCRIPTOR = 'u'


        fun parse(data: String) = OpCompNc(data[2].toInt(), data[3].toInt())
        
        fun getEnterDiagPacket() = OpCompNc(0, DIAGNOST_ENTER)
        fun getLeaveDiagPacket() = OpCompNc(0, DIAGNOST_LEAVE)

    }
    
    
}
