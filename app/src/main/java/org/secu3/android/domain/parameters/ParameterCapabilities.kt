/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.parameters

import org.secu3.android.models.packets.input.FirmwareInfoPacket

/** Immutable firmware capability snapshot shared by parameter presentation and command guards. */
class ParameterCapabilities(firmware: FirmwareInfoPacket? = null) {
    val known = firmware != null
    val fuelInjection = firmware?.isFuelInjectEnabled ?: true
    val gasDose = firmware?.isGdControlEnabled ?: true
    val choke = firmware?.isSmControlEnabled ?: true
    val secu3T = firmware?.isSecu3T ?: false
    val fanPwm = firmware?.isCoolingFanPwmEnabled ?: true
    val tempCurve = firmware?.isThermistorCsEnabled ?: true
    val extraAnalogInputs = firmware?.isTPIC8101 ?: true
    val ckps = firmware?.let { !it.isHallSyncEnabled && !it.isCkpsNplus1Enabled } ?: true
    val hallWindow = !known || !ckps
    val hallOutput = firmware?.let { it.isHallOutputEnabled && ckps } ?: true
    val ignitionCogs = firmware?.let { !it.isDwellControlSupported && !it.isCkps2ChignEnabled && ckps } ?: true
    val inputsMerging = firmware?.let { !it.isCkps2ChignEnabled && ckps } ?: true
    val risingSpark = firmware?.let { it.isDwellControlSupported && !it.isCkps2ChignEnabled } ?: true
    val camRef = firmware?.let { it.isPhaseSensorEnabled && ckps } ?: true
    val cogsBtdc = firmware?.let { ckps && it.options.and(1 shl 2) == 0 } ?: true
    val mttf = firmware?.let { !it.isHallSyncEnabled } ?: true
    val oddCylinders = firmware?.let { it.isPhasedIgnitionEnabled || it.isHallSyncEnabled } ?: true
    val dbw get() = !secu3T
    val sevenCylinders = firmware?.let { it.options.and(1 shl 2) != 0 } ?: true
    private val carbAfr = firmware?.isCarbAfrEnabled ?: false

    val carburetor get() = !known || !fuelInjection
    val fuelOrGasDose get() = fuelInjection || gasDose
    val floodClearThreshold get() = fuelInjection || gasDose || choke
    val nonSecu3TFuelInjection get() = fuelInjection && dbw
    val lambdaControl get() = known && (fuelInjection || gasDose || carbAfr)
    val lambdaChannelSelection get() = lambdaControl
    val lambdaStoich get() = known && fuelOrGasDose

    fun functionsLowerLoadEnabled(useLoadGrid: Boolean): Boolean = !useLoadGrid

    fun functionsUpperLoadEnabled(useLoadGrid: Boolean, loadSource: Int): Boolean = !useLoadGrid && loadSource != 1

    fun adcChannelEnabled(channel: Int): Boolean = when (channel) {
        in 0..5 -> true
        7 -> extraAnalogInputs
        in 6..11 -> dbw
        else -> false
    }

    fun supports(page: ParameterPage): Boolean = when (page) {
        ParameterPage.CHOKE_CONTROL -> !known || choke
        ParameterPage.FUEL_INJECTION -> known && fuelInjection
        ParameterPage.LAMBDA_CONTROL -> lambdaControl
        ParameterPage.ACCELERATION -> known && fuelOrGasDose
        ParameterPage.GAS_DOSE -> known && gasDose
        ParameterPage.LTFT -> known && fuelInjection
        ParameterPage.EL_THROTTLE -> known && dbw
        else -> true
    }

    fun iacAdditionEnabled(throttleAsMap: Boolean): Boolean = fuelInjection && !throttleAsMap

    fun accelerationTimeEnabled(aeType: Int): Boolean = fuelInjection && aeType == 1

    fun mttfEnabled(missingTeeth: Float): Boolean =
        !known || (mttf && (!ckps || missingTeeth != 0f))

    fun camReferenceEnabled(missingTeeth: Float): Boolean =
        !known || (camRef && missingTeeth == 0f)

    fun ckpsCylinderOptions(mergeOutputs: Boolean): List<Int> {
        val odd = oddCylinders || mergeOutputs
        return (1..8).filter { cylinder ->
            if (cylinder == 7) odd && sevenCylinders else cylinder % 2 == 0 || odd
        }
    }

    companion object {
        val All = ParameterCapabilities(null)
    }
}
