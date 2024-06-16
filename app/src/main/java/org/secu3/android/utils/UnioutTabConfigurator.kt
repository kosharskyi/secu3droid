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


    fun configureViews(condition: CONDITION, firstView: FloatParamView, secondView: FloatParamView, firstValue: Float? = null, secondValue: Float? = null) {

        when (condition) {
            CONDITION.COOLANT_TEMPER -> {
                firstView.configure(-40.0f, 180.0f,0.25f, 2, firstValue ?: 55.0f)
                secondView.configure(-40.0f, 180.0f,0.25f, 2, secondValue ?: 50.0f)
            }
            CONDITION.RPM -> {
                firstView.configure(50.0f, 2000.0f,10.0f, 0, firstValue ?: 1500.0f)
                secondView.configure(50.0f, 2000.0f,10.0f, 0, secondValue ?: 1200.0f)
            }
            CONDITION.MAP -> {
                firstView.configure(0.25f, 500.0f,0.25f, 2, firstValue ?: 95.0f)
                secondView.configure(0.25f, 500.0f,0.25f, 2, secondValue ?: 90.0f)
            }
            CONDITION.UBAT -> {
                firstView.configure(5.0f, 16.0f,0.1f, 1, firstValue ?: 10.0f)
                secondView.configure(5.0f, 16.0f,0.1f, 1, secondValue ?: 10.5f)
            }
            CONDITION.CARB -> {
                firstView.configure(0.0f, 1.0f,1.0f, 0, firstValue ?: 0.0f)
                secondView.configure(0.0f, 1.0f,1.0f, 0, secondValue ?: 1.0f)
            }
            CONDITION.VSPD -> {
                firstView.configure(5.0f, 250.0f,0.1f, 1, firstValue ?: 70.0f)
                secondView.configure(5.0f, 250.0f,0.1f, 1, secondValue ?: 65.0f)
            }
            CONDITION.AIRFL -> {
                firstView.configure(0.0f, 16.0f,1.0f, 0, firstValue ?: 13.0f)
                secondView.configure(0.0f, 16.0f,1.0f, 0, secondValue ?: 12.0f)
            }
            CONDITION.TMR, CONDITION.ITTMR, CONDITION.ESTMR -> {
                firstView.configure(0.0f, 600.0f,0.1f, 1, firstValue ?: 0.0f)
                secondView.configure(0.0f, 600.0f,0.1f, 1, secondValue ?: 5.0f)
            }
            CONDITION.CPOS -> {
                firstView.configure(0.0f, 100.0f,0.5f, 1, firstValue ?: 60.0f)
                secondView.configure(0.0f, 100.0f,0.5f, 1, secondValue ?: 55.0f)
            }
            CONDITION.AANG -> {
                firstView.configure(-15.0f, 65.0f,0.1f, 1, firstValue ?: 55.0f)
                secondView.configure(-15.0f, 65.0f,0.1f, 1, secondValue ?: 53.0f)
            }
            CONDITION.KLEV -> {
                firstView.configure(0.0f, 5.0f,0.01f, 2, firstValue ?: 2.5f)
                secondView.configure(0.0f, 5.0f,0.01f, 2, secondValue ?: 2.45f)
            }
            CONDITION.TPS -> {
                firstView.configure(0.0f, 100.0f,0.5f, 1, firstValue ?: 30.0f)
                secondView.configure(0.0f, 100.0f,0.5f, 1, secondValue ?: 29.0f)
            }
            CONDITION.ATS -> {
                firstView.configure(-40.0f, 180.0f,0.25f, 2, firstValue ?: 55.0f)
                secondView.configure(-40.0f, 180.0f,0.25f, 2, secondValue ?: 50.0f)
            }
            CONDITION.AI1, CONDITION.AI2, CONDITION.AI3, CONDITION.AI4,
            CONDITION.AI5, CONDITION.AI6, CONDITION.AI7, CONDITION.AI8 -> {
                firstView.configure(0.0f, 5.0f,0.01f, 2, firstValue ?: 2.5f)
                secondView.configure(0.0f, 5.0f,0.01f, 2, secondValue ?: 2.48f)
            }
            CONDITION.GASV -> {
                firstView.configure(0.0f, 1.0f,1.0f, 0, firstValue ?: 0.0f)
                secondView.configure(0.0f, 1.0f,1.0f, 0, secondValue ?: 1.0f)
            }
            CONDITION.IPW -> {
                firstView.configure(0.01f, 200.0f,0.01f, 2, firstValue ?: 20.0f)
                secondView.configure(0.01f, 200.0f,0.01f, 2, secondValue ?: 19.9f)
            }
            CONDITION.CE -> {
                firstView.configure(0.0f, 1.0f,1.0f, 0, firstValue ?: 0.0f)
                secondView.configure(0.0f, 1.0f,1.0f, 0, secondValue ?: 1.0f)
            }
            CONDITION.OFTMR -> {
                firstView.configure(0.0f, 600.0f,0.1f, 1, firstValue ?: 0.0f)
                secondView.configure(0.0f, 600.0f,0.1f, 1, secondValue ?: 5.0f)
            }
            CONDITION.LOOPTMR -> {
                firstView.configure(0.0f, 600.0f,0.1f, 1, firstValue ?: 1.0f)
                secondView.configure(0.0f, 600.0f,0.1f, 1, secondValue ?: 5.0f)
            }

            CONDITION.GRTS -> {
                firstView.configure(-40.0f, 300.0f,0.25f, 2, firstValue ?: 55.0f)
                secondView.configure(-40.0f, 300.0f,0.25f, 2, secondValue ?: 50.0f)
            }

            CONDITION.MAP2 -> {
                firstView.configure(0.25f, 500.0f,0.25f, 2, firstValue ?: 95.0f)
                secondView.configure(0.25f, 500.0f,0.25f, 2, secondValue ?: 90.0f)
            }

            CONDITION.TMP2 -> {
                firstView.configure(-40.0f, 300.0f,0.25f, 2, firstValue ?: 55.0f)
                firstView.configure(-40.0f, 300.0f,0.25f, 2, secondValue ?: 50.0f)
            }

            CONDITION.INPUT1 -> {
                firstView.configure(0.0f, 1.0f,1.0f, 0, firstValue ?: 0.0f)
                firstView.configure(0.0f, 1.0f,1.0f, 0, secondValue ?: 1.0f)
            }

            CONDITION.INPUT2 -> {
                firstView.configure(0.0f, 1.0f,1.0f, 0, firstValue ?: 0.0f)
                firstView.configure(0.0f, 1.0f,1.0f, 0, secondValue ?: 1.0f)
            }

            CONDITION.MAF -> {
                firstView.configure(0.25f, 650.0f,0.10f, 2, firstValue ?: 95.0f)
                secondView.configure(0.25f, 650.0f,0.10f, 2, secondValue ?: 90.0f)
            }

            CONDITION.TPSDOT -> {
                firstView.configure(0.25f, 500.0f,0.25f, 2, firstValue ?: 95.0f)
                secondView.configure(0.25f, 500.0f,0.25f, 2, secondValue ?: 90.0f)
            }

            CONDITION.GPS -> TODO()
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