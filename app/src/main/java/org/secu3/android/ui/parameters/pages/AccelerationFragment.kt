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
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.AccelerationNumber
import org.secu3.android.ui.parameters.pages.compose.AccelerationScreen
import org.secu3.android.ui.parameters.pages.compose.AccelerationUiState

class AccelerationFragment : BaseParamFragment() {
    private val stateHolder get() = mViewModel.accelerationStateHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            id = R.id.acceleration_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    AccelerationScreen(stateHolder.values?.let(AccelerationUiState::Data) ?: AccelerationUiState.Loading,
                        ::editParameter, stateHolder::selectAeType, stateHolder::selectWallwetModel, stateHolder::setBalance)
                }
            }
        }
    }

    private fun editParameter(number: AccelerationNumber) {
        val current = stateHolder.values ?: return
        val dialog = ParamIntEditDialogFragment.newInstance(
            current.value(number).toInt(), getString(number.titleRes), number.step.toInt(), number.max.toInt(), number.min.toInt(),
        )
        dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateNumber(number, it.toFloat()) }
        dialog.show(childFragmentManager, dialog::class.java.simpleName)
    }

    override fun tabTitle(): Int = R.string.params_tab_acceleration_title
}
