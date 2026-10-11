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
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts: http://secu-3.org; vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme

class CarburScreenTest {
    @get:Rule val compose = createComposeRule()
    private val options = listOf("1", "2", "3", "4", "5", "6", "no")
    private fun state() = CarburUiState.Data(CarburValues(CarburNumber.entries.associateWith { it.ordinal.toFloat() }, false, 1, 15))

    @Test fun loadingScrollAndRecompositionDoNotCallActions() {
        var callbacks = 0
        val state = mutableStateOf<CarburUiState>(CarburUiState.Loading)
        compose.setContent { SecuComposeTheme { CarburScreen(state.value, options, { callbacks++ }, { callbacks++ }, { _, _ -> callbacks++ }) } }
        compose.onNodeWithTag("carbur_loading").assertIsDisplayed()
        compose.runOnIdle { state.value = state() }
        CarburNumber.entries.forEach { compose.onNodeWithTag("carbur_${it.name.lowercase()}").performScrollTo().assertIsDisplayed() }
        compose.onNodeWithTag("carbur_invers").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("carbur_fuelcut_uni").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("carbur_igncut_uni").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value = state() }
        assertEquals(0, callbacks)
    }

    @Test fun allParameterAndSelectorActionsInvokeCallbacksAndDropdownsAreOutlined() {
        val edits = mutableListOf<CarburNumber>()
        val selectors = mutableListOf<Pair<Boolean, Int>>()
        var switches = 0
        compose.setContent { SecuComposeTheme { CarburScreen(state(), options, edits::add, { switches++ }, { ignition, i -> selectors += ignition to i }) } }
        CarburNumber.entries.forEach { compose.onNodeWithTag("carbur_${it.name.lowercase()}").performScrollTo().performClick() }
        assertEquals(CarburNumber.entries, edits)
        compose.onNodeWithTag("carbur_invers-switch").performScrollTo().performClick()
        assertEquals(1, switches)
        listOf("carbur_fuelcut_uni" to false, "carbur_igncut_uni" to true).forEach { (tag, ignition) ->
            compose.onNodeWithTag(tag).performScrollTo().assertHasClickAction().performClick()
            compose.onNodeWithTag("carbur_option_6").performClick()
            assertEquals(ignition, selectors.last().first)
            assertEquals(6, selectors.last().second)
            compose.onNodeWithTag("${tag}-dropdown-indicator", useUnmergedTree = true).assertIsDisplayed()
        }
    }
}
