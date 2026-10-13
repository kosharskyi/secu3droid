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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.KnockScreen
import org.secu3.android.ui.parameters.pages.compose.KnockNumber
import org.secu3.android.ui.parameters.pages.compose.KnockUiState

class KnockFragment : BaseParamFragment() {
    private val stateHolder get() = mViewModel.knockStateHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            id = R.id.knock_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    KnockScreen(
                        state = stateHolder.state,
                        onEdit = ::edit,
                        onEnable = stateHolder::updateEnabled,
                        onChannel = stateHolder::updateChannel,
                        onSelector = stateHolder::updateSelector,
                    )
                }
            }
        }
    }
    private fun edit(number: KnockNumber) {
        val currentValues = (stateHolder.state as? KnockUiState.Data)?.values ?: return
        if (number == KnockNumber.DELAY) {
            val dialog = ParamIntEditDialogFragment.newInstance(
                currentValues.value(number).toInt(),
                getString(number.title),
                number.step.toInt(),
                number.max.toInt(),
                number.min.toInt(),
            )
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it.toFloat()) }
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
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }
    override fun tabTitle() = R.string.params_tab_knock_title
}
