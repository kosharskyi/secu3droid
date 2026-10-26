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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.ChokeValues
import org.secu3.android.ui.compose.SecuComposeTheme

class ChokeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun initialRenderAndRecompositionDoNotSendOrEditAnything() {
        var calls = 0
        val state = mutableStateOf(values(frequency = 8))
        compose.setContent { SecuComposeTheme { ChokeScreen(state.value, {}, { _, _ -> calls++ }, { calls++ }, { calls++ }, { calls++ }, { calls++ }, { calls++ }, { calls++ }) } }
        compose.waitForIdle()
        compose.onNodeWithTag("choke_frequency").assertTextContains("Code 8")
        compose.onNodeWithTag("choke_test").assertExists()
        val decrease = compose.onNodeWithTag("choke_manual_dec").fetchSemanticsNode().boundsInRoot
        val increase = compose.onNodeWithTag("choke_manual_inc").fetchSemanticsNode().boundsInRoot
        val test = compose.onNodeWithTag("choke_test").fetchSemanticsNode().boundsInRoot
        val rootWidth = compose.onRoot().fetchSemanticsNode().boundsInRoot.width
        compose.runOnIdle {
            assertEquals(64f, with(compose.density) { decrease.width.toDp().value }, .5f)
            assertEquals(64f, with(compose.density) { increase.width.toDp().value }, .5f)
            assertTrue("Arrow buttons should have a visible gap", increase.left > decrease.right)
            assertTrue("Test should sit to the right of the arrows", test.left > increase.right)
            assertTrue("Test button should wrap its label", test.width < rootWidth)
        }
        compose.runOnIdle { state.value = state.value.copy() }
        assertEquals(0, calls)
    }

    @Test fun tapHoldSelectorSwitchAndTestAreExplicitCallbacks() {
        var delta = 0
        var releases = 0
        var flushes = 0
        var cancels = 0
        var tests = 0
        val edits = mutableListOf<ChokeNumber>()
        val toggles = mutableListOf<Pair<ChokeNumber, Boolean>>()
        val frequencies = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                ChokeScreen(values(), { edits += it }, { number, checked -> toggles += number to checked },
                    frequencies::add, { delta += it }, { flushes++ }, { releases++ }, { cancels++ }, { tests++ })
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("choke_test").assertIsDisplayed().performClick()
        assertEquals(1, tests)
        ChokeNumber.entries.take(5).forEach { number ->
            compose.onNodeWithTag("choke_${number.name.lowercase()}").performScrollTo().performClick()
        }
        assertEquals(ChokeNumber.entries.take(5), edits)

        compose.onNodeWithTag("choke_manual_dec").performTouchInput { click() }
        assertEquals(-1, delta)
        assertEquals(1, releases)
        assertEquals(0, cancels)

        delta = 0
        val increaseNode = compose.onNodeWithTag("choke_manual_inc")
        increaseNode.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(550L)
        compose.runOnIdle {
            assertTrue("Expected repeat steps before release; got $delta", delta >= 2)
            assertTrue("Expected periodic flush before release; got $flushes", flushes >= 1)
        }
        increaseNode.performTouchInput { up() }
        assertTrue("Expected steps while still held; got $delta", delta >= 2)
        assertTrue("Expected periodic flush while still held; got $flushes", flushes >= 1)
        assertEquals(2, releases)

        compose.onNodeWithTag("choke_manual_dec").performTouchInput { down(center); moveTo(Offset(-10f, -10f)); up() }
        assertEquals(1, cancels)
        assertEquals(2, releases)

        compose.onNodeWithTag("choke_frequency").performScrollTo().performClick()
        compose.onNodeWithTag("choke_frequency_option_3").performClick()
        assertEquals(listOf(3), frequencies)
        listOf(ChokeNumber.CLOSED_LOOP, ChokeNumber.DISABLE_ON_GAS, ChokeNumber.THROTTLE_INIT, ChokeNumber.MAX_FREQUENCY_INIT)
            .forEach { number ->
                compose.onNodeWithTag("choke_${number.name.lowercase()}-switch").performScrollTo().performClick()
            }
        assertEquals(
            listOf(
                ChokeNumber.CLOSED_LOOP to true,
                ChokeNumber.DISABLE_ON_GAS to true,
                ChokeNumber.THROTTLE_INIT to true,
                ChokeNumber.MAX_FREQUENCY_INIT to true,
            ),
            toggles,
        )
        compose.onNodeWithTag("choke_test").assertExists()
        assertEquals(1, cancels)
    }

    @Test fun inactivePageCancelsHeldManualCommandsAndDoesNotResumeThem() {
        val active = mutableStateOf(true)
        var steps = 0
        var flushes = 0
        var cancels = 0
        compose.setContent {
            SecuComposeTheme {
                ChokeScreen(
                    values(), {}, { _, _ -> }, {},
                    { steps++ }, { flushes++ }, {}, { cancels++ }, {}, isActive = active.value,
                )
            }
        }
        val button = compose.onNodeWithTag("choke_manual_inc")
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(550)
        compose.runOnIdle { assertTrue(steps > 1); active.value = false }
        compose.waitForIdle()
        assertTrue(cancels > 0)
        val pausedSteps = steps
        val pausedFlushes = flushes
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle {
            assertEquals(pausedSteps, steps)
            assertEquals(pausedFlushes, flushes)
            active.value = true
        }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle {
            assertEquals(pausedSteps, steps)
            assertEquals(pausedFlushes, flushes)
        }
    }

    @Test fun replacingCallbacksDuringHoldKeepsGestureRunningAndUsesLatestCallbacks() {
        data class Callbacks(
            val step: (Int) -> Unit,
            val flush: () -> Unit,
            val release: () -> Unit,
            val cancel: () -> Unit,
        )
        var originalSteps = 0
        var updatedSteps = 0
        var updatedReleases = 0
        var updatedCancels = 0
        var updatedFlushes = 0
        val callbacks = mutableStateOf(
            Callbacks({ originalSteps++ }, {}, {}, {}),
        )
        compose.setContent {
            val current = callbacks.value
            SecuComposeTheme {
                ChokeScreen(values(), {}, { _, _ -> }, {}, current.step, current.flush,
                    current.release, current.cancel, {})
            }
        }
        val button = compose.onNodeWithTag("choke_manual_inc")
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(250)
        compose.runOnIdle {
            callbacks.value = Callbacks(
                { updatedSteps++ },
                { updatedFlushes++ },
                { updatedReleases++ },
                { updatedCancels++ },
            )
        }
        compose.mainClock.advanceTimeBy(300)
        button.performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(1, originalSteps)
            assertTrue(updatedSteps > 0)
            assertTrue(updatedFlushes > 0)
            assertEquals(1, updatedReleases)
            assertEquals(0, updatedCancels)
        }
    }

    private fun values(frequency: Int = 1) = ChokeValues(
        steps = 500,
        regulatorFactor = .5f,
        coldDuration = 2f,
        hotDuration = 3f,
        crankToRunDuration = 4f,
        closedLoopRegulator = false,
        disableRegulatorOnGas = false,
        throttlePositionAtInit = false,
        maximumFrequencyAtInit = false,
        testing = false,
        frequency = frequency,
    )
}
