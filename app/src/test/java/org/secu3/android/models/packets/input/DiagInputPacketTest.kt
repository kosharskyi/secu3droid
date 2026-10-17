package org.secu3.android.models.packets.input

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagInputPacketTest {
    @Test fun temperatureVoltageUsesUnsignedRawWordAtTheExistingScale() {
        val expected = listOf(
            0x0000 to 0f,
            0x7FFF to (32767f / 400f),
            0x8000 to (32768f / 400f),
            0xFFFF to (65535f / 400f),
            0x0190 to 1f,
        )

        for ((raw, voltage) in expected) {
            val payload = IntArray(31)
            payload[1] = 0x01
            payload[2] = 0xF4 // ECU voltage remains 1.25 V.
            payload[5] = raw ushr 8
            payload[6] = raw and 0xFF
            payload[7] = 0x01
            payload[8] = 0x2C // Adjacent ADD_I1 remains 0.75 V.
            payload[29] = 0x02
            payload[30] = 0x25

            val packet = DiagInputPacket().parse(intArrayOf('@'.code, '='.code) + payload) as DiagInputPacket

            assertEquals(1.25f, packet.voltage, 0f)
            assertEquals(voltage, packet.temperature, 0f)
            assertEquals(0.75f, packet.addI1, 0f)
            assertEquals(0x0225, packet.bits)
        }
    }
}
