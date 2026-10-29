package org.secu3.android.ui.parameters

import org.secu3.android.domain.parameters.TemperatureValues
import org.secu3.android.domain.parameters.IdlingValues

import androidx.compose.runtime.Composable
import org.secu3.android.domain.parameters.StarterValues
import org.secu3.android.domain.parameters.AnglesValues
import org.secu3.android.domain.parameters.CarburValues
import org.secu3.android.domain.parameters.KnockValues
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.ui.parameters.pages.compose.*

/** Loaded UI samples for rendering every basic parameter page outside ECU navigation. */
internal fun basicParameterAppearanceFixtures(): List<ParameterAppearanceFixture> = listOf(
    ParameterAppearanceFixture("starter") {
        StarterScreen(
            StarterValues(1, 2, 3f, 4f, 5, 6, 7f, 8f, 9f, 10f, 11, false, true),
            onEdit = {},
            onFlagChanged = { _, _ -> },
        )
    },
    ParameterAppearanceFixture("angles") {
        AnglesScreen(
            AnglesValues(1.25f, 2.25f, 3.25f, 4.25f, 5.25f, 0, false, true, false, 6.25f),
            onEdit = {},
            onFlagChanged = { _, _ -> },
        )
    },
    ParameterAppearanceFixture("idling") {
        IdlingScreen(
            IdlingValues(
                iFac1 = 1.5f, iFac2 = 2.5f, maxAngle = 3.5f, minAngle = 4.5f, targetRpm = 5,
                deadBandRpm = 6, turnOnTemperature = 7.5f, iacAddAfterExit = 8.5f, rpmAddOnRun = 9,
                proportionalNegative = 1.1f, integralNegative = 1.2f, proportionalPositive = 1.3f,
                integralPositive = 1.4f, transientThreshold1 = 1.5f, transientThreshold2 = 1.6f,
                integratorRpmLimit = 12, mapThreshold = 13.5f, iacMinimumPosition = 14.5f,
                iacMaximumPosition = 15.5f, iacDeadBandRpm = 16, differential = 1.7f,
                rigidityLoad = 1.8f, rigidityRpm = 1.9f, loadThreshold = 20.5f,
                regulatorEnabled = true, regulatorOnGas = false, proportionalMode = true,
                closedLoop = false, throttleAssistMap = true, closedLoopOnGas = false,
                worksWithIacRegulator = true,
            ),
            onEdit = {},
            onFlagChanged = { _, _ -> },
        )
    },
    ParameterAppearanceFixture("temperature") {
        TemperatureScreen(
            TemperatureValues(20f, 25f, 100, 2f, 3f, 900, 60f, true, false, true),
            onEdit = {},
            onFlagChanged = { _, _ -> },
        )
    },
    ParameterAppearanceFixture("adc-errors-corrections") {
        AdcErrorsCorrectionsScreen(
            AdcCorrectionsUiState.Data(
                AdcCorrectionsValues(
                    numbers = AdcNumber.entries.associateWith { 1f + it.ordinal * 0.125f },
                    adcCompMode = true,
                ),
            ),
            onEdit = {},
            onCompModeChanged = {},
        )
    },
    ParameterAppearanceFixture("dbw") {
        DbwScreen(
            org.secu3.android.domain.parameters.DbwValues(1f, 2f, 3f, 45f, 50f, .12f, 6f, 7f, 8f, 9f, 12.5f),
            org.secu3.android.domain.parameters.ParameterCapabilities.All,
            onEdit = {},
        )
    },
    ParameterAppearanceFixture("ltft") {
        LtftScreen(
            org.secu3.android.domain.parameters.LtftValues(0, 1f, 2f, 3f, .25f, 100f, 12f, -10f, 15f,
                800, 5000, 20f, 300f, 1.2f, 3.4f),
            onEdit = {},
            onMode = {},
        )
    },
    ParameterAppearanceFixture("knock") {
        KnockScreen(
            values = KnockValues.from(KnockParamPacket(
                useKnockChannel = 1, bpfFrequency = 0, kWndBeginAngle = 1.25f, kWndEndAngle = 2.25f,
                intTimeCost = 0, retardStep = 3.25f, advanceStep = 4.25f, maxRetard = 5.25f,
                threshold = 1.5f, recoveryDelay = 8, selectedChanels = 0xA5, knkctlThrd = 9.25f,
            )),
            bpfOptions = listOf("1 kHz", "2 kHz"),
            integrationOptions = listOf("10 ms", "20 ms"),
            onEdit = {},
            onEnable = {},
            onChannel = { _, _ -> },
            onSelector = { _, _ -> },
        )
    },
    ParameterAppearanceFixture("carbur") {
        CarburScreen(
            CarburValues(800, 1200, 850, 1250, .25f, 6.25f, 5f, 20f, 15f, 8000, 8100, false, 0, 15),
            options = listOf("1", "2", "3", "4", "5", "6", "no"),
            selectorCodes = listOf(0, 1, 2, 3, 4, 5, 15),
            onEdit = {},
            onInvers = {},
            onSelector = { _, _ -> },
        )
    },
)
