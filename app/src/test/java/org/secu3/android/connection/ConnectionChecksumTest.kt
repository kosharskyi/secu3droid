package org.secu3.android.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalUnsignedTypes::class)
class ConnectionChecksumTest {

    private val connection = TestConnection()

    @Test
    fun isChecksumValid_rejectsShortFramesAndMissingHeaderWithoutThrowing() {
        for (length in 0..3) assertFalse(connection.isChecksumValidForTest(IntArray(length)))
        assertFalse(connection.isChecksumValidForTest(intArrayOf(0, 'x'.code, 0, 0)))
    }

    @Test
    fun checksumMatchesIndependentFirmwareGoldenBytes() {
        assertTrue(connection.isChecksumValidForTest(intArrayOf(0x40, 'v'.code, 1, 2, 3, 4, 0x22, 0x0E)))
        assertFalse(connection.isChecksumValidForTest(intArrayOf(0x40, 'v'.code, 1, 2, 3, 4, 0x0E, 0x22)))
    }

    @Test
    fun isChecksumValid_acceptsPacketWithMatchingChecksum() {
        val packet = packetWithChecksum(0x40, 'x'.code, 0x01, 0x02)

        assertTrue(connection.isChecksumValidForTest(packet))
    }

    @Test
    fun isChecksumValid_rejectsPacketWhenFirstChecksumByteDiffers() {
        val packet = packetWithChecksum(0x40, 'x'.code, 0x01, 0x02)
        packet[packet.lastIndex] = packet[packet.lastIndex] xor 0x01

        assertFalse(connection.isChecksumValidForTest(packet))
    }

    @Test
    fun isChecksumValid_rejectsPacketWhenSecondChecksumByteDiffers() {
        val packet = packetWithChecksum(0x40, 'x'.code, 0x01, 0x02)
        packet[packet.lastIndex - 1] = packet[packet.lastIndex - 1] xor 0x01

        assertFalse(connection.isChecksumValidForTest(packet))
    }

    private fun packetWithChecksum(vararg data: Int): IntArray {
        val checksum = connection.calculateChecksumForTest(data.sliceArray(2 until data.size))
        return data + checksum[1].toInt() + checksum[0].toInt()
    }

    private class TestConnection : Connection<Unit>() {
        fun isChecksumValidForTest(data: IntArray): Boolean = isChecksumValid(data)

        fun calculateChecksumForTest(packet: IntArray): UByteArray = calculateChecksum(packet)

        override fun disconnect() = Unit

        override fun isEndpointOpen(endpoint: Unit): Boolean = false

        override fun writeEndpoint(endpoint: Unit, bytes: ByteArray) = Unit

        override fun closeEndpoint(endpoint: Unit) = Unit
    }


}
