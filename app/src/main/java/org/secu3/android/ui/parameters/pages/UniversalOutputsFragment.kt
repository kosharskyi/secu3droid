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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme

@AndroidEntryPoint
class UniversalOutputsFragment : BaseParamFragment() {
    private lateinit var stateHolder: UniversalOutputsStateHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        stateHolder = UniversalOutputsStateHolder(mViewModel::sendPacket) { mViewModel.isSendAllowed = it }
        stateHolder.reset()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    val conditionNames = UniOutParamPacket.CONDITION.entries.associate { it.id to getString(it.strId) }
                    UniversalOutputsScreen(
                        values = stateHolder.values,
                        conditionNames = conditionNames,
                        logicalFunctions = resources.getStringArray(R.array.logical_functions).toList(),
                        onOutputChange = stateHolder::updateOutput,
                        onPairChange = stateHolder::updatePairFunction,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.withResumed {
                mViewModel.uniOutLiveData.observe(viewLifecycleOwner) { received ->
                    stateHolder.onPacketReceived(received)
                }
            }
        }
    }

    override fun tabTitle(): Int = R.string.params_tab_universal_outputs_title
}
