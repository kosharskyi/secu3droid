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

import org.secu3.android.ui.sensors.models.formatValue
import org.secu3.android.ui.sensors.models.defaultNumericView
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.secu3.android.db.AppDatabase
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.models.sensors.IndicatorType
import org.secu3.android.utils.AppPrefs
import javax.inject.Inject

class SensorsRepository @Inject constructor(
    private val mPrefs: AppPrefs,
    private val db: AppDatabase,
)  {

    suspend fun getStoredGaugeStates(): List<GaugeState> = withContext(Dispatchers.IO) {
        db.gaugeStateDao().getAll()
    }

    suspend fun addGauge(gaugeType: GaugeType) = withContext(Dispatchers.IO)  {
        db.withTransaction {
            val maxIdx = db.gaugeStateDao().getMaxIdx()?.inc() ?: 0
            val state = GaugeState(0, gaugeType, maxIdx, gaugeType.defaultNumericView)
            db.gaugeStateDao().insertOrIgnore(state)
        }
    }

    suspend fun deleteGauge(gaugeType: GaugeType) = withContext(Dispatchers.IO)  {
        db.gaugeStateDao().deleteGauge(gaugeType)
    }

    fun observeGaugeItems(
        packetFlow: Flow<SensorsPacket?>,
        timingModeFlow: Flow<InjectionTimingMode>,
    ): Flow<List<GaugeItem>> = combine(
        db.gaugeStateDao().observeAllOrderByIdx(),
        packetFlow,
        timingModeFlow,
    ) { states, packet, timingMode ->
        states.map { state ->
            val value = packet?.let { currentPacket ->
                val raw = state.gaugeType.getGaugeItem(currentPacket)
                val displayed = if (state.gaugeType == GaugeType.BEGIN_INJ_PHASE || state.gaugeType == GaugeType.END_INJ_PHASE) {
                    timingMode.transform(raw)
                } else raw
                state.gaugeType.formatValue(displayed)
            }.orEmpty()
            GaugeItem(state, value)
        }
    }

    suspend fun getStoredIndicatorStates(): List<IndicatorState> = withContext(Dispatchers.IO) {
        db.indicatorStateDao().getAll()
    }

    suspend fun addIndicator(indicatorType: IndicatorType) = withContext(Dispatchers.IO)  {
        db.withTransaction {
            val maxIdx = db.indicatorStateDao().getMaxIdx()?.inc() ?: 0
            val state = IndicatorState(0, indicatorType, maxIdx)
            db.indicatorStateDao().insertOrIgnore(state)
        }
    }

    suspend fun deleteIndicator(indicatorType: IndicatorType) = withContext(Dispatchers.IO)  {
        db.indicatorStateDao().deleteIndicator(indicatorType)
    }

    fun observeIndicatorItems(packetFlow: Flow<SensorsPacket?>): Flow<List<IndicatorItem>> = combine(
        db.indicatorStateDao().observeAllOrderByIdx(),
        packetFlow,
    ) { states, packet ->
        states.map { state -> IndicatorItem(state, packet?.let(state.indicatorType::isActive) ?: false) }
    }

    suspend fun updateGauges(swipedList: List<GaugeState>) = withContext(Dispatchers.IO) {
        db.gaugeStateDao().updateAll(swipedList)
    }

    suspend fun updateIndicators(swipedList: List<IndicatorState>) = withContext(Dispatchers.IO) {
        db.indicatorStateDao().updateAll(swipedList)
    }
}
