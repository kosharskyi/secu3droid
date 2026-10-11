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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme

class AccelerationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingVisibilityUnknownSelectorsAndCallbacksAreUserDriven() {
        val state = mutableStateOf<AccelerationUiState>(AccelerationUiState.Loading)
        var edits = 0
        val selections = mutableListOf<Int>()
        val balances = mutableListOf<Float>()
        compose.setContent {
            SecuComposeTheme {
                AccelerationScreen(state.value, { edits++ }, { selections += it }, { selections += it }, {
                    balances += it
                    val current = state.value as? AccelerationUiState.Data
                    if (current != null) state.value = AccelerationUiState.Data(current.values.copy(balance = it))
                })
            }
        }
        compose.onNodeWithTag("acceleration_loading").assertIsDisplayed()
        val values = AccelerationValues(AccelerationNumber.entries.associateWith { 10f }, 7, 40f, 5)
        compose.runOnIdle { state.value = AccelerationUiState.Data(values) }
        compose.onNodeWithTag("acceleration_ae_time").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_xtau_start").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_ae_type_value", useUnmergedTree = true).assertTextEquals("Unknown (7)")
        compose.onNodeWithTag("acceleration_wallwet_model_value", useUnmergedTree = true).performScrollTo().assertTextEquals("Unknown (5)")
        compose.onNodeWithTag("acceleration_ae_type_value", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("acceleration_balance_value").assertTextEquals("40%")
        compose.onNodeWithTag("acceleration_balance").performScrollTo()
        compose.onNodeWithText("AE type", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_ae_type_value", useUnmergedTree = true).assertTextEquals("Unknown (7)")
        compose.onNodeWithTag("acceleration_wallwet_model").performScrollTo()
        compose.onNodeWithText("Fuel film model", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_wallwet_model_value", useUnmergedTree = true).assertTextEquals("Unknown (5)")
        compose.runOnIdle { state.value = AccelerationUiState.Data(values.copy()) }
        assertEquals(0, edits)
        assertEquals(emptyList<Int>(), selections)
        assertEquals(emptyList<Float>(), balances)
        compose.onNodeWithTag("acceleration_ae_type").performScrollTo().performClick()
        compose.onNodeWithTag("acceleration_ae_type_option_1").performClick()
        assertEquals(listOf(1), selections)
        compose.onNodeWithTag("acceleration_wallwet_model").performScrollTo().performClick()
        compose.onNodeWithTag("acceleration_wallwet_model_option_2").performClick()
        assertEquals(listOf(1, 2), selections)
        compose.onNodeWithTag("acceleration_tps_threshold").performScrollTo().performClick()
        assertEquals(1, edits)
        compose.onNodeWithTag("acceleration_balance").performScrollTo().performTouchInput { click(center) }
        assertEquals(1, balances.size)
        compose.onNodeWithTag("acceleration_balance_value").assertTextEquals("50%")
        compose.runOnIdle { state.value = AccelerationUiState.Data(values.copy(aeType = 0, wallwetModel = 0)) }
        compose.onAllNodesWithTag("acceleration_ae_time").assertCountEquals(0)
        compose.onAllNodesWithTag("acceleration_xtau_start").assertCountEquals(0)
        compose.runOnIdle { state.value = AccelerationUiState.Data(values.copy(aeType = 0, wallwetModel = 0)) }
        assertEquals(listOf(1, 2), selections)
    }
}
