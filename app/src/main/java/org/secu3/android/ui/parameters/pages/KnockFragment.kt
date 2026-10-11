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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.KnockNumber
import org.secu3.android.ui.parameters.pages.compose.KnockScreen
import org.secu3.android.ui.parameters.pages.compose.KnockSelector
import org.secu3.android.ui.parameters.pages.compose.KnockUiState
import org.secu3.android.ui.parameters.pages.compose.KnockValues
import org.secu3.android.ui.parameters.pages.compose.applyKnockNumber

class KnockFragment : BaseParamFragment() {
    private var packet: KnockParamPacket? = null
    private var values by mutableStateOf<KnockValues?>(null)
    private var enabled by mutableStateOf(false)
    private val bpf by lazy { resources.getStringArray(R.array.knock_filter_frequencies).toList() }
    private val integration by lazy { resources.getStringArray(R.array.knock_integration_timeouts).toList() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.knock_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    KnockScreen(
                        state = values?.let { KnockUiState.Data(it, enabled) } ?: KnockUiState.Loading,
                        onEdit = ::edit,
                        onEnable = ::updateEnabled,
                        onChannel = ::setChannel,
                        onSelector = ::setSelector,
                    )
                }
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            withResumed {
                val liveData = mViewModel.knockLiveData
                liveData.observe(viewLifecycleOwner) { received ->
                    packet = received
                    enabled = received.useKnockChannel > 0
                    values = KnockValues.from(received, bpf, integration)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun edit(number: KnockNumber) {
        val currentValues = values ?: return
        if (number == KnockNumber.DELAY) {
            val dialog = ParamIntEditDialogFragment.newInstance(
                currentValues.value(number).toInt(),
                getString(number.title),
                number.step.toInt(),
                number.max.toInt(),
                number.min.toInt(),
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { update(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(
                currentValues.value(number),
                getString(number.title),
                number.step,
                number.max,
                number.min,
                number.precision,
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { update(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }
    private fun update(number: KnockNumber, value: Float) {
        val currentPacket = packet ?: return
        applyKnockNumber(currentPacket, number, value)
        values = KnockValues.from(currentPacket, bpf, integration)
        mViewModel.sendPacket(currentPacket)
    }
    private fun updateEnabled(checked: Boolean) {
        val currentPacket = packet ?: return
        currentPacket.useKnockChannel = if (checked) 1 else 0
        enabled = checked
        mViewModel.sendPacket(currentPacket)
    }

    private fun setChannel(channel: Int, checked: Boolean) {
        val currentPacket = packet ?: return
        currentPacket.selectKnockChanel(channel, checked)
        values = KnockValues.from(currentPacket, bpf, integration)
        mViewModel.sendPacket(currentPacket)
    }

    private fun setSelector(selector: KnockSelector, index: Int) {
        val currentPacket = packet ?: return
        if (selector == KnockSelector.BPF) {
            currentPacket.bpfFrequency = index
        } else {
            currentPacket.intTimeCost = index
        }
        values = KnockValues.from(currentPacket, bpf, integration)
        mViewModel.sendPacket(currentPacket)
    }

    override fun tabTitle() = R.string.params_tab_knock_title
}
