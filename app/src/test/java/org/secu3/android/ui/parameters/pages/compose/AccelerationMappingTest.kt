/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.out.params.AccelerationParamPacket

class AccelerationMappingTest {
    @Test fun packetFieldsMapAndEditsPreserveScalingSignsAndUnknownTail() {
        val packet = AccelerationParamPacket(
            injAeTpsdotThrd = 42, injAeColdaccMult = 50, injAeDecayTime = 7,
            injAeType = 9, injAeTime = 12, injAeBallance = 99.609375f,
            injAeMapdotThrd = 13, injXtauSThrd = -25f, injXtauFThrd = -8f,
            wallwetModel = 6,
        )
        val values = AccelerationValues.from(packet)
        assertEquals(42f, values.value(AccelerationNumber.TPS_THRESHOLD))
        assertEquals(50f, values.value(AccelerationNumber.COLD_MULTIPLIER))
        assertEquals(-25f, values.value(AccelerationNumber.XTAU_START))
        assertEquals(9, values.aeType)
        assertEquals(6, values.wallwetModel)

        applyAccelerationValue(packet, AccelerationNumber.XTAU_START, -31f)
        applyAccelerationValue(packet, AccelerationNumber.COLD_MULTIPLIER, 50f)
        packet.injAeBallance = 50f
        packet.injAeType = 1
        val packed = packet.pack()
        assertEquals(50f, AccelerationValues.from(packet).value(AccelerationNumber.COLD_MULTIPLIER))
        assertEquals(-31f, packet.injXtauSThrd)
        assertEquals(1, packet.injAeType)
        assertEquals(50f, packet.injAeBallance)
        assertEquals(0xFF, packed[2])
        assertEquals(0xC0, packed[3])
        assertEquals(128, packed[7])
        assertEquals(31, packed[9])
        val withTail = intArrayOf(64, 33) + packed.drop(1) + intArrayOf(0xAB, 0xCD)
        val parsed = AccelerationParamPacket().parse(withTail) as AccelerationParamPacket
        assertEquals(0xAB, parsed.pack()[parsed.pack().size - 2])
        assertEquals(0xCD, parsed.pack().last())
        assertEquals(50, parsed.injAeColdaccMult)
        assertEquals(-31f, parsed.injXtauSThrd)
        assertEquals(50f, parsed.injAeBallance, 0.1f)
    }
}
