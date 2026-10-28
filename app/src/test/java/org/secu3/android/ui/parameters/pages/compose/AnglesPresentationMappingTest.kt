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
import org.secu3.android.domain.parameters.AnglesValues

class AnglesPresentationMappingTest {
    @Test fun editorMetadataRetainsDesktopBoundsAndEstablishedPrecision() {
        assertEquals(-15f, AnglesNumber.MAX.min)
        assertEquals(60f, AnglesNumber.MAX.max)
        assertEquals(-15f, AnglesNumber.MIN.min)
        assertEquals(60f, AnglesNumber.MIN.max)
        assertEquals(.25f, AnglesNumber.MAX.step)
        assertEquals(.25f, AnglesNumber.MIN.step)
        assertEquals(0f, AnglesNumber.DECREASE.min)
        assertEquals(10f, AnglesNumber.DECREASE.max)
        assertEquals(.025f, AnglesNumber.DECREASE.step)
        assertEquals(3, AnglesNumber.DECREASE.precision)
        assertEquals(0f, AnglesNumber.INCREASE.min)
        assertEquals(10f, AnglesNumber.INCREASE.max)
        assertEquals(.025f, AnglesNumber.INCREASE.step)
        assertEquals(3, AnglesNumber.INCREASE.precision)
        assertEquals(-30f, AnglesNumber.OCTANE.min)
        assertEquals(30f, AnglesNumber.OCTANE.max)
        assertEquals(.25f, AnglesNumber.OCTANE.step)
        assertEquals(-15f, AnglesNumber.SHIFT.min)
        assertEquals(55f, AnglesNumber.SHIFT.max)
        assertEquals(.1f, AnglesNumber.SHIFT.step)
        assertEquals(1, AnglesNumber.SHIFT.precision)
    }

    @Test fun eachRowReadsItsNamedDomainValueAndEachSwitchReadsItsNamedFlag() {
        val values = AnglesValues(11f, -12f, 4f, 1f, 2f, 2, true, false, true, -3f)
        assertEquals(11f, AnglesNumber.MAX.value(values))
        assertEquals(-12f, AnglesNumber.MIN.value(values))
        assertEquals(1f, AnglesNumber.DECREASE.value(values))
        assertEquals(2f, AnglesNumber.INCREASE.value(values))
        assertEquals(4f, AnglesNumber.OCTANE.value(values))
        assertEquals(-3f, AnglesNumber.SHIFT.value(values))
        assertEquals(true, AnglesFlag.ZERO.checked(values))
        assertEquals(true, AnglesFlag.WORKING_MAP.checked(values))
        assertEquals(false, AnglesFlag.MANUAL_CORRECTION.checked(values))
        assertEquals(true, AnglesFlag.ZERO_WITH_OCTANE.checked(values))
    }
}
