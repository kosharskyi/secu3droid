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
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment

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
                        onFrequencyEdit = { value, onConfirmed ->
                            showFloatEditor(getString(R.string.test_signal_frequency_hz), value, 0.1f, 5000f, 10f, 1) {
                                onConfirmed(it)
                                updateDiagnosticOutputFrequency(viewModel.outputPacket, it, viewModel::sendDiagOutPacket)
                            }
                        },
                        onDutyEdit = { value, onConfirmed ->
                            showFloatEditor(getString(R.string.test_signal_duty), value, 0.5f, 100f, 0f, 1) {
                                onConfirmed(it)
                                updateDiagnosticOutputDuty(viewModel.outputPacket, it, viewModel::sendDiagOutPacket)
                            }
                        },
                    )
                }
            }
        }

    private fun showFloatEditor(
        title: String,
        value: Float,
        step: Float,
        max: Float,
        min: Float,
        precision: Int,
        onConfirmed: (Float) -> Unit,
    ) {
        ParamFloatEditDialogFragment.newInstance(value, title, step, max, min, precision).also { dialog ->
            dialog.newValueLiveData.observe(viewLifecycleOwner) { result -> onConfirmed(result) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }
}
