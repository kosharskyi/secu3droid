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
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.DASHBOARD_ROUTE
import org.secu3.android.ui.MainActivity

class DashboardOrientationTest {
    @Test
    fun dashboardRestoresLandscapeLockImmersiveBarsAndKeepScreenPolicy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val preferenceKey = context.getString(R.string.pref_keep_screen_key)
        val hadPreference = preferences.contains(preferenceKey)
        val originalPreference = preferences.getBoolean(preferenceKey, false)
        try {
            listOf(false, true).forEach { keepScreenOn ->
                preferences.edit().putBoolean(preferenceKey, keepScreenOn).commit()
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
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
            if (hadPreference) preferences.edit().putBoolean(preferenceKey, originalPreference).commit()
            else preferences.edit().remove(preferenceKey).commit()
        }
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
