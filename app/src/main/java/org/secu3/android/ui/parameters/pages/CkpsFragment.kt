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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.CkpsFlag
import org.secu3.android.ui.parameters.pages.compose.CkpsNumber
import org.secu3.android.ui.parameters.pages.compose.CkpsScreen
import org.secu3.android.ui.parameters.pages.compose.CkpsUiState
import org.secu3.android.ui.parameters.pages.compose.CkpsValues
import org.secu3.android.ui.parameters.pages.compose.applyCkpsChoice
import org.secu3.android.ui.parameters.pages.compose.applyCkpsFlag
import org.secu3.android.ui.parameters.pages.compose.applyCkpsNumber

class CkpsFragment : BaseParamFragment() {
    private var packet: CkpsParamPacket? = null
    private var values by mutableStateOf<CkpsValues?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.ckps_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    val screenState = values?.let(CkpsUiState::Data) ?: CkpsUiState.Loading
                    CkpsScreen(screenState, ::edit, ::flag, ::choice)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = viewLifecycleOwner
        owner.lifecycleScope.launch {
            owner.withResumed {
                val live = mViewModel.ckpsLiveData
                live.observe(owner, object : Observer<CkpsParamPacket> {
                    override fun onChanged(value: CkpsParamPacket) {
                        live.removeObserver(this)
                        mViewModel.isSendAllowed = false
                        packet = value
                        values = CkpsValues.from(value)
                        mViewModel.isSendAllowed = true
                    }
                })
            }
        }
    }

    private fun edit(number: CkpsNumber) {
        val current = values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(
                current.numbers.getValue(number).toInt(),
                getString(number.title),
                number.step.toInt(),
                number.max.toInt(),
                number.min.toInt(),
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateNumber(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(
                current.numbers.getValue(number),
                getString(number.title),
                number.step,
                number.max,
                number.min,
                number.precision,
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateNumber(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun updateNumber(number: CkpsNumber, value: Float) {
        val current = packet ?: return
        applyCkpsNumber(current, number, value)
        val updated = CkpsValues.from(current)
        values = updated
        mViewModel.sendPacket(current)
    }

    private fun flag(flag: CkpsFlag, value: Boolean) {
        val current = packet ?: return
        applyCkpsFlag(current, flag, value)
        values = CkpsValues.from(current)
        mViewModel.sendPacket(current)
    }

    private fun choice(teeth: Boolean, value: Int) {
        val current = packet ?: return
        if (applyCkpsChoice(current, teeth, value)) {
            values = CkpsValues.from(current)
            mViewModel.sendPacket(current)
        }
    }
    override fun tabTitle(): Int = R.string.params_tab_ckps_title
}
