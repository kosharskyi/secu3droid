package org.secu3.android.ui.firmware

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket

class FirmwareInfoSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadingStateShowsProgressAndHidesCapabilities() {
        composeTestRule.setContent {
            FirmwareInfoSheet(
                firmware = null,
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.firmware_information_title)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirmwareLoadingTag).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("OBD_SUPPORT").assertCountEquals(0)
    }

    @Test
    fun firmwareShowsTagAndCapabilitySections() {
        composeTestRule.setContent {
            FirmwareInfoSheet(
                firmware = FirmwareInfoPacket(
                    tag = "secu3t-v6.4.2+build.2311",
                    options = enabledOptions(COPT_OBD_SUPPORT, COPT_FUEL_INJECT),
                ),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("secu3t-v6.4.2+build.2311").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.firmware_enabled_capabilities)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.firmware_disabled_capabilities)).assertIsDisplayed()

        composeTestRule.onNodeWithTag("firmware-capability-OBD_SUPPORT-enabled").assertIsSelected()
        composeTestRule.onNodeWithTag("firmware-capability-FUEL_INJECTION-enabled").assertIsSelected()
        composeTestRule.onNodeWithTag("firmware-capability-GAS_DOSE-disabled").assertIsNotSelected()
        composeTestRule.onNodeWithTag("firmware-capability-ATMEGA1284-disabled").assertIsNotSelected()
    }

    @Test
    fun highestOptionBitIsDisplayedAsEnabled() {
        composeTestRule.setContent {
            FirmwareInfoSheet(
                firmware = FirmwareInfoPacket(options = 1 shl 31),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("firmware-capability-CKPS_NPLUS1-enabled").assertIsSelected()
        composeTestRule.onNodeWithTag("firmware-capability-CARB_AFR-disabled").assertIsNotSelected()
    }

    @Test
    fun fullFirmwareTagRemainsVisibleAtNarrowWidthAndLargeFont() {
        val configuration = Configuration(context.resources.configuration).apply { fontScale = 1.7f }
        val tag = "SECU3-" + "A".repeat(42)
        val largeFontDensity = Density(composeTestRule.density.density, fontScale = 1.7f)
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides largeFontDensity,
            ) {
                Box(Modifier.width(280.dp)) {
                    FirmwareInfoSheet(firmware = FirmwareInfoPacket(tag = tag), onDismiss = {})
                }
            }
        }

        val tagNode = composeTestRule.onNodeWithText(tag).assertIsDisplayed()
        var textLayout: TextLayoutResult? = null
        tagNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val results = mutableListOf<TextLayoutResult>()
            action(results)
            textLayout = results.firstOrNull()
        }
        val layout = requireNotNull(textLayout)
        assertTrue(
            "At enlarged font in 280dp, tag wraps beyond two lines (lines=${layout.lineCount})",
            layout.lineCount > 2,
        )
        assertTrue("Firmware tag has no clipped text", !layout.hasVisualOverflow)
        assertEquals(48, tag.length)
    }

    @Test
    fun closeButtonInvokesDismissCallback() {
        var dismissed = false

        composeTestRule.setContent {
            FirmwareInfoSheet(
                firmware = FirmwareInfoPacket(tag = "firmware"),
                onDismiss = { dismissed = true },
            )
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.close)).performClick()

        assertTrue(dismissed)
    }

    private fun enabledOptions(vararg bits: Int): Int {
        return bits.fold(0) { options, bit -> options or (1 shl bit) }
    }

    private companion object {
        const val COPT_OBD_SUPPORT = 0
        const val COPT_FUEL_INJECT = 28
    }
}
