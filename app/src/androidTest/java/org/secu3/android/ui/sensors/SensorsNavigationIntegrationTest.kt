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
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.SENSOR_SETTINGS_ROUTE
import org.secu3.android.ui.SENSORS_ROUTE
import org.secu3.android.utils.getDefaultSharedPreferences

class SensorsNavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun rawPageAndSensorsViewModelSurviveSettingsNavigationAndRecreation() {
        val preferences = context.getDefaultSharedPreferences()
        val oldViewKey = "old_sensors_view"
        val columnsKey = "columns_count"
        val hadOldView = preferences.contains(oldViewKey)
        val hadColumns = preferences.contains(columnsKey)
        val originalOldView = preferences.getBoolean(oldViewKey, false)
        val originalColumns = preferences.getInt(columnsKey, 2)
        try {
            preferences.edit().putBoolean(oldViewKey, false).putInt(columnsKey, 2).commit()
            val host = navigateToHomeAndSensors()
            val sensorsViewModel = currentSensorsViewModel()
            assertOverlayRoute(host, SENSORS_ROUTE)

            compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).performClick()
            compose.waitForIdle()
            compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).assertIsSelected()
            openSensorsSettings()
            assertEquals(SENSOR_SETTINGS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)

            compose.onNodeWithTag("sensors-old-view-checkbox").performClick()
            compose.onNodeWithTag("sensor-columns-value").performClick()
            compose.onNodeWithText("7", substring = false).performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOn()
            compose.onNodeWithTag("sensor-columns-value").assertTextEquals("7")
            assertEquals(true, preferences.getBoolean(oldViewKey, false))
            assertEquals(7, preferences.getInt(columnsKey, 2))

            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            val restoredHost = nativeHost()
            assertOverlayRoute(restoredHost, SENSOR_SETTINGS_ROUTE)
            compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOn()
            compose.onNodeWithTag("sensor-columns-value").assertTextEquals("7")
            compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
            compose.waitForIdle()
            assertEquals(SENSORS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
            assertSame(sensorsViewModel, currentSensorsViewModel())
            compose.onNodeWithText(context.getString(R.string.menu_raw_sensors)).assertIsSelected()
            compose.onNodeWithText(context.getString(R.string.sensors_tab_title)).performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("old-sensors-list").assertExists()

            openSensorsSettings()
            compose.activityRule.scenario.onActivity {
                preferences.edit().putBoolean(oldViewKey, false).putInt(columnsKey, 4).commit()
            }
            compose.waitForIdle()
            compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOff()
            compose.onNodeWithTag("sensor-columns-value").assertTextEquals("4")
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
            preferences.edit().apply {
                if (hadOldView) putBoolean(oldViewKey, originalOldView) else remove(oldViewKey)
                if (hadColumns) putInt(columnsKey, originalColumns) else remove(columnsKey)
            }.commit()
        }
    }

    private fun navigateToHomeAndSensors(): NavHostController {
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
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
        }
        return host
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
