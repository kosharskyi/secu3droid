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
package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.models.packets.out.params.LtftParamPacket

class LtftMappingTest {
    @Test fun everyFieldMapsInBothDirectionsAndMetadataMatchesLegacyControls() {
        val packet = LtftParamPacket(
            mode = 2, learnClt = 12.5f, learnCltUp = 90f, learnIatUp = 70f,
            learnGrad = .25f, learnGpa = 100f, learnGpd = 12f, min = -10f, max = 15f,
            learnRpm0 = 800, learnRpm1 = 5000, learnLoad0 = 20f, learnLoad1 = 300f,
            deadBand0 = 1.2f, deadBand1 = 3.4f,
        )
        val values = LtftValues.from(packet)
        assertEquals(2, values.mode)
        assertEquals(14, values.numbers.size)
        LtftNumber.entries.forEach { number ->
            val changed = values.value(number) + number.step
            applyLtftValue(packet, number, changed)
            val expected = if (number.integer) changed.toInt().toFloat() else changed
            assertEquals(expected, LtftValues.from(packet).value(number), .0001f)
        }
        assertEquals(listOf("LOWER_CLT", "UPPER_CLT", "UPPER_IAT", "LEARNING_GRADIENT", "ABS_GAS_PRESSURE", "DIFF_GAS_PRESSURE", "BOTTOM_LIMIT", "TOP_LIMIT", "LOWER_RPM", "UPPER_RPM", "LOWER_LOAD", "UPPER_LOAD", "NEGATIVE_INSENSITIVITY", "POSITIVE_INSENSITIVITY"), LtftNumber.entries.map { it.name })
        assertEquals(-24.6f, LtftNumber.BOTTOM_LIMIT.min)
        assertEquals(24.6f, LtftNumber.TOP_LIMIT.max)
        assertEquals(.005f, LtftNumber.LEARNING_GRADIENT.step)
        assertEquals(0, LtftNumber.LOWER_RPM.precision)
    }
}
