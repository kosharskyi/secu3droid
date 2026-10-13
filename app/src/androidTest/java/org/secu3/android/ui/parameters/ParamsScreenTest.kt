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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
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

    @Test fun settledVisiblePageAloneActivatesAndToolbarWorks() {
        var backCalls = 0
        var saveCalls = 0
        val settled = mutableListOf<Int>()
        var unsettled = 0
        val composed = mutableSetOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                ParamsScreen(
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
                    ParamsScreen(
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
                ParamsScreen(
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

    @Test fun connectionChangesActivateOnlyVisiblePageAndReconnect() {
        val connectionState = mutableStateOf<org.secu3.android.connection.ConnectionState?>(null)
        val settled = mutableListOf<Int>()
        var unsettled = 0
        compose.setContent {
            SecuComposeTheme {
                ParamsScreen(
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
                ParamsScreen(
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
