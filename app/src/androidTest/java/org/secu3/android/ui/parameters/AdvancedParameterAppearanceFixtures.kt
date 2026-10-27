package org.secu3.android.ui.parameters

import androidx.compose.runtime.Composable
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.domain.parameters.MiscellaneousControl
import org.secu3.android.domain.parameters.MiscellaneousValues
import org.secu3.android.ui.parameters.pages.MiscField
import org.secu3.android.ui.parameters.pages.MiscellaneousScreen
import org.secu3.android.ui.parameters.pages.SecurityScreen
import org.secu3.android.ui.parameters.pages.UniversalOutputValues
import org.secu3.android.ui.parameters.pages.UniversalOutputsScreen
import org.secu3.android.ui.parameters.pages.UniversalOutputsValues
import org.secu3.android.ui.parameters.pages.compose.AccelerationNumber
import org.secu3.android.ui.parameters.pages.compose.AccelerationScreen
import org.secu3.android.ui.parameters.pages.compose.AccelerationUiState
import org.secu3.android.ui.parameters.pages.compose.AccelerationValues
import org.secu3.android.ui.parameters.pages.compose.CkpsScreen
import org.secu3.android.domain.parameters.CkpsValues
import org.secu3.android.ui.parameters.pages.compose.ChokeScreen
import org.secu3.android.domain.parameters.ChokeValues
import org.secu3.android.domain.parameters.FuelBankValues
import org.secu3.android.ui.parameters.pages.compose.FuelInjectionScreen
import org.secu3.android.domain.parameters.FuelInjectionValues
import org.secu3.android.ui.parameters.pages.compose.FunctionsChoices
import org.secu3.android.ui.parameters.pages.compose.FunctionsNumber
import org.secu3.android.ui.parameters.pages.compose.FunctionsScreen
import org.secu3.android.ui.parameters.pages.compose.FunctionsUiState
import org.secu3.android.ui.parameters.pages.compose.FunctionsValues
import org.secu3.android.ui.parameters.pages.compose.GasDoseScreen
import org.secu3.android.domain.parameters.GasDoseValues
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.ui.parameters.pages.compose.LambdaControlScreen
import org.secu3.android.ui.parameters.pages.compose.LambdaFlag
import org.secu3.android.ui.parameters.pages.compose.LambdaNumber
import org.secu3.android.ui.parameters.pages.compose.LambdaValues

internal fun advancedParameterAppearanceFixtures(): List<ParameterAppearanceFixture> {
    val functionsPacket = FunSetParamPacket()
    val functionNames = listOf("Speed density", "Alpha-N", "Blend")
    val functionChoices = FunctionsChoices.from(
        functionsPacket,
        names = functionNames,
        loads = listOf("MAP", "TPS", "MAF", "RPM", "Blend"),
        bars = listOf("None", "Atmospheric", "Barometric", "Combined"),
        ve2 = listOf("VE table 1", "VE table 2", "Blend"),
    )
    val functions = FunctionsValues(
        FunctionsNumber.entries.associateWith { number ->
            when (number) {
                FunctionsNumber.LOWER -> 20f
                FunctionsNumber.UPPER -> 100f
                else -> 1.25f
            }
        },
        useLoadGrid = true,
    )
    val ckps = CkpsValues(60, 2, 5, 12f, 40f, 2.5f, false, true, true, false, false, 24, 4, (11..55).toList())
    val lambda = LambdaValues(
        numbers = LambdaNumber.entries.associateWith { number -> if (number.integer) 4f else 1.25f },
        flags = LambdaFlag.entries.associateWith { it == LambdaFlag.HEATING_VOLTAGE },
        sensorType = 0,
        channels = listOf(true, true, false, false, true, false, false, false),
    )
    val bank0 = FuelBankValues(240f, 1, 2, 360f, 10f, false, 0, 0.8f, 8f, listOf(0, 1, 2), listOf(1, 2, 3))
    val bank1 = FuelBankValues(240f, 2, 2, 360f, 10f, true, 1, 0.8f, 8f, listOf(0, 1, 2), listOf(1, 2, 3))
    val fuel = FuelInjectionValues(2.0f, listOf(bank0, bank1), 5500f, true, true, false, false, true, 2f, 2f)
    val acceleration = AccelerationValues(
        numbers = AccelerationNumber.entries.associateWith { 10f },
        aeType = 1,
        balance = 50f,
        wallwetModel = 1,
    )
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
                loaded = true,
                useBt = true,
                useImm = true,
                name = "ECU",
                pass = "1234",
                key0 = "010203040506",
                key1 = "AABBCCDDEEFF",
                type = 0,
                types = listOf("Bluetooth module A", "Bluetooth module B"),
                expanded = false,
                error = "",
                useReserveParams = true,
                checkFirmwareCrc = false,
                expand = {}, selectType = {}, toggleBt = {}, toggleImm = {}, changeName = {}, changePass = {},
                changeKey0 = {}, changeKey1 = {}, apply = {},
            )
        },
        ParameterAppearanceFixture("universal-outputs") {
            UniversalOutputsScreen(outputs, outputNames, listOf("OR", "AND", "XOR", "NOR", "AND"), { _, _ -> }, {})
        },
        ParameterAppearanceFixture("functions") {
            FunctionsScreen(FunctionsUiState.Data(functions, functionChoices), {}, {}, { _, _ -> })
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
            LambdaControlScreen(lambda, listOf("Narrowband", "Wideband"), true, {}, { _, _ -> }, {}, { _, _ -> })
        },
        ParameterAppearanceFixture("acceleration") {
            AccelerationScreen(AccelerationUiState.Data(acceleration), {}, {}, {}, {})
        },
    )
}
