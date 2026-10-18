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
package org.secu3.android.ui.sensors.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.SensorGaugeStyle

class SensorsSettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun persistedValuesRenderAndActionsUpdateSettingsAndNavigate() {
        val oldView = mutableStateOf(true)
        val columns = mutableIntStateOf(5)
        var back = false
        compose.setContent {
            SensorsSettingsScreen(oldView.value, columns.intValue, { oldView.value = it }, { columns.intValue = it }, { back = true })
        }
        compose.onNodeWithText(context.getString(R.string.enable_old_sensors_view)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.columns_count)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("sensor-columns-row").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("sensor-columns-value", useUnmergedTree = true).assertTextEquals("5")
        compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOn()
        compose.onNodeWithText(context.getString(R.string.enable_old_sensors_view)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false, oldView.value) }
        compose.onNodeWithTag("sensors-old-view-checkbox").assertIsOff()
        // Exercise both ends of the allowed range and check the selected value after each update.
        compose.onNodeWithTag("sensor-columns-row").performScrollTo().performClick()
        (2..8).forEach { count ->
            compose.onAllNodesWithText(count.toString(), substring = false)
                .assertCountEquals(if (count == 5) 2 else 1)
        }
        compose.onNodeWithText("2", substring = false).performClick()
        compose.runOnIdle { assertEquals(2, columns.intValue) }
        compose.onNodeWithTag("sensor-columns-value", useUnmergedTree = true).assertTextEquals("2")
        compose.onNodeWithTag("sensor-columns-row").performScrollTo().performClick()
        compose.onNodeWithText("8", substring = false).performClick()
        compose.runOnIdle { assertEquals(8, columns.intValue) }
        compose.onNodeWithTag("sensor-columns-value", useUnmergedTree = true).assertTextEquals("8")
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.runOnIdle { assertTrue(back) }
    }

    @Test fun gaugeStyleSelectionRendersCurrentChoiceAndReportsChanges() {
        val selected = mutableStateOf(SensorGaugeStyle.Current)
        compose.setContent {
            SensorsSettingsScreen(
                oldView = false,
                columns = 2,
                onOldViewChange = {},
                onColumnsChange = {},
                onNavigateUp = {},
                gaugeStyle = selected.value,
                onGaugeStyleChange = { selected.value = it },
            )
        }
        compose.onNodeWithTag("sensor-gauge-style-value", useUnmergedTree = true)
            .assertTextEquals(context.getString(R.string.sensor_gauge_style_current))
        compose.onNodeWithTag("sensor-gauge-style-row").performScrollTo().performClick()
        compose.onNodeWithTag("sensor-gauge-style-precise_arc").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(SensorGaugeStyle.PreciseArc, selected.value) }
        compose.onNodeWithTag("sensor-gauge-style-value", useUnmergedTree = true)
            .assertTextEquals(context.getString(R.string.sensor_gauge_style_precise_arc))
    }
}
