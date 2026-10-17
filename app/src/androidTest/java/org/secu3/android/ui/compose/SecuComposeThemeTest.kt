package org.secu3.android.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SecuComposeThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun explicitLightAndDarkSchemesUseTheDesignColorsAndSemanticStates() {
        val dark = mutableStateOf(false)
        var snapshot = Snapshot()
        compose.setContent {
            SecuComposeTheme(darkTheme = dark.value) {
                val scheme = MaterialTheme.colorScheme
                val palette = LocalSecuPalette.current
                val contentColor = LocalContentColor.current.toArgb()
                SideEffect {
                    snapshot = Snapshot(
                        background = scheme.background.toArgb(),
                        contentColor = contentColor,
                        primary = scheme.primary.toArgb(),
                        onSurface = scheme.onSurface.toArgb(),
                        surface = scheme.surface.toArgb(),
                        onSurfaceVariant = scheme.onSurfaceVariant.toArgb(),
                        surfaceVariant = scheme.surfaceVariant.toArgb(),
                        successContainer = palette.successContainer.toArgb(),
                        onSuccessContainer = palette.onSuccessContainer.toArgb(),
                        warningContainer = palette.warningContainer.toArgb(),
                        onWarningContainer = palette.onWarningContainer.toArgb(),
                    )
                }
            }
        }
        compose.runOnIdle {
            assertEquals(android.graphics.Color.rgb(243, 246, 250), snapshot.background)
            assertEquals(android.graphics.Color.rgb(21, 35, 49), snapshot.contentColor)
            assertEquals(android.graphics.Color.rgb(23, 105, 170), snapshot.primary)
            assertContrast(snapshot.onSurface, snapshot.surface)
            assertContrast(snapshot.onSurfaceVariant, snapshot.surfaceVariant)
            assertContrast(snapshot.onSuccessContainer, snapshot.successContainer)
            assertContrast(snapshot.onWarningContainer, snapshot.warningContainer)
        }

        compose.runOnIdle { dark.value = true }
        compose.runOnIdle {
            assertEquals(android.graphics.Color.rgb(17, 24, 32), snapshot.background)
            assertEquals(android.graphics.Color.rgb(238, 244, 248), snapshot.contentColor)
            assertEquals(android.graphics.Color.rgb(121, 187, 241), snapshot.primary)
            assertContrast(snapshot.onSurface, snapshot.surface)
            assertContrast(snapshot.onSurfaceVariant, snapshot.surfaceVariant)
            assertContrast(snapshot.onSuccessContainer, snapshot.successContainer)
            assertContrast(snapshot.onWarningContainer, snapshot.warningContainer)
        }
    }

    @Test
    fun nestedDefaultThemeInheritsRootAppearance() {
        var background = 0
        var contentColor = 0
        compose.setContent {
            SecuComposeTheme(darkTheme = true) {
                SecuComposeTheme {
                    val color = MaterialTheme.colorScheme.background.toArgb()
                    val defaultContent = LocalContentColor.current.toArgb()
                    SideEffect {
                        background = color
                        contentColor = defaultContent
                    }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(android.graphics.Color.rgb(17, 24, 32), background)
            assertEquals(android.graphics.Color.rgb(238, 244, 248), contentColor)
        }
    }

    private fun assertContrast(foreground: Int, background: Int) {
        check(ColorUtils.calculateContrast(foreground, background) >= 4.5) {
            "Expected readable contrast, got ${ColorUtils.calculateContrast(foreground, background)}"
        }
    }

    private data class Snapshot(
        val background: Int = 0,
        val contentColor: Int = 0,
        val primary: Int = 0,
        val onSurface: Int = 0,
        val surface: Int = 0,
        val onSurfaceVariant: Int = 0,
        val surfaceVariant: Int = 0,
        val successContainer: Int = 0,
        val onSuccessContainer: Int = 0,
        val warningContainer: Int = 0,
        val onWarningContainer: Int = 0,
    )
}
