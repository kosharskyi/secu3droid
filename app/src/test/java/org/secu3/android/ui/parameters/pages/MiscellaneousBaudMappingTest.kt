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
package org.secu3.android.ui.parameters.pages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MiscellaneousBaudMappingTest {
    @Test fun allBaudRatesUseProtocolDivisors() {
        assertEquals(mapOf(0x0411 to "2400",0x0208 to "4800",0x0103 to "9600",0x00AD to "14400",0x0081 to "19200",0x0056 to "28800",0x0040 to "38400",0x002A to "57600",0x0015 to "115200",0x0009 to "250000"),MiscellaneousFragment.BAUD_RATE_CODES)
        assertEquals("9600",MiscellaneousFragment.BAUD_RATE_CODES[0x0103])
        assertNull(MiscellaneousFragment.BAUD_RATE_CODES[0x1234])
    }
}
