package org.secu3.android.domain.parameters

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket

class StarterControlTest {
    @Test
    fun receiveAndResetArePassiveAndValuesKeepRawPacketFields() {
        val sent = mutableListOf<IntArray>()
        val control = control(send = { sent += it.pack(); true })
        val packet = StarterParamPacket(
            starterOff = 6100,
            smapAbandon = 250,
            crankToRunTime = 1.2345f,
            injAftstrStroke = 3,
            injAftStrokes1 = 2,
            injPrimeCold = 3.4567f,
            injCrankToRun_time1 = .12345f,
            strtFlags = 0xA4,
        )

        control.receive(packet)

        assertTrue(sent.isEmpty())
        assertEquals(6100, control.values.value?.starterOff)
        assertEquals(.12345f, control.values.value?.crankToRunHot)
        assertEquals(3, control.values.value?.afterstartPetrol)
        assertEquals(2, control.values.value?.afterstartGas)
        assertEquals(0xA4, packet.strtFlags)

        control.reset()
        assertNull(control.values.value)
        assertFalse(control.editStarterOff(100))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun editPreservesUnknownTailFlagsAndUnchangedFractionalFields() {
        val sent = mutableListOf<IntArray>()
        val control = control(send = { sent += it.pack(); true })
        val frame = intArrayOf('@'.code, 'o'.code) + IntArray(19) + intArrayOf(0xA5, 0x5A)
        val packet = StarterParamPacket().parse(frame) as StarterParamPacket
        packet.injCrankToRun_time1 = .12345f
        packet.strtFlags = 0xA4
        control.receive(packet)

        assertTrue(control.setAllowFloodClearStart(true))

        assertEquals(.12345f, packet.injCrankToRun_time1)
        assertEquals(0xA5, packet.strtFlags)
        assertArrayEquals(intArrayOf(0xA5, 0x5A), packet.pack().takeLast(2).toIntArray())
        assertEquals(0xA5, sent.single()[17])
        assertEquals(.12345f, control.values.value?.crankToRunHot)
    }

    @Test
    fun explicitValidationRejectsNonFiniteAndOutOfRangeButAcceptsIntegerEditorValues() {
        val sent = mutableListOf<IntArray>()
        val control = control(send = { sent += it.pack(); true })
        val packet = StarterParamPacket()
        control.receive(packet)

        assertFalse(control.editCrankToRunCold(Float.NaN))
        assertFalse(control.editCrankToRunCold(Float.POSITIVE_INFINITY))
        assertFalse(control.editCrankToRunCold(.09f))
        assertFalse(control.editPrimeCold(100.1f))
        assertFalse(control.editStarterOff(-1))
        assertFalse(control.editStblCountdown(256))
        assertTrue(control.editAfterstartPetrol(2))
        assertTrue(control.editAfterstartGas(3))
        assertEquals(2, packet.injAftstrStroke)
        assertEquals(3, packet.injAftStrokes1)
        assertEquals(1, sent[0][7])
        assertEquals(1, sent[1][15])
        assertEquals(2, control.values.value?.afterstartPetrol)
        assertEquals(3, control.values.value?.afterstartGas)
        assertEquals(2, sent.size)
    }

    @Test
    fun admissionUsesCurrentFirmwareAndNullFirmwareKeepsPreviewPermissive() {
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(null)
        val sent = mutableListOf<IntArray>()
        val control = control(firmware) { sent += it.pack(); true }
        control.receive(StarterParamPacket())

        assertTrue(control.editPrimeCold(1f))
        firmware.value = FirmwareInfoPacket(options = 1 shl 29) // Gas Dose only.
        assertFalse(control.editPrimeCold(2f))
        assertTrue(control.editAfterstartPetrol(4))
        assertTrue(control.editFloodClearThreshold(50f))
        assertFalse(control.setLimitCrankingPw(true))
        firmware.value = FirmwareInfoPacket(options = 1 shl 22) // Choke only.
        assertTrue(control.editFloodClearThreshold(60f))
        assertFalse(control.editAfterstartGas(8))
        assertTrue(control.setAllowFloodClearStart(true))
        assertEquals(5, sent.size)
    }

    @Test
    fun refusedAdmissionRollsBackInPlaceAndKeepsPublishedValuesAndTail() {
        var attempts = 0
        val control = control(send = { attempts++; false })
        val frame = intArrayOf('@'.code, 'o'.code) + IntArray(19) + intArrayOf(0xC3, 0x7E)
        val packet = StarterParamPacket().parse(frame) as StarterParamPacket
        packet.starterOff = 33
        packet.strtFlags = 0xA0
        control.receive(packet)
        val beforeValues = control.values.value
        val beforeBytes = packet.pack()

        assertFalse(control.editStarterOff(44))
        assertFalse(control.setAllowFloodClearStart(true))

        assertEquals(2, attempts)
        assertEquals(33, packet.starterOff)
        assertEquals(0xA0, packet.strtFlags)
        assertArrayEquals(beforeBytes, packet.pack())
        assertEquals(beforeValues, control.values.value)
        assertNotNull(control.values.value)
    }

    @Test
    fun eachAdmittedSendKeepsItsOwnPackedSnapshot() {
        val snapshots = mutableListOf<org.secu3.android.models.packets.base.OutputPacket>()
        val control = control(send = { snapshots += it; true })
        control.receive(StarterParamPacket())

        assertTrue(control.editStarterOff(120))
        val firstBytes = snapshots.single().pack()
        assertTrue(control.editStarterOff(140))

        assertArrayEquals(firstBytes, snapshots.first().pack())
        assertEquals(140, control.values.value?.starterOff)
    }

    @Test
    fun everyNamedEditPreservesItsFieldMappingAndMatchesFrozenStarterWirePayload() {
        val snapshots = mutableListOf<ByteArray>()
        val control = control(send = { snapshots += it.pack().map(Int::toByte).toByteArray(); true })
        val packet = StarterParamPacket(
            starterOff = 100,
            smapAbandon = 200,
            crankToRunTime = 1.11f,
            injAftstrStroke = 8,
            injPrimeCold = 1.6f,
            injPrimeHot = 2.4f,
            injPrimeDelay = 1.2f,
            injFloodclearTps = 10f,
            injAftStrokes1 = 12,
            stblStrCnt = 8,
            strtFlags = 0x80,
            injCrankToRun_time1 = 3.75f,
        )
        control.receive(packet)

        assertEquals(
            StarterValues(100, 200, 1.11f, 3.75f, 8, 12, 1.6f, 2.4f, 1.2f, 10f, 8, false, false),
            control.values.value,
        )
        assertTrue(control.editStarterOff(300))
        assertTrue(control.editSmapAbandon(500))
        assertTrue(control.editCrankToRunCold(1.25f))
        assertTrue(control.editAfterstartPetrol(2))
        assertTrue(control.editPrimeCold(3.2f))
        assertTrue(control.editPrimeHot(6.4f))
        assertTrue(control.editPrimeDelay(2.3f))
        assertTrue(control.editFloodClearThreshold(50f))
        assertTrue(control.editAfterstartGas(3))
        assertTrue(control.editStblCountdown(12))
        assertTrue(control.setAllowFloodClearStart(true))
        assertTrue(control.setLimitCrankingPw(true))
        assertTrue(control.editCrankToRunHot(4.56f))

        assertEquals(300, packet.starterOff)
        assertEquals(500, packet.smapAbandon)
        assertEquals(1.25f, packet.crankToRunTime)
        assertEquals(2, packet.injAftstrStroke)
        assertEquals(3.2f, packet.injPrimeCold)
        assertEquals(6.4f, packet.injPrimeHot)
        assertEquals(2.3f, packet.injPrimeDelay)
        assertEquals(50f, packet.injFloodclearTps)
        assertEquals(3, packet.injAftStrokes1)
        assertEquals(12, packet.stblStrCnt)
        assertEquals(0x83, packet.strtFlags)
        assertEquals(4.56f, packet.injCrankToRun_time1)
        assertEquals(
            StarterValues(300, 500, 1.25f, 4.56f, 2, 3, 3.2f, 6.4f, 2.3f, 50f, 12, true, true),
            control.values.value,
        )
        assertEquals(13, snapshots.size)
        assertArrayEquals(
            intArrayOf('o'.code, 0x01, 0x2C, 0x01, 0xF4, 0x00, 0x7D, 0x01, 0x03, 0xE8, 0x07, 0xD0, 0x17, 0x0C, 0x80, 0x01, 0x0C, 0x83, 0x01, 0xC8)
                .map(Int::toByte).toByteArray(),
            snapshots.last(),
        )
    }

    private fun control(
        firmware: MutableStateFlow<FirmwareInfoPacket?> = MutableStateFlow(null),
        send: (org.secu3.android.models.packets.base.OutputPacket) -> Boolean,
    ) = StarterControl(firmware, send)
}
