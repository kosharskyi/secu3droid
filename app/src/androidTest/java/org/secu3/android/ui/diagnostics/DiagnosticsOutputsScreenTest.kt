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
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.diagnostics.DiagnosticOutput
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
                DiagnosticsOutputsScreen(false, 0, blDe.value, tach.value, controlsEnabled = true) { _, _ -> }
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
        val generatorOutput = mutableStateOf<DiagnosticOutput?>(DiagnosticOutput.IGN_OUT1)
        var outputCalls = 0
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(
                    isSecu3T = false,
                    outputBits = 0,
                    enableBlDe = true,
                    enableTachO = true,
                    generatorOutput = generatorOutput.value,
                    onOutputChanged = { _, _ -> outputCalls++ },
                    controlsEnabled = true,
                )
            }
        }
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsNotEnabled()
        compose.onNodeWithTag("output_IGN_OUT1").assertIsNotEnabled()
        compose.onNodeWithTag("generator_control_caption_IGN_OUT1", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT2", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("output_row_IGN_OUT2").assertIsEnabled()

        generatorOutput.value = DiagnosticOutput.IGN_OUT2
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsEnabled()
        compose.onNodeWithTag("output_IGN_OUT1").assertIsEnabled()
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT1", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("output_row_IGN_OUT2").assertIsNotEnabled()
        compose.onNodeWithTag("output_IGN_OUT2").assertIsNotEnabled()
        compose.onNodeWithTag("generator_control_caption_IGN_OUT2", useUnmergedTree = true).assertIsDisplayed()

        generatorOutput.value = null
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsEnabled()
        compose.onNodeWithTag("output_row_IGN_OUT2").assertIsEnabled()
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT1", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("generator_control_caption_IGN_OUT2", useUnmergedTree = true).assertCountEquals(0)
        assertEquals(0, outputCalls)
    }

    @Test
    fun generatorOutputStateLocksRowsAndTachFilteringKeepsRowsStable() {
        val firmware = mutableStateOf<Boolean?>(false)
        val generatorOutput = mutableStateOf<DiagnosticOutput?>(DiagnosticOutput.ECF0)
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(
                    isSecu3T = firmware.value,
                    outputBits = 0,
                    enableBlDe = true,
                    enableTachO = true,
                    generatorOutput = generatorOutput.value,
                    onOutputChanged = { _, _ -> },
                    controlsEnabled = true,
                )
            }
        }
        compose.onNodeWithTag("output_ECF0").assertIsNotEnabled()

        generatorOutput.value = DiagnosticOutput.TACH_O
        compose.onNodeWithTag("output_TACH_O").assertIsNotEnabled()
        generatorOutput.value = DiagnosticOutput.INJ_O1
        compose.onNodeWithTag("output_ECF0").assertIsEnabled()
        compose.onNodeWithTag("output_INJ_O1").assertIsNotEnabled()

        firmware.value = true
        generatorOutput.value = DiagnosticOutput.ECF
        compose.onNodeWithTag("output_ECF").assertIsNotEnabled()
    }

    @Test
    fun outputToggleCallbacksOnlyComeFromUserActions() {
        val changes = mutableListOf<Pair<DiagnosticOutput, Boolean>>()
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(false, 0, true, true, controlsEnabled = true) { output, enabled ->
                    changes += output to enabled
                }
            }
        }
        assertEquals(emptyList<Pair<DiagnosticOutput, Boolean>>(), changes)
        compose.onNodeWithTag("output_TACH_O").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("output_BL").performScrollTo().performClick()
        compose.onNodeWithTag("output_DE").performScrollTo().performClick()
        assertEquals(
            listOf(DiagnosticOutput.TACH_O to true, DiagnosticOutput.BL to true, DiagnosticOutput.DE to true),
            changes,
        )
    }

    @Test
    fun renderRecompositionAndStateRestorationDoNotInvokeOutputCallbacks() {
        val changes = mutableListOf<Pair<DiagnosticOutput, Boolean>>()
        val permission = mutableStateOf(false)
        val restorationTester = StateRestorationTester(compose)
        restorationTester.setContent {
            SecuComposeTheme {
                DiagnosticsOutputsScreen(false, 0, permission.value, false, controlsEnabled = true) { output, enabled ->
                    changes += output to enabled
                }
            }
        }
        assertEquals(emptyList<Pair<DiagnosticOutput, Boolean>>(), changes)
        permission.value = true
        restorationTester.emulateSavedInstanceStateRestore()
        assertEquals(emptyList<Pair<DiagnosticOutput, Boolean>>(), changes)
        compose.onNodeWithTag("output_IGN_OUT1").assertIsOff()
    }

    @androidx.compose.runtime.Composable
    private fun OutputsScreen(firmware: Boolean?, bits: Int = 0, generatorOutput: DiagnosticOutput? = null) = DiagnosticsOutputsScreen(
        isSecu3T = firmware,
        outputBits = bits,
        enableBlDe = false,
        enableTachO = false,
        generatorOutput = generatorOutput,
        onOutputChanged = { _, _ -> },
        controlsEnabled = true,
    )
}
