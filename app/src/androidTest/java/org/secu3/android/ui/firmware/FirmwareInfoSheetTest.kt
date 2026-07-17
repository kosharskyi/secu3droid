package org.secu3.android.ui.firmware

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
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
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

        composeTestRule.onNodeWithTag(firmwareCapabilityChipTag("OBD_SUPPORT", enabled = true)).assertIsSelected()
        composeTestRule.onNodeWithTag(firmwareCapabilityChipTag("FUEL_INJECTION", enabled = true)).assertIsSelected()
        composeTestRule.onNodeWithTag(firmwareCapabilityChipTag("GAS_DOSE", enabled = false)).assertIsNotSelected()
        composeTestRule.onNodeWithTag(firmwareCapabilityChipTag("ATMEGA1284", enabled = false)).assertIsNotSelected()
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
