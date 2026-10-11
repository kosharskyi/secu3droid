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
        assertNull(ParamIntEditDialogFragment.parseValidInput("-", -6, 12))
        assertNull(ParamIntEditDialogFragment.parseValidInput("2147483648", -6, 12))
        assertEquals(-6, ParamIntEditDialogFragment.parseValidInput("-6", -6, 12))
        assertEquals(-2, ParamIntEditDialogFragment.parseValidInput("-2", -6, 12))
        assertNull(ParamIntEditDialogFragment.parseValidInput("-7", -6, 12))
    }
}
