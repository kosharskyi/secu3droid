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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.domain.parameters.LtftValues

class LtftScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loadingAndDataArePassiveUntilTheUserEditsOrSelects() {
        var edits = 0
        val selections = mutableListOf<Int>()
        val state = mutableStateOf<LtftValues?>(null)
        compose.setContent {
            SecuComposeTheme {
                LtftScreen(state.value, { edits++ }, { selections += it })
            }
        }
        compose.onNodeWithTag("ltft_loading").assertIsDisplayed()
        compose.runOnIdle { state.value = LtftValues(8, 5f, 5f, 5f, 5f, 5f, 5f, -5f, 5f, 5, 5, 5f, 5f, 5f, 5f) }
        compose.onNodeWithTag("ltft_mode_value", useUnmergedTree = true).assertTextEquals("Unknown (8)")
        compose.onNodeWithTag("ltft_lower_clt").assertIsDisplayed()
        compose.onNodeWithTag("ltft_bottom_limit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ltft_positive_insensitivity").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, edits)
            assertEquals(emptyList<Int>(), selections)
        }
        compose.onNodeWithTag("ltft_lower_clt").performScrollTo().performClick()
        compose.onNodeWithTag("ltft_mode").performScrollTo().performClick()
        compose.onNodeWithTag("ltft_mode_option_3").performClick()
        compose.runOnIdle {
            assertEquals(1, edits)
            assertEquals(listOf(3), selections)
        }
    }

    @Test fun dismissingModeChoicesDoesNotSendASelection() {
        val selections = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                LtftScreen(LtftValues(8, 5f, 5f, 5f, .25f, 5f, 5f, -5f, 5f, 5, 5, 5f, 5f, 5f, 5f), {}, selections::add)
            }
        }
        compose.onNodeWithTag("ltft_mode").performClick()
        compose.onNodeWithTag("ltft_mode_option_0").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onAllNodesWithTag("ltft_mode_option_0").assertCountEquals(0)
        compose.runOnIdle { assertEquals(emptyList<Int>(), selections) }
    }
}
