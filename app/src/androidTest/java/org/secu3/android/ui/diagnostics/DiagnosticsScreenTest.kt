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
import androidx.compose.ui.test.assertIsDisplayed
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
    fun toolbarCallbacksAndConnectionStatusAreVisibleAndReactive() {
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
        compose.onNodeWithText(context.getString(R.string.dashboard_status_online)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.title_activity_settings)).performClick()
        assertEquals(1, backCalls)
        assertEquals(1, settingsCalls)
        compose.runOnIdle { connection.value = Disconnected }
        compose.onNodeWithText(context.getString(R.string.dashboard_status_offline)).assertIsDisplayed()
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

    private data class Fixture(
        val connection: MutableLiveData<ConnectionState>,
        val input: MutableLiveData<DiagInputPacket>,
        val packet: DiagOutputPacket,
        val enableBlDe: MutableLiveData<Boolean>,
        val enableTachO: MutableLiveData<Boolean>,
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
        outputPacket = data.packet,
        enableBlDeLiveData = data.enableBlDe,
        enableTachOLiveData = data.enableTachO,
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
        input = MutableLiveData(),
        packet = packet,
        enableBlDe = MutableLiveData(false),
        enableTachO = MutableLiveData(false),
        onOutput = onOutput,
    )
}
