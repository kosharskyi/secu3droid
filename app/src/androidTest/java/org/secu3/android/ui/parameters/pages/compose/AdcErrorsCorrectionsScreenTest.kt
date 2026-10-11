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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme

class AdcErrorsCorrectionsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loadsScrollsAndDoesNotInvokeCallbacksOnInitialCompositionOrRecomposition() {
        var edits = 0
        var switches = 0
        val state = mutableStateOf<AdcCorrectionsUiState>(AdcCorrectionsUiState.Loading)
        compose.setContent {
            SecuComposeTheme { AdcErrorsCorrectionsScreen(state.value, { edits++ }, { switches++ }) }
        }
        compose.onNodeWithTag("adc_loading").assertIsDisplayed()
        val values = AdcCorrectionsValues(AdcNumber.entries.associateWith { it.ordinal.toFloat() }, false)
        compose.runOnIdle { state.value = AdcCorrectionsUiState.Data(values) }
        AdcNumber.entries.forEach { number ->
            compose.onNodeWithTag("adc_${number.name.lowercase()}").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("adc_group_11").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value = AdcCorrectionsUiState.Data(values.copy(numbers = values.numbers.toMap())) }
        assertEquals(0, edits)
        assertEquals(0, switches)
    }

    @Test
    fun callsOnlyForExplicitFieldAndSwitchActions() {
        val edits = mutableListOf<AdcNumber>()
        val switches = mutableListOf<Boolean>()
        val values = AdcCorrectionsValues(AdcNumber.entries.associateWith { 0f }, false)
        compose.setContent {
            SecuComposeTheme { AdcErrorsCorrectionsScreen(AdcCorrectionsUiState.Data(values), { edits += it }, { switches += it }) }
        }
        AdcNumber.entries.forEach { number ->
            compose.onNodeWithTag("adc_${number.name.lowercase()}").performScrollTo().performClick()
            assertEquals(number, edits.last())
        }
        compose.onNodeWithTag("adc_comp_mode-switch").performScrollTo().performClick()
        assertEquals(listOf(true), switches)
    }
}
