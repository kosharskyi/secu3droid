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
package org.secu3.android.ui.parameters.pages.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.InjctrParPacket

data class FuelInjectionEditSpec(
    val title: Int, val value: Float, val step: Float, val min: Float,
    val max: Float, val precision: Int, val integer: Boolean,
)

/** Owns packet, firmware presentation state, and edit/send coordination for one view lifecycle. */
class FuelInjectionStateHolder(
    private val sendPacket: (InjctrParPacket) -> Unit,
    private val setSendAllowed: (Boolean) -> Unit = {},
) {
    private var packet: InjctrParPacket? = null
    var uiState: FuelInjectionUiState by mutableStateOf(FuelInjectionUiState.Loading)
        private set

    fun reset() {
        packet = null
        uiState = FuelInjectionUiState.Loading
    }

    /** Receiving initializes state and enables later edits without echoing the packet. */
    fun receive(received: InjctrParPacket) {
        setSendAllowed(false)
        packet = received
        uiState = FuelInjectionUiState.Data(fuelInjectionValues(received))
        setSendAllowed(true)
    }

    fun editSpec(number: FuelInjectionNumber): FuelInjectionEditSpec? {
        val p = packet ?: return null
        val current = (uiState as? FuelInjectionUiState.Data)?.values ?: return null
        return when (number) {
            FuelInjectionNumber.ENGINE_DISPLACEMENT -> FuelInjectionEditSpec(R.string.injctr_par_enjine_displacement_title, current.engineDisplacement, .0001f, .01f, 8f, 4, false)
            FuelInjectionNumber.FLOW_0 -> FuelInjectionEditSpec(R.string.injctr_par_injector_flow_rate_title, p.flowRate[0], .02f, 50f, 1023f, 2, false)
            FuelInjectionNumber.FLOW_1 -> FuelInjectionEditSpec(R.string.injctr_par_injector_flow_rate_title, p.flowRate[1], .02f, 50f, 1023f, 2, false)
            FuelInjectionNumber.TIMING_0, FuelInjectionNumber.TIMING_1 -> FuelInjectionEditSpec(R.string.injctr_par_injection_timing_title, p.timing[if (number == FuelInjectionNumber.TIMING_0) 0 else 1], 1f, 0f, 720f, 0, true)
            FuelInjectionNumber.CRANKING_0, FuelInjectionNumber.CRANKING_1 -> FuelInjectionEditSpec(R.string.injctr_par_cranking_injection_timing_title, p.timingCrk[if (number == FuelInjectionNumber.CRANKING_0) 0 else 1], 1f, 0f, 720f, 0, true)
            FuelInjectionNumber.MIN_PW_0 -> FuelInjectionEditSpec(R.string.injctr_par_min_inj_pw_title, p.minPw0, .025f, .1f, 6.5f, 2, false)
            FuelInjectionNumber.MIN_PW_1 -> FuelInjectionEditSpec(R.string.injctr_par_min_inj_pw_title, p.minPw1, .025f, .1f, 6.5f, 2, false)
            FuelInjectionNumber.MAX_PW_0 -> FuelInjectionEditSpec(R.string.injctr_par_max_inj_pw_title, p.injMaxPw[0], .1f, 0f, 100f, 2, false)
            FuelInjectionNumber.MAX_PW_1 -> FuelInjectionEditSpec(R.string.injctr_par_max_inj_pw_title, p.injMaxPw[1], .1f, 0f, 100f, 2, false)
            FuelInjectionNumber.PULSES_PER_LITER -> FuelInjectionEditSpec(R.string.injctr_par_pulses_per_litter_of_fuel, p.fffConst, 1f, 8000f, 32000f, 0, true)
            FuelInjectionNumber.DECREASE_RATE -> FuelInjectionEditSpec(R.string.injctr_par_inj_pw_dec_speed_title, p.injPwDecSpeed, .01f, .01f, 10f, 2, false)
            FuelInjectionNumber.INCREASE_RATE -> FuelInjectionEditSpec(R.string.injctr_par_inj_pw_inc_speed_title, p.injPwIncSpeed, .01f, .01f, 10f, 2, false)
        }
    }

    fun edit(number: FuelInjectionNumber, value: Float) {
        val p = packet ?: return
        if (!value.isFinite()) return
        when (number) {
            FuelInjectionNumber.ENGINE_DISPLACEMENT -> p.engineDisp = value
            FuelInjectionNumber.FLOW_0 -> { p.flowRate[0] = value; p.recalculateDerivedConstants() }
            FuelInjectionNumber.FLOW_1 -> { p.flowRate[1] = value; p.recalculateDerivedConstants() }
            FuelInjectionNumber.TIMING_0 -> p.timing[0] = value
            FuelInjectionNumber.TIMING_1 -> p.timing[1] = value
            FuelInjectionNumber.CRANKING_0 -> p.timingCrk[0] = value
            FuelInjectionNumber.CRANKING_1 -> p.timingCrk[1] = value
            FuelInjectionNumber.MIN_PW_0 -> p.minPw0 = value
            FuelInjectionNumber.MIN_PW_1 -> p.minPw1 = value
            FuelInjectionNumber.MAX_PW_0 -> p.injMaxPw[0] = value
            FuelInjectionNumber.MAX_PW_1 -> p.injMaxPw[1] = value
            FuelInjectionNumber.PULSES_PER_LITER -> p.fffConst = value
            FuelInjectionNumber.DECREASE_RATE -> p.injPwDecSpeed = value
            FuelInjectionNumber.INCREASE_RATE -> p.injPwIncSpeed = value
        }
        publishAndSend(p)
    }

    fun changeConfig(bank: Int, config: Int) {
        val p = packet ?: return
        if (bank !in 0..1 || config !in p.validConfigurations()) return
        val choices = p.validPulses(config)
        if (choices.isEmpty()) return
        val oldPulses = if (bank == 0) p.config0Pulses else p.config1Pulses
        val nextPulses = oldPulses.takeIf(choices::contains) ?: choices.first()
        if (bank == 0) { p.config0Pulses = nextPulses; p.config0 = config }
        else { p.config1Pulses = nextPulses; p.config1 = config }
        publishAndSend(p)
    }

    fun changePulses(bank: Int, pulses: Int) {
        val p = packet ?: return
        if (bank !in 0..1) return
        val config = if (bank == 0) p.config0 else p.config1
        if (pulses !in p.validPulses(config)) return
        if (bank == 0) p.config0Pulses = pulses else p.config1Pulses = pulses
        publishAndSend(p)
    }

    fun changeAngleSpec(bank: Int, value: Int) {
        val p = packet ?: return
        if (bank !in 0..1 || value !in 0..2) return
        if (bank == 0) p.angleSpec0 = value else p.angleSpec1 = value
        publishAndSend(p)
    }

    fun changeFlag(flag: FuelInjectionFlag, value: Boolean) {
        val p = packet ?: return
        when (flag) {
            FuelInjectionFlag.USE_MAP_0 -> p.useTimingMap = value
            FuelInjectionFlag.USE_MAP_1 -> p.useTimingMapG = value
            FuelInjectionFlag.ADDITIONAL_CORRECTIONS -> p.useAdditionalCorrections = value
            FuelInjectionFlag.AIR_DENSITY -> p.useAirDensity = value
            FuelInjectionFlag.DIFFERENTIAL_PRESSURE -> p.useDifferentialPressure = value
            FuelInjectionFlag.SWITCH_ROWS -> p.switchSecondInjRow = value
            FuelInjectionFlag.FULL_SEQUENTIAL_AFTER_START -> p.fullSequentialAfterStart = value
        }
        publishAndSend(p)
    }

    private fun publishAndSend(p: InjctrParPacket) {
        uiState = FuelInjectionUiState.Data(fuelInjectionValues(p))
        sendPacket(p)
    }
}
