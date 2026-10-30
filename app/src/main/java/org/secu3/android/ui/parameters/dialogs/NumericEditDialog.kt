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
 */
package org.secu3.android.ui.parameters.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.secu3.android.ui.parameters.ParameterEditRequest

@Composable
internal fun NumericEditDialog(
    request: ParameterEditRequest,
    sessionId: Long,
    onConfirm: (Number) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
    ) {
        when (request) {
            is ParameterEditRequest.IntValue -> {
                var fallback by rememberSaveable(sessionId) { mutableIntStateOf(request.value) }
                NumericEditDialogContent(
                    title = request.title,
                    initialText = request.value.toString(),
                    sessionKey = sessionId,
                    allowDecimal = false,
                    allowNegative = request.min < 0,
                    validate = { request.parseInput(it) != null },
                    onStep = { text, direction ->
                        fallback = request.stepValue(text, fallback, direction)
                        fallback.toString()
                    },
                    onConfirm = { text, changed ->
                        val accepted = if (changed) request.parseInput(text) else request.value
                        accepted?.let(onConfirm)
                    },
                    onCancel = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is ParameterEditRequest.FloatValue -> {
                var fallback by rememberSaveable(sessionId) { mutableFloatStateOf(request.value) }
                NumericEditDialogContent(
                    title = request.title,
                    initialText = request.formatValue(),
                    sessionKey = sessionId,
                    allowDecimal = true,
                    allowNegative = request.min < 0f,
                    validate = { request.parseInput(it) != null },
                    onStep = { text, direction ->
                        fallback = request.stepValue(text, fallback, direction)
                        request.formatValue(fallback)
                    },
                    onConfirm = { text, changed ->
                        val accepted = if (changed) request.parseInput(text) else request.value
                        accepted?.let(onConfirm)
                    },
                    onCancel = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
