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

import androidx.room.Room
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.secu3.android.R
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
                2, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            )
        } }
        compose.onNodeWithTag("gauge-RPM").assertIsDisplayed()
        compose.onNodeWithText("1234").assertIsDisplayed()
        compose.onNodeWithTag("indicator-CHECK_ENGINE")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, context.getString(R.string.sensors_status_accessibility_on)))
        compose.onNodeWithTag("indicator-THROTTLE")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, context.getString(R.string.sensors_status_accessibility_off)))
    }

    @Test fun configuredColumnCountControlsGaugeGridPlacement() {
        val columns = mutableStateOf(2)
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "100"), GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, true), "12.1"), GaugeItem(GaugeState(3, GaugeType.MAP, 2, true), "90")),
                emptyList(), columns.value, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            )
        } }
        val firstTop = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top
        val secondTop = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top
        assertEquals(firstTop, secondTop, 1f)
        val firstBounds = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot
        val twoColumnGrid = compose.onNodeWithTag("gauges-grid").fetchSemanticsNode().boundsInRoot
        assertEquals(firstBounds.width, secondBounds.width, 1f)
        org.junit.Assert.assertTrue(firstBounds.width + secondBounds.width > twoColumnGrid.width * .90f)
        val thirdTopTwoColumns = compose.onNodeWithTag("gauge-MAP").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(thirdTopTwoColumns > firstTop)
        compose.runOnIdle { columns.value = 3 }
        val threeBounds = listOf("RPM", "VOLTAGE", "MAP").map { compose.onNodeWithTag("gauge-$it").fetchSemanticsNode().boundsInRoot }
        assertEquals(threeBounds[0].width, threeBounds[1].width, 1f)
        assertEquals(threeBounds[1].width, threeBounds[2].width, 1f)
        assertEquals(threeBounds[0].top, threeBounds[2].top, 1f)
        compose.runOnIdle { columns.value = 1 }
        val singleColumnSecondTop = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(singleColumnSecondTop > firstTop)
    }

    @Test fun numericGaugeCanSwitchToDialAndDeleteFromCardMenu() {
        var switched: GaugeState? = null
        var deleted: GaugeType? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, true), "2400")), emptyList(),
                2, null, null, {}, {}, {}, {}, {}, {}, { deleted = it }, {}, { switched = it }, {}, {},
            )
        } }
        compose.onNodeWithTag("gauge-RPM").performClick()
        compose.onNodeWithText(context.getString(R.string.switch_view)).performClick()
        compose.runOnIdle { assertEquals(false, switched?.isNumericView) }
        compose.onNodeWithTag("gauge-RPM").performClick()
        compose.onNodeWithText(context.getString(R.string.delete)).performClick()
        compose.runOnIdle { assertEquals(GaugeType.RPM, deleted) }
    }

    @Test fun dialFitsInsideNarrowThreeColumnGaugeCard() {
        compose.setContent {
            SecuComposeTheme {
                Box(Modifier.width(360.dp)) {
                    SensorsScreen(
                        listOf(GaugeItem(GaugeState(1, GaugeType.RPM, 0, false), "2400")),
                        emptyList(), 3, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    )
                }
            }
        }

        val card = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val dial = compose.onNodeWithTag("dial-RPM").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("dial must be narrower than its card", dial.width < card.width)
        org.junit.Assert.assertTrue("normal-mode dial should measure square", kotlin.math.abs(dial.width - dial.height) <= 2f)
        org.junit.Assert.assertTrue("dial must fit horizontally in its card", dial.left >= card.left && dial.right <= card.right)
        org.junit.Assert.assertTrue("dial must fit vertically in its card", dial.top >= card.top && dial.bottom <= card.bottom)
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
                1, gaugeChoices.value, indicatorChoices.value,
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
                2, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, { savedGauges = it }, { savedIndicators = it },
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
            SensorsScreen(
                listOf(
                    GaugeItem(GaugeState(1, GaugeType.RPM, 0, isNumericView = true), "100"),
                    GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, isNumericView = true), "12"),
                ), emptyList(),
                1, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            )
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
            SensorsScreen(
                emptyList(), listOf(
                    IndicatorItem(IndicatorState(1, IndicatorType.THROTTLE, 0), false),
                    IndicatorItem(IndicatorState(2, IndicatorType.GAS_VALVE, 1), true),
                ), 1, null, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            )
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
        // Keep both drag targets in the tablet viewport; SpeedView sizing is verified by its dedicated test.
        compose.mainClock.autoAdvance = false
        var savedOrder: List<GaugeState>? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(
                    GaugeItem(GaugeState(1, GaugeType.RPM, 0, isNumericView = true), "100"),
                    GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, isNumericView = true), "12"),
                ), emptyList(), 1, null, null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, { savedOrder = it }, {},
            )
        } }
        compose.waitForIdle()
        val sourceBounds = compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot
        val sourceTop = sourceBounds.top
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
        val midpointTop = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue("neighbor should be between its start and destination mid-animation", midpointTop < neighborStart.top && midpointTop > sourceTop)

        compose.mainClock.advanceTimeBy(160)
        compose.waitForIdle()
        val destinationTop = compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top
        assertEquals(sourceTop, destinationTop, 1f)

        compose.onNodeWithTag("gauge-RPM").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(48)
        compose.waitForIdle()
        assertEquals("committing after preview settles should not move the neighbor", destinationTop,
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top, 1f)
        assertEquals(listOf(GaugeType.VOLTAGE, GaugeType.RPM), savedOrder?.map { it.gaugeType })
        compose.mainClock.autoAdvance = true
    }

    @Test fun earlyDropWaitsOnlyForRemainingPreviewAnimation() {
        // Keep both drag targets in the tablet viewport; SpeedView sizing is verified by its dedicated test.
        compose.mainClock.autoAdvance = false
        var savedOrder: List<GaugeState>? = null
        compose.setContent { SecuComposeTheme {
            SensorsScreen(
                listOf(
                    GaugeItem(GaugeState(1, GaugeType.RPM, 0, isNumericView = true), "100"),
                    GaugeItem(GaugeState(2, GaugeType.VOLTAGE, 1, isNumericView = true), "12"),
                ), emptyList(), 1, null, null,
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
                ), emptyList(), 1, null, null,
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
            SensorsScreen(
                packetItems.value, emptyList(), 1, null, null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            )
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
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top,
            compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top,
        )
        org.junit.Assert.assertTrue(savedOrder[0] < savedOrder[1])

        // A packet with stale persisted order arrives after drag completion.
        compose.runOnIdle { packetItems.value = packetItems.value.map { it.copy(value = "updated") } }
        val staleOrder = listOf(
            compose.onNodeWithTag("gauge-VOLTAGE").fetchSemanticsNode().boundsInRoot.top,
            compose.onNodeWithTag("gauge-RPM").fetchSemanticsNode().boundsInRoot.top,
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
            SensorsScreen(
                packetItems.value, emptyList(), 1, null, null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                onReorderingChanged = { active ->
                    if (active) packetItems.value = packetItems.value.map { it.copy(value = "updated during drag") }
                },
            )
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
        compose.onNodeWithTag("gauge-RPM").assertTextContains("updated during drag")
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
