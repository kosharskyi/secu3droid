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

package org.secu3.android.ui.sensors

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LiveData
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.utils.Task

class RawSensorsFragment : Fragment() {

    private val mViewModel: SensorsViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val rawSensorsLiveData: LiveData<AdcRawDatPacket> by lazy(LazyThreadSafetyMode.NONE) {
        mViewModel.rawSensorsLiveData
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    val packet by rawSensorsLiveData.observeAsState<AdcRawDatPacket?, AdcRawDatPacket>(null)
                    RawSensorsScreen(packet)
                }
            }
        }

    override fun onResume() {
        super.onResume()
        mViewModel.sendNewTask(Task.Secu3ReadRawSensors)
    }
}
