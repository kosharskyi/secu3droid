package org.secu3.android.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.sensors.GaugeType

class DashboardViewDataTest {
    @Test
    fun inflatePreservesLoadAndMapAsIndependentUnclampedValues() {
        val sameSourceValue = DashboardViewData.inflate(SensorsPacket(load = 148f, map = 148f))
        val distinctValues = DashboardViewData.inflate(SensorsPacket(load = 148f, map = 236f))

        assertEquals(148f, sameSourceValue.load)
        assertEquals(148f, sameSourceValue.map)
        assertEquals(148f, distinctValues.load)
        assertEquals(236f, distinctValues.map)
    }

    @Test
    fun inflateMapsSelectedPrimaryGaugeValuesWithoutLosingTheirTypes() {
        val packet = SensorsPacket(rpm = 2200, speed = 84.6f, addI1 = 1.234f)
        val data = DashboardViewData.inflate(packet, GaugeType.ADD1, GaugeType.VEHICLE_SPEED)

        assertEquals(GaugeType.ADD1, data.primaryLeft.type)
        assertEquals(1.234f, data.primaryLeft.value)
        assertEquals(GaugeType.VEHICLE_SPEED, data.primaryRight.type)
        assertEquals(84.6f, data.primaryRight.value)
    }
}
