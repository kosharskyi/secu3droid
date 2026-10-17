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
 */

package org.secu3.android.ui.start

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

class StartScreenContentTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun connectButtonReportsConnectAndCancelState() {
        val states = mutableListOf<Boolean>()
        var connecting by mutableStateOf(false)
        compose.setContent {
            StartScreenContent(StartScreenState(connecting = connecting), StartScreenActions(onConnectOrCancel = states::add))
        }
        compose.onNodeWithText(context.getString(R.string.connect)).assertIsDisplayed().performClick()
        compose.runOnIdle { connecting = true }
        compose.onNodeWithText(context.getString(R.string.status_connecting)).assertIsDisplayed().performClick()
        assertEquals(listOf(false, true), states)
    }

    @Test fun connectingButtonShowsUkrainianLabelAndContrastingAnimatedRingInBothThemes() {
        val ukrainianConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("uk"))
        }
        val ukrainianContext = context.createConfigurationContext(ukrainianConfiguration)
        var darkTheme by mutableStateOf(false)
        var isConnecting by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides ukrainianConfiguration,
                LocalContext provides ukrainianContext,
            ) {
                SecuComposeTheme(darkTheme = darkTheme) {
                    StartScreenContent(StartScreenState(connecting = isConnecting), StartScreenActions())
                }
            }
        }
        compose.mainClock.advanceTimeBy(60L)
        compose.waitForIdle()
        val connectingLabel = ukrainianContext.getString(R.string.status_connecting)
        val connectLabel = ukrainianContext.getString(R.string.connect)
        val initialBounds = compose.onNodeWithTag("start-connect-button").getUnclippedBoundsInRoot()
        compose.onNodeWithText(connectLabel).assertIsDisplayed()
        assertLabelFitsButton()
        assertButtonBoundsDoNotChangeWhenLabelChanges(initialBounds)

        compose.runOnIdle { isConnecting = true }
        compose.mainClock.advanceTimeBy(60L)
        compose.waitForIdle()
        compose.onNodeWithText(connectingLabel).assertIsDisplayed()
        assertLabelFitsButton()
        assertRingContrast()
        captureDesignScreenshot("regression-start-connecting-light.png")

        compose.runOnIdle { darkTheme = true; isConnecting = false }
        compose.mainClock.advanceTimeBy(60L)
        compose.waitForIdle()
        compose.onNodeWithText(connectLabel).assertIsDisplayed()
        assertLabelFitsButton()
        assertButtonBoundsDoNotChangeWhenLabelChanges(initialBounds)
        compose.runOnIdle { isConnecting = true }
        compose.mainClock.advanceTimeBy(60L)
        compose.waitForIdle()
        compose.onNodeWithText(connectingLabel).assertIsDisplayed()
        assertLabelFitsButton()
        assertRingContrast()
        captureDesignScreenshot("regression-start-connecting-dark.png")
    }

    private fun assertLabelFitsButton() {
        val button = compose.onNodeWithTag("start-connect-button").getUnclippedBoundsInRoot()
        val labelNode = compose.onNodeWithTag("start-connect-label", useUnmergedTree = true)
        val label = labelNode.getUnclippedBoundsInRoot()
        assertTrue(button.left <= label.left && label.right <= button.right)
        assertTrue(button.top <= label.top && label.bottom <= button.bottom)
        var layoutResult: TextLayoutResult? = null
        labelNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val layoutResults = mutableListOf<TextLayoutResult>()
            action(layoutResults)
            layoutResult = layoutResults.firstOrNull()
        }
        val result = requireNotNull(layoutResult) { "Start button label has no text layout" }
        assertTrue("Start button label has no clipped text", !result.hasVisualOverflow)
        assertEquals("Connect state labels remain whole on one line", 1, result.lineCount)
    }

    private fun assertButtonBoundsDoNotChangeWhenLabelChanges(previousBounds: androidx.compose.ui.unit.DpRect) {
        assertEquals(
            "The diameter stays fixed across normal and connecting labels",
            previousBounds,
            compose.onNodeWithTag("start-connect-button").getUnclippedBoundsInRoot(),
        )
    }

    @Test fun stateLabelsFitAndStayFixedInsideTheCompactViewportAtLargeFontScales() {
        val ukrainianConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("uk"))
        }
        val ukrainianContext = context.createConfigurationContext(ukrainianConfiguration)
        val density = mutableStateOf(Density(compose.density.density, fontScale = 1f))
        val connecting = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides ukrainianConfiguration,
                LocalContext provides ukrainianContext,
                LocalDensity provides density.value,
            ) {
                SecuComposeTheme {
                    StartScreenContent(
                        StartScreenState(connecting = connecting.value, usbDeviceDetected = true),
                        StartScreenActions(),
                        modifier = Modifier.size(width = 320.dp, height = 480.dp),
                    )
                }
            }
        }

        listOf(1f, 1.7f, 2f).forEach { scale ->
            compose.runOnIdle {
                density.value = Density(compose.density.density, fontScale = scale)
            }
            compose.waitForIdle()

            val button = compose.onNodeWithTag("start-connect-button").getUnclippedBoundsInRoot()
            assertTrue("Button fits its assigned compact slot at fontScale=$scale", button.right > button.left && button.bottom > button.top)
            val labels = listOf(
                ukrainianContext.getString(R.string.connect),
                ukrainianContext.getString(R.string.status_connecting),
            )
            var stableBounds: androidx.compose.ui.unit.DpRect? = null
            labels.forEachIndexed { index, label ->
                compose.runOnIdle { connecting.value = index == 1 }
                compose.waitForIdle()
                val labelNode = compose.onNodeWithTag("start-connect-label", useUnmergedTree = true)
                labelNode.assertIsDisplayed()
                var layout: TextLayoutResult? = null
                labelNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val results = mutableListOf<TextLayoutResult>()
                    action(results)
                    layout = results.firstOrNull()
                }
                val result = requireNotNull(layout)
                assertTrue("$label does not clip at fontScale=$scale", !result.hasVisualOverflow)
                assertEquals("$label remains on one line at fontScale=$scale", 1, result.lineCount)
                assertTrue("Font scaling remains enabled", result.layoutInput.density.fontScale >= scale)
                val currentBounds = compose.onNodeWithTag("start-connect-button").getUnclippedBoundsInRoot()
                if (stableBounds == null) stableBounds = currentBounds else assertEquals(stableBounds, currentBounds)
            }

            val logs = compose.onNodeWithContentDescription("Logs icon").getUnclippedBoundsInRoot()
            val settings = compose.onNodeWithContentDescription("Settings icon").getUnclippedBoundsInRoot()
            val usb = compose.onNodeWithText(ukrainianContext.getString(R.string.usb_device_detected)).getUnclippedBoundsInRoot()
            assertTrue("Circle clears top icons at fontScale=$scale", button.top >= logs.bottom && button.top >= settings.bottom)
            assertTrue("Circle clears USB hint at fontScale=$scale", button.bottom <= usb.top)
        }
    }

    private fun assertRingContrast() {
        val screenshot = compose.onRoot().captureToImage().toPixelMap()
        val bounds = compose.onNodeWithTag("start-connect-button").fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        val centerX = bounds.center.x.roundToInt()
        val centerY = bounds.center.y.roundToInt()
        val radius = bounds.width / 2f - 3f * density
        val progressAngle = 300f * PI.toFloat() / 180f
        val progressX = (centerX + radius * cos(progressAngle)).roundToInt().coerceIn(0, screenshot.width - 1)
        val progressY = (centerY + radius * sin(progressAngle)).roundToInt().coerceIn(0, screenshot.height - 1)
        val trackX = (centerX - radius).roundToInt().coerceIn(0, screenshot.width - 1)
        val progress = screenshot[progressX, progressY]
        val track = screenshot[trackX, centerY.coerceIn(0, screenshot.height - 1)]
        val background = screenshot[centerX.coerceIn(0, screenshot.width - 1), (centerY + radius * 0.5f).roundToInt().coerceIn(0, screenshot.height - 1)]

        assertTrue("Primary progress arc should remain distinct from the outline track", contrastRatio(progress, track) >= 1.5f)
        assertTrue("Primary progress arc should remain distinct from the screen background", contrastRatio(progress, background) >= 2f)
        assertTrue("Outline track should remain visible against the screen background", contrastRatio(track, background) >= 1.1f)
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        return ColorUtils.calculateContrast(first.toArgb(), second.toArgb()).toFloat()
    }

    private fun captureDesignScreenshot(fileName: String) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val outputDirectory = context.getExternalFilesDir(null) ?: context.cacheDir
        File(outputDirectory, fileName).outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test fun logsAndSettingsInvokeTheirCallbacks() {
        val calls = mutableListOf<String>()
        compose.setContent {
            StartScreenContent(StartScreenState(), StartScreenActions(onLogs = { calls += "logs" }, onSettings = { calls += "settings" }))
        }
        compose.onNodeWithContentDescription("Logs icon").performClick()
        compose.onNodeWithContentDescription("Settings icon").performClick()
        assertEquals(listOf("logs", "settings"), calls)
    }

    @Test fun usbHintAppearsOnlyWhenDeviceIsPresent() {
        var deviceDetected by mutableStateOf(true)
        compose.setContent { StartScreenContent(StartScreenState(usbDeviceDetected = deviceDetected), StartScreenActions()) }
        compose.onNodeWithText(context.getString(R.string.usb_device_detected)).assertIsDisplayed()
        compose.runOnIdle { deviceDetected = false }
        compose.onNodeWithText(context.getString(R.string.usb_device_detected)).assertDoesNotExist()
    }

    @Test fun mainControlsRemainVisibleInCompactViewport() {
        compose.setContent {
            StartScreenContent(
                StartScreenState(usbDeviceDetected = true),
                StartScreenActions(),
                modifier = Modifier.size(width = 320.dp, height = 480.dp),
            )
        }
        compose.onNodeWithText(context.getString(R.string.connect)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.usb_device_detected)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Logs icon").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings icon").assertIsDisplayed()
    }

    @Test fun bluetoothSheetShowsRowsSelectsAddressAndDismisses() {
        val selected = mutableListOf<String>()
        var dismissed = 0
        compose.setContent {
            StartScreenContent(
                StartScreenState(
                    showBluetoothDevices = true,
                    bluetoothDevices = listOf(
                        StartBluetoothDevice("AA:BB:CC:DD:EE:01", "ECU One"),
                        StartBluetoothDevice("AA:BB:CC:DD:EE:02", "ECU Two"),
                    ),
                ),
                StartScreenActions(onBluetoothDeviceSelected = selected::add, onBluetoothSheetDismissed = { dismissed++ }),
            )
        }
        compose.onNodeWithText(context.getString(R.string.select_a_device_from_the_list)).assertIsDisplayed()
        compose.onNodeWithText("ECU One").assertIsDisplayed()
        compose.onNodeWithText("AA:BB:CC:DD:EE:01").assertIsDisplayed().performClick()
        assertEquals(listOf("AA:BB:CC:DD:EE:01"), selected)
        assertEquals(0, dismissed)
    }

    @Test fun bluetoothSheetDismissCallsCleanupCallback() {
        var dismissed = 0
        compose.setContent {
            StartScreenContent(StartScreenState(showBluetoothDevices = true), StartScreenActions(onBluetoothSheetDismissed = { dismissed++ }))
        }
        compose.onNodeWithText(context.getString(R.string.select_a_device_from_the_list)).performTouchInput { swipeDown() }
        compose.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test fun permissionRationaleOffersConfirmAndCancelCallbacks() {
        val calls = mutableListOf<String>()
        compose.setContent {
            StartScreenContent(StartScreenState(showBluetoothRationale = true), StartScreenActions(
                onRationaleConfirmed = { calls += "confirm" }, onRationaleCancelled = { calls += "cancel" },
            ))
        }
        compose.onNodeWithText(context.getString(R.string.permission_rationale_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.onNodeWithText(context.getString(R.string.permission_rationale_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(listOf("cancel", "confirm"), calls)
    }
}
