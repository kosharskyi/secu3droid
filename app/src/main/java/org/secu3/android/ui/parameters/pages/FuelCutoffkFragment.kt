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
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.CarburNumber
import org.secu3.android.ui.parameters.pages.compose.CarburScreen
import org.secu3.android.ui.parameters.pages.compose.CarburUiState
import org.secu3.android.ui.parameters.pages.compose.CarburValues
import org.secu3.android.ui.parameters.pages.compose.applyCarburSelector
import org.secu3.android.ui.parameters.pages.compose.applyCarburValue

class FuelCutoffkFragment : BaseParamFragment() {
    private var packet: CarburParamPacket? = null
    private var values by mutableStateOf<CarburValues?>(null)
    private var options: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        options = (1..6).map(Int::toString) + "no"
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    CarburScreen(values?.let(CarburUiState::Data) ?: CarburUiState.Loading, options, ::editParameter, ::updateInvers, ::updateSelector)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = viewLifecycleOwner
        owner.lifecycleScope.launch {
            owner.withResumed {
                // This getter issues a protocol read; evaluate it once for this view lifecycle.
                val liveData = mViewModel.carburLiveData
                liveData.observe(owner) { received ->
                    mViewModel.isSendAllowed = false
                    packet = received
                    values = CarburValues.from(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun editParameter(number: CarburNumber) {
        val current = values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(current.numbers.getValue(number).toInt(), getString(number.title), number.step.toInt(), number.max.toInt(), number.min.toInt())
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(current.numbers.getValue(number), getString(number.title), number.step, number.max, number.min, number.precision)
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun updateValue(number: CarburNumber, value: Float) {
        val current = packet ?: return
        applyCarburValue(current, number, value)
        values = values?.withValue(number, value)
        mViewModel.sendPacket(current)
    }

    private fun updateInvers(checked: Boolean) {
        val current = packet ?: return
        current.carbInvers = if (checked) 1 else 0
        values = values?.copy(invers = checked)
        mViewModel.sendPacket(current)
    }

    private fun updateSelector(ignition: Boolean, index: Int) {
        val current = packet ?: return
        if (applyCarburSelector(current, ignition, index)) {
            values = values?.copy(fuelCutUni = current.fuelcut_uni, ignitionCutUni = current.igncut_uni)
            mViewModel.sendPacket(current)
        }
    }

    override fun tabTitle(): Int = R.string.params_tab_fuel_cutoff_title
}
