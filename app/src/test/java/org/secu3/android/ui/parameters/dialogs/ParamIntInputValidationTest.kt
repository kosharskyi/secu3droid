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

class ParamIntInputValidationTest {
    @Test fun signedInputRejectsPartialAndOverflowButAcceptsNegativeValuesInRange() {
        assertNull(parseIntegerInput("-", -6, 12))
        assertNull(parseIntegerInput("2147483648", -6, 12))
        assertEquals(-6, parseIntegerInput("-6", -6, 12))
        assertEquals(-2, parseIntegerInput("-2", -6, 12))
        assertNull(parseIntegerInput("-7", -6, 12))
    }

    @Test fun steppingUsesOutOfRangeIntegersBeforeClampingInEitherDirection() {
        val step = ::stepIntegerValue
        assertEquals(9, step("12", 4, 3, 0, 10, -1))
        assertEquals(10, step("12", 4, 3, 0, 10, 1))
        assertEquals(1, step("-2", 4, 3, 0, 10, 1))
        assertEquals(0, step("-2", 4, 3, 0, 10, -1))
    }

    @Test fun steppingHandlesHugeIntegersAndFallsBackOnlyForUnparseableText() {
        val step = ::stepIntegerValue
        assertEquals(10, step("999999999999999999999999999", 4, 3, 0, 10, 1))
        assertEquals(10, step("999999999999999999999999999", 4, 3, 0, 10, -1))
        assertEquals(0, step("-999999999999999999999999999", 4, 3, 0, 10, 1))
        assertEquals(0, step("-999999999999999999999999999", 4, 3, 0, 10, -1))
        assertEquals(7, step("-", 4, 3, 0, 10, 1))
        assertEquals(1, step("-", 4, 3, 0, 10, -1))
    }
}
