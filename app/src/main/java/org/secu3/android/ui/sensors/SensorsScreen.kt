/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.ui.sensors

import android.view.LayoutInflater
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.anastr.speedviewlib.SpeedView
import org.secu3.android.R
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.ui.sensors.models.IndicatorType
import org.secu3.android.ui.sensors.SensorsViewModel.ChoiceRequest
import java.util.Locale

@Composable
internal fun SensorsScreen(
    gauges: List<GaugeItem>, indicators: List<IndicatorItem>, columns: Int,
    gaugeChoices: ChoiceRequest<GaugeType>?, indicatorChoices: ChoiceRequest<IndicatorType>?,
    onAddGaugeRequest: () -> Unit, onAddIndicatorRequest: () -> Unit,
    onAddGauge: (GaugeType) -> Unit, onAddIndicator: (IndicatorType) -> Unit,
    onGaugeChoicesConsumed: (Long) -> Unit, onIndicatorChoicesConsumed: (Long) -> Unit,
    onDeleteGauge: (GaugeType) -> Unit, onDeleteIndicator: (IndicatorType) -> Unit,
    onUpdateGauge: (GaugeState) -> Unit,
    onReorderGauges: (List<GaugeState>) -> Unit, onReorderIndicators: (List<IndicatorState>) -> Unit,
) {
    var localGauges by remember { mutableStateOf(gauges) }
    var localIndicators by remember { mutableStateOf(indicators) }
    var dragging by remember { mutableStateOf(false) }
    var activeDragKey by remember { mutableStateOf<String?>(null) }
    val gaugePositions = remember { mutableStateMapOf<String, Rect>() }
    val indicatorPositions = remember { mutableStateMapOf<String, Rect>() }
    var pendingGaugeOrder by remember { mutableStateOf<List<GaugeType>?>(null) }
    var pendingIndicatorOrder by remember { mutableStateOf<List<IndicatorType>?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    var gaugeDialog by remember { mutableStateOf<List<GaugeType>?>(null) }
    var indicatorDialog by remember { mutableStateOf<List<IndicatorType>?>(null) }
    val latestGauges by rememberUpdatedState(gauges)
    val latestIndicators by rememberUpdatedState(indicators)
    LaunchedEffect(gauges, pendingGaugeOrder) {
        if (!dragging) {
            val pending = pendingGaugeOrder
            if (pending == null) localGauges = gauges else if (gauges.map { it.state.gaugeType } == pending) {
                pendingGaugeOrder = null; localGauges = gauges
            } else {
                val currentTypes = gauges.map { it.state.gaugeType }
                val reconciled = pending.filter { it in currentTypes } + currentTypes.filter { it !in pending }
                if (reconciled != pending) pendingGaugeOrder = reconciled
                if (currentTypes == reconciled) {
                    pendingGaugeOrder = null; localGauges = gauges
                } else localGauges = reconciled.mapNotNull { type -> gauges.find { it.state.gaugeType == type } }
            }
        }
    }
    LaunchedEffect(indicators, pendingIndicatorOrder) {
        if (!dragging) {
            val pending = pendingIndicatorOrder
            if (pending == null) localIndicators = indicators else if (indicators.map { it.state.indicatorType } == pending) {
                pendingIndicatorOrder = null; localIndicators = indicators
            } else {
                val currentTypes = indicators.map { it.state.indicatorType }
                val reconciled = pending.filter { it in currentTypes } + currentTypes.filter { it !in pending }
                if (reconciled != pending) pendingIndicatorOrder = reconciled
                if (currentTypes == reconciled) {
                    pendingIndicatorOrder = null; localIndicators = indicators
                } else localIndicators = reconciled.mapNotNull { type -> indicators.find { it.state.indicatorType == type } }
            }
        }
    }
    LaunchedEffect(gaugeChoices?.requestId) {
        gaugeChoices?.let { request -> gaugeDialog = request.choices; onGaugeChoicesConsumed(request.requestId) }
    }
    LaunchedEffect(indicatorChoices?.requestId) {
        indicatorChoices?.let { request -> indicatorDialog = request.choices; onIndicatorChoicesConsumed(request.requestId) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 88.dp)) {
            Column(Modifier.fillMaxWidth().testTag("gauges-grid")) {
                localGauges.chunked(columns.coerceAtLeast(1)).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth()) {
                        rowItems.forEach { item ->
                            val dragKey = "g:${item.state.gaugeType.name}"
                            val isDragged = activeDragKey == dragKey
                            Box(Modifier.weight(1f)) {
                                GaugeCard(
                                    item,
                                    Modifier.fillMaxWidth().padding(8.dp)
                                        .onGloballyPositioned { coordinates ->
                                            val bounds = coordinates.boundsInRoot()
                                            if (gaugePositions[dragKey] != bounds) gaugePositions[dragKey] = bounds
                                        }.graphicsLayer {
                                            if (isDragged) { scaleX = 1.04f; scaleY = 1.04f; shadowElevation = 12.dp.toPx() }
                                        }.zIndex(if (isDragged) 1f else 0f).reorderGridItem(
                                            gaugePositions, dragKey,
                                            onStart = { dragging = true; activeDragKey = dragKey },
                                            onTarget = { key ->
                                                val target = GaugeType.entries.firstOrNull { it.name == key } ?: return@reorderGridItem
                                                localGauges = localGauges.moveItem(
                                                    source = { it.state.gaugeType == item.state.gaugeType },
                                                    target = { it.state.gaugeType == target },
                                                )
                                            },
                                            onFinish = {
                                                val orderedTypes = localGauges.map { it.state.gaugeType }
                                                localGauges = (orderedTypes + latestGauges.map { it.state.gaugeType }.filter { it !in orderedTypes })
                                                    .mapNotNull { type -> latestGauges.find { it.state.gaugeType == type } }
                                                pendingGaugeOrder = localGauges.map { it.state.gaugeType }
                                                dragging = false; activeDragKey = null
                                                onReorderGauges(localGauges.mapIndexed { index, value -> value.state.copy(idx = index) })
                                            },
                                        ),
                                    onMenu = { menuFor = dragKey },
                                )
                                DropdownMenu(expanded = menuFor == dragKey, onDismissRequest = { menuFor = null }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.switch_view)) },
                                        leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                                        onClick = {
                                            menuFor = null
                                            val updated = item.state.copy(isNumericView = !item.state.isNumericView)
                                            localGauges = localGauges.map { current -> if (current.state.gaugeType == updated.gaugeType) current.copy(state = updated) else current }
                                            onUpdateGauge(updated)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                        onClick = { menuFor = null; onDeleteGauge(item.state.gaugeType) },
                                    )
                                }
                            }
                        }
                        repeat(columns.coerceAtLeast(1) - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().testTag("indicators-grid")) {
                localIndicators.chunked(columns.coerceAtLeast(1)).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth()) {
                        rowItems.forEach { item ->
                            val dragKey = "i:${item.state.indicatorType.name}"
                            val isDragged = activeDragKey == dragKey
                            Box(Modifier.weight(1f)) {
                                IndicatorCard(
                                    item,
                                    Modifier.fillMaxWidth().padding(8.dp)
                                        .onGloballyPositioned { coordinates ->
                                            val bounds = coordinates.boundsInRoot()
                                            if (indicatorPositions[dragKey] != bounds) indicatorPositions[dragKey] = bounds
                                        }.graphicsLayer {
                                            if (isDragged) { scaleX = 1.04f; scaleY = 1.04f; shadowElevation = 12.dp.toPx() }
                                        }.zIndex(if (isDragged) 1f else 0f).reorderGridItem(
                                            indicatorPositions, dragKey,
                                            onStart = { dragging = true; activeDragKey = dragKey },
                                            onTarget = { key ->
                                                val target = IndicatorType.entries.firstOrNull { it.name == key } ?: return@reorderGridItem
                                                localIndicators = localIndicators.moveItem(
                                                    source = { it.state.indicatorType == item.state.indicatorType },
                                                    target = { it.state.indicatorType == target },
                                                )
                                            },
                                            onFinish = {
                                                val orderedTypes = localIndicators.map { it.state.indicatorType }
                                                localIndicators = (orderedTypes + latestIndicators.map { it.state.indicatorType }.filter { it !in orderedTypes })
                                                    .mapNotNull { type -> latestIndicators.find { it.state.indicatorType == type } }
                                                pendingIndicatorOrder = localIndicators.map { it.state.indicatorType }
                                                dragging = false; activeDragKey = null
                                                onReorderIndicators(localIndicators.mapIndexed { index, value -> value.state.copy(idx = index) })
                                            },
                                        ),
                                    onMenu = { menuFor = dragKey },
                                )
                                DropdownMenu(expanded = menuFor == dragKey, onDismissRequest = { menuFor = null }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                        onClick = { menuFor = null; onDeleteIndicator(item.state.indicatorType) },
                                    )
                                }
                            }
                        }
                        repeat(columns.coerceAtLeast(1) - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
            if (fabExpanded) {
                SmallFloatingActionButton(onClick = { fabExpanded = false; onAddIndicatorRequest() }, modifier = Modifier.padding(bottom = 8.dp).testTag("sensors-add-indicator"), containerColor = colorResource(R.color.gauge_blue), contentColor = Color.White) { Icon(painterResource(R.drawable.ic_indicator), stringResource(R.string.sensors_add_indicator)) }
                SmallFloatingActionButton(onClick = { fabExpanded = false; onAddGaugeRequest() }, modifier = Modifier.padding(bottom = 8.dp).testTag("sensors-add-gauge"), containerColor = colorResource(R.color.gauge_blue), contentColor = Color.White) { Icon(painterResource(R.drawable.ic_gauge), stringResource(R.string.sensors_add_gauge)) }
            }
            FloatingActionButton(onClick = { fabExpanded = !fabExpanded }, modifier = Modifier.testTag("sensors-add"), containerColor = colorResource(R.color.colorPrimaryVariant), contentColor = Color.White) { Icon(Icons.Default.Add, stringResource(R.string.sensors_add)) }
        }
    }

    gaugeDialog?.let { choices ->
        AlertDialog(onDismissRequest = { gaugeDialog = null }, title = { Text(stringResource(R.string.select_gauge_to_add)) },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { choices.forEach { type -> TextButton(onClick = { gaugeDialog = null; onAddGauge(type) }) { Text(stringResource(type.title)) } } } },
            confirmButton = { TextButton(onClick = { gaugeDialog = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
    indicatorDialog?.let { choices ->
        AlertDialog(onDismissRequest = { indicatorDialog = null }, title = { Text(stringResource(R.string.select_indicator_to_add)) },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { choices.forEach { type -> TextButton(onClick = { indicatorDialog = null; onAddIndicator(type) }) { Text(stringResource(type.title)) } } } },
            confirmButton = { TextButton(onClick = { indicatorDialog = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
}

@Composable
private fun GaugeCard(item: GaugeItem, modifier: Modifier, onMenu: () -> Unit) {
    val type = item.state.gaugeType
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth().testTag("gauge-${type.name}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, colorResource(R.color.grey)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onMenu,
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(type.title), fontSize = 18.sp)
            if (item.state.isNumericView) {
                Spacer(Modifier.height(32.dp)); Text(item.value, fontSize = 36.sp); Text(stringResource(type.units), fontSize = 18.sp); Spacer(Modifier.height(32.dp))
            } else {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight().testTag("dial-${type.name}"),
                        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.view_gauge_compose_dial, null, false) as SpeedView },
                        update = { gauge ->
                            gauge.unit = context.getString(type.units)
                            gauge.maxSpeed = 10_000f
                            gauge.minSpeed = type.minValue
                            gauge.maxSpeed = type.maxValue
                            gauge.tickNumber = type.tickCount
                            gauge.clearSections(); gauge.addSections(type.getSections(context, gauge.speedometerWidth))
                            gauge.speedTextListener = if (type.isInteger) ({ speed -> speed.toInt().toString() }) else ({ speed -> "%.1f".format(Locale.US, speed) })
                            gauge.setSpeedAt(item.value.toFloatOrNull() ?: 0f)
                        },
                        onRelease = { gauge -> gauge.speedTextListener = { speed -> speed.toString() } },
                    )
            }
        }
    }
}

@Composable
private fun IndicatorCard(item: IndicatorItem, modifier: Modifier, onMenu: () -> Unit) {
    val accessibilityState = stringResource(if (item.isActive) R.string.sensors_status_accessibility_on else R.string.sensors_status_accessibility_off)
    Card(modifier = modifier.fillMaxWidth().testTag("indicator-${item.state.indicatorType.name}").semantics {
        stateDescription = accessibilityState
    }, colors = CardDefaults.cardColors(
        containerColor = if (item.isActive) Color.Green else Color.White,
    ), border = BorderStroke(1.dp, colorResource(R.color.grey)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), onClick = onMenu) {
        Box(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(item.state.indicatorType.title), fontSize = 18.sp)
        }
    }
}

private fun <T> List<T>.moveItem(source: (T) -> Boolean, target: (T) -> Boolean): List<T> {
    val from = indexOfFirst(source)
    val to = indexOfFirst(target)
    if (from < 0 || to < 0 || from == to) return this
    return toMutableList().also { list -> list.add(to, list.removeAt(from)) }
}

private fun Modifier.reorderGridItem(
    positions: Map<String, Rect>,
    itemKey: String,
    onStart: () -> Unit,
    onTarget: (String) -> Unit,
    onFinish: () -> Unit,
): Modifier = pointerInput(positions, itemKey) {
    var dragPosition = Offset.Zero
    detectDragGesturesAfterLongPress(
        onDragStart = {
            val bounds = positions[itemKey]
            if (bounds != null) {
                dragPosition = bounds.center
                onStart()
            }
        },
        onDragEnd = onFinish,
        onDragCancel = onFinish,
        onDrag = { change, delta ->
            change.consume()
            dragPosition += delta
            val target = positions.entries.minByOrNull { (_, bounds) ->
                val dx = bounds.center.x - dragPosition.x
                val dy = bounds.center.y - dragPosition.y
                dx * dx + dy * dy
            }
            if (target != null && target.key != itemKey) onTarget(target.key.removePrefix("g:").removePrefix("i:"))
        },
    )
}
