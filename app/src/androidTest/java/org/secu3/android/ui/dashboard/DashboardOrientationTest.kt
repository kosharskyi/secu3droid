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
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.MainActivity

class DashboardOrientationTest {
    @Test
    fun dashboardLocksLandscapeAndRestoresUnspecifiedWhenRemoved() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            awaitConfigurationOrientation(scenario, Configuration.ORIENTATION_PORTRAIT)

            scenario.onActivity { activity ->
                val navHost = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHost.navController.navigate(R.id.dashboardFragment)
            }

            awaitConfigurationOrientation(scenario, Configuration.ORIENTATION_LANDSCAPE)
            scenario.onActivity { activity ->
                assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
                val navHost = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHost.navController.popBackStack()
            }

            scenario.onActivity { activity ->
                assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
            }
        }
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
            scenario.onActivity { activity ->
                actualOrientation = activity.resources.configuration.orientation
            }
            if (actualOrientation == expectedOrientation) return
            SystemClock.sleep(50L)
        } while (SystemClock.elapsedRealtime() < timeoutAt)

        assertEquals(
            "Activity configuration did not reach requested orientation in time",
            expectedOrientation,
            actualOrientation,
        )
    }
}
