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

import org.secu3.android.ui.compose.DropdownShape

import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.secu3.android.R
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.ui.sensors.models.IndicatorType
import org.secu3.android.ui.sensors.SensorsViewModel.ChoiceRequest
import org.secu3.android.ui.compose.LocalSecuPalette
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun SensorsScreen(
    gauges: List<GaugeItem>, indicators: List<IndicatorItem>,
    gaugeChoices: ChoiceRequest<GaugeType>?, indicatorChoices: ChoiceRequest<IndicatorType>?,
    onAddGaugeRequest: () -> Unit, onAddIndicatorRequest: () -> Unit,
    onAddGauge: (GaugeType) -> Unit, onAddIndicator: (IndicatorType) -> Unit,
    onGaugeChoicesConsumed: (Long) -> Unit, onIndicatorChoicesConsumed: (Long) -> Unit,
    onDeleteGauge: (GaugeType) -> Unit, onDeleteIndicator: (IndicatorType) -> Unit,
    onUpdateGauge: (GaugeState) -> Unit,
    onReorderGauges: (List<GaugeState>) -> Unit, onReorderIndicators: (List<IndicatorState>) -> Unit,
    onReorderingChanged: (Boolean) -> Unit = {},
    dataAvailable: Boolean = true,
    gaugeStyle: SensorGaugeStyle = SensorGaugeStyle.Default,
) {
    var localGauges by remember { mutableStateOf(gauges) }
    var localIndicators by remember { mutableStateOf(indicators) }
    var dragging by remember { mutableStateOf(false) }
    var activeDragKey by remember { mutableStateOf<String?>(null) }
    var activeDragOffset by remember { mutableStateOf(Offset.Zero) }
    var gaugeDragTarget by remember { mutableStateOf<GaugeType?>(null) }
    var indicatorDragTarget by remember { mutableStateOf<IndicatorType?>(null) }
    var gaugeDragBaseOrder by remember { mutableStateOf<List<GaugeType>>(emptyList()) }
    var indicatorDragBaseOrder by remember { mutableStateOf<List<IndicatorType>>(emptyList()) }
    var gaugePreviewOrder by remember { mutableStateOf<List<GaugeType>>(emptyList()) }
    var indicatorPreviewOrder by remember { mutableStateOf<List<IndicatorType>>(emptyList()) }
    var dragBasePositions by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    val previewAnimations = remember { mutableMapOf<String, Animatable<Offset, AnimationVector2D>>() }
    var pendingGaugeDrop by remember { mutableStateOf<Pair<GaugeType, GaugeType>?>(null) }
    var pendingIndicatorDrop by remember { mutableStateOf<Pair<IndicatorType, IndicatorType>?>(null) }
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
    val latestDragging by rememberUpdatedState(dragging)
    val latestReorderingChanged by rememberUpdatedState(onReorderingChanged)
    fun setReordering(active: Boolean) {
        if (dragging != active) {
            dragging = active
            onReorderingChanged(active)
        }
    }
    DisposableEffect(Unit) {
        onDispose { if (latestDragging) latestReorderingChanged(false) }
    }
    LaunchedEffect(gauges, pendingGaugeOrder, dragging) {
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
    LaunchedEffect(indicators, pendingIndicatorOrder, dragging) {
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
    LaunchedEffect(pendingGaugeDrop) {
        val drop = pendingGaugeDrop ?: return@LaunchedEffect
        var elapsedNanos = 0L
        var previousFrame = withFrameNanos { it }
        while (elapsedNanos < PREVIEW_ANIMATION_NANOS && previewAnimations.values.any { it.isRunning }) {
            val frame = withFrameNanos { it }
            elapsedNanos += frame - previousFrame
            previousFrame = frame
        }
        previewAnimations.values.forEach { it.snapTo(Offset.Zero) }
        localGauges = localGauges.moveItem({ it.state.gaugeType == drop.first }, { it.state.gaugeType == drop.second })
        val orderedTypes = localGauges.map { it.state.gaugeType }
        localGauges = (orderedTypes + latestGauges.map { it.state.gaugeType }.filter { it !in orderedTypes })
            .mapNotNull { type -> latestGauges.find { it.state.gaugeType == type } }
        pendingGaugeOrder = localGauges.map { it.state.gaugeType }
        onReorderGauges(localGauges.mapIndexed { index, value -> value.state.copy(idx = index) })
        pendingGaugeDrop = null
        setReordering(false); activeDragKey = null; activeDragOffset = Offset.Zero; gaugeDragTarget = null
        gaugePreviewOrder = emptyList(); gaugeDragBaseOrder = emptyList(); dragBasePositions = emptyMap()
    }
    LaunchedEffect(pendingIndicatorDrop) {
        val drop = pendingIndicatorDrop ?: return@LaunchedEffect
        var elapsedNanos = 0L
        var previousFrame = withFrameNanos { it }
        while (elapsedNanos < PREVIEW_ANIMATION_NANOS && previewAnimations.values.any { it.isRunning }) {
            val frame = withFrameNanos { it }
            elapsedNanos += frame - previousFrame
            previousFrame = frame
        }
        previewAnimations.values.forEach { it.snapTo(Offset.Zero) }
        localIndicators = localIndicators.moveItem({ it.state.indicatorType == drop.first }, { it.state.indicatorType == drop.second })
        val orderedTypes = localIndicators.map { it.state.indicatorType }
        localIndicators = (orderedTypes + latestIndicators.map { it.state.indicatorType }.filter { it !in orderedTypes })
            .mapNotNull { type -> latestIndicators.find { it.state.indicatorType == type } }
        pendingIndicatorOrder = localIndicators.map { it.state.indicatorType }
        onReorderIndicators(localIndicators.mapIndexed { index, value -> value.state.copy(idx = index) })
        pendingIndicatorDrop = null
        setReordering(false); activeDragKey = null; activeDragOffset = Offset.Zero; indicatorDragTarget = null
        indicatorPreviewOrder = emptyList(); indicatorDragBaseOrder = emptyList(); dragBasePositions = emptyMap()
    }
    LaunchedEffect(gaugeChoices?.requestId) {
        gaugeChoices?.let { request -> gaugeDialog = request.choices; onGaugeChoicesConsumed(request.requestId) }
    }
    LaunchedEffect(indicatorChoices?.requestId) {
        indicatorChoices?.let { request -> indicatorDialog = request.choices; onIndicatorChoicesConsumed(request.requestId) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val minimumGaugeColumnWidth = 150.dp * fontScale.coerceAtLeast(1f)
        val renderColumns = (maxWidth / minimumGaugeColumnWidth).toInt().coerceAtLeast(1)
        val gridCellWidth = maxWidth / renderColumns
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = !dragging).padding(bottom = 88.dp)) {
            Column(Modifier.fillMaxWidth().zIndex(if (activeDragKey?.startsWith("g:") == true) 2f else 0f).testTag("gauges-grid")) {
                localGauges.chunked(renderColumns).forEach { rowItems ->
                    Row(
                        Modifier.fillMaxWidth().zIndex(if (rowItems.any { activeDragKey == "g:${it.state.gaugeType.name}" }) 1f else 0f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        rowItems.forEach { item ->
                            val dragKey = "g:${item.state.gaugeType.name}"
                            val isDragged = activeDragKey == dragKey
                            val previewOffset = if (!isDragged && activeDragKey?.startsWith("g:") == true) {
                                dragSlotOffset(dragKey, gaugeDragBaseOrder.map { "g:${it.name}" }, gaugePreviewOrder.map { "g:${it.name}" }, dragBasePositions)
                            } else Offset.Zero
                            val previewAnimation = remember(dragKey) {
                                previewAnimations.getOrPut(dragKey) { Animatable(Offset.Zero, Offset.VectorConverter) }
                            }
                            val animatedPreviewOffset = previewAnimation.value
                            LaunchedEffect(dragKey, previewOffset) {
                                previewAnimation.animateTo(previewOffset, tween(PREVIEW_ANIMATION_MILLIS))
                            }
                            key(dragKey) { Box(Modifier.width(gridCellWidth).zIndex(if (isDragged) 2f else 0f), contentAlignment = Alignment.TopCenter) {
                                GaugeCard(
                                    if (dataAvailable) item else item.copy(value = "—"),
                                    gaugeStyle,
                                    Modifier.fillMaxWidth().padding(8.dp)
                                        .onGloballyPositioned { coordinates ->
                                            val bounds = coordinates.boundsInRoot()
                                            if (gaugePositions[dragKey] != bounds) gaugePositions[dragKey] = bounds
                                        }.graphicsLayer {
                                            if (isDragged) {
                                                scaleX = 1.04f; scaleY = 1.04f; shadowElevation = 12.dp.toPx()
                                                translationX = activeDragOffset.x; translationY = activeDragOffset.y
                                            } else {
                                                translationX = animatedPreviewOffset.x; translationY = animatedPreviewOffset.y
                                            }
                                        }.zIndex(if (isDragged) 1f else 0f).reorderGridItem(
                                            gaugePositions, dragKey,
                                            onStart = {
                                                setReordering(true); activeDragKey = dragKey; activeDragOffset = Offset.Zero
                                                gaugeDragTarget = item.state.gaugeType
                                                gaugeDragBaseOrder = localGauges.map { it.state.gaugeType }
                                                gaugePreviewOrder = gaugeDragBaseOrder
                                                dragBasePositions = gaugePositions.toMap()
                                            },
                                            onDragDelta = { activeDragOffset += it },
                                            onTarget = { key ->
                                                val target = GaugeType.entries.firstOrNull { it.name == key } ?: return@reorderGridItem
                                                gaugeDragTarget = target
                                                gaugePreviewOrder = gaugeDragBaseOrder.moveItem(
                                                    source = { it == item.state.gaugeType }, target = { it == target },
                                                )
                                            },
                                            onFinish = { cancelled ->
                                                if (cancelled) {
                                                    if (pendingGaugeDrop != null) return@reorderGridItem
                                                    setReordering(false); activeDragKey = null; activeDragOffset = Offset.Zero; gaugeDragTarget = null
                                                    gaugePreviewOrder = emptyList(); gaugeDragBaseOrder = emptyList(); dragBasePositions = emptyMap()
                                                    return@reorderGridItem
                                                }
                                                val targetType = gaugeDragTarget ?: item.state.gaugeType
                                                activeDragOffset = dragSlotOffset(
                                                    dragKey, gaugeDragBaseOrder.map { "g:${it.name}" },
                                                    gaugePreviewOrder.map { "g:${it.name}" }, dragBasePositions,
                                                )
                                                pendingGaugeDrop = item.state.gaugeType to targetType
                                            },
                                        ),
                                    onMenu = { menuFor = dragKey },
                                )
                                DropdownMenu(
                                    expanded = menuFor == dragKey,
                                    onDismissRequest = { menuFor = null },
                                    shape = DropdownShape,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
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
                            } }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().zIndex(if (activeDragKey?.startsWith("i:") == true) 2f else 0f).testTag("indicators-grid")) {
                localIndicators.chunked(renderColumns).forEach { rowItems ->
                    Row(
                        Modifier.fillMaxWidth().zIndex(if (rowItems.any { activeDragKey == "i:${it.state.indicatorType.name}" }) 1f else 0f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        rowItems.forEach { item ->
                            val dragKey = "i:${item.state.indicatorType.name}"
                            val isDragged = activeDragKey == dragKey
                            val previewOffset = if (!isDragged && activeDragKey?.startsWith("i:") == true) {
                                dragSlotOffset(dragKey, indicatorDragBaseOrder.map { "i:${it.name}" }, indicatorPreviewOrder.map { "i:${it.name}" }, dragBasePositions)
                            } else Offset.Zero
                            val previewAnimation = remember(dragKey) {
                                previewAnimations.getOrPut(dragKey) { Animatable(Offset.Zero, Offset.VectorConverter) }
                            }
                            val animatedPreviewOffset = previewAnimation.value
                            LaunchedEffect(dragKey, previewOffset) {
                                previewAnimation.animateTo(previewOffset, tween(PREVIEW_ANIMATION_MILLIS))
                            }
                            key(dragKey) { Box(Modifier.width(gridCellWidth).zIndex(if (isDragged) 2f else 0f)) {
                                IndicatorCard(
                                    item,
                                    Modifier.fillMaxWidth().padding(8.dp)
                                        .onGloballyPositioned { coordinates ->
                                            val bounds = coordinates.boundsInRoot()
                                            if (indicatorPositions[dragKey] != bounds) indicatorPositions[dragKey] = bounds
                                        }.graphicsLayer {
                                            if (isDragged) {
                                                scaleX = 1.04f; scaleY = 1.04f; shadowElevation = 12.dp.toPx()
                                                translationX = activeDragOffset.x; translationY = activeDragOffset.y
                                            } else {
                                                translationX = animatedPreviewOffset.x; translationY = animatedPreviewOffset.y
                                            }
                                        }.zIndex(if (isDragged) 1f else 0f).reorderGridItem(
                                            indicatorPositions, dragKey,
                                            onStart = {
                                                setReordering(true); activeDragKey = dragKey; activeDragOffset = Offset.Zero
                                                indicatorDragTarget = item.state.indicatorType
                                                indicatorDragBaseOrder = localIndicators.map { it.state.indicatorType }
                                                indicatorPreviewOrder = indicatorDragBaseOrder
                                                dragBasePositions = indicatorPositions.toMap()
                                            },
                                            onDragDelta = { activeDragOffset += it },
                                            onTarget = { key ->
                                                val target = IndicatorType.entries.firstOrNull { it.name == key } ?: return@reorderGridItem
                                                indicatorDragTarget = target
                                                indicatorPreviewOrder = indicatorDragBaseOrder.moveItem(
                                                    source = { it == item.state.indicatorType }, target = { it == target },
                                                )
                                            },
                                            onFinish = { cancelled ->
                                                if (cancelled) {
                                                    if (pendingIndicatorDrop != null) return@reorderGridItem
                                                    setReordering(false); activeDragKey = null; activeDragOffset = Offset.Zero; indicatorDragTarget = null
                                                    indicatorPreviewOrder = emptyList(); indicatorDragBaseOrder = emptyList(); dragBasePositions = emptyMap()
                                                    return@reorderGridItem
                                                }
                                                val targetType = indicatorDragTarget ?: item.state.indicatorType
                                                activeDragOffset = dragSlotOffset(
                                                    dragKey, indicatorDragBaseOrder.map { "i:${it.name}" },
                                                    indicatorPreviewOrder.map { "i:${it.name}" }, dragBasePositions,
                                                )
                                                pendingIndicatorDrop = item.state.indicatorType to targetType
                                            },
                                        ),
                                    onMenu = { menuFor = dragKey },
                                    dataAvailable = dataAvailable,
                                )
                                DropdownMenu(
                                    expanded = menuFor == dragKey,
                                    onDismissRequest = { menuFor = null },
                                    shape = DropdownShape,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                        onClick = { menuFor = null; onDeleteIndicator(item.state.indicatorType) },
                                    )
                                }
                            } }
                        }
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
            if (fabExpanded) {
                SmallFloatingActionButton(onClick = { fabExpanded = false; onAddIndicatorRequest() }, modifier = Modifier.padding(bottom = 8.dp).testTag("sensors-add-indicator"), containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(painterResource(R.drawable.ic_indicator), stringResource(R.string.sensors_add_indicator)) }
                SmallFloatingActionButton(onClick = { fabExpanded = false; onAddGaugeRequest() }, modifier = Modifier.padding(bottom = 8.dp).testTag("sensors-add-gauge"), containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(painterResource(R.drawable.ic_gauge), stringResource(R.string.sensors_add_gauge)) }
            }
            FloatingActionButton(onClick = { fabExpanded = !fabExpanded }, modifier = Modifier.testTag("sensors-add"), containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Default.Add, stringResource(R.string.sensors_add)) }
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
private fun GaugeCard(item: GaugeItem, gaugeStyle: SensorGaugeStyle, modifier: Modifier, onMenu: () -> Unit) {
    val type = item.state.gaugeType
    val value = item.value.toFloatOrNull()
    val formattedValue = value?.let { if (type.isInteger) it.toInt().toString() else String.format(Locale.US, "%.1f", it) } ?: "—"
    val dialDescription = "${stringResource(type.title)}: $formattedValue ${stringResource(type.units)}"
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().testTag("gauge-${type.name}"),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onMenu,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(type.title),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            if (item.state.isNumericView) {
                Column(
                    Modifier.fillMaxWidth().testTag("gauge-body-${type.name}").padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                        Text(
                            formattedValue,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag("gauge-value-${type.name}"),
                            color = colors.onSurface,
                            textAlign = TextAlign.Center,
                            fontSize = 48.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 48.sp, stepSize = 1.sp),
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        )
                        Text(
                            stringResource(type.units),
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val bodyHeight = if (gaugeStyle == SensorGaugeStyle.Current) {
                        maxWidth
                    } else {
                        measureNewGaugeDial(maxWidth, androidx.compose.ui.platform.LocalDensity.current).height
                    }
                    Box(
                        Modifier.fillMaxWidth().height(bodyHeight).testTag("gauge-body-${type.name}"),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (gaugeStyle == SensorGaugeStyle.Current) {
                            GaugeDial(type, value, Modifier.fillMaxSize().testTag("dial-${type.name}").semantics { contentDescription = dialDescription })
                        } else {
                            NewGaugeDial(
                                type = type,
                                value = value,
                                formattedValue = formattedValue,
                                unit = stringResource(type.units),
                                style = gaugeStyle,
                                modifier = Modifier.fillMaxSize().testTag("dial-${type.name}").semantics { contentDescription = dialDescription },
                            )
                        }
                    }
                }
            }
            if (!item.state.isNumericView && (gaugeStyle == SensorGaugeStyle.Current || gaugeStyle == SensorGaugeStyle.ThinNeedle)) {
                Spacer(Modifier.height(4.dp))
                Column(
                    if (gaugeStyle == SensorGaugeStyle.ThinNeedle) Modifier.clearAndSetSemantics {} else Modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        formattedValue,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 24.sp, stepSize = 1.sp),
                    )
                    Text(
                        stringResource(type.units),
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun GaugeDial(type: GaugeType, value: Float?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val sections = remember(type, resources, context.theme) { type.sections.map { it.start to (it.end to Color(resources.getColor(it.color, context.theme))) } }
    val colors = MaterialTheme.colorScheme
    val normalized = value?.let { ((it - type.minValue) / (type.maxValue - type.minValue)).coerceIn(0f, 1f) } ?: 0f
    Canvas(modifier.aspectRatio(1f)) {
        val cx = size.width / 2f
        val dialSize = size.width
        val cy = dialSize / 2f
        val radius = dialSize / 2f - 5.dp.toPx()
        val stroke = 10.dp.toPx()
        val rect = androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius)
        drawArc(colors.surfaceVariant, 135f, 270f, false, rect.topLeft, rect.size, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
        sections.forEach { (start, segment) ->
            val (end, color) = segment
            drawArc(color, 135f + 270f * start, 270f * (end - start), false, rect.topLeft, rect.size, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
        }
        for (index in 1..8) {
            val markAngle = Math.toRadians((135f + 270f * index / 9f).toDouble())
            val markOuter = radius + stroke / 2f
            val markInner = markOuter - 9.dp.toPx()
            drawLine(colors.onSurfaceVariant, Offset(cx + kotlin.math.cos(markAngle).toFloat() * markOuter, cy + kotlin.math.sin(markAngle).toFloat() * markOuter), Offset(cx + kotlin.math.cos(markAngle).toFloat() * markInner, cy + kotlin.math.sin(markAngle).toFloat() * markInner), 3.dp.toPx())
        }
        for (index in 0 until type.tickCount) {
            val fraction = if (type.tickCount <= 1) 0f else index.toFloat() / (type.tickCount - 1)
            val tickAngle = Math.toRadians((135f + 270f * fraction).toDouble())
            val outer = radius - stroke * .05f
            val inner = outer - 3.dp.toPx()
            drawLine(colors.onSurfaceVariant.copy(alpha = .72f), Offset(cx + kotlin.math.cos(tickAngle).toFloat() * inner, cy + kotlin.math.sin(tickAngle).toFloat() * inner), Offset(cx + kotlin.math.cos(tickAngle).toFloat() * outer, cy + kotlin.math.sin(tickAngle).toFloat() * outer), 1.dp.toPx())
        }
        val angle = Math.toRadians((135f + 270f * normalized).toDouble())
        val direction = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
        val perpendicular = Offset(-direction.y, direction.x)
        val tip = Offset(cx, cy) + direction * (dialSize / 2f)
        val base = Offset(cx, cy) - direction * (dialSize / 6f)
        val halfBase = 5.dp.toPx()
        val needlePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base.x + perpendicular.x * halfBase, base.y + perpendicular.y * halfBase)
            quadraticTo(base.x, base.y, base.x - perpendicular.x * halfBase, base.y - perpendicular.y * halfBase)
            close()
        }
        if (value != null) {
            drawPath(needlePath, colors.primary)
            drawCircle(colors.onSurfaceVariant, 10.dp.toPx(), Offset(cx, cy))
        }
        drawIntoCanvas { canvas ->
            val tickCount = type.tickCount
            val readableTickSize = 12.sp.toPx()
            val tickPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color = colors.onSurfaceVariant.toArgb(); textAlign = NativePaint.Align.CENTER; textSize = readableTickSize }
            val tickLabels = (0 until tickCount).map { index ->
                val fraction = if (tickCount <= 1) 0f else index.toFloat() / (tickCount - 1)
                formatGaugeTickLabel(type.minValue + (type.maxValue - type.minValue) * fraction)
            }
            tickPaint.textSize = readableTickSize
            val largestReadableTickWidth = tickLabels.maxOfOrNull { tickPaint.measureText(it) } ?: 0f
            val tickLabelRadius = radius - stroke / 2f - 11.dp.toPx()
            val labelCapacity = (2..tickCount).lastOrNull { count ->
                val halfStepAngle = Math.toRadians((135f / (count - 1)).toDouble())
                tickLabelRadius * 2f * kotlin.math.sin(halfStepAngle).toFloat() >= largestReadableTickWidth + 18.dp.toPx()
            } ?: minOf(2, tickCount)
            val selectedTickIndices = selectGaugeTickLabelIndices(tickCount, labelCapacity)
            tickPaint.textSize = readableTickSize
            selectedTickIndices.forEach { index ->
                val fraction = if (tickCount <= 1) 0f else index.toFloat() / (tickCount - 1)
                val angle = Math.toRadians((135f + 270f * fraction).toDouble())
                canvas.nativeCanvas.drawText(tickLabels[index], cx + kotlin.math.cos(angle).toFloat() * tickLabelRadius, cy + kotlin.math.sin(angle).toFloat() * tickLabelRadius + tickPaint.textSize * .35f, tickPaint)
            }
        }
    }
}

internal fun formatGaugeTickLabel(value: Float): String = String.format(Locale.getDefault(), "%.0f", value)

internal fun selectGaugeTickLabelIndices(tickCount: Int, labelCount: Int): List<Int> {
    if (tickCount <= 0 || labelCount <= 0) return emptyList()
    if (tickCount <= labelCount) return (0 until tickCount).toList()
    if (labelCount == 1) return listOf(0)
    return (0 until labelCount).map { labelIndex ->
        (labelIndex * (tickCount - 1).toFloat() / (labelCount - 1)).roundToInt()
    }.distinct()
}

@Composable
private fun IndicatorCard(item: IndicatorItem, modifier: Modifier, onMenu: () -> Unit, dataAvailable: Boolean) {
    val accessibilityState = stringResource(when {
        !dataAvailable -> R.string.sensors_status_accessibility_unavailable
        item.isActive -> R.string.sensors_status_accessibility_on
        else -> R.string.sensors_status_accessibility_off
    })
    val palette = LocalSecuPalette.current
    val colors = MaterialTheme.colorScheme
    val active = dataAvailable && item.isActive
    val cardColor = when {
        !dataAvailable -> colors.surfaceVariant
        active -> palette.successContainer
        else -> colors.surface
    }
    val labelColor = when {
        !dataAvailable -> colors.onSurfaceVariant
        active -> palette.onSuccessContainer
        else -> colors.onSurface
    }
    Card(modifier = modifier.fillMaxWidth().testTag("indicator-${item.state.indicatorType.name}").semantics {
        stateDescription = accessibilityState
    }, colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), onClick = onMenu) {
        Column(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(item.state.indicatorType.title), fontSize = 16.sp, color = labelColor, style = MaterialTheme.typography.titleSmall)
        }
    }
}

private fun <T> List<T>.moveItem(source: (T) -> Boolean, target: (T) -> Boolean): List<T> {
    val from = indexOfFirst(source)
    val to = indexOfFirst(target)
    if (from < 0 || to < 0 || from == to) return this
    return toMutableList().also { list -> list.add(to, list.removeAt(from)) }
}

private fun dragSlotOffset(
    itemKey: String,
    baseOrder: List<String>,
    previewOrder: List<String>,
    basePositions: Map<String, Rect>,
): Offset {
    val previewIndex = previewOrder.indexOf(itemKey)
    if (previewIndex < 0) return Offset.Zero
    val destinationKey = baseOrder.getOrNull(previewIndex) ?: return Offset.Zero
    val current = basePositions[itemKey] ?: return Offset.Zero
    val destination = basePositions[destinationKey] ?: return Offset.Zero
    return destination.center - current.center
}

private fun Modifier.reorderGridItem(
    positions: Map<String, Rect>,
    itemKey: String,
    onStart: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onTarget: (String) -> Unit,
    onFinish: (cancelled: Boolean) -> Unit,
): Modifier = pointerInput(positions, itemKey) {
    var dragPosition = Offset.Zero
    var dragBounds: Map<String, Rect> = emptyMap()
    detectDragGesturesAfterLongPress(
        onDragStart = {
            dragBounds = positions.toMap()
            val bounds = dragBounds[itemKey]
            if (bounds != null) {
                dragPosition = bounds.center
                onStart()
            }
        },
        onDragEnd = { onFinish(false) },
        onDragCancel = { onFinish(true) },
        onDrag = { change, delta ->
            change.consume()
            dragPosition += delta
            onDragDelta(delta)
            val target = dragBounds.entries.minByOrNull { (_, bounds) ->
                val dx = bounds.center.x - dragPosition.x
                val dy = bounds.center.y - dragPosition.y
                dx * dx + dy * dy
            }
            if (target != null) onTarget(target.key.removePrefix("g:").removePrefix("i:"))
        },
    )
}

private const val PREVIEW_ANIMATION_MILLIS = 220
private const val PREVIEW_ANIMATION_NANOS = PREVIEW_ANIMATION_MILLIS * 1_000_000L
