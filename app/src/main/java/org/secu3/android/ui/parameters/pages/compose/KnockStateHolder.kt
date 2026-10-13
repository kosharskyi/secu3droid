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
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import org.secu3.android.models.packets.out.params.KnockParamPacket

/** Owns Knock page packet state and coordinates accepted edits with packet sends. */
class KnockStateHolder(
    private val bpfOptions: List<String>,
    private val integrationOptions: List<String>,
    private val sendPacket: (KnockParamPacket) -> Unit,
    private val onPacketMapping: () -> Unit = {},
    private val onPacketMapped: () -> Unit = {},
) {
    private var packet: KnockParamPacket? = null
    var state: KnockUiState by mutableStateOf(KnockUiState.Loading)
        private set

    /** Observes packets for this page view lifecycle. */
    fun observePackets(owner: LifecycleOwner, packets: LiveData<KnockParamPacket>) {
        packets.observe(owner, ::onPacketReceived)
    }

    fun onPacketReceived(received: KnockParamPacket) {
        onPacketMapping()
        packet = received
        state = KnockUiState.Data(
            KnockValues.from(received, bpfOptions, integrationOptions),
            enabled = received.useKnockChannel > 0,
        )
        onPacketMapped()
    }

    fun updateValue(number: KnockNumber, value: Float) {
        val currentPacket = packet ?: return
        val currentState = state as? KnockUiState.Data ?: return
        applyKnockNumber(currentPacket, number, value)
        state = KnockUiState.Data(KnockValues.from(currentPacket, bpfOptions, integrationOptions), currentState.enabled)
        sendPacket(currentPacket)
    }

    fun updateEnabled(checked: Boolean) {
        val currentPacket = packet ?: return
        if (state !is KnockUiState.Data) return
        currentPacket.useKnockChannel = if (checked) 1 else 0
        state = KnockUiState.Data(KnockValues.from(currentPacket, bpfOptions, integrationOptions), checked)
        sendPacket(currentPacket)
    }

    fun updateChannel(channel: Int, checked: Boolean) {
        val currentPacket = packet ?: return
        val currentState = state as? KnockUiState.Data ?: return
        if (channel !in 0..7) return
        currentPacket.selectKnockChanel(channel, checked)
        state = KnockUiState.Data(KnockValues.from(currentPacket, bpfOptions, integrationOptions), currentState.enabled)
        sendPacket(currentPacket)
    }

    fun updateSelector(selector: KnockSelector, index: Int) {
        val currentPacket = packet ?: return
        val currentState = state as? KnockUiState.Data ?: return
        val options = if (selector == KnockSelector.BPF) bpfOptions else integrationOptions
        if (index !in options.indices) return
        if (selector == KnockSelector.BPF) currentPacket.bpfFrequency = index else currentPacket.intTimeCost = index
        state = KnockUiState.Data(KnockValues.from(currentPacket, bpfOptions, integrationOptions), currentState.enabled)
        sendPacket(currentPacket)
    }
}
