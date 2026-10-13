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
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import org.secu3.android.ui.parameters.pages.compose.StarterFlag
import org.secu3.android.ui.parameters.pages.compose.StarterScreen
import org.secu3.android.ui.parameters.pages.compose.StarterStateHolder
import org.secu3.android.ui.parameters.pages.compose.StarterUiState

class StarterFragment : BaseParamFragment() {

    private var starterState: StarterStateHolder? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val stateHolder = StarterStateHolder(
            sendPacket = mViewModel::sendPacket,
            onPacketLoaded = { mViewModel.isSendAllowed = true },
        )
        starterState = stateHolder
        return ComposeView(requireContext()).apply {
            id = R.id.starter_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    StarterScreen(
                        state = stateHolder.state,
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
        starterState?.observePackets(viewLifecycleOwner, starterLiveData)
    }

    private fun editParameter(number: StarterNumber) {
        val current = (starterState?.state as? StarterUiState.Data)?.values ?: return
        when (number) {
            StarterNumber.STARTER_OFF, StarterNumber.SMAP_ABANDON, StarterNumber.AFTERSTART_PETROL,
            StarterNumber.AFTERSTART_GAS, StarterNumber.STBL_COUNTDOWN -> {
                val dialog = ParamIntEditDialogFragment.newInstance(
                    current.value(number).toInt(), getString(number.titleRes), number.step.toInt(),
                    number.max.toInt(), number.min.toInt(),
                )
                dialog.newValueLiveData.observe(viewLifecycleOwner) { starterState?.updateValue(number, it.toFloat()) }
                dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
            else -> {
                val dialog = ParamFloatEditDialogFragment.newInstance(
                    current.value(number), getString(number.titleRes), number.step,
                    number.max, number.min, number.precision,
                )
                dialog.newValueLiveData.observe(viewLifecycleOwner) { starterState?.updateValue(number, it) }
                dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
        }
    }

    private fun updateFlag(flag: StarterFlag, checked: Boolean) {
        starterState?.updateFlag(flag, checked)
    }

    override fun onDestroyView() {
        starterState = null
        super.onDestroyView()
    }

    override fun tabTitle(): Int = R.string.params_tab_start_title
}
