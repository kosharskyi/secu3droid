/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.parameters.pages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.utils.UniOutConditionFormat
import org.secu3.android.utils.uniOutConditionFormat

class UniversalOutputsPacketTest {
    @Test
    fun eachOutputFlagAndThresholdMapsIndependently() {
        val packet = UniOutParamPacket()
        (1..6).forEach { index ->
            setOutput(packet, index, index)
        }
        val snapshot = UniversalOutputsValues.from(packet)
        (1..6).forEach { index ->
            val output = snapshot.outputs[index - 1]
            assertTrue(output.enabled)
            assertEquals(index % 2 == 0, output.condition1Inversion)
            assertEquals(index % 2 != 0, output.condition2Inversion)
            assertEquals(index == 4, output.inversion)
            assertEquals(index - 1, output.logicFunction)
            assertEquals(index * 10f, output.onThreshold1, .01f)
            assertEquals(index * 20f, output.offThreshold2, .01f)
        }
        val remapped = UniOutParamPacket()
        snapshot.outputs.forEachIndexed { index, output -> applyUniversalOutput(remapped, index + 1, UniversalOutputsValues.from(remapped).outputs[index], output) }
        assertEquals(snapshot.outputs, UniversalOutputsValues.from(remapped).outputs)
        assertFalse(snapshot.outputs[2].inversion)
    }

    @Test
    fun signedConditionsRoundTripAcrossBothThresholdPairsAndAllOutputs() {
        val signed = listOf(0, 11, 14, 28, 30, 34, 39, 40)
        (1..6).forEach { output -> signed.forEach { condition ->
            val packet = UniOutParamPacket()
            setOutputConditions(packet, output, condition)
            setOutputThresholds(packet, output, -12f, -5f, -8f, -2f)
            val reparsed = UniOutParamPacket().parse(intArrayOf(0x40, 0x21) + packet.pack().drop(1).toIntArray()) as UniOutParamPacket
            val actual = UniversalOutputsValues.from(reparsed).outputs[output - 1]
            assertEquals("output $output condition $condition", -12f, actual.onThreshold1, .02f)
            assertEquals(-5f, actual.offThreshold1, .02f)
            assertEquals(-8f, actual.onThreshold2, .02f)
            assertEquals(-2f, actual.offThreshold2, .02f)
        } }
    }

    @Test
    fun signedThresholdEncodersUseManagerRoundingForEveryOutputAndCondition() {
        // Independent protocol scales: CTS/ATS/GRTS/TMP2/FTS/OTS ×4; AANG ×32; TPSDOT ×1.
        val signedConditions = listOf(0 to 4f, 11 to 32f, 14 to 4f, 28 to 4f, 30 to 4f, 34 to 1f, 39 to 4f, 40 to 4f)
        val cases = listOf(
            ThresholdRoundingCase(.5f, -.5f, 1.5f, -1.5f, intArrayOf(0x0001, 0xFFFF, 0x0002, 0xFFFE)),
            ThresholdRoundingCase(.4f, -.4f, 1.4f, -1.4f, intArrayOf(0x0000, 0x0000, 0x0001, 0xFFFF)),
        )

        for (output in 1..6) for ((condition, scale) in signedConditions) for (case in cases) {
            val packet = UniOutParamPacket()
            setOutputConditions(packet, output, condition)
            setOutputThresholds(
                packet,
                output,
                case.on1 / scale,
                case.off1 / scale,
                case.on2 / scale,
                case.off2 / scale,
            )
            val thresholdStart = 4 + (output - 1) * 11
            assertArrayEquals(
                "output $output, signed condition $condition, counts ${case.words.contentToString()}",
                case.words.flatMap { listOf(it ushr 8, it and 0xFF) }.toIntArray(),
                packet.pack().sliceArray(thresholdStart..thresholdStart + 7),
            )
        }
    }

    @Test
    fun unsignedRpmThresholdsKeepExistingPositiveTieRounding() {
        val packet = UniOutParamPacket().apply {
            output1Condition1 = 1
            output1Condition2 = 1
            output1OnThrd1 = .5f
            output1OffThrd1 = -.5f
            output1OnThrd2 = 1.5f
            output1OffThrd2 = -1.5f
        }
        assertArrayEquals(intArrayOf(0, 1, 0, 0, 0, 2, 0xFF, 0xFF), packet.pack().sliceArray(4..11))
    }

    private data class ThresholdRoundingCase(
        val on1: Float,
        val off1: Float,
        val on2: Float,
        val off2: Float,
        val words: IntArray,
    )

    @Test
    fun outputChangesPreserveUnrelatedFlagsAndUnknownTail() {
        val source = UniOutParamPacket()
        source.output3Use = true
        source.output3Cond2Inversion = true
        source.output6LogicFunc = 9
        source.output4Inversion = true
        val withTail = UniOutParamPacket().parse(intArrayOf(0x40, 0x21) + source.pack().drop(1).toIntArray() + intArrayOf(0xA5, 0x5A)) as UniOutParamPacket
        withTail.output4Use = true
        val updated = withTail
        assertTrue(updated.output3Use)
        assertTrue(updated.output3Cond2Inversion)
        assertEquals(9, updated.output6LogicFunc)
        assertTrue(updated.output4Inversion)
        assertTrue(updated.pack().takeLast(2).toIntArray().contentEquals(intArrayOf(0xA5, 0x5A)))
    }

    @Test
    fun editingAnotherFieldPreservesRawThresholdsForUnknownConditions() {
        val source = UniOutParamPacket().pack().drop(1).toIntArray()
        source[1] = 250
        source[2] = 251
        source[3] = 0xFE; source[4] = 0xDC
        source[5] = 0xFA; source[6] = 0xCE
        source[7] = 0xED; source[8] = 0xCB
        source[9] = 0xAB; source[10] = 0xCD
        val packet = UniOutParamPacket().parse(intArrayOf(0x40, 0x21) + source) as UniOutParamPacket
        val beforeValues = UniversalOutputsValues.from(packet)
        val outputBefore = beforeValues.outputs.first()
        val outputAfter = outputBefore.copy(enabled = true)
        applyUniversalOutput(packet, 1, outputBefore, outputAfter)

        assertEquals(250, packet.output1Condition1)
        assertEquals(251, packet.output1Condition2)
        assertEquals(listOf(0xFE, 0xDC, 0xFA, 0xCE, 0xED, 0xCB, 0xAB, 0xCD), packet.pack().slice(4..11))
    }

    @Test
    fun desktopConditionFormattingAndDefaultsAreRepresented() {
        assertEquals(UniOutConditionFormat(-40f, 300f, .25f, 2, 55f, 50f), uniOutConditionFormat(0))
        assertEquals(UniOutConditionFormat(50f, 30000f, 10f, 0, 1500f, 1200f), uniOutConditionFormat(1))
        assertEquals(UniOutConditionFormat(-1000f, 1000f, 1f, 0, 55f, 50f), uniOutConditionFormat(34))
        assertEquals(UniOutConditionFormat(0f, 600f, .01f, 2, 1f, 5f), uniOutConditionFormat(23))
        assertEquals(UniOutConditionFormat(0f, 600f, .1f, 2, 2.5f, 2.48f), uniOutConditionFormat(16))
        assertEquals(UniOutConditionFormat(0f, 5f, .01f, 2, 2.5f, 2.45f), uniOutConditionFormat(12))
        assertEquals(UniOutConditionFormat(-40f, 250f, .25f, 1, 100f, 98f), uniOutConditionFormat(39))
        assertEquals(uniOutConditionFormat(39), uniOutConditionFormat(40))
    }

    @Test
    fun selectingEitherConditionResetsOnlyThatConditionsThresholdDefaults() {
        val current = UniversalOutputValues(true, 0, 1, false, false, false, 0, 88f, 77f, 66f, 55f)
        val firstChanged = current.withCondition(1, 16)
        assertEquals(2.5f, firstChanged.onThreshold1, 0f)
        assertEquals(2.48f, firstChanged.offThreshold1, 0f)
        assertEquals(66f, firstChanged.onThreshold2, 0f)
        assertEquals(55f, firstChanged.offThreshold2, 0f)
        val secondChanged = current.withCondition(2, 23)
        assertEquals(88f, secondChanged.onThreshold1, 0f)
        assertEquals(77f, secondChanged.offThreshold1, 0f)
        assertEquals(1f, secondChanged.onThreshold2, 0f)
        assertEquals(5f, secondChanged.offThreshold2, 0f)
    }

    @Test
    fun changingConditionReencodesEqualDisplayedThresholdDefaults() {
        val packet = UniOutParamPacket().also {
            it.output1Condition1 = 13 // TPS uses a different wire scale than ATS.
            it.output1OnThrd1 = 55f
            it.output1OffThrd1 = 50f
        }
        val before = UniversalOutputsValues.from(packet).outputs.first()
        val after = before.withCondition(1, 14) // ATS defaults to the same displayed ON value, 55.
        assertEquals(before.onThreshold1, after.onThreshold1, 0f)

        applyUniversalOutput(packet, 1, before, after)

        val expected = UniOutParamPacket().also {
            it.output1Condition1 = 14
            it.output1OnThrd1 = 55f
            it.output1OffThrd1 = 50f
        }
        assertEquals(expected.pack().slice(4..7), packet.pack().slice(4..7))
    }

    private fun setOutput(packet: UniOutParamPacket, index: Int, value: Int) = when (index) {
        1 -> { packet.output1Use = true; packet.output1Cond1Inversion = index % 2 == 0; packet.output1Cond2Inversion = index % 2 != 0; packet.output1Inversion = index == 4; packet.output1LogicFunc = index - 1; packet.output1OnThrd1 = index * 10f; packet.output1OffThrd2 = index * 20f }
        2 -> { packet.output2Use = true; packet.output2Cond1Inversion = index % 2 == 0; packet.output2Cond2Inversion = index % 2 != 0; packet.output2Inversion = index == 4; packet.output2LogicFunc = index - 1; packet.output2OnThrd1 = index * 10f; packet.output2OffThrd2 = index * 20f }
        3 -> { packet.output3Use = true; packet.output3Cond1Inversion = index % 2 == 0; packet.output3Cond2Inversion = index % 2 != 0; packet.output3Inversion = index == 4; packet.output3LogicFunc = index - 1; packet.output3OnThrd1 = index * 10f; packet.output3OffThrd2 = index * 20f }
        4 -> { packet.output4Use = true; packet.output4Cond1Inversion = index % 2 == 0; packet.output4Cond2Inversion = index % 2 != 0; packet.output4Inversion = index == 4; packet.output4LogicFunc = index - 1; packet.output4OnThrd1 = index * 10f; packet.output4OffThrd2 = index * 20f }
        5 -> { packet.output5Use = true; packet.output5Cond1Inversion = index % 2 == 0; packet.output5Cond2Inversion = index % 2 != 0; packet.output5Inversion = index == 4; packet.output5LogicFunc = index - 1; packet.output5OnThrd1 = index * 10f; packet.output5OffThrd2 = index * 20f }
        6 -> { packet.output6Use = true; packet.output6Cond1Inversion = index % 2 == 0; packet.output6Cond2Inversion = index % 2 != 0; packet.output6Inversion = index == 4; packet.output6LogicFunc = index - 1; packet.output6OnThrd1 = index * 10f; packet.output6OffThrd2 = index * 20f }
        else -> error("Output index must be between 1 and 6")
    }

    private fun setOutputConditions(packet: UniOutParamPacket, index: Int, condition: Int) = when (index) {
        1 -> { packet.output1Condition1 = condition; packet.output1Condition2 = condition }
        2 -> { packet.output2Condition1 = condition; packet.output2Condition2 = condition }
        3 -> { packet.output3Condition1 = condition; packet.output3Condition2 = condition }
        4 -> { packet.output4Condition1 = condition; packet.output4Condition2 = condition }
        5 -> { packet.output5Condition1 = condition; packet.output5Condition2 = condition }
        6 -> { packet.output6Condition1 = condition; packet.output6Condition2 = condition }
        else -> error("Output index must be between 1 and 6")
    }

    private fun setOutputThresholds(packet: UniOutParamPacket, index: Int, on1: Float, off1: Float, on2: Float, off2: Float) = when (index) {
        1 -> { packet.output1OnThrd1 = on1; packet.output1OffThrd1 = off1; packet.output1OnThrd2 = on2; packet.output1OffThrd2 = off2 }
        2 -> { packet.output2OnThrd1 = on1; packet.output2OffThrd1 = off1; packet.output2OnThrd2 = on2; packet.output2OffThrd2 = off2 }
        3 -> { packet.output3OnThrd1 = on1; packet.output3OffThrd1 = off1; packet.output3OnThrd2 = on2; packet.output3OffThrd2 = off2 }
        4 -> { packet.output4OnThrd1 = on1; packet.output4OffThrd1 = off1; packet.output4OnThrd2 = on2; packet.output4OffThrd2 = off2 }
        5 -> { packet.output5OnThrd1 = on1; packet.output5OffThrd1 = off1; packet.output5OnThrd2 = on2; packet.output5OffThrd2 = off2 }
        6 -> { packet.output6OnThrd1 = on1; packet.output6OffThrd1 = off1; packet.output6OnThrd2 = on2; packet.output6OffThrd2 = off2 }
        else -> error("Output index must be between 1 and 6")
    }
}
