/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.ui.sensors

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Disconnected
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.models.sensors.IndicatorType
import androidx.test.platform.app.InstrumentationRegistry

class SensorsTabsScreenTest {
    private val presentationCatalog = SensorPresentationCatalog()
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun tabClickAndSwipeNotifySettledPageOncePerSelection() {
        val pages = mutableListOf<Int>()
        compose.setContent { SecuComposeTheme { SensorsScreen(page = 0, pageChanged = pages::add) } }
        compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).performClick()
        compose.waitForIdle()
        assertEquals(listOf(1), pages)
        compose.onNodeWithTag("sensors-pager").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(listOf(1, 0), pages)
    }

    @Test fun horizontalCardSwipeChangesPage() {
        val indicators = listOf(IndicatorItem(IndicatorState(20, IndicatorType.THROTTLE, 0), true))
        val pages = mutableListOf<Int>()
        compose.setContent { SecuComposeTheme { SensorsScreen(indicators = indicators, pageChanged = pages::add) } }
        compose.onNodeWithTag("indicator-THROTTLE").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(listOf(1), pages)
    }

    @Test fun verticalCardSwipeScrollsList() {
        val gauges = GaugeType.entries.take(10).mapIndexed { index, type ->
            GaugeItem(GaugeState(index + 1, type, index, true), "${100 + index}")
        }
        val pages = mutableListOf<Int>()
        val reorderStates = mutableListOf<Boolean>()
        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.width(320.dp).height(480.dp)) {
                    SensorsScreen(gauges = gauges, pageChanged = pages::add, reorderingChanged = reorderStates::add)
                }
            }
        }
        val rpmTop = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("gauge-RPM").performTouchInput { swipeUp() }
        compose.waitForIdle()
        org.junit.Assert.assertTrue(compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top < rpmTop)
        assertEquals(emptyList<Int>(), pages)
        assertEquals(emptyList<Boolean>(), reorderStates)
    }

    @Test fun longPressReordersGaugeAndIndicatorAndGesturesWorkAfterward() {
        // Compact numeric cards keep gauge and indicator targets in the tablet viewport; dial sizing has its own test.
        val gauges = listOf(
            GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "100"),
            GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, true), "12"),
            GaugeItem(GaugeState(5, GaugeType.MAP, 2, true), "90"),
        )
        val indicators = listOf(
            IndicatorItem(IndicatorState(3, IndicatorType.THROTTLE, 0), true),
            IndicatorItem(IndicatorState(4, IndicatorType.GAS_VALVE, 1), false),
        )
        var gaugeOrder: List<GaugeState>? = null
        var indicatorOrder: List<IndicatorState>? = null
        val pages = mutableListOf<Int>()
        val reorderStates = mutableListOf<Boolean>()
        compose.setContent {
            SecuComposeTheme {
                SensorsScreen(gauges = gauges, indicators = indicators, pageChanged = pages::add,
                    reorderGauges = { gaugeOrder = it }, reorderIndicators = { indicatorOrder = it },
                    reorderingChanged = reorderStates::add)
            }
        }
        compose.onNodeWithTag("gauge-RPM").assertExists()
        dragCard("gauge-RPM", "gauge-MAP")
        assertEquals(listOf(GaugeType.VOLTAGE, GaugeType.MAP, GaugeType.RPM), gaugeOrder?.map { it.gaugeType })
        assertEquals(listOf(true, false), reorderStates)
        assertEquals(emptyList<Int>(), pages)
        dragCard("indicator-THROTTLE", "indicator-GAS_VALVE")
        assertEquals(listOf(IndicatorType.GAS_VALVE, IndicatorType.THROTTLE), indicatorOrder?.map { it.indicatorType })
        assertEquals(listOf(true, false, true, false), reorderStates)
        assertEquals(emptyList<Int>(), pages)
        compose.onNodeWithTag("sensors-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(listOf(1), pages)
    }

    private fun dragCard(sourceTag: String, targetTag: String) {
        val source = compose.onNodeWithTag(sourceTag).fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithTag(targetTag).fetchSemanticsNode().boundsInRoot
        assertPlaced(sourceTag, source)
        assertPlaced(targetTag, target)
        compose.onNodeWithTag(sourceTag).performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(target.center.x - source.center.x, target.center.y - source.center.y))
            up()
        }
        compose.waitForIdle()
    }

    private fun assertPlaced(tag: String, bounds: androidx.compose.ui.geometry.Rect) {
        org.junit.Assert.assertTrue("$tag must be placed before the drag; got $bounds", bounds.width > 0f && bounds.height > 0f)
    }

    @Test fun loggerControlsExposeStartThenMarksAndStop() {
        val started = mutableStateOf(false)
        val starts = mutableListOf<Int>()
        val marks = mutableListOf<Int>()
        val stops = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                SensorsScreen(loggerStarted = started.value, start = { starts += 1; started.value = true }, stop = { stops += 1; started.value = false }, mark = marks::add)
            }
        }
        compose.onNodeWithTag("log-start").performClick()
        compose.onNodeWithTag("log-mark-1").performClick()
        compose.onNodeWithTag("log-mark-2").performClick()
        compose.onNodeWithTag("log-mark-3").performClick()
        compose.onNodeWithTag("log-stop").performClick()
        compose.waitForIdle()
        assertEquals(listOf(1), starts)
        assertEquals(listOf(1, 2, 3), marks)
        assertEquals(listOf(1), stops)
        compose.onNodeWithTag("log-start").assertExists()
    }

    @Test fun asynchronousLoggerFailureRestoresStartControlAndShowsOneSnackbar() {
        val started = mutableStateOf(false)
        val errorId = mutableStateOf(0L)
        val consumedErrors = mutableListOf<Long>()
        compose.setContent {
            SecuComposeTheme {
                SensorsScreen(
                    loggerStarted = started.value,
                    loggerErrorId = errorId.value,
                    start = { started.value = true },
                    onLoggerErrorConsumed = { consumedErrors += it; errorId.value = 0L },
                )
            }
        }
        compose.onNodeWithTag("log-start").performClick()
        compose.onNodeWithTag("log-stop").assertExists()

        compose.runOnIdle {
            started.value = false
            errorId.value = 1
        }
        compose.onNodeWithTag("log-start").assertExists()
        compose.onNodeWithText(context.getString(R.string.sensor_logger_io_error)).assertExists()
        compose.waitUntil(6_000) { consumedErrors == listOf(1L) }
        compose.runOnIdle { started.value = false }
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.sensor_logger_io_error)).assertDoesNotExist()
        assertEquals(listOf(1L), consumedErrors)
    }

    @Test fun settingsAndConnectionStatusArePresent() {
        val settings = mutableListOf<Int>()
        compose.setContent { SecuComposeTheme { SensorsScreen(settings = { settings += 1 }) } }
        compose.onNodeWithContentDescription(context.getString(R.string.title_activity_settings)).performClick()
        compose.waitForIdle()
        assertEquals(listOf(1), settings)
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_disconnected)).assertExists()
    }

    @Test fun disconnectedScreenKeepsConfigurationButMarksLiveReadingsUnavailable() {
        val gauges = listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "2240"))
        val indicators = listOf(IndicatorItem(IndicatorState(2, IndicatorType.THROTTLE, 0), false))
        compose.setContent { SecuComposeTheme { SensorsScreen(gauges = gauges, indicators = indicators) } }

        compose.onNodeWithTag("gauge-RPM").assertExists()
        compose.onNodeWithText("—").assertExists()
        compose.onNodeWithText("2240").assertDoesNotExist()
        compose.onNodeWithTag("indicator-THROTTLE")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, context.getString(R.string.sensors_status_accessibility_unavailable)))
        compose.onNodeWithText(context.getString(R.string.sensors_status_accessibility_unavailable)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.sensors_status_accessibility_off)).assertDoesNotExist()
    }

    @Test fun changingOldViewPreferenceReplacesGaugeScreenWithLegacyScreen() {
        val oldView = mutableStateOf(false)
        compose.setContent { SecuComposeTheme { SensorsScreen(oldView = oldView.value) } }
        compose.onNodeWithTag("gauges-grid").assertExists()
        compose.runOnIdle { oldView.value = true }
        compose.onNodeWithTag("old-sensors-list").assertExists()
    }

    @Test fun landscapeCutoutInsetsApplyToPagesButNotTabsWhileNavigationInsetsRemain() {
        val density = compose.density
        val navigationLeft = with(density) { 16.dp.roundToPx() }
        val navigationRight = with(density) { 20.dp.roundToPx() }
        val navigationTop = with(density) { 24.dp.roundToPx() }
        val navigationBottom = with(density) { 28.dp.roundToPx() }
        val cutoutLeft = with(density) { 80.dp.roundToPx() }
        val cutoutRight = with(density) { 56.dp.roundToPx() }
        val safeTop = with(density) { 36.dp.roundToPx() }
        val safeBottom = with(density) { 44.dp.roundToPx() }
        val safeDrawingInsets = WindowInsets(left = cutoutLeft, top = safeTop, right = cutoutRight, bottom = safeBottom)
        val systemBarsInsets = WindowInsets(left = navigationLeft, top = navigationTop, right = navigationRight, bottom = navigationBottom)

        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.width(800.dp).height(700.dp).testTag("inset-host")) {
                    SensorsScreen(safeDrawingInsets = safeDrawingInsets, systemBarsInsets = systemBarsInsets)
                }
            }
        }

        val host = compose.onNodeWithTag("inset-host").getUnclippedBoundsInRoot()
        val tabs = compose.onNodeWithTag("sensors-tabs").getUnclippedBoundsInRoot()
        val pager = compose.onNodeWithTag("sensors-pager").getUnclippedBoundsInRoot()
        val loggerFab = compose.onNodeWithTag("log-start").getUnclippedBoundsInRoot()
        assertEquals(host.left.value + navigationLeft / density.density, tabs.left.value, 1f)
        assertEquals(host.right.value - navigationRight / density.density, tabs.right.value, 1f)
        assertEquals(host.left.value + cutoutLeft / density.density, pager.left.value, 1f)
        assertEquals(host.right.value - cutoutRight / density.density, pager.right.value, 1f)
        org.junit.Assert.assertTrue("logger FAB stays clear of the cutout and navigation bar", loggerFab.left.value >= host.left.value + cutoutLeft / density.density)
        org.junit.Assert.assertTrue("vertical safe inset remains above the tabs", tabs.top.value >= host.top.value + safeTop / density.density)
        org.junit.Assert.assertTrue("page begins below, not beneath, the tabs", pager.top >= tabs.bottom)
        org.junit.Assert.assertTrue("vertical safe inset remains below the page", pager.bottom.value <= host.bottom.value - safeBottom / density.density)
    }

    @Composable private fun SensorsScreen(
        page: Int = 0,
        oldView: Boolean = false,
        gauges: List<GaugeItem> = emptyList(),
        indicators: List<IndicatorItem> = emptyList(),
        pageChanged: (Int) -> Unit = {},
        reorderGauges: (List<GaugeState>) -> Unit = {},
        reorderIndicators: (List<IndicatorState>) -> Unit = {},
        reorderingChanged: (Boolean) -> Unit = {},
        loggerStarted: Boolean = false,
        loggerErrorId: Long = 0,
        onLoggerErrorConsumed: (Long) -> Unit = {},
        start: () -> Unit = {}, stop: () -> Unit = {}, mark: (Int) -> Unit = {},
        settings: () -> Unit = {},
        safeDrawingInsets: WindowInsets = WindowInsets.safeDrawing,
        systemBarsInsets: WindowInsets = WindowInsets.systemBars,
    ) = SensorsTabsScreen(
        initialPage = page, oldViewEnabled = oldView, loggerEnabled = true, loggerStarted = loggerStarted,
        loggerErrorId = loggerErrorId, snackbarHostState = remember { SnackbarHostState() },
        connectionState = Disconnected, gauges = gauges, indicators = indicators, gaugeChoices = null,
        indicatorChoices = null, sensors = null, rawSensors = null, firmwareInfo = null,
        injectionTimingMode = org.secu3.android.models.InjectionTimingMode.Default,
        presentationCatalog = presentationCatalog,
        onPageChanged = pageChanged,
        onNavigateUp = {}, onSettings = settings,
        onAddGaugeRequest = {}, onAddIndicatorRequest = {}, onAddGauge = {}, onAddIndicator = {},
        onGaugeChoicesConsumed = {}, onIndicatorChoicesConsumed = {}, onDeleteGauge = {}, onDeleteIndicator = {},
        onUpdateGauge = {}, onReorderGauges = reorderGauges, onReorderIndicators = reorderIndicators, onStartLog = start,
        onReorderingChanged = reorderingChanged,
        onStopLog = stop, onMark = mark,
        onLoggerErrorConsumed = onLoggerErrorConsumed,
        gaugeStyle = org.secu3.android.models.SensorGaugeStyle.Current,
        safeDrawingInsets = safeDrawingInsets,
        systemBarsInsets = systemBarsInsets,
    )
}
