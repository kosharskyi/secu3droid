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
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket

class MiscellaneousControlTest {
    private fun control(
        firmware: MutableStateFlow<FirmwareInfoPacket?> = MutableStateFlow(null),
        sent: MutableList<OutputPacket> = mutableListOf(),
        admit: Boolean = true,
    ) = MiscellaneousControl(firmware) { packet -> sent += packet; admit }

    private fun validPacket() = MiscellaneousParamPacket(
        uartDivisor = 0x0103,
        uartPeriodTms = 100,
        ignCutoff = 1,
        ignCutoffThrd = 5000,
        hopStartAng = 33f / 32f,
        hopDuratAng = 65f / 32f,
        flpmpFlags = 0xA8,
        evapAfbegin = 32000,
        evapAfslope = 32f / 40000f,
        fpTimeoutStrt = 3f,
        pwmFrq0 = 100,
        pwmFrq1 = 200,
        vssPeriodDist = 1000f,
    )

    @Test fun receiveDoesNotEchoAndRawPrecisionSurvivesUnrelatedEdits() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val packet = validPacket().apply { vssPeriodDist = 32768000f / 65275f }
        control.receive(packet)

        assertTrue(sent.isEmpty())
        assertEquals(33f / 32f, control.values.value?.start ?: 0f, 0f)
        assertEquals(65f / 32f, control.values.value?.duration ?: 0f, 0f)
        assertEquals(32768000f / 65275f, control.values.value?.vss ?: 0f, 0f)
        assertTrue(control.editPeriod(120))
        assertEquals(33f / 32f, packet.hopStartAng, 0f)
        assertEquals(65f / 32f, packet.hopDuratAng, 0f)
        assertEquals(32768000f / 65275f, packet.vssPeriodDist, 0f)
        assertArrayEquals(intArrayOf(0xFE, 0xFB), sent.single().pack().sliceArray(21..22))

        assertTrue(control.editStart(-4))
        assertTrue(control.editDuration(10))
        assertTrue(control.editVss(1200))
        assertEquals(-4f, control.values.value?.start ?: 0f, 0f)
        assertEquals(10f, control.values.value?.duration ?: 0f, 0f)
        assertEquals(1200f, control.values.value?.vss ?: 0f, 0f)
    }

    @Test fun numericEditorsMapSupportedBoundariesAndRejectOutsideValues() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val packet = validPacket()
        control.receive(packet)

        assertTrue(control.editPeriod(0))
        assertEquals(0, packet.uartPeriodTms)
        assertTrue(control.editPeriod(500))
        assertEquals(500, packet.uartPeriodTms)

        assertTrue(control.editCutoff(1000))
        assertEquals(1000, packet.ignCutoffThrd)
        assertTrue(control.editCutoff(30000))
        assertEquals(30000, packet.ignCutoffThrd)

        assertTrue(control.editStart(-36))
        assertEquals(-36f, packet.hopStartAng, 0f)
        assertTrue(control.editStart(72))
        assertEquals(72f, packet.hopStartAng, 0f)
        assertTrue(control.editDuration(6))
        assertEquals(6f, packet.hopDuratAng, 0f)
        assertTrue(control.editDuration(180))
        assertEquals(180f, packet.hopDuratAng, 0f)

        assertTrue(control.editTimeout(0.1f))
        assertEquals(0.1f, packet.fpTimeoutStrt, 0f)
        assertTrue(control.editTimeout(25.5f))
        assertEquals(25.5f, packet.fpTimeoutStrt, 0f)
        assertTrue(control.editPwm0(10))
        assertEquals(10, packet.pwmFrq0)
        assertTrue(control.editPwm0(5000))
        assertEquals(5000, packet.pwmFrq0)
        assertTrue(control.editPwm1(10))
        assertEquals(10, packet.pwmFrq1)
        assertTrue(control.editPwm1(5000))
        assertEquals(5000, packet.pwmFrq1)
        assertTrue(control.editVss(100))
        assertEquals(100f, packet.vssPeriodDist, 0f)
        assertTrue(control.editVss(60000))
        assertEquals(60000f, packet.vssPeriodDist, 0f)
        assertTrue(control.editVss(1200))
        assertEquals(1200f, packet.vssPeriodDist, 0f)
        assertArrayEquals(intArrayOf(0x6A, 0xAB), packet.pack().sliceArray(21..22))

        val before = packet.pack().toList()
        val acceptedWrites = sent.size
        assertFalse(control.editPeriod(-1))
        assertFalse(control.editPeriod(501))
        assertFalse(control.editCutoff(999))
        assertFalse(control.editCutoff(30001))
        assertFalse(control.editStart(-37))
        assertFalse(control.editStart(73))
        assertFalse(control.editDuration(5))
        assertFalse(control.editDuration(181))
        assertFalse(control.editTimeout(0f))
        assertFalse(control.editTimeout(25.6f))
        assertFalse(control.editTimeout(Float.NaN))
        assertFalse(control.editPwm0(9))
        assertFalse(control.editPwm0(5001))
        assertFalse(control.editPwm1(9))
        assertFalse(control.editPwm1(5001))
        assertFalse(control.editVss(99))
        assertFalse(control.editVss(60001))
        assertEquals(before, packet.pack().toList())
        assertEquals(acceptedWrites, sent.size)
        assertTrue(control.values.value?.hasValidationError == true)
    }

    @Test fun baudCatalogPreservesUnknownReceivedCodeAndOnlyAdmitsKnownSelection() {
        assertEquals(
            linkedMapOf(0x0411 to 2400, 0x0208 to 4800, 0x0103 to 9600, 0x00AD to 14400, 0x0081 to 19200,
                0x0056 to 28800, 0x0040 to 38400, 0x002A to 57600, 0x0015 to 115200, 0x0009 to 250000, 0x0004 to 500000),
            MiscellaneousControl.BAUD_RATES,
        )
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        control.receive(validPacket().apply { uartDivisor = 0x1234 })
        assertEquals(0x1234, control.values.value?.baudDivisor)
        assertFalse(control.selectBaudDivisor(0x1234))
        assertEquals(0x1234, control.values.value?.baudDivisor)
        assertTrue(control.selectBaudDivisor(0x0103))
        assertEquals(0x0103, control.values.value?.baudDivisor)
    }

    @Test fun evapBoundsAndNearUpperLimitQuantizationKeepModelAndWirePairsSafe() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val packet = validPacket()
        control.receive(packet)
        assertEquals(100 to 55500, control.values.value?.beginBounds)
        assertEquals(48500 to 2_000_000, control.values.value?.endBounds)

        listOf(1_983_472 to 2_000_000, 1_983_464 to 2_000_000, 1_008 to 17_508, 1_016 to 17_516).forEach { (begin, end) ->
            val next = validPacket().apply { evapAfbegin = begin; evapAfEnd = end }
            control.receive(next)
            assertTrue(control.editEvapBegin(begin))
            assertTrue(next.evapAfEnd in 100..2_000_000)
            assertTrue(next.evapAfEnd - begin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
            val packed = sent.last().pack()
            val parsed = MiscellaneousParamPacket().parse(intArrayOf(0x40, 'z'.code) + packed.drop(1).toIntArray()) as MiscellaneousParamPacket
            assertTrue(parsed.evapAfEnd in 100..2_000_000)
            assertTrue(parsed.evapAfEnd - parsed.evapAfbegin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        }
    }

    @Test fun minimumEvapEndDeltaSurvivesWireQuantizationAtHalfStartCounts() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        listOf(1008 to 17508, 1016 to 17516).forEach { (begin, requestedEnd) ->
            val packet = validPacket().apply {
                evapAfbegin = begin
                evapAfEnd = begin + 20_000
            }
            control.receive(packet)
            assertTrue(control.editEvapEnd(requestedEnd))
            assertTrue(packet.evapAfEnd - packet.evapAfbegin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)

            val packed = sent.last().pack()
            val parsed = MiscellaneousParamPacket().parse(intArrayOf(0x40, 'z'.code) + packed.drop(1).toIntArray()) as MiscellaneousParamPacket
            assertTrue(parsed.evapAfEnd in 100..2_000_000)
            assertTrue(parsed.evapAfEnd - parsed.evapAfbegin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        }
    }

    @Test fun invalidEvapBlocksOtherEditsButRepairCanSucceedAndPreserveUnknownData() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val source = validPacket().apply { vssPeriodDist = 32768000f / 65275f }
        val input = intArrayOf(0x40, 'z'.code) + source.pack().drop(1).toIntArray() + intArrayOf(0xCA, 0xFE)
        input[15] = 0
        input[16] = 0
        val packet = MiscellaneousParamPacket().parse(input) as MiscellaneousParamPacket
        control.receive(packet)

        assertFalse(control.editPeriod(150))
        assertTrue(control.values.value?.hasValidationError == true)
        assertTrue(sent.isEmpty())
        assertEquals(100, control.values.value?.period)
        assertEquals(32768000f / 65275f, control.values.value?.vss ?: 0f, 0f)

        assertTrue(control.editEvapEnd(100000))
        assertFalse(control.values.value?.hasValidationError == true)
        val snapshot = sent.single().pack()
        assertArrayEquals(intArrayOf(0xCA, 0xFE), snapshot.takeLast(2).toIntArray())
        assertTrue(packet.pack().isNotEmpty())
        assertEquals(32768000f / 65275f, packet.vssPeriodDist, 0f)
        assertTrue(control.values.value!!.end - control.values.value!!.begin >= MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA)
        assertTrue(control.editPeriod(150))
    }

    @Test fun failedCandidateRollsBackScalarsAndRetainsUnknownFlagsAndTail() {
        val sent = mutableListOf<OutputPacket>()
        val control = control(sent = sent)
        val source = validPacket().apply { evapAfbegin = 5000; evapAfEnd = 22000; flpmpFlags = 0xA8 }
        val input = intArrayOf(0x40, 'z'.code) + source.pack().drop(1).toIntArray() + intArrayOf(0xCA, 0xFE)
        val packet = MiscellaneousParamPacket().parse(input) as MiscellaneousParamPacket
        control.receive(packet)
        val before = packet.pack()
        assertFalse(control.editEvapEnd(21491))
        assertEquals(before.toList(), packet.pack().toList())
        assertTrue(control.values.value?.hasValidationError == true)
        assertTrue(sent.isEmpty())
        assertTrue(control.setPumpOffOnGas(true))
        assertEquals(0xA9, packet.flpmpFlags)
        assertArrayEquals(intArrayOf(0xCA, 0xFE), sent.single().pack().takeLast(2).toIntArray())
    }

    @Test fun firmwareAndCurrentCutoffGuardsRejectUnsupportedActions() {
        val firmware = MutableStateFlow<FirmwareInfoPacket?>(FirmwareInfoPacket(options = 0))
        val sent = mutableListOf<OutputPacket>()
        val control = control(firmware, sent)
        val packet = validPacket()
        control.receive(packet)
        assertFalse(control.editStart(5))
        assertFalse(control.editDuration(20))
        assertFalse(control.setInjectorsOffOnGas(true))
        assertFalse(control.setInjectorsOffOnPetrol(true))
        assertFalse(control.editEvapBegin(40000))
        assertEquals(0, sent.size)

        firmware.value = FirmwareInfoPacket(options = (1 shl 28) or (1 shl 19))
        assertTrue(control.editStart(5))
        assertTrue(control.editDuration(20))
        assertTrue(control.setInjectorsOffOnGas(true))
        assertTrue(control.editEvapBegin(40000))

        packet.ignCutoff = 0
        assertFalse(control.editCutoff(6000))
        assertTrue(sent.size == 4)
        assertFalse(control.values.value?.hasValidationError == true)
    }

    @Test fun sendAdmissionResultAndSnapshotsAreImmutable() {
        val snapshots = mutableListOf<OutputPacket>()
        val control = control(sent = snapshots)
        val packet = validPacket()
        control.receive(packet)
        assertTrue(control.editPeriod(110))
        val acceptedBytes = snapshots.single().pack()
        assertTrue(control.editPeriod(120))
        assertEquals(110, acceptedBytes[3] * 10)
        assertEquals(110, snapshots.first().pack()[3] * 10)
        assertNotEquals(snapshots.first().pack().toList(), snapshots.last().pack().toList())
        val exposed = snapshots.first().pack()
        exposed[3] = 99
        assertEquals(11, snapshots.first().pack()[3])

        val denied = control(sent = mutableListOf(), admit = false)
        denied.receive(validPacket())
        assertFalse(denied.editPeriod(120))
        assertEquals(120, denied.values.value?.period)
        denied.reset()
        assertEquals(null, denied.values.value)
    }
}
