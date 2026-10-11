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
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.GasDoseManualDelta
import org.secu3.android.ui.parameters.pages.compose.GasDoseNumber
import org.secu3.android.ui.parameters.pages.compose.GasDoseScreen
import org.secu3.android.ui.parameters.pages.compose.GasDoseValues
import org.secu3.android.ui.parameters.pages.compose.applyGasDoseNumber

class GasDoseFragment : BaseParamFragment() {
    private var packet: GasDoseParamPacket? = null
    private var values by mutableStateOf<GasDoseValues?>(null)
    private var firmwareAvailable by mutableStateOf(true)
    private val manualDelta = GasDoseManualDelta()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        manualDelta.clear()
        firmwareAvailable = mViewModel.fwInfoPacket?.isGdControlEnabled ?: true
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    GasDoseScreen(values, firmwareAvailable, ::edit, ::selectFrequency, ::setMaximumFrequency, ::manualStep, ::finishManual, ::cancelManual, ::toggleTest)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.fwInfoLiveData.observe(viewLifecycleOwner) { firmwareAvailable = it.isGdControlEnabled }
        val owner = viewLifecycleOwner
        owner.lifecycleScope.launch {
            owner.withResumed {
                mViewModel.gasDoseLiveData.observe(owner) { received ->
                    mViewModel.isSendAllowed = false
                    manualDelta.clear()
                    received.manualPositionD = 0
                    packet = received
                    values = GasDoseValues.from(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun edit(number: GasDoseNumber) {
        val current = values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(current.value(number).toInt(), getString(number.title), number.step.toInt(), number.max.toInt(), number.min.toInt())
            dialog.newValueLiveData.observe(viewLifecycleOwner) { update(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(current.value(number), getString(number.title), number.step, number.max, number.min, number.precision)
            dialog.newValueLiveData.observe(viewLifecycleOwner) { update(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun update(number: GasDoseNumber, value: Float) {
        val p = packet ?: return
        applyGasDoseNumber(p, number, value)
        values = GasDoseValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    private fun selectFrequency(index: Int) {
        if (index !in 0..3) return
        val p = packet ?: return
        p.freq = index
        values = GasDoseValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    private fun setMaximumFrequency(enabled: Boolean) {
        val p = packet ?: return
        p.maxFreqInit = if (enabled) 1 else 0
        values = GasDoseValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    private fun manualStep(direction: Int) { if (packet != null) manualDelta.step(direction) }
    private fun finishManual() {
        val p = packet ?: return
        if (manualDelta.value != 0) mViewModel.sendPacket(p.snapshotWithManualPosition(manualDelta.value))
        p.manualPositionD = 0
        manualDelta.clear()
    }
    private fun cancelManual() { packet?.manualPositionD = 0; manualDelta.clear() }
    private fun toggleTest() {
        val p = packet ?: return
        p.testing = !p.testing
        values = GasDoseValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    override fun tabTitle(): Int = R.string.params_tab_gas_dose_title
}
