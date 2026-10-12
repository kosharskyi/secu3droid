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

package org.secu3.android.ui.home

import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import androidx.compose.ui.unit.dp

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun displaysAllMenuDestinationsAndInvokesTheirCallbacks() {
        val called = mutableListOf<String>()
        compose.setContent { HomeScreen(HomeScreenState(), HomeScreenActions(
            onDashboard = { called += "dashboard" }, onSensors = { called += "sensors" },
            onParameters = { called += "parameters" }, onErrors = { called += "errors" },
            onLogs = { called += "logs" }, onSettings = { called += "settings" },
        )) }
        val destinations = listOf(
            R.string.menu_dashboard to "dashboard", R.string.sensors to "sensors", R.string.menu_param to "parameters",
            R.string.title_activity_errors to "errors", R.string.title_activity_diagnostics to "diagnostics",
            R.string.menu_secu_logs to "logs", R.string.title_activity_settings to "settings",
        )
        destinations.forEach { (label, callback) ->
            val text = context.getString(label)
            compose.onNodeWithText(text).performScrollTo().assertIsDisplayed().performClick()
            if (callback == "diagnostics") {
                compose.onNodeWithText(context.getString(R.string.menu_diagnostics_warning_title)).assertIsDisplayed()
                compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
            }
        }
        assertEquals(destinations.map { it.second }.filterNot { it == "diagnostics" }, called)
    }

    @Test fun firmwareIconIsAccessibleAndOnlyClickableWhenFirmwareExists() {
        var opened = 0
        var firmwareAvailable by mutableStateOf(false)
        var status by mutableStateOf(HomeConnectionStatus.CONNECTED)
        compose.setContent { HomeScreen(HomeScreenState(connectionStatus = status, firmwareAvailable = firmwareAvailable), HomeScreenActions(onFirmware = { opened++ })) }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected)).assertIsDisplayed().performClick()
        assertEquals(0, opened)
        compose.runOnIdle { firmwareAvailable = true }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected_firmware)).performClick()
        assertEquals(1, opened)
        compose.runOnIdle { status = HomeConnectionStatus.CONNECTING }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connecting)).assertIsDisplayed()
    }

    @Test fun exitConfirmationSupportsCancelAndConfirm() {
        var exits = 0
        compose.setContent { HomeScreen(HomeScreenState(connectionRunning = true), HomeScreenActions(onExitConfirmed = { exits++ })) }
        compose.onNodeWithText(context.getString(R.string.exit)).performClick()
        compose.onNodeWithText(context.getString(R.string.disconnect_device_msg)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        assertEquals(0, exits)
        compose.onNodeWithText(context.getString(R.string.exit)).performClick()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(1, exits)
    }

    @Test fun diagnosticsWarningConfirmationDelegatesCurrentFirmwareGateToFragment() {
        var requested = 0
        compose.setContent { HomeScreen(HomeScreenState(), HomeScreenActions(onDiagnosticsConfirmed = { requested++ })) }
        compose.onNodeWithText(context.getString(R.string.title_activity_diagnostics)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(1, requested)
        compose.onNodeWithText(context.getString(R.string.diagnostics_not_supported_title)).assertDoesNotExist()
    }

    @Test fun releaseDialogOffersDownloadNotesAndRemindChoices() {
        val choices = mutableListOf<String>()
        var releaseName by mutableStateOf<String?>("v1")
        compose.setContent { HomeScreen(HomeScreenState(releaseName = releaseName, releaseMessage = "Release v1"), HomeScreenActions(onDownloadRelease = { choices += "download" }, onReleaseNotes = { choices += "notes" })) }
        compose.onNodeWithText("Release v1").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.download)).performClick()
        compose.runOnIdle { releaseName = null }
        compose.runOnIdle { releaseName = "v1" }
        compose.onNodeWithText(context.getString(R.string.what_s_new)).performClick()
        compose.runOnIdle { releaseName = null }
        compose.runOnIdle { releaseName = "v1" }
        compose.onNodeWithText(context.getString(R.string.remind_me_later)).performClick()
        compose.onNodeWithText(context.getString(R.string.new_version_available)).assertDoesNotExist()
        assertEquals(listOf("download", "notes"), choices)
    }

    @Test fun layoutUsesGridForPortraitAndTabletAndScrollingColumnsForLandscape() {
        val config = Configuration()
        config.orientation = Configuration.ORIENTATION_PORTRAIT
        assertEquals(HomeLayout.GRID, homeLayout(config))
        config.screenWidthDp = 720
        assertEquals(HomeLayout.GRID, homeLayout(config))
        config.orientation = Configuration.ORIENTATION_LANDSCAPE
        assertEquals(HomeLayout.LANDSCAPE_COLUMNS, homeLayout(config))

        var layout by mutableStateOf(HomeLayout.GRID)
        compose.setContent { HomeScreen(HomeScreenState(), HomeScreenActions(), layoutOverride = layout) }
        val dashboardGrid = compose.onNodeWithTag("home-card-dashboard").fetchSemanticsNode().boundsInRoot
        val sensorsGrid = compose.onNodeWithTag("home-card-sensors").fetchSemanticsNode().boundsInRoot
        assertTrue("Dashboard is full width above the two-column grid", dashboardGrid.width > sensorsGrid.width)
        assertTrue("Grid cards are below the dashboard", sensorsGrid.top > dashboardGrid.top)

        compose.runOnIdle { layout = HomeLayout.LANDSCAPE_COLUMNS }
        val dashboardRow = compose.onNodeWithTag("home-card-dashboard").fetchSemanticsNode().boundsInRoot
        val sensorsRow = compose.onNodeWithTag("home-card-sensors").fetchSemanticsNode().boundsInRoot
        assertTrue("Landscape dashboard is taller than a pair card", dashboardRow.height > sensorsRow.height)
        assertTrue("Landscape dashboard precedes sensor column", sensorsRow.left > dashboardRow.left)
        assertTrue("Sensor and parameter cards stack vertically", compose.onNodeWithTag("home-card-parameters").fetchSemanticsNode().boundsInRoot.top > sensorsRow.top)
        assertTrue("Errors are in the second pair column", compose.onNodeWithTag("home-card-errors").fetchSemanticsNode().boundsInRoot.left > sensorsRow.left)
        assertTrue("Logs are in the third pair column", compose.onNodeWithTag("home-card-logs").fetchSemanticsNode().boundsInRoot.left > compose.onNodeWithTag("home-card-errors").fetchSemanticsNode().boundsInRoot.left)
    }

    @Test fun landscapeScrollReachesSettingsAndCardsRemainClickable() {
        val called = mutableListOf<String>()
        compose.setContent {
            HomeScreen(HomeScreenState(), HomeScreenActions(onSettings = { called += "settings" }), layoutOverride = HomeLayout.LANDSCAPE_COLUMNS)
        }
        compose.onNodeWithTag("home-card-settings").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(listOf("settings"), called)
    }

    @Test fun safeDrawingInsetsKeepControlsAndMenuViewportInsideTheUsableArea() {
        var layout by mutableStateOf(HomeLayout.GRID)
        val topInset = with(compose.density) { 28.dp.roundToPx() }
        val bottomInset = with(compose.density) { 36.dp.roundToPx() }
        val testInsets = WindowInsets(left = 0, top = topInset, right = 0, bottom = bottomInset)
        compose.setContent {
            HomeScreen(HomeScreenState(), HomeScreenActions(), layoutOverride = layout, contentInsets = testInsets)
        }

        fun assertContentWithinInsets() {
            val rootHeight = compose.onRoot().fetchSemanticsNode().boundsInRoot.height
            listOf("home-exit-button", "home-ecu-status", "home-menu-viewport").forEach { tag ->
                val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
                assertTrue("$tag starts below the status bar", bounds.top >= topInset - 1f)
                assertTrue("$tag ends above the navigation bar", bounds.bottom <= rootHeight - bottomInset + 1f)
            }
        }

        assertContentWithinInsets()
        compose.runOnIdle { layout = HomeLayout.LANDSCAPE_COLUMNS }
        assertContentWithinInsets()
        listOf("dashboard", "sensors", "parameters", "errors", "diagnostics", "logs", "settings").forEach { destination ->
            val card = compose.onNodeWithTag("home-card-$destination").fetchSemanticsNode().boundsInRoot
            val icon = compose.onNodeWithTag("home-icon-$destination", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val label = compose.onNodeWithTag("home-label-$destination", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("$destination icon fits inside its landscape card", icon.top >= card.top && icon.bottom <= card.bottom)
            assertTrue("$destination label fits inside its landscape card", label.top >= card.top && label.bottom <= card.bottom)
        }
    }

    @Test fun menuIconsKeepPortraitSizeAndAdaptToLandscapeColumns() {
        var layout by mutableStateOf(HomeLayout.GRID)
        val destinations = listOf("dashboard", "sensors", "parameters", "errors", "diagnostics", "logs", "settings")
        val expectedIconSizePx = with(compose.density) { 96.dp.toPx() }
        compose.setContent { HomeScreen(HomeScreenState(), HomeScreenActions(), layoutOverride = layout) }

        fun assertAllIconFramesHaveConsistentReadableSize() {
            destinations.forEach { destination ->
                val icon = compose.onNodeWithTag("home-icon-$destination", useUnmergedTree = true)
                icon.performScrollTo()
                val bounds = icon.fetchSemanticsNode().boundsInRoot
                assertTrue("$destination icon frame remains visible", bounds.width > 0f && bounds.height > 0f)
                if (layout == HomeLayout.GRID) {
                    assertTrue("Portrait icon frame width stays 96dp", kotlin.math.abs(bounds.width - expectedIconSizePx) < 1f)
                    assertTrue("Portrait icon frame height stays 96dp", kotlin.math.abs(bounds.height - expectedIconSizePx) < 1f)
                }
            }
        }

        assertAllIconFramesHaveConsistentReadableSize()
        compose.runOnIdle { layout = HomeLayout.LANDSCAPE_COLUMNS }
        assertAllIconFramesHaveConsistentReadableSize()
    }
}
