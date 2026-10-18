package org.secu3.android.ui.sensors.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import org.secu3.android.R
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.packets.input.SensorsPacket

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
        val positiveQuarterStep = positiveScale.ticks[1]
        assertEquals(.25f, positiveQuarterStep.value, EPSILON)
        assertEquals(.5f, positiveScale.ticks[2].value, EPSILON)

        val negativeRange = GaugeScale(GaugeType.CURRENT_ANGLE)
        assertEquals(-15f, negativeRange.ticks.first().value, EPSILON)
        assertEquals(65f, negativeRange.ticks.last().value, EPSILON)
        assertEquals(.25f, negativeRange.normalized(5f)!!, EPSILON)
        assertEquals(0f, negativeRange.normalized(-20f)!!, EPSILON)
        assertEquals(1f, negativeRange.normalized(70f)!!, EPSILON)

        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("0.25", positiveScale.format(positiveScale.ticks[1]))
            val coolantScale = GaugeScale(GaugeType.EGO_CORR)
            assertEquals("6.25", coolantScale.format(coolantScale.ticks[9]))
            val oilTemperatureScale = GaugeScale(GaugeType.OTS)
            assertEquals("-38.125", oilTemperatureScale.format(oilTemperatureScale.ticks[1]))
            assertEquals("-7.5", GaugeScale.formatValue(-7.5f))
            Locale.setDefault(Locale.GERMANY)
            assertEquals("0,25", positiveScale.format(positiveScale.ticks[1]))
            assertEquals("-38,125", oilTemperatureScale.format(oilTemperatureScale.ticks[1]))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test fun primaryGaugeRangesTicksAndZonesMatchFrozenDesktopDefaults() {
        val expected = listOf(
        GaugeExpectation(GaugeType.RPM, 0f, 8000f, 17, listOf(Zone(0f, 4000f, R.color.gauge_desktop_64ff64), Zone(4000f, 6000f, R.color.gauge_desktop_ffff64), Zone(6000f, 8000f, R.color.gauge_desktop_ff6464))),
        GaugeExpectation(GaugeType.MAP, 10f, 110f, 21, listOf(Zone(10f, 30f, R.color.gauge_desktop_b48282), Zone(30f, 90f, R.color.gauge_desktop_78c878), Zone(90f, 110f, R.color.gauge_desktop_b48282))),
        GaugeExpectation(GaugeType.VOLTAGE, 2f, 18f, 17, listOf(Zone(2f, 10f, R.color.gauge_desktop_ff8080), Zone(10f, 15f, R.color.gauge_desktop_80ff80), Zone(15f, 18f, R.color.gauge_desktop_ff0000))),
        GaugeExpectation(GaugeType.CURRENT_ANGLE, -15f, 65f, 17, listOf(Zone(-15f, 0f, R.color.gauge_desktop_b48282), Zone(0f, 45f, R.color.gauge_desktop_dcdc78), Zone(45f, 65f, R.color.gauge_desktop_ff6464))),
        GaugeExpectation(GaugeType.TEMPERATURE, -40f, 120f, 17, listOf(Zone(-40f, 50f, R.color.gauge_desktop_8282b4), Zone(50f, 100f, R.color.gauge_desktop_787878), Zone(100f, 120f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.ADD1, 0f, 5f, 21, listOf(Zone(0f, 5f, R.color.gauge_desktop_b4b4e6))),
        GaugeExpectation(GaugeType.ADD2, 0f, 5f, 21, listOf(Zone(0f, 5f, R.color.gauge_desktop_b4b4e6))),
        GaugeExpectation(GaugeType.INJ_PW, 0f, 24f, 25, listOf(Zone(0f, 1f, R.color.gauge_desktop_ff6464), Zone(1f, 10f, R.color.gauge_desktop_64ff64), Zone(10f, 24f, R.color.gauge_desktop_9696fa))),
        GaugeExpectation(GaugeType.IAT, -40f, 120f, 17, listOf(Zone(-40f, 50f, R.color.gauge_desktop_8282b4), Zone(50f, 100f, R.color.gauge_desktop_787878), Zone(100f, 120f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.EGO_CORR, -50f, 50f, 17, listOf(Zone(-50f, -20f, R.color.gauge_desktop_e68282), Zone(-20f, 20f, R.color.gauge_desktop_78be78), Zone(20f, 50f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.CHOKE_POSITION, 0f, 100f, 21, listOf(Zone(0f, 100f, R.color.gauge_desktop_00e6fa))),
        GaugeExpectation(GaugeType.AIR_FLOW, 0f, 16f, 17, listOf(Zone(0f, 1f, R.color.gauge_desktop_be9696), Zone(1f, 2f, R.color.gauge_desktop_b496a0), Zone(2f, 3f, R.color.gauge_desktop_aa96aa), Zone(3f, 4f, R.color.gauge_desktop_a096b4), Zone(4f, 5f, R.color.gauge_desktop_9696be), Zone(5f, 6f, R.color.gauge_desktop_9696c8), Zone(6f, 7f, R.color.gauge_desktop_9696d2), Zone(7f, 8f, R.color.gauge_desktop_9696dc), Zone(8f, 9f, R.color.gauge_desktop_9696e6), Zone(9f, 10f, R.color.gauge_desktop_9696f0), Zone(10f, 11f, R.color.gauge_desktop_9696fa), Zone(11f, 12f, R.color.gauge_desktop_9696ff), Zone(12f, 13f, R.color.gauge_desktop_9696ff), Zone(13f, 14f, R.color.gauge_desktop_9696ff), Zone(14f, 15f, R.color.gauge_desktop_9696ff), Zone(15f, 16f, R.color.gauge_desktop_9696ff))),
        GaugeExpectation(GaugeType.VEHICLE_SPEED, 0f, 220f, 23, listOf(Zone(0f, 60f, R.color.gauge_desktop_64ff64), Zone(60f, 120f, R.color.gauge_desktop_fae664), Zone(120f, 180f, R.color.gauge_desktop_e68264), Zone(180f, 220f, R.color.gauge_desktop_ff7878))),
        GaugeExpectation(GaugeType.TPS_DOT, -500f, 500f, 11, listOf(Zone(-500f, -200f, R.color.gauge_desktop_fa6464), Zone(-200f, -50f, R.color.gauge_desktop_64fa64), Zone(-50f, 0f, R.color.gauge_desktop_6464fa), Zone(0f, 50f, R.color.gauge_desktop_6464fa), Zone(50f, 200f, R.color.gauge_desktop_64fa64), Zone(200f, 500f, R.color.gauge_desktop_fa6464))),
        GaugeExpectation(GaugeType.MAP2, 0f, 400f, 21, listOf(Zone(0f, 100f, R.color.gauge_desktop_b48282), Zone(100f, 400f, R.color.gauge_desktop_78c878))),
        GaugeExpectation(GaugeType.DIFF_PRESSURE, 0f, 400f, 21, listOf(Zone(0f, 100f, R.color.gauge_desktop_b48282), Zone(100f, 400f, R.color.gauge_desktop_78c878))),
        GaugeExpectation(GaugeType.IAT2, -40f, 120f, 17, listOf(Zone(-40f, 50f, R.color.gauge_desktop_8282b4), Zone(50f, 100f, R.color.gauge_desktop_787878), Zone(100f, 120f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.FUEL_CONSUMPTION, 0f, 50f, 25, listOf(Zone(0f, 10f, R.color.gauge_desktop_64dc64), Zone(10f, 20f, R.color.gauge_desktop_b4b464), Zone(20f, 50f, R.color.gauge_desktop_dc6464))),
        GaugeExpectation(GaugeType.KNOCK_RETARD, 0f, 30f, 15, listOf(Zone(0f, 10f, R.color.gauge_desktop_dcdc78), Zone(10f, 30f, R.color.gauge_desktop_dc7878))),
        GaugeExpectation(GaugeType.KNOCK_SIGNAL, 0f, 5f, 21, listOf(Zone(0f, 0.5f, R.color.gauge_desktop_2850dc), Zone(0.5f, 1f, R.color.gauge_desktop_3c64b4), Zone(1f, 1.5f, R.color.gauge_desktop_5078a0), Zone(1.5f, 2f, R.color.gauge_desktop_648c8c), Zone(2f, 2.5f, R.color.gauge_desktop_78a078), Zone(2.5f, 3f, R.color.gauge_desktop_8cb464), Zone(3f, 3.5f, R.color.gauge_desktop_a0a064), Zone(3.5f, 4f, R.color.gauge_desktop_b48c64), Zone(4f, 4.5f, R.color.gauge_desktop_c87864), Zone(4.5f, 5f, R.color.gauge_desktop_e66464))),
        GaugeExpectation(GaugeType.WBO_AFR, 6f, 24f, 19, listOf(Zone(6f, 10f, R.color.gauge_desktop_ff8282), Zone(10f, 14.55f, R.color.gauge_desktop_f0f082), Zone(14.55f, 14.85f, R.color.gauge_desktop_82ff82), Zone(14.85f, 15.45f, R.color.gauge_desktop_f0f078), Zone(15.45f, 15.75f, R.color.gauge_desktop_82ff82), Zone(15.75f, 20f, R.color.gauge_desktop_f0f082), Zone(20f, 24f, R.color.gauge_desktop_ff8282))),
        GaugeExpectation(GaugeType.IAC_VALVE, 0f, 100f, 17, listOf(Zone(0f, 5f, R.color.gauge_desktop_8282b4), Zone(5f, 75f, R.color.gauge_desktop_78b496), Zone(75f, 100f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.GAS_DISPENSER, 0f, 100f, 21, listOf(Zone(0f, 100f, R.color.gauge_desktop_00e6fa))),
        GaugeExpectation(GaugeType.SYNTHETIC_LOAD, 0f, 250f, 21, listOf(Zone(0f, 100f, R.color.gauge_desktop_82fa82), Zone(100f, 250f, R.color.gauge_desktop_fa8282))),
        GaugeExpectation(GaugeType.BEGIN_INJ_PHASE, 0f, 720f, 17, listOf(Zone(0f, 720f, R.color.gauge_desktop_fafafa))),
        GaugeExpectation(GaugeType.END_INJ_PHASE, 0f, 720f, 17, listOf(Zone(0f, 720f, R.color.gauge_desktop_fafafa))),
        GaugeExpectation(GaugeType.FUEL_CONSUMPTION_HZ, 0f, 250f, 21, listOf(Zone(0f, 10f, R.color.gauge_desktop_828296), Zone(10f, 70f, R.color.gauge_desktop_64f064), Zone(70f, 150f, R.color.gauge_desktop_e6e664), Zone(150f, 250f, R.color.gauge_desktop_f06464))),
        GaugeExpectation(GaugeType.GRTS, -40f, 120f, 17, listOf(Zone(-40f, 50f, R.color.gauge_desktop_8282b4), Zone(50f, 100f, R.color.gauge_desktop_787878), Zone(100f, 120f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.FUEL_LEVEL, 0f, 100f, 21, listOf(Zone(0f, 10f, R.color.gauge_desktop_fa6464), Zone(10f, 50f, R.color.gauge_desktop_dcdc64), Zone(50f, 100f, R.color.gauge_desktop_64dc64))),
        GaugeExpectation(GaugeType.EXHAUST_GAS_TEMP, 0f, 1100f, 17, listOf(Zone(0f, 400f, R.color.gauge_desktop_6464ff), Zone(400f, 950f, R.color.gauge_desktop_c8c878), Zone(950f, 1100f, R.color.gauge_desktop_ff6464))),
        GaugeExpectation(GaugeType.OIL_PRESSURE, 0f, 6f, 13, listOf(Zone(0f, 1f, R.color.gauge_desktop_8282be), Zone(1f, 3.5f, R.color.gauge_desktop_78f078), Zone(3.5f, 5f, R.color.gauge_desktop_f0f078), Zone(5f, 6f, R.color.gauge_desktop_ff8282))),
        GaugeExpectation(GaugeType.INJ_DUTY, 0f, 100f, 21, listOf(Zone(0f, 10f, R.color.gauge_desktop_6464ff), Zone(10f, 70f, R.color.gauge_desktop_64ff64), Zone(70f, 90f, R.color.gauge_desktop_f0ff64), Zone(90f, 100f, R.color.gauge_desktop_ff6464))),
        GaugeExpectation(GaugeType.MAF, 0f, 600f, 21, listOf(Zone(0f, 100f, R.color.gauge_desktop_8282b4), Zone(100f, 300f, R.color.gauge_desktop_78c878), Zone(300f, 500f, R.color.gauge_desktop_c8c878), Zone(500f, 600f, R.color.gauge_desktop_c87878))),
        GaugeExpectation(GaugeType.FAN_DUTY, 0f, 100f, 21, listOf(Zone(0f, 10f, R.color.gauge_desktop_6464ff), Zone(10f, 70f, R.color.gauge_desktop_64ff64), Zone(70f, 90f, R.color.gauge_desktop_f0ff64), Zone(90f, 100f, R.color.gauge_desktop_ff6464))),
        GaugeExpectation(GaugeType.MAP_DOT, -500f, 500f, 11, listOf(Zone(-500f, -200f, R.color.gauge_desktop_fa6464), Zone(-200f, -50f, R.color.gauge_desktop_64fa64), Zone(-50f, 0f, R.color.gauge_desktop_6464fa), Zone(0f, 50f, R.color.gauge_desktop_6464fa), Zone(50f, 200f, R.color.gauge_desktop_64fa64), Zone(200f, 500f, R.color.gauge_desktop_fa6464))),
        GaugeExpectation(GaugeType.FUEL_TEMP, -40f, 120f, 17, listOf(Zone(-40f, 50f, R.color.gauge_desktop_8282b4), Zone(50f, 100f, R.color.gauge_desktop_787878), Zone(100f, 120f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.EGO_CORR2, -50f, 50f, 17, listOf(Zone(-50f, -20f, R.color.gauge_desktop_e68282), Zone(-20f, 20f, R.color.gauge_desktop_78be78), Zone(20f, 50f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.WBO_AFR2, 6f, 24f, 19, listOf(Zone(6f, 10f, R.color.gauge_desktop_ff8282), Zone(10f, 14.55f, R.color.gauge_desktop_f0f082), Zone(14.55f, 14.85f, R.color.gauge_desktop_82ff82), Zone(14.85f, 15.45f, R.color.gauge_desktop_f0f078), Zone(15.45f, 15.75f, R.color.gauge_desktop_82ff82), Zone(15.75f, 20f, R.color.gauge_desktop_f0f082), Zone(20f, 24f, R.color.gauge_desktop_ff8282))),
        GaugeExpectation(GaugeType.WBO_AFR_TABL, 6f, 24f, 19, listOf(Zone(6f, 10f, R.color.gauge_desktop_ff8282), Zone(10f, 14.55f, R.color.gauge_desktop_f0f082), Zone(14.55f, 14.85f, R.color.gauge_desktop_82ff82), Zone(14.85f, 15.45f, R.color.gauge_desktop_f0f078), Zone(15.45f, 15.75f, R.color.gauge_desktop_82ff82), Zone(15.75f, 20f, R.color.gauge_desktop_f0f082), Zone(20f, 24f, R.color.gauge_desktop_ff8282))),
        GaugeExpectation(GaugeType.AFR_DIFF, -10f, 10f, 19, listOf(Zone(-9f, -2f, R.color.gauge_desktop_8282ff), Zone(-2f, -1f, R.color.gauge_desktop_f0f082), Zone(-1f, 1f, R.color.gauge_desktop_82ff82), Zone(1f, 2f, R.color.gauge_desktop_f0f082), Zone(2f, 9f, R.color.gauge_desktop_ff8282))),
        GaugeExpectation(GaugeType.AFR_DIFF2, -10f, 10f, 19, listOf(Zone(-9f, -2f, R.color.gauge_desktop_8282ff), Zone(-2f, -1f, R.color.gauge_desktop_f0f082), Zone(-1f, 1f, R.color.gauge_desktop_82ff82), Zone(1f, 2f, R.color.gauge_desktop_f0f082), Zone(2f, 9f, R.color.gauge_desktop_ff8282))),
        GaugeExpectation(GaugeType.GAS_PRESSURE_SENS, 0f, 400f, 21, listOf(Zone(0f, 100f, R.color.gauge_desktop_b48282), Zone(100f, 400f, R.color.gauge_desktop_78c878))),
        GaugeExpectation(GaugeType.FUEL_PRESSURE_SENS, 0f, 700f, 21, listOf(Zone(0f, 300f, R.color.gauge_desktop_b48282), Zone(300f, 700f, R.color.gauge_desktop_78c878))),
        GaugeExpectation(GaugeType.APPS1, 0f, 100f, 17, listOf(Zone(0f, 10f, R.color.gauge_desktop_b6b6d3), Zone(10f, 90f, R.color.gauge_desktop_93ea79), Zone(90f, 100f, R.color.gauge_desktop_fb5537))),
        GaugeExpectation(GaugeType.OTS, -50f, 140f, 17, listOf(Zone(-40f, 50f, R.color.gauge_desktop_8282b4), Zone(50f, 100f, R.color.gauge_desktop_787878), Zone(100f, 140f, R.color.gauge_desktop_e68282))),
        GaugeExpectation(GaugeType.EST_TORQUE, -50f, 200f, 21, listOf(Zone(-50f, 0f, R.color.gauge_desktop_8282b4), Zone(0f, 125f, R.color.gauge_desktop_78bb78), Zone(125f, 160f, R.color.gauge_desktop_ccce84), Zone(160f, 200f, R.color.gauge_desktop_f87070))),
        GaugeExpectation(GaugeType.REQ_TORQUE, -50f, 200f, 21, listOf(Zone(-50f, 0f, R.color.gauge_desktop_8282b4), Zone(0f, 125f, R.color.gauge_desktop_78bb78), Zone(125f, 160f, R.color.gauge_desktop_ccce84), Zone(160f, 200f, R.color.gauge_desktop_f87070))),
        )
        assertEquals("frozen desktop fixture covers all desktop-backed modern gauges", 47, expected.size)
        expected.forEach { fixture ->
            val type = fixture.type
            assertEquals("${type.name} min", fixture.min, type.minValue, EPSILON)
            assertEquals("${type.name} max", fixture.max, type.maxValue, EPSILON)
            assertEquals("${type.name} inclusive tick count", fixture.tickCount, type.tickCount)
            assertEquals("${type.name} zone count", fixture.zones.size, type.sections.size)
            fixture.zones.zip(type.sections).forEachIndexed { index, (zone, actual) ->
                assertEquals("${type.name} zone $index physical start", zone.start, fixture.min + actual.start * (fixture.max - fixture.min), EPSILON)
                assertEquals("${type.name} zone $index physical end", zone.end, fixture.min + actual.end * (fixture.max - fixture.min), EPSILON)
                assertEquals("${type.name} zone $index desktop color resource", zone.color, actual.color)
            }
        }
    }

    @Test fun addedReadoutsUseCorrectPacketFieldsUnitsPrecisionAndNumericDefaults() {
        val packet = SensorsPacket(
            distance = 12.3f,
            fuelFlowFrequency = 10f,
            cons_fuel = 20f,
            speed = 100f,
        )
        packet.inj_ffd = 8f
        packet.inj_ffh = 2.5f
        assertEquals(8f, GaugeType.FUEL_CONSUMPTION.getGaugeItem(packet), EPSILON)
        assertEquals(12.3f, GaugeType.DISTANCE.getGaugeItem(packet), EPSILON)
        assertEquals(2.5f, GaugeType.FUEL_CONSUMPTION_HOURLY.getGaugeItem(packet), EPSILON)
        assertEquals(20f, GaugeType.CONSUMED_FUEL.getGaugeItem(packet), EPSILON)
        assertEquals(1, GaugeType.DISTANCE.decimals)
        assertEquals(2, GaugeType.FUEL_CONSUMPTION_HOURLY.decimals)
        assertEquals(3, GaugeType.CONSUMED_FUEL.decimals)
        assertTrue(GaugeType.DISTANCE.defaultNumericView)
        assertTrue(GaugeType.CONSUMED_FUEL.defaultNumericView)
        assertTrue(!GaugeType.FUEL_CONSUMPTION_HOURLY.defaultNumericView)
    }

    @Test fun indicatorMappingsKeepFuelValveIndependentOfGasMode() {
        val gasOnly = SensorsPacket(sensorsFlags = 1 shl 2)
        assertTrue(IndicatorType.GAS_VALVE.isActive(gasOnly))
        assertTrue(!IndicatorType.FI_FUEL.isActive(gasOnly))
        val ephhOnly = SensorsPacket(sensorsFlags = 1)
        assertTrue(!IndicatorType.GAS_VALVE.isActive(ephhOnly))
        assertTrue(IndicatorType.FI_FUEL.isActive(ephhOnly))
    }

    @Test fun gaugeFormattingUsesPerTypePrecisionAndRejectsNonFiniteReadings() {
        assertEquals("0.012", GaugeType.ADD1.formatValue(.01234f))
        assertEquals("12.9", GaugeType.VEHICLE_SPEED.formatValue(12.9f))
        assertEquals("2.34", GaugeType.INJ_PW.formatValue(2.34f))
        assertEquals("14.55", GaugeType.WBO_AFR.formatValue(14.55f))
        assertEquals("—", GaugeType.ADD1.formatValue(Float.NaN))
    }

    @Test fun injectionTimingModeChangesOnlyPhaseGaugeBounds() {
        val defaultScale = GaugeScale(GaugeType.BEGIN_INJ_PHASE)
        val centeredScale = GaugeScale(GaugeType.BEGIN_INJ_PHASE, InjectionTimingMode.Btdc360)
        assertEquals(0f, defaultScale.minValue, EPSILON)
        assertEquals(720f, defaultScale.maxValue, EPSILON)
        assertEquals(-360f, centeredScale.minValue, EPSILON)
        assertEquals(360f, centeredScale.maxValue, EPSILON)
        assertEquals(-360f, centeredScale.ticks.first().value, EPSILON)
        assertEquals(360f, centeredScale.ticks.last().value, EPSILON)
        assertEquals(defaultScale.sections, centeredScale.sections)
        assertEquals(0.5f, centeredScale.normalized(0f)!!, EPSILON)

        val unrelatedScale = GaugeScale(GaugeType.TEMPERATURE, InjectionTimingMode.Btdc360)
        assertEquals(GaugeType.TEMPERATURE.minValue, unrelatedScale.minValue, EPSILON)
        assertEquals(GaugeType.TEMPERATURE.maxValue, unrelatedScale.maxValue, EPSILON)
    }

    private data class Zone(val start: Float, val end: Float, val color: Int)
    private data class GaugeExpectation(val type: GaugeType, val min: Float, val max: Float, val tickCount: Int, val zones: List<Zone>)

    private companion object {
        const val EPSILON = .0001f
    }
}
