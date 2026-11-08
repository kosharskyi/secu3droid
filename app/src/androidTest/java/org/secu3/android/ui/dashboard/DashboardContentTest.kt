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
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.abs
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.models.DashboardGaugeStyle
import org.secu3.android.ui.sensors.GaugeDialRenderer
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.utils.prefs.UserPrefs
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Connecting

class DashboardContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var contentInstalled = false
    private lateinit var dataState: MutableState<DashboardViewData?>
    private lateinit var connectionState: MutableState<ConnectionState?>
    private lateinit var narrowLayoutState: MutableState<Boolean>
    private lateinit var gaugeStyleState: MutableState<DashboardGaugeStyle>
    private val presentationCatalog = SensorPresentationCatalog()
    private val gaugeDialRenderer = GaugeDialRenderer()

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
        composeRule.waitForIdle()
        captureDesignScreenshot("design-dashboard-light.png")
        assertGaugeGutterMatchesDashboardBackground()

        listOf("2250", context.getString(R.string.units_rpm)).forEach {
            composeRule.onNodeWithText(it, substring = true).assertIsDisplayed()
        }
        composeRule.onNodeWithText("84", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.units_km_h)).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-readout-clt-label")
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.dashboard_readout_clt))
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
            Connecting to R.string.dashboard_status_connecting,
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
        composeRule.onNodeWithTag("dashboard-readout-optional-label")
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.dashboard_readout_aux))
        composeRule.onAllNodesWithText("--").assertCountEquals(5)
    }

    @Test
    fun validZeroPrimaryValuesRemainVisible() {
        showDashboard(data = sampleData().copy(primaryLeft = DashboardGaugeReading(GaugeType.RPM, 0f), primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, 0f)), state = Connected)

        composeRule.onNodeWithTag("dashboard-gauge-value-left")
            .assertTextEquals(presentationCatalog.formatValue(GaugeType.RPM, 0f))
        composeRule.onNodeWithTag("dashboard-gauge-value-right")
            .assertTextEquals(presentationCatalog.formatValue(GaugeType.VEHICLE_SPEED, 0f))
        composeRule.onAllNodesWithText("—", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun primaryGaugeNeedlesAnimateBothDirectionsRetargetAndHideWhenUnavailable() {
        composeRule.mainClock.autoAdvance = false
        try {
            dataState = mutableStateOf(null)
            connectionState = mutableStateOf(Disconnected)
            narrowLayoutState = mutableStateOf(false)
            val contentKey = mutableStateOf(0)
            composeRule.setContent {
                androidx.compose.runtime.key(contentKey.value) {
                    dashboardLayout(dataState.value, connectionState.value, narrowLayoutState.value)
                }
            }
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            val zeroRpmNeedle = gaugeRing("dashboard-rpm-progress")
            val zeroSpeedNeedle = gaugeRing("dashboard-speed-progress")

            composeRule.runOnIdle {
                dataState.value = sampleData().copy(primaryLeft = DashboardGaugeReading(GaugeType.RPM, 5600f), primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, 150f))
                connectionState.value = Connected
                contentKey.value++
            }
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            val restoredRpmEndpoint = gaugeRing("dashboard-rpm-progress")
            val restoredSpeedEndpoint = gaugeRing("dashboard-speed-progress")

            composeRule.runOnIdle {
                dataState.value = sampleData().copy(primaryLeft = DashboardGaugeReading(GaugeType.RPM, 2400f), primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, 66f))
                contentKey.value++
            }
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            composeRule.onNodeWithText("2400").assertIsDisplayed()
            val initial = gaugeRing("dashboard-rpm-progress")
            val initialSpeed = gaugeRing("dashboard-speed-progress")
            assertTrue("initial RPM needle has a visible position", !initial.contentEquals(zeroRpmNeedle))
            assertTrue("initial speed needle has a visible position", !initialSpeed.contentEquals(zeroSpeedNeedle))

            composeRule.runOnIdle { dataState.value = sampleData().copy(primaryLeft = DashboardGaugeReading(GaugeType.RPM, 4000f), primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, 110f)) }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("4000").assertIsDisplayed()
            val beforeRising = gaugeRing("dashboard-rpm-progress")
            composeRule.mainClock.advanceTimeBy(60)
            composeRule.waitForIdle()
            val rising = gaugeRing("dashboard-rpm-progress")
            assertTrue("RPM needle animates upward", !beforeRising.contentEquals(rising))
            assertTrue("speed needle animates alongside RPM", !initialSpeed.contentEquals(gaugeRing("dashboard-speed-progress")))

            composeRule.runOnIdle { dataState.value = sampleData().copy(primaryLeft = DashboardGaugeReading(GaugeType.RPM, 1600f), primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, 30f)) }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            val beforeFalling = gaugeRing("dashboard-rpm-progress")
            composeRule.mainClock.advanceTimeBy(60)
            composeRule.waitForIdle()
            val falling = gaugeRing("dashboard-rpm-progress")
            assertTrue("RPM needle animates downward from its current position", !beforeFalling.contentEquals(falling))

            composeRule.runOnIdle { connectionState.value = Disconnected }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText("—", useUnmergedTree = true).assertCountEquals(2)
            assertTrue("disconnect immediately hides the RPM needle", zeroRpmNeedle.contentEquals(gaugeRing("dashboard-rpm-progress")))
            assertTrue("disconnect immediately hides the speed needle", zeroSpeedNeedle.contentEquals(gaugeRing("dashboard-speed-progress")))
            composeRule.mainClock.advanceTimeBy(60)
            composeRule.waitForIdle()
            assertTrue("unavailable RPM needle stays hidden", zeroRpmNeedle.contentEquals(gaugeRing("dashboard-rpm-progress")))
            assertTrue("unavailable speed needle stays hidden", zeroSpeedNeedle.contentEquals(gaugeRing("dashboard-speed-progress")))

            composeRule.runOnIdle {
                dataState.value = sampleData().copy(primaryLeft = DashboardGaugeReading(GaugeType.RPM, 5600f), primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, 150f))
                connectionState.value = Connected
            }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("5600").assertIsDisplayed()
            val restoring = gaugeRing("dashboard-rpm-progress")
            val restoringSpeed = gaugeRing("dashboard-speed-progress")
            composeRule.mainClock.advanceTimeBy(60)
            composeRule.waitForIdle()
            assertTrue("RPM needle animates from its current progress after reconnect", !restoring.contentEquals(gaugeRing("dashboard-rpm-progress")))
            assertTrue("speed needle animates from its current progress after reconnect", !restoringSpeed.contentEquals(gaugeRing("dashboard-speed-progress")))
            composeRule.mainClock.advanceTimeBy(700)
            composeRule.waitForIdle()
            assertTrue("RPM needle reaches the live target after reconnect", restoredRpmEndpoint.contentEquals(gaugeRing("dashboard-rpm-progress")))
            assertTrue("speed needle reaches the live target after reconnect", restoredSpeedEndpoint.contentEquals(gaugeRing("dashboard-speed-progress")))

            composeRule.runOnIdle { connectionState.value = Disconnected }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeBy(1200)
            composeRule.waitForIdle()
            assertTrue("disconnected RPM needle stays hidden", zeroRpmNeedle.contentEquals(gaugeRing("dashboard-rpm-progress")))
            assertTrue("disconnected speed needle stays hidden", zeroSpeedNeedle.contentEquals(gaugeRing("dashboard-speed-progress")))
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
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
                    dashboardTestContent(sampleData(), Connected)
                }
            }
        }
        composeRule.onNodeWithText("2250", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-readout-clt-label")
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.dashboard_readout_clt))
        composeRule.onRoot().captureToImage()
        assertGaugeGutterMatchesDashboardBackground()
        captureDesignScreenshot("design-dashboard-dark.png")
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val valueBounds = composeRule.onNodeWithText("2250", substring = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("scaled dashboard value should stay within the viewport", valueBounds.left >= rootBounds.left && valueBounds.right <= rootBounds.right)
    }

    @Test
    fun dashboardThemeToggleChangesOnlyDashboardAndSwitchesBack() {
        composeRule.setContent {
            org.secu3.android.ui.compose.SecuComposeTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    dashboardTestContent(sampleData(), Connected)
                    Box(
                        Modifier.align(Alignment.BottomEnd)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .testTag("outer-theme-swatch")
                    )
                }
            }
        }

        val lightDashboard = dashboardCornerColor()
        val outerTheme = outerSwatchColor()
        assertEquals("Dashboard uses the warm light background", 0xFFD9C7AC.toInt(), lightDashboard)
        assertEquals("warm Dashboard palette does not change the app theme", 0xFFF3F6FA.toInt(), outerTheme)
        composeRule.onNodeWithContentDescription(context.getString(R.string.dashboard_theme_switch_to_dark)).performClick()
        val darkDashboard = dashboardCornerColor()
        assertTrue("Dashboard background switches to dark", darkDashboard != lightDashboard)
        assertEquals("the app theme outside Dashboard stays light", outerTheme, outerSwatchColor())
        composeRule.onNodeWithContentDescription(context.getString(R.string.dashboard_theme_switch_to_light)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(context.getString(R.string.dashboard_theme_switch_to_light)).performClick()
        assertEquals("second tap restores the warm Dashboard palette", lightDashboard, dashboardCornerColor())
        assertEquals("the app theme outside Dashboard stays light", outerTheme, outerSwatchColor())
    }

    @Test
    fun dashboardThemeToggleSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            org.secu3.android.ui.compose.SecuComposeTheme(darkTheme = false) {
                dashboardTestContent(sampleData(), Connected)
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.dashboard_theme_switch_to_dark)).performClick()
        val darkDashboard = dashboardCornerColor()
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription(context.getString(R.string.dashboard_theme_switch_to_light)).assertIsDisplayed()
        assertEquals("restored Dashboard retains its local dark theme", darkDashboard, dashboardCornerColor())
    }

    @Test
    fun gaugeSettingsButtonOpensDedicatedRoute() {
        var openedSettings = false
        composeRule.setContent {
            org.secu3.android.ui.compose.SecuComposeTheme(darkTheme = false) {
                DashboardContent(
                    data = sampleData(),
                    connectionState = Connected,
                    dashboardSettings = DashboardUiState(settingsLoaded = true),
                    presentationCatalog = presentationCatalog,
                    gaugeDialRenderer = gaugeDialRenderer,
                    onOpenGaugeSettings = { openedSettings = true },
                )
            }
        }

        composeRule.onNodeWithTag("dashboard-gauge-settings").performClick()
        assertTrue("gear requests the dedicated settings destination", openedSettings)
    }

    @Test
    fun dashboardPrimaryGaugesUseCatalogTitlesUnitsPrecisionAndSectionColors() {
        val customData = sampleData().copy(
            primaryLeft = DashboardGaugeReading(GaugeType.ADD1, 1.234f),
            primaryRight = DashboardGaugeReading(GaugeType.VOLTAGE, 12.34f),
        )
        composeRule.setContent {
            org.secu3.android.ui.compose.SecuComposeTheme(darkTheme = false) {
                DashboardContent(
                    data = customData,
                    connectionState = Connected,
                    dashboardSettings = DashboardUiState(
                        leftGauge = GaugeType.ADD1,
                        rightGauge = GaugeType.VOLTAGE,
                        settingsLoaded = true,
                    ),
                    presentationCatalog = presentationCatalog,
                    gaugeDialRenderer = gaugeDialRenderer,
                    onOpenGaugeSettings = {},
                )
            }
        }

        composeRule.onNodeWithTag("dashboard-gauge-title-left")
            .assertTextEquals(context.getString(presentationCatalog.gauge(GaugeType.ADD1).title))
        composeRule.onNodeWithText("1.234").assertIsDisplayed()
        composeRule.onNode(
            hasText(context.getString(presentationCatalog.gauge(GaugeType.ADD1).units)) and
                hasAnyAncestor(hasTestTag("dashboard-rpm-gauge")),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-gauge-title-right")
            .assertTextEquals(context.getString(presentationCatalog.gauge(GaugeType.VOLTAGE).title))
        composeRule.onNodeWithText("12.34").assertIsDisplayed()
        composeRule.onNode(
            hasText(context.getString(presentationCatalog.gauge(GaugeType.VOLTAGE).units)) and
                hasAnyAncestor(hasTestTag("dashboard-speed-gauge")),
        ).assertIsDisplayed()

        val gaugeBounds = composeRule.onNodeWithTag("dashboard-rpm-gauge").getUnclippedBoundsInRoot()
        val valueBounds = composeRule.onNodeWithTag("dashboard-gauge-value-left").getUnclippedBoundsInRoot()
        val unitBounds = composeRule.onNodeWithTag("dashboard-gauge-unit-left").getUnclippedBoundsInRoot()
        val titleBounds = composeRule.onNodeWithTag("dashboard-gauge-title-left").getUnclippedBoundsInRoot()
        val gaugeCenterY = (gaugeBounds.top + gaugeBounds.bottom) / 2
        val valueAndUnitCenterY = (valueBounds.top + unitBounds.bottom) / 2
        assertTrue("value and unit remain centered independently of the sensor caption", abs((gaugeCenterY - valueAndUnitCenterY).value) < 20f)
        assertTrue("sensor caption stays outside the centered value/unit block", titleBounds.top >= unitBounds.bottom)
        assertEquals(11, DASHBOARD_DECORATIVE_TICK_COUNT)

        val bitmap = captureGauge("dashboard-rpm-progress")
        val renderedPixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(renderedPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val catalogColor = context.getColor(presentationCatalog.gauge(GaugeType.ADD1).sections.first().color)
        assertTrue("Dashboard arc uses the shared catalog section color", renderedPixels.any { it == catalogColor })
    }

    @Test
    fun everyDashboardStyleRendersLiveValuesAndHidesUnavailableProgress() {
        dataState = mutableStateOf(sampleData())
        connectionState = mutableStateOf(Connected)
        narrowLayoutState = mutableStateOf(false)
        gaugeStyleState = mutableStateOf(DashboardGaugeStyle.Default)
        composeRule.setContent {
            dashboardLayout(dataState.value, connectionState.value, narrowLayoutState.value, gaugeStyleState.value)
        }
        DashboardGaugeStyle.entries.forEach { style ->
            composeRule.runOnIdle { gaugeStyleState.value = style; dataState.value = sampleData(); connectionState.value = Connected }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("dashboard-gauge-value-left").assertIsDisplayed()
            composeRule.onNodeWithTag("dashboard-gauge-value-left").assertTextEquals("2250")
            if (style == DashboardGaugeStyle.PreciseArc) {
                val dialBounds = composeRule.onNodeWithTag("dashboard-rpm-progress").getUnclippedBoundsInRoot()
                val valueBounds = composeRule.onNodeWithTag("dashboard-gauge-value-left").getUnclippedBoundsInRoot()
                val unitBounds = composeRule.onNodeWithTag("dashboard-gauge-unit-left").getUnclippedBoundsInRoot()
                val dialCenterY = (dialBounds.top + dialBounds.bottom) / 2
                val readoutCenterY = (valueBounds.top + unitBounds.bottom) / 2
                assertTrue("Precise Arc readout stays at its measured dial center", abs((dialCenterY - readoutCenterY).value) < 20f)
            }
            val liveDial = captureGauge("dashboard-rpm-progress")
            captureDesignScreenshot("dashboard-style-${style.preferenceValue}-light.png")
            composeRule.onNodeWithContentDescription(context.getString(R.string.dashboard_theme_switch_to_dark)).performClick()
            captureDesignScreenshot("dashboard-style-${style.preferenceValue}-dark.png")

            composeRule.onNodeWithContentDescription(context.getString(R.string.dashboard_theme_switch_to_light)).performClick()
            composeRule.runOnIdle { dataState.value = null; connectionState.value = Disconnected }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("dashboard-gauge-value-left").assertTextEquals("—")
            assertTrue("$style hides its active progress when unavailable", !liveDial.sameAs(captureGauge("dashboard-rpm-progress")))

            composeRule.runOnIdle { dataState.value = sampleData().copy(primaryLeft = DashboardGaugeReading(GaugeType.RPM, 0f)); connectionState.value = Connected }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("dashboard-gauge-value-left").assertTextEquals("0")
        }

        composeRule.runOnIdle {
            narrowLayoutState.value = true
            dataState.value = sampleData()
            connectionState.value = Connected
        }
        DashboardGaugeStyle.entries.forEach { style ->
            composeRule.runOnIdle { gaugeStyleState.value = style }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("dashboard-gauge-value-left").assertIsDisplayed()
            val dialBounds = composeRule.onNodeWithTag("dashboard-rpm-progress").getUnclippedBoundsInRoot()
            val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
            assertTrue("$style compact dial stays within the viewport", dialBounds.left.value >= rootBounds.left && dialBounds.right.value <= rootBounds.right && dialBounds.top.value >= rootBounds.top && dialBounds.bottom.value <= rootBounds.bottom)
            val valueBounds = composeRule.onNodeWithTag("dashboard-gauge-value-left").getUnclippedBoundsInRoot()
            val unitBounds = composeRule.onNodeWithTag("dashboard-gauge-unit-left").getUnclippedBoundsInRoot()
            val dialCenterY = (dialBounds.top + dialBounds.bottom) / 2
            val readoutCenterY = (valueBounds.top + unitBounds.bottom) / 2
            if (style == DashboardGaugeStyle.PreciseArc) {
                assertTrue("compact Precise Arc readout stays centered in its dial", abs((dialCenterY - readoutCenterY).value) < 24f)
                captureDesignScreenshot("dashboard-style-precise_arc-compact-light.png")
            }
            if (style == DashboardGaugeStyle.ThinNeedle) {
                assertTrue("compact Thin Needle readout stays below the dial", readoutCenterY > dialCenterY)
                captureDesignScreenshot("dashboard-style-thin_needle-compact-light.png")
            }
            if (style == DashboardGaugeStyle.Classic) assertTrue("compact Classic readout stays below the dial", readoutCenterY > dialCenterY)
            if (style == DashboardGaugeStyle.FilledArc || style == DashboardGaugeStyle.TriangularNeedle) {
                assertTrue("$style compact readout stays inside the dial center", abs((dialCenterY - readoutCenterY).value) < 32f)
            }
        }
    }

    @Test
    fun themeButtonHasReservedSpaceBesideScrollableWarnings() {
        showDashboard(
            data = sampleData(temperature = 106f, voltage = 11f, oilTemperature = 140f),
            state = Connected,
        )

        assertThemeControlBounds()

        composeRule.runOnIdle { narrowLayoutState.value = true }
        composeRule.waitForIdle()
        assertThemeControlBounds()
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
    private fun dashboardTestContent(data: DashboardViewData?, state: ConnectionState?, style: DashboardGaugeStyle = DashboardGaugeStyle.Default) {
        DashboardContent(
            data = data,
            connectionState = state,
            dashboardSettings = DashboardUiState(gaugeStyle = style, settingsLoaded = true),
            presentationCatalog = presentationCatalog,
            gaugeDialRenderer = gaugeDialRenderer,
            onOpenGaugeSettings = {}
        )
    }

    @Composable
    private fun dashboardLayout(data: DashboardViewData?, state: ConnectionState?, narrow: Boolean, style: DashboardGaugeStyle = DashboardGaugeStyle.Default) {
        org.secu3.android.ui.compose.SecuComposeTheme(darkTheme = false) {
            if (narrow) {
                Box(Modifier.width(360.dp).fillMaxHeight()) {
                    dashboardTestContent(data, state, style)
                }
            } else {
                dashboardTestContent(data, state, style)
            }
        }
    }

    private fun assertThemeControlBounds() {
        val statusBounds = composeRule.onNodeWithTag("dashboard-status-bar").getUnclippedBoundsInRoot()
        val toggleBounds = composeRule.onNodeWithTag("dashboard-theme-toggle").getUnclippedBoundsInRoot()
        assertTrue("scrollable warnings stop before the fixed theme button", statusBounds.right <= toggleBounds.left)
        assertTrue("theme button keeps a 48dp touch target", toggleBounds.right - toggleBounds.left >= 48.dp)
        composeRule.onNodeWithTag("dashboard-theme-toggle").assertIsDisplayed()
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

    private fun dashboardCornerColor(): Int = composeRule.onRoot()
        .captureToImage()
        .asAndroidBitmap()
        .getPixel(5, 5)

    private fun outerSwatchColor(): Int {
        val image = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val bounds = composeRule.onNodeWithTag("outer-theme-swatch").getUnclippedBoundsInRoot()
        val x = with(composeRule.density) { ((bounds.left + bounds.right) / 2).roundToPx() }
        val y = with(composeRule.density) { ((bounds.top + bounds.bottom) / 2).roundToPx() }
        return image.getPixel(x, y)
    }

    private fun captureGauge(tag: String) = composeRule.onNodeWithTag(tag).captureToImage().asAndroidBitmap()

    private fun gaugeRing(tag: String): IntArray {
        val bitmap = captureGauge(tag)
        val minDimension = minOf(bitmap.width, bitmap.height).toFloat()
        return IntArray(bitmap.width * bitmap.height) { index ->
            val x = index % bitmap.width
            val y = index / bitmap.width
            val dx = x - bitmap.width / 2f
            val dy = y - bitmap.height / 2f
            if (dx * dx + dy * dy > (minDimension * .29f) * (minDimension * .29f)) bitmap.getPixel(x, y) else 0
        }
    }

    private fun showDashboard(data: DashboardViewData?, state: ConnectionState?) {
        if (contentInstalled) {
            composeRule.runOnIdle {
                dataState.value = data
                connectionState.value = state
                gaugeStyleState.value = DashboardGaugeStyle.Default
            }
        } else {
            dataState = mutableStateOf(data)
            connectionState = mutableStateOf(state)
            narrowLayoutState = mutableStateOf(false)
            gaugeStyleState = mutableStateOf(DashboardGaugeStyle.Default)
            composeRule.setContent {
                dashboardLayout(dataState.value, connectionState.value, narrowLayoutState.value, gaugeStyleState.value)
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
        primaryLeft = DashboardGaugeReading(GaugeType.RPM, 2250f),
        primaryRight = DashboardGaugeReading(GaugeType.VEHICLE_SPEED, 84f),
    )

    private fun captureDesignScreenshot(fileName: String) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, fileName).outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
