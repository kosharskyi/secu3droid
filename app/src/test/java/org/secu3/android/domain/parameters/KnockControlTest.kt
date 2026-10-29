package org.secu3.android.domain.parameters

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.KnockParamPacket

class KnockControlTest {
    private fun control(sent: MutableList<IntArray>, admit: () -> Boolean = { true }) = KnockControl { packet ->
        if (admit()) {
            sent += packet.pack()
            true
        } else {
            false
        }
    }

    private fun samplePacket() = KnockParamPacket(
        useKnockChannel = 5,
        bpfFrequency = 99,
        kWndBeginAngle = -3.75f,
        kWndEndAngle = 9.375f,
        intTimeCost = 255,
        retardStep = -.03125f,
        advanceStep = -1f,
        maxRetard = .15625f,
        threshold = 1.25f,
        recoveryDelay = 7,
        selectedChanels = 0xA5,
        knkctlThrd = -10f,
    )

    @Test
    fun receiveAndResetArePassiveAndExposeNamedRawValues() {
        val sent = mutableListOf<IntArray>()
        val control = control(sent)
        assertNull(control.values.value)
        val packet = samplePacket()

        control.receive(packet)

        assertTrue(sent.isEmpty())
        assertEquals(KnockValues.from(packet), control.values.value)
        assertEquals(5, control.values.value?.useKnockChannel)
        assertEquals(99, control.values.value?.bpfFrequencyCode)
        assertEquals(255, control.values.value?.integrationTimeoutCode)
        assertEquals(-10f, control.values.value?.coolantThreshold)
        control.reset()
        assertNull(control.values.value)
        assertFalse(control.editWindowBegin(1f))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun everyNumericCommandUpdatesItsOwnFieldAndKeepsOtherRawFields() {
        val sent = mutableListOf<IntArray>()
        val control = control(sent)
        val packet = samplePacket()
        control.receive(packet)
        val original = KnockValues.from(packet)

        assertTrue(control.editWindowBegin(1.25f))
        assertEquals(1.25f, control.values.value?.windowBegin)
        assertEquals(original.copy(windowBegin = 1.25f), control.values.value)
        assertTrue(control.editWindowEnd(2.5f))
        assertTrue(control.editRetardStep(1.25f))
        assertTrue(control.editAdvanceStep(2.04f))
        assertTrue(control.editMaxRetard(2.5f))
        assertTrue(control.editVoltageThreshold(2.01f))
        assertTrue(control.editRecoveryDelay(17))
        assertTrue(control.editCoolantThreshold(12.25f))

        assertEquals(8, sent.size)
        assertEquals(1.25f, packet.kWndBeginAngle)
        assertEquals(2.5f, packet.kWndEndAngle)
        assertEquals(1.25f, packet.retardStep)
        assertEquals(2.04f, packet.advanceStep)
        assertEquals(2.5f, packet.maxRetard)
        assertEquals(2.01f, packet.threshold)
        assertEquals(17, packet.recoveryDelay)
        assertEquals(12.25f, packet.knkctlThrd)
        assertEquals(5, packet.useKnockChannel)
        assertEquals(99, packet.bpfFrequency)
        assertEquals(255, packet.intTimeCost)
        assertEquals(0xA5, packet.selectedChanels)
    }

    @Test
    fun numericCommandsEnforceBoundsFiniteValuesAndKeepDelayTypedAsInt() {
        val sent = mutableListOf<IntArray>()
        val control = control(sent)
        control.receive(KnockParamPacket())

        assertTrue(control.editWindowBegin(-12f))
        assertTrue(control.editWindowEnd(54f))
        assertTrue(control.editRetardStep(20f))
        assertTrue(control.editAdvanceStep(5f))
        assertTrue(control.editMaxRetard(25f))
        assertTrue(control.editVoltageThreshold(.1f))
        assertTrue(control.editRecoveryDelay(1))
        assertTrue(control.editRecoveryDelay(99))
        assertTrue(control.editCoolantThreshold(-40f))
        assertTrue(control.editCoolantThreshold(120f))
        val validCount = sent.size

        assertFalse(control.editWindowBegin(-12.01f))
        assertFalse(control.editWindowEnd(Float.NaN))
        assertFalse(control.editRetardStep(-.01f))
        assertFalse(control.editAdvanceStep(Float.POSITIVE_INFINITY))
        assertFalse(control.editMaxRetard(25.01f))
        assertFalse(control.editVoltageThreshold(.09f))
        assertFalse(control.editRecoveryDelay(0))
        assertFalse(control.editRecoveryDelay(100))
        assertFalse(control.editCoolantThreshold(120.01f))
        assertEquals(validCount, sent.size)
    }

    @Test
    fun selectorCodesUseFirmwareBoundsAndRetainUnknownReceivedCodesUntilSelection() {
        val sent = mutableListOf<IntArray>()
        val control = control(sent)
        val packet = samplePacket()
        control.receive(packet)

        assertTrue(control.editWindowBegin(1f))
        assertEquals(99, packet.bpfFrequency)
        assertEquals(255, packet.intTimeCost)
        assertTrue(control.selectBpfFrequency(0))
        assertTrue(control.selectBpfFrequency(63))
        assertTrue(control.selectIntegrationTimeout(0))
        assertTrue(control.selectIntegrationTimeout(31))
        val count = sent.size
        assertFalse(control.selectBpfFrequency(-1))
        assertFalse(control.selectBpfFrequency(64))
        assertFalse(control.selectIntegrationTimeout(-1))
        assertFalse(control.selectIntegrationTimeout(32))
        assertEquals(count, sent.size)
        assertEquals(63, packet.bpfFrequency)
        assertEquals(31, packet.intTimeCost)
    }

    @Test
    fun enableNormalizationAndChannelBitCommandsPreserveOtherBits() {
        val sent = mutableListOf<IntArray>()
        val control = control(sent)
        val packet = KnockParamPacket(useKnockChannel = 5, selectedChanels = 0xA5)
        control.receive(packet)

        assertTrue(control.editWindowBegin(1f))
        assertEquals(5, packet.useKnockChannel)
        for (channel in 0..7) {
            val before = packet.selectedChanels
            assertTrue(control.selectChannel(channel, true))
            assertEquals(before or (1 shl channel), packet.selectedChanels)
            val setMask = packet.selectedChanels
            assertTrue(control.selectChannel(channel, false))
            assertEquals(setMask and (1 shl channel).inv(), packet.selectedChanels)
        }
        val count = sent.size
        assertFalse(control.selectChannel(-1, true))
        assertFalse(control.selectChannel(8, true))
        assertEquals(count, sent.size)
        assertTrue(control.setKnockEnabled(true))
        assertEquals(1, packet.useKnockChannel)
        assertTrue(control.setKnockEnabled(false))
        assertEquals(0, packet.useKnockChannel)
    }

    @Test
    fun refusalRestoresMutatedPacketAndStateAndQueuedSnapshotsDoNotAlias() {
        val sent = mutableListOf<IntArray>()
        val queuedPackets = mutableListOf<OutputPacket>()
        var accepted = true
        val control = KnockControl { packet ->
            if (accepted) {
                queuedPackets += packet
                sent += packet.pack()
                true
            } else false
        }
        val packet = samplePacket()
        control.receive(packet)
        val original = packet.pack()
        val originalValues = control.values.value

        accepted = false
        assertFalse(control.editWindowBegin(4f))
        assertArrayEquals(original, packet.pack())
        assertEquals(originalValues, control.values.value)

        accepted = true
        assertTrue(control.editWindowBegin(4f))
        val queued = queuedPackets.single().pack()
        val originalSnapshot = queued.copyOf()
        queued[0] = 0
        assertArrayEquals(originalSnapshot, queuedPackets.single().pack())
        assertTrue(control.editWindowEnd(4f))
        assertFalse(queuedPackets.first().pack().contentEquals(packet.pack()))
    }

    @Test
    fun parsingAndEditingRetainsSignedWordsAndUnknownTail() {
        val raw = intArrayOf(
            '@'.code, 'w'.code, 2, 99, 0xFF, 0x88, 0x01, 0x2C, 255,
            0xFF, 0xFF, 0xFF, 0xE0, 0x00, 0x05, 0x01, 0xF4,
            7, 0xA5, 0xFF, 0xD8, 0xCA, 0xFE,
        )
        val packet = KnockParamPacket().parse(raw.copyOf()) as KnockParamPacket
        val sent = mutableListOf<IntArray>()
        val control = control(sent)
        control.receive(packet)

        assertTrue(control.editWindowBegin(0f))
        val packed = sent.single()
        assertEquals('w'.code, packed[0])
        assertEquals(2, packed[1])
        assertEquals(99, packed[2])
        assertArrayEquals(intArrayOf(0xFF, 0xFF, 0xFF, 0xE0, 0x00, 0x05), packed.sliceArray(8..13))
        assertArrayEquals(intArrayOf(0xFF, 0xD8), packed.sliceArray(18..19))
        assertArrayEquals(intArrayOf(0xCA, 0xFE), packed.takeLast(2).toIntArray())
    }
}
