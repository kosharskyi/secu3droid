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
package org.secu3.android.ui.parameters.pages

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.domain.parameters.UniversalOutputConditions
import org.secu3.android.domain.parameters.UniversalOutputValues
import org.secu3.android.domain.parameters.UniversalOutputsValues
import org.secu3.android.ui.compose.DropdownShape
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog
import org.secu3.android.ui.parameters.dialogs.ParameterCancelButton
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog
import org.secu3.android.ui.parameters.dialogs.ParameterConfirmButton
import org.secu3.android.domain.parameters.UniOutConditionFormat
import java.util.Locale

@Composable
internal fun UniversalOutputsScreen(
    values: UniversalOutputsValues?, conditionNames: Map<Int, String>, logicalFunctions: List<String>,
    conditions: UniversalOutputConditions,
    onEnabled: (Int, Boolean) -> Unit,
    onCondition: (Int, Int, Int) -> Unit,
    onConditionInversion: (Int, Int, Boolean) -> Unit,
    onResultInversion: (Int, Boolean) -> Unit,
    onLogicFunction: (Int, Int) -> Unit,
    onThreshold: (Int, Int, Boolean, Int, Float) -> Unit,
    onPairFunction: (Int) -> Unit,
) {
    val pageScrollState = rememberScrollState()
    if (values == null) { ParameterLoadingIndicator("uniout-loading"); return }
    ParameterPage(scrollState = pageScrollState) {
        values.outputs.forEachIndexed { index, output ->
            OutputCard(index + 1, output, conditionNames, logicalFunctions, conditions, onEnabled, onCondition,
                onConditionInversion, onResultInversion, onLogicFunction, onThreshold)
            if (index == 0) {
                ParameterSection {
                    FunctionPicker(R.string.uniout_outputs_link_function, values.logicFunction12, logicalFunctions,
                        enabled = values.pairFunctionAvailable, tag = "outputs-link-function", onSelect = { onPairFunction(if (it == 4) 15 else it) })
                }
            }
        }
    }
}

@Composable private fun OutputCard(
    index: Int, values: UniversalOutputValues, names: Map<Int, String>, functions: List<String>, conditions: UniversalOutputConditions,
    onEnabled: (Int, Boolean) -> Unit, onCondition: (Int, Int, Int) -> Unit,
    onConditionInversion: (Int, Int, Boolean) -> Unit, onResultInversion: (Int, Boolean) -> Unit,
    onLogicFunction: (Int, Int) -> Unit, onThreshold: (Int, Int, Boolean, Int, Float) -> Unit,
) {
    ParameterSection(modifier = Modifier.testTag("output-$index-card"), contentSpacing = 0.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(UniversalOutputLabels.entries[index - 1].title), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(values.enabled, { onEnabled(index, it) }, modifier = Modifier.testTag("output-$index-use"))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (values.enabled) {
            ConditionGroup("output-$index-condition-1-group") {
                ConditionPicker(index, 1, UniversalOutputLabels.entries[index - 1].condition1, values.condition1, names, conditions) { onCondition(index, 1, it) }
                ToggleLine(R.string.unioutput1_condition_1_inverse_title, values.condition1Inversion) { onConditionInversion(index, 1, it) }
                Threshold(R.string.unioutput1_condition1_on_value_title, values.onThreshold1, conditions.get(values.condition1), values.condition1, "output-$index-threshold-on-1") { condition, value -> onThreshold(index, 1, true, condition, value) }
                Threshold(R.string.unioutput1_condition1_off_value_title, values.offThreshold1, conditions.get(values.condition1), values.condition1, "output-$index-threshold-off-1") { condition, value -> onThreshold(index, 1, false, condition, value) }
            }
            Spacer(Modifier.height(16.dp))
            FunctionPicker(R.string.unioutput1_logical_functions_title, values.logicFunction, functions, tag = "output-$index-logical-function") { onLogicFunction(index, if (it == 4) 15 else it) }
            if (values.logicFunction != 15) {
                Spacer(Modifier.height(8.dp))
                ConditionGroup("output-$index-condition-2-group") {
                    ConditionPicker(index, 2, UniversalOutputLabels.entries[index - 1].condition2, values.condition2, names, conditions) { onCondition(index, 2, it) }
                    ToggleLine(R.string.unioutput1_condition_1_inverse_title, values.condition2Inversion) { onConditionInversion(index, 2, it) }
                    Threshold(R.string.unioutput1_condition1_on_value_title, values.onThreshold2, conditions.get(values.condition2), values.condition2, "output-$index-threshold-on-2") { condition, value -> onThreshold(index, 2, true, condition, value) }
                    Threshold(R.string.unioutput1_condition1_off_value_title, values.offThreshold2, conditions.get(values.condition2), values.condition2, "output-$index-threshold-off-2") { condition, value -> onThreshold(index, 2, false, condition, value) }
                }
            }
            Spacer(Modifier.height(16.dp))
            ToggleLine(R.string.unioutput_inverse_result_title, values.inversion, tag = "output-$index-result-inversion") { onResultInversion(index, it) }
        } else Text(stringResource(R.string.uniout_disabled), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp).testTag("output-$index-collapsed"))
    }
}

@Composable private fun ConditionGroup(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, DropdownShape).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

private enum class UniversalOutputLabels(val title: Int, val condition1: Int, val condition2: Int) {
    FIRST(R.string.unioutput1_use_title, R.string.unioutput1_condition_1_title, R.string.unioutput1_condition_2_title),
    SECOND(R.string.unioutput2_use_title, R.string.unioutput2_condition_1_title, R.string.unioutput2_condition_2_title),
    THIRD(R.string.unioutput3_use_title, R.string.unioutput3_condition_1_title, R.string.unioutput3_condition_2_title),
    FOURTH(R.string.unioutput4_use_title, R.string.unioutput4_condition_1_title, R.string.unioutput4_condition_2_title),
    FIFTH(R.string.unioutput5_use_title, R.string.unioutput5_condition_1_title, R.string.unioutput5_condition_2_title),
    SIXTH(R.string.unioutput6_use_title, R.string.unioutput6_condition_1_title, R.string.unioutput6_condition_2_title),
}

@Composable private fun ToggleLine(labelId: Int, value: Boolean, tag: String? = null, onValue: (Boolean) -> Unit) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp).clickable { onValue(!value) }.padding(horizontal = 14.dp, vertical = 11.dp)
    Row(if (tag == null) modifier else modifier.testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelId), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Checkbox(value, onValue)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable private fun ConditionPicker(output: Int, slot: Int, labelId: Int, selected: Int, names: Map<Int, String>, conditions: UniversalOutputConditions, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(labelId)
    val conditionName = names[selected] ?: stringResource(R.string.uniout_unknown_code, selected)
    Box {
        ParameterSelectionRow(label, conditionName, "output-$output-condition-$slot-picker") { expanded = true }
        if (expanded) ParameterChoiceDialog(
            title = label,
            choices = names.toSortedMap().filter { (id, _) -> conditions.availableInSlot(id, slot) }
                .map { (id, name) -> ParameterChoice(id, name, "output-$output-condition-$slot-option-$id") },
            onDismiss = { expanded = false },
            onSelect = onSelect,
        )
    }
}

@Composable private fun FunctionPicker(labelId: Int, selected: Int, options: List<String>, enabled: Boolean = true, tag: String, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val knownIndex = when (selected) { 0, 1, 2, 3 -> selected; 15 -> 4; else -> null }
    val selectedText = knownIndex?.let { options.getOrNull(it) } ?: stringResource(R.string.uniout_unknown_code, selected)
    Box {
        ParameterSelectionRow(stringResource(labelId), selectedText, tag, enabled) { expanded = true }
        if (expanded) ParameterChoiceDialog(
            title = stringResource(labelId),
            choices = options.mapIndexed { index, value -> ParameterChoice(index, value, "${tag}_option_$index") },
            onDismiss = { expanded = false },
            onSelect = onSelect,
        )
    }
}

@Composable private fun Threshold(labelId: Int, value: Float, format: UniOutConditionFormat?, condition: Int, tag: String, onValue: (Int, Float) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var editingCondition by remember { mutableIntStateOf(condition) }
    val label = stringResource(labelId)
    if (format == null) {
        Text(stringResource(R.string.uniout_threshold_unavailable, label), Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp))
    } else {
        ParameterTextValueRow(
            title = label,
            value = String.format(Locale.US, "% .${format.precision}f", value).trim(),
            testTag = tag,
            numberText = String.format(Locale.US, "% .${format.precision}f", value).trim(),
            onClick = { editingCondition = condition; editing = true },
        )
        if (editing) {
            var text by remember(value) { mutableStateOf(value.toString()) }
            ParameterAlertDialog(onDismissRequest = { editing = false }, title = { Text(stringResource(R.string.uniout_threshold_edit_title, label)) },
                text = { OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.uniout_threshold_value_label)) }, shape = DropdownShape, modifier = Modifier.testTag("$tag-editor")) },
                confirmButton = { ParameterConfirmButton(stringResource(android.R.string.ok)) { text.toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(format.min, format.max)?.let { onValue(editingCondition, it) }; editing = false } },
                dismissButton = { ParameterCancelButton { editing = false } })
        }
    }
}
