package org.secu3.android.ui.parameters

import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.ui.parameters.pages.compose.*

/** Null firmware is permissive for loading/standalone previews; connected pages use the ECU options. */
data class ParameterCapabilities(
    val known: Boolean = false,
    val fuelInjection: Boolean = true,
    val gasDose: Boolean = true,
    val choke: Boolean = true,
    val secu3T: Boolean = false,
    val fanPwm: Boolean = true,
    val tempCurve: Boolean = true,
    val hallOutput: Boolean = true,
    val extraAnalogInputs: Boolean = true,
    val ckps: Boolean = true,
    val hallWindow: Boolean = true,
    val ignitionCogs: Boolean = true,
    val inputsMerging: Boolean = true,
    val risingSpark: Boolean = true,
    val camRef: Boolean = true,
    val cogsBtdc: Boolean = true,
    val mttf: Boolean = true,
    val oddCylinders: Boolean = true,
    val dbw: Boolean = true,
    val sevenCylinders: Boolean = true,
) {
    val carburetor: Boolean get() = !known || !fuelInjection
    companion object { val All = ParameterCapabilities() }
}

fun parameterCapabilities(fw: FirmwareInfoPacket?): ParameterCapabilities = fw?.let {
    val ckps = !it.isHallSyncEnabled && !it.isCkpsNplus1Enabled
    ParameterCapabilities(
        known = true, fuelInjection = it.isFuelInjectEnabled, gasDose = it.isGdControlEnabled,
        choke = it.isSmControlEnabled, secu3T = it.isSecu3T,
        fanPwm = it.isCoolingFanPwmEnabled, tempCurve = it.isThermistorCsEnabled,
        hallOutput = it.isHallOutputEnabled && ckps, extraAnalogInputs = it.isTPIC8101,
        ckps = ckps, hallWindow = !ckps,
        ignitionCogs = !it.isDwellControlSupported && !it.isCkps2ChignEnabled && ckps,
        inputsMerging = !it.isCkps2ChignEnabled && ckps,
        risingSpark = it.isDwellControlSupported && !it.isCkps2ChignEnabled,
        camRef = it.isPhaseSensorEnabled && ckps,
        // COPT_ODDFIRE_ALGO occupies bit 2 (also used by the desktop controller).
        cogsBtdc = ckps && it.options.and(1 shl 2) == 0,
        mttf = !it.isHallSyncEnabled, oddCylinders = it.isPhasedIgnitionEnabled || it.isHallSyncEnabled,
        dbw = !it.isSecu3T, sevenCylinders = it.options.and(1 shl 2) != 0,
    )
} ?: ParameterCapabilities.All

internal fun starterNumberEnabled(n: StarterNumber, c: ParameterCapabilities): Boolean = when (n) {
    StarterNumber.CRANK_TO_RUN_COLD, StarterNumber.CRANK_TO_RUN_HOT,
    StarterNumber.PRIME_COLD, StarterNumber.PRIME_HOT, StarterNumber.PRIME_DELAY -> c.fuelInjection
    StarterNumber.AFTERSTART_PETROL, StarterNumber.AFTERSTART_GAS -> c.fuelInjection || c.gasDose
    StarterNumber.FLOOD_CLEAR_THRESHOLD -> c.fuelInjection || c.gasDose || c.choke
    else -> true
}
internal fun idlingNumberEnabled(n: IdlingNumber, v: IdlingValues, c: ParameterCapabilities): Boolean = when (n) {
    IdlingNumber.IFAC1, IdlingNumber.IFAC2, IdlingNumber.MAX_ANGLE, IdlingNumber.MIN_ANGLE,
    IdlingNumber.RPM, IdlingNumber.DEAD_BAND, IdlingNumber.ON_TEMP -> true
    IdlingNumber.IAC_ADD -> c.fuelInjection && !v.flags.getValue(IdlingFlag.THRASSMAP)
    else -> c.fuelInjection
}
internal fun ckpsNumberEnabled(n: CkpsNumber, v: CkpsValues, c: ParameterCapabilities): Boolean = when (n) {
    CkpsNumber.WHEEL, CkpsNumber.MISSING -> c.ckps
    CkpsNumber.PULSE -> c.ignitionCogs
    CkpsNumber.HALL_WIDTH, CkpsNumber.DEGREES -> c.hallWindow
    CkpsNumber.MTTF -> !c.known || (c.mttf && (!c.ckps || v.numbers.getValue(CkpsNumber.MISSING) != 0f))
}
