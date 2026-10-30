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

import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.ui.compose.DropdownShape
import org.secu3.android.ui.compose.rememberAnimatedGaugeProgress

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.height
import org.secu3.android.R
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeScale
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.models.sensors.IndicatorType
import org.secu3.android.ui.sensors.SensorsViewModel.ChoiceRequest
import org.secu3.android.ui.compose.LocalSecuPalette

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
    presentationCatalog: SensorPresentationCatalog,
    renderer: GaugeDialRenderer,
    injectionTimingMode: InjectionTimingMode = InjectionTimingMode.Default,
) {
    val latestReorderingChanged by rememberUpdatedState(onReorderingChanged)
    val gaugeReorder = remember { SensorGridReorderState(gauges, { item -> "g:${item.state.gaugeType.name}" }) { latestReorderingChanged(it) } }
    val indicatorReorder = remember { SensorGridReorderState(indicators, { item -> "i:${item.state.indicatorType.name}" }) { latestReorderingChanged(it) } }
    val dragging = gaugeReorder.isDragging || indicatorReorder.isDragging
    val latestGauges by rememberUpdatedState(gauges)
    val latestIndicators by rememberUpdatedState(indicators)
    val latestDragging by rememberUpdatedState(dragging)
    var menuFor by remember { mutableStateOf<String?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    var gaugeDialog by remember { mutableStateOf<List<GaugeType>?>(null) }
    var indicatorDialog by remember { mutableStateOf<List<IndicatorType>?>(null) }
    DisposableEffect(Unit) {
        onDispose { if (latestDragging) latestReorderingChanged(false) }
    }
    LaunchedEffect(gauges, gaugeReorder.pendingOrder, dragging) {
        if (!dragging) gaugeReorder.reconcile(gauges)
    }
    LaunchedEffect(indicators, indicatorReorder.pendingOrder, dragging) {
        if (!dragging) indicatorReorder.reconcile(indicators)
    }
    LaunchedEffect(gaugeReorder.pendingDrop) {
        gaugeReorder.completeDrop({ latestGauges }) { items ->
            onReorderGauges(items.mapIndexed { index, value -> value.state.copy(idx = index) })
        }
    }
    LaunchedEffect(indicatorReorder.pendingDrop) {
        indicatorReorder.completeDrop({ latestIndicators }) { items ->
            onReorderIndicators(items.mapIndexed { index, value -> value.state.copy(idx = index) })
        }
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
        // Gauge cards need this width outside BoxWithConstraints so row intrinsic sizing can measure them.
        val gaugeContentWidth = (gridCellWidth - CARD_CELL_GUTTER * 2f - CARD_CONTENT_INSET * 2f).coerceAtLeast(0.dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = !dragging).padding(bottom = 88.dp)) {
            Column(Modifier.fillMaxWidth().zIndex(if (gaugeReorder.activeKey != null) 2f else 0f).testTag("gauges-grid")) {
                gaugeReorder.items.chunked(renderColumns).forEach { rowItems ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                            .zIndex(if (rowItems.any { gaugeReorder.activeKey == "g:${it.state.gaugeType.name}" }) 1f else 0f),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        rowItems.forEach { item ->
                            val dragKey = "g:${item.state.gaugeType.name}"
                            val isDragged = gaugeReorder.activeKey == dragKey
                            val previewOffset = if (!isDragged && gaugeReorder.activeKey != null) gaugeReorder.previewOffset(dragKey) else Offset.Zero
                            val previewAnimation = remember(dragKey) { gaugeReorder.animation(dragKey) }
                            val animatedPreviewOffset = previewAnimation.value
                            LaunchedEffect(dragKey, previewOffset) {
                                gaugeReorder.animatePreview(dragKey, previewOffset)
                            }
                            key(dragKey) { Box(Modifier.width(gridCellWidth).fillMaxHeight().zIndex(if (isDragged) 2f else 0f), contentAlignment = Alignment.TopCenter) {
                                GaugeCard(
                                    if (dataAvailable) item else item.copy(value = "—"),
                                    gaugeStyle,
                                    injectionTimingMode,
                                    presentationCatalog,
                                    renderer,
                                    gaugeContentWidth,
                                    Modifier.fillMaxWidth().fillMaxHeight().padding(CARD_CELL_GUTTER)
                                        .onGloballyPositioned { coordinates ->
                                            val bounds = coordinates.boundsInRoot()
                                            if (gaugeReorder.positions[dragKey] != bounds) gaugeReorder.positions[dragKey] = bounds
                                        }.graphicsLayer {
                                            if (isDragged) {
                                                scaleX = 1.04f; scaleY = 1.04f; shadowElevation = 12.dp.toPx()
                                                translationX = gaugeReorder.activeOffset.x; translationY = gaugeReorder.activeOffset.y
                                            } else {
                                                translationX = animatedPreviewOffset.x; translationY = animatedPreviewOffset.y
                                            }
                                        }.zIndex(if (isDragged) 1f else 0f).then(gaugeReorder.itemModifier(dragKey)),
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
                                            gaugeReorder.items = gaugeReorder.items.map { current -> if (current.state.gaugeType == updated.gaugeType) current.copy(state = updated) else current }
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
            Column(Modifier.fillMaxWidth().zIndex(if (indicatorReorder.activeKey != null) 2f else 0f).testTag("indicators-grid")) {
                indicatorReorder.items.chunked(renderColumns).forEach { rowItems ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                            .zIndex(if (rowItems.any { indicatorReorder.activeKey == "i:${it.state.indicatorType.name}" }) 1f else 0f),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        rowItems.forEach { item ->
                            val dragKey = "i:${item.state.indicatorType.name}"
                            val isDragged = indicatorReorder.activeKey == dragKey
                            val previewOffset = if (!isDragged && indicatorReorder.activeKey != null) indicatorReorder.previewOffset(dragKey) else Offset.Zero
                            val previewAnimation = remember(dragKey) { indicatorReorder.animation(dragKey) }
                            val animatedPreviewOffset = previewAnimation.value
                            LaunchedEffect(dragKey, previewOffset) {
                                indicatorReorder.animatePreview(dragKey, previewOffset)
                            }
                            key(dragKey) { Box(Modifier.width(gridCellWidth).fillMaxHeight().zIndex(if (isDragged) 2f else 0f)) {
                                IndicatorCard(
                                    item,
                                    presentationCatalog,
                                    Modifier.fillMaxWidth().fillMaxHeight().padding(CARD_CELL_GUTTER)
                                        .onGloballyPositioned { coordinates ->
                                            val bounds = coordinates.boundsInRoot()
                                            if (indicatorReorder.positions[dragKey] != bounds) indicatorReorder.positions[dragKey] = bounds
                                        }.graphicsLayer {
                                            if (isDragged) {
                                                scaleX = 1.04f; scaleY = 1.04f; shadowElevation = 12.dp.toPx()
                                                translationX = indicatorReorder.activeOffset.x; translationY = indicatorReorder.activeOffset.y
                                            } else {
                                                translationX = animatedPreviewOffset.x; translationY = animatedPreviewOffset.y
                                            }
                                        }.zIndex(if (isDragged) 1f else 0f).then(indicatorReorder.itemModifier(dragKey)),
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
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { choices.forEach { type -> TextButton(onClick = { gaugeDialog = null; onAddGauge(type) }) { Text(stringResource(presentationCatalog.gauge(type).title)) } } } },
            confirmButton = { TextButton(onClick = { gaugeDialog = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
    indicatorDialog?.let { choices ->
        AlertDialog(onDismissRequest = { indicatorDialog = null }, title = { Text(stringResource(R.string.select_indicator_to_add)) },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { choices.forEach { type -> TextButton(onClick = { indicatorDialog = null; onAddIndicator(type) }) { Text(stringResource(presentationCatalog.indicatorTitle(type))) } } } },
            confirmButton = { TextButton(onClick = { indicatorDialog = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
}

@Composable
private fun GaugeCard(item: GaugeItem, gaugeStyle: SensorGaugeStyle, injectionTimingMode: InjectionTimingMode, presentationCatalog: SensorPresentationCatalog, renderer: GaugeDialRenderer, cardContentWidth: Dp, modifier: Modifier, onMenu: () -> Unit) {
    val type = item.state.gaugeType
    val value = item.value.toFloatOrNull()?.takeIf(Float::isFinite)
    val presentation = presentationCatalog.gauge(type)
    val scale = remember(type, presentation, injectionTimingMode) {
        GaugeScale(type, presentation, injectionTimingMode)
    }
    val progress = rememberGaugeProgress(scale, value, enabled = !item.state.isNumericView)
    val formattedValue = item.value.takeIf { value != null } ?: "—"
    val dialDescription = "${stringResource(presentation.title)}: $formattedValue ${stringResource(presentation.units)}"
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().testTag("gauge-${type.name}"),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onMenu,
    ) {
        Column(Modifier.fillMaxWidth().padding(CARD_CONTENT_INSET), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(presentation.title),
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
                            stringResource(presentation.units),
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                }
            } else {
                val dialLabelOverflow = if (gaugeStyle == SensorGaugeStyle.Current) 0.dp else 14.dp
                val density = androidx.compose.ui.platform.LocalDensity.current
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val dialMeasure = if (gaugeStyle == SensorGaugeStyle.Current) null else remember(type, cardContentWidth, density, dialLabelOverflow, configuration, injectionTimingMode) {
                    renderer.measureNewGaugeDial(scale, cardContentWidth, density, dialLabelOverflow)
                }
                val bodyHeight = dialMeasure?.height ?: cardContentWidth
                Box(
                    Modifier.fillMaxWidth().height(bodyHeight).testTag("gauge-body-${type.name}"),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (gaugeStyle == SensorGaugeStyle.Current) {
                        GaugeDial(scale, renderer, progress, Modifier.fillMaxSize().testTag("dial-${type.name}").semantics { contentDescription = dialDescription })
                    } else {
                        NewGaugeDial(
                            scale = scale,
                            renderer = renderer,
                            progress = progress,
                            formattedValue = formattedValue,
                            unit = stringResource(presentation.units),
                            style = gaugeStyle,
                            dialMeasure = requireNotNull(dialMeasure),
                            modifier = Modifier.requiredWidth(cardContentWidth + dialLabelOverflow * 2f).height(bodyHeight)
                                .testTag("dial-${type.name}").semantics { contentDescription = dialDescription },
                        )
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
                        stringResource(presentation.units),
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
private fun rememberGaugeProgress(scale: GaugeScale, value: Float?, enabled: Boolean): () -> Float? {
    val target = if (enabled) value?.takeIf(Float::isFinite)?.let(scale::normalized) else null
    return key(scale.type, target != null) {
        if (target == null) {
            remember {
                val progressReader: () -> Float? = { null }
                progressReader
            }
        } else {
            val animatedProgress = rememberAnimatedGaugeProgress(target)
            val progressReader: () -> Float? = { animatedProgress() }
            progressReader
        }
    }
}

@Composable
private fun GaugeDial(scale: GaugeScale, renderer: GaugeDialRenderer, progress: () -> Float?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val sections = remember(scale, resources, context.theme) { scale.sections.map { it.start to (it.end to Color(resources.getColor(it.color, context.theme))) } }
    val tickLabels = remember(scale, androidx.compose.ui.platform.LocalConfiguration.current) { scale.ticks.map(scale::format) }
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.aspectRatio(1f)) {
        with(renderer) { drawClassicGauge(scale, colors, sections, tickLabels, progress()) }
    }
}

@Composable
private fun IndicatorCard(item: IndicatorItem, presentationCatalog: SensorPresentationCatalog, modifier: Modifier, onMenu: () -> Unit, dataAvailable: Boolean) {
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
        Column(Modifier.fillMaxWidth().fillMaxHeight().heightIn(min = 72.dp).padding(CARD_CONTENT_INSET), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(presentationCatalog.indicatorTitle(item.state.indicatorType)), fontSize = 16.sp, color = labelColor, style = MaterialTheme.typography.titleSmall)
        }
    }
}

private val CARD_CELL_GUTTER = 8.dp
private val CARD_CONTENT_INSET = 16.dp
