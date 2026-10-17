package org.secu3.android.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.input.SensorsPacket

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
}
