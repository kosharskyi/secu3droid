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
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.compose.ParameterValueRow
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection

enum class AdcNumber(val title: String, val precision: Int, val step: Float) {
    MAP_FACTOR("MAP factor", 3, .001f), MAP_CORRECTION("MAP correction", 3, .0025f),
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
}

data class AdcCorrectionsValues(val numbers: Map<AdcNumber, Float>, val adcCompMode: Boolean) {
    fun value(number: AdcNumber) = numbers.getValue(number)
    fun withValue(number: AdcNumber, value: Float) = copy(numbers = numbers + (number to value))
    fun withCompMode(value: Boolean) = copy(adcCompMode = value)

    companion object {
        fun from(packet: AdcCorrectionsParamPacket) = AdcCorrectionsValues(
            AdcNumber.entries.associateWith { number -> packet.adcValue(number) }, packet.adcCompMode,
        )
    }
}

fun AdcCorrectionsParamPacket.adcValue(number: AdcNumber): Float = when (number) {
    AdcNumber.MAP_FACTOR -> mapAdcFactor; AdcNumber.MAP_CORRECTION -> mapAdcCorrection
    AdcNumber.VOLTAGE_FACTOR -> ubatAdcFactor; AdcNumber.VOLTAGE_CORRECTION -> ubatAdcCorrection
    AdcNumber.CTS_FACTOR -> tempAdcFactor; AdcNumber.CTS_CORRECTION -> tempAdcCorrection
    AdcNumber.TPS_FACTOR -> tpsAdcFactor; AdcNumber.TPS_CORRECTION -> tpsAdcCorrection
    AdcNumber.ADD_I1_FACTOR -> ai1AdcFactor; AdcNumber.ADD_I1_CORRECTION -> ai1AdcCorrection
    AdcNumber.ADD_I2_FACTOR -> ai2AdcFactor; AdcNumber.ADD_I2_CORRECTION -> ai2AdcCorrection
    AdcNumber.ADD_I3_FACTOR -> ai3AdcFactor; AdcNumber.ADD_I3_CORRECTION -> ai3AdcCorrection
    AdcNumber.ADD_I4_FACTOR -> ai4AdcFactor; AdcNumber.ADD_I4_CORRECTION -> ai4AdcCorrection
    AdcNumber.ADD_I5_FACTOR -> ai5AdcFactor; AdcNumber.ADD_I5_CORRECTION -> ai5AdcCorrection
    AdcNumber.ADD_I6_FACTOR -> ai6AdcFactor; AdcNumber.ADD_I6_CORRECTION -> ai6AdcCorrection
    AdcNumber.ADD_I7_FACTOR -> ai7AdcFactor; AdcNumber.ADD_I7_CORRECTION -> ai7AdcCorrection
    AdcNumber.ADD_I8_FACTOR -> ai8AdcFactor; AdcNumber.ADD_I8_CORRECTION -> ai8AdcCorrection
}

fun AdcCorrectionsParamPacket.setAdcValue(number: AdcNumber, value: Float) {
    markChannelEdited(number.ordinal / 2)
    when (number) {
        AdcNumber.MAP_FACTOR -> mapAdcFactor = value
        AdcNumber.MAP_CORRECTION -> { mapAdcCorrection = value }
        AdcNumber.VOLTAGE_FACTOR -> ubatAdcFactor = value
        AdcNumber.VOLTAGE_CORRECTION -> { ubatAdcCorrection = value }
        AdcNumber.CTS_FACTOR -> tempAdcFactor = value
        AdcNumber.CTS_CORRECTION -> { tempAdcCorrection = value }
        AdcNumber.TPS_FACTOR -> tpsAdcFactor = value
        AdcNumber.TPS_CORRECTION -> { tpsAdcCorrection = value }
        AdcNumber.ADD_I1_FACTOR -> ai1AdcFactor = value
        AdcNumber.ADD_I1_CORRECTION -> { ai1AdcCorrection = value }
        AdcNumber.ADD_I2_FACTOR -> ai2AdcFactor = value
        AdcNumber.ADD_I2_CORRECTION -> { ai2AdcCorrection = value }
        AdcNumber.ADD_I3_FACTOR -> ai3AdcFactor = value
        AdcNumber.ADD_I3_CORRECTION -> { ai3AdcCorrection = value }
        AdcNumber.ADD_I4_FACTOR -> ai4AdcFactor = value
        AdcNumber.ADD_I4_CORRECTION -> { ai4AdcCorrection = value }
        AdcNumber.ADD_I5_FACTOR -> ai5AdcFactor = value
        AdcNumber.ADD_I5_CORRECTION -> { ai5AdcCorrection = value }
        AdcNumber.ADD_I6_FACTOR -> ai6AdcFactor = value
        AdcNumber.ADD_I6_CORRECTION -> { ai6AdcCorrection = value }
        AdcNumber.ADD_I7_FACTOR -> ai7AdcFactor = value
        AdcNumber.ADD_I7_CORRECTION -> { ai7AdcCorrection = value }
        AdcNumber.ADD_I8_FACTOR -> ai8AdcFactor = value
        AdcNumber.ADD_I8_CORRECTION -> { ai8AdcCorrection = value }
    }
}

sealed interface AdcCorrectionsUiState {
    data object Loading : AdcCorrectionsUiState
    data class Data(val values: AdcCorrectionsValues) : AdcCorrectionsUiState
}

@Composable
fun AdcErrorsCorrectionsScreen(
    state: AdcCorrectionsUiState,
    onEdit: (AdcNumber) -> Unit,
    onCompModeChanged: (Boolean) -> Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    when (state) {
        AdcCorrectionsUiState.Loading -> ParameterLoadingIndicator("adc_loading")
        is AdcCorrectionsUiState.Data -> ParameterPage(scrollState = pageScrollState) {
            val groups = listOf("MAP", "Voltage", "CTS", "TPS", "ADD_I1", "ADD_I2", "ADD_I3", "ADD_I4", "ADD_I5", "ADD_I6", "ADD_I7", "ADD_I8")
            ParameterSection {
                groups.forEachIndexed { index, group ->
                    Column(Modifier.fillMaxWidth().testTag("adc_group_$index")) {
                        Text(
                            group,
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val factor = AdcNumber.entries[index * 2]
                        val correction = AdcNumber.entries[index * 2 + 1]
                        listOf(factor, correction).forEach { number ->
                            ParameterValueRow(
                                title = number.title,
                                units = if (number.units.isEmpty()) "" else stringResource(R.string.units_volts),
                                value = state.values.value(number), precision = number.precision,
                                testTag = "adc_${number.name.lowercase()}",
                                enabled = when {
                                    number.name.startsWith("ADD_I4") -> capabilities.extraAnalogInputs
                                    number.ordinal >= AdcNumber.ADD_I3_FACTOR.ordinal -> !capabilities.secu3T
                                    else -> true
                                }, onClick = { onEdit(number) },
                            )
                        }
                    }
                }
                ParameterSwitchRow(
                    stringResource(R.string.adc_cor_multiply_corr_and_koef_title), state.values.adcCompMode,
                    "adc_comp_mode", onCompModeChanged,
                )
            }
        }
    }
}
