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
 */

package org.secu3.android.db

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.models.sensors.IndicatorType
import javax.inject.Inject

class SensorConfigurationRepository @Inject constructor(
    private val db: AppDatabase,
) {
    fun observeGaugeStates(): Flow<List<GaugeState>> = db.gaugeStateDao().observeAllOrderByIdx()

    fun observeIndicatorStates(): Flow<List<IndicatorState>> = db.indicatorStateDao().observeAllOrderByIdx()

    suspend fun getAvailableGaugeTypes(): List<GaugeType> = withContext(Dispatchers.IO) {
        val stored = db.gaugeStateDao().getAll().map { it.gaugeType }.toSet()
        GaugeType.entries.filterNot { it in stored }
    }

    suspend fun addGauge(gaugeType: GaugeType) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val maxIdx = db.gaugeStateDao().getMaxIdx()?.inc() ?: 0
            db.gaugeStateDao().insertOrIgnore(
                GaugeState(0, gaugeType, maxIdx, gaugeType == GaugeType.DISTANCE || gaugeType == GaugeType.CONSUMED_FUEL),
            )
        }
    }

    suspend fun deleteGauge(gaugeType: GaugeType) = withContext(Dispatchers.IO) {
        db.gaugeStateDao().deleteGauge(gaugeType)
    }

    suspend fun getAvailableIndicatorTypes(): List<IndicatorType> = withContext(Dispatchers.IO) {
        val stored = db.indicatorStateDao().getAll().map { it.indicatorType }.toSet()
        IndicatorType.entries.filterNot { it in stored }
    }

    suspend fun addIndicator(indicatorType: IndicatorType) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val maxIdx = db.indicatorStateDao().getMaxIdx()?.inc() ?: 0
            db.indicatorStateDao().insertOrIgnore(IndicatorState(0, indicatorType, maxIdx))
        }
    }

    suspend fun deleteIndicator(indicatorType: IndicatorType) = withContext(Dispatchers.IO) {
        db.indicatorStateDao().deleteIndicator(indicatorType)
    }

    suspend fun updateGauges(swipedList: List<GaugeState>) = withContext(Dispatchers.IO) {
        db.gaugeStateDao().updateAll(swipedList)
    }

    suspend fun updateIndicators(swipedList: List<IndicatorState>) = withContext(Dispatchers.IO) {
        db.indicatorStateDao().updateAll(swipedList)
    }
}
