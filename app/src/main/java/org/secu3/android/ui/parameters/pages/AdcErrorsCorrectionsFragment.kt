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
import org.secu3.android.ui.parameters.pages.compose.AdcCorrectionsStateHolder
import org.secu3.android.ui.parameters.pages.compose.AdcCorrectionsUiState
import org.secu3.android.ui.parameters.pages.compose.AdcErrorsCorrectionsScreen
import org.secu3.android.ui.parameters.pages.compose.AdcNumber

class AdcErrorsCorrectionsFragment : BaseParamFragment() {
    private lateinit var stateHolder: AdcCorrectionsStateHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        stateHolder = AdcCorrectionsStateHolder(mViewModel::sendPacket, { mViewModel.isSendAllowed = false }, { mViewModel.isSendAllowed = true })
        return ComposeView(requireContext()).apply {
            id = R.id.adc_errors_corrections_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    AdcErrorsCorrectionsScreen(
                        stateHolder.state,
                        ::editParameter,
                        ::updateCompMode,
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
                // Accessing the getter sends a device read, so obtain this LiveData only once.
                val liveData = mViewModel.adcCorrectionsLiveData
                liveData.observe(owner) { received ->
                    stateHolder.onPacketReceived(received)
                }
            }
        }
    }

    private fun editParameter(number: AdcNumber) {
        val current = (stateHolder.state as? AdcCorrectionsUiState.Data)?.values ?: return
        val dialog = ParamFloatEditDialogFragment.newInstance(
            current.value(number), number.title, number.step, number.max, number.min, number.precision,
        )
        dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it) }
        dialog.show(childFragmentManager, dialog::class.java.simpleName)
    }

    private fun updateCompMode(enabled: Boolean) = stateHolder.updateCompMode(enabled)

    override fun tabTitle(): Int = R.string.params_tab_adc_errors_corr_title
}
