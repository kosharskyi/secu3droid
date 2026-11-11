/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.ltft

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.domain.ltft.LtftMapsSession
import org.secu3.android.domain.ltft.LtftMapsState
import org.secu3.android.domain.ltft.LtftWorkingPoint
import org.secu3.android.domain.ltft.LtftWriteOperation
import org.secu3.android.domain.ltft.LtftWriteState
import org.secu3.android.ui.compose.ChoiceDialog
import org.secu3.android.ui.compose.ConfirmationDialog
import org.secu3.android.ui.compose.SecuTopAppBar
import java.text.DecimalFormatSymbols

@Composable
internal fun LtftMapsScreen(
    state: LtftMapsState,
    connectionState: ConnectionState?,
    writeState: LtftWriteState,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onCopy: (List<Float>, IntRange, IntRange) -> Unit,
    onExportCsv: (Int, List<Float>, List<Int>, Char) -> Unit,
    onApplyToVe: ((List<List<Float>>) -> Unit)? = null,
) {
    val connected = connectionState == Connected
    var selectedTable by rememberSaveable { mutableIntStateOf(0) }
    val ready = state.takeIf { connected } as? LtftMapsState.Ready
    var selection by remember(selectedTable, ready?.tables) { mutableStateOf<LtftCellSelection?>(null) }
    LifecycleResumeEffect(selectedTable, ready?.tables) {
        onPauseOrDispose { selection = null }
    }
    val writeMessage = when (writeState) {
        LtftWriteState.Idle -> null
        is LtftWriteState.AwaitingAcknowledgement -> when (writeState.operation) {
            LtftWriteOperation.Reset -> R.string.ltft_maps_reset_pending
            LtftWriteOperation.Save -> R.string.ltft_maps_save_pending
        }
        LtftWriteState.ReadingAfterReset -> R.string.ltft_maps_reading_after_reset
        is LtftWriteState.Succeeded -> when (writeState.operation) {
            LtftWriteOperation.Reset -> R.string.ltft_maps_reset_success
            LtftWriteOperation.Save -> R.string.ltft_maps_save_success
        }
        is LtftWriteState.Unconfirmed -> when (writeState.operation) {
            LtftWriteOperation.Reset -> R.string.ltft_maps_reset_unconfirmed
            LtftWriteOperation.Save -> R.string.ltft_maps_save_unconfirmed
        }
        LtftWriteState.ResetReadbackFailed -> R.string.ltft_maps_reset_readback_failed
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            SecuTopAppBar(
                title = stringResource(R.string.ltft_maps_title),
                connectionState = connectionState,
                showStatus = true,
                onNavigateUp = onNavigateUp,
                actions = {
                    if (ready != null) {
                        IconButton(
                            onClick = {
                                selection = null
                                onRefresh()
                            },
                            enabled = !writeState.isInProgress,
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                        LtftMapActions(
                            enabled = !writeState.isInProgress,
                            onReset = onReset,
                            onSave = onSave,
                            onCopy = selection?.let { selected ->
                                { onCopy(ready.tables[selectedTable], selected.rows, selected.columns) }
                            },
                            onExportCsv = { decimalSeparator ->
                                onExportCsv(selectedTable, ready.tables[selectedTable], ready.rpmAxis, decimalSeparator)
                            },
                            onApplyToVe = onApplyToVe?.let { apply -> { apply(ready.tables) } },
                        )
                    }
                },
            )

            when (val visibleState = if (connected) state else LtftMapsState.Disconnected) {
                LtftMapsState.Disconnected -> LtftMapMessage(R.string.ltft_maps_disconnected)
                LtftMapsState.Unsupported -> LtftMapMessage(R.string.ltft_maps_unsupported)
                LtftMapsState.Reading -> LtftMapMessage(
                    if (writeState == LtftWriteState.ReadingAfterReset) {
                        R.string.ltft_maps_reading_after_reset
                    } else {
                        R.string.ltft_maps_reading
                    },
                    isReading = true,
                )
                is LtftMapsState.TimedOut -> {
                    val message = when (visibleState) {
                        LtftMapsState.TimedOut.Read -> R.string.ltft_maps_timeout
                        LtftMapsState.TimedOut.ResetAcknowledgement -> R.string.ltft_maps_reset_unconfirmed
                        LtftMapsState.TimedOut.ResetReadback -> R.string.ltft_maps_reset_readback_failed
                    }
                    LtftMapMessage(
                        message,
                        onRetry = onRefresh,
                        retryLabel = if (visibleState == LtftMapsState.TimedOut.Read) {
                            R.string.retry
                        } else {
                            R.string.ltft_maps_read_again
                        },
                    )
                }
                is LtftMapsState.Ready -> Column(
                    modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (writeMessage != null) {
                        Text(
                            text = stringResource(writeMessage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (writeState is LtftWriteState.Unconfirmed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        repeat(LtftMapsSession.TABLE_COUNT) { table ->
                            SegmentedButton(
                                selected = selectedTable == table,
                                onClick = { selectedTable = table },
                                shape = SegmentedButtonDefaults.itemShape(table, LtftMapsSession.TABLE_COUNT),
                                label = { Text(stringResource(R.string.ltft_maps_table, table + 1)) },
                            )
                        }
                    }
                    val loadUnit = stringResource(
                        if (visibleState.isLoadPressure) R.string.units_pressure_kpa else R.string.units_percents,
                    )
                    Text(
                        text = stringResource(R.string.ltft_maps_axes, loadUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (visibleState.loadAxis == null) {
                        Text(
                            stringResource(R.string.ltft_maps_waiting_baro),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    LtftMapTable(
                        rpmAxis = visibleState.rpmAxis,
                        loadAxis = visibleState.loadAxis,
                        loadUnit = loadUnit,
                        corrections = visibleState.tables[selectedTable],
                        workingPoint = visibleState.workingPoint,
                        selection = selection,
                        onSelectCell = { row, column ->
                            val index = row * LtftMapsSession.AXIS_SIZE + column
                            val current = selection
                            selection = if (current != null && current.end == null) {
                                current.copy(end = index)
                            } else {
                                LtftCellSelection(index)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LtftMapActions(
    enabled: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onCopy: (() -> Unit)?,
    onExportCsv: (Char) -> Unit,
    onApplyToVe: (() -> Unit)?,
) {
    var menuExpanded by remember(enabled) { mutableStateOf(false) }
    var confirmReset by remember(enabled) { mutableStateOf(false) }
    var showCsvOptions by remember(enabled) { mutableStateOf(false) }
    LifecycleResumeEffect(enabled) {
        onPauseOrDispose {
            menuExpanded = false
            confirmReset = false
            showCsvOptions = false
        }
    }

    Box {
        IconButton(onClick = { menuExpanded = true }, enabled = enabled) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.ltft_maps_actions))
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ltft_maps_copy_selection)) },
                enabled = onCopy != null,
                onClick = {
                    menuExpanded = false
                    onCopy?.invoke()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ltft_maps_export_csv)) },
                onClick = {
                    menuExpanded = false
                    showCsvOptions = true
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ltft_ve_apply)) },
                enabled = onApplyToVe != null,
                onClick = {
                    menuExpanded = false
                    onApplyToVe?.invoke()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ltft_maps_save_action)) },
                onClick = {
                    menuExpanded = false
                    onSave()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ltft_maps_reset_action)) },
                onClick = {
                    menuExpanded = false
                    confirmReset = true
                },
            )
        }
    }

    if (showCsvOptions) {
        val locale = LocalConfiguration.current.locales[0]
        var separator by remember(locale) {
            mutableStateOf(if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') ',' else '.')
        }
        ChoiceDialog(
            title = { Text(stringResource(R.string.ltft_maps_csv_decimal_separator)) },
            selected = separator,
            choices = listOf('.', ','),
            label = { stringResource(if (it == '.') R.string.ltft_maps_decimal_point else R.string.ltft_maps_decimal_comma) },
            onSelect = { separator = it },
            onDismiss = { showCsvOptions = false },
            confirmButton = {
                TextButton(onClick = { showCsvOptions = false }) { Text(stringResource(android.R.string.cancel)) }
                Button(onClick = {
                    showCsvOptions = false
                    onExportCsv(separator)
                }) { Text(stringResource(R.string.ltft_maps_export_csv)) }
            },
        )
    }

    if (confirmReset) {
        ConfirmationDialog(
            title = R.string.ltft_maps_reset_action,
            message = R.string.ltft_maps_reset_confirmation,
            onConfirm = {
                confirmReset = false
                onReset()
            },
            onCancel = { confirmReset = false },
        )
    }
}

@Composable
private fun LtftMapMessage(
    text: Int,
    isReading: Boolean = false,
    onRetry: (() -> Unit)? = null,
    retryLabel: Int = R.string.retry,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isReading) CircularProgressIndicator()
        Text(
            stringResource(text),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(stringResource(retryLabel)) }
        }
    }
}

@Composable
private fun LtftMapTable(
    rpmAxis: List<Int>,
    loadAxis: List<Float>?,
    loadUnit: String,
    corrections: List<Float>,
    workingPoint: LtftWorkingPoint?,
    selection: LtftCellSelection?,
    onSelectCell: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val locale = LocalConfiguration.current.locales[0]
    val correctionsText = remember(corrections, locale) { corrections.map { String.format(locale, "%.1f", it) } }
    val loadLabels = remember(loadAxis, locale) { loadAxis?.map { String.format(locale, "%.1f", it) } }
    val lastIndex = LtftMapsSession.AXIS_SIZE - 1

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                LtftMapCell(loadUnit, axisLabel = true)
                Row(Modifier.weight(1f).horizontalScroll(horizontal, enabled = false)) {
                    rpmAxis.forEach { rpm -> LtftMapCell(rpm.toString(), axisLabel = true) }
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxHeight().verticalScroll(vertical, enabled = false)) {
                    for (row in lastIndex downTo 0) {
                        LtftMapCell(loadLabels?.get(row) ?: "—", axisLabel = true)
                    }
                }
                Box(
                    Modifier.weight(1f).fillMaxHeight().testTag("ltft-map-table")
                        .horizontalScroll(horizontal).verticalScroll(vertical),
                ) {
                    LtftMapCells(correctionsText, selection, onSelectCell)
                    if (workingPoint != null) {
                        val markerDescription = stringResource(
                            R.string.ltft_maps_operating_point,
                            workingPoint.rpm,
                            stringResource(R.string.units_rpm),
                            String.format(locale, "%.1f", workingPoint.load),
                            loadUnit,
                        )
                        LtftMapMarker(
                            workingPoint,
                            Modifier.matchParentSize().semantics { contentDescription = markerDescription },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LtftMapCells(
    corrections: List<String>,
    selection: LtftCellSelection?,
    onSelectCell: (Int, Int) -> Unit,
) {
    val axisSize = LtftMapsSession.AXIS_SIZE
    val rows = selection?.rows
    val columns = selection?.columns
    Column {
        for (row in axisSize - 1 downTo 0) {
            Row {
                repeat(axisSize) { column ->
                    val selected = rows?.contains(row) == true && columns?.contains(column) == true
                    LtftMapCell(
                        text = corrections[row * axisSize + column],
                        highlighted = selected,
                        modifier = Modifier.testTag("ltft-cell-$row-$column")
                            .selectable(selected = selected, onClick = { onSelectCell(row, column) }),
                    )
                }
            }
        }
    }
}

@Composable
private fun LtftMapCell(
    text: String,
    axisLabel: Boolean = false,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val background = when {
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        axisLabel -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = modifier.width(CellWidth).height(CellHeight)
            .background(background).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun LtftMapMarker(point: LtftWorkingPoint, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier) {
        val center = Offset(
            x = (point.rpmIndex + 0.5f) * CellWidth.roundToPx(),
            y = (LtftMapsSession.AXIS_SIZE - point.loadIndex - 0.5f) * CellHeight.roundToPx(),
        )
        drawCircle(outline, radius = 5.dp.toPx(), center = center)
        drawCircle(color, radius = 3.5.dp.toPx(), center = center)
    }
}

private data class LtftCellSelection(val start: Int, val end: Int? = null) {
    val rows: IntRange
        get() = bounds(start / LtftMapsSession.AXIS_SIZE, (end ?: start) / LtftMapsSession.AXIS_SIZE)
    val columns: IntRange
        get() = bounds(start % LtftMapsSession.AXIS_SIZE, (end ?: start) % LtftMapsSession.AXIS_SIZE)

    private fun bounds(first: Int, last: Int) = minOf(first, last)..maxOf(first, last)
}

private val CellWidth = 68.dp
private val CellHeight = 36.dp
