package org.secu3.android.domain.parameters

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.CkpsParamPacket

class CkpsControlTest {
    private fun control(
        firmware: MutableStateFlow<FirmwareInfoPacket?> = MutableStateFlow(null),
        sent: MutableList<OutputPacket> = mutableListOf(),
    ) = CkpsControl(firmware) { sent += it; true }

    @Test fun geometryUsesWheelCylinderAndByteBoundWithoutNormalizingReceivedChoice() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        control.receive(CkpsParamPacket(ckpsCogsNum = 60, ckpsCogsBtdc = 56, ckpsEngineCyl = 4))
        assertEquals((11..55).toList(), control.values.value?.teethBeforeTdcOptions)
        assertEquals(56, control.values.value?.teethBeforeTdc)
        assertFalse(control.selectTeethBeforeTdc(10))
        assertTrue(control.selectTeethBeforeTdc(55))
        assertEquals(55, control.values.value?.teethBeforeTdc)

        control.receive(CkpsParamPacket(ckpsCogsNum = 200, ckpsEngineCyl = 1))
        assertEquals(255, control.values.value?.teethBeforeTdcOptions?.last())
        control.receive(CkpsParamPacket(ckpsCogsNum = 0, ckpsEngineCyl = 4, ckpsCogsBtdc = 99))
        assertTrue(control.values.value?.teethBeforeTdcOptions.isNullOrEmpty())
        assertEquals(99, control.values.value?.teethBeforeTdc)
    }

    @Test fun missingTeethLookupZeroAndInvalidInputPreserveStateAndDoNotSend() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val packet = CkpsParamPacket(ckpsCogsNum = 60, ckpsEngineCyl = 4, mttf = 3.75f)
        control.receive(packet)
        listOf(1.5f, 2.5f, 3f, 4f, 5f, 6f).forEachIndexed { index, factor ->
            assertTrue(control.editMissingTeeth((index + 1).toFloat()))
            assertEquals(factor, control.values.value?.mttf)
        }
        assertTrue(control.editMissingTeeth(0f))
        assertEquals(6f, control.values.value?.mttf)
        val before = control.values.value
        val writesBefore = sent.size
        assertFalse(control.editMissingTeeth(7f))
        assertFalse(control.editMissingTeeth(1.5f))
        assertFalse(control.editMissingTeeth(Float.NaN))
        assertFalse(control.editMttf(Float.POSITIVE_INFINITY))
        assertEquals(before, control.values.value)
        assertEquals(writesBefore, sent.size)
        val preserved = control(sent = mutableListOf())
        preserved.receive(CkpsParamPacket(ckpsCogsNum = 60, ckpsEngineCyl = 4, mttf = 3.75f))
        assertTrue(preserved.editMissingTeeth(0f))
        assertEquals(3.75f, preserved.values.value?.mttf)
    }

    @Test fun editsMapAllFieldsPreserveUnknownFlagsTailAndSignedFractions() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val source = CkpsParamPacket(
            ckpsCogsBtdc = 24, ckpsIgnitCogs = 7, ckpsEngineCyl = 4, ckpsCogsNum = 60,
            ckpsMissNum = 2, hallFlags = 0xE0, hallWndWidth = -12.3f,
            hallDegreesBtdc = -45.6f, mttf = 2.5f,
        )
        val packed = source.pack()
        val packet = CkpsParamPacket().parse(intArrayOf(64, packed.first()) + packed.drop(1).toIntArray() + intArrayOf(231, 232)) as CkpsParamPacket
        control.receive(packet)
        assertEquals(-12.3f, control.values.value?.hallWindowWidth ?: 0f, .02f)
        assertEquals(-45.6f, control.values.value?.hallDegreesBeforeTdc ?: 0f, .02f)
        assertTrue(control.editWheel(80f))
        assertTrue(control.editIgnitionPulseCount(9f))
        assertTrue(control.editHallWindowWidth(-13.4f))
        assertTrue(control.editHallDegreesBeforeTdc(-46.7f))
        assertTrue(control.editMttf(7.25f))
        assertTrue(control.setCkpsEdge(true))
        assertTrue(control.setReferenceEdge(true))
        assertTrue(control.setRisingSpark(true))
        assertTrue(control.setMergeOutputs(true))
        assertTrue(control.setCamReference(true))
        assertEquals(0xFF, packet.hallFlags)
        assertArrayEquals(intArrayOf(231, 232), packet.pack().takeLast(2).toIntArray())
        assertEquals(80, control.values.value?.wheel)
        assertEquals(9, control.values.value?.ignitionPulseCount)
        assertEquals(-13.4f, control.values.value?.hallWindowWidth)
        assertEquals(-46.7f, control.values.value?.hallDegreesBeforeTdc)
        assertEquals(7.25f, control.values.value?.mttf)
    }

    @Test fun currentCapabilitiesAndCylinderRulesAreRecheckedAtCommandAdmission() {
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(null)
        val sent = mutableListOf<OutputPacket>()
        val control = control(firmware, sent)
        val packet = CkpsParamPacket(ckpsCogsNum = 60, ckpsEngineCyl = 4, ckpsCogsBtdc = 20)
        control.receive(packet)
        assertTrue(control.selectTeethBeforeTdc(21))
        firmware.value = FirmwareInfoPacket(options = 1 shl 31)
        val writes = sent.size
        assertFalse(control.editWheel(80f))
        assertFalse(control.setRisingSpark(true))
        assertFalse(control.setMergeOutputs(true))
        assertFalse(control.setCamReference(true))
        assertFalse(control.selectTeethBeforeTdc(22))
        assertEquals(writes, sent.size)
        assertFalse(control.selectCylinders(3))
        assertTrue(control.selectCylinders(6))

        val sevenBlocked = CkpsControl(MutableStateFlow(FirmwareInfoPacket()), { true })
        sevenBlocked.receive(CkpsParamPacket(ckpsEngineCyl = 4))
        assertFalse(sevenBlocked.selectCylinders(7))
        val sevenAllowed = CkpsControl(MutableStateFlow(FirmwareInfoPacket(options = 1 shl 2)), { true })
        sevenAllowed.receive(CkpsParamPacket(ckpsEngineCyl = 4, hallFlags = 1 shl 4))
        assertTrue(sevenAllowed.selectCylinders(7))
    }

    @Test fun resetAndReceiveDoNotEchoAndSnapshotsDoNotFollowLaterPacketMutation() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val packet = CkpsParamPacket(ckpsCogsNum = 60, ckpsEngineCyl = 4)
        control.receive(packet)
        assertTrue(sent.isEmpty())
        assertTrue(control.editWheel(80f))
        val captured = sent.single().pack()
        assertTrue(control.editWheel(90f))
        assertEquals(captured.toList(), sent.first().pack().toList())
        assertNotEquals(sent.first().pack().toList(), sent.last().pack().toList())
        assertEquals(80, captured[4])
        control.reset()
        assertEquals(null, control.values.value)
        assertFalse(control.editWheel(100f))
    }
}
