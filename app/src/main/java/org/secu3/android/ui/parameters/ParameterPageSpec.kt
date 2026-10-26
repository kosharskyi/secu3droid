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
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.domain.parameters.ParameterCapabilities
import javax.inject.Inject
import javax.inject.Singleton

internal data class ParameterPageSpec(val page: ParameterPage, val titleRes: Int)

/** UI owns tab labels and order; the domain snapshot decides which entries are available. */
@Singleton
class ParameterPageCatalog @Inject constructor() {
    private val orderedPages = listOf(
        ParameterPageSpec(ParameterPage.STARTER, R.string.params_tab_start_title),
        ParameterPageSpec(ParameterPage.ANGLES, R.string.params_tab_angles_title),
        ParameterPageSpec(ParameterPage.IDLING, R.string.params_tab_idling_title),
        ParameterPageSpec(ParameterPage.FUNCTIONS, R.string.params_tab_functions_title),
        ParameterPageSpec(ParameterPage.TEMPERATURE, R.string.params_tab_temperature_title),
        ParameterPageSpec(ParameterPage.FUEL_CUTOFF, R.string.params_tab_fuel_cutoff_title),
        ParameterPageSpec(ParameterPage.ADC_ERRORS, R.string.params_tab_adc_errors_corr_title),
        ParameterPageSpec(ParameterPage.CKPS, R.string.params_tab_ckps_title),
        ParameterPageSpec(ParameterPage.KNOCK, R.string.params_tab_knock_title),
        ParameterPageSpec(ParameterPage.MISCELLANEOUS, R.string.params_tab_miscellaneous_title),
        ParameterPageSpec(ParameterPage.CHOKE_CONTROL, R.string.params_tab_choke_control_title),
        ParameterPageSpec(ParameterPage.SECURITY, R.string.params_tab_security_title),
        ParameterPageSpec(ParameterPage.UNIVERSAL_OUTPUTS, R.string.params_tab_universal_outputs_title),
        ParameterPageSpec(ParameterPage.FUEL_INJECTION, R.string.params_tab_fuel_injection_title),
        ParameterPageSpec(ParameterPage.LAMBDA_CONTROL, R.string.params_tab_lambda_control_title),
        ParameterPageSpec(ParameterPage.ACCELERATION, R.string.params_tab_acceleration_title),
        ParameterPageSpec(ParameterPage.GAS_DOSE, R.string.params_tab_gas_dose_title),
        ParameterPageSpec(ParameterPage.LTFT, R.string.params_tab_ltft_title),
        ParameterPageSpec(ParameterPage.EL_THROTTLE, R.string.params_tab_el_throttle_title),
    )

    internal fun pageForTitle(titleRes: Int): ParameterPage? =
        orderedPages.firstOrNull { it.titleRes == titleRes }?.page

    internal fun pages(capabilities: ParameterCapabilities): List<ParameterPageSpec> =
        orderedPages.filter { capabilities.supports(it.page) }
}
