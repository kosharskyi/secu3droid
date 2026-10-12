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
package org.secu3.android.ui.parameters.pages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.models.packets.out.params.UniOutParamPacket

class UniversalOutputsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val conditions = (0..41).associateWith { "Condition $it" }
    private val functions = listOf("OR", "AND", "XOR", "2nd", "None")

    @Test
    fun loadingIsPassiveAndSixOutputsStartCollapsed() {
        var changes = 0
        val state = androidx.compose.runtime.mutableStateOf<UniversalOutputsValues?>(null)
        var selectedPair: Int? = null
        compose.setContent {
            UniversalOutputsScreen(state.value, conditions, functions, { output, changed ->
                changes++
                state.value = state.value!!.copy(outputs = state.value!!.outputs.toMutableList().also { it[output - 1] = changed })
            }, { selectedPair = it })
        }
        compose.onNodeWithTag("uniout-loading").assertIsDisplayed()
        compose.runOnIdle { state.value = UniversalOutputsValues.from(UniOutParamPacket()) }
        compose.onAllNodesWithTag("uniout-loading").assertCountEquals(0)
        (1..6).forEach { compose.onNodeWithTag("output-$it-collapsed").performScrollTo().assertIsDisplayed() }
        assertEquals(0, changes)
        val output1Bottom = compose.onNodeWithTag("output-1-card").fetchSemanticsNode().boundsInRoot.bottom
        val pairFunction = compose.onNodeWithTag("outputs-link-function").performScrollTo().assertIsEnabled()
        val pairBounds = pairFunction.fetchSemanticsNode().boundsInRoot
        val output2Top = compose.onNodeWithTag("output-2-card").fetchSemanticsNode().boundsInRoot.top
        assertTrue(kotlin.math.abs((pairBounds.top - output1Bottom) - (output2Top - pairBounds.bottom)) < 1f)
        pairFunction.performClick()
        compose.onNodeWithText("AND").performClick()
        compose.runOnIdle { assertEquals(1, selectedPair) }
        compose.onNodeWithTag("outputs-link-function").performClick()
        compose.onNodeWithText("None").performClick()
        compose.runOnIdle { assertEquals(15, selectedPair) }
    }

    @Test
    fun activeOutputShowsSelectorsThresholdDialogsAndConditionalSecondCondition() {
        val active = UniOutParamPacket().also { it.output1Use = true; it.output1LogicFunc = 15 }
        val state = androidx.compose.runtime.mutableStateOf(UniversalOutputsValues.from(active))
        val events = mutableListOf<String>()
        compose.setContent {
            UniversalOutputsScreen(state.value, conditions, functions, { output, changed ->
                events += "$output:$changed"
                state.value = state.value!!.copy(outputs = state.value!!.outputs.toMutableList().also { it[output - 1] = changed })
            }, {})
        }
        compose.onNodeWithTag("output-1-condition-1-picker").assertIsDisplayed()
            .assertHasClickAction()
        compose.onNodeWithText("Condition 1 for output 1").assertIsDisplayed()
        compose.onNodeWithText("Condition 0").assertIsDisplayed()
        compose.onNodeWithTag("output-1-condition-1-group").assertIsDisplayed()
        compose.onNodeWithTag("output-1-threshold-on-1").assertIsDisplayed().assertHasClickAction().performClick()
        compose.onNodeWithText("ON value threshold").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onAllNodesWithTag("output-1-condition-2-picker").assertCountEquals(0)
        val condition1Bounds = compose.onNodeWithTag("output-1-condition-1-group").fetchSemanticsNode().boundsInRoot
        val logicBounds = compose.onNodeWithTag("output-1-logical-function").fetchSemanticsNode().boundsInRoot
        val resultInversionBounds = compose.onNodeWithTag("output-1-result-inversion").fetchSemanticsNode().boundsInRoot
        assertEquals(16f * compose.density.density, logicBounds.top - condition1Bounds.bottom, 1f)
        assertEquals(16f * compose.density.density, resultInversionBounds.top - logicBounds.bottom, 1f)
        compose.onNodeWithTag("output-1-logical-function").performClick()
        compose.onNodeWithText("AND").performClick()
        compose.onNodeWithTag("output-1-condition-2-picker").assertIsDisplayed()
            .assertHasClickAction()
        compose.onNodeWithTag("output-1-condition-2-group").assertIsDisplayed()
        val condition2Bounds = compose.onNodeWithTag("output-1-condition-2-group").fetchSemanticsNode().boundsInRoot
        assertTrue(condition1Bounds.bottom < condition2Bounds.top)
        val updatedLogicBounds = compose.onNodeWithTag("output-1-logical-function").fetchSemanticsNode().boundsInRoot
        val updatedInversionBounds = compose.onNodeWithTag("output-1-result-inversion").fetchSemanticsNode().boundsInRoot
        assertEquals(16f * compose.density.density, condition2Bounds.top - updatedLogicBounds.bottom, 1f)
        assertEquals(16f * compose.density.density, updatedInversionBounds.top - condition2Bounds.bottom, 1f)
        assertEquals(true, events.isNotEmpty())
    }

    @Test
    fun conditionOneExcludesTimerCodesAndUnknownWireIdsRemainVisibleAndSafe() {
        val values = UniversalOutputsValues.from(UniOutParamPacket().also {
            it.output1Use = true
            it.output1Condition1 = 99
            it.output1LogicFunc = 9
        })
        compose.setContent { UniversalOutputsScreen(values, conditions, functions, { _, _ -> }, {}) }
        compose.onNodeWithTag("output-1-condition-1-picker").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Code 99").assertIsDisplayed()
        compose.onNodeWithText("Thresholds unavailable for ON value").assertIsDisplayed()
        compose.onAllNodesWithTag("output-1-threshold-on-1").assertCountEquals(0)
        compose.onNodeWithTag("output-1-logical-function").assertHasClickAction()
        compose.onNodeWithText("Code 9").assertIsDisplayed()
        compose.onNodeWithTag("output-1-condition-1-picker").performClick()
        compose.onAllNodesWithText(conditions.getValue(7)).assertCountEquals(0)
        compose.onAllNodesWithText(conditions.getValue(23)).assertCountEquals(0)
    }
}
