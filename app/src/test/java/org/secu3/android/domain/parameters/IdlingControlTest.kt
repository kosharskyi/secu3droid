package org.secu3.android.domain.parameters

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.IdlingParamPacket

class IdlingControlTest {
    private fun packet() = IdlingParamPacket(
        idlFlags = 0x80,
        coefThrd1 = 1.25f,
        coefThrd2 = 1.5f,
        iFac1 = -10f,
        iFac2 = 11f,
        idlRegP0 = 5.1f,
        idlRegP1 = -0.25f,
    )

    @Test
    fun receiveIsPassiveAndPublishedValuesUseExplicitWireSigns() {
        val sent = mutableListOf<IntArray>()
        val control = IdlingControl({ null }, { sent += it.pack(); true })
        val received = packet()
        control.receive(received)

        assertTrue(sent.isEmpty())
        assertEquals(-10f, control.values.value?.iFac1)
        assertEquals(11f, control.values.value?.iFac2)
        assertEquals(5.1f, control.values.value?.proportionalNegative)
        assertEquals(-0.25f, control.values.value?.proportionalPositive)
    }

    @Test
    fun receiveMapsAll24RawNumericFieldsAndSevenFlagsByName() {
        val control = IdlingControl({ null }, { false })
        control.receive(IdlingParamPacket(
            idlFlags = 0b0111_1111,
            iFac1 = -1.25f, iFac2 = 2.5f, idlregMaxAngle = 20f, idlregMinAngle = -5f,
            idlingRpm = 800, minefr = 30, idlregTurnOnTemp = 50f, idlToRunAdd = 10f,
            rpmOnRunAdd = 100, idlRegP0 = .1f, idlRegP1 = .2f, idlRegI0 = .3f,
            idlRegI1 = .4f, coefThrd1 = 1.2f, coefThrd2 = 1.3f, integratorRpmLim = 500,
            mapValue = 40f, iacMinPos = 10f, iacMaxPos = 90f, iacRegDb = 20,
            idlRegD = .5f, irrKLoad = 3f, irrKRpm = 4f, loadIdlThrd = 5f,
        ))
        val values = requireNotNull(control.values.value)
        assertEquals(
            listOf(-1.25f, 2.5f, 20f, -5f, 800f, 30f, 50f, 10f, 100f, .1f, .3f, .2f, .4f,
                1.2f, 1.3f, 500f, 40f, 10f, 90f, 20f, .5f, 3f, 4f, 5f),
            listOf(values.iFac1, values.iFac2, values.maxAngle, values.minAngle, values.targetRpm.toFloat(),
                values.deadBandRpm.toFloat(), values.turnOnTemperature, values.iacAddAfterExit,
                values.rpmAddOnRun.toFloat(), values.proportionalNegative, values.integralNegative,
                values.proportionalPositive, values.integralPositive, values.transientThreshold1,
                values.transientThreshold2, values.integratorRpmLimit.toFloat(), values.mapThreshold,
                values.iacMinimumPosition, values.iacMaximumPosition, values.iacDeadBandRpm.toFloat(),
                values.differential, values.rigidityLoad, values.rigidityRpm, values.loadThreshold),
        )
        assertEquals(listOf(true, true, true, true, true, true, true), listOf(
            values.regulatorEnabled, values.regulatorOnGas, values.proportionalMode, values.closedLoop,
            values.throttleAssistMap, values.closedLoopOnGas, values.worksWithIacRegulator,
        ))
    }

    @Test
    fun positiveAndNegativePAndICommandsUseFirmwareWireIndexes() {
        val sent = mutableListOf<IntArray>()
        val control = IdlingControl({ null }, { sent += it.pack(); true })
        val p = packet()
        control.receive(p)

        assertTrue(control.editProportionalPositive(2f))
        assertTrue(control.editProportionalNegative(3f))
        assertTrue(control.editIntegralPositive(4f))
        assertTrue(control.editIntegralNegative(1f))

        assertEquals(2f, p.idlRegP1)
        assertEquals(3f, p.idlRegP0)
        assertEquals(4f, p.idlRegI1)
        assertEquals(1f, p.idlRegI0)
        // Frozen payload words: descriptor offset is 0; P-/P+/I-/I+ are 18..25.
        assertArrayEquals(intArrayOf(2, 0), sent[0].sliceArray(20..21))
        assertArrayEquals(intArrayOf(3, 0), sent[1].sliceArray(18..19))
        assertArrayEquals(intArrayOf(4, 0), sent[2].sliceArray(24..25))
        assertArrayEquals(intArrayOf(1, 0), sent[3].sliceArray(22..23))
    }

    @Test
    fun all24NamedNumericCommandsAndSevenFlagsPublishTheirOwnFields() {
        val sent = mutableListOf<IntArray>()
        val control = IdlingControl({ null }, { sent += it.pack(); true })
        val packet = packet()
        control.receive(packet)
        val edits = listOf<() -> Boolean>(
            { control.editIFac1(2f) }, { control.editIFac2(3f) },
            { control.editMaxAngle(10f) }, { control.editMinAngle(-3f) },
            { control.editTargetRpm(450) }, { control.editDeadBand(120) },
            { control.editTurnOnTemperature(99f) }, { control.editIacAddAfterExit(20f) },
            { control.editRpmAddOnRun(125) }, { control.editProportionalNegative(1.1f) },
            { control.editIntegralNegative(1.2f) }, { control.editProportionalPositive(2.1f) },
            { control.editIntegralPositive(2.2f) }, { control.editTransientThreshold1(1.6f) },
            { control.editTransientThreshold2(1.7f) }, { control.editIntegratorRpmLimit(20) },
            { control.editMapThreshold(44f) }, { control.editIacMinimumPosition(12f) },
            { control.editIacMaximumPosition(88f) }, { control.editIacDeadBand(30) },
            { control.editDifferential(1.3f) }, { control.editRigidityLoad(2.5f) },
            { control.editRigidityRpm(3.5f) }, { control.editLoadThreshold(50f) },
        )
        edits.forEach { assertTrue(it()) }
        val flags = listOf<() -> Boolean>(
            { control.setRegulatorEnabled(true) }, { control.setRegulatorOnGas(true) },
            { control.setProportionalMode(true) }, { control.setClosedLoop(true) },
            { control.setThrottleAssistMap(true) }, { control.setClosedLoopOnGas(true) },
            { control.setWorksWithIacRegulator(true) },
        )
        flags.forEach { assertTrue(it()) }

        assertEquals(31, sent.size)
        assertEquals(
            listOf(2f, 3f, 10f, -3f, 450f, 120f, 99f, 20f, 125f, 1.1f, 1.2f, 2.1f,
                2.2f, 1.6f, 1.7f, 20f, 44f, 12f, 88f, 30f, 1.3f, 2.5f, 3.5f, 50f),
            listOf(
                control.values.value!!.iFac1, control.values.value!!.iFac2, control.values.value!!.maxAngle,
                control.values.value!!.minAngle, control.values.value!!.targetRpm.toFloat(),
                control.values.value!!.deadBandRpm.toFloat(), control.values.value!!.turnOnTemperature,
                control.values.value!!.iacAddAfterExit, control.values.value!!.rpmAddOnRun.toFloat(),
                control.values.value!!.proportionalNegative, control.values.value!!.integralNegative,
                control.values.value!!.proportionalPositive, control.values.value!!.integralPositive,
                control.values.value!!.transientThreshold1, control.values.value!!.transientThreshold2,
                control.values.value!!.integratorRpmLimit.toFloat(), control.values.value!!.mapThreshold,
                control.values.value!!.iacMinimumPosition, control.values.value!!.iacMaximumPosition,
                control.values.value!!.iacDeadBandRpm.toFloat(), control.values.value!!.differential,
                control.values.value!!.rigidityLoad, control.values.value!!.rigidityRpm,
                control.values.value!!.loadThreshold,
            ),
        )
        val values = requireNotNull(control.values.value)
        assertTrue(values.regulatorEnabled && values.regulatorOnGas && values.proportionalMode)
        assertTrue(values.closedLoop && values.throttleAssistMap && values.closedLoopOnGas && values.worksWithIacRegulator)
        assertEquals(0x80, packet.idlFlags and 0x80)
    }

    @Test
    fun limitsNonfiniteValuesAndStaleThrottleGateAreRejectedWithoutWrites() {
        var firmware: FirmwareInfoPacket? = null
        val sent = mutableListOf<IntArray>()
        val control = IdlingControl({ firmware }, { sent += it.pack(); true })
        val p = packet()
        control.receive(p)

        assertFalse(control.editIacAddAfterExit(101f))
        assertFalse(control.editTargetRpm(1801))
        assertFalse(control.editDifferential(Float.NaN))
        assertTrue(control.setThrottleAssistMap(true))
        assertFalse(control.editIacAddAfterExit(12f))
        assertTrue(control.setThrottleAssistMap(false))
        assertTrue(control.editIacAddAfterExit(12f))
        assertEquals(3, sent.size)
        assertEquals(12f, p.idlToRunAdd)
    }

    @Test
    fun firmwareGatesAreLiveAndUnknownFirmwareRemainsPermissive() {
        var firmware: FirmwareInfoPacket? = FirmwareInfoPacket(options = 0)
        val sent = mutableListOf<IntArray>()
        val control = IdlingControl({ firmware }, { sent += it.pack(); true })
        control.receive(packet())
        assertFalse(control.editMapThreshold(30f))
        assertTrue(control.editIFac1(2f))
        assertTrue(control.setRegulatorEnabled(true))
        assertFalse(control.setClosedLoop(true))

        firmware = FirmwareInfoPacket(options = 1 shl 28)
        assertTrue(control.editMapThreshold(30f))
        assertTrue(control.setClosedLoop(true))
        firmware = FirmwareInfoPacket(options = 0)
        assertFalse(control.editMapThreshold(31f))

        val unknown = IdlingControl({ null }, { true })
        unknown.receive(packet())
        assertTrue(unknown.editDifferential(1f))
    }

    @Test
    fun refusedCommandRestoresKnownPacketFieldsAndSnapshotDoesNotAliasArrays() {
        val outgoing = mutableListOf<org.secu3.android.models.packets.base.OutputPacket>()
        val control = IdlingControl({ null }, { outgoing += it; false })
        val p = packet()
        val before = p.pack()
        control.receive(p)

        assertFalse(control.editIFac1(2f))
        assertEquals(-10f, p.iFac1)
        assertEquals(before.toList(), p.pack().toList())
        val frozen = outgoing.single().pack()
        val sentSnapshot = frozen.copyOf()
        frozen[2] = 99
        assertNotSame(frozen, outgoing.single().pack())
        assertEquals(sentSnapshot.toList(), outgoing.single().pack().toList())
        assertFalse(before.contentEquals(sentSnapshot))
    }

    @Test
    fun unrelatedEditsPreserveRawSignedAndUnsignedOutOfBoundsFieldsUnknownFlagsAndTail() {
        val sent = mutableListOf<IntArray>()
        val control = IdlingControl({ null }, { sent += it.pack(); true })
        val encoded = packet().also {
            it.idlFlags = 0x80
            it.iFac1 = -127.5f
            it.iFac2 = 11f
            it.idlregMinAngle = -15.25f
            it.idlRegD = 9f
            it.iacMinPos = 127.5f
        }.pack()
        val parsed = IdlingParamPacket().parse(intArrayOf('@'.code) + encoded + intArrayOf(0xCA, 0xFE)) as IdlingParamPacket
        val before = parsed.pack()
        assertEquals(-127.5f, parsed.iFac1)
        assertEquals(11f, parsed.iFac2)
        assertEquals(127.5f, parsed.iacMinPos)
        assertEquals(0x80, parsed.idlFlags and 0x80)
        control.receive(parsed)

        assertTrue(control.setRegulatorEnabled(true))
        assertEquals(-127.5f, parsed.iFac1)
        assertEquals(11f, parsed.iFac2)
        assertEquals(-15.25f, parsed.idlregMinAngle)
        assertEquals(9f, parsed.idlRegD)
        assertEquals(127.5f, parsed.iacMinPos)
        assertEquals(0x80, parsed.idlFlags and 0x80)
        assertArrayEquals(intArrayOf(0xCA, 0xFE), sent.single().copyOfRange(43, sent.single().size))
        assertArrayEquals(before.copyOfRange(43, before.size), sent.single().copyOfRange(43, sent.single().size))
    }

    @Test
    fun resetReturnsToNullAndNonStepIntegerRpmValuesRemainRawInState() {
        val sent = mutableListOf<IntArray>()
        val control = IdlingControl({ null }, { sent += it.pack(); true })
        val p = packet()
        control.receive(p)
        assertTrue(control.editRpmAddOnRun(15))
        assertTrue(control.editIntegratorRpmLimit(19))
        assertEquals(2, sent[0][17])
        assertEquals(2, sent[1][28])
        assertEquals(15, control.values.value?.rpmAddOnRun)
        assertEquals(19, control.values.value?.integratorRpmLimit)
        control.reset()
        assertEquals(null, control.values.value)
        assertFalse(control.editIFac1(1f))
    }
}
