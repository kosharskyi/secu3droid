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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.CkpsValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class CkpsScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun state() = CkpsValues(60, 2, 5, 12f, 40f, 2.5f, false, true, true, false, false, 24, 4, (11..55).toList())
    @Test
    fun loadingScrollAndRecompositionHaveNoCallbacks() {
        var callbacks = 0
        val state = mutableStateOf<CkpsValues?>(null)
        compose.setContent {
            SecuComposeTheme {
                CkpsScreen(
                    state.value,
                    { callbacks++ },
                    { _, _ -> callbacks++ },
                    { _, _ -> callbacks++ },
                )
            }
        }
        compose.onNodeWithTag("ckps_loading").assertIsDisplayed()
        compose.runOnIdle { state.value = state() }
        listOf(
            "ckps_edge_negative",
            "ckps_ref_positive",
            "ckps_spark-switch",
            "ckps_merge-switch",
            "ckps_wheel",
            "ckps_missing",
            "ckps_teeth_btdc",
            "ckps_cylinders",
            "ckps_pulse",
            "ckps_hall_width",
            "ckps_degrees",
            "ckps_cam-switch",
            "ckps_mttf",
        ).forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        compose.runOnIdle { state.value = state() }
        assertEquals(0, callbacks)
    }

    @Test
    fun radioSwitchChoiceAndNumericActionsReachCallbacks() {
        val edits = mutableListOf<CkpsNumber>()
        val flags = mutableListOf<Pair<CkpsFlag, Boolean>>()
        val choices = mutableListOf<Pair<Boolean, Int>>()
        compose.setContent {
            SecuComposeTheme {
                CkpsScreen(
                    state(),
                    edits::add,
                    { flag, value -> flags += flag to value },
                    { teeth, value -> choices += teeth to value },
                )
            }
        }
        CkpsNumber.entries.forEach { number ->
            compose.onNodeWithTag("ckps_${number.name.lowercase()}")
                .performScrollTo()
                .performClick()
        }
        assertEquals(CkpsNumber.entries, edits)

        listOf("ckps_edge_positive", "ckps_ref_negative").forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().performClick()
        }
        listOf("ckps_spark-switch", "ckps_merge-switch", "ckps_cam-switch").forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().performClick()
        }
        assertEquals(
            listOf(
                CkpsFlag.CKPS_EDGE to true,
                CkpsFlag.REFERENCE_EDGE to false,
                CkpsFlag.SPARK_RISING to false,
                CkpsFlag.MERGE_OUTPUTS to true,
                CkpsFlag.USE_CAM_REFERENCE to true,
            ),
            flags,
        )

        compose.onNodeWithTag("ckps_teeth_btdc").performScrollTo().assertHasClickAction().performClick()
        compose.onNodeWithTag("ckps_option_55").performScrollTo().performClick()
        compose.onNodeWithTag("ckps_cylinders").performScrollTo().performClick()
        compose.onNodeWithTag("ckps_option_8").performScrollTo().performClick()
        assertEquals(listOf(true to 55, false to 8), choices)
    }

    @Test
    fun excludedRawSelectorValuesRemainVisibleWhileChoicesUseCurrentCapabilities() {
        val raw = state().copy(teethBeforeTdc = 56, cylinders = 3)
        compose.setContent {
            SecuComposeTheme {
                CkpsScreen(raw, {}, { _, _ -> }, { _, _ -> }, ParameterCapabilities(FirmwareInfoPacket()))
            }
        }
        compose.onNodeWithTag("ckps_teeth_btdc-value", useUnmergedTree = true).performScrollTo().assertTextEquals("Code 56")
        compose.onNodeWithTag("ckps_cylinders-value", useUnmergedTree = true).performScrollTo().assertTextEquals("3")
        compose.onNodeWithTag("ckps_cylinders").performScrollTo().performClick()
        compose.onNodeWithTag("ckps_option_6").assertIsDisplayed()
        compose.onAllNodesWithTag("ckps_option_3").assertCountEquals(0)
    }
}
