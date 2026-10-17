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
 */
package org.secu3.android.ui.parameters

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.ui.compose.SecuComposeTheme

@RunWith(AndroidJUnit4::class)
class ParamsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun saveIsDisabledOfflineAndEnabledAgainAfterReconnect() {
        val connection = mutableStateOf<org.secu3.android.connection.ConnectionState?>(Disconnected)
        var saves = 0
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter"), connectionState = connection.value,
                    onBack = {}, onSave = { saves++ }, onPageSettled = {},
                    pageContent = { _, _, _ -> Box(Modifier.fillMaxSize()) },
                )
            }
        }
        val save = compose.onNodeWithContentDescription(context.getString(R.string.save))
        compose.onNodeWithTag("parameters-unavailable").assertIsDisplayed()
        save.assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, saves); connection.value = Connected }
        compose.onNodeWithTag("parameters-unavailable").assertDoesNotExist()
        save.assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, saves); connection.value = Disconnected }
        compose.onNodeWithTag("parameters-unavailable").assertIsDisplayed()
        save.assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, saves); connection.value = Connected }
        save.assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, saves) }
    }

    @Test fun failedReadHasAnExplicitRetryAction() {
        var retries = 0
        compose.setContent {
            SecuComposeTheme {
                org.secu3.android.ui.parameters.compose.ParameterReadError { retries++ }
            }
        }
        compose.onNodeWithText(context.getString(R.string.parameter_read_failed)).assertIsDisplayed()
        compose.onNodeWithTag("parameter-read-retry").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun newConnectionSessionReloadsTheSelectedPageEvenWhenStatusRemainsConnected() {
        val session = mutableStateOf(0)
        val settled = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles"), connectionState = Connected,
                    connectionSession = session.value,
                    onBack = {}, onSave = {}, onPageSettled = settled::add,
                    pageContent = { _, _, _ -> Box(Modifier.fillMaxSize()) },
                )
            }
        }
        compose.waitUntil(5_000) { settled == listOf(0) }
        compose.onNodeWithText("Angles").performClick()
        compose.waitUntil(5_000) { settled == listOf(0, 1) }
        compose.runOnIdle { session.value++ }
        compose.waitUntil(5_000) { settled == listOf(0, 1, 1) }
        compose.onNodeWithText("Angles").assertIsSelected()
    }

    @Test fun settledVisiblePageAloneActivatesAndToolbarWorks() {
        var backCalls = 0
        var saveCalls = 0
        val settled = mutableListOf<Int>()
        var unsettled = 0
        val composed = mutableSetOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles", "Idling"),
                    pageKeys = listOf(101, 102, 103),
                    connectionState = Connected,
                    onBack = { backCalls++ },
                    onSave = { saveCalls++ },
                    onPageSettled = settled::add,
                    onPageUnsettled = { unsettled++ },
                    pageContent = { index, _, _ ->
                        SideEffect { composed += index }
                        Box(Modifier.fillMaxSize())
                    },
                )
            }
        }
        compose.waitUntil(5_000) { settled.isNotEmpty() }
        compose.runOnIdle {
            assertEquals(listOf(0), settled)
            assertTrue(composed.contains(1)) // pager neighbor is precomposed but never activated
        }
        compose.onNodeWithText("Idling").performClick()
        compose.waitUntil(5_000) { settled.lastOrNull() == 2 }
        compose.runOnIdle {
            assertEquals(listOf(0, 2), settled) // intermediate page was never settled/read-active
            assertTrue(unsettled > 0)
        }
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.save)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected)).assertExists()
        compose.runOnIdle { assertEquals(1, backCalls); assertEquals(1, saveCalls) }
    }

    @Test fun activationStopsAndRestartsAcrossHostPause() {
        val owner = TestLifecycleOwner()
        compose.runOnIdle { owner.resume() }
        val settled = mutableListOf<Int>()
        var unsettled = 0
        var activeNow: (() -> Boolean)? = null
        val renderedActive = mutableListOf<Boolean>()
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                SecuComposeTheme {
                    ParametersScreen(
                        titles = listOf("Starter", "Angles"), connectionState = Connected,
                        onBack = {}, onSave = {}, onPageSettled = settled::add,
                        onPageUnsettled = { unsettled++ },
                        pageContent = { index, active, checker ->
                            SideEffect {
                                if (index == 0) activeNow = checker
                                if (index == 0) renderedActive += active
                            }
                            Box(Modifier.fillMaxSize())
                        },
                    )
                }
            }
        }
        compose.waitUntil(5_000) { settled.size == 1 }
        compose.runOnIdle {
            assertTrue(activeNow?.invoke() == true)
            owner.pause()
            assertEquals(false, activeNow?.invoke())
        }
        compose.waitUntil(5_000) { unsettled > 0 }
        compose.waitUntil(5_000) { renderedActive.lastOrNull() == false }
        compose.runOnIdle {
            owner.resume()
            assertEquals(true, activeNow?.invoke())
        }
        compose.waitUntil(5_000) { settled.size == 2 }
        compose.waitUntil(5_000) { renderedActive.lastOrNull() == true }
        compose.runOnIdle { assertEquals(listOf(0, 0), settled) }
    }

    @Test fun swipeSettlesOnlyTheNewPage() {
        val settled = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles", "Idling", "Functions"),
                    connectionState = Connected, onBack = {}, onSave = {}, onPageSettled = settled::add,
                    pageContent = { _, _, _ -> Box(Modifier.fillMaxSize()) },
                )
            }
        }
        compose.waitUntil(5_000) { settled.isNotEmpty() }
        compose.onNodeWithTag("params-pager").performTouchInput { swipeLeft() }
        compose.waitUntil(5_000) { settled.lastOrNull() == 1 }
        compose.runOnIdle { assertEquals(listOf(0, 1), settled) }
    }

    @Test fun tabsTrackHeldAndReversedSwipeBeforeSettledPageActivates() {
        val settled = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles", "Idling"),
                    connectionState = Connected,
                    onBack = {}, onSave = {}, onPageSettled = settled::add,
                    pageContent = { _, _, _ -> Box(Modifier.fillMaxSize()) },
                )
            }
        }
        compose.waitUntil(5_000) { settled == listOf(0) }
        val pager = compose.onNodeWithTag("params-pager")
        val width = pager.fetchSemanticsNode().boundsInRoot.width
        val height = pager.fetchSemanticsNode().boundsInRoot.height
        val start = Offset(width * 0.85f, height / 2f)

        // Keep the pointer down after crossing the halfway point: selection should move,
        // while page activation must still wait for the pager to settle.
        pager.performTouchInput {
            down(start)
            moveBy(Offset(-width * 0.66f, 0f))
            advanceEventTime(250)
        }
        compose.waitForIdle()
        compose.onNodeWithText("Angles").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(0), settled) }

        pager.performTouchInput { moveBy(Offset(width * 0.45f, 0f)) }
        compose.waitForIdle()
        compose.onNodeWithText("Starter").assertIsSelected()
        pager.performTouchInput { advanceEventTime(250); up() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(0), settled) }

        pager.performTouchInput {
            down(start)
            moveBy(Offset(-width * 0.66f, 0f))
            advanceEventTime(250)
        }
        compose.waitForIdle()
        compose.onNodeWithText("Angles").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(0), settled) }
        pager.performTouchInput { up() }
        compose.waitUntil(5_000) { settled == listOf(0, 1) }
        compose.runOnIdle { assertEquals(listOf(0, 1), settled) }
    }

    @Test fun newlyOffscreenFormsWaitForIdleButVisibleAndJumpTargetPagesRenderImmediately() {
        val settled = mutableListOf<Int>()
        val rendered = mutableSetOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles", "Idling", "Temperature", "ADC", "Functions"),
                    connectionState = Connected,
                    onBack = {}, onSave = {}, onPageSettled = settled::add,
                    pageContent = { index, _, _ ->
                        SideEffect { rendered += index }
                        Box(Modifier.fillMaxSize().testTag("parameter-page-$index"))
                    },
                )
            }
        }
        compose.waitUntil(5_000) { settled == listOf(0) }
        compose.runOnIdle {
            assertTrue(rendered.containsAll(listOf(0, 1))) // settled page and pager neighbor are ready
            assertTrue(2 !in rendered && 5 !in rendered) // farther full forms wait outside the gesture path
        }

        val pager = compose.onNodeWithTag("params-pager")
        val width = pager.fetchSemanticsNode().boundsInRoot.width
        val height = pager.fetchSemanticsNode().boundsInRoot.height
        val start = Offset(width * 0.85f, height / 2f)
        pager.performTouchInput {
            down(start)
            moveBy(Offset(-width * 0.66f, 0f))
            advanceEventTime(250)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("parameter-page-0").assertExists()
        compose.onNodeWithTag("parameter-page-1").assertExists()
        compose.runOnIdle {
            assertEquals(listOf(0), settled)
            assertTrue(2 !in rendered)
        }
        pager.performTouchInput { moveBy(Offset(width * 0.45f, 0f)); advanceEventTime(250); up() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(0), settled) }

        pager.performTouchInput { swipeLeft() }
        compose.waitUntil(5_000) { settled == listOf(0, 1) }
        compose.runOnIdle { assertTrue(2 in rendered) } // the next form warms after the swipe settles

        // Reveal a far tab first, then verify that motion never leaves the page viewport blank.
        compose.onNodeWithTag("parameters-tabs").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Functions").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Functions").performClick()
        val pagerBounds = pager.fetchSemanticsNode().boundsInRoot
        var movingFrames = 0
        for (frame in 0 until 90) {
            compose.mainClock.advanceTimeBy(32)
            compose.waitForIdle()
            var bodyWidth = 0f
            var pageIsMoving = false
            (0..5).forEach { index ->
                val bounds = runCatching { compose.onNodeWithTag("parameter-page-$index").fetchSemanticsNode().boundsInRoot }.getOrNull()
                if (bounds != null) {
                    bodyWidth += (minOf(bounds.right, pagerBounds.right) - maxOf(bounds.left, pagerBounds.left)).coerceAtLeast(0f)
                    if (bounds.right > pagerBounds.left && bounds.left < pagerBounds.right && kotlin.math.abs(bounds.left - pagerBounds.left) > 2f) pageIsMoving = true
                }
            }
            if (pageIsMoving) {
                movingFrames++
                assertTrue("Visible page bodies left a blank gap while jumping: $bodyWidth/${pagerBounds.width}", bodyWidth >= pagerBounds.width - 2f)
            }
            if (settled.lastOrNull() == 5) break
        }
        assertTrue("Expected to sample the pager while moving", movingFrames > 0)
        compose.mainClock.advanceTimeBy(5_000)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, settled.first()); assertEquals(5, settled.last()) }
        compose.onNodeWithTag("parameter-page-5").assertExists()
    }

    @Test fun boundaryOverscrollAndVerticalScrollDoNotRestartPageReadSession() {
        val settled = mutableListOf<Int>()
        var unsettled = 0
        var scrollState: androidx.compose.foundation.ScrollState? = null
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles", "Idling"),
                    connectionState = Connected,
                    onBack = {}, onSave = {}, onPageSettled = settled::add,
                    onPageUnsettled = { unsettled++ },
                    pageContent = { index, _, _ ->
                        if (index == 0) {
                            val pageScrollState = rememberScrollState()
                            SideEffect { scrollState = pageScrollState }
                            Column(Modifier.fillMaxSize().verticalScroll(pageScrollState)) {
                                repeat(30) { Spacer(Modifier.height(40.dp)) }
                            }
                        } else Box(Modifier.fillMaxSize())
                    },
                )
            }
        }
        compose.waitUntil(5_000) { settled == listOf(0) }
        val pager = compose.onNodeWithTag("params-pager")

        pager.performTouchInput { swipeRight() } // overscroll before the first page
        repeat(6) { pager.performTouchInput { swipeUp() } }
        compose.waitUntil(5_000) { (scrollState?.maxValue ?: 0) > 0 && scrollState?.value == scrollState?.maxValue }
        pager.performTouchInput { swipeUp() } // overscroll after reaching the vertical content's bottom
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(0), settled); assertEquals(0, unsettled) }

        compose.onNodeWithText("Idling").performClick()
        compose.waitUntil(5_000) { settled == listOf(0, 2) }
        pager.performTouchInput { swipeLeft() } // overscroll after the last page
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(0, 2), settled); assertEquals(1, unsettled) }

        compose.onNodeWithText("Starter").performClick()
        compose.waitUntil(5_000) { settled == listOf(0, 2, 0) }
        compose.runOnIdle { assertEquals(2, unsettled) }
    }

    @Test fun connectionChangesActivateOnlyVisiblePageAndReconnect() {
        val connectionState = mutableStateOf<org.secu3.android.connection.ConnectionState?>(null)
        val settled = mutableListOf<Int>()
        var unsettled = 0
        compose.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles", "Idling"), connectionState = connectionState.value,
                    onBack = {}, onSave = {}, onPageSettled = settled::add,
                    onPageUnsettled = { unsettled++ },
                    pageContent = { _, _, _ -> Box(Modifier.fillMaxSize()) },
                )
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(settled.isEmpty()) }
        compose.runOnIdle { connectionState.value = Connected }
        compose.waitUntil(5_000) { settled.size == 1 }
        compose.runOnIdle {
            assertEquals(listOf(0), settled)
            connectionState.value = Disconnected
        }
        compose.waitUntil(5_000) { unsettled > 0 }
        compose.runOnIdle { connectionState.value = Connected }
        compose.waitUntil(5_000) { settled.size == 2 }
        compose.runOnIdle { assertEquals(listOf(0, 0), settled) }
    }

    @Test fun pagerSelectionRestores() {
        val restoration = StateRestorationTester(compose)
        val settled = mutableListOf<Int>()
        restoration.setContent {
            SecuComposeTheme {
                ParametersScreen(
                    titles = listOf("Starter", "Angles", "Idling"), connectionState = Connected,
                    onBack = {}, onSave = {}, onPageSettled = settled::add,
                    pageContent = { _, _, _ -> Box(Modifier.fillMaxSize()) },
                )
            }
        }
        compose.waitUntil(5_000) { settled.isNotEmpty() }
        compose.onNodeWithText("Angles").performClick()
        compose.waitUntil(5_000) { settled.lastOrNull() == 1 }
        compose.runOnIdle { settled.clear() }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(5_000) { settled.lastOrNull() == 1 }
        compose.onNodeWithText("Angles").assertIsSelected()
        compose.runOnIdle { assertTrue(settled.all { it == 1 }) }
    }

    @Test fun longLocalizedParameterTabScrollsIntoViewAndRemainsReadableOnNarrowScreen() {
        val titles = listOf(
            "Запуск", "КВЗ", "Регулятор ХХ", "Функції", "Температура", "Відсічення палива",
            "Корекція АЦП", "ДПКВ", "Детонація", "Інше", "Управління ПЗ", "Безпека",
            "Універсальні виходи",
        )
        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.width(320.dp)) {
                    ParametersScreen(
                        titles = titles,
                        connectionState = Connected,
                        onBack = {}, onSave = {}, onPageSettled = {},
                        pageContent = { _, _, _ -> Box(Modifier.fillMaxSize()) },
                    )
                }
            }
        }
        val tabs = compose.onNodeWithTag("parameters-tabs")
        repeat(4) { tabs.performTouchInput { swipeLeft() } }
        compose.onNodeWithText(titles.last()).performClick()
        compose.onNodeWithText(titles.last()).assertIsDisplayed().assertIsSelected()
        val tabBounds = compose.onNodeWithText(titles.last()).fetchSemanticsNode().boundsInRoot
        assertTrue("Selected Ukrainian tab starts within the viewport", tabBounds.left >= 0f)
        assertTrue("Selected Ukrainian tab stays inside the narrow viewport", tabBounds.right <= 320f * compose.density.density)
        compose.onNodeWithTag("params-pager").assertIsDisplayed()
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun resume() {
            if (registry.currentState == Lifecycle.State.INITIALIZED) registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            if (registry.currentState < Lifecycle.State.STARTED) registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            if (registry.currentState < Lifecycle.State.RESUMED) registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        fun pause() {
            if (registry.currentState == Lifecycle.State.RESUMED) registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            if (registry.currentState == Lifecycle.State.STARTED) registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }
}
