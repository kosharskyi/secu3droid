/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun <T> ChoiceDialog(
    title: @Composable () -> Unit,
    selected: T,
    choices: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    optionTestTag: (T) -> String? = { null },
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    optionMinHeight: Dp = 56.dp,
    optionHorizontalPadding: Dp = 8.dp,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shape,
        containerColor = containerColor,
        title = title,
        text = {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 336.dp)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                choices.forEach { choice ->
                    val choiceTag = optionTestTag(choice)
                    Row(
                        Modifier.fillMaxWidth()
                            .heightIn(min = optionMinHeight)
                            .selectable(
                                selected = selected == choice,
                                role = Role.RadioButton,
                                onClick = { onSelect(choice) },
                            )
                            .then(choiceTag?.let { Modifier.testTag(it) } ?: Modifier)
                            .padding(horizontal = optionHorizontalPadding),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == choice, onClick = null)
                        Text(
                            text = label(choice),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = { confirmButton() },
    )
}
