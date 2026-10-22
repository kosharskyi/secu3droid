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
package org.secu3.android.ui.diagnostics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import org.secu3.android.R
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket

class DiagnosticsInputsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun valuesAndReadOnlyStatusesAreShownAndSecu3iFieldsAreConditional() {
        val input = DiagInputPacket(voltage = 12.345f, map = 1.25f, addI8 = 8.5f, bits = 0b1010101010)
        var firmware by mutableStateOf<Boolean?>(false)
        compose.setContent { DiagnosticsInputsScreen(input, firmware, 0, 10f, 50f, {}, {}, {}) }
        compose.onNodeWithText("12.345 V").assertIsDisplayed()
        compose.onNodeWithTag("analog_ADD_IO8").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("status_GPA4_I").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("output_channel").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("frequency_edit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("duty_edit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("COND_I").performScrollTo().assertIsDisplayed()

        compose.runOnIdle { firmware = true }
        compose.onAllNodesWithTag("analog_ADD_IO8").assertCountEquals(0)
        compose.onAllNodesWithTag("status_GPA4_I").assertCountEquals(0)
        compose.onAllNodesWithTag("status_COND_I").assertCountEquals(0)
        compose.onAllNodesWithTag("analog_ADD_IO3").assertCountEquals(0)
    }

    @Test fun familyLabelsAndAdditionalInputsFollowFirmwareCapabilities() {
        var input by mutableStateOf<DiagInputPacket?>(DiagInputPacket(map = 1.25f, carb = 2.5f, addI3 = 3.75f, addI4 = 0f))
        var firmware by mutableStateOf<Boolean?>(false)
        var hasTpic8101 by mutableStateOf<Boolean?>(false)
        compose.setContent {
            DiagnosticsInputsScreen(
                input = input,
                isSecu3T = firmware,
                selectedChannel = 0,
                frequency = 10f,
                duty = 50f,
                onChannelSelected = {},
                onFrequencyEdit = {},
                onDutyEdit = {},
                hasTpic8101 = hasTpic8101,
            )
        }

        compose.onNodeWithTag("analog_MAP_I").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analog_TPS").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analog_ADD_IO3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("— V")

        compose.runOnIdle { hasTpic8101 = true }
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("0.000 V")
        compose.runOnIdle { input = null }
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("— V")
        compose.runOnIdle { input = DiagInputPacket(map = 1.25f, carb = 2.5f, addI3 = 3.75f, addI4 = 0f) }
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("0.000 V")
        compose.runOnIdle { hasTpic8101 = false }
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("— V")

        compose.runOnIdle { firmware = true; hasTpic8101 = true }
        compose.onNodeWithTag("analog_MAP_S").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analog_CARB").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("analog_ADD_IO3").assertCountEquals(0)
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("0.000 V")

        compose.runOnIdle { firmware = null }
        compose.onNodeWithTag("analog_MAP_S").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analog_CARB").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analog_ADD_IO3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("— V")
    }

    @Test fun absentPacketIsUnavailableButARealZeroAndFalseRemainValidReadouts() {
        var packet by mutableStateOf<DiagInputPacket?>(null)
        compose.setContent { DiagnosticsInputsScreen(packet, false, 0, 10f, 50f, {}, {}, {}) }

        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("— V")
        compose.onNodeWithTag("status_GAS_V").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.sensors_status_accessibility_unavailable),
            ),
        )
        compose.onNodeWithTag("status_GAS_V-value").assertTextEquals(context.getString(R.string.sensors_status_accessibility_unavailable))

        packet = DiagInputPacket(voltage = 0f)
        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("0.000 V")
        compose.onNodeWithTag("status_GAS_V").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.sensors_status_accessibility_off),
            ),
        )
        compose.onNodeWithTag("status_GAS_V-value").assertTextEquals(context.getString(R.string.sensors_status_accessibility_off))
    }

    @Test fun secu3TChannelSelectionSendsSameIndexAsProtocolChannelAndRenderIsSilent() {
        assertEquals(listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IE", "FE", "ECF", "CE", "ST_BLOCK", "ADD_IO1", "ADD_IO2", "BL", "DE"), secu3TOutputChannels)
        val packet = DiagOutputPacket()
        var sendCount = 0
        var current by mutableStateOf(0)
        compose.setContent {
            DiagnosticsInputsScreen(null, true, current, 10f, 50f, { current = it; selectDiagnosticOutputChannel(packet, it) { sendCount++ } }, { sendCount++ }, { sendCount++ }, controlsEnabled = true)
        }
        compose.runOnIdle { assertEquals(0, sendCount) }
        compose.onNodeWithTag("output_channel").performScrollTo().performClick()
        compose.onNodeWithText("ST_BLOCK").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(9, packet.chan); assertEquals(1, sendCount) }
        compose.onNodeWithText("ST_BLOCK").assertIsDisplayed()

        assertEquals(1, sendCount)
    }

    @Test fun secu3iChannelSelectionSendsSameIndexAsProtocolChannel() {
        assertEquals(listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IGN_OUT5", "ECF", "INJ_O1", "INJ_O2", "INJ_O3", "INJ_O4", "INJ_O5", "BL", "DE", "STBL_O", "CEL_O", "FPMP_O", "PWRR_O", "EVAP_O", "O2SH_0", "COND_O", "ADD_O2", "TACH_O", "GPA6_O"), secu3IOutputChannels)
        val packet = DiagOutputPacket()
        var sendCount = 0
        compose.setContent {
            DiagnosticsInputsScreen(null, false, 0, 10f, 50f, { selectDiagnosticOutputChannel(packet, it) { sendCount++ } }, { sendCount++ }, { sendCount++ }, controlsEnabled = true)
        }
        compose.onNodeWithTag("output_channel").performScrollTo().performClick()
        compose.onNodeWithText("GPA6_O").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(23, packet.chan); assertEquals(1, sendCount) }
    }

    @Test fun absentFirmwareDisablesEditsAndRecompositionDoesNotCallSends() {
        var sends = 0
        var refresh by mutableStateOf(0)
        compose.setContent {
            refresh
            DiagnosticsInputsScreen(null, null, 50, 10f, 50f, { sends++ }, { sends++ }, { sends++ })
        }
        compose.onNodeWithTag("output_channel").assertIsNotEnabled()
        compose.onNodeWithTag("frequency_edit").assertIsNotEnabled()
        compose.onNodeWithTag("duty_edit").assertIsNotEnabled()
        compose.runOnIdle { refresh++; assertEquals(0, sends) }
    }

    @Test fun channelDialogCancelAndFirmwareChangeDoNotSendCommands() {
        var firmware by mutableStateOf<Boolean?>(true)
        var sends = 0
        compose.setContent { DiagnosticsInputsScreen(null, firmware, 1, 10f, 50f, { sends++ }, {}, {}, controlsEnabled = true) }

        compose.onNodeWithTag("output_channel").performScrollTo().performClick()
        compose.onNodeWithTag("output_channel_option_1").assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.onNodeWithTag("output_channel_option_1").assertDoesNotExist()

        compose.onNodeWithTag("output_channel").performClick()
        compose.runOnIdle { firmware = null }
        compose.onNodeWithTag("output_channel_option_1").assertDoesNotExist()
        compose.onNodeWithTag("output_channel").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, sends) }
    }

    @Test fun frequencyAndDutyEditRequestsOnlyComeFromUserConfirmationEntryPoints() {
        var frequencyRequests = 0
        var dutyRequests = 0
        var frequency by mutableStateOf(10f)
        var duty by mutableStateOf(50f)
        val packet = DiagOutputPacket()
        compose.setContent {
            DiagnosticsInputsScreen(null, true, 0, frequency, duty, {}, {
                frequencyRequests++
                frequency = 11.2f
                updateDiagnosticOutputFrequency(packet, frequency) { frequencyRequests += 100 }
            }, {
                dutyRequests++
                duty = 51.5f
                updateDiagnosticOutputDuty(packet, duty) { dutyRequests += 100 }
            }, controlsEnabled = true)
        }
        compose.runOnIdle { assertEquals(0, frequencyRequests); assertEquals(0, dutyRequests) }
        compose.onNodeWithTag("frequency_edit").performScrollTo().performClick()
        compose.onNodeWithTag("duty_edit").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, frequencyRequests)
            assertEquals(1, dutyRequests)
            assertEquals(11.2f, packet.frq)
            assertEquals(51.5f, packet.duty)
        }
        compose.onNodeWithText("11.2 Hz").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("51.5 %").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            packet.chan = 1
            updateDiagnosticOutputFrequency(packet, 20f) { frequencyRequests++ }
            updateDiagnosticOutputDuty(packet, 60f) { dutyRequests++ }
            assertEquals(2, frequencyRequests)
            assertEquals(2, dutyRequests)
        }
    }

    @Test fun outputFieldsRemainTappableAndReadableAtNarrowWidth() {
        var channelChanges = 0
        var frequencyEdits = 0
        var dutyEdits = 0
        compose.setContent {
            val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.7f)) {
                Box(Modifier.width(280.dp)) {
                    DiagnosticsInputsScreen(null, true, 1, 123.4f, 42.5f,
                        { channelChanges++ }, { frequencyEdits++ }, { dutyEdits++ }, controlsEnabled = true)
                }
            }
        }
        compose.onNodeWithText("Test signal").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Output channel").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("IGN_OUT1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("123.4 Hz").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("42.5 %").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("frequency_edit").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("duty_edit").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(0, channelChanges)
            assertEquals(1, frequencyEdits)
            assertEquals(1, dutyEdits)
        }
    }

    @Test fun receivedValuesAndRestorationNeverInvokeOutputCallbacks() {
        val restoration = StateRestorationTester(compose)
        var packet by mutableStateOf(DiagInputPacket(voltage = 11f))
        var sends = 0
        restoration.setContent {
            DiagnosticsInputsScreen(packet, true, 0, 10f, 50f, { sends++ }, { sends++ }, { sends++ })
        }
        compose.runOnIdle { packet = DiagInputPacket(voltage = 13f); assertEquals(0, sends) }
        compose.onNodeWithText("13.000 V").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(0, sends) }
        compose.onNodeWithText("13.000 V").assertIsDisplayed()
    }
}
