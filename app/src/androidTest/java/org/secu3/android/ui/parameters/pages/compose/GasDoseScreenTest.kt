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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class GasDoseScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingAndUnavailableStatesRenderWithoutSending() {
        val firmwareAvailable = mutableStateOf(true)
        val values = mutableStateOf<GasDoseValues?>(null)
        var calls = 0
        compose.setContent {
            SecuComposeTheme {
                GasDoseScreen(values.value, firmwareAvailable.value, { calls++ }, { calls++ }, { calls++ }, { calls++ }, { calls++ }, { calls++ }, { calls++ })
            }
        }
        compose.onNodeWithTag("gas_dose_loading").assertExists()
        compose.runOnIdle { firmwareAvailable.value = false }
        compose.onNodeWithTag("gas_dose_unavailable").assertExists()
        assertEquals(0, calls)
        compose.runOnIdle {
            firmwareAvailable.value = true
            values.value = GasDoseValues.from(GasDoseParamPacket())
        }
        compose.onNodeWithTag("gas_dose_test").performClick()
        assertEquals(1, calls)
    }

    @Test fun renderRecompositionAndExplicitActions() {
        val state = mutableStateOf(GasDoseValues.from(GasDoseParamPacket(freq = 7)))
        var edits = 0
        val frequencies = mutableListOf<Int>()
        var maximumFrequencyChanges = 0
        var manualDelta = 0
        var manualReleases = 0
        var manualCancels = 0
        var tests = 0
        compose.setContent {
            SecuComposeTheme {
                GasDoseScreen(
                    state.value, true, { edits++ }, frequencies::add,
                    { maximumFrequencyChanges++ }, { manualDelta += it },
                    { manualReleases++ }, { manualCancels++ }, { tests++ },
                )
            }
        }
        compose.onNodeWithTag("gas_dose_frequency").assertTextContains("Code 7")
        compose.runOnIdle { state.value = state.value.copy() }
        assertEquals(0, edits)
        assertEquals(emptyList<Int>(), frequencies)
        assertEquals(0, maximumFrequencyChanges)
        assertEquals(0, manualDelta)
        assertEquals(0, manualReleases)
        assertEquals(0, manualCancels)
        assertEquals(0, tests)
        compose.onNodeWithTag("gas_dose_test").performClick()
        assertEquals(1, tests)
        compose.onNodeWithTag("gas_dose_manual_dec").performTouchInput { click() }
        assertEquals(-1, manualDelta)
        assertEquals(1, manualReleases)
        assertEquals(0, manualCancels)
        assertEquals(1, tests)
        compose.onNodeWithTag("gas_dose_frequency").performScrollTo().performClick()
        compose.onNodeWithTag("gas_dose_frequency_option_3").performClick()
        assertEquals(listOf(3), frequencies)
        assertEquals(1, tests)
        assertEquals(0, edits)
        assertEquals(0, maximumFrequencyChanges)
    }
}
