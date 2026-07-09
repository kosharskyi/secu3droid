package org.secu3.android.connection

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.utils.UserPrefs
import sun.misc.Unsafe

class ConnectionEscapingTest {

    private val connection = TestConnection()

    @Test
    fun escTxPacket_escapesReservedPayloadBytes() {
        val packet = intArrayOf(0x21, 'x'.code, 0x21, 0x0D, 0x0A, 0x55, 0x0D)

        val escaped = connection.escapeTxForTest(packet)

        assertArrayEquals(
            intArrayOf(0x21, 'x'.code, 0x0A, 0x81, 0x0A, 0x83, 0x0A, 0x84, 0x55, 0x0D),
            escaped
        )
    }

    @Test
    fun escTxPacket_keepsHeaderDescriptorAndEndMarkerUnescaped() {
        val packet = intArrayOf(0x21, 0x0D, 0x55, 0x0D)

        val escaped = connection.escapeTxForTest(packet)

        assertArrayEquals(packet, escaped)
    }

    @Test
    fun escRxPacket_unescapesIncomingReservedBytes() {
        val packet = intArrayOf(0x40, 'x'.code, 0x0A, 0x82, 0x0A, 0x83, 0x0A, 0x84)

        val unescaped = connection.escapeRxForTest(packet)

        assertArrayEquals(intArrayOf(0x40, 'x'.code, 0x40, 0x0D, 0x0A), unescaped)
    }

    private class TestConnection : Connection(newUserPrefsWithoutAndroidContext()) {
        override val isConnected: Boolean = false

        fun escapeTxForTest(packet: IntArray): IntArray = escTxPacket(packet)

        fun escapeRxForTest(packet: IntArray): IntArray = escRxPacket(packet)

        override fun sendData(sendPacket: OutputPacket) = Unit

        override fun disconnect() = Unit
    }

    companion object {
        private fun newUserPrefsWithoutAndroidContext(): UserPrefs {
            val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe").apply {
                isAccessible = true
            }
            val unsafe = unsafeField.get(null) as Unsafe
            return unsafe.allocateInstance(UserPrefs::class.java) as UserPrefs
        }
    }
}
