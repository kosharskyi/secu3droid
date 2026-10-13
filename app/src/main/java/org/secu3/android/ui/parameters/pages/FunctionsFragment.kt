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
import androidx.lifecycle.Observer
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.FunctionsNumber
import org.secu3.android.ui.parameters.pages.compose.FunctionsScreen
import org.secu3.android.ui.parameters.pages.compose.FunctionsUiState
import org.secu3.android.ui.parameters.pages.compose.FunctionsStateHolder

class FunctionsFragment : BaseParamFragment() {
    private val stateHolder by lazy { FunctionsStateHolder(mViewModel::sendPacket) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        stateHolder.reset()
        return ComposeView(requireContext()).apply {
            id = R.id.functions_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    FunctionsScreen(stateHolder.state, ::editParameter, stateHolder::updateFlag, stateHolder::updateChoice)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = viewLifecycleOwner
        owner.lifecycleScope.launch {
            owner.withResumed {
                val fnNamesLiveData = mViewModel.fnNameLiveData
                val fnNamesObserver = object : Observer<FnNameDatPacket> {
                    override fun onChanged(value: FnNameDatPacket) {
                        fnNamesLiveData.removeObserver(this)
                        mViewModel.isSendAllowed = false
                        stateHolder.onNamesReceived(value)
                        mViewModel.isSendAllowed = true

                        // The LiveData getter starts the next protocol read. Evaluate it once,
                        // only after all function table names have arrived.
                        val funsetLiveData = mViewModel.funsetLiveData
                        funsetLiveData.observe(owner) { receivedSet ->
                            mViewModel.isSendAllowed = false
                            stateHolder.onPacketReceived(
                                received = receivedSet,
                                loads = resources.getStringArray(R.array.load_measurement_items).toList(),
                                bars = resources.getStringArray(R.array.barocorr_items).toList(),
                                ve2 = resources.getStringArray(R.array.ve2_map_func_items).toList(),
                            )
                            mViewModel.isSendAllowed = true
                        }
                    }
                }
                fnNamesLiveData.observe(owner, fnNamesObserver)
            }
        }
    }

    private fun editParameter(number: FunctionsNumber) {
        val current = (stateHolder.state as? FunctionsUiState.Data)?.values ?: return
        val dialog = ParamFloatEditDialogFragment.newInstance(
            current.value(number),
            getString(number.titleRes),
            number.step,
            number.max,
            number.min,
            number.precision,
        )
        val observer = object : Observer<Float> {
            override fun onChanged(value: Float) {
                dialog.newValueLiveData.removeObserver(this)
                stateHolder.updateValue(number, value)
            }
        }
        dialog.newValueLiveData.observe(viewLifecycleOwner, observer)
        dialog.show(childFragmentManager, dialog::class.java.simpleName)
    }

    override fun tabTitle(): Int = R.string.params_tab_functions_title
}
