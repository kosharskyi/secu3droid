/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.ui.dashboard

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress

class DashboardContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var contentInstalled = false
    private lateinit var dataState: MutableState<DashboardViewData?>
    private lateinit var connectionState: MutableState<ConnectionState?>

    @Before
    fun enterLandscapeHost() {
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        while (SystemClock.elapsedRealtime() < timeoutAt) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) return
            SystemClock.sleep(50L)
        }
        error("Compose test host did not reach landscape orientation")
    }

    @After
    fun restoreHostOrientation() {
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @Test
    fun displaysPrimaryValuesUnitsAndSecondaryReadoutsInLandscape() {
        showDashboard(data = sampleData(), state = Connected)

        listOf("2250", context.getString(R.string.units_rpm)).forEach {
            composeRule.onNodeWithText(it, substring = true).assertIsDisplayed()
        }
        composeRule.onNodeWithText("84", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.units_km_h)).assertIsDisplayed()
        composeRule.onNodeWithText("CLT").assertIsDisplayed()
        composeRule.onNodeWithText("87").assertIsDisplayed()
        composeRule.onAllNodesWithText("°C").assertCountEquals(2)
    }

    @Test
    fun rendersEachConnectionState() {
        listOf<Pair<ConnectionState, Int>>(
            Connected to R.string.dashboard_status_online,
            InProgress to R.string.dashboard_status_connecting,
            Disconnected to R.string.dashboard_status_offline,
        ).forEach { (state, label) ->
            showDashboard(data = sampleData(), state = state)
            composeRule.onNodeWithText(context.getString(label)).assertIsDisplayed()
        }
    }

    @Test
    fun reportsWarningThresholdsAndDoesNotShowWarningsBelowThem() {
        showDashboard(data = sampleData(temperature = 105f, voltage = 11.7f), state = Connected)
        composeRule.onNodeWithText(context.getString(R.string.dashboard_warning_overheat)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dashboard_warning_low_voltage)).assertIsDisplayed()

        showDashboard(data = sampleData(temperature = 99f, voltage = 12.5f), state = Connected)
        composeRule.onAllNodesWithText(context.getString(R.string.dashboard_warning_overheat)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.dashboard_warning_hot)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.dashboard_warning_low_voltage)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.dashboard_warning_voltage)).assertCountEquals(0)
    }

    @Test
    fun missingPacketShowsNoDataZerosAndUnavailableOptionalReadout() {
        showDashboard(data = null, state = null)

        composeRule.onNodeWithText(context.getString(R.string.dashboard_status_no_data)).assertIsDisplayed()
        composeRule.onAllNodesWithText("0", useUnmergedTree = true).assertCountEquals(2)
        composeRule.onNodeWithText("Aux").assertIsDisplayed()
        composeRule.onAllNodesWithText("--").assertCountEquals(5)
    }

    private fun showDashboard(data: DashboardViewData?, state: ConnectionState?) {
        if (contentInstalled) {
            composeRule.runOnIdle {
                dataState.value = data
                connectionState.value = state
            }
        } else {
            dataState = mutableStateOf(data)
            connectionState = mutableStateOf(state)
            composeRule.setContent {
                DashboardContent(data = dataState.value, connectionState = connectionState.value)
            }
            contentInstalled = true
        }
        composeRule.waitForIdle()
    }

    private fun sampleData(temperature: Float = 87f, voltage: Float = 13.8f) = DashboardViewData(
        rpm = 2250f,
        speed = 84f,
        load = 42f,
        map = 62f,
        tps = 18f,
        temperature = temperature,
        voltage = voltage,
        currentAngle = 12f,
        afr = 14.7f,
        oilTemperature = 92f,
        fuelLevel = 36f,
        intakeAirTemperature = 28f,
        knockRetard = 0f,
        ledCheckEngine = false,
        ledGasoline = true,
        ledEco = false,
        ledPower = false,
        ledChoke = false,
        ledFan = false,
        ledRevLimit = false,
    )
}
