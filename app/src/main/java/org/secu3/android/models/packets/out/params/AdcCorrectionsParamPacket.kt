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
package org.secu3.android.models.packets.out.params

import org.secu3.android.models.packets.base.BaseOutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import kotlin.math.roundToInt

data class AdcCorrectionsParamPacket(

    var adcFlags: Int = 0,
    var mapAdcFactor: Float = 0f,
    var mapAdcCorrection: Float = 0f,
    var ubatAdcFactor: Float = 0f,
    var ubatAdcCorrection: Float = 0f,
    var tempAdcFactor: Float = 0f,
    var tempAdcCorrection: Float = 0f,
    var tpsAdcFactor: Float = 0f,
    var tpsAdcCorrection: Float = 0f,
    var ai1AdcFactor: Float = 0f,
    var ai1AdcCorrection: Float = 0f,
    var ai2AdcFactor: Float = 0f,
    var ai2AdcCorrection: Float = 0f,
    var ai3AdcFactor: Float = 0f,
    var ai3AdcCorrection: Float = 0f,
    var ai4AdcFactor: Float = 0f,
    var ai4AdcCorrection: Float = 0f,
    var ai5AdcFactor: Float = 0f,
    var ai5AdcCorrection: Float = 0f,
    var ai6AdcFactor: Float = 0f,
    var ai6AdcCorrection: Float = 0f,
    var ai7AdcFactor: Float = 0f,
    var ai7AdcCorrection: Float = 0f,
    var ai8AdcFactor: Float = 0f,
    var ai8AdcCorrection: Float = 0f,

) : BaseOutputPacket() {

    var adcCompMode: Boolean
        get() = adcFlags.getBitValue(0) > 0
        set(value) {
            adcFlags = adcFlags.setBitValue(value, 0)
        }

    override fun pack(): String {
        var data = "$DESCRIPTOR"

        data += adcFlags.toChar()

        data += mapAdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        mapAdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) mapAdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ubatAdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ubatAdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ubatAdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += tempAdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        tempAdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) tempAdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += tpsAdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        tpsAdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) tpsAdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai1AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai1AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai1AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai2AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai2AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai2AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai3AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai3AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai3AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai4AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai4AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai4AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai5AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai5AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai5AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai6AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai6AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai6AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai7AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai7AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai7AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += ai8AdcFactor.times(FACTOR_DIVIDER).roundToInt().write2Bytes()
        ai8AdcCorrection.unaryMinus().div(ADC_DISCRETE).roundToInt().let {
            val factor = if (adcCompMode) ai8AdcFactor else 1.0f
            data += FACTOR_DIVIDER.times(0.5f - it.times(factor)).roundToInt().write4Bytes()
        }

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'r'

        private const val FACTOR_DIVIDER = 16384
        private const val ADC_DISCRETE = 0.0025f

        fun parse(data: String) = AdcCorrectionsParamPacket().apply {
            adcFlags = data.get1Byte()

            mapAdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                mapAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / mapAdcFactor
                mapAdcCorrection *= ADC_DISCRETE
            }

            ubatAdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ubatAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ubatAdcFactor
                ubatAdcCorrection *= ADC_DISCRETE
            }

            tempAdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                tempAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / tempAdcFactor
                tempAdcCorrection *= ADC_DISCRETE
            }

            tpsAdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                tpsAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / tpsAdcFactor
                tpsAdcCorrection *= ADC_DISCRETE
            }

            ai1AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai1AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai1AdcFactor
                ai1AdcCorrection *= ADC_DISCRETE
            }

            ai2AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai2AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai2AdcFactor
                ai2AdcCorrection *= ADC_DISCRETE
            }

            ai3AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai3AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) /ai3AdcFactor
                ai3AdcCorrection *= ADC_DISCRETE
            }

            ai4AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai4AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai4AdcFactor
                ai4AdcCorrection *= ADC_DISCRETE
            }

            ai5AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai5AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai5AdcFactor
                ai5AdcCorrection *= ADC_DISCRETE
            }

            ai6AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai6AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai6AdcFactor
                ai6AdcCorrection *= ADC_DISCRETE
            }

            ai7AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai7AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai7AdcFactor
                ai7AdcCorrection *= ADC_DISCRETE
            }

            ai8AdcFactor = data.get2Bytes().toFloat() / FACTOR_DIVIDER
            data.get4Bytes().toFloat().let {
                ai8AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai8AdcFactor
                ai8AdcCorrection *= ADC_DISCRETE
            }

            data.setUnhandledParams()
        }
    }
}
