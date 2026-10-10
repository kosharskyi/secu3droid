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
import org.secu3.android.ui.compose.SecuComposeTheme

class AnglesScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun loadingThenAllValuesAndFlagsRenderWithoutCallbacksOnRecomposition() {
        var edits = 0
        var changes = 0
        val state = mutableStateOf<AnglesUiState>(AnglesUiState.Loading)
        compose.setContent { SecuComposeTheme { AnglesScreen(state.value, { edits++ }, { _, _ -> changes++ }) } }
        compose.onNodeWithTag("angles_loading").assertIsDisplayed()
        val values = AnglesValues(AnglesNumber.entries.associateWith { it.ordinal + 1f }, AnglesFlag.entries.associateWith { it.ordinal % 2 == 0 })
        compose.runOnIdle { state.value = AnglesUiState.Data(values) }
        AnglesNumber.entries.forEach { number ->
            compose.onNodeWithTag("angles_${number.name.lowercase()}").performScrollTo().assertIsDisplayed()
            val expected = String.format(java.util.Locale.getDefault(), "%.${number.precision}f", number.ordinal + 1f) + " " +
                context.getString(number.unitRes)
            compose.onNodeWithTag("angles_${number.name.lowercase()}-value", useUnmergedTree = true).assertTextEquals(expected)
        }
        AnglesFlag.entries.forEach { compose.onNodeWithTag("angles_flag_${it.name.lowercase()}").performScrollTo().assertIsDisplayed() }
        compose.runOnIdle { state.value = AnglesUiState.Data(values.copy(numbers = values.numbers.toMap())) }
        assertEquals(0, edits)
        assertEquals(0, changes)
    }

    @Test fun editAndSwitchActionsReportOnlyUserInput() {
        var edited: AnglesNumber? = null
        val changes = mutableListOf<Pair<AnglesFlag, Boolean>>()
        val values = AnglesValues(AnglesNumber.entries.associateWith { 1f }, AnglesFlag.entries.associateWith { false })
        compose.setContent { SecuComposeTheme { AnglesScreen(AnglesUiState.Data(values), { edited = it }, { flag, checked -> changes += flag to checked }) } }
        compose.onNodeWithTag("angles_min").performClick()
        assertEquals(AnglesNumber.MIN, edited)
        compose.onNodeWithTag("angles_flag_working_map-switch").performScrollTo().performClick()
        assertEquals(listOf(AnglesFlag.WORKING_MAP to true), changes)
    }
}
