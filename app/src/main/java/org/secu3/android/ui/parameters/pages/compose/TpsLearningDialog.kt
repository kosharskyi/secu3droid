/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import org.secu3.android.R
import org.secu3.android.domain.parameters.TpsLearningState
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceList

@Composable
internal fun TpsLearningDialog(
    state: TpsLearningState,
    onCapture: () -> Unit,
    onApply: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit,
) {
    val capture = (state as? TpsLearningState.Measure)?.capture
    val measuring = capture == TpsLearningState.Capture.Measuring
    val buttonLabel = when (state) {
        is TpsLearningState.Measure -> if (capture == TpsLearningState.Capture.Retry) {
            R.string.tps_learning_retry
        } else R.string.tps_learning_measure
        is TpsLearningState.Review -> R.string.apply
        is TpsLearningState.Invalid -> R.string.tps_learning_restart
    }
    ParameterAlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.tps_learning_title)) },
        text = {
            ParameterChoiceList {
                when (state) {
                    is TpsLearningState.Measure -> {
                        val opening = state.openVoltage == null
                        Text(
                            stringResource(
                                if (opening) R.string.tps_learning_open_throttle else R.string.tps_learning_close_throttle,
                            ),
                            modifier = Modifier.testTag(if (opening) "tps_learning_measure_open" else "tps_learning_measure_closed"),
                        )
                        if (measuring) Text(stringResource(R.string.tps_learning_measuring))
                        if (capture == TpsLearningState.Capture.Retry) {
                            Text(stringResource(R.string.tps_learning_read_failed), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is TpsLearningState.Review -> {
                        val locale = LocalConfiguration.current.locales[0]
                        Text(
                            stringResource(
                                R.string.tps_learning_samples,
                                String.format(locale, "%.4f", state.openVoltage),
                                String.format(locale, "%.4f", state.closedVoltage),
                                stringResource(R.string.units_volts),
                            ),
                        )
                        Text(
                            stringResource(
                                R.string.tps_learning_coefficients,
                                String.format(locale, "%.4f", state.offsetVolts),
                                stringResource(R.string.units_volts),
                                String.format(locale, "%.3f", state.gradientPercentPerVolt),
                                stringResource(R.string.units_percent_per_volts),
                            ),
                            modifier = Modifier.testTag("tps_learning_review"),
                        )
                    }
                    is TpsLearningState.Invalid -> {
                        val error = when (state.reason) {
                            TpsLearningState.Error.SpanTooSmall -> R.string.tps_learning_span_too_small
                            TpsLearningState.Error.OutsidePacketRange -> R.string.tps_learning_outside_packet_range
                        }
                        Text(stringResource(error), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("tps_learning_cancel")) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                enabled = !measuring,
                onClick = when (state) {
                    is TpsLearningState.Measure -> onCapture
                    is TpsLearningState.Review -> onApply
                    is TpsLearningState.Invalid -> onRestart
                },
                modifier = Modifier.testTag("tps_learning_continue"),
            ) { Text(stringResource(buttonLabel)) }
        },
    )
}
