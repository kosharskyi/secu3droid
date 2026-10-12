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
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.pages.compose.LambdaControlScreen
import org.secu3.android.ui.parameters.pages.compose.LambdaFlag
import org.secu3.android.ui.parameters.pages.compose.LambdaNumber
import org.secu3.android.ui.parameters.pages.compose.LambdaValues
import org.secu3.android.ui.parameters.pages.compose.applyLambdaChannel
import org.secu3.android.ui.parameters.pages.compose.applyLambdaFlag
import org.secu3.android.ui.parameters.pages.compose.applyLambdaNumber

class LambdaControlFragment : BaseParamFragment() {
    private var packet: LambdaParamPacket? = null
    private var values by mutableStateOf<LambdaValues?>(null)
    private val sensorTypes by lazy { resources.getStringArray(R.array.lambda_sensors_types).toList() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        values = null
        return ComposeView(requireContext()).apply {
            id = R.id.lambda_control_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    LambdaControlScreen(values, sensorTypes, mViewModel.fwInfoPacket?.isFuelInjectEnabled == true,
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
                liveData.observe(owner) { received ->
                    mViewModel.isSendAllowed = false
                    packet = received
                    values = LambdaValues.from(received)
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun editParameter(number: LambdaNumber) {
        val current = values ?: return
        if (number.integer) {
            val dialog = ParamIntEditDialogFragment.newInstance(current.value(number).toInt(), getString(number.title), number.step.toInt(), number.max.toInt(), number.min.toInt())
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it.toFloat()) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            val dialog = ParamFloatEditDialogFragment.newInstance(current.value(number), getString(number.title), number.step, number.max, number.min, number.precision)
            dialog.newValueLiveData.observe(viewLifecycleOwner) { updateValue(number, it) }
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        }
    }

    private fun updateValue(number: LambdaNumber, value: Float) {
        val current = packet ?: return
        applyLambdaNumber(current, number, value)
        val applied = if (number == LambdaNumber.MS) current.msPerStp.toFloat() else value.coerceIn(number.min, number.max)
        values = values?.withValue(number, applied)
        mViewModel.sendPacket(current)
    }

    private fun updateFlag(flag: LambdaFlag, checked: Boolean) {
        val current = packet ?: return
        applyLambdaFlag(current, flag, checked)
        values = values?.withFlag(flag, checked)
        mViewModel.sendPacket(current)
    }

    private fun updateChannel(index: Int, checked: Boolean) {
        val current = packet ?: return
        applyLambdaChannel(current, index, checked)
        values = values?.copy(channels = values!!.channels.toMutableList().also { it[index] = checked })
        mViewModel.sendPacket(current)
    }

    private fun updateSensorType(index: Int) {
        if (index !in sensorTypes.indices) return
        val current = packet ?: return
        current.senstype = index
        values = values?.copy(sensorType = index)
        mViewModel.sendPacket(current)
    }

    override fun tabTitle(): Int = R.string.params_tab_lambda_control_title
}
