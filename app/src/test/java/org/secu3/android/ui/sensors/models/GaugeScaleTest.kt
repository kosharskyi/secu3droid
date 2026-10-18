package org.secu3.android.ui.sensors.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GaugeScaleTest {
    @Test fun everyGaugeUsesItsConfiguredInclusiveTicksAndSharedAngles() {
        GaugeType.entries.forEach { type ->
            val scale = GaugeScale(type)
            assertEquals(type.tickCount, scale.ticks.size)
            assertSame(type.sections, scale.sections)

            scale.ticks.forEachIndexed { index, tick ->
                val fraction = if (type.tickCount <= 1) 0f else index.toFloat() / (type.tickCount - 1)
                assertEquals(index, tick.index)
                assertEquals(fraction, tick.fraction, EPSILON)
                assertEquals(type.minValue + (type.maxValue - type.minValue) * fraction, tick.value, EPSILON)
                assertEquals(GaugeScale.START_ANGLE_DEGREES + GaugeScale.SWEEP_ANGLE_DEGREES * fraction, tick.angleDegrees, EPSILON)
            }

            assertEquals(type.minValue, scale.ticks.first().value, EPSILON)
            assertEquals(type.maxValue, scale.ticks.last().value, EPSILON)
            assertEquals(GaugeScale.START_ANGLE_DEGREES, scale.ticks.first().angleDegrees, EPSILON)
            assertEquals(GaugeScale.START_ANGLE_DEGREES + GaugeScale.SWEEP_ANGLE_DEGREES, scale.ticks.last().angleDegrees, EPSILON)
            assertEquals(0f, scale.normalized(type.minValue)!!, EPSILON)
            assertEquals(1f, scale.normalized(type.maxValue)!!, EPSILON)
            assertEquals(0f, scale.normalized(Float.NEGATIVE_INFINITY)!!, EPSILON)
            assertEquals(1f, scale.normalized(Float.POSITIVE_INFINITY)!!, EPSILON)
            assertNull(scale.normalized(null))
            assertTrue(scale.sections.all { it.start in 0f..1f && it.end in 0f..1f && it.start < it.end })
        }
    }

    @Test fun sharedScaleExposesFractionalPositiveAndNegativeTicks() {
        val positiveScale = GaugeScale(GaugeType.ADD1)
        val positiveHalfStep = positiveScale.ticks[1]
        assertEquals(.5f, positiveHalfStep.value, EPSILON)

        val negativeRange = GaugeScale(GaugeType.CURRENT_ANGLE)
        assertEquals(-15f, negativeRange.ticks.first().value, EPSILON)
        assertEquals(65f, negativeRange.ticks.last().value, EPSILON)
        assertEquals(.25f, negativeRange.normalized(5f)!!, EPSILON)
        assertEquals(0f, negativeRange.normalized(-20f)!!, EPSILON)
        assertEquals(1f, negativeRange.normalized(70f)!!, EPSILON)

        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("0.5", positiveScale.format(positiveHalfStep))
            assertEquals("-7.5", GaugeScale.formatValue(-7.5f))
            Locale.setDefault(Locale.GERMANY)
            assertEquals("0,5", positiveScale.format(positiveHalfStep))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private companion object {
        const val EPSILON = .0001f
    }
}
