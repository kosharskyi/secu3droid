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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.R
import org.secu3.android.ui.compose.ConfirmationDialog
import org.secu3.android.ui.compose.SecuComposeTheme

data class StartBluetoothDevice(val id: String, val name: String)

data class StartScreenState(
    val connecting: Boolean = false,
    val usbDeviceDetected: Boolean = false,
    val showBluetoothDevices: Boolean = false,
    val bluetoothDevices: List<StartBluetoothDevice> = emptyList(),
    val showBluetoothRationale: Boolean = false,
)

data class StartScreenActions(
    val onConnectOrCancel: (currentlyConnecting: Boolean) -> Unit = {},
    val onLogs: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onBluetoothDeviceSelected: (id: String) -> Unit = {},
    val onBluetoothSheetDismissed: () -> Unit = {},
    val onRationaleConfirmed: () -> Unit = {},
    val onRationaleCancelled: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenContent(
    state: StartScreenState,
    actions: StartScreenActions,
    modifier: Modifier = Modifier,
) {
    SecuComposeTheme {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().systemBarsPadding()) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().height(64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_log_file),
                            contentDescription = "Logs icon",
                            modifier = Modifier.size(64.dp).padding(16.dp).clickable(onClick = actions.onLogs),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings icon",
                            modifier = Modifier.size(64.dp).padding(16.dp).clickable(onClick = actions.onSettings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularButton(
                            textNormal = stringResource(R.string.connect),
                            textInProgress = stringResource(R.string.status_connecting),
                            isInProgress = state.connecting,
                            onClick = { actions.onConnectOrCancel(state.connecting) },
                            size = 200.dp,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            progressSegmentColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            borderWidth = 2.dp,
                            progressSegmentWidth = 6.dp,
                            modifier = Modifier.testTag("start-connect-button"),
                        )
                    }
                    if (state.usbDeviceDetected) {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.usb_device_detected),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Spacer(Modifier.height(64.dp))
                    }
                }
                if (state.showBluetoothDevices) {
                    ModalBottomSheet(
                        onDismissRequest = actions.onBluetoothSheetDismissed,
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                        modifier = Modifier.testTag("bluetooth-device-sheet"),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().fillMaxHeight().testTag("bluetooth-device-sheet-content"),
                        ) {
                            Text(
                                text = stringResource(R.string.select_a_device_from_the_list),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                            )
                            HorizontalDivider()
                            LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("bluetooth-device-list")) {
                                items(state.bluetoothDevices, key = { it.id }) { device ->
                                    ListItem(
                                        headlineContent = {
                                            Row {
                                                Image(Icons.Default.Bluetooth, contentDescription = "Bluetooth icon",
                                                    modifier = Modifier.padding(end = 8.dp).align(Alignment.CenterVertically))
                                                Column {
                                                    Text(device.name.ifEmpty { stringResource(R.string.unknown_device) })
                                                    Text(device.id, fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                            }
                                        },
                                        modifier = Modifier.clickable { actions.onBluetoothDeviceSelected(device.id) },
                                    )
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
                if (state.showBluetoothRationale) {
                    ConfirmationDialog(
                        title = R.string.permission_rationale_title,
                        message = R.string.permission_rationale_message,
                        onConfirm = actions.onRationaleConfirmed,
                        onCancel = actions.onRationaleCancelled,
                    )
                }
            }
        }
    }
}
