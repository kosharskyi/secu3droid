/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 */
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.domain.parameters.AdcCorrectionsValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow

enum class AdcNumber(val title: String, val precision: Int, val step: Float) {
    MAP_FACTOR("MAP factor", 3, .001f), MAP_CORRECTION("MAP correction", 4, .0025f),
    VOLTAGE_FACTOR("Voltage factor", 3, .001f), VOLTAGE_CORRECTION("Voltage correction", 4, .0025f),
    CTS_FACTOR("CTS factor", 3, .001f), CTS_CORRECTION("CTS correction", 4, .0025f),
    TPS_FACTOR("TPS factor", 3, .001f), TPS_CORRECTION("TPS correction", 4, .0025f),
    ADD_I1_FACTOR("ADD_I1 factor", 3, .001f), ADD_I1_CORRECTION("ADD_I1 correction", 4, .0025f),
    ADD_I2_FACTOR("ADD_I2 factor", 3, .001f), ADD_I2_CORRECTION("ADD_I2 correction", 4, .0025f),
    ADD_I3_FACTOR("ADD_I3 factor", 3, .001f), ADD_I3_CORRECTION("ADD_I3 correction", 4, .0025f),
    ADD_I4_FACTOR("ADD_I4 factor", 3, .001f), ADD_I4_CORRECTION("ADD_I4 correction", 4, .0025f),
    ADD_I5_FACTOR("ADD_I5 factor", 3, .001f), ADD_I5_CORRECTION("ADD_I5 correction", 4, .0025f),
    ADD_I6_FACTOR("ADD_I6 factor", 3, .001f), ADD_I6_CORRECTION("ADD_I6 correction", 4, .0025f),
    ADD_I7_FACTOR("ADD_I7 factor", 3, .001f), ADD_I7_CORRECTION("ADD_I7 correction", 4, .0025f),
    ADD_I8_FACTOR("ADD_I8 factor", 3, .001f), ADD_I8_CORRECTION("ADD_I8 correction", 4, .0025f);

    val isFactor = name.endsWith("FACTOR")
    val min = if (isFactor) 0f else -2f
    val max = if (isFactor) 3.999f else 1.999f
    val units: String get() = if (name.endsWith("CORRECTION")) "V" else ""

    fun value(values: AdcCorrectionsValues): Float = when (this) {
        MAP_FACTOR -> values.mapFactor; MAP_CORRECTION -> values.mapCorrection
        VOLTAGE_FACTOR -> values.voltageFactor; VOLTAGE_CORRECTION -> values.voltageCorrection
        CTS_FACTOR -> values.ctsFactor; CTS_CORRECTION -> values.ctsCorrection
        TPS_FACTOR -> values.tpsFactor; TPS_CORRECTION -> values.tpsCorrection
        ADD_I1_FACTOR -> values.addI1Factor; ADD_I1_CORRECTION -> values.addI1Correction
        ADD_I2_FACTOR -> values.addI2Factor; ADD_I2_CORRECTION -> values.addI2Correction
        ADD_I3_FACTOR -> values.addI3Factor; ADD_I3_CORRECTION -> values.addI3Correction
        ADD_I4_FACTOR -> values.addI4Factor; ADD_I4_CORRECTION -> values.addI4Correction
        ADD_I5_FACTOR -> values.addI5Factor; ADD_I5_CORRECTION -> values.addI5Correction
        ADD_I6_FACTOR -> values.addI6Factor; ADD_I6_CORRECTION -> values.addI6Correction
        ADD_I7_FACTOR -> values.addI7Factor; ADD_I7_CORRECTION -> values.addI7Correction
        ADD_I8_FACTOR -> values.addI8Factor; ADD_I8_CORRECTION -> values.addI8Correction
    }
}

@Composable
fun AdcErrorsCorrectionsScreen(
    state: AdcCorrectionsValues?,
    onEdit: (AdcNumber) -> Unit,
    onCompModeChanged: (Boolean) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (state == null) {
        ParameterLoadingIndicator("adc_loading")
    } else {
        ParameterPage(scrollState = pageScrollState) {
            val groups = listOf("MAP", "Voltage", "CTS", "TPS", "ADD_I1", "ADD_I2", "ADD_I3", "ADD_I4", "ADD_I5", "ADD_I6", "ADD_I7", "ADD_I8")
            ParameterSection {
                groups.forEachIndexed { index, group ->
                    Column(Modifier.fillMaxWidth().testTag("adc_group_$index")) {
                        Text(group, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), style = MaterialTheme.typography.titleSmall)
                        listOf(AdcNumber.entries[index * 2], AdcNumber.entries[index * 2 + 1]).forEach { number ->
                            ParameterValueRow(
                                title = number.title,
                                units = if (number.units.isEmpty()) "" else stringResource(R.string.units_volts),
                                value = number.value(state), precision = number.precision,
                                testTag = "adc_${number.name.lowercase()}",
                                enabled = capabilities.adcChannelEnabled(index),
                                onClick = { onEdit(number) },
                            )
                        }
                    }
                }
                ParameterSwitchRow(stringResource(R.string.adc_cor_multiply_corr_and_koef_title), state.adcCompMode, "adc_comp_mode", onCompModeChanged)
            }
        }
    }
}
