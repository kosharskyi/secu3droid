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
package org.secu3.android.models.packets.params

import org.secu3.android.models.packets.BaseOutputPacket

data class AdcCorrectionsParamPacket(

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

        data += END_PACKET_SYMBOL
        return data
    }

    companion object {

        internal const val DESCRIPTOR = 'r'

        private const val FACTOR_DIVIDER = 16384
        private const val ADC_DISCRETE = 0.0025f

        fun parse(data: String) = AdcCorrectionsParamPacket().apply {
            mapAdcFactor = data.get2Bytes(2).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(4).toFloat().let {
                mapAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / mapAdcFactor
                mapAdcCorrection *= ADC_DISCRETE
            }

            ubatAdcFactor = data.get2Bytes(8).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(10).toFloat().let {
                ubatAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ubatAdcFactor
                ubatAdcCorrection *= ADC_DISCRETE
            }

            tempAdcFactor = data.get2Bytes(14).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(16).toFloat().let {
                tempAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / tempAdcFactor
                tempAdcCorrection *= ADC_DISCRETE
            }

            tpsAdcFactor = data.get2Bytes(20).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(22).toFloat().let {
                tpsAdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / tpsAdcFactor
                tpsAdcCorrection *= ADC_DISCRETE
            }

            ai1AdcFactor = data.get2Bytes(26).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(28).toFloat().let {
                ai1AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai1AdcFactor
                ai1AdcCorrection *= ADC_DISCRETE
            }

            ai2AdcFactor = data.get2Bytes(32).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(34).toFloat().let {
                ai2AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai2AdcFactor
                ai2AdcCorrection *= ADC_DISCRETE
            }

            ai3AdcFactor = data.get2Bytes(38).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(40).toFloat().let {
                ai3AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) /ai3AdcFactor
                ai3AdcCorrection *= ADC_DISCRETE
            }

            ai4AdcFactor = data.get2Bytes(44).toFloat() / FACTOR_DIVIDER
            data.get4Bytes(46).toFloat().let {
                ai4AdcCorrection = ((it / FACTOR_DIVIDER) - 0.5f) / ai4AdcFactor
                ai4AdcCorrection *= ADC_DISCRETE
            }

//            ai5AdcFactor = data.get2Bytes(50).toFloat() / FACTOR_DIVIDER
//            ai5AdcCorrection = data.get4Bytes(52).toFloat()  //fixme
//            ai6AdcFactor = data.get2Bytes(56).toFloat() / FACTOR_DIVIDER
//            ai6AdcCorrection = data.get4Bytes(58).toFloat()  //fixme
//            ai7AdcFactor = data.get2Bytes(62).toFloat() / FACTOR_DIVIDER
//            ai7AdcCorrection = data.get4Bytes(64).toFloat()  //fixme
//            ai8AdcFactor = data.get2Bytes(68).toFloat() / FACTOR_DIVIDER
//            ai8AdcCorrection = data.get4Bytes(70).toFloat()  //fixme
        }
    }
}
