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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.CarburNumber
import org.secu3.android.ui.parameters.pages.compose.CarburScreen
import org.secu3.android.ui.parameters.pages.compose.CarburUiState
import org.secu3.android.ui.parameters.pages.compose.CarburStateHolder

class FuelCutoffkFragment : BaseParamFragment() {
    private lateinit var stateHolder: CarburStateHolder
    private var options: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        stateHolder = CarburStateHolder(
            sendPacket = mViewModel::sendPacket,
            onPacketMapping = { mViewModel.isSendAllowed = false },
            onPacketMapped = { mViewModel.isSendAllowed = true },
        )
        options = (1..6).map(Int::toString) + "no"
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    CarburScreen(stateHolder.state, options, ::editParameter, ::updateInvers, ::updateSelector)
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
                stateHolder.observePackets(owner, liveData)
            }
        }
    }

    private fun editParameter(number: CarburNumber) {
        val current = (stateHolder.state as? CarburUiState.Data)?.values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(current.numbers.getValue(number).toInt(), getString(number.title), number.step.toInt(), number.max.toInt(), number.min.toInt())
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(current.numbers.getValue(number), getString(number.title), number.step, number.max, number.min, number.precision)
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun updateInvers(checked: Boolean) {
        stateHolder.updateInvers(checked)
    }

    private fun updateSelector(ignition: Boolean, index: Int) {
        stateHolder.updateSelector(ignition, index)
    }

    override fun tabTitle(): Int = R.string.params_tab_fuel_cutoff_title
}
