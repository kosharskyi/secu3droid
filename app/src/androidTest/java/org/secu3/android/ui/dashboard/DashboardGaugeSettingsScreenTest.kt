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

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.models.DashboardGaugeStyle
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.utils.prefs.UserPrefs
import java.io.File

class DashboardGaugeSettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val catalog = SensorPresentationCatalog()

    @Before
    fun enterLandscape() {
        composeRule.activityRule.scenario.onActivity {
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val timeoutAt = SystemClock.elapsedRealtime() + 10_000L
        while (SystemClock.elapsedRealtime() < timeoutAt) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) return
            SystemClock.sleep(50L)
        }
        error("Compose test host did not reach landscape orientation")
    }

    @After
    fun restoreOrientation() {
        composeRule.activityRule.scenario.onActivity {
            it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @Test
    fun ordinaryRowsShowCurrentChoicesAndResetSavesBothDefaults() {
        var saved: Triple<GaugeType, GaugeType, DashboardGaugeStyle>? = null
        setScreen(
            state = DashboardGaugeSettingsUiState(
                leftGauge = GaugeType.MAF,
                rightGauge = GaugeType.ADD1,
                gaugeStyle = DashboardGaugeStyle.FilledArc,
                settingsLoaded = true,
            ),
            onSave = { left, right, style -> saved = Triple(left, right, style) },
        )

        composeRule.onNodeWithTag("dashboard-gauge-side-left-row")
            .assertTextContains(composeRule.activity.getString(catalog.gauge(GaugeType.MAF).title))
        composeRule.onNodeWithTag("dashboard-gauge-side-right-row")
            .assertTextContains(composeRule.activity.getString(catalog.gauge(GaugeType.ADD1).title))
        composeRule.onNodeWithTag("dashboard-gauge-reset-row").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-save").performClick()

        assertEquals(
            Triple(UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE, UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE, DashboardGaugeStyle.Default),
            saved,
        )
    }

    @Test
    fun dashboardStyleDialogOffersFiveChoicesAndSavesDraftSelection() {
        var savedStyle: DashboardGaugeStyle? = null
        setScreen(
            state = DashboardGaugeSettingsUiState(settingsLoaded = true),
            onSave = { _, _, style -> savedStyle = style },
        )

        composeRule.onNodeWithTag("dashboard-gauge-style-row").performClick()
        DashboardGaugeStyle.entries.forEach { style ->
            composeRule.onNodeWithTag("dashboard-gauge-style-${style.preferenceValue}").assertIsDisplayed()
        }
        captureSettingsScreenshot("dashboard-gauge-style-choices.png")
        composeRule.onNodeWithTag("dashboard-gauge-style-filled_arc").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-save").performClick()
        assertEquals(DashboardGaugeStyle.FilledArc, savedStyle)
    }

    @Test
    fun selectorEditsOneDraftAndCancelDiscardsWithoutWriting() {
        var saveCalls = 0
        var navigatedUp = false
        setScreen(
            state = DashboardGaugeSettingsUiState(settingsLoaded = true),
            onSave = { _, _, _ -> saveCalls++ },
            onNavigateUp = { navigatedUp = true },
        )

        composeRule.onNodeWithTag("dashboard-gauge-style-row").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-style-filled_arc").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard-gauge-side-left-row").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-gauge-selector")
            .performScrollToNode(hasTestTag("dashboard-gauge-option-ADD1"))
        composeRule.onNodeWithTag("dashboard-gauge-option-ADD1").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-side-left-row")
            .assertTextContains(composeRule.activity.getString(catalog.gauge(GaugeType.ADD1).title))
        composeRule.onNodeWithTag("dashboard-gauge-cancel").performClick()

        assertEquals(0, saveCalls)
        assertEquals(true, navigatedUp)
    }

    @Test
    fun failedSaveScrollsMainSettingsErrorIntoView() {
        val state = mutableStateOf(DashboardGaugeSettingsUiState(settingsLoaded = true))
        composeRule.setContent {
            SecuComposeTheme {
                Box(Modifier.width(480.dp).height(320.dp)) {
                    DashboardGaugeSettingsScreen(
                        state = state.value,
                        presentationCatalog = catalog,
                        onSave = { _, _, _ -> },
                        onNavigateUp = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("dashboard-gauge-settings-screen")
            .performScrollToNode(hasTestTag("dashboard-gauge-reset-row"))
        composeRule.runOnIdle {
            state.value = state.value.copy(saveStatus = DashboardGaugeSaveStatus.Failed)
        }
        composeRule.onNodeWithTag("dashboard-gauge-storage-error").assertIsDisplayed()
    }

    @Test
    fun savingBlocksToolbarAndSystemBack() {
        var navigatedUp = false
        setScreen(
            state = DashboardGaugeSettingsUiState(
                settingsLoaded = true,
                saveStatus = DashboardGaugeSaveStatus.Saving,
            ),
            onNavigateUp = { navigatedUp = true },
        )

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("dashboard-gauge-cancel").performClick()
        assertFalse("both Back paths stay guarded during persistence", navigatedUp)
    }

    @Test
    fun selectorSearchAndDraftSurviveStateRestorationThenSaveAsPair() {
        var savedSettings: Triple<GaugeType, GaugeType, DashboardGaugeStyle>? = null
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            SecuComposeTheme {
                DashboardGaugeSettingsScreen(
                    state = DashboardGaugeSettingsUiState(settingsLoaded = true),
                    presentationCatalog = catalog,
                    onSave = { left, right, style -> savedSettings = Triple(left, right, style) },
                    onNavigateUp = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard-gauge-settings-screen")
            .performScrollToNode(hasTestTag("dashboard-gauge-style-row"))
        composeRule.onNodeWithTag("dashboard-gauge-style-row").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-style-filled_arc").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard-gauge-side-left-row").performClick()
        val voltageLabel = composeRule.activity.getString(catalog.gauge(GaugeType.VOLTAGE).title)
        composeRule.onNodeWithTag("dashboard-gauge-search").performTextInput(voltageLabel)
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("dashboard-gauge-style-row")
            .assertTextContains(composeRule.activity.getString(R.string.dashboard_gauge_style_filled_arc))
        composeRule.onNodeWithTag("dashboard-gauge-search").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-gauge-search").assertTextContains(voltageLabel)
        composeRule.onNodeWithTag("dashboard-gauge-search").performTextReplacement("")

        composeRule.onNodeWithTag("dashboard-gauge-selector")
            .performScrollToNode(hasTestTag("dashboard-gauge-option-VOLTAGE"))
        composeRule.onNodeWithTag("dashboard-gauge-option-VOLTAGE").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-side-right-row").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-selector")
            .performScrollToNode(hasTestTag("dashboard-gauge-option-MAF"))
        composeRule.onNodeWithTag("dashboard-gauge-option-MAF").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-save").performClick()

        assertEquals(Triple(GaugeType.VOLTAGE, GaugeType.MAF, DashboardGaugeStyle.FilledArc), savedSettings)
    }

    @Test
    fun compactLandscapeLargeTextKeepsRowsAndSelectorSearchAvailable() {
        val originalDensity = composeRule.activity.resources.displayMetrics.density
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(originalDensity, 1.5f)) {
                SecuComposeTheme {
                    Box(Modifier.width(568.dp).height(320.dp)) {
                        DashboardGaugeSettingsScreen(
                            state = DashboardGaugeSettingsUiState(settingsLoaded = true),
                            presentationCatalog = catalog,
                            onSave = { _, _, _ -> },
                            onNavigateUp = {},
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithTag("dashboard-gauge-save").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-gauge-side-left-row").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-gauge-side-right-row").performClick()
        val selector = composeRule.onNodeWithTag("dashboard-gauge-selector")
        selector.assertIsDisplayed()
        val selectorWidthPx = selector.fetchSemanticsNode().boundsInRoot.width
        val windowWidthPx = composeRule.activity.window.decorView.width.toFloat()
        val insetTolerancePx = 48f * originalDensity
        assertTrue(
            "selector width $selectorWidthPx px should span the $windowWidthPx px window",
            selectorWidthPx >= windowWidthPx - insetTolerancePx,
        )
        val toolbarBottom = composeRule.onNodeWithTag("dashboard-gauge-selector-toolbar")
            .fetchSemanticsNode().boundsInRoot.bottom
        val searchField = composeRule.onNodeWithTag("dashboard-gauge-search")
        val searchBounds = searchField.fetchSemanticsNode().boundsInRoot
        assertTrue("search must appear below the toolbar", searchBounds.top >= toolbarBottom)
        searchField.assertIsDisplayed()
        searchField.performClick()
        val voltageLabel = composeRule.activity.getString(catalog.gauge(GaugeType.VOLTAGE).title)
        searchField.performTextInput(voltageLabel)
        val selectorHeightPx = selector.fetchSemanticsNode().boundsInRoot.height
        assertTrue(
            "scrollable selector viewport should remain useful while IME is open; got $selectorHeightPx px",
            selectorHeightPx >= 46f * originalDensity,
        )
        selector.performScrollToNode(hasTestTag("dashboard-gauge-option-VOLTAGE"))
        composeRule.onNodeWithTag("dashboard-gauge-option-VOLTAGE").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard-gauge-option-VOLTAGE").performClick()
        composeRule.onNodeWithTag("dashboard-gauge-side-right-row")
            .assertTextContains(voltageLabel)
    }

    private fun setScreen(
        state: DashboardGaugeSettingsUiState,
        onSave: (GaugeType, GaugeType, DashboardGaugeStyle) -> Unit = { _, _, _ -> },
        onNavigateUp: () -> Unit = {},
    ) {
        composeRule.setContent {
            SecuComposeTheme {
                DashboardGaugeSettingsScreen(
                    state = state,
                    presentationCatalog = catalog,
                    onSave = onSave,
                    onNavigateUp = onNavigateUp,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun captureSettingsScreenshot(fileName: String) {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        if (androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(instrumentation.targetContext.cacheDir, fileName).outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
