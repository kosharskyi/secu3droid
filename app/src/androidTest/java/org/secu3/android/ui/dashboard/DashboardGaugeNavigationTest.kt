/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.DASHBOARD_GAUGE_SETTINGS_ROUTE
import org.secu3.android.ui.DASHBOARD_ROUTE
import org.secu3.android.ui.HOME_ROUTE
import org.secu3.android.ui.MainActivity

class DashboardGaugeNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardThemeOverrideSurvivesSettingsRouteRoundTrip() {
        composeRule.waitUntil(10_000L) {
            composeRule.activity.rootNavigationController?.currentDestination != null
        }
        composeRule.activity.runOnUiThread {
            composeRule.activity.rootNavigationController?.navigate(HOME_ROUTE)
            composeRule.activity.openDashboard()
        }
        waitForRoute(DASHBOARD_ROUTE)
        composeRule.onNodeWithTag("dashboard-theme-toggle").assertIsDisplayed()

        val switchToDark = composeRule.activity.getString(R.string.dashboard_theme_switch_to_dark)
        val switchToLight = composeRule.activity.getString(R.string.dashboard_theme_switch_to_light)
        val initialSwitch = if (
            composeRule.onAllNodesWithContentDescription(switchToDark).fetchSemanticsNodes().isNotEmpty()
        ) switchToDark else switchToLight
        val nextSwitch = if (initialSwitch == switchToDark) switchToLight else switchToDark
        composeRule.onNodeWithContentDescription(initialSwitch).performClick()

        composeRule.onNodeWithTag("dashboard-gauge-settings").performClick()
        waitForRoute(DASHBOARD_GAUGE_SETTINGS_ROUTE)
        composeRule.onNodeWithTag("dashboard-gauge-cancel").performClick()
        waitForRoute(DASHBOARD_ROUTE)
        composeRule.onNodeWithContentDescription(nextSwitch).assertIsDisplayed()
    }

    private fun waitForRoute(route: String) {
        composeRule.waitUntil(10_000L) {
            composeRule.activity.rootNavigationController?.currentDestination?.route == route
        }
        assertTrue(composeRule.activity.rootNavigationController?.currentDestination?.route == route)
    }
}
