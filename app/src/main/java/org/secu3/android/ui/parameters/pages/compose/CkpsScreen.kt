/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2026 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.parameters.pages.compose

import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.domain.parameters.CkpsControl
import org.secu3.android.domain.parameters.CkpsValues

import org.secu3.android.ui.compose.DropdownShape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog
import org.secu3.android.ui.parameters.dialogs.ParameterCancelButton
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceList
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceOption

enum class CkpsFlag {
    CKPS_EDGE,
    REFERENCE_EDGE,
    SPARK_RISING,
    MERGE_OUTPUTS,
    USE_CAM_REFERENCE,
}

enum class CkpsNumber(
    val title: Int,
    val units: Int,
    val precision: Int,
    val min: Float,
    val max: Float,
    val step: Float,
    val integer: Boolean = false,
) {
    WHEEL(R.string.ckps_cogs_number_title, R.string.units_empty, 0, CkpsControl.WHEEL_MIN, CkpsControl.WHEEL_MAX, 1f, true),
    MISSING(R.string.ckps_missing_cogs_number_title, R.string.units_empty, 0, CkpsControl.MISSING_MIN, CkpsControl.MISSING_MAX, 1f, true),
    PULSE(R.string.ckps_ignition_pulse_delay_title, R.string.units_empty, 0, CkpsControl.PULSE_MIN, CkpsControl.PULSE_MAX, 1f, true),
    HALL_WIDTH(R.string.ckps_hall_wnd_width_title, R.string.units_degrees_cs, 1, CkpsControl.HALL_WIDTH_MIN, CkpsControl.HALL_WIDTH_MAX, .1f),
    DEGREES(R.string.ckps_degrees_before_tdc_title, R.string.units_degrees_cs, 1, CkpsControl.DEGREES_MIN, CkpsControl.DEGREES_MAX, .1f),
    MTTF(R.string.ckps_missing_teeths_detection_factor_title, R.string.units_empty, 2, CkpsControl.MTTF_MIN, CkpsControl.MTTF_MAX, .01f);

    fun value(values: CkpsValues): Float = when (this) {
        WHEEL -> values.wheel.toFloat()
        MISSING -> values.missingTeeth.toFloat()
        PULSE -> values.ignitionPulseCount.toFloat()
        HALL_WIDTH -> values.hallWindowWidth
        DEGREES -> values.hallDegreesBeforeTdc
        MTTF -> values.mttf
    }
}

@Composable
fun CkpsScreen(
    values: CkpsValues?,
    onEdit: (CkpsNumber) -> Unit,
    onFlag: (CkpsFlag, Boolean) -> Unit,
    onChoice: (Boolean, Int) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("ckps_loading")
        return
    }

    val tdcOptions = values.teethBeforeTdcOptions
    val availableCylinders = capabilities.ckpsCylinderOptions(values.mergeOutputs)
    var open by remember { mutableStateOf<Boolean?>(null) }

    ParameterPage(scrollState = pageScrollState) {
            ParameterSection {
        EdgeGroup(stringResource(R.string.ckps_ckps_edge_title), values.ckpsEdge, "ckps_edge") {
            onFlag(CkpsFlag.CKPS_EDGE, it)
        }
        EdgeGroup(stringResource(R.string.ckps_ref_s_edge_title), values.referenceEdge, "ckps_ref") {
            onFlag(CkpsFlag.REFERENCE_EDGE, it)
        }
        ParameterSwitchRow(
            stringResource(R.string.ckps_spark_on_rising_edge_for_cdi_title),
            values.risingSpark,
            "ckps_spark", enabled = capabilities.risingSpark,
        ) { onFlag(CkpsFlag.SPARK_RISING, it) }
        ParameterSwitchRow(
            stringResource(R.string.ckps_merge_outputs_title),
            values.mergeOutputs,
            "ckps_merge", enabled = capabilities.inputsMerging,
        ) { onFlag(CkpsFlag.MERGE_OUTPUTS, it) }

        CkpsNumber.entries.filter { it != CkpsNumber.MTTF }.forEach { n ->
            if (n == CkpsNumber.PULSE) {
                Choice(
                    title = stringResource(R.string.ckps_cogs_before_tdc_title),
                    value = values.teethBeforeTdc.toString().takeIf { values.teethBeforeTdc in tdcOptions } ?: "Code ${values.teethBeforeTdc}",
                    tag = "ckps_teeth_btdc", enabled = capabilities.cogsBtdc,
                ) { open = true }
                Choice(
                    title = stringResource(R.string.ckps_engine_cylinders_title),
                    value = values.cylinders.toString().takeIf { values.cylinders in 1..8 } ?: "Code ${values.cylinders}",
                    tag = "ckps_cylinders",
                ) { open = false }
            }

            ParameterValueRow(
                title = stringResource(n.title),
                units = stringResource(n.units),
                value = n.value(values),
                precision = n.precision,
                testTag = "ckps_${n.name.lowercase()}",
                enabled = when (n) {
                    CkpsNumber.WHEEL, CkpsNumber.MISSING -> capabilities.ckps
                    CkpsNumber.PULSE -> capabilities.ignitionCogs
                    CkpsNumber.HALL_WIDTH, CkpsNumber.DEGREES -> capabilities.hallWindow
                    CkpsNumber.MTTF -> capabilities.mttfEnabled(values.missingTeeth.toFloat())
                },
            ) { onEdit(n) }
        }

        ParameterSwitchRow(
            stringResource(R.string.ckps_use_cam_sensor_title),
            values.camReference,
            "ckps_cam", enabled = capabilities.camReferenceEnabled(values.missingTeeth.toFloat()),
        ) { onFlag(CkpsFlag.USE_CAM_REFERENCE, it) }
        ParameterValueRow(
            title = stringResource(CkpsNumber.MTTF.title),
            units = stringResource(CkpsNumber.MTTF.units),
            value = values.mttf,
            precision = CkpsNumber.MTTF.precision,
            testTag = "ckps_mttf", enabled = capabilities.mttfEnabled(values.missingTeeth.toFloat()),
        ) { onEdit(CkpsNumber.MTTF) }
    }

    open?.let { teeth ->
        val options = if (teeth) tdcOptions else availableCylinders
        ParameterAlertDialog(
            onDismissRequest = { open = null },
            title = {
                Text(
                    if (teeth) stringResource(R.string.ckps_cogs_before_tdc_title)
                    else stringResource(R.string.ckps_engine_cylinders_title),
                )
            },
            text = {
                ParameterChoiceList {
                    options.forEach { option ->
                        ParameterChoiceOption(option.toString(), "ckps_option_$option") {
                            onChoice(teeth, option)
                            open = null
                        }
                    }
                }
            },
            confirmButton = {
                ParameterCancelButton { open = null }
            },
        )
    }
            }
        }

@Composable
private fun EdgeGroup(
    title: String,
    positive: Boolean,
    tag: String,
    onSelect: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
            listOf(
                false to R.string.ckps_edge_negative,
                true to R.string.ckps_edge_positive,
            ).forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .clickable { onSelect(value) }
                        .testTag("${tag}_${if (value) "positive" else "negative"}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = value == positive, onClick = { onSelect(value) })
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                }
            }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Choice(title: String, value: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    ParameterSelectionRow(title, value, tag, enabled = enabled, onClick = onClick)
}
