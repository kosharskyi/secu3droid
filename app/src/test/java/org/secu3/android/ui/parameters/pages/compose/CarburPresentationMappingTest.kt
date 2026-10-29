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
import org.secu3.android.domain.parameters.CarburValues

class CarburPresentationMappingTest {
    @Test fun eachEditorMapsToItsNamedRawFieldAndRetainsDesktopMetadata() {
        val values = CarburValues(
            idleLowRpm = 251,
            idleHighRpm = 252,
            idleLowGasRpm = 253,
            idleHighGasRpm = 254,
            cutoffDelay = .55f,
            valvePressure = 6.25f,
            tpsThreshold = 31.5f,
            mapThreshold = 88.5f,
            coolantThreshold = -10.5f,
            revLimitLowRpm = 5001,
            revLimitHighRpm = 5502,
            inverted = false,
            fuelCutOutput = 0,
            ignitionCutOutput = 15,
        )
        val expected = listOf(251f, 252f, 253f, 254f, .55f, 6.25f, 31.5f, 88.5f, -10.5f, 5001f, 5502f)

        assertEquals(expected, CarburNumber.entries.map { it.value(values) })
        assertEquals(listOf(250f, 250f, 250f, 250f, 0f, 0f, 0f, 0f, -40f, 500f, 500f), CarburNumber.entries.map { it.min })
        assertEquals(listOf(30000f, 30000f, 30000f, 30000f, 2.5f, 500f, 100f, 250f, 300f, 30000f, 30000f), CarburNumber.entries.map { it.max })
        assertEquals(listOf(0, 0, 0, 0, 2, 1, 1, 1, 1, 0, 0), CarburNumber.entries.map { it.precision })
        assertEquals(listOf(true, true, true, true, false, false, false, false, false, true, true), CarburNumber.entries.map { it.integer })
    }
}
