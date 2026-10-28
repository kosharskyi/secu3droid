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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.StarterValues
import org.secu3.android.ui.compose.SecuComposeTheme

class StarterScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadingAndAllNumericValuesRenderAndRecompositionDoesNotSend() {
        var sent = 0
        val values = androidx.compose.runtime.mutableStateOf<StarterValues?>(null)
        compose.setContent {
            SecuComposeTheme {
                StarterScreen(values.value, onEdit = {}, onFlagChanged = { _, _ -> sent++ })
            }
        }
        compose.onNodeWithTag("starter_loading").assertIsDisplayed()
        val expectedValues = StarterValues(1, 2, 3f, 4f, 5, 6, 7f, 8f, 9f, 10f, 11, false, true)
        compose.runOnIdle { values.value = expectedValues }
        StarterNumber.entries.forEach {
            compose.onNodeWithTag("starter_${it.name.lowercase()}").performScrollTo().assertIsDisplayed()
            val expected = String.format(java.util.Locale.getDefault(), "%.${it.precision}f", it.value(expectedValues)) +
                " " + (it.unit.text ?: context.getString(it.unit.resourceId!!))
            compose.onNodeWithTag("starter_${it.name.lowercase()}-value", useUnmergedTree = true).assertTextEquals(expected)
        }
        compose.onNodeWithTag("starter_afterstart_petrol-value", useUnmergedTree = true).assertTextEquals("5 num")
        compose.onNodeWithTag("starter_afterstart_gas-value", useUnmergedTree = true).assertTextEquals("6 num")
        compose.onNodeWithTag("starter_stbl_countdown-value", useUnmergedTree = true).assertTextEquals("11 str")
        StarterFlag.entries.forEach {
            compose.onNodeWithTag("starter_flag_row_${it.name.lowercase()}").performScrollTo().assertIsDisplayed()
        }
        compose.runOnIdle { values.value = expectedValues.copy() }
        assertEquals(0, sent)
    }

    @Test
    fun tappingValueReportsEditAndSwitchReportsOnlyUserChanges() {
        var edit: StarterNumber? = null
        val changedFlags = mutableListOf<Pair<StarterFlag, Boolean>>()
        val initial = StarterValues(12, 12, 12f, 12f, 12, 12, 12f, 12f, 12f, 12f, 12, false, true)
        val values = androidx.compose.runtime.mutableStateOf<StarterValues?>(initial)
        compose.setContent {
            SecuComposeTheme {
                StarterScreen(
                    values.value,
                    onEdit = {
                        edit = it
                        values.value = initial.copy(starterOff = 42)
                    },
                    onFlagChanged = { flag, checked -> changedFlags += flag to checked },
                )
            }
        }
        compose.onNodeWithTag("starter_starter_off").performClick()
        assertEquals(StarterNumber.STARTER_OFF, edit)
        compose.onNodeWithTag("starter_starter_off-value", useUnmergedTree = true)
            .assertTextEquals(String.format(java.util.Locale.getDefault(), "%.0f", 42f) + " " +
                context.getString(StarterNumber.STARTER_OFF.unit.resourceId!!))
        compose.onNodeWithTag("starter_flag_row_allow_flood_clear_start-switch").performScrollTo().performClick()
        compose.onNodeWithTag("starter_flag_row_limit_cranking_pw-switch").performScrollTo().performClick()
        assertEquals(
            listOf(
                StarterFlag.ALLOW_FLOOD_CLEAR_START to true,
                StarterFlag.LIMIT_CRANKING_PW to false,
            ),
            changedFlags,
        )
    }
}
