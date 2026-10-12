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
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.AdcCorrectionsUiState
import org.secu3.android.ui.parameters.pages.compose.AdcCorrectionsValues
import org.secu3.android.ui.parameters.pages.compose.AdcErrorsCorrectionsScreen
import org.secu3.android.ui.parameters.pages.compose.AdcNumber
import org.secu3.android.ui.parameters.pages.compose.setAdcValue

class AdcErrorsCorrectionsFragment : BaseParamFragment() {
    private var packet: AdcCorrectionsParamPacket? = null
    private var values by mutableStateOf<AdcCorrectionsValues?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.adc_errors_corrections_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    AdcErrorsCorrectionsScreen(
                        values?.let(AdcCorrectionsUiState::Data) ?: AdcCorrectionsUiState.Loading,
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
                    mViewModel.isSendAllowed = false
                    packet = received
                    values = AdcCorrectionsValues.from(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun editParameter(number: AdcNumber) {
        val current = values ?: return
        val dialog = ParamFloatEditDialogFragment.newInstance(
            current.value(number), number.title, number.step, number.max, number.min, number.precision,
        )
        dialog.newValueLiveData.observe(viewLifecycleOwner) { newValue ->
            val currentPacket = packet ?: return@observe
            currentPacket.setAdcValue(number, newValue)
            values = values?.withValue(number, newValue)
            mViewModel.sendPacket(currentPacket)
        }
        dialog.show(childFragmentManager, dialog::class.java.simpleName)
    }

    private fun updateCompMode(enabled: Boolean) {
        val currentPacket = packet ?: return
        currentPacket.adcCompMode = enabled
        values = values?.withCompMode(enabled)
        mViewModel.sendPacket(currentPacket)
    }

    override fun tabTitle(): Int = R.string.params_tab_adc_errors_corr_title
}
