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
 */
package org.secu3.android.ui.parameters

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket

class ParamsPageSelectionTest {
    @Test fun selectionIsClampedWhenSavedIndexIsOutsidePageRange() {
        assertEquals(0, clampPageIndex(-1, 4))
        assertEquals(3, clampPageIndex(8, 4))
        assertEquals(0, clampPageIndex(3, 0))
        assertEquals(2, clampPageIndex(2, 4))
    }

    @Test fun baseTabsKeepTheirOrderAndFirmwareFeaturesAppendOnlyTheirPages() {
        val base = listOf(
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
        assertEquals(base, parameterPageSpecs(null).map { it.titleRes })

        val allFeatures = FirmwareInfoPacket(options = (1 shl 28) or (1 shl 29) or (1 shl 30) or (1 shl 17))
        assertEquals(
            base + listOf(
                R.string.params_tab_fuel_injection_title,
                R.string.params_tab_lambda_control_title,
                R.string.params_tab_acceleration_title,
                R.string.params_tab_gas_dose_title,
                R.string.params_tab_ltft_title,
            ),
            parameterPageSpecs(allFeatures).map { it.titleRes },
        )

        val carbAfrOnly = FirmwareInfoPacket(options = 1 shl 30)
        assertEquals(
            base + listOf(R.string.params_tab_lambda_control_title, R.string.params_tab_el_throttle_title),
            parameterPageSpecs(carbAfrOnly).map { it.titleRes },
        )

        val gasDoseOnly = FirmwareInfoPacket(options = 1 shl 29)
        assertEquals(
            base + listOf(
                R.string.params_tab_lambda_control_title,
                R.string.params_tab_acceleration_title,
                R.string.params_tab_gas_dose_title,
                R.string.params_tab_el_throttle_title,
            ),
            parameterPageSpecs(gasDoseOnly).map { it.titleRes },
        )
    }
}
