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
import org.secu3.android.models.packets.out.params.UniOutParamPacket
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
import org.secu3.android.utils.UniOutConditionFormat
import org.secu3.android.utils.uniOutConditionFormat
import java.util.Locale

internal data class UniversalOutputValues(
    val enabled: Boolean,
    val condition1: Int,
    val condition2: Int,
    val condition1Inversion: Boolean,
    val condition2Inversion: Boolean,
    val inversion: Boolean,
    val logicFunction: Int,
    val onThreshold1: Float,
    val offThreshold1: Float,
    val onThreshold2: Float,
    val offThreshold2: Float,
) {
    fun withCondition(slot: Int, condition: Int): UniversalOutputValues {
        val format = uniOutConditionFormat(condition)
        return if (slot == 1) copy(condition1 = condition, onThreshold1 = format.onDefault, offThreshold1 = format.offDefault)
        else copy(condition2 = condition, onThreshold2 = format.onDefault, offThreshold2 = format.offDefault)
    }
}

internal data class UniversalOutputsValues(val outputs: List<UniversalOutputValues>, val logicFunction12: Int) {
    companion object {
        fun from(packet: UniOutParamPacket) = UniversalOutputsValues((1..6).map { outputValues(packet, it) }, packet.logicFunction_1_2)
    }
}

private fun outputValues(packet: UniOutParamPacket, index: Int): UniversalOutputValues = when (index) {
    1 -> UniversalOutputValues(packet.output1Use, packet.output1Condition1, packet.output1Condition2, packet.output1Cond1Inversion, packet.output1Cond2Inversion, packet.output1Inversion, packet.output1LogicFunc, packet.output1OnThrd1, packet.output1OffThrd1, packet.output1OnThrd2, packet.output1OffThrd2)
    2 -> UniversalOutputValues(packet.output2Use, packet.output2Condition1, packet.output2Condition2, packet.output2Cond1Inversion, packet.output2Cond2Inversion, packet.output2Inversion, packet.output2LogicFunc, packet.output2OnThrd1, packet.output2OffThrd1, packet.output2OnThrd2, packet.output2OffThrd2)
    3 -> UniversalOutputValues(packet.output3Use, packet.output3Condition1, packet.output3Condition2, packet.output3Cond1Inversion, packet.output3Cond2Inversion, packet.output3Inversion, packet.output3LogicFunc, packet.output3OnThrd1, packet.output3OffThrd1, packet.output3OnThrd2, packet.output3OffThrd2)
    4 -> UniversalOutputValues(packet.output4Use, packet.output4Condition1, packet.output4Condition2, packet.output4Cond1Inversion, packet.output4Cond2Inversion, packet.output4Inversion, packet.output4LogicFunc, packet.output4OnThrd1, packet.output4OffThrd1, packet.output4OnThrd2, packet.output4OffThrd2)
    5 -> UniversalOutputValues(packet.output5Use, packet.output5Condition1, packet.output5Condition2, packet.output5Cond1Inversion, packet.output5Cond2Inversion, packet.output5Inversion, packet.output5LogicFunc, packet.output5OnThrd1, packet.output5OffThrd1, packet.output5OnThrd2, packet.output5OffThrd2)
    6 -> UniversalOutputValues(packet.output6Use, packet.output6Condition1, packet.output6Condition2, packet.output6Cond1Inversion, packet.output6Cond2Inversion, packet.output6Inversion, packet.output6LogicFunc, packet.output6OnThrd1, packet.output6OffThrd1, packet.output6OnThrd2, packet.output6OffThrd2)
    else -> error("Output index must be between 1 and 6")
}

private fun applyOutputValues(packet: UniOutParamPacket, index: Int, before: UniversalOutputValues, after: UniversalOutputValues) {
    when (index) {
        1 -> {
            if (before.enabled != after.enabled) packet.output1Use = after.enabled
            if (before.condition1 != after.condition1) {
                packet.output1Condition1 = after.condition1
                packet.output1OnThrd1 = after.onThreshold1
                packet.output1OffThrd1 = after.offThreshold1
            } else {
                if (before.onThreshold1 != after.onThreshold1) packet.output1OnThrd1 = after.onThreshold1
                if (before.offThreshold1 != after.offThreshold1) packet.output1OffThrd1 = after.offThreshold1
            }
            if (before.condition2 != after.condition2) {
                packet.output1Condition2 = after.condition2
                packet.output1OnThrd2 = after.onThreshold2
                packet.output1OffThrd2 = after.offThreshold2
            } else {
                if (before.onThreshold2 != after.onThreshold2) packet.output1OnThrd2 = after.onThreshold2
                if (before.offThreshold2 != after.offThreshold2) packet.output1OffThrd2 = after.offThreshold2
            }
            if (before.condition1Inversion != after.condition1Inversion) packet.output1Cond1Inversion = after.condition1Inversion
            if (before.condition2Inversion != after.condition2Inversion) packet.output1Cond2Inversion = after.condition2Inversion
            if (before.inversion != after.inversion) packet.output1Inversion = after.inversion
            if (before.logicFunction != after.logicFunction) packet.output1LogicFunc = after.logicFunction
        }
        2 -> {
            if (before.enabled != after.enabled) packet.output2Use = after.enabled
            if (before.condition1 != after.condition1) {
                packet.output2Condition1 = after.condition1
                packet.output2OnThrd1 = after.onThreshold1
                packet.output2OffThrd1 = after.offThreshold1
            } else {
                if (before.onThreshold1 != after.onThreshold1) packet.output2OnThrd1 = after.onThreshold1
                if (before.offThreshold1 != after.offThreshold1) packet.output2OffThrd1 = after.offThreshold1
            }
            if (before.condition2 != after.condition2) {
                packet.output2Condition2 = after.condition2
                packet.output2OnThrd2 = after.onThreshold2
                packet.output2OffThrd2 = after.offThreshold2
            } else {
                if (before.onThreshold2 != after.onThreshold2) packet.output2OnThrd2 = after.onThreshold2
                if (before.offThreshold2 != after.offThreshold2) packet.output2OffThrd2 = after.offThreshold2
            }
            if (before.condition1Inversion != after.condition1Inversion) packet.output2Cond1Inversion = after.condition1Inversion
            if (before.condition2Inversion != after.condition2Inversion) packet.output2Cond2Inversion = after.condition2Inversion
            if (before.inversion != after.inversion) packet.output2Inversion = after.inversion
            if (before.logicFunction != after.logicFunction) packet.output2LogicFunc = after.logicFunction
        }
        3 -> {
            if (before.enabled != after.enabled) packet.output3Use = after.enabled
            if (before.condition1 != after.condition1) {
                packet.output3Condition1 = after.condition1
                packet.output3OnThrd1 = after.onThreshold1
                packet.output3OffThrd1 = after.offThreshold1
            } else {
                if (before.onThreshold1 != after.onThreshold1) packet.output3OnThrd1 = after.onThreshold1
                if (before.offThreshold1 != after.offThreshold1) packet.output3OffThrd1 = after.offThreshold1
            }
            if (before.condition2 != after.condition2) {
                packet.output3Condition2 = after.condition2
                packet.output3OnThrd2 = after.onThreshold2
                packet.output3OffThrd2 = after.offThreshold2
            } else {
                if (before.onThreshold2 != after.onThreshold2) packet.output3OnThrd2 = after.onThreshold2
                if (before.offThreshold2 != after.offThreshold2) packet.output3OffThrd2 = after.offThreshold2
            }
            if (before.condition1Inversion != after.condition1Inversion) packet.output3Cond1Inversion = after.condition1Inversion
            if (before.condition2Inversion != after.condition2Inversion) packet.output3Cond2Inversion = after.condition2Inversion
            if (before.inversion != after.inversion) packet.output3Inversion = after.inversion
            if (before.logicFunction != after.logicFunction) packet.output3LogicFunc = after.logicFunction
        }
        4 -> {
            if (before.enabled != after.enabled) packet.output4Use = after.enabled
            if (before.condition1 != after.condition1) {
                packet.output4Condition1 = after.condition1
                packet.output4OnThrd1 = after.onThreshold1
                packet.output4OffThrd1 = after.offThreshold1
            } else {
                if (before.onThreshold1 != after.onThreshold1) packet.output4OnThrd1 = after.onThreshold1
                if (before.offThreshold1 != after.offThreshold1) packet.output4OffThrd1 = after.offThreshold1
            }
            if (before.condition2 != after.condition2) {
                packet.output4Condition2 = after.condition2
                packet.output4OnThrd2 = after.onThreshold2
                packet.output4OffThrd2 = after.offThreshold2
            } else {
                if (before.onThreshold2 != after.onThreshold2) packet.output4OnThrd2 = after.onThreshold2
                if (before.offThreshold2 != after.offThreshold2) packet.output4OffThrd2 = after.offThreshold2
            }
            if (before.condition1Inversion != after.condition1Inversion) packet.output4Cond1Inversion = after.condition1Inversion
            if (before.condition2Inversion != after.condition2Inversion) packet.output4Cond2Inversion = after.condition2Inversion
            if (before.inversion != after.inversion) packet.output4Inversion = after.inversion
            if (before.logicFunction != after.logicFunction) packet.output4LogicFunc = after.logicFunction
        }
        5 -> {
            if (before.enabled != after.enabled) packet.output5Use = after.enabled
            if (before.condition1 != after.condition1) {
                packet.output5Condition1 = after.condition1
                packet.output5OnThrd1 = after.onThreshold1
                packet.output5OffThrd1 = after.offThreshold1
            } else {
                if (before.onThreshold1 != after.onThreshold1) packet.output5OnThrd1 = after.onThreshold1
                if (before.offThreshold1 != after.offThreshold1) packet.output5OffThrd1 = after.offThreshold1
            }
            if (before.condition2 != after.condition2) {
                packet.output5Condition2 = after.condition2
                packet.output5OnThrd2 = after.onThreshold2
                packet.output5OffThrd2 = after.offThreshold2
            } else {
                if (before.onThreshold2 != after.onThreshold2) packet.output5OnThrd2 = after.onThreshold2
                if (before.offThreshold2 != after.offThreshold2) packet.output5OffThrd2 = after.offThreshold2
            }
            if (before.condition1Inversion != after.condition1Inversion) packet.output5Cond1Inversion = after.condition1Inversion
            if (before.condition2Inversion != after.condition2Inversion) packet.output5Cond2Inversion = after.condition2Inversion
            if (before.inversion != after.inversion) packet.output5Inversion = after.inversion
            if (before.logicFunction != after.logicFunction) packet.output5LogicFunc = after.logicFunction
        }
        6 -> {
            if (before.enabled != after.enabled) packet.output6Use = after.enabled
            if (before.condition1 != after.condition1) {
                packet.output6Condition1 = after.condition1
                packet.output6OnThrd1 = after.onThreshold1
                packet.output6OffThrd1 = after.offThreshold1
            } else {
                if (before.onThreshold1 != after.onThreshold1) packet.output6OnThrd1 = after.onThreshold1
                if (before.offThreshold1 != after.offThreshold1) packet.output6OffThrd1 = after.offThreshold1
            }
            if (before.condition2 != after.condition2) {
                packet.output6Condition2 = after.condition2
                packet.output6OnThrd2 = after.onThreshold2
                packet.output6OffThrd2 = after.offThreshold2
            } else {
                if (before.onThreshold2 != after.onThreshold2) packet.output6OnThrd2 = after.onThreshold2
                if (before.offThreshold2 != after.offThreshold2) packet.output6OffThrd2 = after.offThreshold2
            }
            if (before.condition1Inversion != after.condition1Inversion) packet.output6Cond1Inversion = after.condition1Inversion
            if (before.condition2Inversion != after.condition2Inversion) packet.output6Cond2Inversion = after.condition2Inversion
            if (before.inversion != after.inversion) packet.output6Inversion = after.inversion
            if (before.logicFunction != after.logicFunction) packet.output6LogicFunc = after.logicFunction
        }
        else -> error("Output index must be between 1 and 6")
    }
}
internal fun applyUniversalOutput(packet: UniOutParamPacket, output: Int, before: UniversalOutputValues, after: UniversalOutputValues) = applyOutputValues(packet, output, before, after)

@Composable
internal fun UniversalOutputsScreen(
    values: UniversalOutputsValues?, conditionNames: Map<Int, String>, logicalFunctions: List<String>,
    onOutputChange: (output: Int, values: UniversalOutputValues) -> Unit,
    onPairChange: (Int) -> Unit
) {
    val pageScrollState = rememberScrollState()
    if (values == null) {
        ParameterLoadingIndicator("uniout-loading")
        return
    }
    ParameterPage(scrollState = pageScrollState) {
        values.outputs.forEachIndexed { index, output ->
            OutputCard(index + 1, output, conditionNames, logicalFunctions, onOutputChange)
            if (index == 0) {
                ParameterSection {
                    FunctionPicker(R.string.uniout_outputs_link_function, values.logicFunction12, logicalFunctions, tag = "outputs-link-function") {
                        onPairChange(if (it == 4) 15 else it)
                    }
                }
            }
        }
    }
}

@Composable private fun OutputCard(index: Int, values: UniversalOutputValues, names: Map<Int, String>, functions: List<String>, onChange: (Int, UniversalOutputValues) -> Unit) {
    ParameterSection(modifier = Modifier.testTag("output-$index-card"), contentSpacing = 0.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(outputTitles[index - 1]), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(values.enabled, { onChange(index, values.copy(enabled = it)) }, modifier = Modifier.testTag("output-$index-use"))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (values.enabled) {
            ConditionGroup("output-$index-condition-1-group") {
                ConditionPicker(index, 1, outputConditionTitles[index - 1][0], values.condition1, names) { onChange(index, values.withCondition(1, it)) }
                ToggleLine(R.string.unioutput1_condition_1_inverse_title, values.condition1Inversion) { onChange(index, values.copy(condition1Inversion = it)) }
                Threshold(R.string.unioutput1_condition1_on_value_title, values.onThreshold1, names[values.condition1]?.let { uniOutConditionFormat(values.condition1) }, "output-$index-threshold-on-1") { onChange(index, values.copy(onThreshold1 = it)) }
                Threshold(R.string.unioutput1_condition1_off_value_title, values.offThreshold1, names[values.condition1]?.let { uniOutConditionFormat(values.condition1) }, "output-$index-threshold-off-1") { onChange(index, values.copy(offThreshold1 = it)) }
            }
            Spacer(Modifier.height(16.dp))
            FunctionPicker(R.string.unioutput1_logical_functions_title, values.logicFunction, functions, tag = "output-$index-logical-function") { onChange(index, values.copy(logicFunction = if (it == 4) 15 else it)) }
            if (values.logicFunction != 15) {
                Spacer(Modifier.height(8.dp))
                ConditionGroup("output-$index-condition-2-group") {
                    ConditionPicker(index, 2, outputConditionTitles[index - 1][1], values.condition2, names) { onChange(index, values.withCondition(2, it)) }
                    ToggleLine(R.string.unioutput1_condition_1_inverse_title, values.condition2Inversion) { onChange(index, values.copy(condition2Inversion = it)) }
                    Threshold(R.string.unioutput1_condition1_on_value_title, values.onThreshold2, names[values.condition2]?.let { uniOutConditionFormat(values.condition2) }, "output-$index-threshold-on-2") { onChange(index, values.copy(onThreshold2 = it)) }
                    Threshold(R.string.unioutput1_condition1_off_value_title, values.offThreshold2, names[values.condition2]?.let { uniOutConditionFormat(values.condition2) }, "output-$index-threshold-off-2") { onChange(index, values.copy(offThreshold2 = it)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            ToggleLine(R.string.unioutput_inverse_result_title, values.inversion, tag = "output-$index-result-inversion") { onChange(index, values.copy(inversion = it)) }
        } else Text(stringResource(R.string.uniout_disabled), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp).testTag("output-$index-collapsed"))
    }
}

@Composable private fun ConditionGroup(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, DropdownShape)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

private val outputTitles = listOf(R.string.unioutput1_use_title, R.string.unioutput2_use_title, R.string.unioutput3_use_title, R.string.unioutput4_use_title, R.string.unioutput5_use_title, R.string.unioutput6_use_title)
private val outputConditionTitles = listOf(
    listOf(R.string.unioutput1_condition_1_title, R.string.unioutput1_condition_2_title),
    listOf(R.string.unioutput2_condition_1_title, R.string.unioutput2_condition_2_title),
    listOf(R.string.unioutput3_condition_1_title, R.string.unioutput3_condition_2_title),
    listOf(R.string.unioutput4_condition_1_title, R.string.unioutput4_condition_2_title),
    listOf(R.string.unioutput5_condition_1_title, R.string.unioutput5_condition_2_title),
    listOf(R.string.unioutput6_condition_1_title, R.string.unioutput6_condition_2_title),
)

@Composable private fun ToggleLine(labelId: Int, value: Boolean, tag: String? = null, onValue: (Boolean) -> Unit) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp).clickable { onValue(!value) }.padding(horizontal = 14.dp, vertical = 11.dp)
    Row(if (tag == null) modifier else modifier.testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelId), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Checkbox(value, onValue)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable private fun ConditionPicker(output: Int, slot: Int, labelId: Int, selected: Int, names: Map<Int, String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(labelId)
    val conditionName = names[selected] ?: stringResource(R.string.uniout_unknown_code, selected)
    Box {
        ParameterSelectionRow(label, conditionName, "output-$output-condition-$slot-picker") { expanded = true }
        if (expanded) ParameterChoiceDialog(
            title = label,
            choices = names.toSortedMap().filter { (id, _) -> slot != 1 || id !in setOf(7, 23) }
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

@Composable private fun Threshold(labelId: Int, value: Float, format: UniOutConditionFormat?, tag: String, onValue: (Float) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val label = stringResource(labelId)
    if (format == null) {
        Text(stringResource(R.string.uniout_threshold_unavailable, label), Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp))
    } else {
        ParameterTextValueRow(
            title = label,
            value = String.format(Locale.US, "% .${format.precision}f", value).trim(),
            testTag = tag,
            numberText = String.format(Locale.US, "% .${format.precision}f", value).trim(),
            onClick = { editing = true },
        )
        if (editing) {
            var text by remember(value) { mutableStateOf(value.toString()) }
            ParameterAlertDialog(onDismissRequest = { editing = false }, title = { Text(stringResource(R.string.uniout_threshold_edit_title, label)) },
                text = { OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.uniout_threshold_value_label)) }, shape = DropdownShape) },
                confirmButton = { ParameterConfirmButton(stringResource(android.R.string.ok)) { text.toFloatOrNull()?.coerceIn(format.min, format.max)?.let(onValue); editing = false } },
                dismissButton = { ParameterCancelButton { editing = false } })
        }
    }
}
