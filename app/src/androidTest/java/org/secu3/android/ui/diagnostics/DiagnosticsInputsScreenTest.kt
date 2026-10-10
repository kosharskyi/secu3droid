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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
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

    @Test fun valuesAndReadOnlyStatusesAreShownAndSecu3iFieldsAreConditional() {
        val input = DiagInputPacket(voltage = 12.345f, map = 1.25f, addI8 = 8.5f, bits = 0b1010101010)
        var firmware by mutableStateOf<Boolean?>(false)
        compose.setContent { DiagnosticsInputsScreen(input, firmware, 0, 10f, 50f, {}, {}, {}) }
        compose.onNodeWithText("12.345 V").assertIsDisplayed()
        compose.onNodeWithTag("analog_ADD_IO8").assertIsDisplayed()
        compose.onNodeWithTag("status_GPA4_I").assertIsDisplayed()
        compose.onNodeWithTag("output_channel").assertIsDisplayed()
        compose.onNodeWithTag("frequency_edit").assertIsDisplayed()
        compose.onNodeWithTag("duty_edit").assertIsDisplayed()
        compose.onNodeWithText("COND_I").assertIsDisplayed()

        compose.runOnIdle { firmware = true }
        compose.onAllNodesWithTag("analog_ADD_IO8").assertCountEquals(0)
        compose.onAllNodesWithTag("status_GPA4_I").assertCountEquals(0)
        compose.onAllNodesWithTag("status_COND_I").assertCountEquals(0)
        compose.onNodeWithText("ADD_IO3").assertIsDisplayed()
    }

    @Test fun secu3TChannelSelectionSendsSameIndexAsProtocolChannelAndRenderIsSilent() {
        assertEquals(listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IE", "FE", "ECF", "CE", "ST_BLOCK", "ADD_IO1", "ADD_IO2", "BL", "DE"), secu3TOutputChannels)
        val packet = DiagOutputPacket()
        var sendCount = 0
        var current by mutableStateOf(0)
        compose.setContent {
            DiagnosticsInputsScreen(null, true, current, 10f, 50f, { current = it; selectDiagnosticOutputChannel(packet, it) { sendCount++ } }, { sendCount++ }, { sendCount++ })
        }
        compose.runOnIdle { assertEquals(0, sendCount) }
        compose.onNodeWithTag("output_channel").performClick()
        compose.onNodeWithText("ST_BLOCK").performClick()
        compose.runOnIdle { assertEquals(9, packet.chan); assertEquals(1, sendCount) }
        compose.onNodeWithText("ST_BLOCK").assertIsDisplayed()

        assertEquals(1, sendCount)
    }

    @Test fun secu3iChannelSelectionSendsSameIndexAsProtocolChannel() {
        assertEquals(listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IGN_OUT5", "ECF", "INJ_O1", "INJ_O2", "INJ_O3", "INJ_O4", "INJ_O5", "BL", "DE", "STBL_O", "CEL_O", "FPMP_O", "PWRR_O", "EVAP_O", "O2SH_0", "COND_O", "ADD_O2", "TACH_O", "GPA6_O"), secu3IOutputChannels)
        val packet = DiagOutputPacket()
        var sendCount = 0
        compose.setContent {
            DiagnosticsInputsScreen(null, false, 0, 10f, 50f, { selectDiagnosticOutputChannel(packet, it) { sendCount++ } }, { sendCount++ }, { sendCount++ })
        }
        compose.onNodeWithTag("output_channel").performClick()
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
            })
        }
        compose.runOnIdle { assertEquals(0, frequencyRequests); assertEquals(0, dutyRequests) }
        compose.onNodeWithTag("frequency_edit").performClick()
        compose.onNodeWithTag("duty_edit").performClick()
        compose.runOnIdle {
            assertEquals(1, frequencyRequests)
            assertEquals(1, dutyRequests)
            assertEquals(11.2f, packet.frq)
            assertEquals(51.5f, packet.duty)
        }
        compose.onNodeWithTag("frequency_edit").assertTextEquals("Test signal frequency: 11.2 Hz")
        compose.onNodeWithTag("duty_edit").assertTextEquals("Test signal duty: 51.5 %")
        compose.runOnIdle {
            packet.chan = 1
            updateDiagnosticOutputFrequency(packet, 20f) { frequencyRequests++ }
            updateDiagnosticOutputDuty(packet, 60f) { dutyRequests++ }
            assertEquals(2, frequencyRequests)
            assertEquals(2, dutyRequests)
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
