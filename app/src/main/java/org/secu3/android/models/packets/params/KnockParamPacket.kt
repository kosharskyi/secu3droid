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
package org.secu3.android.models.packets.params

import org.secu3.android.models.packets.BaseOutputPacket
import org.secu3.android.utils.getBitValue
import kotlin.math.roundToInt

data class KnockParamPacket(

    var useKnockChannel: Int = 0,
    var bpfFrequency: Int = 0,
    var kWndBeginAngle: Float = 0f,
    var kWndEndAngle: Float = 0f,
    var intTimeCost: Int = 0,

    var retardStep: Float = 0f,
    var advanceStep: Float = 0f,
    var maxRetard: Float = 0f,
    var threshold: Float = 0f,
    var recoveryDelay: Int = 0,
    var selectedChanels: Int = 0,         //!< 1 bit per channel (cylinder). 0 - 1st KS, 1 - 2nd KS
    var knkctlThrd: Float = 0f,

    ) : BaseOutputPacket() {

    fun isKnockChanelSelected(knockPosition: Int): Boolean {
        return selectedChanels.getBitValue(knockPosition) > 0
    }

    fun selectKnockChanel(knockChanel: Int, isSelected: Boolean) {
        if (isSelected) {
            (1 shl knockChanel).or(selectedChanels)
        } else {
            (1 shl knockChanel).inv().and(selectedChanels)
        }
    }

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += useKnockChannel.toChar()
        data += bpfFrequency.toChar()

        data += kWndBeginAngle.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += kWndEndAngle.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += intTimeCost.toChar()

        data += retardStep.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += advanceStep.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += maxRetard.times(ANGLE_DIVIDER).roundToInt().write2Bytes()
        data += threshold.times(VOLTAGE_MULTIPLIER).roundToInt().write2Bytes()

        data += recoveryDelay.toChar()

        data += selectedChanels.toChar()
        data += knkctlThrd.times(TEMPERATURE_MULTIPLIER).roundToInt().write2Bytes()

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'w'

        fun parse(data: String) = KnockParamPacket().apply {
            useKnockChannel = data[2].code
            bpfFrequency = data[3].code
            kWndBeginAngle = data.get2Bytes(4).toShort().toFloat() / ANGLE_DIVIDER
            kWndEndAngle = data.get2Bytes(6).toShort().toFloat() / ANGLE_DIVIDER
            intTimeCost = data[8].code

            retardStep = data.get2Bytes(9).toFloat() / ANGLE_DIVIDER
            advanceStep = data.get2Bytes(11).toFloat() / ANGLE_DIVIDER
            maxRetard = data.get2Bytes(13).toFloat() / ANGLE_DIVIDER
            threshold = data.get2Bytes(15).toFloat() / VOLTAGE_MULTIPLIER
            recoveryDelay = data[17].code
            selectedChanels = data[18].code
            knkctlThrd = data.get2Bytes(19).toFloat() / TEMPERATURE_MULTIPLIER

            if (data.length == 21) {
                return@apply
            }

            unhandledParams = data.substring(21)
        }
    }
}
