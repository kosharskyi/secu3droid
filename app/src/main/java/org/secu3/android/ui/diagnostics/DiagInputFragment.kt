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
package org.secu3.android.ui.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import org.secu3.android.R
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.models.packets.out.DiagOutputPacket

class DiagInputFragment : Fragment() {
    private val viewModel: DiagnosticsViewModel by navGraphViewModels(
        R.id.diagnosticsFragment,
        factoryProducer = { defaultViewModelProviderFactory },
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        (inflater.inflate(R.layout.fragment_diag_input, container, false) as ComposeView).apply {
            val diagInputLiveData = viewModel.diagInputLiveData
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val firmware = viewModel.firmwareInfo
                val input = diagInputLiveData.observeAsState().value
                var selectedChannel by remember { mutableIntStateOf(viewModel.outputPacket.chan) }
                var frequency by remember { mutableFloatStateOf(viewModel.outputPacket.frq) }
                var duty by remember {
                    mutableFloatStateOf(if (viewModel.outputPacket.chan == 0 && viewModel.outputPacket.duty == 0f) 50f else viewModel.outputPacket.duty)
                }
                SecuComposeTheme {
                DiagnosticsInputsScreen(
                    input = input,
                    isSecu3T = firmware?.isSecu3T,
                    selectedChannel = selectedChannel,
                    frequency = frequency,
                    duty = duty,
                    frequencyTitle = getString(R.string.test_signal_frequency_hz),
                    dutyTitle = getString(R.string.test_signal_duty),
                    onChannelSelected = { index ->
                        selectedChannel = index
                        selectDiagnosticOutputChannel(viewModel.outputPacket, index, viewModel::sendDiagOutPacket)
                    },
                    onFrequencyEdit = { showFloatEditor(getString(R.string.test_signal_frequency_hz), frequency, 0.1f, 5000f, 10f, 1) { value ->
                        frequency = value
                        updateDiagnosticOutputFrequency(viewModel.outputPacket, value, viewModel::sendDiagOutPacket)
                    } },
                    onDutyEdit = { showFloatEditor(getString(R.string.test_signal_duty), duty, 0.5f, 100f, 0f, 1) { value ->
                        duty = value
                        updateDiagnosticOutputDuty(viewModel.outputPacket, value, viewModel::sendDiagOutPacket)
                    } },
                )
                }
            }
        }

    private fun showFloatEditor(title: String, value: Float, step: Float, max: Float, min: Float, precision: Int, onConfirmed: (Float) -> Unit) {
        ParamFloatEditDialogFragment.newInstance(value, title, step, max, min, precision).also { dialog ->
            dialog.newValueLiveData.observe(viewLifecycleOwner) { result -> onConfirmed(result) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }
}

internal fun selectDiagnosticOutputChannel(packet: DiagOutputPacket, channelIndex: Int, send: () -> Unit) {
    packet.chan = channelIndex
    send()
}

internal fun updateDiagnosticOutputFrequency(packet: DiagOutputPacket, value: Float, send: () -> Unit) {
    packet.frq = value
    if (packet.chan > 0) send()
}

internal fun updateDiagnosticOutputDuty(packet: DiagOutputPacket, value: Float, send: () -> Unit) {
    packet.duty = value
    if (packet.chan > 0) send()
}
