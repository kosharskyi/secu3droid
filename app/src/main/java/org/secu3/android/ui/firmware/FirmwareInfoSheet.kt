/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.firmware

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.ui.compose.LocalSecuPalette
import org.secu3.android.ui.compose.SecuComposeTheme

@Composable
internal fun FirmwareInfoSheet(
    firmware: FirmwareInfoPacket?,
    onDismiss: () -> Unit,
) {
    SecuComposeTheme {
        val maxSheetHeight = with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp() * 0.9f
        }
        val scrollState = rememberScrollState()

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(scrollState),
            ) {
                FirmwareHeader(onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(4.dp))

                if (firmware == null) {
                    FirmwareLoadingState()
                    return@Column
                }

                Text(
                    text = firmware.tag.ifBlank { stringResource(R.string.firmware_unknown_value) },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )

                Spacer(modifier = Modifier.height(22.dp))

                val capabilities = with(firmware) {
                    listOf(
                        FirmwareCapability("OBD_SUPPORT", isObdSupported),
                        FirmwareCapability("ATMEGA1284", isATMEGA1284),
                        FirmwareCapability("SPLIT_ANGLE", isSplitAngle),
                        FirmwareCapability("TPIC8101", isTPIC8101),
                        FirmwareCapability("CAM_SYNC", isCamSyncSupported),
                        FirmwareCapability("DWELL_CONTROL", isDwellControlSupported),
                        FirmwareCapability("COOLINGFAN_PWM", isCoolingFanPwmEnabled),
                        FirmwareCapability("REALTIME_TABLES", isRealTimeTablesEnabled),
                        FirmwareCapability("ICCAVR_COMPILER", isIccAvrCompiler),
                        FirmwareCapability("AVRGCC_COMPILER", isAvrGccCompiler),
                        FirmwareCapability("DEBUG_VARIABLES", isDebugVariablesEnabled),
                        FirmwareCapability("PHASE_SENSOR", isPhaseSensorEnabled),
                        FirmwareCapability("PHASED_IGNITION", isPhasedIgnitionEnabled),
                        FirmwareCapability("FUEL_PUMP", isFuelPumpEnabled),
                        FirmwareCapability("THERMISTOR_CS", isThermistorCsEnabled),
                        FirmwareCapability("SECU3T_COMPAT", isSecu3T),
                        FirmwareCapability("DIAGNOSTICS", isDiagnosticsEnabled),
                        FirmwareCapability("HALL_OUTPUT", isHallOutputEnabled),
                        FirmwareCapability("REV9_BOARD", isRev9Board),
                        FirmwareCapability("STROBOSCOPE", isStroboscopeEnabled),
                        FirmwareCapability("SM_CONTROL", isSmControlEnabled),
                        FirmwareCapability("HALL_SYNC", isHallSyncEnabled),
                        FirmwareCapability("CKPS_2CHIGN", isCkps2ChignEnabled),
                        FirmwareCapability("FUEL_INJECTION", isFuelInjectEnabled),
                        FirmwareCapability("GAS_DOSE", isGdControlEnabled),
                        FirmwareCapability("CARB_AFR", isCarbAfrEnabled),
                        FirmwareCapability("CKPS_NPLUS1", isCkpsNplus1Enabled),
                    )
                }
                CapabilitySection(
                    title = stringResource(R.string.firmware_enabled_capabilities),
                    capabilities = capabilities.filter { it.enabled },
                    enabled = true,
                )

                Spacer(modifier = Modifier.height(20.dp))

                CapabilitySection(
                    title = stringResource(R.string.firmware_disabled_capabilities),
                    capabilities = capabilities.filterNot { it.enabled },
                    enabled = false,
                )

            }
        }
    }
}

@Composable
private fun FirmwareHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.firmware_information_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close_24),
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapabilitySection(
    title: String,
    capabilities: List<FirmwareCapability>,
    enabled: Boolean,
) {
    Text(
        text = title,
        color = if (enabled) LocalSecuPalette.current.success else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
    Spacer(modifier = Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        capabilities.forEach { capability ->
            CapabilityChip(
                label = capability.label,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun CapabilityChip(
    label: String,
    enabled: Boolean,
) {
    val contentColor = if (enabled) LocalSecuPalette.current.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (enabled) LocalSecuPalette.current.successContainer else MaterialTheme.colorScheme.surfaceVariant
    val stroke = MaterialTheme.colorScheme.outlineVariant

    Text(
        text = label,
        color = contentColor,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .testTag("firmware-capability-$label-${if (enabled) "enabled" else "disabled"}")
            .semantics { selected = enabled }
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, stroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun FirmwareLoadingState() {
    Box(
        modifier = Modifier
            .testTag(FirmwareLoadingTag)
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = LocalSecuPalette.current.success)
    }
}

private data class FirmwareCapability(
    val label: String,
    val enabled: Boolean,
)

internal const val FirmwareLoadingTag = "firmware-loading"
