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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.domain.parameters.AccelerationValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class AccelerationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingVisibilityUnknownSelectorsAndCallbacksAreUserDriven() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unknown = context.getString(R.string.firmware_unknown_value)
        assertEquals(2, context.resources.getStringArray(R.array.accel_enrichment_types).size)
        assertEquals(4, context.resources.getStringArray(R.array.accel_wallwet_types).size)
        val state = mutableStateOf<AccelerationValues?>(null)
        var edits = 0
        val selections = mutableListOf<Int>()
        val balances = mutableListOf<Float>()
        compose.setContent {
            SecuComposeTheme {
                AccelerationScreen(state.value, { edits++ }, { selections += it }, { selections += it }, {
                    balances += it
                    state.value = state.value?.copy(balance = it)
                })
            }
        }
        compose.onNodeWithTag("acceleration_loading").assertIsDisplayed()
        val values = AccelerationValues(10, 100f, 10, 7, 10, 40f, 10, -10f, -10f, 5)
        compose.runOnIdle { state.value = values }
        compose.onNodeWithTag("acceleration_ae_time").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_xtau_start").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_ae_type_value", useUnmergedTree = true).assertTextEquals("$unknown (7)")
        compose.onNodeWithTag("acceleration_wallwet_model_value", useUnmergedTree = true).performScrollTo().assertTextEquals("$unknown (5)")
        compose.onNodeWithTag("acceleration_ae_type_value", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("acceleration_balance_value").assertTextEquals("40%")
        compose.onNodeWithTag("acceleration_balance").performScrollTo()
        compose.onNodeWithText(context.getString(R.string.accel_par_ae_type_title), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_ae_type_value", useUnmergedTree = true).assertTextEquals("$unknown (7)")
        compose.onNodeWithTag("acceleration_wallwet_model").performScrollTo()
        compose.onNodeWithText(context.getString(R.string.accel_par_fuel_film_model_title), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("acceleration_wallwet_model_value", useUnmergedTree = true).assertTextEquals("$unknown (5)")
        compose.runOnIdle { state.value = values.copy() }
        assertEquals(0, edits)
        assertEquals(emptyList<Int>(), selections)
        assertEquals(emptyList<Float>(), balances)
        compose.onNodeWithTag("acceleration_ae_type").performScrollTo().performClick()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        assertEquals(emptyList<Int>(), selections)
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
        compose.runOnIdle { state.value = values.copy(aeType = 0, wallwetModel = 0) }
        compose.onAllNodesWithTag("acceleration_ae_time").assertCountEquals(0)
        compose.onAllNodesWithTag("acceleration_xtau_start").assertCountEquals(0)
        compose.runOnIdle { state.value = values.copy(aeType = 0, wallwetModel = 0) }
        assertEquals(listOf(1, 2), selections)
    }

    @Test fun dependentRowsRemainVisibleButDisabledWhenFirmwareLacksInjection() {
        val values = AccelerationValues(10, 100f, 10, 1, 10, 40f, 10, -10f, -10f, 1)
        compose.setContent {
            SecuComposeTheme {
                AccelerationScreen(values, {}, {}, {}, {}, capabilities = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 29)))
            }
        }
        compose.onNodeWithTag("acceleration_ae_time").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("acceleration_wallwet_model").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("acceleration_xtau_start").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    }
}
