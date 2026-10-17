/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
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
package org.secu3.android.ui.parameters.pages

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.*
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow

internal val BAUD_RATE_CODES = linkedMapOf(0x0411 to "2400", 0x0208 to "4800", 0x0103 to "9600", 0x00AD to "14400", 0x0081 to "19200", 0x0056 to "28800", 0x0040 to "38400", 0x002A to "57600", 0x0015 to "115200", 0x0009 to "250000", 0x0004 to "500000")

internal data class MiscState(val baudDivisor:Int,val period:Int,val ign:Boolean,val cutoff:Int,val start:Int,val duration:Int,val pump:Boolean,val gas:Boolean,val petrol:Boolean,val begin:Int,val end:Int,val timeout:Float,val pwm0:Int,val pwm1:Int,val vss:Int)
internal enum class MiscToggle { IGN, PUMP, GAS, PETROL }
internal enum class MiscField { PERIOD, CUTOFF, START, DURATION, BEGIN, END, TIMEOUT, PWM0, PWM1, VSS }
internal fun miscFieldEnabled(field: MiscField, ignitionCutoffEnabled: Boolean): Boolean =
    field != MiscField.CUTOFF || ignitionCutoffEnabled
internal fun evapEditBounds(field:MiscField, begin:Int, end:Int): Pair<Int,Int>? {
    val min = EVAP_MIN
    val max = EVAP_MAX
    val gap = MiscellaneousParamPacket.MIN_EVAP_AIRFLOW_DELTA
    val bounds = when (field) {
        MiscField.BEGIN -> min to minOf(max, end - gap)
        MiscField.END -> maxOf(min, begin + gap) to max
        else -> return null
    }
    return bounds.takeIf { it.first <= it.second }
}

private const val EVAP_MIN = 100
private const val EVAP_MAX = 2_000_000

@Composable
internal fun MiscellaneousScreen(
    s: MiscState?, hasValidationError:Boolean, rates: Map<Int,String>, intEdit: (MiscField)->Unit, floatEdit: ()->Unit,
    baud: (Int)->Unit, toggleAction:(MiscToggle,Boolean)->Unit,
    capabilities: ParameterCapabilities = ParameterCapabilities.All,
) {
    val pageScrollState = rememberScrollState()
    if (s == null) { ParameterLoadingIndicator("misc-loading"); return }
    val locale = LocalConfiguration.current.locales[0]
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection {
            var expanded by remember { mutableStateOf(false) }
            Box {
                ParameterSelectionRow(stringResource(R.string.miscel_baudrate_title), rates[s.baudDivisor] ?: "Code ${s.baudDivisor}", "misc-baud-selector") { expanded=true }
                if (expanded) ParameterChoiceDialog(
                    title = stringResource(R.string.miscel_baudrate_title),
                    choices = rates.map { (code, label) -> ParameterChoice(code, label, "misc-baud-option-$code") },
                    onDismiss = { expanded = false },
                    onSelect = baud,
                )
            }
            IntRow(stringResource(R.string.miscel_period_title),s.period.toString(),stringResource(R.string.units_ms),MiscField.PERIOD){intEdit(MiscField.PERIOD)}
        }
        if (hasValidationError) Text(stringResource(R.string.misc_invalid_packet),color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("misc-validation-error"))
        ParameterSection {
            CheckRow(stringResource(R.string.miscel_ignition_cutoff_title),s.ign,"ign"){toggleAction(MiscToggle.IGN,it)}
            IntRow(stringResource(R.string.miscel_ignition_cutoff_rpm_title),s.cutoff.toString(),stringResource(R.string.units_rpm),MiscField.CUTOFF, enabled = miscFieldEnabled(MiscField.CUTOFF, s.ign)){intEdit(MiscField.CUTOFF)}
            IntRow(stringResource(R.string.miscel_start_rel_to_tdc_title),s.start.toString(),stringResource(R.string.units_degrees_cs),MiscField.START, enabled = capabilities.hallOutput){intEdit(MiscField.START)}
            IntRow(stringResource(R.string.miscel_hall_output_delay_title),s.duration.toString(),stringResource(R.string.units_degrees_cs),MiscField.DURATION, enabled = capabilities.hallOutput){intEdit(MiscField.DURATION)}
        }
        ParameterSection {
            CheckRow(stringResource(R.string.miscel_turn_off_fuel_pump_after_switching_gas_title),s.pump,"pump"){toggleAction(MiscToggle.PUMP,it)}
            CheckRow(stringResource(R.string.miscel_turn_off_injectors_pump_after_switching_gas_title),s.gas,"gas", enabled = capabilities.fuelInjection){toggleAction(MiscToggle.GAS,it)}
            CheckRow(stringResource(R.string.miscel_turn_off_injectors_pump_after_switching_petrol_title),s.petrol,"petrol", enabled = capabilities.fuelInjection){toggleAction(MiscToggle.PETROL,it)}
        }
        ParameterSection {
            IntRow(stringResource(R.string.miscel_evap_starting_air_flow_title),s.begin.toString(),"RxL",MiscField.BEGIN, enabled = capabilities.fuelInjection && !capabilities.secu3T){intEdit(MiscField.BEGIN)}
            IntRow(stringResource(R.string.miscel_evap_ending_air_flow_title),s.end.toString(),"RxL",MiscField.END, enabled = capabilities.fuelInjection && !capabilities.secu3T){intEdit(MiscField.END)}
        }
        ParameterSection {
            val timeout = String.format(locale, "%.1f", s.timeout)
            IntRow(stringResource(R.string.miscel_fuel_pump_working_time_title),timeout,stringResource(R.string.units_sec),MiscField.TIMEOUT){floatEdit()}
            IntRow(stringResource(R.string.miscel_pwmfrq1_title),s.pwm0.toString(),stringResource(R.string.units_hertz),MiscField.PWM0){intEdit(MiscField.PWM0)}
            IntRow(stringResource(R.string.miscel_pwmfrq2_title),s.pwm1.toString(),stringResource(R.string.units_hertz),MiscField.PWM1){intEdit(MiscField.PWM1)}
            IntRow(stringResource(R.string.miscel_num_vss_pulsed_per_dist_title),s.vss.toString(),"",MiscField.VSS){intEdit(MiscField.VSS)}
        }
    }
}
@Composable private fun IntRow(title:String,value:String,units:String,key:MiscField,enabled:Boolean=true,onClick:()->Unit) {
    val tag = key.name.lowercase()
    ParameterTextValueRow(title, "$value ${units}".trim(), "misc-$tag", enabled=enabled, numberText=value, units=units, onClick=onClick)
}
@Composable private fun CheckRow(title:String,value:Boolean,key:String,enabled:Boolean=true,onChange:(Boolean)->Unit) = ParameterSwitchRow(title, value, "misc-$key", enabled, onChange)
