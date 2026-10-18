/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.ui.sensors

import android.graphics.Bitmap
import androidx.room.Room
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.util.Locale
import org.secu3.android.R
import org.secu3.android.models.SensorGaugeStyle
import org.secu3.android.db.AppDatabase
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.ui.sensors.models.IndicatorType

class SensorsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext



    @Test fun rendersPersistedGaugeAndIndicatorValuesAndActiveState() {
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(GaugeItem(GaugeState(4, GaugeType.RPM, 0, true), "1234")),
                listOf(IndicatorItem(IndicatorState(2, IndicatorType.CHECK_ENGINE, 0), true), IndicatorItem(IndicatorState(3, IndicatorType.THROTTLE, 1), false)),
                null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            )
        } }
        compose.onNodeWithTag("gauge-RPM").assertIsDisplayed()
        compose.onNodeWithText("1234").assertIsDisplayed()
        compose.onNodeWithTag("indicator-CHECK_ENGINE")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, context.getString(R.string.sensors_status_accessibility_on)))
        compose.onNodeWithTag("indicator-THROTTLE")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, context.getString(R.string.sensors_status_accessibility_off)))
        compose.onAllNodesWithText(context.getString(R.string.sensors_status_accessibility_on)).assertCountEquals(0)
        compose.onAllNodesWithText(context.getString(R.string.sensors_status_accessibility_off)).assertCountEquals(0)
    }

    @Test fun indicatorColorsCommunicateStateInBothThemesWhileSemanticsRemainAvailable() {
        val darkTheme = mutableStateOf(false)
        val available = mutableStateOf(true)
        val active = mutableStateOf(true)
        compose.setContent {
            SecuComposeTheme(darkTheme = darkTheme.value) {
                SensorsScreen(
                    emptyList(),
                    listOf(IndicatorItem(IndicatorState(1, IndicatorType.CHECK_ENGINE, 0), active.value)),
                    null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    dataAvailable = available.value,
                )
            }
        }

        fun indicatorBackgroundColor(): Int {
            val bitmap = compose.onNodeWithTag("indicator-CHECK_ENGINE").captureToImage().asAndroidBitmap()
            val y = with(compose.density) { 10.dp.roundToPx() }.coerceIn(0, bitmap.height - 1)
            return bitmap.getPixel(bitmap.width / 2, y)
        }

        fun assertState(resource: Int) {
            compose.onNodeWithTag("indicator-CHECK_ENGINE")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, context.getString(resource)))
        }

        assertState(R.string.sensors_status_accessibility_on)
        val lightActive = indicatorBackgroundColor()
        compose.runOnIdle { active.value = false }
        compose.waitForIdle()
        assertState(R.string.sensors_status_accessibility_off)
        val lightInactive = indicatorBackgroundColor()
        compose.runOnIdle { available.value = false }
        compose.waitForIdle()
        assertState(R.string.sensors_status_accessibility_unavailable)
        val lightUnavailable = indicatorBackgroundColor()
        assertNotEquals("light active and inactive indicators need distinct colors", lightActive, lightInactive)
        assertNotEquals("light unavailable and inactive indicators need distinct colors", lightUnavailable, lightInactive)

        compose.runOnIdle {
            darkTheme.value = true
            available.value = true
            active.value = true
        }
        compose.waitForIdle()
        assertState(R.string.sensors_status_accessibility_on)
        val darkActive = indicatorBackgroundColor()
        compose.runOnIdle { active.value = false }
        compose.waitForIdle()
        assertState(R.string.sensors_status_accessibility_off)
        val darkInactive = indicatorBackgroundColor()
        compose.runOnIdle { available.value = false }
        compose.waitForIdle()
        assertState(R.string.sensors_status_accessibility_unavailable)
        val darkUnavailable = indicatorBackgroundColor()
        assertNotEquals("dark active and inactive indicators need distinct colors", darkActive, darkInactive)
        assertNotEquals("dark unavailable and inactive indicators need distinct colors", darkUnavailable, darkInactive)
        compose.onAllNodesWithText(context.getString(R.string.sensors_status_accessibility_on)).assertCountEquals(0)
        compose.onAllNodesWithText(context.getString(R.string.sensors_status_accessibility_off)).assertCountEquals(0)
        compose.onAllNodesWithText(context.getString(R.string.sensors_status_accessibility_unavailable)).assertCountEquals(0)
    }

    @Test fun darkThemeAndLargeFontKeepNumericReadoutsInsideTheCard() {
        val screenshotMode = mutableStateOf(false)
        compose.setContent {
            val fontDensity = LocalDensity.current.density
            SecuComposeTheme(darkTheme = screenshotMode.value) {
                CompositionLocalProvider(LocalDensity provides Density(fontDensity, 2f)) {
                    Box(Modifier.width(360.dp)) {
                        SensorsScreen(
                            gauges = listOf(
                                GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "1234"),
                                GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, true), "12.4"),
                            ),
                            indicators = listOf(IndicatorItem(IndicatorState(2, IndicatorType.CHECK_ENGINE, 0), true)),
                            gaugeChoices = null,
                            indicatorChoices = null,
                            onAddGaugeRequest = {},
                            onAddIndicatorRequest = {},
                            onAddGauge = {},
                            onAddIndicator = {},
                            onGaugeChoicesConsumed = {},
                            onIndicatorChoicesConsumed = {},
                            onDeleteGauge = {},
                            onDeleteIndicator = {},
                            onUpdateGauge = {},
                            onReorderGauges = {},
                            onReorderIndicators = {},
                        )
                    }
                }
            }
        }

        val card = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val value = compose.onNodeWithText("1234").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("large type should remain within the gauge card", value.left >= card.left && value.right <= card.right)
        val secondGauge = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("the width-derived two-column layout should reflow for enlarged numeric values", secondGauge.top > card.top)
        compose.onNodeWithTag("indicator-CHECK_ENGINE").performScrollTo().assertIsDisplayed()
        captureDesignScreenshot("design-sensors-light.png")
        compose.runOnIdle { screenshotMode.value = true }
        compose.waitForIdle()
        captureDesignScreenshot("design-sensors-dark.png")
    }

    @Test fun gaugeAndIndicatorColumnsAdaptToWindowWidthAndFontScale() {
        val width = mutableStateOf(320)
        val fontScale = mutableStateOf(1f)
        val gauges = GaugeType.entries.take(7).mapIndexed { index, type -> GaugeItem(GaugeState(index + 1, type, index, true), "12") }
        val indicators = IndicatorType.entries.take(7).mapIndexed { index, type -> IndicatorItem(IndicatorState(index + 1, type, index), false) }
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                SecuComposeTheme {
                    Box(Modifier.width(width.value.dp).testTag("adaptive-host")) {
                        SensorsScreen(
                            gauges, indicators, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                            gaugeStyle = SensorGaugeStyle.Current,
                        )
                    }
                }
            }
        }

        fun assertColumnCount(expected: Int) {
            compose.waitForIdle()
            val gaugeBounds = gauges.map { compose.onNodeWithTag("gauge-${it.state.gaugeType.name}").getUnclippedBoundsInRoot() }
            val indicatorBounds = indicators.map { compose.onNodeWithTag("indicator-${it.state.indicatorType.name}").getUnclippedBoundsInRoot() }
            val host = compose.onNodeWithTag("adaptive-host").getUnclippedBoundsInRoot()
            val hostCenter = (host.left + host.right).value / 2f
            assertEquals(1, gaugeBounds.take(expected).map { it.top }.distinct().size)
            assertEquals(1, indicatorBounds.take(expected).map { it.top }.distinct().size)
            assertEquals((gaugeBounds.first().right - gaugeBounds.first().left).value, (gaugeBounds[expected - 1].right - gaugeBounds[expected - 1].left).value, 1f)
            assertEquals((indicatorBounds.first().right - indicatorBounds.first().left).value, (indicatorBounds[expected - 1].right - indicatorBounds[expected - 1].left).value, 1f)
            if (gauges.size > expected) org.junit.Assert.assertTrue(gaugeBounds[expected].top > gaugeBounds.first().top)
            if (indicators.size > expected) org.junit.Assert.assertTrue(indicatorBounds[expected].top > indicatorBounds.first().top)
            // An incomplete last row keeps the same cell width as a full row.
            assertEquals((gaugeBounds.first().right - gaugeBounds.first().left).value, (gaugeBounds.last().right - gaugeBounds.last().left).value, 1f)
            assertEquals((indicatorBounds.first().right - indicatorBounds.first().left).value, (indicatorBounds.last().right - indicatorBounds.last().left).value, 1f)
            val gaugesInLastRow = gaugeBounds.takeLast(gauges.size % expected).ifEmpty { gaugeBounds.takeLast(expected) }
            val indicatorsInLastRow = indicatorBounds.takeLast(indicators.size % expected).ifEmpty { indicatorBounds.takeLast(expected) }
            val gaugeRowCenter = gaugesInLastRow.map { (it.left + it.right).value / 2f }.average().toFloat()
            val indicatorRowCenter = indicatorsInLastRow.map { (it.left + it.right).value / 2f }.average().toFloat()
            assertEquals("partial gauge rows stay centered", hostCenter, gaugeRowCenter, 1f)
            assertEquals("partial indicator rows stay centered", hostCenter, indicatorRowCenter, 1f)
        }

        listOf(320 to 2, 360 to 2, 280 to 1, 600 to 4, 900 to 6).forEach { (availableWidth, expectedColumns) ->
            compose.runOnIdle { width.value = availableWidth; fontScale.value = 1f }
            assertColumnCount(expectedColumns)
        }
        compose.runOnIdle { width.value = 600; fontScale.value = 1.5f }
        assertColumnCount(2)
    }

    @Test fun centeredPartialRowsRemainDragTargetsForBothGrids() {
        val gauges = listOf(
            GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "1200"),
            GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, true), "12.4"),
            GaugeItem(GaugeState(3, GaugeType.MAP, 2, true), "42.0"),
        )
        val indicators = listOf(
            IndicatorItem(IndicatorState(1, IndicatorType.THROTTLE, 0), false),
            IndicatorItem(IndicatorState(2, IndicatorType.GAS_VALVE, 1), true),
            IndicatorItem(IndicatorState(3, IndicatorType.CHECK_ENGINE, 2), false),
        )
        var savedGauges: List<GaugeState>? = null
        var savedIndicators: List<IndicatorState>? = null
        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.width(360.dp).testTag("centered-grid-host")) {
                    SensorsScreen(
                        gauges, indicators, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {},
                        { savedGauges = it }, { savedIndicators = it },
                    )
                }
            }
        }

        fun assertCenteredLastRow(firstTag: String, lastTag: String) {
            val host = compose.onNodeWithTag("centered-grid-host").getUnclippedBoundsInRoot()
            val first = compose.onNodeWithTag(firstTag).getUnclippedBoundsInRoot()
            val last = compose.onNodeWithTag(lastTag).getUnclippedBoundsInRoot()
            assertEquals((first.right - first.left).value, (last.right - last.left).value, 1f)
            assertEquals((host.left + host.right).value / 2f, (last.left + last.right).value / 2f, 1f)
        }
        assertCenteredLastRow("gauge-RPM", "gauge-MAP")
        assertCenteredLastRow("indicator-THROTTLE", "indicator-CHECK_ENGINE")

        fun dragTo(sourceTag: String, targetTag: String) {
            val source = compose.onNodeWithTag(sourceTag).fetchSemanticsNode().boundsInRoot
            val target = compose.onNodeWithTag(targetTag).fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag(sourceTag).performTouchInput {
                down(center)
                advanceEventTime(600)
                moveBy(Offset(target.center.x - source.center.x, target.center.y - source.center.y))
                up()
            }
            compose.waitForIdle()
        }
        dragTo("gauge-RPM", "gauge-MAP")
        assertEquals(listOf(GaugeType.VOLTAGE, GaugeType.MAP, GaugeType.RPM), savedGauges?.map { it.gaugeType })
        dragTo("indicator-THROTTLE", "indicator-CHECK_ENGINE")
        assertEquals(listOf(IndicatorType.GAS_VALVE, IndicatorType.CHECK_ENGINE, IndicatorType.THROTTLE), savedIndicators?.map { it.indicatorType })
    }

    @Test fun numericGaugeCanSwitchToDialAndDeleteFromCardMenu() {
        var switched: GaugeState? = null
        var deleted: GaugeType? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "2400")), emptyList(),
                null, null, {}, {}, {}, {}, {}, {}, { deleted = it }, {}, { switched = it }, {}, {},
                gaugeStyle = SensorGaugeStyle.Current,
            )
        } }
        compose.onNodeWithTag("gauge-RPM").performClick()
        compose.onNodeWithText(context.getString(R.string.switch_view)).performClick()
        compose.runOnIdle { assertEquals(false, switched?.isNumericView) }
        compose.onNodeWithTag("gauge-RPM").performClick()
        compose.onNodeWithText(context.getString(R.string.delete)).performClick()
        compose.runOnIdle { assertEquals(GaugeType.RPM, deleted) }
    }

    @Test fun switchingGaugeViewResizesOnlyTheChangedCardInMixedRow() {
        val gauges = mutableStateOf(
            listOf(
                GaugeItem(GaugeState(1, GaugeType.TPS_DOT, 0, true), "-500"),
                GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, false), "12.4"),
                GaugeItem(GaugeState(3, GaugeType.MAP, 2, true), "90"),
            ),
        )
        compose.setContent {
            val fontDensity = LocalDensity.current.density
            SecuComposeTheme {
                CompositionLocalProvider(LocalDensity provides Density(fontDensity, 1f)) {
                    Box(Modifier.width(520.dp)) {
                        SensorsScreen(
                            gauges.value, emptyList(), null, null,
                            {}, {}, {}, {}, {}, {}, {}, {},
                            { state -> gauges.value = gauges.value.map { if (it.state.gaugeType == state.gaugeType) it.copy(state = state) else it } },
                            {}, {},
                            gaugeStyle = SensorGaugeStyle.Current,
                        )
                    }
                }
            }
        }

        val names = listOf("TPS_DOT", "VOLTAGE", "MAP")
        val originalBounds = names.associateWith { compose.onNodeWithTag("gauge-$it").fetchSemanticsNode().boundsInRoot }
        assertEquals("configured cards should share the row", 1, originalBounds.values.map { it.top }.distinct().size)
        org.junit.Assert.assertTrue("numeric content should use less height than the dial", originalBounds.getValue("TPS_DOT").height < originalBounds.getValue("VOLTAGE").height)
        compose.onNodeWithText("-500").assertIsDisplayed()
        compose.onNodeWithText(context.getString(GaugeType.TPS_DOT.units)).assertIsDisplayed()
        var numericValueLayout: TextLayoutResult? = null
        compose.onNodeWithTag("gauge-value-TPS_DOT", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val results = mutableListOf<TextLayoutResult>()
                action(results)
                numericValueLayout = results.single()
            }
        compose.runOnIdle { assertEquals("long numeric values must shrink to fit the card body", false, numericValueLayout?.hasVisualOverflow) }
        captureDesignScreenshot("design-sensors-mixed-row.png")
        compose.onNodeWithTag("gauge-TPS_DOT").performClick()
        compose.onNodeWithText(context.getString(R.string.switch_view)).performClick()
        compose.waitForIdle()

        val dialBounds = names.associateWith { compose.onNodeWithTag("gauge-$it").fetchSemanticsNode().boundsInRoot }
        names.forEach { name ->
            assertEquals("$name width changed when a neighbor switched", originalBounds.getValue(name).width, dialBounds.getValue(name).width, 1f)
        }
        assertEquals("mixed row cards should keep top alignment", 1, dialBounds.values.map { it.top }.distinct().size)
        org.junit.Assert.assertTrue("switching numeric content to a classic dial should grow its card", dialBounds.getValue("TPS_DOT").height > originalBounds.getValue("TPS_DOT").height)
        compose.onNodeWithTag("gauge-body-TPS_DOT", useUnmergedTree = true).assertIsDisplayed()
        val dialDescription = "${context.getString(GaugeType.TPS_DOT.title)}: -500 ${context.getString(GaugeType.TPS_DOT.units)}"
        compose.onNodeWithTag("dial-TPS_DOT", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(dialDescription)))

        compose.onNodeWithTag("gauge-TPS_DOT").performClick()
        compose.onNodeWithText(context.getString(R.string.switch_view)).performClick()
        compose.waitForIdle()
        val restoredBounds = compose.onNodeWithTag("gauge-TPS_DOT").fetchSemanticsNode().boundsInRoot
        assertEquals(originalBounds.getValue("TPS_DOT").width, restoredBounds.width, 1f)
        assertEquals("switching back restores the original content height", originalBounds.getValue("TPS_DOT").height, restoredBounds.height, 1f)
        val restoredNamesBounds = names.associateWith { compose.onNodeWithTag("gauge-$it").fetchSemanticsNode().boundsInRoot }
        names.forEach { name ->
            assertEquals("$name dimensions changed after switching back", originalBounds.getValue(name).width, restoredNamesBounds.getValue(name).width, 1f)
            assertEquals("$name height changed after switching back", originalBounds.getValue(name).height, restoredNamesBounds.getValue(name).height, 1f)
        }
        compose.onNodeWithText("-500").assertIsDisplayed()
    }

    @Test fun gaugeCardResizesAcrossStylesAndKeepsTheDialFaceFullSize() {
        val style = mutableStateOf(SensorGaugeStyle.Current)
        val numeric = mutableStateOf(false)
        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.width(360.dp)) {
                    SensorsScreen(
                        gauges = listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, numeric.value), "2840")),
                        indicators = emptyList(), gaugeChoices = null, indicatorChoices = null,
                        onAddGaugeRequest = {}, onAddIndicatorRequest = {}, onAddGauge = {}, onAddIndicator = {},
                        onGaugeChoicesConsumed = {}, onIndicatorChoicesConsumed = {}, onDeleteGauge = {}, onDeleteIndicator = {},
                        onUpdateGauge = {}, onReorderGauges = {}, onReorderIndicators = {}, gaugeStyle = style.value,
                    )
                }
            }
        }
        fun cardBounds() = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        fun dialBounds() = compose.onNodeWithTag("dial-RPM", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val classic = cardBounds()
        val classicDial = dialBounds()

        compose.runOnIdle { style.value = SensorGaugeStyle.PreciseArc }
        compose.waitForIdle()
        val precise = cardBounds()
        val preciseDial = dialBounds()
        org.junit.Assert.assertTrue("precise arc should omit the classic footer", precise.height < classic.height)
        assertEquals("precise arc keeps the classic dial width", classicDial.width, preciseDial.width, 1f)
        org.junit.Assert.assertTrue("precise arc allocates only the outside-label inset below the full-size circle", preciseDial.height > classicDial.height)

        compose.runOnIdle { style.value = SensorGaugeStyle.ThinNeedle }
        compose.waitForIdle()
        val thin = cardBounds()
        val thinDial = dialBounds()
        org.junit.Assert.assertTrue("thin needle keeps the full-size circle and visible footer", thin.height > classic.height)
        assertEquals("thin needle keeps the classic dial width", classicDial.width, thinDial.width, 1f)
        assertEquals("thin needle allocates the same outside-label inset", preciseDial.height, thinDial.height, 1f)

        compose.runOnIdle { numeric.value = true }
        compose.waitForIdle()
        val numericCard = cardBounds()
        org.junit.Assert.assertTrue("numeric content does not retain a blank dial footer", numericCard.height < precise.height)

        compose.runOnIdle { numeric.value = false; style.value = SensorGaugeStyle.Current }
        compose.waitForIdle()
        assertEquals("returning to classic restores the original height", classic.height, cardBounds().height, 1f)
    }

    @Test fun dialFitsInsideAutomaticGridCell() {
        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.width(480.dp)) {
                    SensorsScreen(
                        listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, false), "2400")),
                        emptyList(), null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                        gaugeStyle = SensorGaugeStyle.Current,
                    )
                }
            }
        }

        val card = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val dial = compose.onNodeWithTag("dial-RPM", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag("gauge-body-RPM", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("dial must be narrower than its card", dial.width < card.width)
        assertEquals("dial face should fill its shared square body", body.width, dial.width, 1f)
        assertEquals("dial face should fill its shared square body", body.height, dial.height, 1f)
        org.junit.Assert.assertTrue("dial must fit horizontally in its card", dial.left >= card.left && dial.right <= card.right)
        org.junit.Assert.assertTrue("dial must fit vertically in its card", dial.top >= card.top && dial.bottom <= card.bottom)
    }

    @Test fun composeDialKeepsValueAndNegativeRangeWithinCardsAcrossWindowWidths() {
        val width = mutableStateOf(240)
        val gauges = listOf(
            GaugeItem(GaugeState(1, GaugeType.RPM, 0, false), "1234"),
            GaugeItem(GaugeState(2, GaugeType.CURRENT_ANGLE, 1, false), "-7.5"),
            GaugeItem(GaugeState(3, GaugeType.VOLTAGE, 2, false), "12.4"),
        )
        compose.setContent { SecuComposeTheme {
            Box(Modifier.width(width.value.dp)) { SensorsScreen(gauges, emptyList(), null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, gaugeStyle = SensorGaugeStyle.Current) }
        } }
        listOf(240 to 1, 360 to 2, 480 to 3).forEach { (availableWidth, expectedColumns) ->
            compose.runOnIdle { width.value = availableWidth }
            compose.waitForIdle()
            val cardBounds = listOf("RPM", "CURRENT_ANGLE", "VOLTAGE").map { compose.onNodeWithTag("gauge-$it").getUnclippedBoundsInRoot() }
            val firstTop = cardBounds.first().top
            assertEquals(1, cardBounds.take(expectedColumns).map { it.top }.distinct().size)
            if (expectedColumns == 1) org.junit.Assert.assertTrue(cardBounds[1].top > firstTop)
            else assertEquals(firstTop.value, cardBounds[1].top.value, 1f)
            if (expectedColumns == 3) assertEquals(firstTop.value, cardBounds[2].top.value, 1f)
            for ((index, name) in listOf("RPM", "CURRENT_ANGLE", "VOLTAGE").withIndex()) {
                val card = cardBounds[index]
                val dial = compose.onNodeWithTag("dial-$name", useUnmergedTree = true).getUnclippedBoundsInRoot()
                org.junit.Assert.assertTrue("$name dial is clipped at $expectedColumns columns: card=$card dial=$dial", dial.left >= card.left && dial.right <= card.right && dial.top >= card.top && dial.bottom <= card.bottom)
            }
        }
        val angleDescription = "${context.getString(GaugeType.CURRENT_ANGLE.title)}: -7.5 ${context.getString(GaugeType.CURRENT_ANGLE.units)}"
        compose.onNodeWithTag("dial-CURRENT_ANGLE", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(angleDescription)))
    }

    @Test fun preciseArcUsesReadableMinimumCellWidth() {
        val style = mutableStateOf(SensorGaugeStyle.PreciseArc)
        val gauges = listOf(
            GaugeItem(GaugeState(1, GaugeType.RPM, 0, false), "2840"),
            GaugeItem(GaugeState(2, GaugeType.TEMPERATURE, 1, false), "36.0"),
            GaugeItem(GaugeState(3, GaugeType.VOLTAGE, 2, false), "13.8"),
        )
        compose.setContent { SecuComposeTheme {
            Box(Modifier.width(320.dp)) {
                SensorsScreen(gauges, emptyList(), null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, gaugeStyle = style.value)
            }
        } }

        val firstCard = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val secondCard = compose.onNodeWithTag("gauge-TEMPERATURE").fetchSemanticsNode().boundsInRoot
        val thirdCard = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        val dialBody = compose.onNodeWithTag("gauge-body-RPM", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("the available width should reflow the third card", thirdCard.top > firstCard.top)
        val minimumWidthPx = (150f - 16f) * context.resources.displayMetrics.density
        org.junit.Assert.assertTrue("reflowed precise-arc card should retain a useful width", firstCard.width >= minimumWidthPx)
        org.junit.Assert.assertTrue("precise-arc drawing bounds should remain non-empty", dialBody.width > 0f && dialBody.height > dialBody.width)
        val description = "${context.getString(GaugeType.RPM.title)}: 2840 ${context.getString(GaugeType.RPM.units)}"
        compose.onNodeWithTag("dial-RPM", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)))
        compose.runOnIdle { style.value = SensorGaugeStyle.Current }
        compose.onNodeWithTag("gauge-RPM").assertIsDisplayed()
        org.junit.Assert.assertEquals(firstCard.width, compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.width, 1f)
    }

    @Test fun composeDialReportsLiveValueAndViewModeChanges() {
        val value = mutableStateOf("100")
        val numeric = mutableStateOf(false)
        compose.setContent { SecuComposeTheme {
            SensorsScreen(listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, numeric.value), value.value)), emptyList(), null, null,
                {}, {}, {}, {}, {}, {}, {}, {}, { numeric.value = !numeric.value }, {}, {}, gaugeStyle = SensorGaugeStyle.Current)
        } }
        fun rpmDescription(value: String) = "${context.getString(GaugeType.RPM.title)}: $value ${context.getString(GaugeType.RPM.units)}"
        compose.onNodeWithTag("dial-RPM", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(rpmDescription("100"))))
        compose.runOnIdle { value.value = "2750" }
        compose.onNodeWithTag("dial-RPM", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(rpmDescription("2750"))))
        compose.onNodeWithTag("gauge-RPM").performClick()
        compose.onNodeWithText(context.getString(R.string.switch_view)).performClick()
        compose.onNodeWithText("2750").assertIsDisplayed()
        compose.onNodeWithTag("gauge-RPM").performClick()
        compose.onNodeWithText(context.getString(R.string.switch_view)).performClick()
        compose.onNodeWithTag("dial-RPM", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun allGaugeDialStylesKeepTheLiveAccessibleValueAndShowUnavailableReadingsAsMissing() {
        val style = mutableStateOf(SensorGaugeStyle.Current)
        val reading = mutableStateOf("2840")
        compose.setContent {
            SecuComposeTheme {
                SensorsScreen(
                    gauges = listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, false), reading.value)),
                    indicators = emptyList(),
                    gaugeChoices = null,
                    indicatorChoices = null,
                    onAddGaugeRequest = {}, onAddIndicatorRequest = {}, onAddGauge = {}, onAddIndicator = {},
                    onGaugeChoicesConsumed = {}, onIndicatorChoicesConsumed = {}, onDeleteGauge = {}, onDeleteIndicator = {},
                    onUpdateGauge = {}, onReorderGauges = {}, onReorderIndicators = {},
                    gaugeStyle = style.value,
                )
            }
        }
        fun assertReadingDescription(value: String) {
            val description = "${context.getString(GaugeType.RPM.title)}: $value ${context.getString(GaugeType.RPM.units)}"
            compose.onNodeWithTag("dial-RPM", useUnmergedTree = true)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)))
        }
        listOf(SensorGaugeStyle.Current, SensorGaugeStyle.PreciseArc, SensorGaugeStyle.ThinNeedle).forEach { candidate ->
            compose.runOnIdle { style.value = candidate }
            compose.onNodeWithTag("dial-RPM", useUnmergedTree = true).assertIsDisplayed()
            assertReadingDescription("2840")
        }
        compose.runOnIdle { reading.value = "not-a-number" }
        listOf(SensorGaugeStyle.Current, SensorGaugeStyle.PreciseArc, SensorGaugeStyle.ThinNeedle).forEach { candidate ->
            compose.runOnIdle { style.value = candidate }
            assertReadingDescription("—")
        }
    }

    @Test fun tickLabelsRoundToWholeNumbersUsingDefaultLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("148", formatGaugeTickLabel(147.6f))
            assertEquals("-7", formatGaugeTickLabel(-7.4f))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private fun captureDesignScreenshot(fileName: String) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, fileName).outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test fun narrowDialTickLabelsUseEvenSubsetIncludingEndpoints() {
        assertEquals(listOf(0, 5, 10, 14, 19), selectGaugeTickLabelIndices(20, 5))
        assertEquals((0 until 20).toList(), selectGaugeTickLabelIndices(20, 20))
        assertEquals(listOf(0, 19), selectGaugeTickLabelIndices(20, 2))
        assertEquals(emptyList<Int>(), selectGaugeTickLabelIndices(0, 5))
    }

    @Test fun addDialogsSelectGaugeAndIndicatorAndIndicatorDelete() {
        var addedGauge: GaugeType? = null
        var addedIndicator: IndicatorType? = null
        var deletedIndicator: IndicatorType? = null
        lateinit var indicatorChoices: androidx.compose.runtime.MutableState<SensorsViewModel.ChoiceRequest<IndicatorType>?>
        compose.setContent { SecuComposeTheme {
            val gaugeChoices = remember { mutableStateOf<SensorsViewModel.ChoiceRequest<GaugeType>?>(SensorsViewModel.ChoiceRequest(1, listOf(GaugeType.VOLTAGE))) }
            indicatorChoices = remember { mutableStateOf<SensorsViewModel.ChoiceRequest<IndicatorType>?>(null) }
            SensorsScreen(
                emptyList(), listOf(IndicatorItem(IndicatorState(0, IndicatorType.THROTTLE, 0), false)),
                gaugeChoices.value, indicatorChoices.value,
                {}, {}, { addedGauge = it }, { addedIndicator = it }, { gaugeChoices.value = null }, { indicatorChoices.value = null }, {}, { deletedIndicator = it }, {}, {}, {},
            )
        } }
        compose.onNodeWithText(context.getString(GaugeType.VOLTAGE.title)).performClick()
        compose.runOnIdle { assertEquals(GaugeType.VOLTAGE, addedGauge) }
        compose.runOnIdle { indicatorChoices.value = SensorsViewModel.ChoiceRequest(2, listOf(IndicatorType.CHECK_ENGINE)) }
        compose.onNodeWithText(context.getString(IndicatorType.CHECK_ENGINE.title)).performClick()
        compose.runOnIdle { assertEquals(IndicatorType.CHECK_ENGINE, addedIndicator) }
        compose.onNodeWithTag("indicator-THROTTLE").performClick()
        compose.onNodeWithText(context.getString(R.string.delete)).performClick()
        compose.runOnIdle { assertEquals(IndicatorType.THROTTLE, deletedIndicator) }
    }

    @Test fun separateReorderCallbacksReceiveCopiedOrderedStatesOnDrop() {
        var savedGauges: List<GaugeState>? = null
        var savedIndicators: List<IndicatorState>? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0), "100"), GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1), "12.1")),
                listOf(IndicatorItem(IndicatorState(3, IndicatorType.THROTTLE, 0), false), IndicatorItem(IndicatorState(4, IndicatorType.GAS_VALVE, 1), true)),
                null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, { savedGauges = it }, { savedIndicators = it },
            )
        } }
        val gaugeSource = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val gaugeTarget = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            down(center); advanceEventTime(600)
            moveBy(Offset(gaugeTarget.left + gaugeTarget.width / 2f - gaugeSource.left - gaugeSource.width / 2f,
                gaugeTarget.top + gaugeTarget.height / 2f - gaugeSource.top - gaugeSource.height / 2f))
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf(0, 1), savedGauges?.map { it.idx })
            assertEquals(listOf(GaugeType.VOLTAGE, GaugeType.RPM), savedGauges?.map { it.gaugeType })
            assertEquals(null, savedIndicators)
        }
        val indicatorSource = compose.onNodeWithTag("indicator-THROTTLE").fetchSemanticsNode().boundsInRoot
        val indicatorTarget = compose.onNodeWithTag("indicator-GAS_VALVE").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("indicator-THROTTLE").performTouchInput {
            down(center); advanceEventTime(600)
            moveBy(Offset(indicatorTarget.left + indicatorTarget.width / 2f - indicatorSource.left - indicatorSource.width / 2f,
                indicatorTarget.top + indicatorTarget.height / 2f - indicatorSource.top - indicatorSource.height / 2f))
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf(0, 1), savedIndicators?.map { it.idx })
            assertEquals(listOf(IndicatorType.GAS_VALVE, IndicatorType.THROTTLE), savedIndicators?.map { it.indicatorType })
        }
    }

    @Test fun activeGaugeFollowsFingerUntilDrop() {
        // Compact numeric cards keep both drag targets in the tablet viewport; dial sizing is tested separately.
        compose.setContent { SecuComposeTheme {
            Box(Modifier.width(280.dp)) {
                SensorsScreen(
                    listOf(
                        GaugeItem(GaugeState(1, GaugeType.RPM, 0, isNumericView = true), "100"),
                        GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, isNumericView = true), "12"),
                    ), emptyList(),
                    null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                )
            }
        } }
        val initialBounds = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val neighborBounds = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        assertPlaced("RPM gauge", initialBounds)
        assertPlaced("Voltage gauge", neighborBounds)
        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(neighborBounds.center.x - initialBounds.center.x, neighborBounds.center.y - initialBounds.center.y))
        }
        compose.waitForIdle()
        val draggedBounds = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val previewBounds = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("active card should follow the finger", draggedBounds.top > initialBounds.top + 50f)
        org.junit.Assert.assertTrue("neighbor should preview into the vacated slot", previewBounds.top < neighborBounds.top - 50f)
        compose.onNodeWithTag("gauge-RPM").performTouchInput { up() }
        compose.waitForIdle()
        val droppedRpm = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val droppedVoltage = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("drop should commit the previewed slot order", droppedVoltage.top < droppedRpm.top)
    }

    @Test fun activeIndicatorPreviewsNeighborMovingToVacatedSlot() {
        compose.setContent { SecuComposeTheme {
            Box(Modifier.width(280.dp)) {
                SensorsScreen(
                    emptyList(), listOf(
                        IndicatorItem(IndicatorState(1, IndicatorType.THROTTLE, 0), false),
                        IndicatorItem(IndicatorState(2, IndicatorType.GAS_VALVE, 1), true),
                    ), null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                )
            }
        } }
        val throttleStart = compose.onNodeWithTag("indicator-THROTTLE").fetchSemanticsNode().boundsInRoot
        val gasStart = compose.onNodeWithTag("indicator-GAS_VALVE").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("indicator-THROTTLE").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(gasStart.center.x - throttleStart.center.x, gasStart.center.y - throttleStart.center.y))
        }
        compose.waitForIdle()
        val throttleDragged = compose.onNodeWithTag("indicator-THROTTLE").fetchSemanticsNode().boundsInRoot
        val gasPreview = compose.onNodeWithTag("indicator-GAS_VALVE").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(throttleDragged.top > throttleStart.top + 20f)
        org.junit.Assert.assertTrue("indicator neighbor should move into the open slot", gasPreview.top < gasStart.top - 20f)
        compose.onNodeWithTag("indicator-THROTTLE").performTouchInput { up() }
        compose.waitForIdle()
        org.junit.Assert.assertTrue(
            compose.onNodeWithTag("indicator-GAS_VALVE").fetchSemanticsNode().boundsInRoot.top <
                compose.onNodeWithTag("indicator-THROTTLE").fetchSemanticsNode().boundsInRoot.top,
        )
    }

    @Test fun previewMovesNeighborSmoothlyUnderControlledClock() {
        // Keep both drag targets in the tablet viewport for a reliable drag gesture.
        compose.mainClock.autoAdvance = false
        var savedOrder: List<GaugeState>? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(
                    GaugeItem(GaugeState(1, GaugeType.RPM, 0, isNumericView = true), "100"),
                    GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, isNumericView = true), "12"),
                ), emptyList(), null, null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, { savedOrder = it }, {},
            )
        } }
        compose.waitForIdle()
        val sourceBounds = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val sourceLeft = sourceBounds.left
        val neighborStart = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        assertPlaced("RPM gauge", sourceBounds)
        assertPlaced("Voltage gauge", neighborStart)
        val delta = Offset(neighborStart.center.x - compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.center.x,
            neighborStart.center.y - compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.center.y)
        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(delta)
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val midpointLeft = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.left
        org.junit.Assert.assertTrue("neighbor should be between its start and destination mid-animation", midpointLeft < neighborStart.left && midpointLeft > sourceLeft)

        compose.mainClock.advanceTimeBy(160)
        compose.waitForIdle()
        val destinationLeft = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.left
        assertEquals(sourceLeft, destinationLeft, 1f)

        compose.onNodeWithTag("gauge-RPM").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(48)
        compose.waitForIdle()
        assertEquals("committing after preview settles should not move the neighbor", destinationLeft,
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.left, 1f)
        assertEquals(listOf(GaugeType.VOLTAGE, GaugeType.RPM), savedOrder?.map { it.gaugeType })
        compose.mainClock.autoAdvance = true
    }

    @Test fun earlyDropWaitsOnlyForRemainingPreviewAnimation() {
        // Keep both drag targets in the tablet viewport for a reliable drag gesture.
        compose.mainClock.autoAdvance = false
        var savedOrder: List<GaugeState>? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(
                    GaugeItem(GaugeState(1, GaugeType.RPM, 0, isNumericView = true), "100"),
                    GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, isNumericView = true), "12"),
                ), emptyList(), null, null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, { savedOrder = it }, {},
            )
        } }
        compose.waitForIdle()
        val source = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        assertPlaced("RPM gauge", source)
        assertPlaced("Voltage gauge", target)
        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(target.center.x - source.center.x, target.center.y - source.center.y))
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        compose.onNodeWithTag("gauge-RPM").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        assertEquals("drop must wait for the remaining preview animation", null, savedOrder)
        compose.mainClock.advanceTimeBy(150)
        compose.waitForIdle()
        assertEquals(listOf(GaugeType.VOLTAGE, GaugeType.RPM), savedOrder?.map { it.gaugeType })
        compose.mainClock.autoAdvance = true
    }

    private fun assertPlaced(label: String, bounds: androidx.compose.ui.geometry.Rect) {
        org.junit.Assert.assertTrue("$label must be placed before the drag; got $bounds", bounds.width > 0f && bounds.height > 0f)
    }

    @Test fun repeatedTargetEventsKeepPreviewStableAndReturningHomeCancelsReorder() {
        var savedOrder: List<GaugeState>? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(
                    GaugeItem(GaugeState(1, GaugeType.RPM, 0), "100"),
                    GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1), "12"),
                ), emptyList(), null, null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, { savedOrder = it }, {},
            )
        } }
        val source = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        val delta = Offset(target.center.x - source.center.x, target.center.y - source.center.y)
        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(delta)
        }
        compose.waitForIdle()
        val firstPreview = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top

        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            moveBy(Offset(1f, 0f))
            moveBy(Offset(-1f, 0f))
        }
        compose.waitForIdle()
        val repeatedPreview = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top
        assertEquals("repeated events over the same target must not oscillate", firstPreview, repeatedPreview, 1f)

        compose.onNodeWithTag("gauge-RPM").performTouchInput { moveBy(Offset(-delta.x, -delta.y)) }
        compose.waitForIdle()
        val homePreview = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top
        assertEquals("returning the finger to the source restores the base preview", target.top, homePreview, 1f)
        compose.onNodeWithTag("gauge-RPM").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf(GaugeType.RPM, GaugeType.VOLTAGE), savedOrder?.map { it.gaugeType })
    }

    @Test fun packetAndMembershipUpdatesDoNotUndoPendingDropOrder() {
        val packetItems = mutableStateOf(listOf(
            GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "100"),
            GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, true), "12"),
        ))
        compose.setContent { SecuComposeTheme {
            Box(Modifier.width(320.dp)) {
                SensorsScreen(
                    packetItems.value, emptyList(), null, null,
                    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                )
            }
        } }
        val sourceBounds = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val targetBounds = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            down(center); advanceEventTime(600)
            moveBy(Offset(targetBounds.left + targetBounds.width / 2f - sourceBounds.left - sourceBounds.width / 2f,
                targetBounds.top + targetBounds.height / 2f - sourceBounds.top - sourceBounds.height / 2f))
            up()
        }
        val savedOrder = listOf(
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.left,
            compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.left,
        )
        org.junit.Assert.assertTrue(savedOrder[0] < savedOrder[1])

        // A packet with stale persisted order arrives after drag completion.
        compose.runOnIdle { packetItems.value = packetItems.value.map { it.copy(value = "updated") } }
        val staleOrder = listOf(
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.left,
            compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.left,
        )
        org.junit.Assert.assertTrue(staleOrder[0] < staleOrder[1])

        // A newly added gauge joins the pending order without moving its existing entries.
        compose.runOnIdle {
            packetItems.value = packetItems.value + GaugeItem(GaugeState(3, GaugeType.MAP, 2, true), "1.0")
        }
        val mapTop = compose.onNodeWithTag("gauge-MAP").fetchSemanticsNode().boundsInRoot.top
        val rpmTop = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(mapTop > rpmTop)

        // A removed gauge is pruned from the pending order and the remaining database order is accepted.
        compose.runOnIdle { packetItems.value = listOf(packetItems.value[0], packetItems.value[2]) }
        compose.onNodeWithTag("gauge-RPM").assertIsDisplayed()
        compose.onNodeWithTag("gauge-MAP").assertIsDisplayed()
        compose.onNodeWithTag("gauge-VOLTAGE").assertDoesNotExist()
    }

    @Test fun cancellingDragAfterPacketUpdateKeepsPendingOrderAndReconciles() {
        val packetItems = mutableStateOf(listOf(
            GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "100"),
            GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, true), "12"),
        ))
        compose.setContent { SecuComposeTheme {
            Box(Modifier.width(280.dp)) {
                SensorsScreen(
                    packetItems.value, emptyList(), null, null,
                    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    onReorderingChanged = { active ->
                        if (active) packetItems.value = packetItems.value.map { it.copy(value = "4321") }
                    },
                )
            }
        } }
        val source = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("gauge-RPM").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(target.center.x - source.center.x, target.center.y - source.center.y))
            up()
        }
        val movedOrder = listOf(
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top,
            compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top,
        )
        org.junit.Assert.assertTrue(movedOrder[0] < movedOrder[1])

        val voltage = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        val rpm = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("gauge-VOLTAGE").performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(rpm.center.x - voltage.center.x, rpm.center.y - voltage.center.y))
            cancel()
        }
        compose.waitForIdle()
        val orderAfterCancel = listOf(
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top,
            compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top,
        )
        org.junit.Assert.assertTrue("cancel must preserve the prior pending order", orderAfterCancel[0] < orderAfterCancel[1])
        assertEquals(voltage.top, compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top, 1f)
        assertEquals(rpm.top, compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.onNodeWithTag("gauge-RPM").assertTextContains("4321")
    }

    @Test fun reorderedGaugeAndIndicatorOrderSurvivesDatabaseReopen() = runBlocking {
        val name = "sensor-order-${System.nanoTime()}.db"
        val first = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        first.gaugeStateDao().insertAll(listOf(
            GaugeState(gaugeType = GaugeType.RPM, idx = 0),
            GaugeState(gaugeType = GaugeType.VOLTAGE, idx = 1),
        ))
        first.indicatorStateDao().insertAll(listOf(
            IndicatorState(indicatorType = IndicatorType.THROTTLE, idx = 0),
            IndicatorState(indicatorType = IndicatorType.GAS_VALVE, idx = 1),
        ))
        first.gaugeStateDao().updateAll(listOf(
            first.gaugeStateDao().getByGaugeType(GaugeType.VOLTAGE)!!.copy(idx = 0),
            first.gaugeStateDao().getByGaugeType(GaugeType.RPM)!!.copy(idx = 1, isNumericView = true),
        ))
        first.indicatorStateDao().updateAll(listOf(
            first.indicatorStateDao().getByIndicatorType(IndicatorType.GAS_VALVE)!!.copy(idx = 0),
            first.indicatorStateDao().getByIndicatorType(IndicatorType.THROTTLE)!!.copy(idx = 1),
        ))
        first.close()

        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            assertEquals(listOf(GaugeType.VOLTAGE, GaugeType.RPM), reopened.gaugeStateDao().getAllOrderByIdx().map { it.gaugeType })
            assertEquals(true, reopened.gaugeStateDao().getByGaugeType(GaugeType.RPM)?.isNumericView)
            assertEquals(listOf(IndicatorType.GAS_VALVE, IndicatorType.THROTTLE), reopened.indicatorStateDao().getAllOrderByIdx().map { it.indicatorType })
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}
