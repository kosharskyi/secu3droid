/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android.utils

import org.secu3.android.models.packets.out.params.UniOutParamPacket.CONDITION
import org.secu3.android.ui.parameters.views.FloatParamView
import javax.inject.Inject

class UnioutTabConfigurator @Inject constructor() {


    fun configureViews(condition: Int, first: FloatParamView, second: FloatParamView) {

        if (condition !in 0..CONDITION.entries.size) {
            return
        }

        when (condition) {
            CONDITION.COOLANT_TEMPER.id -> {
                first.configure(-40.0f, 180.0f,0.25f, 2, 55.0f)
                second.configure(-40.0f, 180.0f,0.25f, 2, 50.0f)
            }
            CONDITION.RPM.id -> {
                first.configure(50.0f, 2000.0f,10.0f, 0, 1500.0f)
                second.configure(50.0f, 2000.0f,10.0f, 0, 1200.0f)
            }
            CONDITION.MAP.id -> {
                first.configure(0.25f, 500.0f,0.25f, 2, 95.0f)
                second.configure(0.25f, 500.0f,0.25f, 2, 90.0f)
            }
            CONDITION.UBAT.id -> {
                first.configure(5.0f, 16.0f,0.1f, 1, 10.0f)
                second.configure(5.0f, 16.0f,0.1f, 1, 10.5f)
            }
            CONDITION.CARB.id -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                second.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }
            CONDITION.VSPD.id -> {
                first.configure(5.0f, 250.0f,0.1f, 1, 70.0f)
                second.configure(5.0f, 250.0f,0.1f, 1, 65.0f)
            }
            CONDITION.AIRFL.id -> {
                first.configure(0.0f, 16.0f,1.0f, 0, 13.0f)
                second.configure(0.0f, 16.0f,1.0f, 0, 12.0f)
            }
            CONDITION.TMR.id, CONDITION.ITTMR.id, CONDITION.ESTMR.id -> {
                first.configure(0.0f, 600.0f,0.1f, 1, 0.0f)
                second.configure(0.0f, 600.0f,0.1f, 1, 5.0f)
            }
            CONDITION.CPOS.id -> {
                first.configure(0.0f, 100.0f,0.5f, 1, 60.0f)
                second.configure(0.0f, 100.0f,0.5f, 1, 55.0f)
            }
            CONDITION.AANG.id -> {
                first.configure(-15.0f, 65.0f,0.1f, 1, 55.0f)
                second.configure(-15.0f, 65.0f,0.1f, 1, 53.0f)
            }
            CONDITION.KLEV.id -> {
                first.configure(0.0f, 5.0f,0.01f, 2, 2.5f)
                second.configure(0.0f, 5.0f,0.01f, 2, 2.45f)
            }
            CONDITION.TPS.id -> {
                first.configure(0.0f, 100.0f,0.5f, 1, 30.0f)
                second.configure(0.0f, 100.0f,0.5f, 1, 29.0f)
            }
            CONDITION.ATS.id -> {
                first.configure(-40.0f, 180.0f,0.25f, 2, 55.0f)
                second.configure(-40.0f, 180.0f,0.25f, 2, 50.0f)
            }
            CONDITION.AI1.id, CONDITION.AI2.id, CONDITION.AI3.id, CONDITION.AI4.id,
            CONDITION.AI5.id, CONDITION.AI6.id, CONDITION.AI7.id, CONDITION.AI8.id -> {
                first.configure(0.0f, 5.0f,0.01f, 2, 2.5f)
                second.configure(0.0f, 5.0f,0.01f, 2, 2.48f)
            }
            CONDITION.GASV.id -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                second.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }
            CONDITION.IPW.id -> {
                first.configure(0.01f, 200.0f,0.01f, 2, 20.0f)
                second.configure(0.01f, 200.0f,0.01f, 2, 19.9f)
            }
            CONDITION.CE.id -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                second.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }
            CONDITION.OFTMR.id -> {
                first.configure(0.0f, 600.0f,0.1f, 1, 0.0f)
                second.configure(0.0f, 600.0f,0.1f, 1, 5.0f)
            }
            CONDITION.LOOPTMR.id -> {
                first.configure(0.0f, 600.0f,0.1f, 1, 1.0f)
                second.configure(0.0f, 600.0f,0.1f, 1, 5.0f)
            }

            CONDITION.GRTS.id -> {
                first.configure(-40.0f, 300.0f,0.25f, 2, 55.0f)
                second.configure(-40.0f, 300.0f,0.25f, 2, 50.0f)
            }

            CONDITION.MAP2.id -> {
                first.configure(0.25f, 500.0f,0.25f, 2, 95.0f)
                second.configure(0.25f, 500.0f,0.25f, 2, 90.0f)
            }

            CONDITION.TMP2.id -> {
                first.configure(-40.0f, 300.0f,0.25f, 2, 55.0f)
                first.configure(-40.0f, 300.0f,0.25f, 2, 50.0f)
            }

            CONDITION.INPUT1.id -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                first.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }

            CONDITION.INPUT2.id -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                first.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }

            CONDITION.MAF.id -> {
                first.configure(0.25f, 650.0f,0.10f, 2, 95.0f)
                second.configure(0.25f, 650.0f,0.10f, 2, 90.0f)
            }

            CONDITION.TPSDOT.id -> {
                first.configure(0.25f, 500.0f,0.25f, 2, 95.0f)
                second.configure(0.25f, 500.0f,0.25f, 2, 90.0f)
            }
        }
    }

    private fun FloatParamView.configure(min: Float, max: Float, stepValue: Float, precisionValue: Int, newValue: Float) {
        minValue = min
        maxValue = max
        step = stepValue
        precision = precisionValue
        value = newValue
    }
}