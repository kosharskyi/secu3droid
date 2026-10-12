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
import org.secu3.android.models.packets.out.params.DbwParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.DbwNumber
import org.secu3.android.ui.parameters.pages.compose.DbwScreen
import org.secu3.android.ui.parameters.pages.compose.DbwUiState
import org.secu3.android.ui.parameters.pages.compose.DbwValues
import org.secu3.android.ui.parameters.pages.compose.applyDbwValue

class DbwFragment : BaseParamFragment() {

    private var packet: DbwParamPacket? = null
    private var values by mutableStateOf<DbwValues?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.dbw_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    DbwScreen(values?.let(DbwUiState::Data) ?: DbwUiState.Loading, ::editParameter)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = viewLifecycleOwner
        owner.lifecycleScope.launch {
            owner.withResumed {
                // Accessing this getter sends the DBW read request, so evaluate it only once here.
                val dbwLiveData = mViewModel.dbwLiveData
                dbwLiveData.observe(owner) { received ->
                    mViewModel.isSendAllowed = false
                    packet = received
                    values = DbwValues.from(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun editParameter(number: DbwNumber) {
        val current = values ?: return
        val dialog = ParamFloatEditDialogFragment.newInstance(
            current.value(number), getString(number.titleRes), number.step, number.max, number.min, number.precision,
        )
        dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it) }
        dialog.show(childFragmentManager, dialog::class.java.simpleName)
    }

    private fun updateValue(number: DbwNumber, value: Float) {
        val currentPacket = packet ?: return
        applyDbwValue(currentPacket, number, value)
        values = values?.withValue(number, value)
        mViewModel.sendPacket(currentPacket)
    }

    override fun tabTitle(): Int = R.string.params_tab_el_throttle_title
}
