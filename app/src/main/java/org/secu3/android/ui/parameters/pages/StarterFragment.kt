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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import org.secu3.android.ui.parameters.pages.compose.StarterFlag
import org.secu3.android.ui.parameters.pages.compose.StarterScreen
import org.secu3.android.ui.parameters.pages.compose.StarterUiState
import org.secu3.android.ui.parameters.pages.compose.StarterValues
import org.secu3.android.ui.parameters.pages.compose.applyStarterValue

class StarterFragment : BaseParamFragment() {

    private var packet: StarterParamPacket? = null
    private var values by mutableStateOf<StarterValues?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.starter_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    StarterScreen(
                        state = values?.let { StarterUiState.Data(it) } ?: StarterUiState.Loading,
                        onEdit = ::editParameter,
                        onFlagChanged = ::updateFlag,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This getter creates a command-producing Flow. Read it once per view lifecycle.
        val starterLiveData = mViewModel.starterLiveData
        starterLiveData.observe(viewLifecycleOwner) { received ->
            packet = received
            values = StarterValues.from(received)
            mViewModel.isSendAllowed = true
        }
    }

    private fun editParameter(number: StarterNumber) {
        val current = values ?: return
        when (number) {
            StarterNumber.STARTER_OFF, StarterNumber.SMAP_ABANDON, StarterNumber.AFTERSTART_PETROL,
            StarterNumber.AFTERSTART_GAS, StarterNumber.STBL_COUNTDOWN -> {
                val dialog = ParamIntEditDialogFragment.newInstance(
                    current.value(number).toInt(), getString(number.titleRes), number.step.toInt(),
                    number.max.toInt(), number.min.toInt(),
                )
                dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it.toFloat()) }
                dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
            else -> {
                val dialog = ParamFloatEditDialogFragment.newInstance(
                    current.value(number), getString(number.titleRes), number.step,
                    number.max, number.min, number.precision,
                )
                dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it) }
                dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
        }
    }

    private fun updateValue(number: StarterNumber, value: Float) {
        val currentPacket = packet ?: return
        applyStarterValue(currentPacket, number, value)
        values = values?.withValue(number, value)
        mViewModel.sendPacket(currentPacket)
    }

    private fun updateFlag(flag: StarterFlag, checked: Boolean) {
        val currentPacket = packet ?: return
        when (flag) {
            StarterFlag.ALLOW_FLOOD_CLEAR_START -> currentPacket.allowStartOnClearFlood = checked
            StarterFlag.LIMIT_CRANKING_PW -> currentPacket.limitMaxInjPwOnCranking = checked
        }
        values = values?.withFlag(flag, checked)
        mViewModel.sendPacket(currentPacket)
    }

    override fun tabTitle(): Int = R.string.params_tab_start_title
}
