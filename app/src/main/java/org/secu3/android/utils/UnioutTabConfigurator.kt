/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2021 Vitaliy O. Kosharskiy. Ukraine, Kharkiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv
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

import org.secu3.android.ui.parameters.views.FloatParamView
import javax.inject.Inject

class UnioutTabConfigurator @Inject constructor() {


    fun configureViews(condition: Int, first: FloatParamView, second: FloatParamView) {

        if (condition !in 0..UNIOUT_COND_AI8) {
            return
        }

        when (condition) {
            UNIOUT_COND_CTS -> {
                first.configure(-40.0f, 180.0f,0.25f, 2, 55.0f)
                second.configure(-40.0f, 180.0f,0.25f, 2, 50.0f)
            }
            UNIOUT_COND_RPM -> {
                first.configure(50.0f, 2000.0f,10.0f, 0, 1500.0f)
                second.configure(50.0f, 2000.0f,10.0f, 0, 1200.0f)
            }
            UNIOUT_COND_MAP -> {
                first.configure(0.25f, 500.0f,0.25f, 2, 95.0f)
                second.configure(0.25f, 500.0f,0.25f, 2, 90.0f)
            }
            UNIOUT_COND_UBAT -> {
                first.configure(5.0f, 16.0f,0.1f, 1, 10.0f)
                second.configure(5.0f, 16.0f,0.1f, 1, 10.5f)
            }
            UNIOUT_COND_CARB -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                second.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }
            UNIOUT_COND_VSPD -> {
                first.configure(5.0f, 250.0f,0.1f, 1, 70.0f)
                second.configure(5.0f, 250.0f,0.1f, 1, 65.0f)
            }
            UNIOUT_COND_AIRFL -> {
                first.configure(0.0f, 16.0f,1.0f, 0, 13.0f)
                second.configure(0.0f, 16.0f,1.0f, 0, 12.0f)
            }
            UNIOUT_COND_TMR, UNIOUT_COND_ITTMR, UNIOUT_COND_ESTMR -> {
                first.configure(0.0f, 600.0f,0.1f, 1, 0.0f)
                second.configure(0.0f, 600.0f,0.1f, 1, 5.0f)
            }
            UNIOUT_COND_CPOS -> {
                first.configure(0.0f, 100.0f,0.5f, 1, 60.0f)
                second.configure(0.0f, 100.0f,0.5f, 1, 55.0f)
            }
            UNIOUT_COND_AANG -> {
                first.configure(-15.0f, 65.0f,0.1f, 1, 55.0f)
                second.configure(-15.0f, 65.0f,0.1f, 1, 53.0f)
            }
            UNIOUT_COND_KLEV -> {
                first.configure(0.0f, 5.0f,0.01f, 2, 2.5f)
                second.configure(0.0f, 5.0f,0.01f, 2, 2.45f)
            }
            UNIOUT_COND_TPS -> {
                first.configure(0.0f, 100.0f,0.5f, 1, 30.0f)
                second.configure(0.0f, 100.0f,0.5f, 1, 29.0f)
            }
            UNIOUT_COND_ATS -> {
                first.configure(-40.0f, 180.0f,0.25f, 2, 55.0f)
                second.configure(-40.0f, 180.0f,0.25f, 2, 50.0f)
            }
            UNIOUT_COND_AI1,UNIOUT_COND_AI2,UNIOUT_COND_AI3,UNIOUT_COND_AI4,
            UNIOUT_COND_AI5,UNIOUT_COND_AI6,UNIOUT_COND_AI7,UNIOUT_COND_AI8 -> {
                first.configure(0.0f, 5.0f,0.01f, 2, 2.5f)
                second.configure(0.0f, 5.0f,0.01f, 2, 2.48f)
            }
            UNIOUT_COND_GASV -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                second.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }
            UNIOUT_COND_IPW -> {
                first.configure(0.01f, 200.0f,0.01f, 2, 20.0f)
                second.configure(0.01f, 200.0f,0.01f, 2, 19.9f)
            }
            UNIOUT_COND_CE -> {
                first.configure(0.0f, 1.0f,1.0f, 0, 0.0f)
                second.configure(0.0f, 1.0f,1.0f, 0, 1.0f)
            }
            UNIOUT_COND_OFTMR -> {
                first.configure(0.0f, 600.0f,0.1f, 1, 0.0f)
                second.configure(0.0f, 600.0f,0.1f, 1, 5.0f)
            }
            UNIOUT_COND_LOOPTMR -> {
                first.configure(0.0f, 600.0f,0.1f, 1, 1.0f)
                second.configure(0.0f, 600.0f,0.1f, 1, 5.0f)
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

    companion object {

        private const val UNIOUT_COND_CTS = 0 // Coolant temperature

        private const val UNIOUT_COND_RPM = 1 // RPM

        private const val UNIOUT_COND_MAP = 2 // MAP

        private const val UNIOUT_COND_UBAT = 3 // Board voltage

        private const val UNIOUT_COND_CARB = 4 // Throttle position limit switch

        private const val UNIOUT_COND_VSPD = 5 // Vehicle speed

        private const val UNIOUT_COND_AIRFL = 6 // Air flow

        private const val UNIOUT_COND_TMR = 7 // Timer, allowed only for 2nd condition

        private const val UNIOUT_COND_ITTMR = 8 // Timer, triggered after turning on of ignition

        private const val UNIOUT_COND_ESTMR = 9 // Timer, triggered after starting of engine

        private const val UNIOUT_COND_CPOS = 10 // Choke position

        private const val UNIOUT_COND_AANG = 11 // Advance angle

        private const val UNIOUT_COND_KLEV = 12 // Knock signal level

        private const val UNIOUT_COND_TPS = 13 // Throttle position sensor

        private const val UNIOUT_COND_ATS = 14 // Intake air temperature sensor

        private const val UNIOUT_COND_AI1 = 15 // Analog input 1

        private const val UNIOUT_COND_AI2 = 16 // Analog input 2

        private const val UNIOUT_COND_GASV = 17 // Gas valve input

        private const val UNIOUT_COND_IPW = 18 // Injector pulse width

        private const val UNIOUT_COND_CE = 19 // CE state

        private const val UNIOUT_COND_OFTMR = 20 // On/Off delay timer

        private const val UNIOUT_COND_AI3 = 21 // Analog input 3

        private const val UNIOUT_COND_AI4 = 22 // Analog input 4

        private const val UNIOUT_COND_LOOPTMR = 23 // Looper tmr. 1 cond. (sec.). For second condition only

        private const val UNIOUT_COND_AI5 = 24 // Analog input 5

        private const val UNIOUT_COND_AI6 = 25 // Analog input 6

        private const val UNIOUT_COND_AI7 = 26 // Analog input 7

        private const val UNIOUT_COND_AI8 = 27 // Analog input 8

    }
}