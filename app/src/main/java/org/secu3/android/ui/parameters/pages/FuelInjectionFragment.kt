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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.*

class FuelInjectionFragment : BaseParamFragment() {
    private lateinit var stateHolder: FuelInjectionStateHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            stateHolder = FuelInjectionStateHolder(mViewModel::sendPacket) { mViewModel.isSendAllowed = it }
            stateHolder.reset()
            id = R.id.fuel_injection_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    val labels = InjctrParPacket.InjConfig.entries.associate { it.id to getString(it.strId) }
                    FuelInjectionScreen(stateHolder.uiState, labels, ::editParameter, stateHolder::changeConfig,
                        stateHolder::changePulses, stateHolder::changeAngleSpec, stateHolder::changeFlag)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.fwInfoLiveData.observe(viewLifecycleOwner) {
            stateHolder.updateFirmware(it.isFuelInjectEnabled, it.isSecu3T)
        }
        val liveData = mViewModel.fuelInjectionLiveData
        liveData.observe(viewLifecycleOwner) { received ->
            // The pure mapping handles each bank directly and leaves unknown config IDs intact.
            stateHolder.receive(received)
        }
    }

    private fun editParameter(number: FuelInjectionNumber) {
        val spec = stateHolder.editSpec(number) ?: return
        val onValue: (Float) -> Unit = { stateHolder.edit(number, it) }
        if (spec.integer) {
            ParamIntEditDialogFragment.newInstance(spec.value.toInt(), getString(spec.title), spec.step.toInt(), spec.max.toInt(), spec.min.toInt()).also { dialog ->
                dialog.newValueLiveData.observe(viewLifecycleOwner) { onValue(it.toFloat()) }; dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
        } else {
            ParamFloatEditDialogFragment.newInstance(spec.value, getString(spec.title), spec.step, spec.max, spec.min, spec.precision).also { dialog ->
                dialog.newValueLiveData.observe(viewLifecycleOwner) { onValue(it) }; dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
        }
    }

    override fun tabTitle(): Int = R.string.params_tab_fuel_injection_title
}
