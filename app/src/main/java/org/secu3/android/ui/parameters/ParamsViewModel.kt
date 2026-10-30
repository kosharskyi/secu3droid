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
package org.secu3.android.ui.parameters

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.ui.PARAMETERS_ROUTE
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.parameters.EepromSaveResult
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.domain.parameters.ParameterReadResult
import org.secu3.android.domain.parameters.ParameterSession
import org.secu3.android.domain.parameters.ParameterSessionEvent
import org.secu3.android.domain.parameters.GasDoseControl
import org.secu3.android.domain.parameters.FuelInjectionControl
import org.secu3.android.domain.parameters.ChokeControl
import org.secu3.android.domain.parameters.CkpsControl
import org.secu3.android.domain.parameters.MiscellaneousControl
import org.secu3.android.domain.parameters.AnglesControl
import org.secu3.android.domain.parameters.SecurityControl
import org.secu3.android.domain.parameters.StarterControl
import org.secu3.android.domain.parameters.TemperatureControl
import org.secu3.android.domain.parameters.CarburControl
import org.secu3.android.domain.parameters.IdlingControl
import org.secu3.android.domain.parameters.KnockControl
import org.secu3.android.domain.parameters.AccelerationControl
import org.secu3.android.domain.parameters.LtftControl
import org.secu3.android.domain.parameters.DbwControl
import org.secu3.android.domain.parameters.FunctionsControl
import org.secu3.android.domain.parameters.AdcCorrectionsControl
import org.secu3.android.domain.parameters.UniversalOutputsControl
import org.secu3.android.domain.parameters.UniversalOutputConditions
import org.secu3.android.domain.parameters.LambdaControl
import org.secu3.android.domain.parameters.SecurityCredentialsResult
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.models.packets.out.params.DbwParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.ui.parameters.pages.compose.AnglesFlag
import org.secu3.android.ui.parameters.pages.compose.AnglesNumber
import org.secu3.android.ui.parameters.pages.compose.CarburNumber
import org.secu3.android.ui.parameters.pages.compose.DbwNumber
import org.secu3.android.ui.parameters.pages.compose.AdcNumber
import org.secu3.android.ui.parameters.pages.compose.LambdaFlag
import org.secu3.android.ui.parameters.pages.compose.LambdaNumber
import org.secu3.android.ui.parameters.pages.compose.LtftNumber
import org.secu3.android.ui.parameters.pages.compose.IdlingNumber
import org.secu3.android.ui.parameters.pages.compose.IdlingFlag
import org.secu3.android.ui.parameters.pages.compose.KnockNumber
import org.secu3.android.ui.parameters.pages.compose.KnockSelector
import org.secu3.android.ui.parameters.pages.compose.AccelerationNumber
import org.secu3.android.ui.parameters.pages.compose.TemperatureFlag
import org.secu3.android.ui.parameters.pages.compose.TemperatureNumber
import org.secu3.android.ui.parameters.pages.MiscField
import org.secu3.android.ui.parameters.pages.MiscToggle
import org.secu3.android.ui.parameters.pages.compose.FunctionsChoices
import org.secu3.android.ui.parameters.pages.compose.FunctionsNumber
import org.secu3.android.ui.parameters.pages.compose.ChokeNumber
import org.secu3.android.ui.parameters.pages.compose.GasDoseNumber
import org.secu3.android.ui.parameters.pages.compose.FuelInjectionNumber
import org.secu3.android.ui.parameters.pages.compose.FuelInjectionFlag
import org.secu3.android.ui.parameters.pages.compose.CkpsNumber
import org.secu3.android.ui.parameters.pages.compose.CkpsFlag
import org.secu3.android.ui.parameters.pages.compose.StarterFlag
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import javax.inject.Inject

@HiltViewModel
class ParamsViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // TODO: move this out
    private val secu3ConnectionManager: Secu3ConnectionManager,
    private val parameterSession: ParameterSession,
    internal val chokeControl: ChokeControl,
    internal val gasDoseControl: GasDoseControl,
    internal val fuelInjectionControl: FuelInjectionControl,
    internal val ckpsControl: CkpsControl,
    internal val miscellaneousControl: MiscellaneousControl,
    internal val securityControl: SecurityControl,
    internal val starterControl: StarterControl,
    internal val anglesControl: AnglesControl,
    internal val temperatureControl: TemperatureControl,
    internal val carburControl: CarburControl,
    internal val idlingControl: IdlingControl,
    internal val knockControl: KnockControl,
    internal val accelerationControl: AccelerationControl,
    internal val ltftControl: LtftControl,
    internal val dbwControl: DbwControl,
    internal val functionsControl: FunctionsControl,
    internal val adcCorrectionsControl: AdcCorrectionsControl,
    internal val universalOutputsControl: UniversalOutputsControl,
    internal val lambdaControl: LambdaControl,
    internal val universalOutputConditions: UniversalOutputConditions,
    private val uiMessageQueue: UiMessageQueue,
    private val parameterPageCatalog: ParameterPageCatalog,
) : ViewModel() {

    internal var parameterEditSession: ParameterEditSession? by mutableStateOf(null)
        private set
    private var nextParameterEditSessionId = 0L

    internal var connectionSession by mutableIntStateOf(0)
        private set
    internal var failedPage: ParameterPage? by mutableStateOf(null)
        private set

    val connectionState = secu3ConnectionManager.connectionStateFlow
    val capabilities = secu3ConnectionManager.firmwareInfo
        .map(::ParameterCapabilities)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ParameterCapabilities(secu3ConnectionManager.fwInfo))

    internal fun parameterPages(capabilities: ParameterCapabilities): List<ParameterPageSpec> =
        parameterPageCatalog.pages(capabilities)

    fun editStarterInt(number: StarterNumber, value: Int): Boolean {
        val admitted = when (number) {
            StarterNumber.STARTER_OFF -> starterControl.editStarterOff(value)
            StarterNumber.SMAP_ABANDON -> starterControl.editSmapAbandon(value)
            StarterNumber.AFTERSTART_PETROL -> starterControl.editAfterstartPetrol(value)
            StarterNumber.AFTERSTART_GAS -> starterControl.editAfterstartGas(value)
            StarterNumber.STBL_COUNTDOWN -> starterControl.editStblCountdown(value)
            else -> false
        }
        return reportStarterEdit(admitted)
    }

    fun editStarterFloat(number: StarterNumber, value: Float): Boolean {
        val admitted = when (number) {
            StarterNumber.CRANK_TO_RUN_COLD -> starterControl.editCrankToRunCold(value)
            StarterNumber.CRANK_TO_RUN_HOT -> starterControl.editCrankToRunHot(value)
            StarterNumber.PRIME_COLD -> starterControl.editPrimeCold(value)
            StarterNumber.PRIME_HOT -> starterControl.editPrimeHot(value)
            StarterNumber.PRIME_DELAY -> starterControl.editPrimeDelay(value)
            StarterNumber.FLOOD_CLEAR_THRESHOLD -> starterControl.editFloodClearThreshold(value)
            else -> false
        }
        return reportStarterEdit(admitted)
    }

    fun setStarterFlag(flag: StarterFlag, checked: Boolean): Boolean {
        val admitted = when (flag) {
            StarterFlag.ALLOW_FLOOD_CLEAR_START -> starterControl.setAllowFloodClearStart(checked)
            StarterFlag.LIMIT_CRANKING_PW -> starterControl.setLimitCrankingPw(checked)
        }
        return reportStarterEdit(admitted)
    }

    private fun reportStarterEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    fun editAngles(number: AnglesNumber, value: Float): Boolean {
        val admitted = when (number) {
            AnglesNumber.MAX -> anglesControl.editMaxAngle(value)
            AnglesNumber.MIN -> anglesControl.editMinAngle(value)
            AnglesNumber.DECREASE -> anglesControl.editAngleDecSpeed(value)
            AnglesNumber.INCREASE -> anglesControl.editAngleIncSpeed(value)
            AnglesNumber.OCTANE -> anglesControl.editAngleCorrection(value)
            AnglesNumber.SHIFT -> anglesControl.editShiftIgnitionTiming(value)
        }
        return reportAnglesEdit(admitted)
    }

    fun setAnglesFlag(flag: AnglesFlag, checked: Boolean): Boolean {
        val admitted = when (flag) {
            AnglesFlag.ZERO -> anglesControl.setZeroAdvAngle(checked)
            AnglesFlag.WORKING_MAP -> anglesControl.setAlwaysUseIgnitionMap(checked)
            AnglesFlag.MANUAL_CORRECTION -> anglesControl.setApplyManualTimingCorrOnIdl(checked)
            AnglesFlag.ZERO_WITH_OCTANE -> anglesControl.setZeroAdvAngleWithCorr(checked)
        }
        return reportAnglesEdit(admitted)
    }

    private fun reportAnglesEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    fun editTemperatureFloat(number: TemperatureNumber, value: Float): Boolean {
        val admitted = when (number) {
            TemperatureNumber.FAN_ON -> temperatureControl.editFanOn(value)
            TemperatureNumber.FAN_OFF -> temperatureControl.editFanOff(value)
            TemperatureNumber.AC_ON -> temperatureControl.editConditionerOn(value)
            TemperatureNumber.AC_OFF -> temperatureControl.editConditionerOff(value)
            else -> false
        }
        return reportTemperatureEdit(admitted)
    }

    fun editTemperatureInt(number: TemperatureNumber, value: Int): Boolean {
        val admitted = when (number) {
            TemperatureNumber.PWM_FREQUENCY -> temperatureControl.editPwmFrequency(value)
            TemperatureNumber.AC_MIN_RPM -> temperatureControl.editConditionerMinRpm(value)
            TemperatureNumber.FAN_TIMER -> temperatureControl.editFanTimer(value)
            else -> false
        }
        return reportTemperatureEdit(admitted)
    }

    fun setTemperatureFlag(flag: TemperatureFlag, checked: Boolean): Boolean {
        val admitted = when (flag) {
            TemperatureFlag.COOLANT_SENSOR -> temperatureControl.setCoolantSensor(checked)
            TemperatureFlag.PWM_CONTROL -> temperatureControl.setPwmControl(checked)
            TemperatureFlag.SENSOR_CURVE_TABLE -> temperatureControl.setSensorCurveTable(checked)
        }
        return reportTemperatureEdit(admitted)
    }

    private fun reportTemperatureEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    fun editCarburInt(number: CarburNumber, value: Int): Boolean {
        val admitted = when (number) {
            CarburNumber.IDLE_LOW -> carburControl.editIdleLowRpm(value)
            CarburNumber.IDLE_HIGH -> carburControl.editIdleHighRpm(value)
            CarburNumber.IDLE_LOW_GAS -> carburControl.editIdleLowGasRpm(value)
            CarburNumber.IDLE_HIGH_GAS -> carburControl.editIdleHighGasRpm(value)
            CarburNumber.REV_LOW -> carburControl.editRevLimitLowRpm(value)
            CarburNumber.REV_HIGH -> carburControl.editRevLimitHighRpm(value)
            else -> false
        }
        return reportCarburEdit(admitted)
    }

    fun editCarburFloat(number: CarburNumber, value: Float): Boolean {
        val admitted = when (number) {
            CarburNumber.CUTOFF_DELAY -> carburControl.editCutoffDelay(value)
            CarburNumber.VALVE_PRESSURE -> carburControl.editValvePressure(value)
            CarburNumber.TPS -> carburControl.editTpsThreshold(value)
            CarburNumber.MAP -> carburControl.editMapThreshold(value)
            CarburNumber.CTS -> carburControl.editCoolantThreshold(value)
            else -> false
        }
        return reportCarburEdit(admitted)
    }

    fun setCarburInverted(checked: Boolean): Boolean = reportCarburEdit(carburControl.setInverted(checked))

    fun selectCarburOutput(ignition: Boolean, index: Int): Boolean = reportCarburEdit(
        if (ignition) carburControl.selectIgnitionCutOutput(index) else carburControl.selectFuelCutOutput(index),
    )

    private fun reportCarburEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal fun editIdlingInt(number: IdlingNumber, value: Int): Boolean {
        val admitted = when (number) {
            IdlingNumber.RPM -> idlingControl.editTargetRpm(value)
            IdlingNumber.DEAD_BAND -> idlingControl.editDeadBand(value)
            IdlingNumber.RPM_ADD -> idlingControl.editRpmAddOnRun(value)
            IdlingNumber.INTEGRATOR_RPM -> idlingControl.editIntegratorRpmLimit(value)
            IdlingNumber.IAC_DB -> idlingControl.editIacDeadBand(value)
            else -> false
        }
        return reportIdlingEdit(admitted)
    }

    internal fun editIdlingFloat(number: IdlingNumber, value: Float): Boolean {
        val admitted = when (number) {
            IdlingNumber.IFAC1 -> idlingControl.editIFac1(value)
            IdlingNumber.IFAC2 -> idlingControl.editIFac2(value)
            IdlingNumber.MAX_ANGLE -> idlingControl.editMaxAngle(value)
            IdlingNumber.MIN_ANGLE -> idlingControl.editMinAngle(value)
            IdlingNumber.ON_TEMP -> idlingControl.editTurnOnTemperature(value)
            IdlingNumber.IAC_ADD -> idlingControl.editIacAddAfterExit(value)
            IdlingNumber.P0 -> idlingControl.editProportionalPositive(value)
            IdlingNumber.I0 -> idlingControl.editIntegralPositive(value)
            IdlingNumber.P1 -> idlingControl.editProportionalNegative(value)
            IdlingNumber.I1 -> idlingControl.editIntegralNegative(value)
            IdlingNumber.THRD1 -> idlingControl.editTransientThreshold1(value)
            IdlingNumber.THRD2 -> idlingControl.editTransientThreshold2(value)
            IdlingNumber.MAP -> idlingControl.editMapThreshold(value)
            IdlingNumber.IAC_MIN -> idlingControl.editIacMinimumPosition(value)
            IdlingNumber.IAC_MAX -> idlingControl.editIacMaximumPosition(value)
            IdlingNumber.D -> idlingControl.editDifferential(value)
            IdlingNumber.IRR_LOAD -> idlingControl.editRigidityLoad(value)
            IdlingNumber.IRR_RPM -> idlingControl.editRigidityRpm(value)
            IdlingNumber.LOAD -> idlingControl.editLoadThreshold(value)
            else -> false
        }
        return reportIdlingEdit(admitted)
    }

    internal fun setIdlingFlag(flag: IdlingFlag, enabled: Boolean): Boolean {
        val admitted = when (flag) {
            IdlingFlag.REGULATOR -> idlingControl.setRegulatorEnabled(enabled)
            IdlingFlag.ON_GAS -> idlingControl.setRegulatorOnGas(enabled)
            IdlingFlag.PROPORTIONAL -> idlingControl.setProportionalMode(enabled)
            IdlingFlag.CLOSED_LOOP -> idlingControl.setClosedLoop(enabled)
            IdlingFlag.THRASSMAP -> idlingControl.setThrottleAssistMap(enabled)
            IdlingFlag.CLOSED_LOOP_GAS -> idlingControl.setClosedLoopOnGas(enabled)
            IdlingFlag.WORKS_IAC -> idlingControl.setWorksWithIacRegulator(enabled)
        }
        return reportIdlingEdit(admitted)
    }

    private fun reportIdlingEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal fun editKnockFloat(number: KnockNumber, value: Float): Boolean {
        val admitted = when (number) {
            KnockNumber.BEGIN -> knockControl.editWindowBegin(value)
            KnockNumber.END -> knockControl.editWindowEnd(value)
            KnockNumber.RETARD -> knockControl.editRetardStep(value)
            KnockNumber.ADVANCE -> knockControl.editAdvanceStep(value)
            KnockNumber.MAX_RETARD -> knockControl.editMaxRetard(value)
            KnockNumber.THRESHOLD -> knockControl.editVoltageThreshold(value)
            KnockNumber.CONTROL -> knockControl.editCoolantThreshold(value)
            KnockNumber.DELAY -> false
        }
        return reportKnockEdit(admitted)
    }

    internal fun editKnockDelay(value: Int): Boolean = reportKnockEdit(knockControl.editRecoveryDelay(value))

    internal fun setKnockEnabled(enabled: Boolean): Boolean = reportKnockEdit(knockControl.setKnockEnabled(enabled))

    internal fun selectKnockChannel(channel: Int, checked: Boolean): Boolean =
        reportKnockEdit(knockControl.selectChannel(channel, checked))

    internal fun selectKnock(selector: KnockSelector, code: Int): Boolean = reportKnockEdit(
        when (selector) {
            KnockSelector.BPF -> knockControl.selectBpfFrequency(code)
            KnockSelector.INTEGRATION -> knockControl.selectIntegrationTimeout(code)
        },
    )

    private fun reportKnockEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal fun editAcceleration(number: AccelerationNumber, value: Int): Boolean {
        val admitted = when (number) {
            AccelerationNumber.TPS_THRESHOLD -> accelerationControl.editTpsThreshold(value)
            AccelerationNumber.COLD_MULTIPLIER -> accelerationControl.editColdMultiplier(value)
            AccelerationNumber.DECAY_TIME -> accelerationControl.editDecayTime(value)
            AccelerationNumber.AE_TIME -> accelerationControl.editAeTime(value)
            AccelerationNumber.MAPDOT_THRESHOLD -> accelerationControl.editMapdotThreshold(value)
            AccelerationNumber.XTAU_START -> accelerationControl.editXtauStart(value)
            AccelerationNumber.XTAU_FINISH -> accelerationControl.editXtauFinish(value)
        }
        return reportAccelerationEdit(admitted)
    }

    internal fun selectAccelerationAeType(code: Int): Boolean = reportAccelerationEdit(accelerationControl.selectAeType(code))
    internal fun selectAccelerationWallwetModel(code: Int): Boolean = reportAccelerationEdit(accelerationControl.selectWallwetModel(code))
    internal fun setAccelerationBalance(value: Float): Boolean = reportAccelerationEdit(accelerationControl.setBalance(value))

    private fun reportAccelerationEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal fun editAdc(number: AdcNumber, value: Float): Boolean {
        val admitted = when (number) {
            AdcNumber.MAP_FACTOR -> adcCorrectionsControl.editMapFactor(value)
            AdcNumber.MAP_CORRECTION -> adcCorrectionsControl.editMapCorrection(value)
            AdcNumber.VOLTAGE_FACTOR -> adcCorrectionsControl.editVoltageFactor(value)
            AdcNumber.VOLTAGE_CORRECTION -> adcCorrectionsControl.editVoltageCorrection(value)
            AdcNumber.CTS_FACTOR -> adcCorrectionsControl.editCtsFactor(value)
            AdcNumber.CTS_CORRECTION -> adcCorrectionsControl.editCtsCorrection(value)
            AdcNumber.TPS_FACTOR -> adcCorrectionsControl.editTpsFactor(value)
            AdcNumber.TPS_CORRECTION -> adcCorrectionsControl.editTpsCorrection(value)
            AdcNumber.ADD_I1_FACTOR -> adcCorrectionsControl.editAddI1Factor(value)
            AdcNumber.ADD_I1_CORRECTION -> adcCorrectionsControl.editAddI1Correction(value)
            AdcNumber.ADD_I2_FACTOR -> adcCorrectionsControl.editAddI2Factor(value)
            AdcNumber.ADD_I2_CORRECTION -> adcCorrectionsControl.editAddI2Correction(value)
            AdcNumber.ADD_I3_FACTOR -> adcCorrectionsControl.editAddI3Factor(value)
            AdcNumber.ADD_I3_CORRECTION -> adcCorrectionsControl.editAddI3Correction(value)
            AdcNumber.ADD_I4_FACTOR -> adcCorrectionsControl.editAddI4Factor(value)
            AdcNumber.ADD_I4_CORRECTION -> adcCorrectionsControl.editAddI4Correction(value)
            AdcNumber.ADD_I5_FACTOR -> adcCorrectionsControl.editAddI5Factor(value)
            AdcNumber.ADD_I5_CORRECTION -> adcCorrectionsControl.editAddI5Correction(value)
            AdcNumber.ADD_I6_FACTOR -> adcCorrectionsControl.editAddI6Factor(value)
            AdcNumber.ADD_I6_CORRECTION -> adcCorrectionsControl.editAddI6Correction(value)
            AdcNumber.ADD_I7_FACTOR -> adcCorrectionsControl.editAddI7Factor(value)
            AdcNumber.ADD_I7_CORRECTION -> adcCorrectionsControl.editAddI7Correction(value)
            AdcNumber.ADD_I8_FACTOR -> adcCorrectionsControl.editAddI8Factor(value)
            AdcNumber.ADD_I8_CORRECTION -> adcCorrectionsControl.editAddI8Correction(value)
        }
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal fun setAdcCompensationMode(enabled: Boolean): Boolean {
        val admitted = adcCorrectionsControl.setCompensationMode(enabled)
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal fun editLambdaInt(number: LambdaNumber, value: Int): Boolean = reportLambdaEdit(when (number) {
        LambdaNumber.STROKES -> lambdaControl.editStrokesPerStep(value)
        LambdaNumber.MS -> lambdaControl.editMsPerStep(value)
        LambdaNumber.RPM -> lambdaControl.editRpmThreshold(value)
        LambdaNumber.DELAY -> lambdaControl.editActivationDelay(value)
        LambdaNumber.HEAT_COLD -> lambdaControl.editColdHeatingTime(value)
        LambdaNumber.HEAT_HOT -> lambdaControl.editHotHeatingTime(value)
        LambdaNumber.HEAT_TEMP -> lambdaControl.editHeatingTemperature(value)
        LambdaNumber.AIR_FLOW -> lambdaControl.editAirFlowThreshold(value)
        else -> false
    })

    internal fun editLambdaFloat(number: LambdaNumber, value: Float): Boolean = reportLambdaEdit(when (number) {
        LambdaNumber.STEP_POS -> lambdaControl.editPositiveStep(value)
        LambdaNumber.STEP_NEG -> lambdaControl.editNegativeStep(value)
        LambdaNumber.LIMIT_POS -> lambdaControl.editPositiveLimit(value)
        LambdaNumber.LIMIT_NEG -> lambdaControl.editNegativeLimit(value)
        LambdaNumber.SWITCH -> lambdaControl.editSwitchPoint(value)
        LambdaNumber.CTS -> lambdaControl.editCoolantThreshold(value)
        LambdaNumber.DEADBAND -> lambdaControl.editDeadband(value)
        LambdaNumber.STOICH -> lambdaControl.editStoichiometricRatio(value)
        LambdaNumber.HEAT_PWM -> lambdaControl.editHeatingPwmTime(value)
        else -> false
    })

    internal fun setLambdaFlag(flag: LambdaFlag, checked: Boolean): Boolean = reportLambdaEdit(when (flag) {
        LambdaFlag.HEATING_VOLTAGE -> lambdaControl.setHeatingVoltage(checked)
        LambdaFlag.IDLING_CORRECTION -> lambdaControl.setIdlingCorrection(checked)
        LambdaFlag.BEFORE_CRANKING -> lambdaControl.setBeforeCranking(checked)
        LambdaFlag.MIX_SENSORS -> lambdaControl.setMixSensors(checked)
    })

    internal fun selectLambdaSensorType(code: Int): Boolean = reportLambdaEdit(lambdaControl.selectSensorType(code))
    internal fun setLambdaChannel(index: Int, selected: Boolean): Boolean = reportLambdaEdit(lambdaControl.setChannelSelected(index, selected))

    private fun reportLambdaEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }


    internal fun editLtftInt(number: LtftNumber, value: Int): Boolean = reportLtftEdit(
        when (number) {
            LtftNumber.LOWER_RPM -> ltftControl.editLowerRpm(value)
            LtftNumber.UPPER_RPM -> ltftControl.editUpperRpm(value)
            else -> false
        },
    )

    internal fun editLtftFloat(number: LtftNumber, value: Float): Boolean = reportLtftEdit(
        when (number) {
            LtftNumber.LOWER_CLT -> ltftControl.editLowerClt(value)
            LtftNumber.UPPER_CLT -> ltftControl.editUpperClt(value)
            LtftNumber.UPPER_IAT -> ltftControl.editUpperIat(value)
            LtftNumber.LEARNING_GRADIENT -> ltftControl.editLearningGradient(value)
            LtftNumber.ABS_GAS_PRESSURE -> ltftControl.editAbsoluteGasPressure(value)
            LtftNumber.DIFF_GAS_PRESSURE -> ltftControl.editDifferentialGasPressure(value)
            LtftNumber.BOTTOM_LIMIT -> ltftControl.editBottomLimit(value)
            LtftNumber.TOP_LIMIT -> ltftControl.editTopLimit(value)
            LtftNumber.LOWER_LOAD -> ltftControl.editLowerLoad(value)
            LtftNumber.UPPER_LOAD -> ltftControl.editUpperLoad(value)
            LtftNumber.NEGATIVE_INSENSITIVITY -> ltftControl.editNegativeInsensitivity(value)
            LtftNumber.POSITIVE_INSENSITIVITY -> ltftControl.editPositiveInsensitivity(value)
            else -> false
        },
    )

    internal fun selectLtftMode(code: Int): Boolean = reportLtftEdit(ltftControl.selectMode(code))

    private fun reportLtftEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal fun editDbw(number: DbwNumber, value: Float): Boolean {
        val admitted = when (number) {
            DbwNumber.ETC_P -> dbwControl.editEtcP(value)
            DbwNumber.ETC_I -> dbwControl.editEtcI(value)
            DbwNumber.ETC_D -> dbwControl.editEtcD(value)
            DbwNumber.NMAX_DUTY -> dbwControl.editNegativeMaxDuty(value)
            DbwNumber.PMAX_DUTY -> dbwControl.editPositiveMaxDuty(value)
            DbwNumber.PID_PERIOD -> dbwControl.editPidPeriod(value)
            DbwNumber.FRICTION_TORQUE_OPEN -> dbwControl.editFrictionTorqueOpen(value)
            DbwNumber.FRICTION_TORQUE_CLOSE -> dbwControl.editFrictionTorqueClose(value)
            DbwNumber.FRICTION_THRD -> dbwControl.editFrictionThreshold(value)
            DbwNumber.FRICTORQ_IDLE_ADD_MAX -> dbwControl.editFrictionIdleAdditionMax(value)
        }
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    private fun reportUniversalOutputsEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }
    internal fun setUniversalOutputEnabled(output: Int, enabled: Boolean) = reportUniversalOutputsEdit(universalOutputsControl.setOutputEnabled(output, enabled))
    internal fun selectUniversalOutputCondition(output: Int, slot: Int, code: Int) = reportUniversalOutputsEdit(universalOutputsControl.selectCondition(output, slot, code))
    internal fun setUniversalOutputConditionInverted(output: Int, slot: Int, inverted: Boolean) = reportUniversalOutputsEdit(universalOutputsControl.setConditionInverted(output, slot, inverted))
    internal fun setUniversalOutputResultInverted(output: Int, inverted: Boolean) = reportUniversalOutputsEdit(universalOutputsControl.setResultInverted(output, inverted))
    internal fun selectUniversalOutputLogicFunction(output: Int, code: Int) = reportUniversalOutputsEdit(universalOutputsControl.selectLogicFunction(output, code))
    internal fun editUniversalOutputThreshold(output: Int, slot: Int, on: Boolean, expectedCondition: Int, value: Float) = reportUniversalOutputsEdit(
        if (on) universalOutputsControl.editOnThreshold(output, slot, expectedCondition, value) else universalOutputsControl.editOffThreshold(output, slot, expectedCondition, value),
    )
    internal fun selectUniversalOutputsPairFunction(code: Int) = reportUniversalOutputsEdit(universalOutputsControl.selectPairFunction(code))

    internal fun editFunctions(number: FunctionsNumber, value: Float): Boolean {
        val admitted = when (number) {
            FunctionsNumber.LOWER -> functionsControl.editLoadLower(value)
            FunctionsNumber.UPPER -> functionsControl.editLoadUpper(value)
            FunctionsNumber.MAP_OFFSET -> functionsControl.editMapCurveOffset(value)
            FunctionsNumber.MAP_GRADIENT -> functionsControl.editMapCurveGradient(value)
            FunctionsNumber.TPS_OFFSET -> functionsControl.editTpsCurveOffset(value)
            FunctionsNumber.TPS_GRADIENT -> functionsControl.editTpsCurveGradient(value)
            FunctionsNumber.MAP2_OFFSET -> functionsControl.editMap2CurveOffset(value)
            FunctionsNumber.MAP2_GRADIENT -> functionsControl.editMap2CurveGradient(value)
            FunctionsNumber.GPS_OFFSET -> functionsControl.editGpsCurveOffset(value)
            FunctionsNumber.GPS_GRADIENT -> functionsControl.editGpsCurveGradient(value)
            FunctionsNumber.FPS_OFFSET -> functionsControl.editFpsCurveOffset(value)
            FunctionsNumber.FPS_GRADIENT -> functionsControl.editFpsCurveGradient(value)
            FunctionsNumber.APPS_OFFSET -> functionsControl.editApps1CurveOffset(value)
            FunctionsNumber.APPS_GRADIENT -> functionsControl.editApps1CurveGradient(value)
        }
        return reportFunctionsEdit(admitted)
    }

    internal fun setFunctionsLoadGrid(checked: Boolean): Boolean = reportFunctionsEdit(functionsControl.setUseLoadGrid(checked))

    internal fun selectFunctionsChoice(selector: FunctionsChoices.Selector, code: Int): Boolean {
        val admitted = when (selector) {
            FunctionsChoices.Selector.GASOLINE_MAP -> functionsControl.selectGasolineMap(code)
            FunctionsChoices.Selector.GAS_MAP -> functionsControl.selectGasMap(code)
            FunctionsChoices.Selector.LOAD_SOURCE -> functionsControl.selectLoadSource(code)
            FunctionsChoices.Selector.MAPSEL_PETROL -> functionsControl.selectMapsForPetrol(code)
            FunctionsChoices.Selector.MAPSEL_GAS -> functionsControl.selectMapsForGas(code)
            FunctionsChoices.Selector.BAROCORR -> functionsControl.selectBarometricCorrection(code)
            FunctionsChoices.Selector.VE2 -> functionsControl.selectVe2Function(code)
            FunctionsChoices.Selector.GAS_V_UNI -> functionsControl.selectGasVoltageInput(code)
        }
        return reportFunctionsEdit(admitted)
    }

    internal fun updateFunctionsMafEngineDisplacement(value: Float): Boolean =
        reportFunctionsEdit(functionsControl.updateMafEngineDisplacement(value))

    private fun reportFunctionsEdit(admitted: Boolean): Boolean {
        if (admitted) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
        return admitted
    }

    internal var securityName by mutableStateOf("")
        private set
    internal var securityPassword by mutableStateOf("")
        private set
    internal var securityKey0 by mutableStateOf("")
        private set
    internal var securityKey1 by mutableStateOf("")
        private set
    internal var securityError by mutableStateOf("")
        private set

    init {
        parameterSession.start(viewModelScope, ::onParameterSessionEvent)
        chokeControl.start(viewModelScope)
        gasDoseControl.start(viewModelScope)
    }

    internal fun activatePage(page: ParameterPage) = parameterSession.activatePage(page)

    internal fun deactivatePage() = parameterSession.deactivatePage()

    internal fun pausePageSession() = parameterSession.pause()

    internal fun refreshPage(page: ParameterPage) = parameterSession.refreshPage(page)

    internal fun cancelManualCommands() {
        chokeControl.cancelManual()
        gasDoseControl.cancelManual()
    }

    internal fun editChoke(number: ChokeNumber, value: Float) {
        val sent = when (number) {
            ChokeNumber.STEPS -> chokeControl.editSteps(value)
            ChokeNumber.REGULATOR_FACTOR -> chokeControl.editRegulatorFactor(value)
            ChokeNumber.COLD_DURATION -> chokeControl.editColdDuration(value)
            ChokeNumber.HOT_DURATION -> chokeControl.editHotDuration(value)
            ChokeNumber.CRANK_TO_RUN -> chokeControl.editCrankToRunDuration(value)
            else -> false
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun changeSecurityName(value: String) {
        if (securityControl.values.value != null && value.length <= 16) securityName = value
        securityError = ""
    }

    internal fun changeSecurityPassword(value: String) {
        if (securityControl.values.value != null && value.length <= 8 && value.all { it in '0'..'9' }) {
            securityPassword = value
        }
        securityError = ""
    }

    internal fun changeSecurityKey(index: Int, value: String) {
        if (securityControl.values.value == null || index !in 0..1 || value.length > 12) return
        val normalized = value.uppercase()
        if (index == 0) securityKey0 = normalized else securityKey1 = normalized
        showParameterPacketSentMessage(securityControl.changeKey(index, normalized))
        securityError = ""
    }

    internal fun isSecurityKeyValid(value: String): Boolean = securityControl.isValidKey(value)

    internal fun changeSecurityType(value: Int) = showParameterPacketSentMessage(securityControl.changeType(value))

    internal fun changeSecurityBluetooth(value: Boolean) = showParameterPacketSentMessage(securityControl.changeBluetooth(value))

    internal fun changeSecurityImmobilizer(value: Boolean) = showParameterPacketSentMessage(securityControl.changeImmobilizer(value))

    internal fun applySecurityCredentials() {
        when (securityControl.applyCredentials(securityName, securityPassword)) {
            SecurityCredentialsResult.ADMITTED -> {
                securityName = ""
                securityPassword = ""
                securityError = ""
                showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
            }
            SecurityCredentialsResult.INVALID -> securityError = context.getString(R.string.sec_invalid_credentials)
            SecurityCredentialsResult.NOT_ADMITTED -> Unit
        }
    }

    internal fun toggleChoke(number: ChokeNumber, enabled: Boolean) {
        val sent = when (number) {
            ChokeNumber.CLOSED_LOOP -> chokeControl.setClosedLoopRegulator(enabled)
            ChokeNumber.DISABLE_ON_GAS -> chokeControl.setRegulatorDisabledOnGas(enabled)
            ChokeNumber.THROTTLE_INIT -> chokeControl.setThrottlePositionAtInit(enabled)
            ChokeNumber.MAX_FREQUENCY_INIT -> chokeControl.setMaximumFrequencyAtInit(enabled)
            else -> false
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun selectChokeFrequency(index: Int) = showParameterPacketSentMessage(chokeControl.selectFrequency(index))

    internal fun toggleChokeTest() = showParameterPacketSentMessage(chokeControl.toggleTest())

    internal fun stepChoke(direction: Int) = chokeControl.manualStep(direction)

    internal fun flushChokeManual() = showParameterPacketSentMessage(chokeControl.flushManual())

    internal fun finishChokeManual() = showParameterPacketSentMessage(chokeControl.finishManual())

    internal fun cancelChokeManual() = chokeControl.cancelManual()

    internal fun editGasDose(number: GasDoseNumber, value: Float) {
        val sent = when (number) {
            GasDoseNumber.STEPS -> gasDoseControl.editSteps(value)
            GasDoseNumber.STOICHIOMETRIC_RATIO -> gasDoseControl.editStoichiometricRatio(value)
            GasDoseNumber.FUEL_CUT_CLOSING -> gasDoseControl.editFuelCutClosing(value)
            GasDoseNumber.CORRECTION_POSITIVE -> gasDoseControl.editCorrectionPositive(value)
            GasDoseNumber.CORRECTION_NEGATIVE -> gasDoseControl.editCorrectionNegative(value)
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun selectGasDoseFrequency(index: Int) = showParameterPacketSentMessage(gasDoseControl.selectFrequency(index))

    internal fun setGasDoseMaximumFrequency(enabled: Boolean) =
        showParameterPacketSentMessage(gasDoseControl.setMaximumFrequency(enabled))

    internal fun toggleGasDoseTest() = showParameterPacketSentMessage(gasDoseControl.toggleTest())

    internal fun stepGasDose(direction: Int) = gasDoseControl.manualStep(direction)

    internal fun flushGasDoseManual() = showParameterPacketSentMessage(gasDoseControl.flushManual())

    internal fun finishGasDoseManual() = showParameterPacketSentMessage(gasDoseControl.finishManual())

    internal fun cancelGasDoseManual() = gasDoseControl.cancelManual()

    internal fun editFuelInjection(number: FuelInjectionNumber, value: Float) {
        val sent = when (number) {
            FuelInjectionNumber.ENGINE_DISPLACEMENT -> fuelInjectionControl.editEngineDisplacement(value)
            FuelInjectionNumber.FLOW_0 -> fuelInjectionControl.editFlow(0, value)
            FuelInjectionNumber.TIMING_0 -> fuelInjectionControl.editTiming(0, value)
            FuelInjectionNumber.CRANKING_0 -> fuelInjectionControl.editCrankingTiming(0, value)
            FuelInjectionNumber.MIN_PW_0 -> fuelInjectionControl.editMinimumPulseWidth(0, value)
            FuelInjectionNumber.MAX_PW_0 -> fuelInjectionControl.editMaximumPulseWidth(0, value)
            FuelInjectionNumber.FLOW_1 -> fuelInjectionControl.editFlow(1, value)
            FuelInjectionNumber.TIMING_1 -> fuelInjectionControl.editTiming(1, value)
            FuelInjectionNumber.CRANKING_1 -> fuelInjectionControl.editCrankingTiming(1, value)
            FuelInjectionNumber.MIN_PW_1 -> fuelInjectionControl.editMinimumPulseWidth(1, value)
            FuelInjectionNumber.MAX_PW_1 -> fuelInjectionControl.editMaximumPulseWidth(1, value)
            FuelInjectionNumber.PULSES_PER_LITER -> fuelInjectionControl.editPulsesPerLiter(value)
            FuelInjectionNumber.DECREASE_RATE -> fuelInjectionControl.editDecreaseRate(value)
            FuelInjectionNumber.INCREASE_RATE -> fuelInjectionControl.editIncreaseRate(value)
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun editCkps(number: CkpsNumber, value: Float) {
        val sent = when (number) {
            CkpsNumber.WHEEL -> ckpsControl.editWheel(value)
            CkpsNumber.MISSING -> ckpsControl.editMissingTeeth(value)
            CkpsNumber.PULSE -> ckpsControl.editIgnitionPulseCount(value)
            CkpsNumber.HALL_WIDTH -> ckpsControl.editHallWindowWidth(value)
            CkpsNumber.DEGREES -> ckpsControl.editHallDegreesBeforeTdc(value)
            CkpsNumber.MTTF -> ckpsControl.editMttf(value)
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun toggleCkps(flag: CkpsFlag, value: Boolean) {
        val sent = when (flag) {
            CkpsFlag.CKPS_EDGE -> ckpsControl.setCkpsEdge(value)
            CkpsFlag.REFERENCE_EDGE -> ckpsControl.setReferenceEdge(value)
            CkpsFlag.SPARK_RISING -> ckpsControl.setRisingSpark(value)
            CkpsFlag.MERGE_OUTPUTS -> ckpsControl.setMergeOutputs(value)
            CkpsFlag.USE_CAM_REFERENCE -> ckpsControl.setCamReference(value)
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun selectCkpsChoice(teeth: Boolean, value: Int) {
        showParameterPacketSentMessage(if (teeth) ckpsControl.selectTeethBeforeTdc(value) else ckpsControl.selectCylinders(value))
    }

    internal fun editMiscellaneous(field: MiscField, value: Int) {
        val sent = when (field) {
            MiscField.PERIOD -> miscellaneousControl.editPeriod(value)
            MiscField.CUTOFF -> miscellaneousControl.editCutoff(value)
            MiscField.START -> miscellaneousControl.editStart(value)
            MiscField.DURATION -> miscellaneousControl.editDuration(value)
            MiscField.BEGIN -> miscellaneousControl.editEvapBegin(value)
            MiscField.END -> miscellaneousControl.editEvapEnd(value)
            MiscField.PWM0 -> miscellaneousControl.editPwm0(value)
            MiscField.PWM1 -> miscellaneousControl.editPwm1(value)
            MiscField.VSS -> miscellaneousControl.editVss(value)
            MiscField.TIMEOUT -> false
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun editMiscellaneousTimeout(value: Float) =
        showParameterPacketSentMessage(miscellaneousControl.editTimeout(value))

    internal fun selectMiscellaneousBaud(value: Int) =
        showParameterPacketSentMessage(miscellaneousControl.selectBaudDivisor(value))

    internal fun toggleMiscellaneous(flag: MiscToggle, value: Boolean) {
        val sent = when (flag) {
            MiscToggle.IGN -> miscellaneousControl.setIgnitionCutoff(value)
            MiscToggle.PUMP -> miscellaneousControl.setPumpOffOnGas(value)
            MiscToggle.GAS -> miscellaneousControl.setInjectorsOffOnGas(value)
            MiscToggle.PETROL -> miscellaneousControl.setInjectorsOffOnPetrol(value)
        }
        showParameterPacketSentMessage(sent)
    }

    internal fun changeFuelInjectionConfig(bank: Int, value: Int) =
        showParameterPacketSentMessage(fuelInjectionControl.changeConfig(bank, value))

    internal fun changeFuelInjectionPulses(bank: Int, value: Int) =
        showParameterPacketSentMessage(fuelInjectionControl.changePulses(bank, value))

    internal fun changeFuelInjectionAngleSpec(bank: Int, value: Int) =
        showParameterPacketSentMessage(fuelInjectionControl.changeAngleSpec(bank, value))

    internal fun changeFuelInjectionFlag(flag: FuelInjectionFlag, value: Boolean) {
        val sent = when (flag) {
            FuelInjectionFlag.USE_MAP_0 -> fuelInjectionControl.setUseMap(0, value)
            FuelInjectionFlag.USE_MAP_1 -> fuelInjectionControl.setUseMap(1, value)
            FuelInjectionFlag.ADDITIONAL_CORRECTIONS -> fuelInjectionControl.setAdditionalCorrections(value)
            FuelInjectionFlag.AIR_DENSITY -> fuelInjectionControl.setAirDensity(value)
            FuelInjectionFlag.DIFFERENTIAL_PRESSURE -> fuelInjectionControl.setDifferentialPressure(value)
            FuelInjectionFlag.SWITCH_ROWS -> fuelInjectionControl.setSwitchRows(value)
            FuelInjectionFlag.FULL_SEQUENTIAL_AFTER_START -> fuelInjectionControl.setFullSequentialAfterStart(value)
        }
        showParameterPacketSentMessage(sent)
    }

    private fun showParameterPacketSentMessage(sent: Boolean) {
        if (sent) showUiMessage(R.string.packet_sent, UiMessageDuration.SHORT)
    }

    internal fun openParameterEditor(request: ParameterEditRequest) {
        parameterEditSession = ParameterEditSession(++nextParameterEditSessionId, request)
    }

    internal fun dismissParameterEditor(sessionId: Long) {
        if (parameterEditSession?.id == sessionId) parameterEditSession = null
    }

    internal fun acceptParameterEditor(sessionId: Long, value: Number) {
        val session = parameterEditSession?.takeIf { it.id == sessionId } ?: return
        val accepted = when (val request = session.request) {
            is ParameterEditRequest.IntValue -> (value as? Int)?.takeIf { it in request.min..request.max }
            is ParameterEditRequest.FloatValue -> (value as? Float)?.takeIf { it.isFinite() && it in request.min..request.max }
        } ?: return
        parameterEditSession = null
        when (val request = session.request) {
            is ParameterEditRequest.IntValue -> request.onValue(accepted as Int)
            is ParameterEditRequest.FloatValue -> request.onValue(accepted as Float)
        }
    }

    internal fun clearPageState() = parameterSession.closePageSession()

    private fun onParameterSessionEvent(event: ParameterSessionEvent) {
        when (event) {
            is ParameterSessionEvent.Invalidated -> {
                connectionSession = event.sessionId
                parameterEditSession = null
                failedPage = null
                resetAllStateHolders()
            }
            is ParameterSessionEvent.PageLoading -> {
                if (failedPage == event.page) failedPage = null
                resetStateHolder(event.page)
            }
            is ParameterSessionEvent.PageLoaded -> applyPageResult(event.page, event.result)
            is ParameterSessionEvent.PageFailed -> failedPage = event.page
            ParameterSessionEvent.SaveStarted -> showUiMessage(R.string.saving, UiMessageDuration.SHORT)
            is ParameterSessionEvent.SaveFinished -> when (event.result) {
                EepromSaveResult.Confirmed -> showUiMessage(R.string.packet_saved, UiMessageDuration.SHORT)
                EepromSaveResult.Unconfirmed -> showUiMessage(R.string.parameter_save_unconfirmed, UiMessageDuration.LONG)
                EepromSaveResult.NotStarted -> Unit
            }
        }
    }

    private fun resetStateHolder(page: ParameterPage) {
        when (page) {
            ParameterPage.STARTER -> starterControl.reset()
            ParameterPage.ANGLES -> anglesControl.reset()
            ParameterPage.IDLING -> idlingControl.reset()
            ParameterPage.TEMPERATURE -> temperatureControl.reset()
            ParameterPage.FUEL_CUTOFF -> carburControl.reset()
            ParameterPage.CKPS -> ckpsControl.reset()
            ParameterPage.ADC_ERRORS -> adcCorrectionsControl.reset()
            ParameterPage.KNOCK -> knockControl.reset()
            ParameterPage.LAMBDA_CONTROL -> lambdaControl.reset()
            ParameterPage.ACCELERATION -> accelerationControl.reset()
            ParameterPage.LTFT -> ltftControl.reset()
            ParameterPage.FUNCTIONS -> functionsControl.reset()
            ParameterPage.MISCELLANEOUS -> miscellaneousControl.reset()
            ParameterPage.SECURITY -> {
                securityControl.reset()
                securityName = ""
                securityPassword = ""
                securityKey0 = ""
                securityKey1 = ""
                securityError = ""
            }
            ParameterPage.CHOKE_CONTROL -> chokeControl.reset()
            ParameterPage.GAS_DOSE -> gasDoseControl.reset()
            ParameterPage.EL_THROTTLE -> dbwControl.reset()
            ParameterPage.FUEL_INJECTION -> fuelInjectionControl.reset()
            ParameterPage.UNIVERSAL_OUTPUTS -> universalOutputsControl.reset()
        }
    }

    private fun resetAllStateHolders() {
        ParameterPage.entries.forEach(::resetStateHolder)
    }

    private fun applyPageResult(page: ParameterPage, result: ParameterReadResult) {
        when (page) {
            ParameterPage.STARTER -> starterControl.receive(packet<StarterParamPacket>(result))
            ParameterPage.ANGLES -> anglesControl.receive(packet(result))
            ParameterPage.IDLING -> idlingControl.receive(packet<IdlingParamPacket>(result))
            ParameterPage.TEMPERATURE -> temperatureControl.receive(packet<TemperatureParamPacket>(result))
            ParameterPage.FUEL_CUTOFF -> carburControl.receive(packet<CarburParamPacket>(result))
            ParameterPage.CKPS -> ckpsControl.receive(packet<CkpsParamPacket>(result))
            ParameterPage.ADC_ERRORS -> adcCorrectionsControl.receive(packet<AdcCorrectionsParamPacket>(result))
            ParameterPage.KNOCK -> knockControl.receive(packet<KnockParamPacket>(result))
            ParameterPage.LAMBDA_CONTROL -> lambdaControl.receive(packet<LambdaParamPacket>(result))
            ParameterPage.ACCELERATION -> accelerationControl.receive(packet<AccelerationParamPacket>(result))
            ParameterPage.LTFT -> ltftControl.receive(packet<LtftParamPacket>(result))
            ParameterPage.FUNCTIONS -> {
                val functionParameters = (result as ParameterReadResult.Functions).parameters
                functionsControl.receive(functionParameters)
            }
            ParameterPage.MISCELLANEOUS -> miscellaneousControl.receive(packet<MiscellaneousParamPacket>(result))
            ParameterPage.SECURITY -> {
                securityControl.receive(packet(result))
                val current = securityControl.values.value ?: return
                securityName = ""
                securityPassword = ""
                securityKey0 = current.key0
                securityKey1 = current.key1
                securityError = ""
            }
            ParameterPage.CHOKE_CONTROL -> {
                chokeControl.receive(packet<ChokeControlParPacket>(result))
            }
            ParameterPage.GAS_DOSE -> {
                gasDoseControl.receive(packet<GasDoseParamPacket>(result))
            }
            ParameterPage.EL_THROTTLE -> dbwControl.receive(packet<DbwParamPacket>(result))
            ParameterPage.FUEL_INJECTION -> fuelInjectionControl.receive(packet<InjctrParPacket>(result))
            ParameterPage.UNIVERSAL_OUTPUTS -> universalOutputsControl.receive(packet<UniOutParamPacket>(result))
        }
    }

    private inline fun <reified T : org.secu3.android.models.packets.base.InputPacket> packet(result: ParameterReadResult): T =
        (result as ParameterReadResult.Packet).packet as T

    fun savePacket() = parameterSession.save()

    internal fun showUiMessage(textRes: Int, duration: UiMessageDuration) {
        uiMessageQueue.publish(textRes, duration, scope = PARAMETERS_ROUTE)
    }
}
