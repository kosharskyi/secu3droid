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

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class FirmwareInfoPacket(
    var tag: String = "",
    var options: Int = 0,
    var version: Char = '0'
) : BaseSecu3Packet() {

    val isObdSupported: Boolean
        get() = options.getBitValue(COPT_OBD_SUPPORT) > 0

    val isATMEGA1284: Boolean
        get() = options.getBitValue(COPT_ATMEGA1284) > 0

    val isSplitAngle: Boolean
        get() = options.getBitValue(COPT_SPLIT_ANGLE) > 0

    val isTPIC8101: Boolean
        get() = options.getBitValue(COPT_TPIC8101) > 0

    val isCamSyncSupported: Boolean
        get() = options.getBitValue(COPT_CAM_SYNC) > 0

    val isDwellControlSupported: Boolean
        get() = options.getBitValue(COPT_DWELL_CONTROL) > 0

    val isCoolingFanPwmEnabled: Boolean
        get() = options.getBitValue(COPT_COOLINGFAN_PWM) > 0

    val isRealTimeTablesEnabled: Boolean
        get() = options.getBitValue(COPT_REALTIME_TABLES) > 0

    val isIccAvrCompiler: Boolean
        get() = options.getBitValue(COPT_ICCAVR_COMPILER) > 0

    val isAvrGccCompiler: Boolean
        get() = options.getBitValue(COPT_AVRGCC_COMPILER) > 0

    val isDebugVariablesEnabled: Boolean
        get() = options.getBitValue(COPT_DEBUG_VARIABLES) > 0

    val isPhaseSensorEnabled: Boolean
        get() = options.getBitValue(COPT_PHASE_SENSOR) > 0

    val isPhasedIgnitionEnabled: Boolean
        get() = options.getBitValue(COPT_PHASED_IGNITION) > 0

    val isFuelPumpEnabled: Boolean
        get() = options.getBitValue(COPT_FUEL_PUMP) > 0

    val isThermistorCsEnabled: Boolean
        get() = options.getBitValue(COPT_THERMISTOR_CS) > 0

    val isSecu3T: Boolean
        get() = options.getBitValue(COPT_SECU3T) > 0

    val isDiagnosticsEnabled: Boolean
        get() = options.getBitValue(COPT_DIAGNOSTICS) > 0

    val isHallOutputEnabled: Boolean
        get() = options.getBitValue(COPT_HALL_OUTPUT) > 0

    val isRev9Board: Boolean
        get() = options.getBitValue(COPT_REV9_BOARD) > 0

    val isStroboscopeEnabled: Boolean
        get() = options.getBitValue(COPT_STROBOSCOPE) > 0

    val isSmControlEnabled: Boolean
        get() = options.getBitValue(COPT_SM_CONTROL) > 0

    val isVRef5VEnabled: Boolean
        get() = options.getBitValue(COPT_VREF_5V) > 0

    val isHallSyncEnabled: Boolean
        get() = options.getBitValue(COPT_HALL_SYNC) > 0

    val isUartBinaryEnabled: Boolean
        get() = options.getBitValue(COPT_UART_BINARY) > 0

    val isCkps2ChignEnabled: Boolean
        get() = options.getBitValue(COPT_CKPS_2CHIGN) > 0

    val isATMEGA644: Boolean
        get() = options.getBitValue(COPT_ATMEGA644) > 0

    val isFuelInjectEnabled: Boolean
        get() = options.getBitValue(COPT_FUEL_INJECT) > 0

    val isGdControlEnabled: Boolean
        get() = options.getBitValue(COPT_GD_CONTROL) > 0

    val isCarbAfrEnabled: Boolean
        get() = options.getBitValue(COPT_CARB_AFR) > 0

    val isCkpsNplus1Enabled: Boolean
        get() = options.getBitValue(COPT_CKPS_NPLUS1) > 0


    companion object {

        internal const val DESCRIPTOR = 'y'

        fun parse(data: String) = FirmwareInfoPacket().apply {
            tag = data.substring(2, 50).toByteArray(StandardCharsets.ISO_8859_1).toString(Charset.forName("IBM866"))
            options = data.get4Bytes(50)
            version = data[54]
        }

        private const val COPT_OBD_SUPPORT = 0
        private const val COPT_ATMEGA1284 = 1
        private const val COPT_ATMEGA64 = 2   //  Left for compatibility
        private const val COPT_ATMEGA128 = 3   // Left for compatibility
        private const val COPT_SPLIT_ANGLE = 4
        private const val COPT_TPIC8101 = 5
        private const val COPT_CAM_SYNC = 6
        private const val COPT_DWELL_CONTROL = 7
        private const val COPT_COOLINGFAN_PWM = 8
        private const val COPT_REALTIME_TABLES = 9
        private const val COPT_ICCAVR_COMPILER = 10
        private const val COPT_AVRGCC_COMPILER = 11
        private const val COPT_DEBUG_VARIABLES = 12
        private const val COPT_PHASE_SENSOR = 13
        private const val COPT_PHASED_IGNITION = 14
        private const val COPT_FUEL_PUMP = 15
        private const val COPT_THERMISTOR_CS = 16
        private const val COPT_SECU3T = 17
        private const val COPT_DIAGNOSTICS = 18
        private const val COPT_HALL_OUTPUT = 19
        private const val COPT_REV9_BOARD = 20
        private const val COPT_STROBOSCOPE = 21
        private const val COPT_SM_CONTROL = 22
        private const val COPT_VREF_5V = 23
        private const val COPT_HALL_SYNC = 24
        private const val COPT_UART_BINARY = 25
        private const val COPT_CKPS_2CHIGN = 26
        private const val COPT_ATMEGA644 = 27
        private const val COPT_FUEL_INJECT = 28
        private const val COPT_GD_CONTROL = 29
        private const val COPT_CARB_AFR = 30
        private const val COPT_CKPS_NPLUS1 = 31

    }
}