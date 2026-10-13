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

import org.secu3.android.ui.compose.DropdownShape

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.withResumed
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment

class MiscellaneousFragment : BaseParamFragment() {
    private lateinit var stateHolder: MiscellaneousStateHolder

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        stateHolder = MiscellaneousStateHolder(mViewModel::sendPacket)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SecuComposeTheme { MiscellaneousScreen(stateHolder.state, stateHolder.hasValidationError, BAUD_RATE_CODES, ::editInt, ::editFloat, ::selectBaud, ::toggle) } }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewOwner = viewLifecycleOwner
        viewOwner.lifecycleScope.launch {
            viewOwner.lifecycle.withResumed {
                val liveData = mViewModel.miscellaneousLiveData
                liveData.observe(viewOwner) { p ->
                    stateHolder.onPacketReceived(p)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun selectBaud(code: Int) = stateHolder.selectBaud(code)
    private fun editInt(key: MiscField, value: Int) = stateHolder.editInt(key, value)
    private fun editFloat(value: Float) = stateHolder.editFloat(value)
    private fun toggle(key: MiscToggle, value: Boolean) = stateHolder.toggle(key, value)

    private fun openInt(key: MiscField, title: String, value: Int, min: Int, max: Int, step: Int) {
        if (min > max) {
            android.widget.Toast.makeText(requireContext(), R.string.miscel_evap_invalid_range, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        ParamIntEditDialogFragment.newInstance(value.coerceIn(min, max), title, step, max, min).also { dialog ->
            dialog.newValueLiveData.observe(viewLifecycleOwner) { editInt(key,it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }
    private fun editInt(key: MiscField) {
        val p = stateHolder.currentPacket ?: return
        val parameter = when (key) {
            MiscField.PERIOD -> Triple(R.string.miscel_period_title, p.uartPeriodTms, 0 to 500)
            MiscField.CUTOFF -> Triple(R.string.miscel_ignition_cutoff_rpm_title, p.ignCutoffThrd, 1000 to 30000)
            MiscField.START -> Triple(R.string.miscel_start_rel_to_tdc_title, p.hopStartAng, -6 to 12)
            MiscField.DURATION -> Triple(R.string.miscel_hall_output_delay_title, p.hopDuratAng, 1 to 30)
            MiscField.BEGIN -> Triple(
                R.string.miscel_evap_starting_air_flow_title,
                p.evapAfbegin,
                evapEditBounds(MiscField.BEGIN, p.evapAfbegin, p.evapAfEnd),
            )
            MiscField.END -> Triple(
                R.string.miscel_evap_ending_air_flow_title,
                p.evapAfEnd,
                evapEditBounds(MiscField.END, p.evapAfbegin, p.evapAfEnd),
            )
            MiscField.PWM0 -> Triple(R.string.miscel_pwmfrq1_title, p.pwmFrq0, 10 to 5000)
            MiscField.PWM1 -> Triple(R.string.miscel_pwmfrq2_title, p.pwmFrq1, 10 to 5000)
            MiscField.VSS -> Triple(R.string.miscel_num_vss_pulsed_per_dist_title, p.vssPeriodDist, 100 to 60000)
            MiscField.TIMEOUT -> error("Float parameter routed through integer editor")
        }
        val range = parameter.third ?: run {
            android.widget.Toast.makeText(requireContext(), R.string.miscel_evap_invalid_range, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val step = if (key == MiscField.PERIOD) 10 else 1
        openInt(key, getString(parameter.first), parameter.second, range.first, range.second, step)
    }
    private fun editFloat() {
        val p = stateHolder.currentPacket ?: return
        ParamFloatEditDialogFragment.newInstance(p.fpTimeoutStrt,getString(R.string.miscel_fuel_pump_working_time_title),.1f,25.5f,.1f,1).also { d -> d.newValueLiveData.observe(viewLifecycleOwner) { editFloat(it) }; d.show(childFragmentManager,d::class.java.simpleName) }
    }
    override fun tabTitle() = R.string.params_tab_miscellaneous_title

    companion object {
        internal val BAUD_RATE_CODES = linkedMapOf(0x0411 to "2400", 0x0208 to "4800", 0x0103 to "9600", 0x00AD to "14400", 0x0081 to "19200", 0x0056 to "28800", 0x0040 to "38400", 0x002A to "57600", 0x0015 to "115200", 0x0009 to "250000")
    }
}

internal data class MiscState(val baudDivisor:Int,val period:Int,val ign:Boolean,val cutoff:Int,val start:Int,val duration:Int,val pump:Boolean,val gas:Boolean,val petrol:Boolean,val begin:Int,val end:Int,val timeout:Float,val pwm0:Int,val pwm1:Int,val vss:Int)
internal enum class MiscToggle { IGN, PUMP, GAS, PETROL }
internal enum class MiscField { PERIOD, CUTOFF, START, DURATION, BEGIN, END, TIMEOUT, PWM0, PWM1, VSS }
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
private const val EVAP_MAX = 200000

@Composable
internal fun MiscellaneousScreen(
    s: MiscState?, hasValidationError:Boolean, rates: Map<Int,String>, intEdit: (MiscField)->Unit, floatEdit: ()->Unit,
    baud: (Int)->Unit, toggleAction:(MiscToggle,Boolean)->Unit,
) {
    if (s == null) { ParameterLoadingIndicator("misc-loading"); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Card { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.miscel_baudrate_title),style=MaterialTheme.typography.titleMedium)
            var expanded by remember { mutableStateOf(false) }
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().testTag("misc-baud-selector"), shape = DropdownShape) { Text(rates[s.baudDivisor] ?: "Code ${s.baudDivisor}"); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown,contentDescription=stringResource(R.string.misc_choose_baud)) }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}, shape = DropdownShape) { rates.forEach { (code,label) -> DropdownMenuItem(text={Text(label,Modifier.testTag("misc-baud-option-$code"))},onClick={expanded=false;baud(code)}) } }
            IntRow(stringResource(R.string.miscel_period_title),"${s.period} ${stringResource(R.string.units_ms)}",MiscField.PERIOD){intEdit(MiscField.PERIOD)}
        } }
        if (hasValidationError) Text(stringResource(R.string.misc_invalid_packet),color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("misc-validation-error"))
        Card { Column(Modifier.padding(16.dp)) {
            CheckRow(stringResource(R.string.miscel_ignition_cutoff_title),s.ign,"ign"){toggleAction(MiscToggle.IGN,it)}
            IntRow(stringResource(R.string.miscel_ignition_cutoff_rpm_title),"${s.cutoff} ${stringResource(R.string.units_rpm)}",MiscField.CUTOFF){intEdit(MiscField.CUTOFF)}
            IntRow(stringResource(R.string.miscel_start_rel_to_tdc_title),"${s.start} ${stringResource(R.string.units_degrees_cs)}",MiscField.START){intEdit(MiscField.START)}
            IntRow(stringResource(R.string.miscel_hall_output_delay_title),"${s.duration} ${stringResource(R.string.units_degrees_cs)}",MiscField.DURATION){intEdit(MiscField.DURATION)}
        } }
        Card { Column(Modifier.padding(16.dp)) {
            CheckRow(stringResource(R.string.miscel_turn_off_fuel_pump_after_switching_gas_title),s.pump,"pump"){toggleAction(MiscToggle.PUMP,it)}
            CheckRow(stringResource(R.string.miscel_turn_off_injectors_pump_after_switching_gas_title),s.gas,"gas"){toggleAction(MiscToggle.GAS,it)}
            CheckRow(stringResource(R.string.miscel_turn_off_injectors_pump_after_switching_petrol_title),s.petrol,"petrol"){toggleAction(MiscToggle.PETROL,it)}
        } }
        Card { Column(Modifier.padding(16.dp)) {
            IntRow(stringResource(R.string.miscel_evap_starting_air_flow_title),"${s.begin} RxL",MiscField.BEGIN){intEdit(MiscField.BEGIN)}
            IntRow(stringResource(R.string.miscel_evap_ending_air_flow_title),"${s.end} RxL",MiscField.END){intEdit(MiscField.END)}
        } }
        Card { Column(Modifier.padding(16.dp)) {
            IntRow(stringResource(R.string.miscel_fuel_pump_working_time_title),"%.1f %s".format(s.timeout,stringResource(R.string.units_sec)),MiscField.TIMEOUT){floatEdit()}
            IntRow(stringResource(R.string.miscel_pwmfrq1_title),"${s.pwm0} ${stringResource(R.string.units_hertz)}",MiscField.PWM0){intEdit(MiscField.PWM0)}
            IntRow(stringResource(R.string.miscel_pwmfrq2_title),"${s.pwm1} ${stringResource(R.string.units_hertz)}",MiscField.PWM1){intEdit(MiscField.PWM1)}
            IntRow(stringResource(R.string.miscel_num_vss_pulsed_per_dist_title),"${s.vss}",MiscField.VSS){intEdit(MiscField.VSS)}
        } }
    }
}
@Composable private fun IntRow(title:String,value:String,key:MiscField,onClick:()->Unit) { val tag=key.name.lowercase(); Row(Modifier.fillMaxWidth().testTag("misc-$tag").clickable(onClick=onClick).padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title); Text(value,Modifier.testTag("misc-$tag-value"),style=MaterialTheme.typography.bodySmall) }; Text("✎",fontSize=20.sp) } }
@Composable private fun CheckRow(title:String,value:Boolean,key:String,onChange:(Boolean)->Unit) { Row(Modifier.fillMaxWidth().testTag("misc-$key"),verticalAlignment=Alignment.CenterVertically) { Text(title,Modifier.weight(1f)); Switch(checked=value,onCheckedChange=onChange,modifier=Modifier.testTag("misc-$key-switch")) } }
