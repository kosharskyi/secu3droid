package org.secu3.android.ui.compose

import android.content.res.Configuration
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R

class ConfirmationDialogTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun longLocalizedConfirmationWrapsAtNarrowWidthAndActionsRemainAvailableInBothThemes() {
        val configuration = Configuration(context.resources.configuration).apply { fontScale = 1.7f }
        val darkTheme = mutableStateOf(false)
        var confirms = 0
        var cancels = 0
        val title = context.getString(R.string.menu_diagnostics_warning_title)
        val message = context.getString(R.string.disconnect_device_msg)
        val largeFontDensity = Density(compose.density.density, fontScale = 1.7f)

        compose.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides largeFontDensity,
            ) {
                SecuComposeTheme(darkTheme = darkTheme.value) {
                    ConfirmationDialog(
                        title = R.string.menu_diagnostics_warning_title,
                        message = R.string.disconnect_device_msg,
                        onConfirm = { confirms++ },
                        onCancel = { cancels++ },
                        modifier = Modifier.width(280.dp),
                    )
                }
            }
        }

        repeat(2) {
            compose.onNodeWithText(title).assertIsDisplayed().assertCenteredAndUnclipped()
            compose.onNodeWithText(message).assertIsDisplayed()
            compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
            compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
            if (!darkTheme.value) darkTheme.value = true
            compose.waitForIdle()
        }

        assertEquals(2, cancels)
        assertEquals(2, confirms)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertCenteredAndUnclipped() {
        var layout: TextLayoutResult? = null
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val results = mutableListOf<TextLayoutResult>()
            action(results)
            layout = results.firstOrNull()
        }
        val textLayout = requireNotNull(layout)
        assertTrue("Long dialog title wraps", textLayout.lineCount > 1)
        assertTrue("Dialog title has no clipped text", !textLayout.hasVisualOverflow)
        assertEquals(TextAlign.Center, textLayout.layoutInput.style.textAlign)
    }
}
