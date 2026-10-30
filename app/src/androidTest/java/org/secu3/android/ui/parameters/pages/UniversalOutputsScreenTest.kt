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

import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.domain.parameters.UniversalOutputConditions
import org.secu3.android.domain.parameters.UniversalOutputValues
import org.secu3.android.domain.parameters.UniversalOutputsValues
import org.secu3.android.domain.parameters.UniversalOutputsControl

class UniversalOutputsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val conditions = (0..41).associateWith { "Condition $it" }
    private val functions = listOf("OR", "AND", "XOR", "2nd", "None")

    @Test
    fun loadingIsPassiveAndSixOutputsStartCollapsed() {
        var changes = 0
        val state = androidx.compose.runtime.mutableStateOf<UniversalOutputsValues?>(null)
        var selectedPair: Int? = null
        compose.setContent {
            UniversalOutputsScreen(state.value, conditions, functions, UniversalOutputConditions(),
                { output, enabled -> changes++; state.value = state.value!!.copy(outputs = state.value!!.outputs.toMutableList().also { it[output - 1] = it[output - 1].copy(enabled = enabled) }) },
                { _, _, _ -> changes++ }, { _, _, _ -> changes++ }, { _, _ -> changes++ }, { _, _ -> changes++ }, { _, _, _, _, _ -> changes++ }, { selectedPair = it })
        }
        compose.onNodeWithTag("uniout-loading").assertIsDisplayed()
        compose.runOnIdle { state.value = values(UniOutParamPacket()) }
        compose.onAllNodesWithTag("uniout-loading").assertCountEquals(0)
        (1..6).forEach { compose.onNodeWithTag("output-$it-collapsed").performScrollTo().assertIsDisplayed() }
        assertEquals(0, changes)
        val output1Bottom = compose.onNodeWithTag("output-1-card").fetchSemanticsNode().boundsInRoot.bottom
        val pairFunction = compose.onNodeWithTag("outputs-link-function").performScrollTo().assertIsDisplayed()
        val pairBounds = pairFunction.fetchSemanticsNode().boundsInRoot
        val output2Top = compose.onNodeWithTag("output-2-card").fetchSemanticsNode().boundsInRoot.top
        assertTrue(kotlin.math.abs((pairBounds.top - output1Bottom) - (output2Top - pairBounds.bottom)) < 1f)
        pairFunction.assertIsNotEnabled()
    }

    @Test
    fun pairSelectorEnabledOnlyWhenFirstTwoOutputsAreEnabledAndFifthChoiceUsesCode15() {
        val state = androidx.compose.runtime.mutableStateOf(values(UniOutParamPacket().apply { output1Use = true; output2Use = true }))
        var selectedPair: Int? = null
        compose.setContent { UniversalOutputsScreen(state.value, conditions, functions, UniversalOutputConditions(),
            { output, enabled -> state.value = state.value.copy(outputs = state.value.outputs.toMutableList().also { it[output - 1] = it[output - 1].copy(enabled = enabled) }) },
            { _, _, _ -> }, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, { _, _, _, _, _ -> }, { selectedPair = it }) }
        compose.onNodeWithTag("outputs-link-function").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("AND").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, selectedPair) }
        compose.onNodeWithTag("outputs-link-function").performScrollTo().performClick()
        compose.onNodeWithText("None").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(15, selectedPair) }

    }

    @Test
    fun thresholdCancelAndStaleConfirmationUseCapturedConditionWithoutRewindingOtherFields() {
        val packet = UniOutParamPacket().apply { output1Use = true; output1Condition1 = 13; output1OnThrd1 = 55f; output1OffThrd1 = 50f }
        val control = UniversalOutputsControl(UniversalOutputConditions()) { true }
        control.receive(packet)
        assertTrue(control.setResultInverted(1, true))
        val state = androidx.compose.runtime.mutableStateOf(control.values.value!!)
        val thresholdAttempts = mutableListOf<Triple<Int, Float, Boolean>>()
        fun refresh(accepted: Boolean) { if (accepted) state.value = control.values.value!! }
        compose.setContent { UniversalOutputsScreen(state.value, conditions, functions, UniversalOutputConditions(),
            { output, enabled -> refresh(control.setOutputEnabled(output, enabled)) },
            { output, slot, code -> refresh(control.selectCondition(output, slot, code)) },
            { output, slot, inverted -> refresh(control.setConditionInverted(output, slot, inverted)) },
            { output, inverted -> refresh(control.setResultInverted(output, inverted)) },
            { output, code -> refresh(control.selectLogicFunction(output, if (code == 4) 15 else code)) },
            { output, slot, on, expected, value -> thresholdAttempts += Triple(expected, value, if (on) control.editOnThreshold(output, slot, expected, value) else control.editOffThreshold(output, slot, expected, value)) },
            { refresh(control.selectPairFunction(if (it == 4) 15 else it)) }) }

        compose.onNodeWithTag("output-1-threshold-on-1").performScrollTo().performClick()
        compose.onNodeWithText(targetContext.getString(android.R.string.cancel)).performClick()
        assertTrue(thresholdAttempts.isEmpty())

        compose.onNodeWithTag("output-1-threshold-on-1").performScrollTo().performClick()
        compose.onNodeWithTag("output-1-threshold-on-1-editor").performTextClearance(); compose.onNodeWithTag("output-1-threshold-on-1-editor").performTextInput("60")
        compose.runOnIdle {
            assertTrue(control.selectCondition(1, 1, 14)) // TPS 55 and ATS default 55 display identically.
            state.value = control.values.value!!
        }
        compose.onNodeWithText(targetContext.getString(android.R.string.ok)).performClick()
        compose.runOnIdle {
            assertEquals(listOf(Triple(13, 60f, false)), thresholdAttempts)
            assertEquals(14, control.values.value!!.outputs.first().condition1)
            assertEquals(55f, control.values.value!!.outputs.first().onThreshold1, 0f)
            assertTrue(control.values.value!!.outputs.first().inversion)
        }

        compose.onNodeWithTag("output-1-threshold-on-1").performScrollTo().performClick()
        compose.onNodeWithTag("output-1-threshold-on-1-editor").performTextClearance(); compose.onNodeWithTag("output-1-threshold-on-1-editor").performTextInput("60")
        compose.onNodeWithText(targetContext.getString(android.R.string.ok)).performClick()
        compose.runOnIdle {
            assertEquals(listOf(Triple(13, 60f, false), Triple(14, 60f, true)), thresholdAttempts)
            assertEquals(60f, control.values.value!!.outputs.first().onThreshold1, 0f)
            assertTrue(control.values.value!!.outputs.first().inversion)
        }
    }

    @Test
    fun addI2ThresholdDialogClampsInputToFiveVolts() {
        val initial = UniOutParamPacket().apply { output1Use = true; output1Condition1 = 16 }
        val state = androidx.compose.runtime.mutableStateOf(values(initial))
        val attempts = mutableListOf<Triple<Int, Float, Boolean>>()
        compose.setContent {
            UniversalOutputsScreen(state.value, conditions, functions, UniversalOutputConditions(),
                { _, _ -> }, { _, _, _ -> }, { _, _, _ -> }, { _, _ -> }, { _, _ -> },
                { output, slot, on, expected, value -> attempts += Triple(expected, value, on) }, {})
        }

        compose.onNodeWithTag("output-1-threshold-on-1").performScrollTo().performClick()
        compose.onNodeWithTag("output-1-threshold-on-1-editor").performTextClearance()
        compose.onNodeWithTag("output-1-threshold-on-1-editor").performTextInput("600")
        compose.onNodeWithText(targetContext.getString(android.R.string.ok)).performClick()

        compose.runOnIdle { assertEquals(listOf(Triple(16, 5f, true)), attempts) }
    }

    @Test
    fun activeOutputShowsSelectorsThresholdDialogsAndConditionalSecondCondition() {
        val active = UniOutParamPacket().also { it.output1Use = true; it.output1LogicFunc = 15 }
        val state = androidx.compose.runtime.mutableStateOf(values(active))
        val events = mutableListOf<String>()
        compose.setContent {
            UniversalOutputsScreen(state.value, conditions, functions, UniversalOutputConditions(),
                { output, enabled -> events += "$output:enabled=$enabled"; state.value = state.value!!.copy(outputs = state.value!!.outputs.toMutableList().also { it[output - 1] = it[output - 1].copy(enabled = enabled) }) },
                { output, slot, code -> events += "$output:$slot:condition=$code" }, { _, _, _ -> events += "condition-inversion" },
                { _, _ -> events += "result-inversion" }, { output, code -> events += "$output:logic=$code"; state.value = state.value!!.copy(outputs = state.value!!.outputs.toMutableList().also { it[output - 1] = it[output - 1].copy(logicFunction = code) }) },
                { _, _, _, _, _ -> events += "threshold" }, {})
        }
        compose.onNodeWithTag("output-1-condition-1-picker").assertIsDisplayed()
            .assertHasClickAction()
        compose.onNodeWithText(targetContext.getString(R.string.unioutput1_condition_1_title)).assertIsDisplayed()
        compose.onNodeWithText("Condition 0").assertIsDisplayed()
        compose.onNodeWithTag("output-1-condition-1-group").assertIsDisplayed()
        compose.onNodeWithTag("output-1-threshold-on-1").assertIsDisplayed().assertHasClickAction().performClick()
        val onValueLabel = targetContext.getString(R.string.unioutput1_condition1_on_value_title)
        compose.onNodeWithText(targetContext.getString(R.string.uniout_threshold_edit_title, onValueLabel)).assertIsDisplayed()
        compose.onNodeWithText(targetContext.getString(android.R.string.cancel)).performClick()
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
        compose.onNodeWithTag("output-1-result-inversion").performScrollTo()
        val refreshedCondition2Bounds = compose.onNodeWithTag("output-1-condition-2-group").fetchSemanticsNode().boundsInRoot
        val updatedInversionBounds = compose.onNodeWithTag("output-1-result-inversion").fetchSemanticsNode().boundsInRoot
        assertEquals(16f * compose.density.density, condition2Bounds.top - updatedLogicBounds.bottom, 1f)
        assertEquals(16f * compose.density.density, updatedInversionBounds.top - refreshedCondition2Bounds.bottom, 1f)
        assertEquals(true, events.isNotEmpty())
    }

    @Test
    fun conditionOneExcludesTimerCodesAndUnknownWireIdsRemainVisibleAndSafe() {
        val values = values(UniOutParamPacket().also {
            it.output1Use = true
            it.output1Condition1 = 99
            it.output1LogicFunc = 9
        })
        compose.setContent { UniversalOutputsScreen(values, conditions, functions, UniversalOutputConditions(), { _, _ -> }, { _, _, _ -> }, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, { _, _, _, _, _ -> }, {}) }
        compose.onNodeWithTag("output-1-condition-1-picker").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText(targetContext.getString(R.string.uniout_unknown_code, 99)).assertIsDisplayed()
        compose.onNodeWithText(targetContext.getString(R.string.uniout_threshold_unavailable, targetContext.getString(R.string.unioutput1_condition1_on_value_title))).assertIsDisplayed()
        compose.onAllNodesWithTag("output-1-threshold-on-1").assertCountEquals(0)
        compose.onNodeWithTag("output-1-logical-function").assertHasClickAction()
        compose.onNodeWithText(targetContext.getString(R.string.uniout_unknown_code, 9)).assertIsDisplayed()
        compose.onNodeWithTag("output-1-condition-1-picker").performClick()
        compose.onAllNodesWithText(conditions.getValue(7)).assertCountEquals(0)
        compose.onAllNodesWithText(conditions.getValue(23)).assertCountEquals(0)
    }
    private fun values(packet: UniOutParamPacket): UniversalOutputsValues {
        val control = UniversalOutputsControl(UniversalOutputConditions()) { true }
        control.receive(packet)
        return control.values.value!!
    }
}
