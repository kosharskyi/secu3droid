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
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
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
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.InProgress
import org.secu3.android.ui.compose.SecuComposeTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import java.io.File
import java.util.Locale

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun displaysAllMenuDestinationsAndInvokesTheirCallbacks() {
        val called = mutableListOf<String>()
        var screen by mutableStateOf(HomeScreenState())
        compose.setContent { HomeScreen(screen, HomeScreenActions(
            onDashboard = { called += "dashboard" }, onSensors = { called += "sensors" },
            onParameters = { called += "parameters" }, onErrors = { called += "errors" },
            onLogs = { called += "logs" }, onSettings = { called += "settings" },
            onDiagnosticsRequested = { screen = screen.copy(dialog = HomeDialog.DIAGNOSTICS) },
            onDialogDismissed = { screen = screen.copy(dialog = null) },
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
        var status by mutableStateOf<ConnectionState>(Connected)
        compose.setContent { HomeScreen(HomeScreenState(connectionStatus = status, firmwareAvailable = firmwareAvailable), HomeScreenActions(onFirmware = { opened++ })) }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected)).assertIsDisplayed().performClick()
        assertEquals(0, opened)
        compose.runOnIdle { firmwareAvailable = true }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected_firmware)).performClick()
        assertEquals(1, opened)
        compose.runOnIdle { status = InProgress }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connecting)).assertIsDisplayed()
    }

    @Test fun exitConfirmationSupportsCancelAndConfirm() {
        var exits = 0
        var screen by mutableStateOf(HomeScreenState(connectionRunning = true))
        compose.setContent { HomeScreen(screen, HomeScreenActions(onExitRequested = { screen = screen.copy(dialog = HomeDialog.EXIT) }, onExitCancelled = { screen = screen.copy(dialog = null) }, onExitConfirmed = { exits++; screen = screen.copy(dialog = null) })) }
        compose.onNodeWithText(context.getString(R.string.exit)).performClick()
        compose.onNodeWithText(context.getString(R.string.disconnect_device_msg)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        assertEquals(0, exits)
        compose.onNodeWithText(context.getString(R.string.exit)).performClick()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(1, exits)
    }

    @Test fun confirmedUsbHidesExitAndKeepsFeatureNavigationAvailable() {
        var openedSensors = 0
        var locked by mutableStateOf(true)
        compose.setContent {
            HomeScreen(
                HomeScreenState(connectionStatus = Connected, connectionRunning = true, usbSessionLocked = locked),
                HomeScreenActions(onSensors = { openedSensors++ }),
            )
        }
        compose.onNodeWithTag("home-exit-button").assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.sensors)).performScrollTo().performClick()
        assertEquals(1, openedSensors)
        compose.runOnIdle { locked = false }
        compose.onNodeWithTag("home-exit-button").assertIsDisplayed()
    }

    @Test fun diagnosticsWarningConfirmationDelegatesThroughCallback() {
        var requested = 0
        var screen by mutableStateOf(HomeScreenState())
        compose.setContent { HomeScreen(screen, HomeScreenActions(onDiagnosticsRequested = { screen = screen.copy(dialog = HomeDialog.DIAGNOSTICS) }, onDialogDismissed = { screen = screen.copy(dialog = null) }, onDiagnosticsConfirmed = { requested++ })) }
        compose.onNodeWithText(context.getString(R.string.title_activity_diagnostics)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(1, requested)
        compose.onNodeWithText(context.getString(R.string.diagnostics_not_supported_title)).assertDoesNotExist()
    }

    @Test fun releaseDialogOffersDownloadNotesAndRemindChoices() {
        val choices = mutableListOf<String>()
        var screen by mutableStateOf(HomeScreenState(dialog = HomeDialog.RELEASE, releaseMessage = "Release v1"))
        compose.setContent { HomeScreen(screen, HomeScreenActions(onDialogDismissed = { screen = screen.copy(dialog = null) }, onDownloadRelease = { choices += "download" }, onReleaseNotes = { choices += "notes" })) }
        compose.onNodeWithText("Release v1").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.download)).performClick()
        compose.runOnIdle { screen = screen.copy(dialog = HomeDialog.RELEASE) }
        compose.onNodeWithText(context.getString(R.string.what_s_new)).performClick()
        compose.runOnIdle { screen = screen.copy(dialog = HomeDialog.RELEASE) }
        compose.onNodeWithText(context.getString(R.string.remind_me_later)).performClick()
        compose.onNodeWithText(context.getString(R.string.new_version_available)).assertDoesNotExist()
        assertEquals(listOf("download", "notes"), choices)
    }

    @Test fun longReleaseMessageCanScrollToRemindAction() {
        val configuration = Configuration(context.resources.configuration).apply { fontScale = 1.6f }
        val largeFontDensity = Density(compose.density.density, fontScale = 1.6f)
        val releaseMessage = ("Long release notes describe compatibility, fixes, and installation details.\n\n").repeat(40)
        var dismissed = 0
        val screen = HomeScreenState(dialog = HomeDialog.RELEASE, releaseMessage = releaseMessage)
        compose.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides largeFontDensity,
            ) {
                HomeScreen(screen, HomeScreenActions(onDialogDismissed = { dismissed++ }))
            }
        }

        compose.onNodeWithText(context.getString(R.string.remind_me_later))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, dismissed)
    }

    @Test fun layoutUsesGridForPortraitAndTabletAndScrollingColumnsForLandscape() {
        var layout by mutableStateOf(HomeLayout.GRID)
        compose.setContent { HomeScreen(HomeScreenState(), HomeScreenActions(), layoutOverride = layout) }
        val dashboardGrid = compose.onNodeWithTag("home-card-dashboard").getUnclippedBoundsInRoot()
        val sensorsGrid = compose.onNodeWithTag("home-card-sensors").getUnclippedBoundsInRoot()
        assertTrue("Dashboard is full width above the two-column grid", (dashboardGrid.right - dashboardGrid.left) > (sensorsGrid.right - sensorsGrid.left))
        assertTrue("Grid cards are below the dashboard", sensorsGrid.top > dashboardGrid.top)

        compose.runOnIdle { layout = HomeLayout.LANDSCAPE_COLUMNS }
        val dashboardRow = compose.onNodeWithTag("home-card-dashboard").getUnclippedBoundsInRoot()
        val sensorsRow = compose.onNodeWithTag("home-card-sensors").getUnclippedBoundsInRoot()
        assertTrue("Landscape dashboard is taller than a pair card", (dashboardRow.bottom - dashboardRow.top) > (sensorsRow.bottom - sensorsRow.top))
        assertTrue("Landscape dashboard precedes sensor column", sensorsRow.left > dashboardRow.left)
        assertTrue("Sensor and parameter cards stack vertically", compose.onNodeWithTag("home-card-parameters").getUnclippedBoundsInRoot().top > sensorsRow.top)
        assertTrue("Errors are in the second pair column", compose.onNodeWithTag("home-card-errors").getUnclippedBoundsInRoot().left > sensorsRow.left)
        assertTrue("Logs are in the third pair column", compose.onNodeWithTag("home-card-logs").getUnclippedBoundsInRoot().left > compose.onNodeWithTag("home-card-errors").getUnclippedBoundsInRoot().left)
    }

    @Test fun configurationOrientationSelectsRenderedMenuLayout() {
        var configuration by mutableStateOf(Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
        })
        compose.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                HomeScreen(HomeScreenState(), HomeScreenActions())
            }
        }

        val dashboardGrid = compose.onNodeWithTag("home-card-dashboard").getUnclippedBoundsInRoot()
        val sensorsGrid = compose.onNodeWithTag("home-card-sensors").getUnclippedBoundsInRoot()
        assertTrue("Portrait dashboard is full width above the two-column grid", (dashboardGrid.right - dashboardGrid.left) > (sensorsGrid.right - sensorsGrid.left))
        assertTrue("Portrait grid cards are below the dashboard", sensorsGrid.top > dashboardGrid.top)

        compose.runOnIdle {
            configuration = Configuration(configuration).apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
            }
        }
        val dashboardColumns = compose.onNodeWithTag("home-card-dashboard").getUnclippedBoundsInRoot()
        val sensorsColumns = compose.onNodeWithTag("home-card-sensors").getUnclippedBoundsInRoot()
        assertTrue("Landscape dashboard precedes the sensor column", sensorsColumns.left > dashboardColumns.left)
        assertTrue("Landscape dashboard is taller than a pair card", (dashboardColumns.bottom - dashboardColumns.top) > (sensorsColumns.bottom - sensorsColumns.top))
    }

    @Test fun menuCardsUseCallbacksFromTheLatestActions() {
        val called = mutableListOf<String>()
        var onSettings by mutableStateOf<() -> Unit>({ called += "old" })
        compose.setContent {
            HomeScreen(HomeScreenState(), HomeScreenActions(onSettings = onSettings), layoutOverride = HomeLayout.GRID)
        }

        compose.runOnIdle { onSettings = { called += "latest" } }
        compose.onNodeWithTag("home-card-settings").performScrollTo().performClick()
        assertEquals(listOf("latest"), called)
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
                val bounds = compose.onNodeWithTag(tag, useUnmergedTree = tag == "home-ecu-status").fetchSemanticsNode().boundsInRoot
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

    @Test fun menuIconsKeepOriginalPortraitSizeAndAdaptToLandscapeColumns() {
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

    @Test fun ukrainianMenuLabelsFitOriginalCardsAndCaptureThemeVariants() {
        val ukrainianConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("uk"))
        }
        val ukrainianContext = context.createConfigurationContext(ukrainianConfiguration)
        var darkTheme by mutableStateOf(false)
        var layout by mutableStateOf(HomeLayout.GRID)
        val destinations = listOf(
            "dashboard" to R.string.menu_dashboard,
            "sensors" to R.string.sensors,
            "parameters" to R.string.menu_param,
            "errors" to R.string.title_activity_errors,
            "diagnostics" to R.string.title_activity_diagnostics,
            "logs" to R.string.menu_secu_logs,
            "settings" to R.string.title_activity_settings,
        )
        compose.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides ukrainianConfiguration,
                LocalContext provides ukrainianContext,
            ) {
                SecuComposeTheme(darkTheme = darkTheme) {
                    HomeScreen(HomeScreenState(), HomeScreenActions(), layoutOverride = layout)
                }
            }
        }

        fun assertLabelsFitCards() {
            destinations.forEach { (destination, labelResource) ->
                val labelText = ukrainianContext.getString(labelResource)
                val label = compose.onNodeWithTag("home-label-$destination", useUnmergedTree = true)
                    .performScrollTo()
                label.assertIsDisplayed()
                val cardBounds = compose.onNodeWithTag("home-card-$destination").fetchSemanticsNode().boundsInRoot
                val labelBounds = label.fetchSemanticsNode().boundsInRoot
                val localizedText = compose.onNodeWithText(labelText, useUnmergedTree = true)
                    .performScrollTo()
                localizedText.assertIsDisplayed()
                var textLayout: TextLayoutResult? = null
                var hasTextLayout = false
                var layoutCount = 0
                label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val layoutResults = mutableListOf<TextLayoutResult>()
                    hasTextLayout = action(layoutResults)
                    layoutCount = layoutResults.size
                    textLayout = layoutResults.firstOrNull()
                }
                assertTrue("$destination label has text layout", hasTextLayout)
                assertEquals("$destination label has one text layout", 1, layoutCount)
                val layout = requireNotNull(textLayout)
                val diagnostic = "$destination label overflow=${layout.hasVisualOverflow} " +
                    "width=${layout.didOverflowWidth} height=${layout.didOverflowHeight} " +
                    "lines=${layout.lineCount} layoutSize=${layout.size} " +
                    "fontSize=${layout.layoutInput.style.fontSize} fontScale=${ukrainianConfiguration.fontScale} " +
                    "card=$cardBounds label=$labelBounds"
                assertTrue(diagnostic, !layout.hasVisualOverflow)
                assertTrue("$destination label fits within original card width", labelBounds.left >= cardBounds.left && labelBounds.right <= cardBounds.right)
                assertTrue("$destination label fits within original card height", labelBounds.top >= cardBounds.top && labelBounds.bottom <= cardBounds.bottom)
            }
        }

        captureDesignScreenshot("regression-home-light-portrait.png")
        assertLabelsFitCards()
        compose.runOnIdle { darkTheme = true }
        compose.waitForIdle()
        captureDesignScreenshot("regression-home-dark-portrait.png")
        assertLabelsFitCards()
        compose.runOnIdle { darkTheme = false; layout = HomeLayout.LANDSCAPE_COLUMNS }
        compose.waitForIdle()
        captureDesignScreenshot("regression-home-light-columns-preview.png")
        assertLabelsFitCards()
    }

    private fun captureDesignScreenshot(fileName: String) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val outputDirectory = context.getExternalFilesDir(null) ?: context.cacheDir
        File(outputDirectory, fileName).outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
