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
package org.secu3.android.ui.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

@AndroidEntryPoint
class DiagnosticsFragment : Fragment() {
    private val viewModel: DiagnosticsViewModel by navGraphViewModels(
        R.id.diagnosticsFragment,
        factoryProducer = { defaultViewModelProviderFactory },
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.diagnostics_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            // Cache these LiveData instances once per view; their getters wrap Flows.
            val connectionStatus = viewModel.connectionStatusLiveData
            val diagInput = viewModel.diagInputLiveData
            setContent {
                SecuComposeTheme {
                    DiagnosticsScreen(
                        isSecu3T = viewModel.firmwareInfo?.isSecu3T,
                        connectionStatus = connectionStatus,
                        inputLiveData = diagInput,
                        outputPacket = viewModel.outputPacket,
                        enableBlDeLiveData = viewModel.enableBlDe,
                        enableTachOLiveData = viewModel.enableTachO,
                        onNavigateUp = { findNavController().navigateUp() },
                        onAdditionalSettings = {
                            findNavController().navigate(DiagnosticsFragmentDirections.actionDiagnosticToAdditional())
                        },
                        onOutputChanged = { output, enabled ->
                            setDiagnosticOutput(viewModel.outputPacket, output, enabled)
                            viewModel.sendDiagOutPacket()
                        },
                        onChannelSelected = { index ->
                            selectDiagnosticOutputChannel(viewModel.outputPacket, index, viewModel::sendDiagOutPacket)
                        },
                        onFrequencyEdit = { value -> updateDiagnosticOutputFrequency(viewModel.outputPacket, value, viewModel::sendDiagOutPacket) },
                        onDutyEdit = { value -> updateDiagnosticOutputDuty(viewModel.outputPacket, value, viewModel::sendDiagOutPacket) },
                    )
                }
            }
        }

}
