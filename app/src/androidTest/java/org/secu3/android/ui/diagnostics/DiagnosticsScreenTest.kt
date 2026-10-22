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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.models.packets.input.DiagInputPacket
import org.secu3.android.models.packets.out.DiagOutputPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class DiagnosticsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun tabsSwipeAndClick_withoutSendingOutputPackets() {
        var outputCalls = 0
        val data = fixture(onOutput = { _, _ -> outputCalls++ })
        val tabTitles = context.resources.getStringArray(R.array.diagnostics_fragments_title)
        compose.setContent { SecuComposeTheme { Render(data) } }
        compose.waitForIdle()

        compose.onAllNodesWithTag("diagnostics-tabs").assertCountEquals(1)
        tabTitles.drop(2).forEach { compose.onAllNodesWithText(it).assertCountEquals(0) }
        compose.onNodeWithText(tabTitles[0]).assertIsSelected()
        compose.onNodeWithTag("diagnostics-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText(tabTitles[1]).assertIsSelected()
        compose.onNodeWithText(tabTitles[0]).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(tabTitles[0]).assertIsSelected()
        assertEquals(0, outputCalls)
    }

    @Test
    fun toolbarCallbacksAndAccessibleConnectionStatusAreReactive() {
        var backCalls = 0
        var settingsCalls = 0
        val connection = MutableLiveData<ConnectionState>(Connected)
        val data = fixture(connection = connection)
        compose.setContent {
            SecuComposeTheme {
                Render(data, onNavigateUp = { backCalls++ }, onAdditionalSettings = { settingsCalls++ })
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_connected)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_connection_connected)).assertDoesNotExist()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.title_activity_settings)).performClick()
        assertEquals(1, backCalls)
        assertEquals(1, settingsCalls)
        compose.runOnIdle { connection.value = Disconnected }
        compose.onNodeWithContentDescription(context.getString(R.string.home_connection_disconnected)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_connection_disconnected)).assertDoesNotExist()
    }

    @Test
    fun selectedTabRestoresAndPagingDoesNotSendOutput() {
        var outputCalls = 0
        val data = fixture(onOutput = { _, _ -> outputCalls++ })
        val tabTitles = context.resources.getStringArray(R.array.diagnostics_fragments_title)
        val restorationTester = StateRestorationTester(compose)
        restorationTester.setContent { SecuComposeTheme { Render(data) } }
        compose.waitForIdle()
        compose.onNodeWithTag("diagnostics-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        restorationTester.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(tabTitles[1]).assertIsSelected()
        assertEquals(0, outputCalls)
    }

    @Test
    fun channelSelectionPreservesDefaultDutyInDisplayAndPacket() {
        val packet = DiagOutputPacket()
        val state = mutableStateOf(DiagnosticsViewModel.UiState(controlsEnabled = true))
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsScreen(
                    isSecu3T = false,
                    connectionStatus = MutableLiveData<ConnectionState>(Connected),
                    inputLiveData = MutableLiveData<DiagInputPacket?>(DiagInputPacket()),
                    state = state.value,
                    onNavigateUp = {},
                    onAdditionalSettings = {},
                    onOutputChanged = { _, _ -> },
                    onChannelSelected = { channel ->
                        packet.chan = channel
                        state.value = state.value.copy(channel = channel)
                    },
                    onFrequencyEdit = {},
                    onDutyEdit = {},
                )
            }
        }
        compose.onNodeWithTag("output_channel").performScrollTo().performClick()
        compose.onNodeWithText("IGN_OUT1").performClick()
        compose.onNodeWithTag("duty_edit").performScrollTo()
        compose.onNodeWithText("50.0 %").assertIsDisplayed()
        assertEquals(1, packet.chan)
        assertEquals(50f, packet.duty)
    }

    @Test
    fun selectedGeneratorChannelLocksCorrespondingOutputOnTheOutputsTab() {
        val state = mutableStateOf(DiagnosticsViewModel.UiState(channel = 1, controlsEnabled = true))
        val tabTitles = context.resources.getStringArray(R.array.diagnostics_fragments_title)
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsScreen(
                    isSecu3T = false,
                    connectionStatus = MutableLiveData<ConnectionState>(Connected),
                    inputLiveData = MutableLiveData<DiagInputPacket?>(DiagInputPacket()),
                    state = state.value,
                    onNavigateUp = {},
                    onAdditionalSettings = {},
                    onOutputChanged = { _, _ -> },
                    onChannelSelected = {},
                    onFrequencyEdit = {},
                    onDutyEdit = {},
                )
            }
        }
        compose.onNodeWithText(tabTitles[1]).performClick()
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsNotEnabled()

        state.value = state.value.copy(channel = 2)
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsEnabled()
        compose.onNodeWithTag("output_row_IGN_OUT2").assertIsNotEnabled()
        state.value = state.value.copy(channel = 0)
        compose.onNodeWithTag("output_row_IGN_OUT1").assertIsEnabled()
    }

    @Test
    fun firmwareFamilyAndTpicFlagsUpdateInputRowsWhileScreenIsOpen() {
        val firmware = mutableStateOf<Boolean?>(null)
        val hasTpic8101 = mutableStateOf<Boolean?>(null)
        val packet = DiagInputPacket(map = 1.25f, carb = 2.5f, addI3 = 3.75f, addI4 = 0f)
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsScreen(
                    isSecu3T = firmware.value,
                    hasTpic8101 = hasTpic8101.value,
                    connectionStatus = MutableLiveData<ConnectionState>(Connected),
                    inputLiveData = MutableLiveData(packet),
                    state = DiagnosticsViewModel.UiState(controlsEnabled = true),
                    onNavigateUp = {},
                    onAdditionalSettings = {},
                    onOutputChanged = { _, _ -> },
                    onChannelSelected = {},
                    onFrequencyEdit = {},
                    onDutyEdit = {},
                )
            }
        }

        compose.onNodeWithTag("analog_MAP_S").assertExists()
        compose.onNodeWithTag("analog_CARB").assertExists()
        compose.onNodeWithTag("analog_ADD_IO3").assertExists()
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("— V")

        firmware.value = false
        hasTpic8101.value = true
        compose.onNodeWithTag("analog_MAP_I").assertExists()
        compose.onNodeWithTag("analog_TPS").assertExists()
        compose.onNodeWithTag("analog_ADD_IO3").assertExists()
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("0.000 V")

        firmware.value = true
        compose.onNodeWithTag("analog_MAP_S").assertExists()
        compose.onNodeWithTag("analog_CARB").assertExists()
        compose.onAllNodesWithTag("analog_ADD_IO3").assertCountEquals(0)
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("0.000 V")

        hasTpic8101.value = false
        compose.onNodeWithTag("analog_ADD_IO4-value").assertTextEquals("— V")
    }

    @Test
    fun frequencyAndDutyEditorsUseCurrentCompositionCallbacks() {
        val packet = DiagOutputPacket().apply { frq = 123.456f; duty = 24.25f }
        val data = fixture(packet = packet)
        val frequencies = mutableListOf<Float>()
        val duties = mutableListOf<Float>()
        compose.setContent {
            SecuComposeTheme {
                Render(data, onFrequencyEdit = frequencies::add, onDutyEdit = duties::add)
            }
        }
        compose.onNodeWithTag("frequency_edit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("numeric_confirm").performClick()
        assertEquals(listOf(123.456f), frequencies)

        compose.onNodeWithTag("duty_edit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("numeric_value").performTextClearance()
        compose.onNodeWithTag("numeric_value").performTextInput("37.5")
        compose.onNodeWithTag("numeric_confirm").performClick()
        assertEquals(listOf(37.5f), duties)
    }

    @Test
    fun losingReadinessClosesNumericEditorAndDisablesControls() {
        val connection = MutableLiveData<ConnectionState>(Connected)
        val input = MutableLiveData<DiagInputPacket?>(DiagInputPacket(voltage = 12.5f))
        val state = mutableStateOf(DiagnosticsViewModel.UiState(controlsEnabled = true))
        var edits = 0
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsScreen(
                    isSecu3T = false,
                    connectionStatus = connection,
                    inputLiveData = input,
                    state = state.value,
                    onNavigateUp = {},
                    onAdditionalSettings = {},
                    onOutputChanged = { _, _ -> },
                    onChannelSelected = {},
                    onFrequencyEdit = { edits++ },
                    onDutyEdit = {},
                )
            }
        }
        compose.onNodeWithTag("frequency_edit").performScrollTo().performClick()
        compose.onNodeWithTag("numeric_confirm").assertExists()

        compose.runOnIdle {
            connection.value = Disconnected
            state.value = state.value.copy(controlsEnabled = false)
        }
        compose.onNodeWithTag("numeric_confirm").assertDoesNotExist()
        compose.onNodeWithTag("diagnostics-settings-icon").assertIsNotEnabled()
        compose.onNodeWithTag("frequency_edit").assertIsNotEnabled()
        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("— V")
        assertEquals(0, edits)
    }

    @Test
    fun losingReadinessDismissesChannelChoicesAndMasksRetainedInput() {
        val connection = MutableLiveData<ConnectionState>(Connected)
        val input = MutableLiveData<DiagInputPacket?>(DiagInputPacket(voltage = 12.5f))
        val state = mutableStateOf(DiagnosticsViewModel.UiState(controlsEnabled = true))
        compose.setContent {
            SecuComposeTheme {
                DiagnosticsScreen(
                    isSecu3T = false,
                    connectionStatus = connection,
                    inputLiveData = input,
                    state = state.value,
                    onNavigateUp = {},
                    onAdditionalSettings = {},
                    onOutputChanged = { _, _ -> },
                    onChannelSelected = {},
                    onFrequencyEdit = {},
                    onDutyEdit = {},
                )
            }
        }
        compose.onNodeWithTag("output_channel").performScrollTo().performClick()
        compose.onNodeWithTag("output_channel_option_1").assertExists()

        compose.runOnIdle {
            connection.value = Disconnected
            state.value = state.value.copy(controlsEnabled = false)
        }
        compose.onNodeWithTag("output_channel_option_1").assertDoesNotExist()
        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("— V")
    }

    private data class Fixture(
        val connection: MutableLiveData<ConnectionState>,
        val input: MutableLiveData<DiagInputPacket>,
        val state: DiagnosticsViewModel.UiState,
        val onOutput: (DiagnosticOutput, Boolean) -> Unit,
    )

    @Composable
    private fun Render(
        data: Fixture,
        onNavigateUp: () -> Unit = {},
        onAdditionalSettings: () -> Unit = {},
        onFrequencyEdit: (Float) -> Unit = {},
        onDutyEdit: (Float) -> Unit = {},
    ) = DiagnosticsScreen(
        isSecu3T = false,
        connectionStatus = data.connection,
        inputLiveData = data.input,
        state = data.state,
        onNavigateUp = onNavigateUp,
        onAdditionalSettings = onAdditionalSettings,
        onOutputChanged = data.onOutput,
        onChannelSelected = {},
        onFrequencyEdit = onFrequencyEdit,
        onDutyEdit = onDutyEdit,
    )

    private fun fixture(
        connection: MutableLiveData<ConnectionState> = MutableLiveData(Connected),
        packet: DiagOutputPacket = DiagOutputPacket(),
        onOutput: (DiagnosticOutput, Boolean) -> Unit = { _, _ -> },
    ) = Fixture(
        connection = connection,
        input = MutableLiveData(DiagInputPacket()),
        state = DiagnosticsViewModel.UiState(
            outputBits = packet.out,
            channel = packet.chan,
            frequency = packet.frq,
            duty = if (packet.chan == 0 && packet.duty == 0f) 50f else packet.duty,
            controlsEnabled = true,
        ),
        onOutput = onOutput,
    )
}
