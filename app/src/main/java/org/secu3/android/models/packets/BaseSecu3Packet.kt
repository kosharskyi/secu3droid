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
package org.secu3.android.models.packets

import org.secu3.android.models.packets.params.*

abstract class BaseSecu3Packet {

    protected var packetCrc: UByteArray = UByteArray(2)

    protected fun String.get2Bytes(startIndex: Int): Int {
        if (startIndex + 2 > length) {
            throw IllegalArgumentException("Packet too short; request ${startIndex + 2} but length is $length")
        }

        return this.substring(startIndex, startIndex + 2).binToInt()
    }

    protected fun String.get3Bytes(startIndex: Int): Int {
        if (startIndex + 3 > length) {
            throw IllegalArgumentException("Packet too short; request ${startIndex + 3} but length is $length")
        }
        return this.substring(startIndex, startIndex + 3).binToInt()
    }

    protected fun String.get4Bytes(startIndex: Int): Int {
        if (startIndex + 4 > length) {
            throw IllegalArgumentException("Packet too short; request ${startIndex + 4} but length is $length")
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

        const val MAX_PACKET_SIZE = 250

//        fun parse(data: String, notEscaped: IntArray): BaseSecu3Packet? {
//            return try {
//                when (data[1]) {
//                    SensorsPacket.DESCRIPTOR -> SensorsPacket.parse(data)
//                    FirmwareInfoPacket.DESCRIPTOR -> FirmwareInfoPacket.parse(data)
//                    AdcRawDatPacket.DESCRIPTOR -> AdcRawDatPacket.parse(data, firmwarePacket)
//                    CheckEngineErrorsPacket.DESCRIPTOR -> CheckEngineErrorsPacket.parse(data)
//                    CheckEngineSavedErrorsPacket.DESCRIPTOR -> CheckEngineSavedErrorsPacket.parse(data)
//                    DiagInputPacket.DESCRIPTOR -> DiagInputPacket.parse(data)
//
//                    StarterParamPacket.DESCRIPTOR -> StarterParamPacket.parse(data)
//                    AnglesParamPacket.DESCRIPTOR -> AnglesParamPacket.parse(data)
//                    IdlingParamPacket.DESCRIPTOR -> IdlingParamPacket.parse(data)
//                    FunSetParamPacket.DESCRIPTOR -> FunSetParamPacket.parse(data)
//                    TemperatureParamPacket.DESCRIPTOR -> TemperatureParamPacket.parse(data)
//                    CarburParamPacket.DESCRIPTOR -> CarburParamPacket.parse(data)
//                    AdcCorrectionsParamPacket.DESCRIPTOR -> AdcCorrectionsParamPacket.parse(data)
//                    CkpsParamPacket.DESCRIPTOR -> CkpsParamPacket.parse(data)
//                    KnockParamPacket.DESCRIPTOR -> KnockParamPacket.parse(data)
//                    MiscellaneousParamPacket.DESCRIPTOR -> MiscellaneousParamPacket.parse(data)
//                    ChokeControlParPacket.DESCRIPTOR -> ChokeControlParPacket.parse(data)
//                    SecurityParamPacket.DESCRIPTOR -> SecurityParamPacket.parse(data)
//                    UniOutParamPacket.DESCRIPTOR -> UniOutParamPacket.parse(data)
//                    InjctrParPacket.DESCRIPTOR -> InjctrParPacket.parse(data)
//                    LambdaParamPacket.DESCRIPTOR -> LambdaParamPacket.parse(data)
//                    AccelerationParamPacket.DESCRIPTOR -> AccelerationParamPacket.parse(data)
//                    GasDoseParamPacket.DESCRIPTOR -> GasDoseParamPacket.parse(data)
//
//                    FnNameDatPacket.DESCRIPTOR -> FnNameDatPacket.parse(data)
//                    OpCompNc.DESCRIPTOR -> OpCompNc.parse(data)
//
//                    else -> null
//                }
//
////                packet?.apply {
////                    packetCrc[0] = notEscaped[notEscaped.lastIndex - 2].toUByte()
////                    packetCrc[1] = notEscaped[notEscaped.lastIndex - 1].toUByte()
////                }
////
////                if (packet != null) {
////                    val checksum = PacketUtils.calculateChecksum(data.substring(2, data.length))
////
////                    if (packet.packetCrc[0] == checksum[1] && packet.packetCrc[1] == checksum[0]) {
////                        return packet
////                    }
////                }
////
////                throw java.lang.IllegalArgumentException("checksumm doesn't match")
//            } catch (e: IllegalArgumentException) {
//                e.printStackTrace()
//                null
//            } catch (e: StringIndexOutOfBoundsException) {
//                e.printStackTrace()
//                null
//            }
//        }
    }
}