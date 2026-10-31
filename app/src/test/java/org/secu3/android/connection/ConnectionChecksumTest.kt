package org.secu3.android.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionChecksumTest {

    @Test
    fun isChecksumValid_rejectsShortFramesAndMissingHeaderWithoutThrowing() {
        for (length in 0..3) assertFalse(PacketReceiver.isChecksumValid(IntArray(length)))
        assertFalse(PacketReceiver.isChecksumValid(intArrayOf(0, 'x'.code, 0, 0)))
    }

    @Test
    fun checksumMatchesIndependentFirmwareGoldenBytes() {
        assertTrue(PacketReceiver.isChecksumValid(intArrayOf(0x40, 'v'.code, 1, 2, 3, 4, 0x22, 0x0E)))
        assertFalse(PacketReceiver.isChecksumValid(intArrayOf(0x40, 'v'.code, 1, 2, 3, 4, 0x0E, 0x22)))
    }

    @Test
    fun isChecksumValid_acceptsPacketWithMatchingChecksum() {
        val packet = packetWithChecksum(0x40, 'x'.code, 0x01, 0x02)

        assertTrue(PacketReceiver.isChecksumValid(packet))
    }

    @Test
    fun isChecksumValid_rejectsPacketWhenFirstChecksumByteDiffers() {
        val packet = packetWithChecksum(0x40, 'x'.code, 0x01, 0x02)
        packet[packet.lastIndex] = packet[packet.lastIndex] xor 0x01

        assertFalse(PacketReceiver.isChecksumValid(packet))
    }

    @Test
    fun isChecksumValid_rejectsPacketWhenSecondChecksumByteDiffers() {
        val packet = packetWithChecksum(0x40, 'x'.code, 0x01, 0x02)
        packet[packet.lastIndex - 1] = packet[packet.lastIndex - 1] xor 0x01

        assertFalse(PacketReceiver.isChecksumValid(packet))
    }

    private fun packetWithChecksum(vararg data: Int): IntArray {
        val checksum = PacketReceiver.calculateChecksum(data.sliceArray(2 until data.size))
        return data + checksum[1] + checksum[0]
    }

}
