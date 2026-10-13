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
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.ChokeNumber
import org.secu3.android.ui.parameters.pages.compose.ChokeScreen
import org.secu3.android.ui.parameters.pages.compose.ChokeControlStateHolder
import org.secu3.android.ui.parameters.pages.compose.ChokeSelector
import org.secu3.android.ui.parameters.pages.compose.ChokeValues

class ChokeControlFragment : BaseParamFragment() {
    private var values by mutableStateOf<ChokeValues?>(null)
    private lateinit var stateHolder: ChokeControlStateHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        values = null
        stateHolder = ChokeControlStateHolder(
            onValuesChanged = { values = it },
            onSend = mViewModel::sendPacket,
        )
        return ComposeView(requireContext()).apply {
            id = R.id.choke_control_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    ChokeScreen(values, ::edit, stateHolder::toggle, stateHolder::selectFrequency,
                        stateHolder::manualStep, stateHolder::flushManual, stateHolder::finishManual, stateHolder::cancelManual, stateHolder::toggleTest)
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
                    stateHolder.receive(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun edit(number: ChokeNumber) {
        val v = values ?: return
        if (number == ChokeNumber.STEPS) {
            val dialog = ParamIntEditDialogFragment.newInstance(v.value(number).toInt(), getString(number.title), 1, number.max.toInt(), number.min.toInt())
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.edit(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(v.value(number), getString(number.title), number.step, number.max, number.min, number.precision)
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.edit(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    override fun tabTitle(): Int = R.string.params_tab_choke_control_title
}
