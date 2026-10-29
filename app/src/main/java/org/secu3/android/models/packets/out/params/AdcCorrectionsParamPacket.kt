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

import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import org.secu3.android.models.packets.base.protocolRoundToInt
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

    ) : Secu3Packet(), InputPacket, OutputPacket {

    private var parsedCompMode: Boolean? = null
    private var parsedFactorWords: IntArray? = null
    private var parsedCorrectionWords: IntArray? = null
    private val editedChannels = BooleanArray(12)

    /** A transactional edit snapshot that also preserves zero-factor raw-word behavior. */
    internal fun checkpoint(): EditCheckpoint = EditCheckpoint(
        adcFlags, mapAdcFactor, mapAdcCorrection, ubatAdcFactor, ubatAdcCorrection,
        tempAdcFactor, tempAdcCorrection, tpsAdcFactor, tpsAdcCorrection,
        ai1AdcFactor, ai1AdcCorrection, ai2AdcFactor, ai2AdcCorrection,
        ai3AdcFactor, ai3AdcCorrection, ai4AdcFactor, ai4AdcCorrection,
        ai5AdcFactor, ai5AdcCorrection, ai6AdcFactor, ai6AdcCorrection,
        ai7AdcFactor, ai7AdcCorrection, ai8AdcFactor, ai8AdcCorrection,
        parsedCompMode, parsedFactorWords?.copyOf(), parsedCorrectionWords?.copyOf(), editedChannels.copyOf(),
    )

    internal fun restore(checkpoint: EditCheckpoint) {
        adcFlags = checkpoint.adcFlags
        mapAdcFactor = checkpoint.mapAdcFactor; mapAdcCorrection = checkpoint.mapAdcCorrection
        ubatAdcFactor = checkpoint.ubatAdcFactor; ubatAdcCorrection = checkpoint.ubatAdcCorrection
        tempAdcFactor = checkpoint.tempAdcFactor; tempAdcCorrection = checkpoint.tempAdcCorrection
        tpsAdcFactor = checkpoint.tpsAdcFactor; tpsAdcCorrection = checkpoint.tpsAdcCorrection
        ai1AdcFactor = checkpoint.ai1AdcFactor; ai1AdcCorrection = checkpoint.ai1AdcCorrection
        ai2AdcFactor = checkpoint.ai2AdcFactor; ai2AdcCorrection = checkpoint.ai2AdcCorrection
        ai3AdcFactor = checkpoint.ai3AdcFactor; ai3AdcCorrection = checkpoint.ai3AdcCorrection
        ai4AdcFactor = checkpoint.ai4AdcFactor; ai4AdcCorrection = checkpoint.ai4AdcCorrection
        ai5AdcFactor = checkpoint.ai5AdcFactor; ai5AdcCorrection = checkpoint.ai5AdcCorrection
        ai6AdcFactor = checkpoint.ai6AdcFactor; ai6AdcCorrection = checkpoint.ai6AdcCorrection
        ai7AdcFactor = checkpoint.ai7AdcFactor; ai7AdcCorrection = checkpoint.ai7AdcCorrection
        ai8AdcFactor = checkpoint.ai8AdcFactor; ai8AdcCorrection = checkpoint.ai8AdcCorrection
        parsedCompMode = checkpoint.parsedCompMode
        parsedFactorWords = checkpoint.parsedFactorWords?.copyOf()
        parsedCorrectionWords = checkpoint.parsedCorrectionWords?.copyOf()
        checkpoint.editedChannels.copyInto(editedChannels)
    }

    internal class EditCheckpoint internal constructor(
        internal val adcFlags: Int,
        internal val mapAdcFactor: Float, internal val mapAdcCorrection: Float,
        internal val ubatAdcFactor: Float, internal val ubatAdcCorrection: Float,
        internal val tempAdcFactor: Float, internal val tempAdcCorrection: Float,
        internal val tpsAdcFactor: Float, internal val tpsAdcCorrection: Float,
        internal val ai1AdcFactor: Float, internal val ai1AdcCorrection: Float,
        internal val ai2AdcFactor: Float, internal val ai2AdcCorrection: Float,
        internal val ai3AdcFactor: Float, internal val ai3AdcCorrection: Float,
        internal val ai4AdcFactor: Float, internal val ai4AdcCorrection: Float,
        internal val ai5AdcFactor: Float, internal val ai5AdcCorrection: Float,
        internal val ai6AdcFactor: Float, internal val ai6AdcCorrection: Float,
        internal val ai7AdcFactor: Float, internal val ai7AdcCorrection: Float,
        internal val ai8AdcFactor: Float, internal val ai8AdcCorrection: Float,
        internal val parsedCompMode: Boolean?,
        internal val parsedFactorWords: IntArray?, internal val parsedCorrectionWords: IntArray?,
        internal val editedChannels: BooleanArray,
    )

    /** The correction has no inverse when compensation is enabled and its factor is zero. */
    fun markChannelEdited(channelIndex: Int) {
        if (channelIndex in editedChannels.indices) editedChannels[channelIndex] = true
    }

    var adcCompMode: Boolean
        get() = adcFlags.getBitValue(0) > 0
        set(value) {
            if (value != adcCompMode) parsedCompMode = null
            adcFlags = adcFlags.setBitValue(value, 0)
        }

    override fun pack(): IntArray {

        var data = intArrayOf(
            DESCRIPTOR.code
        )

        data += adcFlags

        adcChannels().forEachIndexed { index, (factor, correction) ->
            val factorWord = factor.times(FACTOR_DIVIDER).roundToInt()
            if (parsedFactorWords?.getOrNull(index)?.let { it != factorWord } == true) editedChannels[index] = true
            data += factorWord.write2Bytes()
            val rawFallback = parsedCorrectionWords?.getOrNull(index)
            val factorWasZero = parsedFactorWords?.getOrNull(index) == 0
            val canPreserveRaw = adcCompMode && parsedCompMode == true && factorWord == 0 &&
                factorWasZero && !editedChannels[index] && rawFallback != null
            val correctionWord = if (canPreserveRaw) {
                rawFallback!!
            } else {
                encodeCorrection(correction, if (adcCompMode) factor else 1.0f)
            }
            data += correctionWord.write4Bytes()
        }

        data += unhandledParams

        return data
    }

    override fun parse(data: IntArray): InputPacket {
        adcFlags = data.get1Byte()

        val channels = List(12) {
            val factorWord = data.get2Bytes()
            val correctionWord = data.get4Bytes()
            factorWord.toFloat() / FACTOR_DIVIDER to correctionWord
        }
        parsedCompMode = adcCompMode
        parsedFactorWords = channels.map { (factor, _) -> (factor * FACTOR_DIVIDER).roundToInt() }.toIntArray()
        parsedCorrectionWords = channels.map { it.second }.toIntArray()
        editedChannels.fill(false)
        channels.forEachIndexed { index, (factor, correctionWord) ->
            val divisor = if (adcCompMode) factor else 1.0f
            val correction = if (divisor == 0f) 0f else decodeCorrection(correctionWord, divisor)
            setChannel(index, factor, correction)
        }

        data.setUnhandledParams()

        return this
    }

    private fun adcChannels(): List<Pair<Float, Float>> = listOf(
        mapAdcFactor to mapAdcCorrection,
        ubatAdcFactor to ubatAdcCorrection,
        tempAdcFactor to tempAdcCorrection,
        tpsAdcFactor to tpsAdcCorrection,
        ai1AdcFactor to ai1AdcCorrection,
        ai2AdcFactor to ai2AdcCorrection,
        ai3AdcFactor to ai3AdcCorrection,
        ai4AdcFactor to ai4AdcCorrection,
        ai5AdcFactor to ai5AdcCorrection,
        ai6AdcFactor to ai6AdcCorrection,
        ai7AdcFactor to ai7AdcCorrection,
        ai8AdcFactor to ai8AdcCorrection,
    )

    private fun setChannel(index: Int, factor: Float, correction: Float) {
        when (index) {
            0 -> { mapAdcFactor = factor; mapAdcCorrection = correction }
            1 -> { ubatAdcFactor = factor; ubatAdcCorrection = correction }
            2 -> { tempAdcFactor = factor; tempAdcCorrection = correction }
            3 -> { tpsAdcFactor = factor; tpsAdcCorrection = correction }
            4 -> { ai1AdcFactor = factor; ai1AdcCorrection = correction }
            5 -> { ai2AdcFactor = factor; ai2AdcCorrection = correction }
            6 -> { ai3AdcFactor = factor; ai3AdcCorrection = correction }
            7 -> { ai4AdcFactor = factor; ai4AdcCorrection = correction }
            8 -> { ai5AdcFactor = factor; ai5AdcCorrection = correction }
            9 -> { ai6AdcFactor = factor; ai6AdcCorrection = correction }
            10 -> { ai7AdcFactor = factor; ai7AdcCorrection = correction }
            11 -> { ai8AdcFactor = factor; ai8AdcCorrection = correction }
        }
    }

    private fun encodeCorrection(correction: Float, factor: Float): Int {
        if (factor == 0f) return FACTOR_DIVIDER / 2
        val correctionDiscrete = (-correction / ADC_DISCRETE).protocolRoundToInt()
        return (FACTOR_DIVIDER * (0.5f - correctionDiscrete * factor)).protocolRoundToInt()
    }

    private fun decodeCorrection(rawCorrection: Int, factor: Float): Float =
        ((rawCorrection.toFloat() / FACTOR_DIVIDER - 0.5f) / factor) * ADC_DISCRETE

    companion object {

        internal const val DESCRIPTOR = 'r'

        private const val FACTOR_DIVIDER = 16384
        private const val ADC_DISCRETE = 0.0025f
    }
}
