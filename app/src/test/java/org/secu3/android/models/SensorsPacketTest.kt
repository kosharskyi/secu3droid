package org.secu3.android.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.input.SensorsPacket

class SensorsPacketTest {
    @Test fun fuelAndOilTemperaturesUseIndependentSignedQuarterDegreeWords() {
        val goldenTemperatures = listOf(
            0xFFFC to -1f,
            0xFFD8 to -10f,
            0xFFFF to -.25f,
            0x0000 to 0f,
            0x0190 to 100f,
        )
        goldenTemperatures.forEachIndexed { index, (fuelRaw, fuelTemp) ->
            val (oilRaw, oilTemp) = goldenTemperatures[(index + 1) % goldenTemperatures.size]
            val payload = IntArray(116)
            payload[88] = fuelRaw ushr 8; payload[89] = fuelRaw and 255
            payload[90] = 0x01; payload[91] = 0x02; payload[92] = 0x03 // consumed-fuel alignment sentinel
            payload[110] = oilRaw ushr 8; payload[111] = oilRaw and 255
            payload[112] = 0x00; payload[113] = 0x64 // est torque sentinel: 50 Nm
            payload[114] = 0x00; payload[115] = 0xC8 // requested torque sentinel: 100 Nm
            val packet = RawPacket(intArrayOf('@'.code, 'q'.code) + payload + intArrayOf(0, 0))
                .parse(null) as SensorsPacket

            assertEquals(fuelTemp, packet.fts, .0001f)
            assertEquals(oilTemp, packet.ots, .0001f)
            assertEquals(50f, packet.estTorque, .0001f)
            assertEquals(100f, packet.reqTorque, .0001f)
        }
    }

    @Test fun exhaustTemperatureUsesSignedWordWithoutClampingAndKeepsNeighborChannelsAligned() {
        val goldenTemperatures = listOf(
            0xFFFC to -1f,
            0xFFD8 to -10f,
            0xFFFF to -.25f,
            0x0000 to 0f,
            0x0FA0 to 1000f,
            0x8000 to -8192f,
        )
        goldenTemperatures.forEach { (raw, temperature) ->
            val payload = IntArray(116)
            payload[75] = 0x00; payload[76] = 0x40 // fuel tank level = 1
            payload[77] = raw ushr 8; payload[78] = raw and 255
            payload[79] = 0x01; payload[80] = 0x00 // oil pressure = 1
            val packet = RawPacket(intArrayOf('@'.code, 'q'.code) + payload + intArrayOf(0, 0))
                .parse(null) as SensorsPacket

            assertEquals(temperature, packet.egts, .0001f)
            assertEquals(1f, packet.ftls, .0001f)
            assertEquals(1f, packet.ops, .0001f)
        }
    }
}
