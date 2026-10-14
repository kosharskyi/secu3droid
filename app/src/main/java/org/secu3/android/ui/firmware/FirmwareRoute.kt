/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.firmware

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FirmwareRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onDismiss: () -> Unit,
) {
    val viewModel: FirmwareViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val firmware by viewModel.firmwareLiveData.observeAsState(viewModel.initialFirmware)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SecuComposeTheme {
        Box(Modifier.fillMaxSize()) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                dragHandle = null,
                sheetGesturesEnabled = false,
            ) {
                FirmwareInfoSheet(firmware = firmware, onDismiss = onDismiss)
            }
        }
    }
}
