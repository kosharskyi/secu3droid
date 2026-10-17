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

import android.graphics.Bitmap
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
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
    private lateinit var narrowLayoutState: MutableState<Boolean>

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
        captureDesignScreenshot("design-dashboard-light.png")
        assertGaugeGutterMatchesDashboardBackground()

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
    fun loadReadoutDoesNotAssumePercentAndMapAndTpsKeepTheirUnits() {
        showDashboard(
            data = sampleData(load = 148f, map = 148f, tps = 55f, oilTemperature = 0f, afr = 0f, fuelLevel = 0f),
            state = Connected,
        )

        composeRule.onNodeWithTag("dashboard-readout-load-value").assertTextEquals("148")
        composeRule.onNodeWithTag("dashboard-readout-load-unit").assertTextEquals("")
        composeRule.onNodeWithTag("dashboard-readout-optional-label").assertTextEquals(context.getString(R.string.dashboard_readout_map))
        composeRule.onNodeWithTag("dashboard-readout-optional-value").assertTextEquals("148")
        composeRule.onNodeWithTag("dashboard-readout-optional-unit").assertTextEquals(context.getString(R.string.units_pressure_kpa))
        composeRule.onNodeWithTag("dashboard-readout-tps-unit").assertTextEquals(context.getString(R.string.units_percents))
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
    fun missingPacketShowsNoDataPlaceholdersAndUnavailableOptionalReadout() {
        showDashboard(data = null, state = null)

        composeRule.onNodeWithText(context.getString(R.string.dashboard_status_no_data)).assertIsDisplayed()
        composeRule.onAllNodesWithText("—", useUnmergedTree = true).assertCountEquals(2)
        composeRule.onNodeWithText("Aux").assertIsDisplayed()
        composeRule.onAllNodesWithText("--").assertCountEquals(5)
    }

    @Test
    fun validZeroPrimaryValuesRemainVisible() {
        showDashboard(data = sampleData().copy(rpm = 0f, speed = 0f), state = Connected)

        composeRule.onAllNodesWithText("0", useUnmergedTree = true).assertCountEquals(2)
        composeRule.onAllNodesWithText("—", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun disconnectedConnectionHidesPreviouslyReceivedValues() {
        showDashboard(data = sampleData(), state = Disconnected)

        composeRule.onNodeWithText(context.getString(R.string.dashboard_status_offline)).assertIsDisplayed()
        composeRule.onAllNodesWithText("—", useUnmergedTree = true).assertCountEquals(2)
        composeRule.onAllNodesWithText("2250", substring = true).assertCountEquals(0)
    }

    @Test
    fun dashboardRendersWithDarkThemeAndLargeFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                org.secu3.android.ui.compose.SecuComposeTheme(darkTheme = true) {
                    DashboardContent(data = sampleData(), connectionState = Connected)
                }
            }
        }
        composeRule.onNodeWithText("2250", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("CLT").assertIsDisplayed()
        composeRule.onRoot().captureToImage()
        assertGaugeGutterMatchesDashboardBackground()
        captureDesignScreenshot("design-dashboard-dark.png")
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val valueBounds = composeRule.onNodeWithText("2250", substring = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("scaled dashboard value should stay within the viewport", valueBounds.left >= rootBounds.left && valueBounds.right <= rootBounds.right)
    }

    @Test
    fun statusGaugesAndReadoutsKeepGapsInLandscapeAndNarrowLayouts() {
        showDashboard(data = sampleData(), state = Connected)
        assertVerticalGap("dashboard-status-bar", "dashboard-primary-region")
        assertVerticalGap("dashboard-primary-region", "dashboard-secondary-readouts")
        assertHorizontalGap("dashboard-readout-clt", "dashboard-readout-voltage")

        composeRule.runOnIdle { narrowLayoutState.value = true }
        composeRule.waitForIdle()
        assertVerticalGap("dashboard-status-bar", "dashboard-primary-region")
        assertVerticalGap("dashboard-primary-region", "dashboard-secondary-readouts")
        assertHorizontalGap("dashboard-readout-clt", "dashboard-readout-voltage")
    }

    @Composable
    private fun dashboardLayout(data: DashboardViewData?, state: ConnectionState?, narrow: Boolean) {
        org.secu3.android.ui.compose.SecuComposeTheme(darkTheme = false) {
            if (narrow) {
                Box(Modifier.width(360.dp).fillMaxHeight()) {
                    DashboardContent(data = data, connectionState = state)
                }
            } else {
                DashboardContent(data = data, connectionState = state)
            }
        }
    }

    private fun assertVerticalGap(upperTag: String, lowerTag: String) {
        val upper = composeRule.onNodeWithTag(upperTag).getUnclippedBoundsInRoot()
        val lower = composeRule.onNodeWithTag(lowerTag).getUnclippedBoundsInRoot()
        assertTrue("$upperTag and $lowerTag should have a visible vertical gap", lower.top - upper.bottom > 0.dp)
    }

    private fun assertHorizontalGap(leftTag: String, rightTag: String) {
        val left = composeRule.onNodeWithTag(leftTag).getUnclippedBoundsInRoot()
        val right = composeRule.onNodeWithTag(rightTag).getUnclippedBoundsInRoot()
        assertTrue("$leftTag and $rightTag should have a visible horizontal gap", right.left - left.right > 0.dp)
    }

    private fun assertGaugeGutterMatchesDashboardBackground() {
        val image = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val gauge = composeRule.onNodeWithTag("dashboard-rpm-gauge").getUnclippedBoundsInRoot()
        val x = with(composeRule.density) { (gauge.left + 2.dp).roundToPx() }
        val y = with(composeRule.density) { ((gauge.top + gauge.bottom) / 2).roundToPx() }
        val background = image.getPixel(5, 5)
        assertEquals("gauge gutter should remain on the dashboard background", background, image.getPixel(x, y))
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
            narrowLayoutState = mutableStateOf(false)
            composeRule.setContent {
                dashboardLayout(dataState.value, connectionState.value, narrowLayoutState.value)
            }
            contentInstalled = true
        }
        composeRule.waitForIdle()
    }

    private fun sampleData(
        temperature: Float = 87f,
        voltage: Float = 13.8f,
        load: Float = 42f,
        map: Float = 62f,
        tps: Float = 18f,
        oilTemperature: Float = 92f,
        afr: Float = 14.7f,
        fuelLevel: Float = 36f,
    ) = DashboardViewData(
        rpm = 2250f,
        speed = 84f,
        load = load,
        map = map,
        tps = tps,
        temperature = temperature,
        voltage = voltage,
        currentAngle = 12f,
        afr = afr,
        oilTemperature = oilTemperature,
        fuelLevel = fuelLevel,
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

    private fun captureDesignScreenshot(fileName: String) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, fileName).outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
