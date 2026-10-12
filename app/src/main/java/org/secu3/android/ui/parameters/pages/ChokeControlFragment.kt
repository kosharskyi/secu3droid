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
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.ChokeNumber
import org.secu3.android.ui.parameters.pages.compose.ChokeManualDelta
import org.secu3.android.ui.parameters.pages.compose.ChokeScreen
import org.secu3.android.ui.parameters.pages.compose.ChokeSelector
import org.secu3.android.ui.parameters.pages.compose.ChokeValues
import org.secu3.android.ui.parameters.pages.compose.applyChokeNumber

class ChokeControlFragment : BaseParamFragment() {
    private var packet: ChokeControlParPacket? = null
    private var values by mutableStateOf<ChokeValues?>(null)
    private val manualDelta = ChokeManualDelta()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.choke_control_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    ChokeScreen(values, ::edit, ::toggle, ::selectFrequency, ::manualStep, ::finishManual, ::cancelManual, ::toggleTest)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            withResumed {
                mViewModel.chokeLiveData.observe(viewLifecycleOwner) { received ->
                    mViewModel.isSendAllowed = false
                    manualDelta.clear()
                    received.manualPositionD = 0
                    packet = received
                    values = ChokeValues.from(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun edit(number: ChokeNumber) {
        val v = values ?: return
        if (number == ChokeNumber.STEPS) {
            val dialog = ParamIntEditDialogFragment.newInstance(v.value(number).toInt(), getString(number.title), 1, number.max.toInt(), number.min.toInt())
            dialog.newValueLiveData.observe(viewLifecycleOwner) { update(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(v.value(number), getString(number.title), number.step, number.max, number.min, number.precision)
            dialog.newValueLiveData.observe(viewLifecycleOwner) { update(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun update(number: ChokeNumber, value: Float) {
        val p = packet ?: return
        applyChokeNumber(p, number, value)
        values = ChokeValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    private fun toggle(number: ChokeNumber, checked: Boolean) {
        val p = packet ?: return
        when (number) {
            ChokeNumber.CLOSED_LOOP -> p.useClosedLoopRmpRegulator = checked
            ChokeNumber.DISABLE_ON_GAS -> p.dontUseRpmRegOnGas = checked
            ChokeNumber.THROTTLE_INIT -> p.useThrottlePosInChokeInit = checked
            ChokeNumber.MAX_FREQUENCY_INIT -> p.maxSTEPfreqAtInit = checked
            else -> return
        }
        values = ChokeValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    private fun selectFrequency(index: Int) {
        if (index !in 0..3) return
        val p = packet ?: return
        p.smFreq = index
        values = ChokeValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    private fun manualStep(direction: Int) {
        if (packet != null) manualDelta.step(direction)
    }

    private fun finishManual() {
        val p = packet ?: return
        if (manualDelta.value != 0) {
            mViewModel.sendPacket(p.snapshotWithManualPosition(manualDelta.value))
        }
        p.manualPositionD = 0
        manualDelta.clear()
    }

    private fun cancelManual() {
        packet?.manualPositionD = 0
        manualDelta.clear()
    }

    private fun toggleTest() {
        val p = packet ?: return
        p.testing = !p.testing
        values = ChokeValues.from(p)
        mViewModel.sendPacket(p.snapshotWithManualPosition(0))
    }

    override fun tabTitle(): Int = R.string.params_tab_choke_control_title
}
