/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 */
package org.secu3.android.ui.dashboard

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.DASHBOARD_ROUTE
import org.secu3.android.ui.HOME_ROUTE
import org.secu3.android.ui.MainActivity
import org.secu3.android.utils.UserPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DashboardOrientationTest {
    @Test
    fun dashboardRestoresLandscapeLockImmersiveBarsAndKeepScreenPolicy() {
        var originalPreference: Boolean? = null
        var preferences: UserPrefs? = null
        try {
            listOf(false, true).forEach { keepScreenOn ->
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    awaitNavigationReady(scenario)
                    lateinit var currentPreferences: UserPrefs
                    scenario.onActivity { currentPreferences = it.userPrefs }
                    preferences = currentPreferences
                    if (originalPreference == null) {
                        originalPreference = runBlocking { currentPreferences.settings.first().isKeepScreenAliveActive }
                    }
                    runBlocking { currentPreferences.setKeepScreenAliveActive(keepScreenOn) }
                    scenario.onActivity { activity ->
                        val host = requireNotNull(activity.rootNavigationController)
                        host.navigate(org.secu3.android.ui.HOME_ROUTE)
                    }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    scenario.onActivity { activity ->
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    awaitConfigurationOrientation(scenario, Configuration.ORIENTATION_PORTRAIT)

                    var originalStatusColor = 0
                    var originalNavigationColor = 0
                    var originalLightStatus = false
                    var originalLightNavigation = false
                    var originalBarsBehavior = 0
                    var originalSystemBarsMask = 0
                    scenario.onActivity { activity ->
                        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                        originalStatusColor = activity.window.statusBarColor
                        originalNavigationColor = activity.window.navigationBarColor
                        originalLightStatus = controller.isAppearanceLightStatusBars
                        originalLightNavigation = controller.isAppearanceLightNavigationBars
                        originalBarsBehavior = controller.systemBarsBehavior
                        originalSystemBarsMask = systemBarsVisibilityMask(activity)
                        assertEquals(primarySystemBarsMask, originalSystemBarsMask and primarySystemBarsMask)
                        activity.openDashboard()
                    }

                    awaitConfigurationOrientation(scenario, Configuration.ORIENTATION_LANDSCAPE)
                    awaitDashboardUiState(scenario)
                    assertDashboardActive(scenario)
                    scenario.recreate()
                    awaitConfigurationOrientation(scenario, Configuration.ORIENTATION_LANDSCAPE)
                    awaitDashboardUiState(scenario)
                    assertDashboardActive(scenario)

                    scenario.onActivity { activity -> requireNotNull(activity.rootNavigationController).popBackStack() }
                    awaitHomeWindowState(scenario, keepScreenOn, originalSystemBarsMask)
                    scenario.onActivity { activity ->
                        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
                        assertEquals(org.secu3.android.ui.HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
                        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                        assertEquals(originalStatusColor, activity.window.statusBarColor)
                        assertEquals(originalNavigationColor, activity.window.navigationBarColor)
                        assertEquals(originalLightStatus, controller.isAppearanceLightStatusBars)
                        assertEquals(originalLightNavigation, controller.isAppearanceLightNavigationBars)
                        assertEquals(originalBarsBehavior, controller.systemBarsBehavior)
                        assertEquals(originalSystemBarsMask, systemBarsVisibilityMask(activity))
                        val keepScreenOnFlag = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
                        assertEquals(keepScreenOn, keepScreenOnFlag)
                    }
                }
            }
        } finally {
            originalPreference?.let { value -> runBlocking { preferences?.setKeepScreenAliveActive(value) } }
        }
    }

    private fun awaitNavigationReady(scenario: ActivityScenario<MainActivity>) {
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            var ready = false
            scenario.onActivity { ready = it.rootNavigationController?.currentDestination != null }
            if (ready) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < timeoutAt)
        throw AssertionError("Navigation did not become ready after preferences loaded")
    }

    private fun assertDashboardActive(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
            assertEquals(DASHBOARD_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            assertEquals(0, systemBarsVisibilityMask(activity) and primarySystemBarsMask)
            assertEquals(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
                WindowInsetsControllerCompat(activity.window, activity.window.decorView).systemBarsBehavior,
            )
            assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        }
    }

    private fun systemBarsVisibilityMask(activity: MainActivity): Int {
        val types = listOf(
            WindowInsetsCompat.Type.statusBars(),
            WindowInsetsCompat.Type.navigationBars(),
            WindowInsetsCompat.Type.captionBar(),
            WindowInsetsCompat.Type.systemOverlays(),
        )
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        return types.fold(0) { mask, type ->
            if (insets?.isVisible(type) != false) mask or type else mask
        }
    }

    private val primarySystemBarsMask: Int
        get() = WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()

    private fun awaitDashboardUiState(scenario: ActivityScenario<MainActivity>) {
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        var ready = false
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                ready = activity.rootNavigationController?.currentDestination?.route == DASHBOARD_ROUTE &&
                    (systemBarsVisibilityMask(activity) and primarySystemBarsMask) == 0 &&
                    activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
            }
            if (ready) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < timeoutAt)
        assertTrue("Dashboard window effects did not become active", ready)
    }

    private fun awaitHomeWindowState(
        scenario: ActivityScenario<MainActivity>,
        keepScreenOn: Boolean,
        expectedSystemBarsMask: Int,
    ) {
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        var ready = false
        var observedState = "not observed"
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val keepFlag = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
                val route = activity.rootNavigationController?.currentDestination?.route
                val requestedOrientation = activity.requestedOrientation
                val visibleMask = systemBarsVisibilityMask(activity)
                observedState = "route=$route, systemBarsMask=$visibleMask " +
                    "(expected=$expectedSystemBarsMask), " +
                    "keepScreenOn=$keepFlag, requestedOrientation=$requestedOrientation"
                ready = route == org.secu3.android.ui.HOME_ROUTE &&
                    visibleMask == expectedSystemBarsMask && keepFlag == keepScreenOn &&
                    requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            if (ready) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < timeoutAt)
        assertTrue("Home window effects were not restored ($observedState)", ready)
    }

    private fun awaitConfigurationOrientation(
        scenario: ActivityScenario<MainActivity>,
        expectedOrientation: Int,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        var actualOrientation = Configuration.ORIENTATION_UNDEFINED
        do {
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity -> actualOrientation = activity.resources.configuration.orientation }
            if (actualOrientation == expectedOrientation) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < timeoutAt)
        assertEquals("Activity configuration did not reach requested orientation in time", expectedOrientation, actualOrientation)
    }
}

class DashboardTransportSelectionTest {
    @Test
    fun dashboardDoesNotRequireBluetoothSelection() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var preferences: UserPrefs? = null
        var oldAddress: String? = null
        var oldName: String? = null
        try {
            awaitNavigationReady(scenario)
            scenario.onActivity { preferences = it.userPrefs }
            val userPrefs = requireNotNull(preferences)
            runBlocking {
                val settings = userPrefs.settings.first()
                oldAddress = settings.bluetoothDeviceAddress
                oldName = settings.bluetoothDeviceName
                userPrefs.setBluetoothDevice(null, null)
            }

            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { activity ->
                val host = requireNotNull(activity.rootNavigationController)
                host.navigate(HOME_ROUTE)
                host.navigate(DASHBOARD_ROUTE)
            }
            awaitDashboardRouteComposition(scenario)
            // The Activity's RESUMED-only queue consumer is paused, so an old warning stays observable.
            SystemClock.sleep(500)

            var hasBluetoothWarning = false
            scenario.onActivity { activity ->
                hasBluetoothWarning = activity.uiMessageQueue.pending.value.any { it.textRes == R.string.choose_bluetooth_adapter }
            }
            assertFalse(hasBluetoothWarning)
        } finally {
            val address = oldAddress
            val name = oldName
            if (address != null || name != null) {
                runBlocking { preferences?.setBluetoothDevice(address, name) }
            }
            scenario.close()
        }
    }

    private fun awaitDashboardRouteComposition(scenario: ActivityScenario<MainActivity>) {
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        var composed = false
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                composed = activity.rootNavigationController?.currentDestination?.route == DASHBOARD_ROUTE &&
                    activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            if (composed) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < timeoutAt)
        assertTrue("Dashboard route did not compose while Activity was STARTED", composed)
    }

    private fun awaitNavigationReady(scenario: ActivityScenario<MainActivity>) {
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        var ready = false
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { ready = it.rootNavigationController?.currentDestination != null }
            if (ready) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < timeoutAt)
        assertTrue("Navigation did not become ready", ready)
    }
}
