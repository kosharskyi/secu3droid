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
package org.secu3.android.ui.parameters.dialogs

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import org.secu3.android.R

@Composable
internal fun NumericEditDialogContent(
    title: String,
    initialText: String,
    allowDecimal: Boolean,
    allowNegative: Boolean,
    validate: (String) -> Boolean,
    onStep: (String, Int) -> String,
    onConfirm: (String, Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    sessionKey: Long = 0L,
) {
    var inputValue by rememberSaveable(sessionKey, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialText, selection = androidx.compose.ui.text.TextRange(initialText.length)))
    }
    val inputText = inputValue.text
    var changed by rememberSaveable(sessionKey) { mutableStateOf(false) }
    val valid = validate(inputText)

    val dialogShape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, dialogShape)
            .testTag("numeric_editor_surface"),
        shape = dialogShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        changed = true
                        val stepped = onStep(inputText, -1)
                        inputValue = TextFieldValue(stepped, selection = androidx.compose.ui.text.TextRange(stepped.length))
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Decrease value" },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Column(Modifier.weight(1f)) {
                    val inputShape = RoundedCornerShape(12.dp)
                    val borderColor = if (valid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                    val inputTextColor = MaterialTheme.colorScheme.onSurface
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .clip(inputShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, borderColor, inputShape)
                            .padding(horizontal = 12.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        BasicTextField(
                            value = inputValue,
                            onValueChange = { value ->
                                if (value.text != inputValue.text) changed = true
                                inputValue = value
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("numeric_value"),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = inputTextColor,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                ) {
                                    innerTextField()
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = when {
                                    allowDecimal && allowNegative -> KeyboardType.DecimalSigned
                                    allowDecimal -> KeyboardType.Decimal
                                    allowNegative -> KeyboardType.NumberSigned
                                    else -> KeyboardType.Number
                                },
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }
                    if (!valid) {
                        Text(
                            stringResource(R.string.numeric_edit_invalid_value),
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                FilledTonalButton(
                    onClick = {
                        changed = true
                        val stepped = onStep(inputText, 1)
                        inputValue = TextFieldValue(stepped, selection = androidx.compose.ui.text.TextRange(stepped.length))
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Increase value" },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.height(16.dp))
            NumericDialogActions(
                onCancel = onCancel,
                onConfirm = { onConfirm(inputText, changed) },
                enabled = valid,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumericDialogActions(onCancel: () -> Unit, onConfirm: () -> Unit, enabled: Boolean) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
                TextButton(onClick = onCancel, modifier = Modifier.sizeIn(minHeight = 48.dp).testTag("numeric_cancel")) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = enabled,
                    modifier = Modifier.sizeIn(minHeight = 48.dp).testTag("numeric_confirm"),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(android.R.string.ok)) }
    }
}
