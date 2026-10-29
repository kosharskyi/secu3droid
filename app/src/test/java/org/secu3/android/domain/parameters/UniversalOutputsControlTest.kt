package org.secu3.android.domain.parameters

import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket

class UniversalOutputsControlTest {
    @Test fun namedCommandsEditAllSixOutputsIndependentlyAndSnapshotBytes() {
        val sends = mutableListOf<OutputPacket>()
        val control = UniversalOutputsControl(UniversalOutputConditions()) { sends += it; true }
        val packet = UniOutParamPacket()
        control.receive(packet)
        for (i in 1..6) {
            assertTrue(control.setOutputEnabled(i, true))
            assertTrue(control.setConditionInverted(i, 1, i % 2 == 0))
            assertTrue(control.setConditionInverted(i, 2, i % 2 != 0))
            assertTrue(control.setResultInverted(i, i == 4))
            val logic = listOf(0, 1, 2, 3, 15, 0)[i - 1]
            assertTrue(control.selectCondition(i, 1, 0))
            assertTrue(control.selectCondition(i, 2, 1))
            assertTrue(control.editOnThreshold(i, 1, 0, 50f + i))
            assertTrue(control.editOffThreshold(i, 1, 0, 40f + i))
            assertTrue(control.editOnThreshold(i, 2, 1, 1000f + i))
            assertTrue(control.editOffThreshold(i, 2, 1, 900f + i))
            assertTrue(control.selectLogicFunction(i, logic))
        }
        for (i in 1..6) {
            val state = control.values.value!!.outputs[i - 1]
            assertTrue(state.enabled)
            assertEquals(i % 2 == 0, state.condition1Inversion)
            assertEquals(i % 2 != 0, state.condition2Inversion)
            assertEquals(i == 4, state.inversion)
            assertEquals(listOf(0, 1, 2, 3, 15, 0)[i - 1], state.logicFunction)
            assertEquals(50f + i, state.onThreshold1, 0f)
            assertEquals(900f + i, state.offThreshold2, 0f)
        }
        assertTrue(control.selectPairFunction(15))
        assertEquals(15, control.values.value!!.logicFunction12)
        assertTrue(sends.isNotEmpty())
        val first = sends.first().pack()
        control.selectPairFunction(0)
        assertArrayEquals(first, sends.first().pack())
    }

    @Test fun defaultSelectionReencodesEqualDisplayThresholdsAndUnknownRawWordsSurviveOtherEdits() {
        val control = UniversalOutputsControl(UniversalOutputConditions()) { true }
        val bytes = UniOutParamPacket().apply {
            output1Use = true; output1Condition1 = 13; output1OnThrd1 = 55f; output1OffThrd1 = 50f
            output3Condition1 = 250
        }.pack()
        val parsed = UniOutParamPacket().parse(intArrayOf('@'.code, '&'.code) + bytes.drop(1)) as UniOutParamPacket
        val payload = parsed.pack().copyOf()
        control.receive(parsed)
        assertTrue(control.selectCondition(1, 1, 14))
        assertEquals(14, parsed.output1Condition1)
        assertEquals(55f, parsed.output1OnThrd1, 0f)
        assertNotEquals("ATS ×4 encoding must replace TPS ×2 encoding", payload[4], parsed.pack()[4])
        assertTrue(control.setOutputEnabled(2, true))
        assertEquals(250, parsed.output3Condition1)
    }

    @Test fun rejectedCommandsAndFailedAdmissionRestoreEveryRawFieldAndTail() {
        var admit = false
        val control = UniversalOutputsControl(UniversalOutputConditions()) { admit }
        val original = UniOutParamPacket().apply { output1Use = true; output3Use = true; output3Condition1 = 5 }
        val payload = original.pack().drop(1).toIntArray().also { raw ->
            raw[1] = 250; raw[2] = 251
            raw[3] = 0xFE; raw[4] = 0xDC; raw[5] = 0xFA; raw[6] = 0xCE
            raw[7] = 0xED; raw[8] = 0xCB; raw[9] = 0xAB; raw[10] = 0xCD
            raw[23] = 5; raw[25] = 0xFF; raw[26] = 0xFF
        } + intArrayOf(0xA5, 0x5A)
        val packet = UniOutParamPacket().parse(intArrayOf('@'.code, '&'.code) + payload) as UniOutParamPacket
        control.receive(packet)
        val before = packet.pack()
        assertFalse(control.setResultInverted(1, true)) // no admission, raw rollback
        assertArrayEquals(before, packet.pack())
        assertFalse(control.selectCondition(1, 1, 7)) // timer is slot two only
        assertFalse(control.selectCondition(1, 1, 100))
        assertFalse(control.editOnThreshold(1, 1, 250, Float.NaN))
        assertFalse(control.editOffThreshold(1, 1, 250, Float.POSITIVE_INFINITY))
        assertFalse(control.editOnThreshold(1, 1, 250, 100000f))
        assertEquals(999.9f, packet.output3OnThrd1, 0f)
        assertFalse(control.setResultInverted(3, true)) // attempted edit must restore the clamped VSPD raw word exactly.
        assertFalse(control.selectCondition(1, 1, 0)) // condition/default rewrite is rolled back on failed admission.
        assertFalse(control.selectPairFunction(1))
        assertArrayEquals(before, packet.pack())
        assertEquals(250, packet.output1Condition1)
        assertEquals(251, packet.output1Condition2)
        assertArrayEquals(intArrayOf(0xFE, 0xDC, 0xFA, 0xCE, 0xED, 0xCB, 0xAB, 0xCD), packet.pack().sliceArray(4..11))
        assertEquals(listOf(0xFF, 0xFF), packet.pack().slice(26..27))
        assertEquals(listOf(0xA5, 0x5A), packet.pack().takeLast(2))
        admit = true
        assertTrue(control.setResultInverted(1, true))
        assertTrue(packet.output1Inversion)
    }

    @Test fun finiteThresholdValidationUsesKnownConditionBoundsAndDoesNotSendInvalidValues() {
        var sends = 0
        val control = UniversalOutputsControl(UniversalOutputConditions()) { sends++; true }
        val packet = UniOutParamPacket().apply { output1Use = true; output1Condition1 = 0 }
        control.receive(packet)
        val before = packet.pack()
        assertFalse(control.editOnThreshold(1, 1, 0, Float.NaN))
        assertFalse(control.editOnThreshold(1, 1, 0, Float.POSITIVE_INFINITY))
        assertFalse(control.editOnThreshold(1, 1, 0, -40.25f))
        assertFalse(control.editOffThreshold(1, 1, 0, 300.25f))
        assertEquals(0, sends)
        assertArrayEquals(before, packet.pack())
    }

    @Test fun pairRequiresBothOutputsAndSlotTwoRequiresNonNoneLogic() {
        val control = UniversalOutputsControl(UniversalOutputConditions()) { true }
        val p = UniOutParamPacket().apply { output1Use = true; output1LogicFunc = 15; output2Use = true }
        control.receive(p)
        assertTrue(control.selectPairFunction(1))
        assertFalse(control.setConditionInverted(1, 2, true))
        assertFalse(control.editOnThreshold(1, 2, 0, 2f))
        assertTrue(control.selectLogicFunction(1, 0))
        assertTrue(control.selectCondition(1, 2, 23))
        assertTrue(control.editOnThreshold(1, 2, 23, 2f))
        assertTrue(control.selectPairFunction(1))
        assertTrue(control.setOutputEnabled(2, false))
        assertFalse(control.selectPairFunction(2))
    }

    @Test fun resetIsPassiveAndStaleThresholdEditorCannotWriteAfterConditionChange() {
        val sends = mutableListOf<OutputPacket>()
        val control = UniversalOutputsControl(UniversalOutputConditions()) { sends += it; true }
        control.receive(UniOutParamPacket().apply { output1Use = true })
        assertTrue(control.selectCondition(1, 1, 14))
        val count = sends.size
        assertFalse(control.editOnThreshold(1, 1, 13, 60f))
        assertEquals(count, sends.size)
        control.reset()
        assertNull(control.values.value)
        assertFalse(control.setOutputEnabled(1, true))
        assertEquals(count, sends.size)
    }
}
