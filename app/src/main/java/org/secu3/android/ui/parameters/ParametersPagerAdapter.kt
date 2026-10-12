/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.ui.parameters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.ui.parameters.pages.*

internal data class ParameterPageSpec(
    val titleRes: Int,
    val create: () -> BaseParamFragment,
)

/** Single source of truth for pager pages, their order, and firmware gates. */
internal fun parameterPageSpecs(fwInfoPacket: FirmwareInfoPacket?): List<ParameterPageSpec> {
    val pages = mutableListOf(
        ParameterPageSpec(R.string.params_tab_start_title, ::StarterFragment),
        ParameterPageSpec(R.string.params_tab_angles_title, ::AnglesFragment),
        ParameterPageSpec(R.string.params_tab_idling_title, ::IdlingFragment),
        ParameterPageSpec(R.string.params_tab_functions_title, ::FunctionsFragment),
        ParameterPageSpec(R.string.params_tab_temperature_title, ::TemperatureFragment),
        ParameterPageSpec(R.string.params_tab_fuel_cutoff_title, ::FuelCutoffkFragment),
        ParameterPageSpec(R.string.params_tab_adc_errors_corr_title, ::AdcErrorsCorrectionsFragment),
        ParameterPageSpec(R.string.params_tab_ckps_title, ::CkpsFragment),
        ParameterPageSpec(R.string.params_tab_knock_title, ::KnockFragment),
        ParameterPageSpec(R.string.params_tab_miscellaneous_title, ::MiscellaneousFragment),
        ParameterPageSpec(R.string.params_tab_choke_control_title, ::ChokeControlFragment),
        ParameterPageSpec(R.string.params_tab_security_title, ::SecurityFragment),
        ParameterPageSpec(R.string.params_tab_universal_outputs_title, ::UniversalOutputsFragment),
    )

    fwInfoPacket?.let { firmware ->
        if (firmware.isFuelInjectEnabled) {
            pages += ParameterPageSpec(R.string.params_tab_fuel_injection_title, ::FuelInjectionFragment)
        }
        if (firmware.isFuelInjectEnabled || firmware.isCarbAfrEnabled || firmware.isGdControlEnabled) {
            pages += ParameterPageSpec(R.string.params_tab_lambda_control_title, ::LambdaControlFragment)
        }
        if (firmware.isFuelInjectEnabled || firmware.isGdControlEnabled) {
            pages += ParameterPageSpec(R.string.params_tab_acceleration_title, ::AccelerationFragment)
        }
        if (firmware.isGdControlEnabled) {
            pages += ParameterPageSpec(R.string.params_tab_gas_dose_title, ::GasDoseFragment)
        }
        if (firmware.isFuelInjectEnabled) {
            pages += ParameterPageSpec(R.string.params_tab_ltft_title, ::LtftFragment)
        }
        if (!firmware.isSecu3T) {
            pages += ParameterPageSpec(R.string.params_tab_el_throttle_title, ::DbwFragment)
        }
    }
    return pages
}

class ParametersPagerAdapter(fragment: Fragment, fwInfoPacket: FirmwareInfoPacket?) : FragmentStateAdapter(fragment) {
    private val pageSpecs = parameterPageSpecs(fwInfoPacket)
    private val pages = pageSpecs.map { it.create() }

    val titles: List<Int>
        get() = pageSpecs.map { it.titleRes }

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment = pages[position]
}
