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

    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += adcFlags.toChar()

        mapAdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        mapAdcCorrection.div(ADC_DISCRETE).times(mapAdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ubatAdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ubatAdcCorrection.div(ADC_DISCRETE).times(ubatAdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        tempAdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        tempAdcCorrection.div(ADC_DISCRETE).times(tempAdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        tpsAdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        tpsAdcCorrection.div(ADC_DISCRETE).times(tpsAdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai1AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai1AdcCorrection.div(ADC_DISCRETE).times(ai1AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai2AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai2AdcCorrection.div(ADC_DISCRETE).times(ai2AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai3AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai3AdcCorrection.div(ADC_DISCRETE).times(ai3AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai4AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai4AdcCorrection.div(ADC_DISCRETE).times(ai4AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai5AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai5AdcCorrection.div(ADC_DISCRETE).times(ai5AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai6AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai6AdcCorrection.div(ADC_DISCRETE).times(ai6AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai7AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai7AdcCorrection.div(ADC_DISCRETE).times(ai7AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        ai8AdcFactor.times(FACTOR_DIVIDER).toInt().let { data += data.write2Bytes(it) }
        ai8AdcCorrection.div(ADC_DISCRETE).times(ai8AdcFactor).plus(0.5f).times(FACTOR_DIVIDER).toInt().let {
            data += data.write4Bytes(it)
        }

        data += unhandledParams

        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'r'

        private const val FACTOR_DIVIDER = 16384
        private const val ADC_DISCRETE = 0.0025f

        fun parse(data: String) = AdcCorrectionsParamPacket().apply {
            adcFlags = data[2].code

            mapAdcFactor = data.get2Bytes(3).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(5).toFloat().let {
                mapAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / mapAdcFactor
                mapAdcCorrection *= ADC_DISCRETE
            }

            ubatAdcFactor = data.get2Bytes(9).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(11).toFloat().let {
                ubatAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ubatAdcFactor
                ubatAdcCorrection *= ADC_DISCRETE
            }

            tempAdcFactor = data.get2Bytes(15).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(17).toFloat().let {
                tempAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / tempAdcFactor
                tempAdcCorrection *= ADC_DISCRETE
            }

            tpsAdcFactor = data.get2Bytes(21).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(23).toFloat().let {
                tpsAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / tpsAdcFactor
                tpsAdcCorrection *= ADC_DISCRETE
            }

            ai1AdcFactor = data.get2Bytes(27).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(29).toFloat().let {
                ai1AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai1AdcFactor
                ai1AdcCorrection *= ADC_DISCRETE
            }

            ai2AdcFactor = data.get2Bytes(32).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(35).toFloat().let {
                ai2AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai2AdcFactor
                ai2AdcCorrection *= ADC_DISCRETE
            }

            ai3AdcFactor = data.get2Bytes(39).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(41).toFloat().let {
                ai3AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) /ai3AdcFactor
                ai3AdcCorrection *= ADC_DISCRETE
            }

            ai4AdcFactor = data.get2Bytes(45).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(47).toFloat().let {
                ai4AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai4AdcFactor
                ai4AdcCorrection *= ADC_DISCRETE
            }

            ai5AdcFactor = data.get2Bytes(51).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(53).toFloat().let {
                ai5AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai5AdcFactor
                ai5AdcCorrection *= ADC_DISCRETE
            }

            ai6AdcFactor = data.get2Bytes(57).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(59).toFloat().let {
                ai6AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai6AdcFactor
                ai6AdcCorrection *= ADC_DISCRETE
            }

            ai7AdcFactor = data.get2Bytes(63).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(65).toFloat().let {
                ai7AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai7AdcFactor
                ai7AdcCorrection *= ADC_DISCRETE
            }

            ai8AdcFactor = data.get2Bytes(69).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(71).toFloat().let {
                ai8AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai8AdcFactor
                ai8AdcCorrection *= ADC_DISCRETE
            }

            if (data.length == 75) {
                return@apply
            }

            unhandledParams = data.substring(75)

        }
    }
}
