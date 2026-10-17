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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.models.CheckEngineError
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.LocalSecuPalette

@Composable
internal fun ErrorsScreen(
    connectionState: ConnectionState?,
    errors: List<CheckEngineError>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    val displayedErrors = errors.filter { it.isActive || it.isSaved }

    SecuComposeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                ErrorsTopBar(
                    connectionState = connectionState,
                    onBackClick = onBackClick,
                    onClearClick = onClearClick,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                if (isLoading) {
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
private fun ErrorsTopBar(
    connectionState: ConnectionState?,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.errors_title_ecu),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
        ConnectionStatusIcon(connectionState = connectionState)
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(ToolbarIconButtonSize)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                .clickable(onClick = onClearClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_outline_24),
                contentDescription = stringResource(R.string.errors_clear_all_short),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ConnectionStatusIcon(connectionState: ConnectionState?) {
    val contentColor = when (connectionState) {
        Connected -> LocalSecuPalette.current.success
        InProgress -> LocalSecuPalette.current.warning
        Disconnected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.size(ToolbarIconButtonSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_car),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
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
