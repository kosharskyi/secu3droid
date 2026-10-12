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
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.LtftNumber
import org.secu3.android.ui.parameters.pages.compose.LtftScreen
import org.secu3.android.ui.parameters.pages.compose.LtftUiState
import org.secu3.android.ui.parameters.pages.compose.LtftValues
import org.secu3.android.ui.parameters.pages.compose.applyLtftValue

class LtftFragment : BaseParamFragment() {
    private var packet: LtftParamPacket? = null
    private var values by mutableStateOf<LtftValues?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.ltft_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    LtftScreen(values?.let(LtftUiState::Data) ?: LtftUiState.Loading, ::editParameter, ::selectMode)
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
                val ltftLiveData = mViewModel.ltftLiveData
                ltftLiveData.observe(owner) { received ->
                    mViewModel.isSendAllowed = false
                    packet = received
                    values = LtftValues.from(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun editParameter(number: LtftNumber) {
        val current = values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(
                current.value(number).toInt(), getString(number.titleRes), number.step.toInt(),
                number.max.toInt(), number.min.toInt(),
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(
                current.value(number), getString(number.titleRes), number.step,
                number.max, number.min, number.precision,
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun updateValue(number: LtftNumber, value: Float) {
        val currentPacket = packet ?: return
        applyLtftValue(currentPacket, number, value)
        values = values?.withValue(number, value)
        mViewModel.sendPacket(currentPacket)
    }

    private fun selectMode(index: Int) {
        val currentPacket = packet ?: return
        if (index !in 0..3) return
        currentPacket.mode = index
        values = values?.copy(mode = index)
        mViewModel.sendPacket(currentPacket)
    }

    override fun tabTitle(): Int = R.string.params_tab_ltft_title
}
