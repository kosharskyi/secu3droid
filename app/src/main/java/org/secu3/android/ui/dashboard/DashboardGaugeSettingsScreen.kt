/*
 *SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *SECU-3  - An open source, free engine control unit
 *Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *contacts:
 *                http://secu-3.org
 *                email: vetalkosharskyi@gmail.com
 */

package org.secu3.android.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.models.DashboardGaugeStyle
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.compose.ChoiceDialog
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.ui.compose.SettingsActionRow
import org.secu3.android.ui.compose.SettingsSection
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog
import org.secu3.android.utils.prefs.UserPrefs

@Composable
internal fun DashboardGaugeSettingsScreen(
    state: DashboardGaugeSettingsUiState,
    presentationCatalog: SensorPresentationCatalog,
    onSave: (GaugeType, GaugeType, DashboardGaugeStyle) -> Unit,
    onNavigateUp: () -> Unit,
) {
    var draftLeftName by rememberSaveable { mutableStateOf(UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE.name) }
    var draftRightName by rememberSaveable { mutableStateOf(UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE.name) }
    var draftGaugeStyleName by rememberSaveable { mutableStateOf(DashboardGaugeStyle.Default.name) }
    var draftAwaitingSettings by rememberSaveable { mutableStateOf(true) }
    var editingLeftGauge by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var editingGaugeStyle by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selectorScrollState = rememberScrollState()
    val pageScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val leftGauge = remember(draftLeftName) {
        GaugeType.entries.firstOrNull { it.name == draftLeftName } ?: UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE
    }
    val rightGauge = remember(draftRightName) {
        GaugeType.entries.firstOrNull { it.name == draftRightName } ?: UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE
    }
    val gaugeStyle = remember(draftGaugeStyleName) {
        DashboardGaugeStyle.entries.firstOrNull { it.name == draftGaugeStyleName } ?: DashboardGaugeStyle.Default
    }
    val styleLabelResource: (DashboardGaugeStyle) -> Int = { style ->
        when (style) {
            DashboardGaugeStyle.Classic -> R.string.sensor_gauge_style_current
            DashboardGaugeStyle.PreciseArc -> R.string.sensor_gauge_style_precise_arc
            DashboardGaugeStyle.ThinNeedle -> R.string.sensor_gauge_style_thin_needle
            DashboardGaugeStyle.FilledArc -> R.string.dashboard_gauge_style_filled_arc
            DashboardGaugeStyle.TriangularNeedle -> R.string.dashboard_gauge_style_triangular_needle
        }
    }
    val isSaving = state.saveStatus == DashboardGaugeSaveStatus.Saving
    val canEdit = state.settingsLoaded && !draftAwaitingSettings && !isSaving
    BackHandler(enabled = isSaving) {}

    androidx.compose.runtime.LaunchedEffect(state.settingsLoaded, draftAwaitingSettings) {
        if (state.settingsLoaded && draftAwaitingSettings) {
            draftLeftName = state.leftGauge.name
            draftRightName = state.rightGauge.name
            draftGaugeStyleName = state.gaugeStyle.name
            draftAwaitingSettings = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(state.settingsReadFailed, state.saveStatus) {
        if (state.settingsReadFailed || state.saveStatus == DashboardGaugeSaveStatus.Failed) {
            pageScrollState.animateScrollTo(0)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SecuTopAppBar(
                title = stringResource(R.string.dashboard_gauges_title),
                onNavigateUp = { if (!isSaving) onNavigateUp() },
                showStatus = false,
                navigationIconModifier = Modifier.testTag("dashboard-gauge-cancel"),
                actions = {
                    TextButton(
                        onClick = { if (canEdit) onSave(leftGauge, rightGauge, gaugeStyle) },
                        enabled = canEdit,
                        modifier = Modifier.testTag("dashboard-gauge-save"),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.testTag("dashboard-gauge-saving"),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(pageScrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("dashboard-gauge-settings-screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.settingsReadFailed || state.saveStatus == DashboardGaugeSaveStatus.Failed) {
                Text(
                    text = stringResource(R.string.dashboard_gauges_storage_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("dashboard-gauge-storage-error"),
                )
            }

            SettingsSection(title = stringResource(R.string.settings_group_instruments)) {
                SettingsActionRow(
                    title = stringResource(R.string.dashboard_gauges_left),
                    summary = stringResource(presentationCatalog.gauge(leftGauge).title),
                    onClick = {
                        if (canEdit) {
                            searchQuery = ""
                            scope.launch { selectorScrollState.scrollTo(0) }
                            editingLeftGauge = true
                        }
                    },
                    testTag = "dashboard-gauge-side-left",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    title = stringResource(R.string.dashboard_gauges_right),
                    summary = stringResource(presentationCatalog.gauge(rightGauge).title),
                    onClick = {
                        if (canEdit) {
                            searchQuery = ""
                            scope.launch { selectorScrollState.scrollTo(0) }
                            editingLeftGauge = false
                        }
                    },
                    testTag = "dashboard-gauge-side-right",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    title = stringResource(R.string.dashboard_gauge_style),
                    summary = stringResource(styleLabelResource(gaugeStyle)),
                    onClick = { if (canEdit) editingGaugeStyle = true },
                    testTag = "dashboard-gauge-style",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsActionRow(
                    title = stringResource(R.string.dashboard_gauges_reset),
                    onClick = {
                        if (canEdit) {
                            draftLeftName = UserPrefs.DEFAULT_DASHBOARD_LEFT_GAUGE.name
                            draftRightName = UserPrefs.DEFAULT_DASHBOARD_RIGHT_GAUGE.name
                            draftGaugeStyleName = DashboardGaugeStyle.Default.name
                        }
                    },
                    testTag = "dashboard-gauge-reset",
                )
            }
        }
    }

    val editingSide = editingLeftGauge
    if (editingSide != null) {
        DashboardGaugeSelectorDialog(
            title = stringResource(if (editingSide) R.string.dashboard_gauges_left else R.string.dashboard_gauges_right),
            selectedGauge = if (editingSide) leftGauge else rightGauge,
            searchQuery = searchQuery,
            scrollState = selectorScrollState,
            canEdit = canEdit,
            presentationCatalog = presentationCatalog,
            onSearchQueryChange = { query -> searchQuery = query },
            onSelectGauge = { type ->
                if (editingSide) draftLeftName = type.name else draftRightName = type.name
                editingLeftGauge = null
            },
            onDismiss = { if (!isSaving) editingLeftGauge = null },
        )
    }
    if (editingGaugeStyle) {
        ChoiceDialog(
            title = { Text(stringResource(R.string.dashboard_gauge_style)) },
            selected = gaugeStyle,
            choices = DashboardGaugeStyle.entries,
            label = { style -> stringResource(styleLabelResource(style)) },
            onSelect = { style ->
                if (canEdit) {
                    draftGaugeStyleName = style.name
                    editingGaugeStyle = false
                }
            },
            onDismiss = { if (!isSaving) editingGaugeStyle = false },
            confirmButton = { TextButton(onClick = { if (!isSaving) editingGaugeStyle = false }, enabled = !isSaving) { Text(stringResource(R.string.close)) } },
            optionTestTag = { style -> "dashboard-gauge-style-${style.preferenceValue}" },
            optionMinHeight = 48.dp,
            optionHorizontalPadding = 0.dp,
        )
    }
}

@Composable
private fun DashboardGaugeSelectorDialog(
    title: String,
    selectedGauge: GaugeType,
    searchQuery: String,
    scrollState: androidx.compose.foundation.ScrollState,
    canEdit: Boolean,
    presentationCatalog: SensorPresentationCatalog,
    onSearchQueryChange: (String) -> Unit,
    onSelectGauge: (GaugeType) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        DashboardGaugeSelectorContent(
            title = title,
            selectedGauge = selectedGauge,
            searchQuery = searchQuery,
            scrollState = scrollState,
            canEdit = canEdit,
            presentationCatalog = presentationCatalog,
            onSearchQueryChange = onSearchQueryChange,
            onSelectGauge = onSelectGauge,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun DashboardGaugeSelectorContent(
    title: String,
    selectedGauge: GaugeType,
    searchQuery: String,
    scrollState: androidx.compose.foundation.ScrollState,
    canEdit: Boolean,
    presentationCatalog: SensorPresentationCatalog,
    onSearchQueryChange: (String) -> Unit,
    onSelectGauge: (GaugeType) -> Unit,
    onDismiss: () -> Unit,
) {
    val filteredTypes = GaugeType.entries.filter { type ->
        stringResource(presentationCatalog.gauge(type).title).contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .consumeWindowInsets(WindowInsets.statusBars),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                ),
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .verticalScroll(scrollState)
                        .selectableGroup()
                        .testTag("dashboard-gauge-selector"),
                ) {
                    Box(Modifier.fillMaxWidth().testTag("dashboard-gauge-selector-toolbar")) {
                        SecuTopAppBar(
                            title = title,
                            onNavigateUp = onDismiss,
                            showStatus = false,
                            navigationIconModifier = Modifier.testTag("dashboard-gauge-selector-back"),
                        )
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.dashboard_gauges_search)) },
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("dashboard-gauge-search"),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("dashboard-gauge-settings-list"),
                    ) {
                        filteredTypes.forEach { type ->
                            val presentation = presentationCatalog.gauge(type)
                            RowGaugeOption(
                                type = type,
                                label = stringResource(presentation.title),
                                selected = type == selectedGauge,
                                enabled = canEdit,
                                onClick = { onSelectGauge(type) },
                            )
                        }
                        if (filteredTypes.isEmpty()) {
                            Text(
                                text = stringResource(R.string.dashboard_gauges_no_results),
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowGaugeOption(
    type: GaugeType,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .testTag("dashboard-gauge-option-${type.name}")
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .defaultMinSize(minHeight = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "Dashboard Gauge Settings Ordinary Rows Landscape", widthDp = 780, heightDp = 360, showBackground = true)
@Composable
private fun DashboardGaugeSettingsOrdinaryRowsLandscapePreview() {
    SecuComposeTheme {
        DashboardGaugeSettingsScreen(
            state = DashboardGaugeSettingsUiState(settingsLoaded = true, leftGauge = GaugeType.MAF),
            presentationCatalog = SensorPresentationCatalog(),
            onSave = { _, _, _ -> },
            onNavigateUp = {},
        )
    }
}

@Preview(name = "Dashboard Gauge Settings Portrait", widthDp = 360, heightDp = 780, showBackground = true)
@Composable
private fun DashboardGaugeSettingsPortraitPreview() {
    DashboardGaugeSettingsOrdinaryRowsLandscapePreview()
}

@Preview(name = "Dashboard Gauge Settings Tablet", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
private fun DashboardGaugeSettingsTabletPreview() {
    DashboardGaugeSettingsOrdinaryRowsLandscapePreview()
}

@Preview(name = "Dashboard Gauge Settings Compact 1.5x", widthDp = 568, heightDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun DashboardGaugeSettingsCompactPreview() {
    DashboardGaugeSettingsOrdinaryRowsLandscapePreview()
}

@Preview(name = "Dashboard Gauge Selector Compact 1.5x", widthDp = 568, heightDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun DashboardGaugeSelectorSearchBelowToolbarSafeAreaPreview() {
    SecuComposeTheme {
        DashboardGaugeSelectorContent(
            title = "Left gauge",
            selectedGauge = GaugeType.RPM,
            searchQuery = "",
            scrollState = rememberScrollState(),
            canEdit = true,
            presentationCatalog = SensorPresentationCatalog(),
            onSearchQueryChange = {},
            onSelectGauge = {},
            onDismiss = {},
        )
    }
}
