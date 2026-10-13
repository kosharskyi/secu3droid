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

class ParamFloatInputValidationTest {
    @Test fun acceptsNegativeValuesWithinBoundsAndRejectsPartialOrOutOfRangeInput() {
        assertTrue(isValidFloatInput("-14.5", -15f, 60f))
        assertTrue(isValidFloatInput("-15", -15f, 60f))
        assertTrue(isValidFloatInput("60", -15f, 60f))
        listOf("", "-", "-.", ".", "-15.1", "60.1").forEach {
            assertFalse("$it must be invalid", isValidFloatInput(it, -15f, 60f))
        }
        assertFalse(isValidFloatInput("-1", 0f, 10f))
    }

    @Test fun steppingUsesOutOfRangeFloatsBeforeClampingInEitherDirection() {
        val step = ::stepFloatValue
        assertEquals(2f, step("2.5", 1f, .5f, -2f, 2f, -1), 0f)
        assertEquals(2f, step("2.5", 1f, .5f, -2f, 2f, 1), 0f)
        assertEquals(-2f, step("-2.5", 1f, .5f, -2f, 2f, 1), 0f)
        assertEquals(-2f, step("-2.5", 1f, .5f, -2f, 2f, -1), 0f)
    }

    @Test fun steppingIncompleteFloatSyntaxFallsBackToLastValidValue() {
        val step = ::stepFloatValue
        assertEquals(1.5f, step("-.", 1f, .5f, -2f, 2f, 1), 0f)
        assertEquals(.5f, step(".", 1f, .5f, -2f, 2f, -1), 0f)
    }
}
