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

import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
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
) {
    var inputText by remember(initialText) { mutableStateOf(initialText) }
    var changed by remember(initialText) { mutableStateOf(false) }
    val valid = validate(inputText)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(title, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { changed = true; inputText = onStep(inputText, -1) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Decrease value" },
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Column(Modifier.weight(1f)) {
                    val inputShape = RoundedCornerShape(4.dp)
                    val borderColor = if (valid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                    val inputTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .border(1.dp, borderColor, inputShape)
                            .padding(horizontal = 8.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("numeric_value"),
                            factory = { context ->
                                EditText(context).apply {
                                    id = android.R.id.edit
                                    background = null
                                    gravity = Gravity.CENTER
                                    textSize = 16f
                                    setTextColor(inputTextColor)
                                    inputType = InputType.TYPE_CLASS_NUMBER or
                                        (if (allowDecimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0) or
                                        (if (allowNegative) InputType.TYPE_NUMBER_FLAG_SIGNED else 0)
                                    imeOptions = EditorInfo.IME_ACTION_DONE
                                    isSingleLine = true
                                    setText(inputText)
                                    addTextChangedListener { editable ->
                                        inputText = editable?.toString().orEmpty()
                                        changed = true
                                    }
                                }
                            },
                            update = { editText ->
                                editText.setTextColor(inputTextColor)
                                if (editText.text.toString() != inputText) {
                                    editText.setText(inputText)
                                    editText.setSelection(editText.text.length)
                                }
                            },
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
                OutlinedButton(
                    onClick = { changed = true; inputText = onStep(inputText, 1) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Increase value" },
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = { onConfirm(inputText, changed) },
                    enabled = valid,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) { Text(stringResource(android.R.string.ok)) }
            }
        }
    }
}
