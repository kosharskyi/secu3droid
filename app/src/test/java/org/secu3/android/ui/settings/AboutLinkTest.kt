/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AboutLinkTest {
    @Test
    fun sourceRepositoryUrlCanBeLocatedInLocalizedProse() {
        val text = "Исходный код доступен здесь: https://github.com/kosharskyi/secu3droid"
        val range = sourceUrlRange(text)

        requireNotNull(range)
        assertEquals("https://github.com/kosharskyi/secu3droid", text.substring(range.first, range.second))
    }

    @Test
    fun sourceLinkFallbackLeavesTextWithoutRepositoryUrlUntouched() {
        assertNull(sourceUrlRange("Source code is available on the project website."))
    }
}
