package org.secu3.android.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.secu3.android.models.packets.input.SensorsPacket
import org.threeten.bp.LocalTime
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SecuLoggerBinaryRecordTest {
    @Test
    fun binaryRecordMatchesDesktopOffsetsAndKeepsDownstreamFields() {
        val packet = SensorsPacket(
            airflow = 0x5A,
            sensorsFlags = (1 shl 15) or (1 shl 11) or (1 shl 4) or (1 shl 1),
            additionalFlags = 0x2468,
            tps = 12.5f,
            ecuErrors = 0x12345678,
            serviceFlags = 0x1357,
        )
        val bytes = encodeBinaryLogRecord(packet, mark = 4, now = LocalTime.of(1, 2, 3, 456_000_000))
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(252, bytes.size)
        assertEquals(0x5A.toByte(), bytes[32])
        assertEquals(0xCC.toByte(), bytes[33])
        assertEquals(0x8031, buffer.getShort(34).toInt() and 0xFFFF)
        assertEquals(0x1234, buffer.getShort(36).toInt() and 0xFFFF)
        assertEquals(0xCCCC.toShort(), buffer.getShort(38))
        assertEquals(12.5f, buffer.getFloat(40), 0f)
        assertEquals(4.toByte(), bytes[244])
        assertEquals(0x1157, buffer.getShort(246).toInt() and 0xFFFF)
        assertEquals(0x12345678, buffer.getInt(248))
    }

    @Test
    fun idlingVeMapsToServiceBitNineInBinaryAndCsvWithoutChangingOtherBits() {
        val otherServiceBits = (1 shl 8) or (1 shl 4) or 1
        val cases = listOf(
            Triple(SensorsPacket(additionalFlags = 1, serviceFlags = otherServiceBits), otherServiceBits or (1 shl 9), "   785"),
            Triple(SensorsPacket(additionalFlags = 0, serviceFlags = otherServiceBits or (1 shl 9)), otherServiceBits, "   273"),
            Triple(SensorsPacket(additionalFlags = 0xFE, serviceFlags = 0), 0, "     0"),
        )

        cases.forEach { (packet, expectedServiceFlags, expectedCsvColumn) ->
            val originalServiceFlags = packet.serviceFlags
            val record = encodeBinaryLogRecord(packet, mark = 0, now = LocalTime.MIDNIGHT)
            val encodedServiceFlags = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN)
                .getShort(246).toInt() and 0xFFFF

            assertEquals(expectedServiceFlags, packet.serviceFlagsForLog())
            assertEquals(expectedServiceFlags, encodedServiceFlags)
            assertEquals(expectedCsvColumn, packet.csvServiceFlagsColumn())
            assertEquals(originalServiceFlags, packet.serviceFlags)
        }

        assertFalse(cases.last().first.corrIdlve > 0)
    }
}
