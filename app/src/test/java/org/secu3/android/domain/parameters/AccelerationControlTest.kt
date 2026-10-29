package org.secu3.android.domain.parameters

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.AccelerationParamPacket

class AccelerationControlTest {
    private val injection = FirmwareInfoPacket(options = 1 shl 28)
    private val gasDose = FirmwareInfoPacket(options = 1 shl 29)

    private fun fixture(
        firmware: MutableStateFlow<FirmwareInfoPacket?> = MutableStateFlow(injection),
        admit: () -> Boolean = { true },
    ): Fixture {
        val sent = mutableListOf<IntArray>()
        val outgoing = mutableListOf<org.secu3.android.models.packets.base.OutputPacket>()
        val control = AccelerationControl(firmware) { snapshot ->
            val packed = snapshot.pack()
            if (admit()) {
                outgoing += snapshot
                sent += packed
                true
            } else false
        }
        return Fixture(control, sent, outgoing, firmware)
    }

    @Test
    fun receiveAndResetArePassiveAndPublishNamedRawValues() {
        val f = fixture()
        val packet = sample()
        f.control.receive(packet)

        assertTrue(f.sent.isEmpty())
        assertEquals(AccelerationValues.from(packet), f.control.values.value)
        assertEquals(42, f.control.values.value?.tpsThreshold)
        assertEquals(150f, f.control.values.value?.coldMultiplier)
        assertEquals(-25f, f.control.values.value?.xtauStart)
        f.control.reset()
        assertNull(f.control.values.value)
        assertFalse(f.control.editTpsThreshold(50))
        assertTrue(f.sent.isEmpty())
    }

    @Test
    fun typedNumericCommandsAndSelectorsUpdateOnlyTheirOwnFields() {
        val f = fixture()
        val packet = sample()
        f.control.receive(packet)

        assertTrue(f.control.editTpsThreshold(41))
        assertTrue(f.control.editColdMultiplier(250))
        assertTrue(f.control.editDecayTime(21))
        assertTrue(f.control.selectAeType(1))
        assertTrue(f.control.editAeTime(17))
        assertTrue(f.control.setBalance(50f))
        assertTrue(f.control.editMapdotThreshold(88))
        assertTrue(f.control.selectWallwetModel(2))
        assertTrue(f.control.editXtauStart(-37))
        assertTrue(f.control.editXtauFinish(-19))

        assertEquals(10, f.sent.size)
        assertEquals(41, packet.injAeTpsdotThrd)
        assertEquals(250f, packet.injAeColdaccMult)
        assertEquals(21, packet.injAeDecayTime)
        assertEquals(1, packet.injAeType)
        assertEquals(17, packet.injAeTime)
        assertEquals(50f, packet.injAeBallance)
        assertEquals(88, packet.injAeMapdotThrd)
        assertEquals(2, packet.wallwetModel)
        assertEquals(-37f, packet.injXtauSThrd)
        assertEquals(-19f, packet.injXtauFThrd)
    }

    @Test
    fun boundsAndLiveFieldGatesRejectStaleCommandsWithoutChangingState() {
        val f = fixture()
        val packet = sample().also { it.injAeType = 0; it.wallwetModel = 0 }
        f.control.receive(packet)
        assertFalse(f.control.editTpsThreshold(-1))
        assertFalse(f.control.editTpsThreshold(256))
        assertFalse(f.control.editColdMultiplier(99))
        assertFalse(f.control.editColdMultiplier(601))
        assertFalse(f.control.editDecayTime(-1))
        assertFalse(f.control.editDecayTime(256))
        assertFalse(f.control.editAeTime(10))
        assertFalse(f.control.editAeTime(-1))
        assertFalse(f.control.editAeTime(256))
        assertFalse(f.control.editMapdotThreshold(-1))
        assertFalse(f.control.editMapdotThreshold(256))
        assertFalse(f.control.setBalance(Float.NaN))
        assertFalse(f.control.setBalance(Float.POSITIVE_INFINITY))
        assertFalse(f.control.editXtauStart(-1))
        assertFalse(f.control.editXtauFinish(-256))
        assertFalse(f.control.editXtauFinish(1))
        assertFalse(f.control.selectAeType(2))
        assertFalse(f.control.selectAeType(-1))
        assertFalse(f.control.selectWallwetModel(4))
        assertFalse(f.control.selectWallwetModel(-1))
        assertEquals(0, f.sent.size)

        assertTrue(f.control.selectAeType(1))
        assertTrue(f.control.editAeTime(10))
        assertTrue(f.control.selectAeType(0))
        assertFalse("an editor opened in time mode is stale after switching back to pump", f.control.editAeTime(11))
        assertTrue(f.control.selectAeType(1))
        assertTrue(f.control.selectWallwetModel(1))
        assertTrue(f.control.editXtauStart(-1))
        assertTrue(f.control.selectWallwetModel(0))
        assertFalse("an Xtau editor is stale after disabling wallwet", f.control.editXtauFinish(-2))
        assertTrue(f.control.selectWallwetModel(1))
        assertTrue(f.control.editXtauFinish(-2))
        assertFalse(f.control.editAeTime(256))
        assertFalse(f.control.editXtauFinish(-256))
    }

    @Test
    fun firmwareRulesAreRecheckedAndUnknownFirmwareKeepsPermissiveFieldPreview() {
        val f = fixture()
        val packet = sample()
        f.control.receive(packet)
        assertTrue(f.control.editTpsThreshold(50))

        f.firmware.value = gasDose
        assertTrue("gas-dose-only firmware supports the base page", f.control.editTpsThreshold(51))
        assertFalse("AE_TIME requires fuel injection", f.control.editAeTime(10))
        assertFalse("wallwet requires fuel injection", f.control.selectWallwetModel(2))
        assertFalse("Xtau requires fuel injection", f.control.editXtauStart(-2))

        f.firmware.value = FirmwareInfoPacket(options = 0)
        assertFalse(f.control.editTpsThreshold(52))
        assertEquals(2, f.sent.size)

        f.firmware.value = null
        assertTrue("unknown firmware keeps permissive field capabilities", f.control.editTpsThreshold(53))
        assertTrue(f.control.editAeTime(11))
        assertTrue(f.control.selectWallwetModel(2))
        assertEquals(5, f.sent.size)
    }

    @Test
    fun unrelatedEditsPreserveUnsignedColdWordUnknownCodesAndTail() {
        val payload = intArrayOf(0, 0x80, 0x00, 4, 9, 20, 255, 5, 255, 254, 6, 0xCA, 0xFE)
        val packet = AccelerationParamPacket().parse(intArrayOf('@'.code, '|'.code) + payload) as AccelerationParamPacket
        val f = fixture()
        f.control.receive(packet)
        assertEquals(100f + 32768f * 100f / 128f, f.control.values.value!!.coldMultiplier, .0001f)
        assertTrue(f.control.editTpsThreshold(77))
        assertEquals(0x80, f.sent.single()[2])
        assertEquals(0x00, f.sent.single()[3])
        assertEquals(9, packet.injAeType)
        assertEquals(6, packet.wallwetModel)
        assertEquals(255, f.sent.single()[7])
        assertArrayEquals(intArrayOf(0xCA, 0xFE), f.sent.single().takeLast(2).toIntArray())
    }

    @Test
    fun coldMultiplierWordOneAndUnsignedHighWordsSurviveUnrelatedEdits() {
        listOf(1, 0x8000, 0xFFFF).forEach { word ->
            val payload = intArrayOf(10, word shr 8, word and 0xFF, 20, 1, 30, 255, 40, 1, 2, 3)
            val packet = AccelerationParamPacket().parse(intArrayOf('@'.code, '|'.code) + payload) as AccelerationParamPacket
            val f = fixture()
            f.control.receive(packet)

            assertEquals((word + 128).toFloat() / 128f * 100f, f.control.values.value!!.coldMultiplier, 0f)
            assertTrue(f.control.editTpsThreshold(11))
            assertEquals(word shr 8, f.sent.single()[2])
            assertEquals(word and 0xFF, f.sent.single()[3])
        }
    }

    @Test
    fun rollbackAndSnapshotAreNonVacuousAndSnapshotCannotBeMutated() {
        var admit = false
        val f = fixture(admit = { admit })
        val packet = sample()
        f.control.receive(packet)
        val beforeValues = f.control.values.value
        val beforePacked = packet.pack()
        assertFalse(f.control.editTpsThreshold(73))
        assertEquals(beforeValues, f.control.values.value)
        assertArrayEquals(beforePacked, packet.pack())

        admit = true
        assertTrue(f.control.editTpsThreshold(74))
        val firstSnapshot = f.outgoing.single().pack()
        firstSnapshot[1] = 0
        assertEquals(74, f.outgoing.single().pack()[1])
        assertEquals(74, packet.injAeTpsdotThrd)
    }

    @Test
    fun outgoingWireFieldsMatchFrozenExpectedPayload() {
        val f = fixture()
        val packet = sample().also {
            it.injAeType = 1
            it.wallwetModel = 1
        }
        f.control.receive(packet)
        assertTrue(f.control.setBalance(100f))
        assertArrayEquals(
            intArrayOf('|'.code, 42, 0, 64, 7, 1, 12, 255, 13, 25, 8, 1),
            f.sent.single(),
        )
        assertTrue(f.control.editColdMultiplier(250))
        assertEquals(0x00, f.sent.last()[2])
        assertEquals(0xC0, f.sent.last()[3])
    }

    private fun sample() = AccelerationParamPacket(
        injAeTpsdotThrd = 42,
        injAeColdaccMult = 150f,
        injAeDecayTime = 7,
        injAeType = 1,
        injAeTime = 12,
        injAeBallance = 50f,
        injAeMapdotThrd = 13,
        injXtauSThrd = -25f,
        injXtauFThrd = -8f,
        wallwetModel = 1,
    )

    private data class Fixture(
        val control: AccelerationControl,
        val sent: MutableList<IntArray>,
        val outgoing: MutableList<org.secu3.android.models.packets.base.OutputPacket>,
        val firmware: MutableStateFlow<FirmwareInfoPacket?>,
    )
}
