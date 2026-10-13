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
package org.secu3.android.ui.parameters

import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket

internal data class ParameterPageSpec(val titleRes: Int)

/** Page order and firmware visibility rules for the parameter pager. */
internal fun parameterPageSpecs(fwInfoPacket: FirmwareInfoPacket?): List<ParameterPageSpec> {
    val pages = mutableListOf(
        R.string.params_tab_start_title,
        R.string.params_tab_angles_title,
        R.string.params_tab_idling_title,
        R.string.params_tab_functions_title,
        R.string.params_tab_temperature_title,
        R.string.params_tab_fuel_cutoff_title,
        R.string.params_tab_adc_errors_corr_title,
        R.string.params_tab_ckps_title,
        R.string.params_tab_knock_title,
        R.string.params_tab_miscellaneous_title,
        R.string.params_tab_choke_control_title,
        R.string.params_tab_security_title,
        R.string.params_tab_universal_outputs_title,
    )
    fwInfoPacket?.let { firmware ->
        if (firmware.isFuelInjectEnabled) pages += R.string.params_tab_fuel_injection_title
        if (firmware.isFuelInjectEnabled || firmware.isCarbAfrEnabled || firmware.isGdControlEnabled) pages += R.string.params_tab_lambda_control_title
        if (firmware.isFuelInjectEnabled || firmware.isGdControlEnabled) pages += R.string.params_tab_acceleration_title
        if (firmware.isGdControlEnabled) pages += R.string.params_tab_gas_dose_title
        if (firmware.isFuelInjectEnabled) pages += R.string.params_tab_ltft_title
        if (!firmware.isSecu3T) pages += R.string.params_tab_el_throttle_title
    }
    return pages.map(::ParameterPageSpec)
}
