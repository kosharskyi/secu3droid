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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.utils.Task

class SensorsFragment : Fragment() {
    private val viewModel: SensorsViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private val gauges: LiveData<List<GaugeItem>> by lazy(LazyThreadSafetyMode.NONE) { viewModel.gaugesLiveData }
    private val indicators: LiveData<List<IndicatorItem>> by lazy(LazyThreadSafetyMode.NONE) { viewModel.indicatorLiveData }
    private val addGauges: LiveData<SensorsViewModel.ChoiceRequest<org.secu3.android.ui.sensors.models.GaugeType>?> by lazy(LazyThreadSafetyMode.NONE) { viewModel.showAddGaugeLiveData }
    private val addIndicators: LiveData<SensorsViewModel.ChoiceRequest<org.secu3.android.ui.sensors.models.IndicatorType>?> by lazy(LazyThreadSafetyMode.NONE) { viewModel.showAddIndicatorLiveData }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    val gaugeItems by gauges.observeAsState(emptyList())
                    val indicatorItems by indicators.observeAsState(emptyList())
                    val gaugeChoices by addGauges.observeAsState()
                    val indicatorChoices by addIndicators.observeAsState()
                    SensorsScreen(
                        gaugeItems, indicatorItems, viewModel.columnsCount, gaugeChoices, indicatorChoices,
                        onAddGaugeRequest = viewModel::addGaugeClick,
                        onAddIndicatorRequest = viewModel::addIndicatorClick,
                        onAddGauge = viewModel::addGauge,
                        onAddIndicator = viewModel::addIndicator,
                        onGaugeChoicesConsumed = viewModel::consumeGaugeChoices,
                        onIndicatorChoicesConsumed = viewModel::consumeIndicatorChoices,
                        onDeleteGauge = viewModel::deleteGauge,
                        onDeleteIndicator = viewModel::deleteIndicator,
                        onUpdateGauge = { item -> viewModel.updateGaugeState(item) },
                        onReorderGauges = viewModel::gaugesSwiped,
                        onReorderIndicators = viewModel::indicatorsSwiped,
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        viewModel.sendNewTask(Task.Secu3ReadSensors)
    }
}
