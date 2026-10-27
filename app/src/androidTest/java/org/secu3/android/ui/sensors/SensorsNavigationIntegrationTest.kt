package org.secu3.android.ui.sensors

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.SENSOR_SETTINGS_ROUTE
import org.secu3.android.ui.SENSORS_ROUTE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SensorsNavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun rawPageAndSensorsViewModelSurviveSettingsNavigationAndRecreation() {
        val prefs = compose.activity.userPrefs
        val originalOldView = runBlocking { prefs.settings.first().oldSensorViewEnabled }
        try {
            runBlocking { prefs.setOldSensorViewEnabled(false) }
            val host = navigateToHomeAndSensors()
            val sensorsViewModel = currentSensorsViewModel()
            assertOverlayRoute(host, SENSORS_ROUTE)

            compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).performClick()
            compose.waitForIdle()
            compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).assertIsSelected()
            openSensorsSettings()
            assertEquals(SENSOR_SETTINGS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)

            compose.onNodeWithTag("sensors-old-view-checkbox").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOn()
            compose.waitUntil(5_000) { runBlocking { prefs.settings.first().oldSensorViewEnabled } }

            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            val restoredHost = nativeHost()
            assertOverlayRoute(restoredHost, SENSOR_SETTINGS_ROUTE)
            compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOn()
            compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
            compose.waitForIdle()
            assertEquals(SENSORS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
            assertSame(sensorsViewModel, currentSensorsViewModel())
            compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).assertIsSelected()
            compose.onNodeWithText(context.getString(R.string.sensors_tab_title)).performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("old-sensors-list").assertExists()

            openSensorsSettings()
            runBlocking { prefs.setOldSensorViewEnabled(false) }
            compose.waitForIdle()
            compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOff()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertEquals(SENSORS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
            assertSame(sensorsViewModel, currentSensorsViewModel())
            compose.onNodeWithTag("gauges-grid").assertExists()

            compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
            compose.waitForIdle()
            assertLegacyHome(nativeHost())
            compose.onNodeWithTag("home-card-sensors").performScrollTo().performClick()
            compose.waitForIdle()
            val reopenedViewModel = currentSensorsViewModel()
            org.junit.Assert.assertNotSame(sensorsViewModel, reopenedViewModel)
            compose.onNodeWithTag("gauges-grid").assertExists()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertLegacyHome(nativeHost())
        } finally {
            runBlocking { prefs.setOldSensorViewEnabled(originalOldView) }
        }
    }

    @Test fun gaugeStylePersistsAndUpdatesAcrossSettingsAndSensorsRoutes() {
        val prefs = compose.activity.userPrefs
        val originalStyle = runBlocking { prefs.settings.first().sensorGaugeStyle }
        try {
            runBlocking { prefs.setSensorGaugeStyle(SensorGaugeStyle.PreciseArc) }
            navigateToHomeAndSensors()
            val sensorsViewModel = currentSensorsViewModel()
            assertEquals(SensorGaugeStyle.PreciseArc, sensorsViewModel.state.value.gaugeStyle)

            openSensorsSettings()
            compose.onNodeWithTag("sensor-gauge-style-row").performScrollTo().performClick()
            compose.onNodeWithTag("sensor-gauge-style-current").performClick()
            compose.waitForIdle()
            compose.waitUntil(5_000) { runBlocking { prefs.settings.first().sensorGaugeStyle == SensorGaugeStyle.Current } }
            assertEquals(SensorGaugeStyle.Current, sensorsViewModel.state.value.gaugeStyle)
            compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
            compose.waitForIdle()

            openSensorsSettings()
            compose.onNodeWithTag("sensor-gauge-style-row").performScrollTo().performClick()
            compose.onNodeWithTag("sensor-gauge-style-precise_arc").performClick()
            compose.waitForIdle()
            assertEquals("precise_arc", runBlocking { prefs.settings.first().sensorGaugeStyle.preferenceValue })
            assertEquals(SensorGaugeStyle.PreciseArc, sensorsViewModel.state.value.gaugeStyle)

            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithTag("sensor-gauge-style-value", useUnmergedTree = true)
                .assertTextEquals(context.getString(R.string.sensor_gauge_style_precise_arc))
            compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
            compose.waitForIdle()
            assertSame(sensorsViewModel, currentSensorsViewModel())
            assertEquals(SensorGaugeStyle.PreciseArc, sensorsViewModel.state.value.gaugeStyle)

            runBlocking { prefs.setSensorGaugeStyle(SensorGaugeStyle.ThinNeedle) }
            compose.waitForIdle()
            assertEquals(SensorGaugeStyle.ThinNeedle, sensorsViewModel.state.value.gaugeStyle)
            openSensorsSettings()
            compose.onNodeWithTag("sensor-gauge-style-value", useUnmergedTree = true)
                .assertTextEquals(context.getString(R.string.sensor_gauge_style_thin_needle))
        } finally {
            runBlocking { prefs.setSensorGaugeStyle(originalStyle) }
        }
    }

    private fun navigateToHomeAndSensors(): NavHostController {
        awaitRootNavigation()
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
            host.navigate(org.secu3.android.ui.HOME_ROUTE)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("home-card-sensors").performScrollTo().performClick()
        compose.waitForIdle()
        return host
    }

    private fun openSensorsSettings() {
        compose.onNodeWithContentDescription(context.getString(R.string.title_activity_settings)).performClick()
        compose.waitForIdle()
    }

    private fun currentSensorsViewModel(): SensorsViewModel {
        lateinit var viewModel: SensorsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            val entry = requireNotNull(activity.rootNavigationController).getBackStackEntry(SENSORS_ROUTE)
            viewModel = ViewModelProvider(
                entry.viewModelStore,
                activity.defaultViewModelProviderFactory,
                entry.defaultViewModelCreationExtras,
            )[SensorsViewModel::class.java]
        }
        return viewModel
    }

    private fun nativeHost(): NavHostController {
        awaitRootNavigation()
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
        }
        return host
    }

    private fun awaitRootNavigation() {
        compose.waitUntil(5_000) {
            var ready = false
            compose.activityRule.scenario.onActivity { ready = it.rootNavigationController?.currentDestination != null }
            ready
        }
    }

    private fun assertLegacyHome(host: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(org.secu3.android.ui.HOME_ROUTE, host.currentDestination?.route)
            assertEquals(Lifecycle.State.RESUMED, host.getBackStackEntry(org.secu3.android.ui.HOME_ROUTE).lifecycle.currentState)
            assertEquals(org.secu3.android.ui.HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        compose.onNodeWithTag("home-card-sensors").assertExists()
    }

    private fun assertOverlayRoute(host: NavHostController, route: String) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(route, activity.rootNavigationController?.currentDestination?.route)
            host.getBackStackEntry(org.secu3.android.ui.HOME_ROUTE)
        }
    }
}
