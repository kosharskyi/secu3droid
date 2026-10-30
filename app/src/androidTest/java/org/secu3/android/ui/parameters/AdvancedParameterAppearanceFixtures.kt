package org.secu3.android.ui.parameters

import androidx.compose.runtime.Composable
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.domain.parameters.UniversalOutputValues
import org.secu3.android.domain.parameters.UniversalOutputsValues
import org.secu3.android.domain.parameters.UniversalOutputConditions
import org.secu3.android.domain.parameters.MiscellaneousControl
import org.secu3.android.domain.parameters.MiscellaneousValues
import org.secu3.android.domain.parameters.SecurityValues
import org.secu3.android.ui.parameters.pages.MiscField
import org.secu3.android.ui.parameters.pages.MiscellaneousScreen
import org.secu3.android.ui.parameters.pages.SecurityScreen
import org.secu3.android.ui.parameters.pages.UniversalOutputsScreen
import org.secu3.android.ui.parameters.pages.compose.AccelerationNumber
import org.secu3.android.ui.parameters.pages.compose.AccelerationScreen
import org.secu3.android.domain.parameters.AccelerationValues
import org.secu3.android.ui.parameters.pages.compose.CkpsScreen
import org.secu3.android.domain.parameters.CkpsValues
import org.secu3.android.ui.parameters.pages.compose.ChokeScreen
import org.secu3.android.domain.parameters.ChokeValues
import org.secu3.android.domain.parameters.FuelBankValues
import org.secu3.android.ui.parameters.pages.compose.FuelInjectionScreen
import org.secu3.android.domain.parameters.FuelInjectionValues
import org.secu3.android.domain.parameters.FunctionsValues
import org.secu3.android.ui.parameters.pages.compose.FunctionsScreen
import org.secu3.android.ui.parameters.pages.compose.GasDoseScreen
import org.secu3.android.domain.parameters.GasDoseValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.domain.parameters.LambdaValues
import org.secu3.android.ui.parameters.pages.compose.LambdaControlScreen
import org.secu3.android.ui.parameters.pages.compose.LambdaFlag
import org.secu3.android.ui.parameters.pages.compose.LambdaNumber

internal fun advancedParameterAppearanceFixtures(): List<ParameterAppearanceFixture> {
    val functionsPacket = FunSetParamPacket()
    val functionNames = listOf("Speed density", "Alpha-N", "Blend")
    functionsPacket.loadLower = 20f
    functionsPacket.loadUpper = 100f
    functionsPacket.mapCurveOffset = 1.25f
    functionsPacket.useLoadGrid = true
    val functions = FunctionsValues.from(functionsPacket, functionNames)
    val ckps = CkpsValues(60, 2, 5, 12f, 40f, 2.5f, false, true, true, false, false, 24, 4, (11..55).toList())
    val lambda = LambdaValues(
        strokesPerStep = 4, msPerStep = 40, positiveStep = 1.25f, negativeStep = 1.25f,
        positiveLimit = 1.25f, negativeLimit = 1.25f, switchPoint = 1.25f, coolantThreshold = 1.25f,
        rpmThreshold = 4, activationDelay = 4, deadband = 1.25f, sensorTypeCode = 0,
        stoichiometricRatio = 1.25f, coldHeatingTime = 4, hotHeatingTime = 4,
        heatingTemperature = 4, heatingPwmTime = 1.25f, airFlowThreshold = 4f,
        heatingVoltage = true, idlingCorrection = false, beforeCranking = false, mixSensors = false,
        selectedChannels = listOf(true, true, false, false, true, false, false, false),
    )
    val bank0 = FuelBankValues(240f, 1, 2, 360f, 10f, false, 0, 0.8f, 8f, listOf(0, 1, 2), listOf(1, 2, 3))
    val bank1 = FuelBankValues(240f, 2, 2, 360f, 10f, true, 1, 0.8f, 8f, listOf(0, 1, 2), listOf(1, 2, 3))
    val fuel = FuelInjectionValues(2.0f, listOf(bank0, bank1), 5500f, true, true, false, false, true, 2f, 2f)
    val acceleration = AccelerationValues(10, 100f, 10, 1, 10, 50f, 10, -10f, -10f, 1)
    val outputNames = mapOf(0 to "RPM", 1 to "Coolant temperature", 2 to "Throttle position", 3 to "Vehicle speed")
    val outputs = UniversalOutputsValues(
        outputs = List(6) { index ->
            UniversalOutputValues(
                enabled = index == 0,
                condition1 = 0,
                condition2 = 1,
                condition1Inversion = false,
                condition2Inversion = false,
                inversion = false,
                logicFunction = 0,
                onThreshold1 = 1000f,
                offThreshold1 = 900f,
                onThreshold2 = 95f,
                offThreshold2 = 90f,
            )
        },
        logicFunction12 = 0,
    )

    return listOf(
        ParameterAppearanceFixture("miscellaneous") {
            MiscellaneousScreen(
                MiscellaneousValues(0x0015, 100, true, 6000, 10f, 20f, true, false, true, 500, 20000, 100 to 3500, 17000 to 2_000_000, 2.5f, 200, 400, 4f, false),
                rates = MiscellaneousControl.BAUD_RATES.mapValues { it.value.toString() },
                intEdit = {},
                floatEdit = {},
                baud = {},
                toggleAction = { _, _ -> },
            )
        },
        ParameterAppearanceFixture("security") {
            SecurityScreen(
                values = SecurityValues(true, true, 0, true, false, "010203040506", "AABBCCDDEEFF"),
                name = "ECU",
                pass = "1234",
                key0 = "010203040506",
                key1 = "AABBCCDDEEFF",
                types = listOf("Bluetooth module A", "Bluetooth module B"),
                expanded = false,
                error = "",
                isKeyValid = { value -> value.length == 12 && value.all { it.digitToIntOrNull(16) != null } },
                expand = {}, selectType = {}, toggleBt = {}, toggleImm = {}, changeName = {}, changePass = {},
                changeKey0 = {}, changeKey1 = {}, apply = {},
            )
        },
        ParameterAppearanceFixture("universal-outputs") {
            UniversalOutputsScreen(outputs, outputNames, listOf("OR", "AND", "XOR", "NOR", "AND"), UniversalOutputConditions(), { _, _ -> }, { _, _, _ -> }, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, { _, _, _, _, _ -> }, {})
        },
        ParameterAppearanceFixture("functions") {
            FunctionsScreen(functions, {}, {}, { _, _ -> })
        },
        ParameterAppearanceFixture("ckps") {
            CkpsScreen(ckps, {}, { _, _ -> }, { _, _ -> })
        },
        ParameterAppearanceFixture("choke") {
            ChokeScreen(ChokeValues(500, .5f, 2f, 3f, 4f, false, false, false, false, false, 1), {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
        },
        ParameterAppearanceFixture("gas-dose") {
            GasDoseScreen(GasDoseValues(500f, 14.7f, 20f, 15f, 15f, false, 1, true), ParameterCapabilities.All, {}, {}, {}, {}, {}, {}, {}, {})
        },
        ParameterAppearanceFixture("fuel-injection") {
            FuelInjectionScreen(fuel, mapOf(0 to "Batch", 1 to "Semi-sequential", 2 to "Sequential"), {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, { _, _ -> })
        },
        ParameterAppearanceFixture("lambda-control") {
            LambdaControlScreen(lambda, listOf("Narrowband", "Wideband"), ParameterCapabilities(FirmwareInfoPacket(options = (1 shl 28) or (1 shl 30))), {}, { _, _ -> }, {}, { _, _ -> })
        },
        ParameterAppearanceFixture("acceleration") {
            AccelerationScreen(acceleration, {}, {}, {}, {})
        },
    )
}
