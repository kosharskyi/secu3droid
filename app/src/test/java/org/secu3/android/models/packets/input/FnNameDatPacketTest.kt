package org.secu3.android.models.packets.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FnNameDatPacketTest {
    private fun frame(count: Int, index: Int, name: IntArray) =
        intArrayOf('@'.code, FnNameDatPacket.DESCRIPTOR.code, count, index) + name

    @Test fun decodesCyrillicFunctionNamesUsingIbm866AndKeepsFixedWidthSpaces() {
        val name = intArrayOf(0x80, 0x81, 0x82, 0x83) + IntArray(12) { ' '.code }
        val packet = FnNameDatPacket().parse(frame(3, 2, name)) as FnNameDatPacket

        assertEquals(3, packet.tablesNumber)
        assertEquals(2, packet.fnName.index)
        assertEquals("АБВГ            ", packet.fnName.name)
    }

    @Test fun keepsAllSixteenAsciiBytesAndMapsEveryFfByteToSpace() {
        val ascii = "0123456789ABCDEF".map(Char::code).toIntArray()
        val asciiPacket = FnNameDatPacket().parse(frame(1, 0, ascii)) as FnNameDatPacket
        assertEquals("0123456789ABCDEF", asciiPacket.fnName.name)

        val padded = intArrayOf('A'.code, 0xFF, 'B'.code, 0xFF, 0) + IntArray(11)
        val paddedPacket = FnNameDatPacket().parse(frame(1, 0, padded)) as FnNameDatPacket
        assertEquals("A B ", paddedPacket.fnName.name)
    }

    @Test fun nulStopsDisplayedNameButTheEntireSixteenByteFieldIsRequired() {
        val nulName = intArrayOf('A'.code, 'B'.code, 0, 'C'.code, 'D'.code) + IntArray(11) { ' '.code }
        val packet = FnNameDatPacket().parse(frame(2, 1, nulName)) as FnNameDatPacket
        assertEquals("AB", packet.fnName.name)
        assertEquals(2, packet.tablesNumber)
        assertEquals(1, packet.fnName.index)

        assertThrows(IllegalArgumentException::class.java) {
            FnNameDatPacket().parse(frame(2, 1, nulName.copyOf(15)))
        }
    }
}
