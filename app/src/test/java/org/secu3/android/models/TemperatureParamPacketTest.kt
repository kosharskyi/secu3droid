package org.secu3.android.models

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.out.params.TemperatureParamPacket

class TemperatureParamPacketTest {
    @Test fun fanTimerRetainsUnsignedCentisecondsAcrossUnrelatedEditsAndTail() {
        for ((raw, seconds) in listOf(
            127 to 1.27f,
            99 to .99f,
            0 to 0f,
            100 to 1f,
            32768 to 327.68f,
            65535 to 655.35f,
        )) {
            val payload = IntArray(15)
            payload[0] = 0x80
            payload[5] = 0x14; payload[6] = 0x7B // PWM frequency raw for approximately 100 Hz
            payload[11] = 0x03; payload[12] = 0xE8 // minimum RPM = 1000
            payload[13] = raw ushr 8; payload[14] = raw and 255
            val frame = intArrayOf('@'.code, 'j'.code) + payload + intArrayOf(0xCA, 0xFE)
            val packet = TemperatureParamPacket().parse(frame) as TemperatureParamPacket

            assertEquals(seconds, packet.ventTmr, .0001f)
            packet.coolantUse = true
            packet.condMinRpm = 1200
            val packed = packet.pack()
            assertArrayEquals(intArrayOf(raw ushr 8, raw and 255), packed.sliceArray(14..15))
            assertArrayEquals(intArrayOf(0xCA, 0xFE), packed.takeLast(2).toIntArray())
        }
    }
}
