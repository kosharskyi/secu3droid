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
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.parameters.dialogs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.ui.parameters.ParameterEditRequest

class ParamFloatInputValidationTest {
    @Test fun acceptsNegativeValuesWithinBoundsAndRejectsPartialOrOutOfRangeInput() {
        val request = ParameterEditRequest.FloatValue("Value", 1f, -15f, 60f, .5f, 2) {}
        assertTrue(request.parseInput("-14.5") != null)
        assertTrue(request.parseInput("-15") != null)
        assertTrue(request.parseInput("60") != null)
        listOf("", "-", "-.", ".", "-15.1", "60.1").forEach {
            assertFalse("$it must be invalid", request.parseInput(it) != null)
        }
        val nonNegative = ParameterEditRequest.FloatValue("Value", 1f, 0f, 10f, .5f, 2) {}
        assertFalse(nonNegative.parseInput("-1") != null)
    }

    @Test fun steppingUsesOutOfRangeFloatsBeforeClampingInEitherDirection() {
        val request = ParameterEditRequest.FloatValue("Value", 1f, -2f, 2f, .5f, 2) {}
        assertEquals(2f, request.stepValue("2.5", 1f, -1), 0f)
        assertEquals(2f, request.stepValue("2.5", 1f, 1), 0f)
        assertEquals(-2f, request.stepValue("-2.5", 1f, 1), 0f)
        assertEquals(-2f, request.stepValue("-2.5", 1f, -1), 0f)
    }

    @Test fun steppingIncompleteFloatSyntaxFallsBackToLastValidValue() {
        val request = ParameterEditRequest.FloatValue("Value", 1f, -2f, 2f, .5f, 2) {}
        assertEquals(1.5f, request.stepValue("-.", 1f, 1), 0f)
        assertEquals(.5f, request.stepValue(".", 1f, -1), 0f)
        assertEquals(1.5f, request.stepValue("NaN", 1f, 1), 0f)
    }

    @Test fun formattingUsesUsDecimalSeparatorAndFallbackPrecision() {
        val request = ParameterEditRequest.FloatValue("Value", 1.25f, -2f, 2f, .5f, 2) {}
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("1.25", request.formatValue())
            val fallbackPrecision = ParameterEditRequest.FloatValue("Value", 1.23456f, -2f, 2f, .5f, 9) {}
            assertEquals("1.2346", fallbackPrecision.formatValue())
        } finally {
            java.util.Locale.setDefault(original)
        }
    }
}
