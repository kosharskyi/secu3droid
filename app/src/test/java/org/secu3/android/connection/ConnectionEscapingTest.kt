package org.secu3.android.connection

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionEscapingTest {

    private val connection = TestConnection()

    @Test
    fun escTxPacket_escapesReservedPayloadBytes() {
        val packet = intArrayOf(0x21, 'x'.code, 0x21, 0x0D, 0x0A, 0x55)

        val escaped = connection.escapeTxForTest(packet)

        assertArrayEquals(
            intArrayOf(0x21, 'x'.code, 0x0A, 0x81, 0x0A, 0x83, 0x0A, 0x84, 0x55),
            escaped
        )
    }

    @Test
    fun escTxPacket_keepsHeaderAndDescriptorUnescaped() {
        val packet = intArrayOf(0x21, 0x0D, 0x55)

        val escaped = connection.escapeTxForTest(packet)

        assertArrayEquals(packet, escaped)
    }

    @Test
    fun escTxPacket_escapesTheFinalChecksumByteBeforeTerminatorIsAppended() {
        // One payload byte: Fletcher checksum includes the payload length.
        val cases = listOf(
            intArrayOf(0x21, 'x'.code, 12, 25, 13) to intArrayOf(0x21, 'x'.code, 12, 25, 10, 0x83),
            intArrayOf(0x21, 'x'.code, 9, 19, 10) to intArrayOf(0x21, 'x'.code, 9, 19, 10, 0x84),
            intArrayOf(0x21, 'x'.code, 32, 65, 33) to intArrayOf(0x21, 'x'.code, 32, 65, 10, 0x81),
        )
        for ((packet, expected) in cases) assertArrayEquals(expected, connection.escapeTxForTest(packet))
    }

    @Test
    fun escRxPacket_rejectsIncompleteAndWrongDirectionEscapes() {
        for (tail in listOf(intArrayOf(10), intArrayOf(10, 0x81), intArrayOf(10, 0x99), intArrayOf(10, 10))) {
            assertNull(connection.escapeRxForTest(intArrayOf(0x40, 'x'.code) + tail))
        }
    }

    @Test
    fun escRxPacket_unescapesIncomingReservedBytes() {
        val packet = intArrayOf(0x40, 'x'.code, 0x0A, 0x82, 0x0A, 0x83, 0x0A, 0x84)

        val unescaped = connection.escapeRxForTest(packet)

        assertArrayEquals(intArrayOf(0x40, 'x'.code, 0x40, 0x0D, 0x0A), unescaped)
    }

    private class TestConnection : Connection<Unit>() {
        fun escapeTxForTest(packet: IntArray): IntArray = escTxPacket(packet)

        fun escapeRxForTest(packet: IntArray): IntArray? = escRxPacket(packet)

        override fun disconnect() = Unit

        override fun isEndpointOpen(endpoint: Unit): Boolean = false

        override fun writeEndpoint(endpoint: Unit, bytes: ByteArray) = Unit

        override fun closeEndpoint(endpoint: Unit) = Unit
    }


}
