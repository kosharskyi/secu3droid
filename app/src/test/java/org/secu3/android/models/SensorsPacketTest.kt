package org.secu3.android.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun signedFlowPressureAirflowAndKnockWordsPreservePhysicalValuesAndSentinel() {
        fun parseWithWords(knock: Int, fuelLevel: Int, oilPressure: Int, maf: Int): SensorsPacket {
            val payload = IntArray(116)
            payload[12] = knock ushr 8; payload[13] = knock and 255
            payload[75] = fuelLevel ushr 8; payload[76] = fuelLevel and 255
            payload[79] = oilPressure ushr 8; payload[80] = oilPressure and 255
            payload[82] = maf ushr 8; payload[83] = maf and 255
            return RawPacket(intArrayOf('@'.code, 'q'.code) + payload + intArrayOf(0, 0))
                .parse(null) as SensorsPacket
        }

        val negative = parseWithWords(knock = 0xFFE0, fuelLevel = 0xFFC0, oilPressure = 0xFF00, maf = 0xFFC0)
        assertTrue(negative.knkret_use)
        assertEquals(-1f, negative.knockRetard, .0001f)
        assertEquals(-1f, negative.ftls, .0001f)
        assertEquals(-1f, negative.ops, .0001f)
        assertEquals(-1f, negative.maf, .0001f)

        val negativeOne = parseWithWords(knock = 0xFFFF, fuelLevel = 0xFFFF, oilPressure = 0xFFFF, maf = 0xFFFF)
        assertEquals(-1f / 32f, negativeOne.knockRetard, .0001f)
        assertEquals(-1f / 64f, negativeOne.ftls, .0001f)
        assertEquals(-1f / 256f, negativeOne.ops, .0001f)
        assertEquals(-1f / 64f, negativeOne.maf, .0001f)

        val positive = parseWithWords(knock = 0x0020, fuelLevel = 0x0040, oilPressure = 0x0100, maf = 0x0040)
        assertTrue(positive.knkret_use)
        assertEquals(1f, positive.knockRetard, .0001f)
        assertEquals(1f, positive.ftls, .0001f)
        assertEquals(1f, positive.ops, .0001f)
        assertEquals(1f, positive.maf, .0001f)

        val disabled = parseWithWords(knock = 0x7FFF, fuelLevel = 0, oilPressure = 0, maf = 0)
        assertFalse(disabled.knkret_use)
        assertEquals(0f, disabled.knockRetard, .0001f)
        assertEquals(0f, disabled.ftls, .0001f)
        assertEquals(0f, disabled.ops, .0001f)
        assertEquals(0f, disabled.maf, .0001f)
    }
}
