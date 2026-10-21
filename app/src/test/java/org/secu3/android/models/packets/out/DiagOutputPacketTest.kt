package org.secu3.android.models.packets.out

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagOutputPacketTest {
    private data class SpecialOutput(
        val directionBit: Int,
        val stateBit: Int,
        val getState: (DiagOutputPacket) -> Boolean,
        val setState: (DiagOutputPacket, Boolean) -> Unit,
        val getEnabled: (DiagOutputPacket) -> Boolean,
        val setEnabled: (DiagOutputPacket, Boolean) -> Unit,
    )

    private val outputs = listOf(
        SpecialOutput(12, 11, { it.bl }, { packet, value -> packet.bl = value }, { it.blOutputEnabled }, { packet, value -> packet.blOutputEnabled = value }),
        SpecialOutput(14, 13, { it.de }, { packet, value -> packet.de = value }, { it.deOutputEnabled }, { packet, value -> packet.deOutputEnabled = value }),
        SpecialOutput(24, 23, { it.tachO }, { packet, value -> packet.tachO = value }, { it.tachOOutputEnabled }, { packet, value -> packet.tachOOutputEnabled = value }),
    )

    @Test fun newDiagnosticOutputPacketUsesFiftyPercentDutyOnWire() {
        val packet = DiagOutputPacket()
        assertEquals(50f, packet.duty)
        assertEquals(128, packet.pack()[7])
    }

    @Test fun blDeAndTachDistinguishHighLowAndHighImpedanceWithoutChangingOtherBits() {
        for (output in outputs) {
            val unrelated = 1 shl 5
            val packet = DiagOutputPacket().apply { out = unrelated }
            assertEquals(false, output.getEnabled(packet))
            assertEquals(false, output.getState(packet))

            output.setState(packet, false)
            assertEquals(unrelated or (1 shl output.directionBit), packet.out) // driven LOW
            assertEquals(true, output.getEnabled(packet))
            assertEquals(false, output.getState(packet))

            output.setState(packet, true)
            val high = unrelated or (1 shl output.directionBit) or (1 shl output.stateBit)
            assertEquals(high, packet.out) // driven HIGH
            assertEquals(true, output.getState(packet))

            output.setState(packet, false)
            assertEquals(unrelated or (1 shl output.directionBit), packet.out)
            output.setEnabled(packet, false)
            assertEquals(unrelated, packet.out) // Hi-Z
            assertEquals(false, output.getEnabled(packet))
            assertEquals(false, output.getState(packet))
            assertArrayEquals(
                intArrayOf(0, 0, 0, unrelated),
                packet.pack().sliceArray(1..4),
            )
        }
    }

    @Test fun specialOutputPropertiesUseDesktopLowerStateAndUpperDirectionBits() {
        for (output in outputs) {
            val packet = DiagOutputPacket()
            packet.out = 1 shl output.stateBit
            assertEquals(true, output.getState(packet))
            assertEquals(false, output.getEnabled(packet))
            packet.out = 1 shl output.directionBit
            assertEquals(false, output.getState(packet))
            assertEquals(true, output.getEnabled(packet))
            packet.out = (1 shl output.stateBit) or (1 shl output.directionBit)
            assertEquals(true, output.getState(packet))
            assertEquals(true, output.getEnabled(packet))
        }
    }

    @Test fun specialOutputWordsMatchDesktopLowAndHighGoldenMasks() {
        val packet = DiagOutputPacket().apply {
            bl = false
            de = false
            tachO = false
        }
        assertEquals(0x01005000, packet.out)
        assertArrayEquals(intArrayOf(0x01, 0x00, 0x50, 0x00), packet.pack().sliceArray(1..4))

        packet.bl = true
        packet.de = true
        packet.tachO = true
        assertEquals(0x01807800, packet.out)
        assertArrayEquals(intArrayOf(0x01, 0x80, 0x78, 0x00), packet.pack().sliceArray(1..4))
    }
}
