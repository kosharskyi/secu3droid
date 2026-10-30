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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.secu3.android.ui.parameters.ParameterEditRequest

class ParamIntInputValidationTest {
    @Test fun signedInputRejectsPartialAndOverflowButAcceptsNegativeValuesInRange() {
        val request = ParameterEditRequest.IntValue("Value", 0, -6, 12, 3) {}
        assertNull(request.parseInput("-"))
        assertNull(request.parseInput("2147483648"))
        assertEquals(-6, request.parseInput("-6"))
        assertEquals(-2, request.parseInput("-2"))
        assertNull(request.parseInput("-7"))
    }

    @Test fun steppingUsesOutOfRangeIntegersBeforeClampingInEitherDirection() {
        val request = ParameterEditRequest.IntValue("Value", 4, 0, 10, 3) {}
        assertEquals(9, request.stepValue("12", 4, -1))
        assertEquals(10, request.stepValue("12", 4, 1))
        assertEquals(1, request.stepValue("-2", 4, 1))
        assertEquals(0, request.stepValue("-2", 4, -1))
    }

    @Test fun steppingHandlesHugeIntegersAndFallsBackOnlyForUnparseableText() {
        val request = ParameterEditRequest.IntValue("Value", 4, 0, 10, 3) {}
        assertEquals(10, request.stepValue("999999999999999999999999999", 4, 1))
        assertEquals(10, request.stepValue("999999999999999999999999999", 4, -1))
        assertEquals(0, request.stepValue("-999999999999999999999999999", 4, 1))
        assertEquals(0, request.stepValue("-999999999999999999999999999", 4, -1))
        assertEquals(7, request.stepValue("-", 4, 1))
        assertEquals(1, request.stepValue("-", 4, -1))
    }
}
