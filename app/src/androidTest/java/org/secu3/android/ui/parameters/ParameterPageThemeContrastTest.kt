package org.secu3.android.ui.parameters

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.domain.parameters.StarterValues
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.pages.compose.StarterFlag
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import org.secu3.android.ui.parameters.pages.compose.StarterScreen

class ParameterPageThemeContrastTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun actualParametersScreenStarterTextStaysReadableInLightAndDarkModes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ukrainianConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale("uk"))
        }
        val ukrainianResources = context.createConfigurationContext(ukrainianConfiguration).resources
        val ukrainianContext = context.createConfigurationContext(ukrainianConfiguration)
        val dark = mutableStateOf(false)
        val starter = StarterValues(300, 400, 1.5f, .5f, 40, 20, 3f, 1.5f, 2f, 90f, 10, false, true)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides ukrainianContext,
                LocalConfiguration provides ukrainianConfiguration,
                LocalResources provides ukrainianResources,
            ) {
                SecuComposeTheme(darkTheme = dark.value) {
                    ParametersScreen(
                        titles = listOf(
                            androidx.compose.ui.res.stringResource(R.string.params_tab_start_title),
                            androidx.compose.ui.res.stringResource(R.string.params_tab_angles_title),
                        ),
                        connectionState = Connected,
                        onBack = {},
                        onSave = {},
                        onPageSettled = {},
                        pageContent = { index, _, _ ->
                            if (index == 0) {
                                StarterScreen(starter, onEdit = {}, onFlagChanged = { _, _ -> })
                            } else {
                                Box(Modifier.fillMaxSize())
                            }
                        },
                    )
                }
            }
        }

        captureStarterPage("light")
        assertReadableAgainst(android.graphics.Color.rgb(243, 246, 250))
        compose.runOnIdle { dark.value = true }
        captureStarterPage("dark")
        assertReadableAgainst(android.graphics.Color.rgb(17, 24, 32))
    }

    private fun captureStarterPage(theme: String) {
        compose.onNodeWithTag("starter_crank_to_run_cold").performScrollTo()
        captureDesignScreenshot("design-parameters-$theme-uk.png")
        compose.onNodeWithTag("starter_flag_row_limit_cranking_pw").performScrollTo()
        captureDesignScreenshot("design-parameters-$theme-bottom-uk.png")
        compose.onNodeWithTag("starter_crank_to_run_cold").performScrollTo()
    }

    private fun assertReadableAgainst(background: Int) {
        val value = compose.onNodeWithTag("starter_crank_to_run_cold-value", useUnmergedTree = true)
        // Capture the unscrolled real Parameters layout first, even if device metrics place this row below the fold.
        val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val bounds = value.fetchSemanticsNode().boundsInRoot
        value.performScrollTo().assertIsDisplayed()
        val visibleBounds = value.fetchSemanticsNode().boundsInRoot
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val left = visibleBounds.left.toInt().coerceIn(0, bitmap.width)
        val right = visibleBounds.right.toInt().coerceIn(0, bitmap.width)
        val top = visibleBounds.top.toInt().coerceIn(0, bitmap.height)
        val bottom = visibleBounds.bottom.toInt().coerceIn(0, bitmap.height)
        val contrastingPixels = bitmap.countContrastingPixels(background, left, top, right, bottom)
        assertTrue(
            "Expected real Starter page value text to contrast with its themed background; found $contrastingPixels readable pixels (before scroll bounds=$bounds, root=$rootBounds)",
            contrastingPixels > 5,
        )
    }

    private fun Bitmap.countContrastingPixels(background: Int, left: Int, top: Int, right: Int, bottom: Int): Int {
        var count = 0
        for (y in top until bottom) {
            for (x in left until right) {
                val pixel = getPixel(x, y)
                if (ColorUtils.calculateContrast(pixel, background) >= 4.5) count++
            }
        }
        return count
    }

    private fun captureDesignScreenshot(fileName: String) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDirectory = context.getExternalFilesDir(null) ?: context.cacheDir
        File(outputDirectory, fileName).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
