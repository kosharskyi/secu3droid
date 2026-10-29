/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.domain.parameters.TemperatureValues

class TemperaturePresentationMappingTest {
    @Test fun editorMetadataAndIntegerFieldsRemainDesktopCompatible() {
        assertEquals(listOf(TemperatureNumber.PWM_FREQUENCY, TemperatureNumber.AC_MIN_RPM, TemperatureNumber.FAN_TIMER), TemperatureNumber.entries.filter { it.integer })
        assertEquals(listOf(2, 2, 0, 4, 4, 0, 0), TemperatureNumber.entries.map { it.precision })
        assertEquals(listOf(-10f, -10f, 10f, 0f, 0f, 500f, 0f), TemperatureNumber.entries.map { it.min })
        assertEquals(listOf(125f, 125f, 5000f, 5f, 5f, 3000f, 600f), TemperatureNumber.entries.map { it.max })
        assertEquals(listOf(.2f, .2f, 1f, .01f, .01f, 10f, 1f), TemperatureNumber.entries.map { it.step })
        assertEquals(3, TemperatureFlag.entries.size)
    }

    @Test fun eachRowAndSwitchReadsItsNamedDomainValue() {
        val values = TemperatureValues(95.4f, 88.2f, 100, 1.234f, 2.345f, 900, 20.5f, true, false, true)
        assertEquals(listOf(95.4f, 88.2f, 100f, 1.234f, 2.345f, 900f, 20.5f), TemperatureNumber.entries.map { it.value(values) })
        assertEquals(listOf(true, false, true), TemperatureFlag.entries.map { it.checked(values) })
    }
}
