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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.LambdaControlScreen
import org.secu3.android.ui.parameters.pages.compose.LambdaControlStateHolder
import org.secu3.android.ui.parameters.pages.compose.LambdaFlag
import org.secu3.android.ui.parameters.pages.compose.LambdaNumber

class LambdaControlFragment : BaseParamFragment() {
    private lateinit var stateHolder: LambdaControlStateHolder
    private val sensorTypes by lazy { resources.getStringArray(R.array.lambda_sensors_types).toList() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        stateHolder = LambdaControlStateHolder(
            sendPacket = mViewModel::sendPacket,
            onPacketMapping = { mViewModel.isSendAllowed = false },
            onPacketMapped = { mViewModel.isSendAllowed = true },
        )
        return ComposeView(requireContext()).apply {
            id = R.id.lambda_control_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    LambdaControlScreen(stateHolder.values, sensorTypes, mViewModel.fwInfoPacket?.isFuelInjectEnabled == true,
                        ::editParameter, ::updateFlag, ::updateSensorType, ::updateChannel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = viewLifecycleOwner
        owner.lifecycleScope.launch {
            owner.withResumed {
                val liveData = mViewModel.lambdaLiveData
                liveData.observe(owner, stateHolder::onPacketReceived)
            }
        }
    }

    private fun editParameter(number: LambdaNumber) {
        val current = stateHolder.values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(current.value(number).toInt(), getString(number.title), number.step.toInt(), number.max.toInt(), number.min.toInt())
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(current.value(number), getString(number.title), number.step, number.max, number.min, number.precision)
            dialog.newValueLiveData.observe(viewLifecycleOwner) { stateHolder.updateValue(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun updateFlag(flag: LambdaFlag, checked: Boolean) {
        stateHolder.updateFlag(flag, checked)
    }

    private fun updateChannel(index: Int, checked: Boolean) {
        stateHolder.updateChannel(index, checked)
    }

    private fun updateSensorType(index: Int) {
        stateHolder.updateSensorType(index, sensorTypes.size)
    }

    override fun tabTitle(): Int = R.string.params_tab_lambda_control_title
}
