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

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.ui.compose.ConfirmationDialog
import org.secu3.android.R
import org.secu3.android.connection.ConnectionState
import org.secu3.android.models.CheckEngineError
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.LocalSecuPalette
import org.secu3.android.ui.compose.SecuTopAppBar

@Composable
internal fun ErrorsContent(
    connectionState: ConnectionState?,
    snapshot: ErrorsSnapshot,
    errorTitles: List<String>,
    canClearErrors: Boolean,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onRetryClick: () -> Unit = {},
) {
    val errors = if (snapshot.saved != null && snapshot.active != null) errorTitles.mapIndexed { index, title ->
        CheckEngineError(
            title = title,
            isActive = snapshot.active.isError(index),
            isSaved = snapshot.saved.isError(index),
        )
    } else emptyList()
    ErrorsScreen(
        connectionState = connectionState,
        errors = errors,
        isLoading = snapshot.saved == null || snapshot.active == null,
        clearStatus = snapshot.clearStatus,
        readStatus = snapshot.readStatus,
        canClearErrors = canClearErrors,
        onBackClick = onBackClick,
        onClearClick = onClearClick,
        onRetryClick = onRetryClick,
    )
}

@Composable
internal fun ErrorsScreen(
    connectionState: ConnectionState?,
    errors: List<CheckEngineError>,
    isLoading: Boolean,
    clearStatus: ErrorsClearStatus = ErrorsClearStatus.Idle,
    canClearErrors: Boolean = true,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    readStatus: ErrorsReadStatus = ErrorsReadStatus.Idle,
    onRetryClick: () -> Unit = {},
) {
    val displayedErrors = errors.filter { it.isActive || it.isSaved }
    val isClearing = clearStatus == ErrorsClearStatus.Clearing

    SecuComposeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                SecuTopAppBar(
                    title = stringResource(R.string.errors_title_ecu),
                    connectionState = connectionState,
                    onNavigateUp = onBackClick,
                    showStatus = true,
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(ToolbarIconButtonSize)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                                .testTag("errors-clear-button")
                                .clickable(enabled = canClearErrors && !isClearing, onClick = onClearClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_outline_24),
                                contentDescription = stringResource(R.string.errors_clear_all_short),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                if (clearStatus != ErrorsClearStatus.Idle) {
                    ClearStatusMessage(clearStatus)
                }
                if (readStatus != ErrorsReadStatus.Idle) {
                    ReadFailureState(
                        status = readStatus,
                        retryEnabled = connectionState == org.secu3.android.connection.Connected && !isClearing,
                        onRetryClick = onRetryClick,
                    )
                } else if (isLoading) {
                    LoadingErrorsState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "summary") {
                            ErrorsSummary(errors = errors)
                        }
                        if (displayedErrors.isEmpty()) {
                            item(key = "empty") {
                                EmptyErrorsState(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(horizontal = 4.dp),
                                )
                            }
                        } else {
                            items(displayedErrors, key = { it.title }) { error ->
                                ErrorCard(error = error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadFailureState(status: ErrorsReadStatus, retryEnabled: Boolean, onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(if (status == ErrorsReadStatus.SavedTimeout) R.string.errors_saved_read_timeout else R.string.errors_active_read_timeout),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        androidx.compose.material3.TextButton(onClick = onRetryClick, enabled = retryEnabled, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun ClearStatusMessage(status: ErrorsClearStatus) {
    val (message, color) = when (status) {
        ErrorsClearStatus.Idle -> return
        ErrorsClearStatus.Clearing -> R.string.errors_clear_busy to MaterialTheme.colorScheme.primary
        ErrorsClearStatus.Succeeded -> R.string.errors_clear_success to MaterialTheme.colorScheme.primary
        ErrorsClearStatus.AckTimeout -> R.string.errors_clear_ack_timeout to MaterialTheme.colorScheme.error
        ErrorsClearStatus.ReadbackTimeout -> R.string.errors_clear_readback_timeout to MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("errors-clear-status")
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == ErrorsClearStatus.Clearing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        Text(text = stringResource(message), color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorsSummary(errors: List<CheckEngineError>) {
    val activeCount = errors.count { it.isActive }
    val savedCount = errors.count { it.isSaved }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(1.5.dp, LocalSecuPalette.current.warning, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "!",
                color = LocalSecuPalette.current.warning,
            fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.errors_summary, activeCount, savedCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ErrorCard(error: CheckEngineError) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(errorCardTag(error.title))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = error.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StateBadge(
                text = stringResource(R.string.error_state_active),
                selected = error.isActive,
                testTag = errorStatusTag(error.title, ErrorStatus.Active),
                selectedColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedContainer = MaterialTheme.colorScheme.errorContainer,
                selectedStroke = MaterialTheme.colorScheme.error,
            )
            StateBadge(
                text = stringResource(R.string.error_state_saved),
                selected = error.isSaved,
                testTag = errorStatusTag(error.title, ErrorStatus.Saved),
                selectedColor = LocalSecuPalette.current.onWarningContainer,
                selectedContainer = LocalSecuPalette.current.warningContainer,
                selectedStroke = LocalSecuPalette.current.warning,
            )
        }
    }
}

@Composable
private fun StateBadge(
    text: String,
    selected: Boolean,
    testTag: String,
    selectedColor: androidx.compose.ui.graphics.Color,
    selectedContainer: androidx.compose.ui.graphics.Color,
    selectedStroke: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = text,
        color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .testTag(testTag)
            .semantics {
                this.selected = selected
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) selectedContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = if (selected) selectedStroke else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun EmptyErrorsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.errors_empty_state),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun LoadingErrorsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = LocalSecuPalette.current.warning,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = stringResource(R.string.errors_loading_state),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private val ToolbarIconButtonSize = 48.dp

internal enum class ErrorStatus {
    Active,
    Saved,
}

internal fun errorCardTag(title: String): String = "error-card:$title"

internal fun errorStatusTag(title: String, status: ErrorStatus): String = "error-status:$title:${status.name}"
