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
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.ui.test.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class LambdaControlScreenTest {
    @get:Rule val compose = createComposeRule()
    private val sensors = listOf("NBO", "WBO")

    @Test fun loadingAndInitialRenderArePassiveAndUnknownSensorIsSafe() {
        var calls = 0
        val state = mutableStateOf<LambdaValues?>(null)
        compose.setContent { SecuComposeTheme { LambdaControlScreen(state.value, sensors, ParameterCapabilities(null), { calls++ }, { _, _ -> calls++ }, { calls++ }, { _, _ -> calls++ }) } }
        compose.onNodeWithTag("lambda_loading").assertExists()
        compose.runOnIdle { assertEquals(0, calls) }
        val values = LambdaValues.from(LambdaParamPacket(senstype = 7, strPerStp = 1))
        compose.runOnIdle { state.value = values }
        compose.onNodeWithTag("lambda_sensor_type").assertTextContains("Code 7")
        compose.onNodeWithTag("lambda_ms_disabled").assertExists()
        compose.onNodeWithTag("lambda_stoich_disabled").assertExists()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun explicitControlsInvokeCallbacksAndRespectFirmwareAndStrokeRules() {
        val edits = mutableListOf<LambdaNumber>()
        val flags = mutableListOf<Pair<LambdaFlag, Boolean>>()
        val channels = mutableListOf<Pair<Int, Boolean>>()
        val sensorsSelected = mutableListOf<Int>()
        compose.setContent {
            SecuComposeTheme {
                LambdaControlScreen(LambdaValues.from(LambdaParamPacket(senstype = 0, strPerStp = 0)), sensors, ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 28)),
                    edits::add, { flag, checked -> flags += flag to checked }, sensorsSelected::add, { index, checked -> channels += index to checked })
            }
        }
        compose.onNodeWithTag("lambda_ms").performScrollTo().performClick()
        assertEquals(listOf(LambdaNumber.MS), edits)
        compose.onNodeWithTag("lambda_stoich").performScrollTo().performClick()
        assertEquals(listOf(LambdaNumber.MS, LambdaNumber.STOICH), edits)
        compose.onNodeWithTag("lambda_before_cranking-switch").performScrollTo().performClick()
        assertEquals(listOf(LambdaFlag.BEFORE_CRANKING to true), flags)
        compose.onNodeWithTag("lambda_channel_8").performScrollTo().performClick()
        assertEquals(listOf(7 to true), channels)
        compose.onNodeWithTag("lambda_sensor_type").performScrollTo().performClick()
        compose.onNodeWithTag("lambda_sensor_option_1").performClick()
        assertEquals(listOf(1), sensorsSelected)
    }

    @Test fun carbAfrChannelsAreEnabledButStoichIsDisabled() {
        val capabilities = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 30))
        var channelCalls = 0
        var editCalls = 0
        compose.setContent {
            SecuComposeTheme {
                LambdaControlScreen(
                    LambdaValues.from(LambdaParamPacket()), sensors, capabilities,
                    { editCalls++ }, { _, _ -> }, {}, { _, _ -> channelCalls++ },
                )
            }
        }

        compose.onNodeWithTag("lambda_channel_1").assertIsEnabled().performScrollTo().performClick()
        compose.onNodeWithTag("lambda_stoich_disabled").assertExists()
        compose.runOnIdle {
            assertEquals(1, channelCalls)
            assertEquals(0, editCalls)
        }
    }

    @Test fun unsupportedAndUnknownFirmwareDisableChannelsAndStoichWithoutCallbacks() {
        val capabilities = mutableStateOf(ParameterCapabilities(FirmwareInfoPacket(options = 0)))
        var channelCalls = 0
        var editCalls = 0
        compose.setContent {
            SecuComposeTheme {
                LambdaControlScreen(
                    LambdaValues.from(LambdaParamPacket()), sensors, capabilities.value,
                    { editCalls++ }, { _, _ -> }, {}, { _, _ -> channelCalls++ },
                )
            }
        }

        (1..8).forEach { compose.onNodeWithTag("lambda_channel_$it").assertIsNotEnabled() }
        compose.onNodeWithTag("lambda_stoich_disabled").assertExists()
        compose.onNodeWithTag("lambda_channel_1").performScrollTo().performTouchInput { click(center) }
        compose.runOnIdle { capabilities.value = ParameterCapabilities(null) }
        (1..8).forEach { compose.onNodeWithTag("lambda_channel_$it").assertIsNotEnabled() }
        compose.onNodeWithTag("lambda_stoich_disabled").assertExists()
        compose.runOnIdle {
            assertEquals(0, channelCalls)
            assertEquals(0, editCalls)
        }
    }

    @Test fun gasDoseEnablesChannelsAndStoichThenFirmwareChangesDisableThem() {
        val capabilities = mutableStateOf(ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 29)))
        val channelCalls = mutableListOf<Pair<Int, Boolean>>()
        val edits = mutableListOf<LambdaNumber>()
        compose.setContent {
            SecuComposeTheme {
                LambdaControlScreen(
                    LambdaValues.from(LambdaParamPacket()), sensors, capabilities.value,
                    edits::add, { _, _ -> }, {}, { index, checked -> channelCalls += index to checked },
                )
            }
        }

        (1..8).forEach { compose.onNodeWithTag("lambda_channel_$it").assertIsEnabled() }
        compose.onNodeWithTag("lambda_stoich").performScrollTo().performClick()
        compose.onNodeWithTag("lambda_channel_1").performScrollTo().performClick()
        assertEquals(listOf(LambdaNumber.STOICH), edits)
        assertEquals(listOf(0 to true), channelCalls)

        compose.runOnIdle { capabilities.value = ParameterCapabilities(FirmwareInfoPacket(options = 0)) }
        (1..8).forEach { compose.onNodeWithTag("lambda_channel_$it").assertIsNotEnabled() }
        compose.onNodeWithTag("lambda_stoich_disabled").assertExists()
        compose.onNodeWithTag("lambda_channel_1").performScrollTo().performTouchInput { click(center) }
        compose.runOnIdle { capabilities.value = ParameterCapabilities(null) }
        (1..8).forEach { compose.onNodeWithTag("lambda_channel_$it").assertIsNotEnabled() }
        compose.onNodeWithTag("lambda_stoich_disabled").assertExists()
        assertEquals(1, channelCalls.size)
        assertEquals(1, edits.size)
    }
}
