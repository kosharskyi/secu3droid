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

import org.secu3.android.models.packets.base.BaseOutputPacket

data class OpCompNc(

    val opData: Int,

    val opCode: Int

) : BaseOutputPacket() {
    
    override fun pack(): IntArray {
        return intArrayOf(
            DESCRIPTOR.code,
            opData,
            opCode
        )
    }

    val isEepromParamSave: Boolean
        get() = opCode == EEPROM_PARAM_SAVE

    companion object {

        private const val EEPROM_PARAM_SAVE: Int    = 1
        private const val CE_SAVE_ERRORS: Int       = 2
        private const val READ_FW_SIG_INFO: Int     = 3
        private const val LOAD_TABLSET: Int         = 4  //realtime tables
        private const val SAVE_TABLSET: Int         = 5  //realtime tables
        private const val DIAGNOST_ENTER: Int       = 6  //enter diagnostic mode
        private const val DIAGNOST_LEAVE: Int       = 7  //leave diagnostic mode
        private const val RESET_EEPROM: Int         = 0xCF //reset EEPROM
        private const val BL_CONFIRM: Int           = 0xCB //boot loader entering confirmation
        private const val OPCODE_BL_MANSTART        = 0xC8 //boot loader started manually (using a jumper)
        private const val OPDATA_BL_MANSTART        = 0x8C //data for OPCODE_BL_MANSTART
        private const val OPCODE_RESET_LTFT         = 0xCA //Reset LTFT map
        private const val OPDATA_RESET_LTFT         = 0xBB //Reset LTFT map
        private const val OPCODE_SAVE_LTFT          = 9  //save LTFT


        internal const val DESCRIPTOR = 'u'


        fun parse(data: IntArray) = OpCompNc(data[2], data[3])
        
        fun getEnterDiagCommand() = OpCompNc(0, DIAGNOST_ENTER)
        fun getLeaveDiagCommand() = OpCompNc(0, DIAGNOST_LEAVE)

        fun getSaveEepromCommand() = OpCompNc(0, EEPROM_PARAM_SAVE)

    }
    
    
}
