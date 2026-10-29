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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.secu3.android.domain.parameters.FunctionsValues
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme

class FunctionsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loadsScrollsAndRecompositionDoesNotInvokeActions() {
        var editCount = 0
        var flagChangeCount = 0
        var choiceCount = 0
        val state = mutableStateOf<FunctionsValues?>(null)

        compose.setContent {
            SecuComposeTheme {
                FunctionsScreen(
                    state.value,
                    onEdit = { editCount++ },
                    onFlag = { flagChangeCount++ },
                    onChoice = { _, _ -> choiceCount++ },
                )
            }
        }

        compose.onNodeWithTag("functions_loading").assertIsDisplayed()
        compose.runOnIdle { state.value = createDataState() }

        FunctionsNumber.entries.forEach { number ->
            compose.onNodeWithTag("functions_${number.name.lowercase()}")
                .performScrollTo()
                .assertIsDisplayed()
        }
        selectorTags.forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }

        compose.onNodeWithTag("functions_gasoline").performScrollTo()
        val gasolineTop = compose.onNodeWithTag("functions_gasoline")
            .fetchSemanticsNode().boundsInRoot.top
        val gasTop = compose.onNodeWithTag("functions_gas")
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue("GASOLINE_MAP should precede GAS_MAP", gasolineTop < gasTop)

        compose.onNodeWithTag("functions_gas").performScrollTo()
        val gasRowTop = compose.onNodeWithTag("functions_gas")
            .fetchSemanticsNode().boundsInRoot.top
        val tpsOffsetTop = compose.onNodeWithTag("functions_tps_offset")
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            "GAS_MAP should precede TPS_OFFSET, got $gasRowTop, $tpsOffsetTop",
            gasRowTop < tpsOffsetTop,
        )

        compose.runOnIdle { state.value = createDataState() }
        assertEquals(0, editCount)
        assertEquals(0, flagChangeCount)
        assertEquals(0, choiceCount)
    }

    @Test
    fun userEditsFlagsAndDropdownSelectionsInvokeOnlyTheirCallbacks() {
        val edits = mutableListOf<FunctionsNumber>()
        val choices = mutableListOf<Pair<FunctionsChoices.Selector, Int>>()
        var flagChangeCount = 0

        compose.setContent {
            SecuComposeTheme {
                FunctionsScreen(
                    createDataState(),
                    onEdit = { edits += it },
                    onFlag = { flagChangeCount++ },
                    onChoice = { selector, code -> choices += selector to code },
                )
            }
        }

        FunctionsNumber.entries.forEach { number ->
            compose.onNodeWithTag("functions_${number.name.lowercase()}")
                .performScrollTo()
                .performClick()
        }
        assertEquals(FunctionsNumber.entries.toList(), edits)

        compose.onNodeWithTag("functions_load_grid-switch")
            .performScrollTo()
            .performClick()
        assertEquals(1, flagChangeCount)

        FunctionsChoices.Selector.entries.forEachIndexed { index, selector ->
            compose.onNodeWithTag(selectorTags[index]).performScrollTo().performClick()
            compose.onNodeWithTag("functions_option_1").performClick()
        }
        assertEquals(FunctionsChoices.Selector.entries.map { it to 1 }, choices)
    }

    @Test
    fun outputSelectorLabelsUseWireCodesAndUnknownIncomingCodesStayBlank() {
        val values = createDataState().copy(
            mapselPetrol = 6,
            mapselGas = 15,
            gasVUni = 6,
        )
        val choices = FunctionsChoices.from(values, listOf("MAP", "TPS", "MAF", "RPM", "Blend"),
            listOf("None", "Atmospheric", "Barometric", "Combined"), listOf("VE 1", "VE 2", "Blend"))
        for (selector in listOf(
            FunctionsChoices.Selector.MAPSEL_PETROL,
            FunctionsChoices.Selector.MAPSEL_GAS,
            FunctionsChoices.Selector.GAS_V_UNI,
        )) {
            assertEquals((0..5).map { FunctionsChoices.Option(it, (it + 1).toString()) } + FunctionsChoices.Option(15, "no"), choices.options.getValue(selector))
        }
        assertNull(choices.selected[FunctionsChoices.Selector.MAPSEL_PETROL])
        assertEquals("no", choices.selected[FunctionsChoices.Selector.MAPSEL_GAS])
        assertNull(choices.selected[FunctionsChoices.Selector.GAS_V_UNI])
    }

    @Test
    fun dropdownFieldsLookClickableAndExposeTheirLabelAndSelectionAccessibly() {
        val longMapName = "Map calibration with a deliberately long localized name"
        val populatedState = createDataState().copy(functionNames = listOf(longMapName))

        compose.setContent {
            SecuComposeTheme {
                FunctionsScreen(populatedState, onEdit = {}, onFlag = {}, onChoice = { _, _ -> })
            }
        }

        val field = compose.onNodeWithTag("functions_gasoline").performScrollTo()
        val label = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(org.secu3.android.R.string.funset_maps_set_for_gasoline)
        field.assertIsDisplayed()
            .assertHasClickAction()
            .assertContentDescriptionEquals("$label, $longMapName")
            .assertTextContains(longMapName)
        compose.onNodeWithTag(
            "functions_gasoline-dropdown-indicator",
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun longUkrainianSelectionWrapsAtNarrowWidthAndLargeFontInBothThemes() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag("uk"))
        }
        val localizedContext = context.createConfigurationContext(configuration)
        val longName = "Калібрування паливної системи для тривалого прогрівання двигуна"
        val label = localizedContext.getString(org.secu3.android.R.string.funset_maps_set_for_gasoline)
        val state = createDataState().copy(functionNames = listOf(longName))
        val dark = mutableStateOf(false)
        var selected = 0
        val baseDensity = compose.density.density
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalResources provides localizedContext.resources,
                LocalDensity provides Density(baseDensity, fontScale = 1.7f),
            ) {
                SecuComposeTheme(darkTheme = dark.value) {
                    Box(Modifier.width(320.dp)) {
                        FunctionsScreen(state, onEdit = {}, onFlag = {}, onChoice = { _, _ -> selected++ })
                    }
                }
            }
        }

        listOf(false, true).forEach { darkTheme ->
            compose.runOnIdle { dark.value = darkTheme }
            compose.onNodeWithTag("functions_gasoline").performScrollTo()
                .assertHasClickAction()
                .assertContentDescriptionEquals("$label, $longName")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, longName))
            assertTextDoesNotOverflow("functions_gasoline-label")
            assertTextDoesNotOverflow("functions_gasoline-value")
            compose.onNodeWithTag("functions_gasoline").performClick()
            compose.onNodeWithTag("functions_option_0").performClick()
        }
        compose.runOnIdle { assertEquals(2, selected) }
    }

    private fun assertTextDoesNotOverflow(tag: String) {
        var result: TextLayoutResult? = null
        compose.onNodeWithTag(tag, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val results = mutableListOf<TextLayoutResult>()
                action(results)
                result = results.firstOrNull()
            }
        assertTrue("$tag exposes its text layout", result != null)
        assertTrue("$tag fits without clipping", !requireNotNull(result).hasVisualOverflow)
    }

    private fun createDataState() = FunctionsValues(
        fnGasoline = 0, fnGas = 0,
        loadLower = 10f, loadUpper = 50f,
        mapCurveOffset = 0f, mapCurveGradient = 0f,
        tpsCurveOffset = 0f, tpsCurveGradient = 0f,
        map2CurveOffset = 0f, map2CurveGradient = 0f,
        gpsCurveOffset = 0f, gpsCurveGradient = 0f,
        fpsCurveOffset = 0f, fpsCurveGradient = 0f,
        apps1CurveOffset = 0f, apps1CurveGradient = 0f,
        loadSource = 0, mapselPetrol = 0, mapselGas = 0, barocorrType = 0,
        useLoadGrid = false, ve2MapFunc = 0, gasVUni = 0,
        functionNames = listOf("Selected function", "Second function"),
        engineCylinderCount = 4, engineDisplacementPerCylinder = .5f, mafLoadConstant = 0f,
    )

    private val selectorTags = listOf(
        "functions_gasoline",
        "functions_gas",
        "functions_load_source",
        "functions_mapsel_petrol",
        "functions_mapsel_gas",
        "functions_barocorr",
        "functions_ve2",
        "functions_gas_v_uni",
    )
}
