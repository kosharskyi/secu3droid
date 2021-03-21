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
import java.util.*

abstract class BaseSecu3Packet {

    protected fun String.get2Bytes(startIndex: Int): Int {
        if (startIndex + 2 > length) {
            throw IllegalArgumentException("Packet too short")
        }

        return this.substring(startIndex, startIndex + 2).binToInt()
    }

    protected fun String.get3Bytes(startIndex: Int): Int {
        if (startIndex + 3 > length) {
            throw IllegalArgumentException("Packet too short")
        }
        return this.substring(startIndex, startIndex + 3).binToInt()
    }

    protected fun String.get4Bytes(startIndex: Int): Int {
        if (startIndex + 4 > length) {
            throw IllegalArgumentException("Packet too short")
        }
        return this.substring(startIndex, startIndex + 4).binToInt()
    }

    private fun String.binToInt(): Int {
        var v = 0
        for (element in this) {
            v = v shl 8
            v = v or element.toInt()
        }
        return v
    }

    protected fun Int.getBitValue(bitNumber: Int): Int {
        return this shr bitNumber and 1
    }

    protected fun Int.setBitValue(value: Boolean, bitNumber: Int): Int {
        return if (value) {
            1.shl(bitNumber).or(this)
        } else {
            1.shl(bitNumber).inv().and(this)
        }
    }

    var speedSensorPulses: Int = 0

    val periodDistance: Float
        get() = (1000.0f / speedSensorPulses)


    companion object {

        private const val TAG = "SecuPacketParser"

        // There are several special reserved symbols in binary mode: 0x21, 0x40,
        // 0x0D, 0x0A
        const val FIBEGIN = 0x21 // '!' indicates beginning of the

        // ingoing packet
        private const val FOBEGIN = 0x40 // '@' indicates beginning of the

        // outgoing packet
        private const val FIOEND = 0x0D // '\r' indicates ending of the

        // ingoing/outgoing packet
        private const val FESC = 0x0A // '\n' Packet escape (FESC)

        // Following bytes are used only in escape sequeces and may appear in the
        // data without any problems
        private const val TFIBEGIN = 0x81 // Transposed FIBEGIN

        private const val TFOBEGIN = 0x82 // Transposed FOBEGIN

        private const val TFIOEND = 0x83 // Transposed FIOEND

        private const val TFESC = 0x84 // Transposed FESC


        const val INPUT_PACKET_SYMBOL = '@'
        const val OUTPUT_PACKET_SYMBOL = '!'
        const val END_PACKET_SYMBOL = '\r'

        internal const val VOLTAGE_MULTIPLIER: Int = 400
        internal const val MAP_MULTIPLIER: Int = 64
        internal const val TEMPERATURE_MULTIPLIER: Int = 4
        internal const val TPS_MULTIPLIER: Int = 2
        internal const val GAS_DOSE_MULTIPLIER: Int = 2
        internal const val ANGLE_DIVIDER: Int = 32
        internal const val PARINJTIM_DIVIDER: Int = 16

        internal const val ADC_MULTIPLIER = 400
        internal const val CHOKE_MULTIPLIER = 2

        internal const val AFR_MULTIPLIER: Int = 128

        const val MAX_PACKET_SIZE = 128

        fun parse(data: String): BaseSecu3Packet? {
            return try {
                when (data[1]) {
                    SensorsPacket.DESCRIPTOR -> SensorsPacket.parse(data)
                    FirmwareInfoPacket.DESCRIPTOR -> FirmwareInfoPacket.parse(data)
                    AdcRawDatPacket.DESCRIPTOR -> AdcRawDatPacket.parse(data)
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

        fun EscRxPacket(packetBuffer: IntArray): IntArray {
            val buf = IntArray(packetBuffer.size)
            var esc = false
            var idx = 0
            for (i in packetBuffer.indices) {
                if (packetBuffer[i] == FESC && i >= 2) {
                    esc = true
                    continue
                }
                if (esc) {
                    esc = false
                    when(packetBuffer[i]) {
                        TFOBEGIN -> buf[idx++] = FOBEGIN
                        TFIOEND -> buf[idx++] = FIOEND
                        TFESC -> buf[idx++] = FESC
                    }
                } else buf[idx++] = packetBuffer[i]
            }
            return buf
        }

        fun EscTxPacket(packetBuffer: String): String {
            val buf = ArrayList<Int>(packetBuffer.length - 3)
            for (i in packetBuffer.indices) {
                if (i >= 2 && i < packetBuffer.length - 1) {
                    if (packetBuffer[i].toInt() == FIBEGIN) {
                        buf.add(FESC)
                        buf.add(TFIBEGIN)
                        continue
                    } else if (packetBuffer[i].toInt() == FIOEND) {
                        buf.add(FESC)
                        buf.add(FIOEND)
                        continue
                    } else if (packetBuffer[i].toInt() == FESC) {
                        buf.add(FESC)
                        buf.add(TFESC)
                        continue
                    }
                }
                buf.add(packetBuffer[i].toInt())
            }
            val outBuf = IntArray(buf.size)
            for (i in buf.indices) {
                outBuf[i] = buf[i]
            }
            return String(outBuf, 0, outBuf.size)
        }

    }

}