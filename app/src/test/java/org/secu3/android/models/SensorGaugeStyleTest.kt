package org.secu3.android.models

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorGaugeStyleTest {
    @Test
    fun missingAndUnknownPreferencesUsePreciseArcWhileSavedChoicesArePreserved() {
        assertEquals(SensorGaugeStyle.PreciseArc, SensorGaugeStyle.fromPreference(null))
        assertEquals(SensorGaugeStyle.PreciseArc, SensorGaugeStyle.fromPreference("unknown"))
        assertEquals(SensorGaugeStyle.Current, SensorGaugeStyle.fromPreference("current"))
        assertEquals(SensorGaugeStyle.ThinNeedle, SensorGaugeStyle.fromPreference("thin_needle"))
    }
}
