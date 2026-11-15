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
import org.secu3.android.domain.parameters.PositionLearningState
import org.secu3.android.domain.parameters.PositionSensor
import org.secu3.android.ui.parameters.dialogs.ParameterAlertDialog
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceList

@Composable
internal fun PositionLearningDialog(
    state: PositionLearningState,
    onCapture: () -> Unit,
    onApply: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit,
) {
    val pedal = state.sensor == PositionSensor.APPS1
    val tag = "${state.sensor.name.lowercase()}_learning"
    val capture = (state as? PositionLearningState.Measure)?.capture
    val measuring = capture == PositionLearningState.Capture.Measuring
    val buttonLabel = when (state) {
        is PositionLearningState.Measure -> if (capture == PositionLearningState.Capture.Retry) {
            R.string.tps_learning_retry
        } else R.string.tps_learning_measure
        is PositionLearningState.Review -> R.string.apply
        is PositionLearningState.Invalid -> R.string.tps_learning_restart
    }
    ParameterAlertDialog(
        onDismissRequest = onCancel,
        title = { Text(state.sensor.learningTitle()) },
        text = {
            ParameterChoiceList {
                when (state) {
                    is PositionLearningState.Measure -> {
                        val full = state.fullVoltage == null
                        val instruction = when {
                            pedal && full -> R.string.apps1_learning_press_pedal
                            pedal -> R.string.apps1_learning_release_pedal
                            full -> R.string.tps_learning_open_throttle
                            else -> R.string.tps_learning_close_throttle
                        }
                        Text(
                            stringResource(instruction),
                            modifier = Modifier.testTag(if (full) "${tag}_measure_open" else "${tag}_measure_closed"),
                        )
                        if (measuring) Text(stringResource(R.string.tps_learning_measuring))
                        if (capture == PositionLearningState.Capture.Retry) {
                            val message = if (pedal) R.string.apps1_learning_read_failed else R.string.tps_learning_read_failed
                            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is PositionLearningState.Review -> {
                        val locale = LocalConfiguration.current.locales[0]
                        Text(
                            stringResource(
                                if (pedal) R.string.apps1_learning_samples else R.string.tps_learning_samples,
                                String.format(locale, "%.4f", state.fullVoltage),
                                String.format(locale, "%.4f", state.restVoltage),
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
                            modifier = Modifier.testTag("${tag}_review"),
                        )
                    }
                    is PositionLearningState.Invalid -> {
                        val error = when (state.reason) {
                            PositionLearningState.Error.SpanTooSmall -> R.string.tps_learning_span_too_small
                            PositionLearningState.Error.OutsidePacketRange -> R.string.tps_learning_outside_packet_range
                        }
                        Text(stringResource(error), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("${tag}_cancel")) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                enabled = !measuring,
                onClick = when (state) {
                    is PositionLearningState.Measure -> onCapture
                    is PositionLearningState.Review -> onApply
                    is PositionLearningState.Invalid -> onRestart
                },
                modifier = Modifier.testTag("${tag}_continue"),
            ) { Text(stringResource(buttonLabel)) }
        },
    )
}

@Composable
internal fun PositionSensor.learningTitle(): String = stringResource(
    when (this) {
        PositionSensor.TPS -> R.string.tps_learning_title
        PositionSensor.APPS1 -> R.string.apps1_learning_title
    },
)
