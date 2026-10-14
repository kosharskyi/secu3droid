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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket

@Composable
internal fun FirmwareInfoSheet(
    firmware: FirmwareInfoPacket?,
    onDismiss: () -> Unit,
) {
    MaterialTheme {
        val maxSheetHeight = with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp() * 0.9f
        }
        val scrollState = rememberScrollState()

        Surface(
            color = FirmwareSheetBackground,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 22.dp)
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
                    color = FirmwareTextSecondary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(22.dp))

                val capabilities = firmware.capabilities()
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
            color = FirmwareTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close_24),
                contentDescription = stringResource(R.string.close),
                tint = FirmwareTextMuted,
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
        color = if (enabled) FirmwareGreen else FirmwareTextDisabled,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp,
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
    val contentColor = if (enabled) FirmwareGreen else FirmwareTextDisabled
    val background = if (enabled) FirmwareGreenContainer else FirmwareDisabledContainer
    val stroke = if (enabled) FirmwareGreenStroke else FirmwareDisabledStroke

    Text(
        text = label,
        color = contentColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .testTag(firmwareCapabilityChipTag(label, enabled))
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
        CircularProgressIndicator(color = FirmwareGreen)
    }
}

private data class FirmwareCapability(
    val label: String,
    val enabled: Boolean,
)

internal fun firmwareCapabilityChipTag(label: String, enabled: Boolean): String {
    return "firmware-capability-$label-${if (enabled) "enabled" else "disabled"}"
}

internal const val FirmwareLoadingTag = "firmware-loading"

private fun FirmwareInfoPacket.capabilities(): List<FirmwareCapability> {
    return listOf(
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

private val FirmwareSheetBackground = Color(0xFF18212A)
private val FirmwareTextPrimary = Color(0xFFE6EDF5)
private val FirmwareTextSecondary = Color(0xFF8FA1B4)
private val FirmwareTextMuted = Color(0xFF7F90A3)
private val FirmwareTextDisabled = Color(0xFF6F7882)
private val FirmwareGreen = Color(0xFF20D67A)
private val FirmwareGreenContainer = Color(0xFF123529)
private val FirmwareGreenStroke = Color(0xFF1B6A4D)
private val FirmwareDisabledContainer = Color(0xFF26313B)
private val FirmwareDisabledStroke = Color(0xFF3A4651)
