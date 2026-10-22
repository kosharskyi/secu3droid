package org.secu3.android.ui.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.secu3.android.models.packets.out.DiagOutputPacket

class DiagnosticOutputControllerTest {
    private data class Snapshot(val out: Int, val channel: Int, val frequency: Float, val duty: Float)

    @Test
    fun channelAlwaysSendsButFrequencyAndDutySendOnlyForSelectedChannel() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Snapshot>()
        val controller = DiagnosticOutputController(packet) { sends += packet.snapshot() }
        controller.initializeIgnitionOutputs(isSecu3T = false)

        controller.editFrequency(123f)
        controller.editDuty(37.5f)
        assertEquals(123f, packet.frq)
        assertEquals(37.5f, packet.duty)
        assertEquals(emptyList<Snapshot>(), sends)

        controller.selectChannel(0)
        controller.selectChannel(4)
        controller.editFrequency(234f)
        controller.editDuty(42f)
        assertEquals(
            listOf(
                Snapshot(0, 0, 123f, 37.5f),
                Snapshot(0, 4, 123f, 37.5f),
                Snapshot(0, 4, 234f, 37.5f),
                Snapshot(0, 4, 234f, 42f),
            ),
            sends,
        )
    }

    @Test
    fun firstDiagnosticInputInitializesAndSendsSafeSecu3TIgnitionMaskOnce() {
        val packet = DiagOutputPacket().apply { out = (1 shl 5) or (1 shl 1) }
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }

        controller.onDiagnosticInput(isSecu3T = true)
        assertEquals((1 shl 5) or 0b11, packet.out)
        assertEquals(listOf((1 shl 5) or 0b11), sends)

        controller.setOutput(DiagnosticOutput.IGN_OUT3, true)
        val userState = packet.out
        controller.onDiagnosticInput(isSecu3T = false)
        controller.onDiagnosticInput(isSecu3T = false)
        assertEquals(userState, packet.out)
        assertEquals(2, sends.size)
    }

    @Test
    fun firstDiagnosticInputInitializesSecu3iIgnitionMaskToLowAndSendsOnce() {
        val packet = DiagOutputPacket().apply { out = (1 shl 5) or 0b11 }
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }

        controller.onDiagnosticInput(isSecu3T = false)
        assertEquals(1 shl 5, packet.out)
        assertEquals(listOf(1 shl 5), sends)
        controller.onDiagnosticInput(isSecu3T = true)
        assertEquals(1 shl 5, packet.out)
        assertEquals(1, sends.size)
    }

    @Test
    fun connectionLossResetsLocalOutputsAndChannelWithoutSendingAndPreservesGeneratorValues() {
        val packet = DiagOutputPacket().apply { frq = 432f; duty = 37.5f }
        val sends = mutableListOf<Snapshot>()
        val controller = DiagnosticOutputController(packet) { sends += packet.snapshot() }
        controller.initializeIgnitionOutputs(isSecu3T = true)
        controller.onDiagnosticInput(isSecu3T = true)
        controller.selectChannel(3)
        controller.editFrequency(123f)
        controller.editDuty(25f)
        controller.setOutput(DiagnosticOutput.IGN_OUT4, true)
        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        controller.setOutput(DiagnosticOutput.BL, true)
        controller.setOutput(DiagnosticOutput.DE, true)
        controller.setOutput(DiagnosticOutput.TACH_O, true)
        sends.clear()

        controller.resetForConnectionLoss(firmwareFamilyIsSecu3T = true)

        assertEquals(0b11, packet.out)
        assertEquals(0, packet.chan)
        assertEquals(123f, packet.frq)
        assertEquals(25f, packet.duty)
        assertEquals(false, packet.blOutputEnabled)
        assertEquals(false, packet.deOutputEnabled)
        assertEquals(false, packet.tachOOutputEnabled)
        assertEquals(emptyList<Snapshot>(), sends)

        controller.resetForConnectionLoss(firmwareFamilyIsSecu3T = null)
        assertEquals(0b11, packet.out)
        controller.onDiagnosticInput(isSecu3T = false)
        assertEquals(0, packet.out)
        assertEquals(1, sends.size)
        assertEquals(0, sends.single().channel)
        assertEquals(123f, sends.single().frequency)
        assertEquals(25f, sends.single().duty)
    }

    @Test
    fun unknownFamilySuppressesUserSendsUntilFirstDiagnosticInputInitializesPacket() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }

        controller.enableBlDe(true)
        assertEquals(emptyList<Int>(), sends)
        controller.onDiagnosticInput(isSecu3T = true)
        assertEquals(listOf((1 shl 12) or (1 shl 14) or 0b11), sends)
    }

    @Test
    fun knownSecu3TFirmwarePreparesBeforeCommandsAndFirstInputPreservesUserState() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }

        controller.initializeIgnitionOutputs(isSecu3T = true)
        assertEquals(0b11, packet.out)
        assertEquals(emptyList<Int>(), sends)

        controller.setOutput(DiagnosticOutput.IGN_OUT1, false)
        assertEquals(listOf(0b10), sends)
        controller.onDiagnosticInput(isSecu3T = true)
        controller.onDiagnosticInput(isSecu3T = true)
        assertEquals(0b10, packet.out)
        assertEquals(listOf(0b10, 0b10), sends)
    }

    @Test
    fun additionalOptionsDisableBlDeThenTachoWithSeparatePacketSnapshots() {
        val packet = DiagOutputPacket().apply { bl = true; de = true; tachO = true }
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }
        controller.initializeIgnitionOutputs(isSecu3T = false)

        controller.applyAdditionalOptions(enableBlDe = false, enableTachO = false)

        assertEquals(listOf(1 shl 23 or (1 shl 24), 0), sends)
        assertEquals(0, packet.out)
    }

    @Test
    fun freshChannelStartsAtFiftyPercentAndSpecialOutputRequiresDirectionEnable() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Snapshot>()
        val controller = DiagnosticOutputController(packet) { sends += packet.snapshot() }
        controller.initializeIgnitionOutputs(isSecu3T = false)

        controller.selectChannel(0)
        assertEquals(50f, packet.duty)
        assertEquals(128, packet.pack()[7])
        controller.editDuty(25f)
        controller.selectChannel(2)
        assertEquals(25f, packet.duty)
        assertEquals(64, packet.pack()[7])
        controller.enableBlDe(true)
        controller.setOutput(DiagnosticOutput.BL, true)

        assertEquals(
            listOf(
                Snapshot(0, 0, 10f, 50f),
                Snapshot(0, 2, 10f, 25f),
                Snapshot((1 shl 12) or (1 shl 14), 2, 10f, 25f),
                Snapshot((1 shl 11) or (1 shl 12) or (1 shl 14), 2, 10f, 25f),
            ),
            sends,
        )
    }

    @Test
    fun enablingBothAdditionalOptionsSendsLowResetCommands() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }
        controller.initializeIgnitionOutputs(isSecu3T = false)
        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        assertEquals(listOf((1 shl 12) or (1 shl 14), (1 shl 12) or (1 shl 14) or (1 shl 24)), sends)
    }

    @Test
    fun unchangedOptionsAreNoOpsAndSingleGroupChangesPreserveOtherHighState() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }
        controller.initializeIgnitionOutputs(isSecu3T = false)
        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        controller.setOutput(DiagnosticOutput.BL, true)
        controller.setOutput(DiagnosticOutput.DE, true)
        controller.setOutput(DiagnosticOutput.TACH_O, true)

        val blDeHighAndTachHigh = packet.out
        val sendsAtHigh = sends.size
        controller.enableBlDe(true)
        controller.enableBlDe(true)
        controller.enableTachO(true)
        controller.enableTachO(true)
        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        assertEquals(blDeHighAndTachHigh, packet.out)
        assertEquals(sendsAtHigh, sends.size)

        val tachHighMask = (1 shl 23) or (1 shl 24)
        controller.enableBlDe(false)
        assertEquals(tachHighMask, packet.out)
        val afterBlDeDisable = sends.size
        controller.enableBlDe(false)
        assertEquals(afterBlDeDisable, sends.size)

        controller.enableBlDe(true)
        assertEquals(tachHighMask or (1 shl 12) or (1 shl 14), packet.out)
        controller.setOutput(DiagnosticOutput.BL, true)
        controller.setOutput(DiagnosticOutput.DE, true)
        val blDeHighMask = (1 shl 11) or (1 shl 12) or (1 shl 13) or (1 shl 14)

        controller.enableTachO(false)
        assertEquals(blDeHighMask, packet.out)
        val afterTachDisable = sends.size
        controller.enableTachO(false)
        assertEquals(afterTachDisable, sends.size)

        controller.enableTachO(true)
        assertEquals(blDeHighMask or (1 shl 24), packet.out)
        val afterTachEnable = sends.size
        controller.enableTachO(true)
        assertEquals(afterTachEnable, sends.size)
    }

    @Test
    fun inconsistentBlDeDirectionFlagsAreCorrectedAsOneOptionGroup() {
        val packet = DiagOutputPacket().apply {
            blOutputEnabled = true
            bl = true
            deOutputEnabled = false
        }
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }
        controller.initializeIgnitionOutputs(isSecu3T = false)

        controller.enableBlDe(true)

        assertEquals((1 shl 12) or (1 shl 14), packet.out)
        assertEquals(listOf((1 shl 12) or (1 shl 14)), sends)
        controller.enableBlDe(true)
        assertEquals(1, sends.size)
    }

    @Test
    fun specialOutputOptionsDriveLowResetStateAndGateStaleCommands() {
        val packet = DiagOutputPacket().apply { out = 1 shl 2 }
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }
        controller.initializeIgnitionOutputs(isSecu3T = false)

        controller.setOutput(DiagnosticOutput.BL, true)
        controller.setOutput(DiagnosticOutput.DE, true)
        controller.setOutput(DiagnosticOutput.TACH_O, true)
        assertEquals(emptyList<Int>(), sends)
        assertEquals(1 shl 2, packet.out)

        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14), sends[0])
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14) or (1 shl 24), sends[1])
        assertEquals(false, packet.bl)
        assertEquals(false, packet.de)
        assertEquals(false, packet.tachO)

        controller.setOutput(DiagnosticOutput.BL, true)
        controller.setOutput(DiagnosticOutput.BL, false)
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14) or (1 shl 24), sends[3])
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14) or (1 shl 24), packet.out)

        controller.setOutput(DiagnosticOutput.TACH_O, true)
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14) or (1 shl 23) or (1 shl 24), packet.out)
        controller.setOutput(DiagnosticOutput.BL, true)
        controller.setOutput(DiagnosticOutput.DE, true)
        controller.applyAdditionalOptions(enableBlDe = false, enableTachO = false)
        assertEquals(1 shl 2 or (1 shl 23) or (1 shl 24), sends[sends.lastIndex - 1])
        assertEquals(1 shl 2, sends.last())

        val sendCount = sends.size
        controller.setOutput(DiagnosticOutput.TACH_O, true)
        controller.setOutput(DiagnosticOutput.BL, false)
        assertEquals(sendCount, sends.size)
        assertEquals(1 shl 2, packet.out)

        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14), sends[sends.lastIndex - 1])
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14) or (1 shl 24), sends.last())
        assertEquals(false, packet.bl)
        assertEquals(false, packet.de)
        assertEquals(false, packet.tachO)

        val reenabledSendCount = sends.size
        controller.setOutput(DiagnosticOutput.IGN_OUT1, true)
        assertEquals(1 shl 2 or (1 shl 12) or (1 shl 14) or (1 shl 24) or 1, packet.out)
        assertEquals(reenabledSendCount + 1, sends.size)
    }

    @Test
    fun generatorChannelLocksManualOutputAndSwitchingOrNoneUnlocksIt() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Snapshot>()
        val controller = DiagnosticOutputController(packet) { sends += packet.snapshot() }
        controller.initializeIgnitionOutputs(isSecu3T = false)

        controller.selectChannel(1)
        controller.setOutput(DiagnosticOutput.IGN_OUT1, true)
        assertEquals(0, packet.out)
        assertEquals(listOf(Snapshot(0, 1, 10f, 50f)), sends)

        controller.selectChannel(2)
        controller.setOutput(DiagnosticOutput.IGN_OUT1, true)
        assertEquals(1, packet.out)
        controller.setOutput(DiagnosticOutput.IGN_OUT2, true)
        assertEquals(1, packet.out)
        assertEquals(
            listOf(Snapshot(0, 1, 10f, 50f), Snapshot(0, 2, 10f, 50f), Snapshot(1, 2, 10f, 50f)),
            sends,
        )

        controller.selectChannel(0)
        controller.setOutput(DiagnosticOutput.IGN_OUT2, true)
        assertEquals(3, packet.out)
        assertEquals(Snapshot(3, 0, 10f, 50f), sends.last())
    }

    @Test
    fun everyGeneratorChannelMapsByFamilyOrderIncludingFilteredTachChannel() {
        val secu3T = listOf(
            null, DiagnosticOutput.IGN_OUT1, DiagnosticOutput.IGN_OUT2, DiagnosticOutput.IGN_OUT3,
            DiagnosticOutput.IGN_OUT4, DiagnosticOutput.IE, DiagnosticOutput.FE, DiagnosticOutput.ECF,
            DiagnosticOutput.CE, DiagnosticOutput.ST_BLOCK, DiagnosticOutput.ADD_IO1, DiagnosticOutput.ADD_IO2,
            DiagnosticOutput.BL, DiagnosticOutput.DE,
        )
        val secu3I = listOf(
            null, DiagnosticOutput.IGN_OUT1, DiagnosticOutput.IGN_OUT2, DiagnosticOutput.IGN_OUT3,
            DiagnosticOutput.IGN_OUT4, DiagnosticOutput.IGN_OUT5, DiagnosticOutput.ECF0, DiagnosticOutput.INJ_O1,
            DiagnosticOutput.INJ_O2, DiagnosticOutput.INJ_O3, DiagnosticOutput.INJ_O4, DiagnosticOutput.INJ_O5,
            DiagnosticOutput.BL, DiagnosticOutput.DE, DiagnosticOutput.STBL_O, DiagnosticOutput.CEL_O,
            DiagnosticOutput.FPMP_O, DiagnosticOutput.PWRR_O, DiagnosticOutput.EVAP_O, DiagnosticOutput.O2SH_O,
            DiagnosticOutput.COND_O, DiagnosticOutput.ADD_O2, DiagnosticOutput.TACH_O, DiagnosticOutput.GPA6_O,
        )
        secu3T.forEachIndexed { channel, output -> assertEquals(output, diagnosticOutputForChannel(true, channel)) }
        secu3I.forEachIndexed { channel, output -> assertEquals(output, diagnosticOutputForChannel(false, channel)) }
        assertNull(diagnosticOutputForChannel(null, 1))
        assertNull(diagnosticOutputForChannel(false, secu3I.size))
    }

    @Test
    fun selectedSpecialGeneratorOutputsRemainProtectedByExistingPermissionGates() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }
        controller.initializeIgnitionOutputs(isSecu3T = false)
        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)

        controller.selectChannel(12) // BL
        val beforeBl = packet.out
        val sentBeforeBl = sends.size
        controller.setOutput(DiagnosticOutput.BL, true)
        assertEquals(beforeBl, packet.out)
        assertEquals(sentBeforeBl, sends.size)

        controller.selectChannel(13) // DE
        val beforeDe = packet.out
        val sentBeforeDe = sends.size
        controller.setOutput(DiagnosticOutput.DE, true)
        assertEquals(beforeDe, packet.out)
        assertEquals(sentBeforeDe, sends.size)

        controller.selectChannel(22) // TACH_O, even when its row is filtered out
        val beforeTach = packet.out
        val sentBeforeTach = sends.size
        controller.setOutput(DiagnosticOutput.TACH_O, true)
        assertEquals(beforeTach, packet.out)
        assertEquals(sentBeforeTach, sends.size)
    }

    private fun DiagOutputPacket.snapshot() = Snapshot(out, chan, frq, duty)
}
