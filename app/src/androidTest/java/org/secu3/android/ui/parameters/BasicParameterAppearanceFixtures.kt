package org.secu3.android.ui.parameters

import org.secu3.android.domain.parameters.TemperatureValues

import androidx.compose.runtime.Composable
import org.secu3.android.domain.parameters.StarterValues
import org.secu3.android.domain.parameters.AnglesValues
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
            IdlingUiState.Data(
                IdlingValues(
                    numbers = IdlingNumber.entries.associateWith { it.ordinal + 1.5f },
                    flags = IdlingFlag.entries.associateWith { it.ordinal % 2 == 0 },
                ),
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
            DbwUiState.Data(
                DbwValues(DbwNumber.entries.associateWith { it.ordinal + 1f }, homePosition = 12.5f),
            ),
            onEdit = {},
        )
    },
    ParameterAppearanceFixture("ltft") {
        LtftScreen(
            LtftUiState.Data(LtftValues(LtftNumber.entries.associateWith { it.ordinal + 1f }, mode = 0)),
            onEdit = {},
            onMode = {},
        )
    },
    ParameterAppearanceFixture("knock") {
        KnockScreen(
            KnockUiState.Data(
                KnockValues(
                    numbers = KnockNumber.entries.associateWith { it.ordinal + 1.25f },
                    selectedChannels = 0xA5,
                    bpfIndex = 0,
                    integrationIndex = 0,
                    bpfOptions = listOf("1 kHz", "2 kHz"),
                    integrationOptions = listOf("10 ms", "20 ms"),
                ),
                enabled = true,
            ),
            onEdit = {},
            onEnable = {},
            onChannel = { _, _ -> },
            onSelector = { _, _ -> },
        )
    },
    ParameterAppearanceFixture("carbur") {
        CarburScreen(
            CarburUiState.Data(
                CarburValues(
                    numbers = CarburNumber.entries.associateWith { it.ordinal + 1f },
                    invers = false,
                    fuelCutUni = 1,
                    ignitionCutUni = 15,
                ),
            ),
            options = listOf("1", "2", "3", "4", "5", "6", "no"),
            onEdit = {},
            onInvers = {},
            onSelector = { _, _ -> },
        )
    },
)
