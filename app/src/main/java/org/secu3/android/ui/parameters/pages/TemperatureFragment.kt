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
import org.secu3.android.ui.parameters.pages.compose.TemperatureFlag
import org.secu3.android.ui.parameters.pages.compose.TemperatureNumber
import org.secu3.android.ui.parameters.pages.compose.TemperatureScreen
import org.secu3.android.ui.parameters.pages.compose.TemperatureUiState
import org.secu3.android.ui.parameters.pages.compose.TemperatureStateHolder

class TemperatureFragment : BaseParamFragment() {
    private lateinit var stateHolder: TemperatureStateHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        stateHolder = TemperatureStateHolder(
            sendPacket = mViewModel::sendPacket,
            onPacketMapping = { mViewModel.isSendAllowed = false },
            onPacketMapped = { mViewModel.isSendAllowed = true },
        )
        return ComposeView(requireContext()).apply {
            id = R.id.temperature_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    TemperatureScreen(
                        stateHolder.state,
                        ::editParameter,
                        ::updateFlag,
                    )
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
                val liveData = mViewModel.temperatureLiveData
                stateHolder.observePackets(owner, liveData)
            }
        }
    }

    private fun editParameter(number: TemperatureNumber) {
        val current = (stateHolder.state as? TemperatureUiState.Data)?.values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(
                current.value(number).toInt(), getString(number.titleRes), number.step.toInt(),
                number.max.toInt(), number.min.toInt(),
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(
                current.value(number), getString(number.titleRes), number.step,
                number.max, number.min, number.precision,
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun updateFlag(flag: TemperatureFlag, checked: Boolean) {
        stateHolder.updateFlag(flag, checked)
    }

    override fun tabTitle(): Int = R.string.params_tab_temperature_title
}
