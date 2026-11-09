/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.errors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.domain.errors.ErrorsReadStatus
import org.secu3.android.domain.errors.ErrorsSnapshot
import org.secu3.android.domain.errors.ErrorsWriteStatus
import org.secu3.android.models.CheckEngineError
import org.secu3.android.ui.compose.LocalSecuPalette
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.ui.compose.SettingsSection
import org.secu3.android.utils.getBitValue

@Composable
internal fun ErrorsContent(
    connectionState: ConnectionState?,
    snapshot: ErrorsSnapshot,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onLiveChange: (Boolean) -> Unit,
    onHoldChange: (Boolean) -> Unit,
    onAutoReadChange: (Boolean) -> Unit,
    onReadSaved: () -> Unit,
    onRetryActive: () -> Unit,
    onEditSaved: () -> Unit,
    onDraftChange: (Int, Boolean) -> Unit,
    onSetAllDraft: (Boolean) -> Unit,
    onDiscardDraft: () -> Unit,
    onWriteDraft: () -> Unit,
) {
    val connected = connectionState == Connected
    var showCatalog by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val titles = stringArrayResource(R.array.errors_ecu_errors_names)
    val blinkCodes = integerArrayResource(R.array.errors_ecu_blink_codes)
    val activeMask = snapshot.activeMask.takeIf { connected }
    val savedMask = snapshot.savedMask.takeIf { connected }
    val draft = snapshot.draftMask.takeIf { connected }
    val reportedMask = (activeMask ?: 0) or (savedMask ?: 0)
    val hasUnknownErrors = reportedMask ushr titles.size != 0
    val errors = titles.mapIndexed { bit, title ->
        CheckEngineError(
            bit = bit,
            title = title,
            blinkCode = blinkCodes[bit],
            isActive = activeMask?.getBitValue(bit) == 1,
            isSaved = savedMask?.getBitValue(bit) == 1,
        )
    }
    val showAll = showCatalog || draft != null
    val visibleErrors = if (showAll) errors else errors.filter { it.isActive || it.isSaved }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column {
            SecuTopAppBar(
                title = stringResource(R.string.errors_title_ecu),
                connectionState = connectionState,
                showStatus = true,
                onNavigateUp = onBackClick,
                actions = {
                    IconButton(
                        modifier = Modifier.testTag("errors-clear-button"),
                        enabled = connected && snapshot.canClearErrors,
                        onClick = onClearClick,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_outline_24),
                            contentDescription = stringResource(R.string.clear_all_errors),
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.errors_settings))
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.errors_help))
                    }
                },
            )
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("errors-list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (connected) {
                    item(key = "controls") {
                        SettingsSection {
                            ErrorToggle(
                                text = stringResource(R.string.errors_live_mode),
                                checked = snapshot.liveEnabled,
                                onCheckedChange = onLiveChange,
                            )
                            if (snapshot.liveEnabled) {
                                ReadState(
                                    status = snapshot.activeReadStatus,
                                    count = activeMask?.countOneBits(),
                                    readingText = R.string.errors_active_loading,
                                    timeoutText = R.string.errors_active_read_timeout,
                                    countText = R.string.errors_active_count,
                                )
                                if (snapshot.activeReadStatus == ErrorsReadStatus.Timeout) {
                                    TextButton(onClick = onRetryActive, modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                            ReadState(
                                status = snapshot.savedReadStatus,
                                count = savedMask?.countOneBits(),
                                readingText = R.string.errors_saved_loading,
                                timeoutText = R.string.errors_saved_read_timeout,
                                countText = R.string.errors_saved_count,
                            )
                            FlowRow(Modifier.padding(horizontal = 8.dp)) {
                                TextButton(onClick = onReadSaved, enabled = snapshot.canReadSaved) {
                                    Text(stringResource(R.string.errors_read_saved))
                                }
                                if (draft == null) {
                                    TextButton(onClick = onEditSaved, enabled = snapshot.canEditSaved) {
                                        Text(stringResource(R.string.errors_edit_saved))
                                    }
                                }
                            }
                        }
                    }
                    if (snapshot.writeStatus != ErrorsWriteStatus.Idle) {
                        item(key = "write-status") { WriteStatus(snapshot.writeStatus) }
                    }
                }
                item(key = "catalog-controls") {
                    FlowRow {
                        if (draft != null) {
                            TextButton(onClick = { onSetAllDraft(true) }) {
                                Text(stringResource(R.string.errors_select_all))
                            }
                            TextButton(onClick = { onSetAllDraft(false) }) {
                                Text(stringResource(R.string.errors_deselect_all))
                            }
                        } else {
                            TextButton(onClick = { showCatalog = !showCatalog }) {
                                Text(stringResource(if (showCatalog) R.string.errors_show_reported else R.string.errors_show_all))
                            }
                        }
                    }
                }
                if (hasUnknownErrors) {
                    item(key = "unknown") {
                        Text(
                            text = stringResource(R.string.errors_unknown_flags),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (visibleErrors.isEmpty() && reportedMask == 0 && (activeMask != null || savedMask != null)) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.errors_no_reported),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                items(visibleErrors, key = { it.bit }) { error ->
                    ErrorCard(
                        error = error,
                        hasActiveData = activeMask != null,
                        hasSavedData = savedMask != null,
                        draftChecked = draft?.getBitValue(error.bit)?.let { it == 1 },
                        onDraftChange = { onDraftChange(error.bit, it) },
                    )
                }
            }
            if (draft != null) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDiscardDraft) { Text(stringResource(android.R.string.cancel)) }
                        Button(onClick = onWriteDraft, enabled = snapshot.canWriteDraft) {
                            Text(stringResource(R.string.errors_write_saved))
                        }
                    }
                }
            }
        }
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(stringResource(R.string.errors_settings)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ErrorToggle(
                        text = stringResource(R.string.errors_auto_read_saved),
                        checked = snapshot.autoReadSavedErrors == true,
                        enabled = snapshot.autoReadSavedErrors != null,
                        onCheckedChange = onAutoReadChange,
                    )
                    ErrorToggle(
                        text = stringResource(R.string.errors_hold_active),
                        checked = snapshot.holdActiveErrors,
                        onCheckedChange = onHoldChange,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.errors_help)) },
            text = {
                Text(
                    stringResource(R.string.errors_help_text),
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

@Composable
private fun ErrorToggle(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, enabled = enabled, onCheckedChange = null)
    }
}

@Composable
private fun ReadState(
    status: ErrorsReadStatus,
    count: Int?,
    readingText: Int,
    timeoutText: Int,
    countText: Int,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        if (count != null) {
            Text(stringResource(countText, count), style = MaterialTheme.typography.bodyMedium)
        }
        when (status) {
            ErrorsReadStatus.Idle -> Unit
            ErrorsReadStatus.Reading -> {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(stringResource(readingText), style = MaterialTheme.typography.bodyMedium)
                }
            }
            ErrorsReadStatus.Timeout -> Text(
                text = stringResource(timeoutText),
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun WriteStatus(status: ErrorsWriteStatus) {
    val (message, color) = when (status) {
        ErrorsWriteStatus.Idle -> return
        ErrorsWriteStatus.Writing -> R.string.errors_write_busy to MaterialTheme.colorScheme.primary
        ErrorsWriteStatus.Succeeded -> R.string.errors_write_success to MaterialTheme.colorScheme.primary
        ErrorsWriteStatus.AckTimeout -> R.string.errors_write_ack_timeout to MaterialTheme.colorScheme.error
        ErrorsWriteStatus.ReadbackTimeout -> R.string.errors_write_readback_timeout to MaterialTheme.colorScheme.error
    }
    Text(
        text = stringResource(message),
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun ErrorCard(
    error: CheckEngineError,
    hasActiveData: Boolean,
    hasSavedData: Boolean,
    draftChecked: Boolean?,
    onDraftChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("error-card:${error.bit}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        val editModifier = if (draftChecked != null) {
            Modifier.testTag("error-draft:${error.bit}")
                .toggleable(draftChecked, role = Role.Checkbox, onValueChange = onDraftChange)
        } else Modifier
        Row(
            modifier = editModifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.errors_blink_code, error.blinkCode),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(error.title, style = MaterialTheme.typography.titleMedium)
                if (hasActiveData || hasSavedData) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasActiveData) {
                            StateBadge(
                                text = stringResource(R.string.error_state_active),
                                selected = error.isActive,
                                testTag = "error-active:${error.bit}",
                                selectedContainer = MaterialTheme.colorScheme.errorContainer,
                                selectedColor = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        if (hasSavedData) {
                            StateBadge(
                                text = stringResource(R.string.error_state_saved),
                                selected = error.isSaved,
                                testTag = "error-saved:${error.bit}",
                                selectedContainer = LocalSecuPalette.current.warningContainer,
                                selectedColor = LocalSecuPalette.current.onWarningContainer,
                            )
                        }
                    }
                }
            }
            if (draftChecked != null) {
                Checkbox(checked = draftChecked, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun StateBadge(
    text: String,
    selected: Boolean,
    testTag: String,
    selectedContainer: androidx.compose.ui.graphics.Color,
    selectedColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = if (selected) selectedContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag(testTag).semantics { this.selected = selected },
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
    }
}
