package org.secu3.android.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.input.SensorsPacket
import org.threeten.bp.LocalTime
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

class SecuLoggerBinaryRecordTest {
    private val encoder = SensorLogEncoder()

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
        val bytes = encoder.encodeBinaryRecord(packet, mark = 4, now = LocalTime.of(1, 2, 3, 456_000_000))
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
    fun csvHeaderAndRowsKeepColumnsDelimiterCrlfAndUsDecimalFormatting() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val delimiter = "|"
            val header = encoder.csvTitle(delimiter)
            val packet = SensorsPacket(rpm = 1_234, currentAngle = 12.5f, map = 1.25f)
            val row = encoder.encodeCsvRow(packet, mark = 2, delimiter, LocalTime.MIDNIGHT)
            val headerColumns = header.trimEnd('\r', '\n').split(delimiter)
            val rowColumns = row.trimEnd('\r', '\n').split(delimiter)

            assertEquals(headerColumns.size, rowColumns.size)
            assertEquals(listOf("Time", "RPM", "IgnTim"), headerColumns.take(3))
            assertEquals(listOf("00:00:00.00", "1234", "12.50"), rowColumns.take(3).map(String::trim))
            assertEquals("ServFlag", headerColumns[headerColumns.lastIndex - 1])
            assertEquals("CECodes", headerColumns.last())
            assertTrue(header.endsWith("\r\n"))
            assertTrue(row.endsWith("\r\n"))
            assertTrue(row.contains("12.50"))
            assertFalse(row.contains("12,50"))
        } finally {
            Locale.setDefault(previousLocale)
        }
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
            val record = encoder.encodeBinaryRecord(packet, mark = 0, now = LocalTime.MIDNIGHT)
            val encodedServiceFlags = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN)
                .getShort(246).toInt() and 0xFFFF

            assertEquals(expectedServiceFlags, encodedServiceFlags)
            assertEquals(expectedCsvColumn, encoder.encodeCsvRow(packet, 0, ";", LocalTime.MIDNIGHT).split(';').let { it[it.lastIndex - 1] })
            assertEquals(originalServiceFlags, packet.serviceFlags)
        }

        assertFalse(cases.last().first.corrIdlve > 0)
    }
}
