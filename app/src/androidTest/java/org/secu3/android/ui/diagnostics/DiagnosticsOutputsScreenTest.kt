/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2025 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2025 Alexey A. Shabelnikov. Ukraine, Kyiv
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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class DiagnosticsOutputsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun displaysFirmwareSpecificControlsAndHidesUnknownFirmware() {
        val firmware = mutableStateOf<Boolean?>(true)
        compose.setContent { SecuComposeTheme { OutputsScreen(firmware = firmware.value) } }
        compose.onNodeWithText("IE").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("FE").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("IGN_OUT5").assertCountEquals(0)
        firmware.value = false
        compose.onNodeWithText("IGN_OUT5").assertIsDisplayed()
        compose.onAllNodesWithText("IE").assertCountEquals(0)
        firmware.value = null
        compose.onAllNodesWithText("IGN_OUT1").assertCountEquals(0)
    }

    @Test
    fun displaysEveryFirmwareControlIncludingAliases() {
        val firmware = mutableStateOf<Boolean?>(true)
        compose.setContent { SecuComposeTheme { OutputsScreen(firmware = firmware.value) } }
        val secu3TLabels = listOf("IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IE", "FE", "ECF", "CE", "ST_BLOCK", "ADD_IO1", "ADD_IO2", "BL", "DE")
        secu3TLabels.forEach { compose.onAllNodesWithText(it).assertCountEquals(1) }
        assertEquals(13, secu3TLabels.size)
        firmware.value = false
        val secu3ILabels = listOf("IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IGN_OUT5", "ECF0", "INJ_O1", "INJ_O2", "INJ_O3", "INJ_O4", "INJ_O5", "BL", "DE", "STBL_O", "CEL_O", "FPMP_O", "PWRR_O", "EVAP_O", "O2SH_O", "COND_O", "ADD_O2", "GPA6_O")
        secu3ILabels.forEach { compose.onAllNodesWithText(it).assertCountEquals(1) }
        assertEquals(23, secu3ILabels.size + 1) // TACH_O appears after its permission is granted.
        compose.onAllNodesWithText("ECF").assertCountEquals(0)
        compose.onAllNodesWithTag("output_TACH_O").assertCountEquals(0)
    }

    @Test
    fun additionalOutputsRespectPermissionGates() {
        val blDe = mutableStateOf(false)
        val tach = mutableStateOf(false)
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(false, 0, blDe.value, tach.value) { _, _ -> }
            }
        }
        compose.onNodeWithTag("output_BL").assertIsNotEnabled()
        compose.onNodeWithTag("output_DE").assertIsNotEnabled()
        compose.onAllNodesWithTag("output_TACH_O").assertCountEquals(0)
        compose.onNodeWithText("BL").performScrollTo().assertIsDisplayed()
        blDe.value = true
        tach.value = true
        compose.onAllNodesWithTag("output_BL").assertCountEquals(1)
        compose.onNodeWithTag("output_BL").assertIsOff()
        compose.onAllNodesWithTag("output_TACH_O").assertCountEquals(1)
    }

    @Test
    fun selectedGeneratorOutputDisablesRowAndCheckboxAndChannelChangesUnlock() {
        val selectedChannel = mutableIntStateOf(1)
        var outputCalls = 0
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(
                    isSecu3T = false,
                    outputBits = 0,
                    enableBlDe = true,
                    enableTachO = true,
                    selectedChannel = selectedChannel.intValue,
                    onOutputChanged = { _, _ -> outputCalls++ },
                )
            }
        }
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsNotEnabled()
        compose.onNodeWithTag("output_IGN_OUT1").assertIsNotEnabled()
        compose.onNodeWithTag("generator_control_caption_IGN_OUT1", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT2", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("output_row_IGN_OUT2").assertIsEnabled()

        selectedChannel.intValue = 2
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsEnabled()
        compose.onNodeWithTag("output_IGN_OUT1").assertIsEnabled()
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT1", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("output_row_IGN_OUT2").assertIsNotEnabled()
        compose.onNodeWithTag("output_IGN_OUT2").assertIsNotEnabled()
        compose.onNodeWithTag("generator_control_caption_IGN_OUT2", useUnmergedTree = true).assertIsDisplayed()

        selectedChannel.intValue = 0
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsEnabled()
        compose.onNodeWithTag("output_row_IGN_OUT2").assertIsEnabled()
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT1", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT2", useUnmergedTree = true).assertCountEquals(0)
        assertEquals(0, outputCalls)
    }

    @Test
    fun selectedChannelMappingUsesFamilyOrderForEcfAndFilteredTach() {
        val firmware = mutableStateOf<Boolean?>(false)
        val selectedChannel = mutableIntStateOf(6) // SECU-3i channel 6 is ECF -> ECF0 bit.
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(
                    isSecu3T = firmware.value,
                    outputBits = 0,
                    enableBlDe = true,
                    enableTachO = true,
                    selectedChannel = selectedChannel.intValue,
                    onOutputChanged = { _, _ -> },
                )
            }
        }
        compose.onNodeWithTag("output_ECF0").assertIsNotEnabled()

        selectedChannel.intValue = 22 // TACH_O remains channel 22 when listed; filtering only hides its row.
        compose.onNodeWithTag("output_TACH_O").assertIsNotEnabled()
        selectedChannel.intValue = 7
        compose.onNodeWithTag("output_ECF0").assertIsEnabled()
        compose.onNodeWithTag("output_INJ_O1").assertIsNotEnabled()

        firmware.value = true
        selectedChannel.intValue = 7 // SECU-3T channel 7 is ECF.
        compose.onNodeWithTag("output_ECF").assertIsNotEnabled()
    }

    @Test
    fun enabledTachOAndBlDeCanBeChangedOnceAndUpdateTheirProtocolBits() {
        val packet = DiagOutputPacket()
        val sends = mutableListOf<Int>()
        val bits = mutableIntStateOf(packet.out)
        val blDe = mutableStateOf(true)
        val tach = mutableStateOf(true)
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(false, bits.intValue, blDe.value, tach.value) { output, enabled ->
                    setDiagnosticOutput(packet, output, enabled)
                    bits.intValue = packet.out
                    sends += packet.out
                }
            }
        }
        compose.onNodeWithTag("output_TACH_O").performScrollTo().assertIsOff().performClick()
        assertEquals(1, sends.size)
        assertEquals((1 shl 23) or (1 shl 24), packet.out)
        compose.onNodeWithTag("output_BL").performScrollTo().performClick()
        assertEquals(2, sends.size)
        assertEquals((1 shl 11) or (1 shl 12) or (1 shl 23) or (1 shl 24), packet.out)
        compose.onNodeWithTag("output_DE").performScrollTo().performClick()
        assertEquals(3, sends.size)
        assertEquals((1 shl 11) or (1 shl 12) or (1 shl 13) or (1 shl 14) or (1 shl 23) or (1 shl 24), packet.out)
        compose.onNodeWithText("GPA6_O").performScrollTo().performClick()
        assertEquals(4, sends.size)
        assertEquals((1 shl 11) or (1 shl 12) or (1 shl 13) or (1 shl 14) or (1 shl 23) or (1 shl 24) or (1 shl 25), packet.out)
        compose.onNodeWithTag("output_DE").performScrollTo().performClick()
        assertEquals(5, sends.size)
        assertEquals((1 shl 11) or (1 shl 12) or (1 shl 14) or (1 shl 23) or (1 shl 24) or (1 shl 25), packet.out)
    }

    @Test
    fun renderRecompositionAndStateRestorationDoNotSendPackets() {
        val packet = DiagOutputPacket().apply { ignOut1 = true; ecf0 = true }
        val sends = mutableListOf<Int>()
        val bits = mutableIntStateOf(packet.out)
        val permission = mutableStateOf(false)
        val restorationTester = StateRestorationTester(compose)
        restorationTester.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(false, bits.intValue, permission.value, false) { output, enabled ->
                    setDiagnosticOutput(packet, output, enabled)
                    bits.intValue = packet.out
                    sends += packet.out
                }
            }
        }
        assertEquals(emptyList<Int>(), sends)
        compose.onNodeWithTag("output_IGN_OUT1").assertIsOn()
        compose.onNodeWithTag("output_ECF0").assertIsOn()
        permission.value = true
        restorationTester.emulateSavedInstanceStateRestore()
        assertEquals(emptyList<Int>(), sends)
        compose.onNodeWithTag("output_IGN_OUT1").assertIsOn()
        compose.onNodeWithTag("output_ECF0").assertIsOn()
    }

    @Test
    fun everyOutputMapsToExpectedPacketBits() {
        val expectedBits = mapOf(
            DiagnosticOutput.IGN_OUT1 to (1 shl 0), DiagnosticOutput.IGN_OUT2 to (1 shl 1), DiagnosticOutput.IGN_OUT3 to (1 shl 2), DiagnosticOutput.IGN_OUT4 to (1 shl 3),
            DiagnosticOutput.IE to (1 shl 4), DiagnosticOutput.FE to (1 shl 5), DiagnosticOutput.ECF to (1 shl 6), DiagnosticOutput.CE to (1 shl 7), DiagnosticOutput.ST_BLOCK to (1 shl 8),
            DiagnosticOutput.ADD_IO1 to (1 shl 9), DiagnosticOutput.ADD_IO2 to (1 shl 10), DiagnosticOutput.IGN_OUT5 to (1 shl 4), DiagnosticOutput.ECF0 to (1 shl 5),
            DiagnosticOutput.INJ_O1 to (1 shl 6), DiagnosticOutput.INJ_O2 to (1 shl 7), DiagnosticOutput.INJ_O3 to (1 shl 8), DiagnosticOutput.INJ_O4 to (1 shl 9),
            DiagnosticOutput.INJ_O5 to (1 shl 10), DiagnosticOutput.BL to ((1 shl 11) or (1 shl 12)), DiagnosticOutput.DE to ((1 shl 13) or (1 shl 14)),
            DiagnosticOutput.STBL_O to (1 shl 15), DiagnosticOutput.CEL_O to (1 shl 16), DiagnosticOutput.FPMP_O to (1 shl 17), DiagnosticOutput.PWRR_O to (1 shl 18),
            DiagnosticOutput.EVAP_O to (1 shl 19), DiagnosticOutput.O2SH_O to (1 shl 20), DiagnosticOutput.COND_O to (1 shl 21), DiagnosticOutput.ADD_O2 to (1 shl 22),
            DiagnosticOutput.TACH_O to ((1 shl 23) or (1 shl 24)), DiagnosticOutput.GPA6_O to (1 shl 25),
        )
        DiagnosticOutput.entries.forEach { output ->
            val packet = DiagOutputPacket()
            setDiagnosticOutput(packet, output, true)
            val expected = expectedBits.getValue(output)
            assertEquals(output.name, expected, packet.out)
            setDiagnosticOutput(packet, output, false)
            val expectedOff = when (output) {
                DiagnosticOutput.BL -> 1 shl 12
                DiagnosticOutput.DE -> 1 shl 14
                DiagnosticOutput.TACH_O -> 1 shl 24
                else -> 0
            }
            assertEquals(output.name, expectedOff, packet.out)
        }
    }

    @androidx.compose.runtime.Composable
    private fun OutputsScreen(firmware: Boolean?, bits: Int = 0, selectedChannel: Int = 0) = DiagnosticsOutputsScreen(
        isSecu3T = firmware,
        outputBits = bits,
        enableBlDe = false,
        enableTachO = false,
        selectedChannel = selectedChannel,
        onOutputChanged = { _, _ -> },
    )
}
