package org.secu3.android.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.utils.UserPrefs
import sun.misc.Unsafe

@OptIn(ExperimentalUnsignedTypes::class)
class ConnectionChecksumTest {

    private val connection = TestConnection()

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

    private class TestConnection : Connection(newUserPrefsWithoutAndroidContext()) {
        override val isConnected: Boolean = false

        fun isChecksumValidForTest(data: IntArray): Boolean = isChecksumValid(data)

        fun calculateChecksumForTest(packet: IntArray): UByteArray = calculateChecksum(packet)

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
