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
 */

package org.secu3.android.ui.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R

class StartScreenContentTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun connectButtonReportsConnectAndCancelState() {
        val states = mutableListOf<Boolean>()
        var connecting by mutableStateOf(false)
        compose.setContent {
            StartScreenContent(StartScreenState(connecting = connecting), StartScreenActions(onConnectOrCancel = states::add))
        }
        compose.onNodeWithText(context.getString(R.string.connect)).assertIsDisplayed().performClick()
        compose.runOnIdle { connecting = true }
        compose.onNodeWithText(context.getString(R.string.status_connecting)).assertIsDisplayed().performClick()
        assertEquals(listOf(false, true), states)
    }

    @Test fun logsAndSettingsInvokeTheirCallbacks() {
        val calls = mutableListOf<String>()
        compose.setContent {
            StartScreenContent(StartScreenState(), StartScreenActions(onLogs = { calls += "logs" }, onSettings = { calls += "settings" }))
        }
        compose.onNodeWithContentDescription("Logs icon").performClick()
        compose.onNodeWithContentDescription("Settings icon").performClick()
        assertEquals(listOf("logs", "settings"), calls)
    }

    @Test fun usbHintAppearsOnlyWhenDeviceIsPresent() {
        var deviceDetected by mutableStateOf(true)
        compose.setContent { StartScreenContent(StartScreenState(usbDeviceDetected = deviceDetected), StartScreenActions()) }
        compose.onNodeWithText(context.getString(R.string.usb_device_detected)).assertIsDisplayed()
        compose.runOnIdle { deviceDetected = false }
        compose.onNodeWithText(context.getString(R.string.usb_device_detected)).assertDoesNotExist()
    }

    @Test fun mainControlsRemainVisibleInCompactViewport() {
        compose.setContent {
            StartScreenContent(
                StartScreenState(usbDeviceDetected = true),
                StartScreenActions(),
                modifier = Modifier.size(width = 320.dp, height = 480.dp),
            )
        }
        compose.onNodeWithText(context.getString(R.string.connect)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.usb_device_detected)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Logs icon").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings icon").assertIsDisplayed()
    }

    @Test fun bluetoothSheetShowsRowsSelectsAddressAndDismisses() {
        val selected = mutableListOf<String>()
        var dismissed = 0
        compose.setContent {
            StartScreenContent(
                StartScreenState(
                    showBluetoothDevices = true,
                    bluetoothDevices = listOf(
                        StartBluetoothDevice("AA:BB:CC:DD:EE:01", "ECU One"),
                        StartBluetoothDevice("AA:BB:CC:DD:EE:02", "ECU Two"),
                    ),
                ),
                StartScreenActions(onBluetoothDeviceSelected = selected::add, onBluetoothSheetDismissed = { dismissed++ }),
            )
        }
        compose.onNodeWithText(context.getString(R.string.select_a_device_from_the_list)).assertIsDisplayed()
        compose.onNodeWithText("ECU One").assertIsDisplayed()
        compose.onNodeWithText("AA:BB:CC:DD:EE:01").assertIsDisplayed().performClick()
        assertEquals(listOf("AA:BB:CC:DD:EE:01"), selected)
        assertEquals(0, dismissed)
    }

    @Test fun bluetoothSheetDismissCallsCleanupCallback() {
        var dismissed = 0
        compose.setContent {
            StartScreenContent(StartScreenState(showBluetoothDevices = true), StartScreenActions(onBluetoothSheetDismissed = { dismissed++ }))
        }
        compose.onNodeWithText(context.getString(R.string.select_a_device_from_the_list)).performTouchInput { swipeDown() }
        compose.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test fun permissionRationaleOffersConfirmAndCancelCallbacks() {
        val calls = mutableListOf<String>()
        compose.setContent {
            StartScreenContent(StartScreenState(showBluetoothRationale = true), StartScreenActions(
                onRationaleConfirmed = { calls += "confirm" }, onRationaleCancelled = { calls += "cancel" },
            ))
        }
        compose.onNodeWithText(context.getString(R.string.permission_rationale_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.onNodeWithText(context.getString(R.string.permission_rationale_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(listOf("cancel", "confirm"), calls)
    }
}
