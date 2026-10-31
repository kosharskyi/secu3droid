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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme

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
    val firmware by viewModel.firmwareInfo.collectAsStateWithLifecycle()
    SecuComposeTheme {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)).clickable(onClick = onDismiss).testTag("firmware-overlay"), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.BottomCenter).clickable { },
                verticalArrangement = Arrangement.Bottom,
            ) {
                FirmwareInfoSheet(firmware = firmware, onDismiss = onDismiss)
            }
        }
    }
}
