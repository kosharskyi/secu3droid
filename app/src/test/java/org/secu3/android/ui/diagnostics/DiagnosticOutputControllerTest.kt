package org.secu3.android.ui.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.out.DiagOutputPacket

class DiagnosticOutputControllerTest {
    private data class Snapshot(val out: Int, val channel: Int, val frequency: Float, val duty: Float)

    @Test
    fun channelAlwaysSendsButFrequencyAndDutySendOnlyForSelectedChannel() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Snapshot>()
        val controller = DiagnosticOutputController(packet) { sends += packet.snapshot() }

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
    fun additionalOptionsDisableBlDeThenTachoWithSeparatePacketSnapshots() {
        val packet = DiagOutputPacket().apply { bl = true; de = true; tachO = true }
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }

        controller.applyAdditionalOptions(enableBlDe = false, enableTachO = false)

        assertEquals(listOf(1 shl 23 or (1 shl 24), 0), sends)
        assertEquals(0, packet.out)
    }

    @Test
    fun freshChannelKeepsProtocolDutyZeroAndSpecialOutputRequiresDirectionEnable() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Snapshot>()
        val controller = DiagnosticOutputController(packet) { sends += packet.snapshot() }

        controller.selectChannel(2)
        assertEquals(0f, packet.duty)
        controller.enableBlDe(true)
        controller.setOutput(DiagnosticOutput.BL, true)

        assertEquals(
            listOf(
                Snapshot(0, 2, 10f, 0f),
                Snapshot((1 shl 12) or (1 shl 14), 2, 10f, 0f),
                Snapshot((1 shl 11) or (1 shl 12) or (1 shl 14), 2, 10f, 0f),
            ),
            sends,
        )
    }

    @Test
    fun enablingBothAdditionalOptionsSendsLowResetCommands() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }
        controller.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        assertEquals(listOf((1 shl 12) or (1 shl 14), (1 shl 12) or (1 shl 14) or (1 shl 24)), sends)
    }

    @Test
    fun specialOutputOptionsDriveLowResetStateAndGateStaleCommands() {
        val packet = DiagOutputPacket().apply { out = 1 shl 2 }
        val sends = mutableListOf<Int>()
        val controller = DiagnosticOutputController(packet) { sends += packet.out }

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

    private fun DiagOutputPacket.snapshot() = Snapshot(out, chan, frq, duty)
}
