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
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

class DbwScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadingDataAndRendersHaveNoCallbacksAndHomePositionIsPassive() {
        var callbacks = 0
        val state = mutableStateOf<DbwUiState>(DbwUiState.Loading)
        compose.setContent { SecuComposeTheme { DbwScreen(state.value, onEdit = { callbacks++ }) } }
        compose.onNodeWithTag("dbw_loading").assertIsDisplayed()
        val values = DbwValues(DbwNumber.entries.associateWith { it.ordinal.toFloat() }, 37.25f)
        compose.runOnIdle { state.value = DbwUiState.Data(values) }
        DbwNumber.entries.forEach { number ->
            compose.onNodeWithTag("dbw_${number.name.lowercase()}").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("dbw_home_position").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dbw_home_position-value", useUnmergedTree = true)
            .assertTextEquals("37.25 ${context.getString(R.string.units_percents)}")
        compose.onNodeWithTag("dbw_home_position").assertHasNoClickAction()
        compose.runOnIdle { state.value = DbwUiState.Data(values.copy()) }
        assertEquals(0, callbacks)
    }

    @Test
    fun editableRowsInvokeOnlyTheirSelectedCallback() {
        val edits = mutableListOf<DbwNumber>()
        compose.setContent {
            SecuComposeTheme {
                DbwScreen(DbwUiState.Data(DbwValues(DbwNumber.entries.associateWith { 1f }, 12f)), edits::add)
            }
        }
        compose.onNodeWithTag("dbw_etc_p").performClick()
        assertEquals(listOf(DbwNumber.ETC_P), edits)
    }
}
