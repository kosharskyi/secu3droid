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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.TemperatureValues
import org.secu3.android.ui.compose.SecuComposeTheme

class TemperatureScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadingRendersAndDataScrollsWithoutInitializationCallbacks() {
        var edits = 0
        var flags = 0
        val state = mutableStateOf<TemperatureValues?>(null)
        compose.setContent {
            SecuComposeTheme { TemperatureScreen(state.value, { edits++ }, { _, _ -> flags++ }) }
        }
        compose.onNodeWithTag("temperature_loading").assertIsDisplayed()
        val values = TemperatureValues(1f, 2f, 3, 4f, 5f, 6, 7f, true, false, true)
        compose.runOnIdle { state.value = values }
        TemperatureNumber.entries.forEach { number ->
            compose.onNodeWithTag("temperature_${number.name.lowercase()}").performScrollTo().assertIsDisplayed()
            val expected = String.format(java.util.Locale.getDefault(), "%.${number.precision}f", number.value(values)) + " " + context.getString(number.unitRes)
            compose.onNodeWithTag("temperature_${number.name.lowercase()}-value", useUnmergedTree = true).assertTextEquals(expected)
        }
        TemperatureFlag.entries.forEach { flag ->
            compose.onNodeWithTag("temperature_flag_${flag.name.lowercase()}").performScrollTo().assertIsDisplayed()
        }
        compose.runOnIdle { state.value = values.copy(fanOn = 8f) }
        assertEquals(0, edits)
        assertEquals(0, flags)
    }

    @Test
    fun callbacksRunOnlyForExplicitRowAndSwitchActions() {
        val edits = mutableListOf<TemperatureNumber>()
        val changes = mutableListOf<Pair<TemperatureFlag, Boolean>>()
        val values = TemperatureValues(1f, 1f, 1, 1f, 1f, 1000, 1f, false, false, false)
        compose.setContent {
            SecuComposeTheme { TemperatureScreen(values, { edits += it }, { flag, value -> changes += flag to value }) }
        }
        TemperatureNumber.entries.forEach { number ->
            compose.onNodeWithTag("temperature_${number.name.lowercase()}").performScrollTo().performClick()
            assertEquals(number, edits.last())
        }
        TemperatureFlag.entries.forEach { flag ->
            compose.onNodeWithTag("temperature_flag_${flag.name.lowercase()}-switch").performScrollTo().performClick()
            assertEquals(flag to true, changes.last())
        }
    }
}
