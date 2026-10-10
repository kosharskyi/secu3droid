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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Disconnected
import org.secu3.android.db.models.GaugeState
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.utils.Task
import androidx.test.platform.app.InstrumentationRegistry

class SensorsTabsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun tabClickAndSwipeNotifySettledPageOncePerSelection() {
        val pages = mutableListOf<Int>()
        compose.setContent { SecuComposeTheme { screen(page = 0, pageChanged = pages::add) } }
        compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).performClick()
        compose.waitForIdle()
        assertEquals(listOf(1), pages)
        compose.onNodeWithTag("sensors-pager").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(listOf(1, 0), pages)
    }

    @Test fun loggerControlsExposeStartThenMarksAndStop() {
        val started = mutableStateOf(false)
        val starts = mutableListOf<Int>()
        val marks = mutableListOf<Int>()
        val stops = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                screen(loggerStarted = started.value, start = { starts += 1; started.value = true }, stop = { stops += 1; started.value = false }, mark = marks::add)
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

    @Test fun settingsAndConnectionStatusArePresent() {
        val settings = mutableListOf<Int>()
        compose.setContent { SecuComposeTheme { screen(settings = { settings += 1 }) } }
        compose.onNodeWithContentDescription(context.getString(R.string.title_activity_settings)).performClick()
        compose.waitForIdle()
        assertEquals(listOf(1), settings)
        compose.onNodeWithText(context.getString(R.string.dashboard_status_offline)).assertExists()
    }

    @Test fun pageTaskMappingSelectsSensorAndRawTasks() {
        assertEquals(Task.Secu3ReadSensors, taskForPage(0))
        assertEquals(Task.Secu3ReadRawSensors, taskForPage(1))
    }

    @Test fun changingOldViewPreferenceReplacesGaugeScreenWithLegacyScreen() {
        val oldView = mutableStateOf(false)
        compose.setContent { SecuComposeTheme { screen(oldView = oldView.value) } }
        compose.onNodeWithTag("gauges-grid").assertExists()
        compose.runOnIdle { oldView.value = true }
        compose.onNodeWithTag("old-sensors-list").assertExists()
    }

    @Test fun columnPreferenceUpdatesWithoutChangingOldViewSelection() {
        val columns = mutableStateOf(2)
        val gauges = listOf(
            GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "100"),
            GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, true), "12.1"),
            GaugeItem(GaugeState(3, GaugeType.MAP, 2, true), "90"),
        )
        compose.setContent { SecuComposeTheme { screen(columns = columns.value, oldView = false, gauges = gauges) } }
        val twoColumnTop = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top
        val thirdGaugeTop = compose.onNodeWithTag("gauge-MAP").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(thirdGaugeTop > twoColumnTop)
        compose.runOnIdle { columns.value = 3 }
        val threeColumnTop = compose.onNodeWithTag("gauge-MAP").fetchSemanticsNode().boundsInRoot.top
        assertEquals(twoColumnTop, threeColumnTop, 1f)
        compose.onNodeWithTag("gauges-grid").assertExists()
    }

    @Composable private fun screen(
        page: Int = 0,
        columns: Int = 2,
        oldView: Boolean = false,
        gauges: List<GaugeItem> = emptyList(),
        pageChanged: (Int) -> Unit = {},
        loggerStarted: Boolean = false,
        start: () -> Unit = {}, stop: () -> Unit = {}, mark: (Int) -> Unit = {},
        settings: () -> Unit = {},
    ) = SensorsTabsScreen(
        initialPage = page, oldViewEnabled = oldView, loggerEnabled = true, loggerStarted = loggerStarted, columns = columns,
        connectionState = Disconnected, gauges = gauges, indicators = emptyList(), gaugeChoices = null,
        indicatorChoices = null, sensors = null, rawSensors = null, onPageChanged = pageChanged,
        onNavigateUp = {}, onSettings = settings,
        onAddGaugeRequest = {}, onAddIndicatorRequest = {}, onAddGauge = {}, onAddIndicator = {},
        onGaugeChoicesConsumed = {}, onIndicatorChoicesConsumed = {}, onDeleteGauge = {}, onDeleteIndicator = {},
        onUpdateGauge = {}, onReorderGauges = {}, onReorderIndicators = {}, onStartLog = start,
        onStopLog = stop, onMark = mark,
    )
}
