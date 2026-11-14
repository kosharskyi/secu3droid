package org.secu3.android.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InjectionTimingModeTest {
    @Test fun transformsDesktopPhaseBoundariesAndWrapsOnce() {
        val source = listOf(0f, 180f, 360f, 540f, 720f, 800f, 1200f)
        assertEquals(listOf(0f, 180f, -360f, -180f, 0f, 80f, 480f), source.map(InjectionTimingMode.Btdc360::transform))
        assertEquals(listOf(720f, 540f, 360f, 180f, 0f, 640f, 240f), source.map(InjectionTimingMode.Atdc720::transform))
        assertEquals(listOf(0f, -180f, 360f, 180f, 0f, -80f, -480f), source.map(InjectionTimingMode.Atdc360::transform))
    }

    @Test fun preservesNonFiniteValuesAndUsesStablePreferenceValues() {
        assertTrue(InjectionTimingMode.Btdc720.transform(Float.NaN).isNaN())
        assertEquals(Float.POSITIVE_INFINITY, InjectionTimingMode.Atdc360.transform(Float.POSITIVE_INFINITY), 0f)
        assertEquals("btdc_720", InjectionTimingMode.Default.preferenceValue)
        assertEquals(InjectionTimingMode.Atdc360, InjectionTimingMode.fromPreference("atdc_360"))
        assertEquals(InjectionTimingMode.Default, InjectionTimingMode.fromPreference("invalid"))
    }

    @Test fun normalizesZeroAtTopDeadCenterForSignedConventions() {
        assertEquals(0f, InjectionTimingMode.Btdc360.transform(720f), 0f)
        assertEquals(0f, InjectionTimingMode.Atdc360.transform(0f), 0f)
    }

    @Test fun encodesEditedAnglesUsingTheDesktopConventions() {
        val unsigned = listOf(0f, 180f, 720f)
        val centered = listOf(-360f, -180f, 0f, 180f, 360f)
        assertEquals(unsigned, unsigned.map(InjectionTimingMode.Btdc720::toEcuAngle))
        assertEquals(listOf(720f, 540f, 0f), unsigned.map(InjectionTimingMode.Atdc720::toEcuAngle))
        assertEquals(listOf(360f, 540f, 720f, 900f, 1080f), centered.map(InjectionTimingMode.Btdc360::toEcuAngle))
        assertEquals(listOf(1080f, 900f, 720f, 540f, 360f), centered.map(InjectionTimingMode.Atdc360::toEcuAngle))
    }
}
