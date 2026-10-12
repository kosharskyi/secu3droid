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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.ui.compose.SecuComposeTheme
import java.util.Locale

class RawSensorsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val labelIds = listOf(
        R.string.raw_sensors_frag_map_sensor_label,
        R.string.raw_sensors_frag_voltage_label,
        R.string.raw_sensors_frag_coolant_sensor_label,
        R.string.raw_sensors_frag_knock_signal_lavel_label,
        R.string.raw_sensors_frag_throttle_position_sensor_label,
        R.string.raw_sensors_frag_add_io1_input_label,
        R.string.raw_sensors_frag_add_io2_input_label,
        R.string.raw_sensors_frag_add_io3_input_label,
        R.string.raw_sensors_frag_add_io4_input_label,
        R.string.raw_sensors_frag_add_io5_input_label,
        R.string.raw_sensors_frag_add_io6_input_label,
        R.string.raw_sensors_frag_add_io7_input_label,
        R.string.raw_sensors_frag_add_io8_input_label,
    )

    @Test fun packetValuesRenderInFieldOrderWithUsThreeDecimalFormatting() {
        val values = (1..13).map { it / 4f }
        val packet = packet(values)
        compose.setContent { SecuComposeTheme { RawSensorsScreen(packet) } }

        val tops = labelIds.map { labelId ->
            compose.onNodeWithTag("raw-sensor-row-$labelId").fetchSemanticsNode().boundsInRoot.top
        }
        assertTrue("Rows should follow the legacy field order", tops.zipWithNext().all { (before, after) -> before < after })
        labelIds.indices.forEach { index ->
            val rowTag = "raw-sensor-row-${labelIds[index]}"
            compose.onNodeWithTag("raw-sensors-list").performScrollToNode(hasText(context.getString(labelIds[index])))
            compose.onNodeWithTag("$rowTag-label").assertTextEquals(context.getString(labelIds[index]))
            compose.onNodeWithTag("$rowTag-value").assertTextEquals(String.format(Locale.US, "%.3f", values[index]))
        }
    }

    @Test fun noRawPacketShowsNoInventedZeroValues() {
        compose.setContent { SecuComposeTheme { RawSensorsScreen(null) } }

        compose.onNodeWithTag("raw-sensors-list").assertExists()
        compose.onNodeWithText(context.getString(labelIds.first())).assertDoesNotExist()
        compose.onNodeWithText("0.000").assertDoesNotExist()
    }

    @Test fun updatedPacketStateUpdatesVisibleValue() {
        val current = mutableStateOf(packet((1..13).map { it.toFloat() }))
        compose.setContent { SecuComposeTheme { RawSensorsScreen(current.value) } }

        compose.onNodeWithTag("raw-sensor-row-${labelIds.first()}-value").assertTextEquals("1.000")
        compose.runOnIdle { current.value = packet(listOf(2.3456f) + (2..13).map { it.toFloat() }) }
        compose.onNodeWithTag("raw-sensor-row-${labelIds.first()}-value").assertTextEquals("2.346")
    }

    @Test fun scrollRevealsTheLastAdditionalInput() {
        compose.setContent { SecuComposeTheme { RawSensorsScreen(packet((1..13).map { it.toFloat() })) } }

        compose.onNodeWithTag("raw-sensors-list")
            .performScrollToNode(hasText(context.getString(labelIds.last())))
        compose.onNodeWithText(context.getString(labelIds.last())).assertIsDisplayed()
        compose.onNodeWithText("13.000").assertIsDisplayed()
    }

    private fun packet(v: List<Float>) = AdcRawDatPacket(
        map = v[0], voltage = v[1], temperature = v[2], knockValue = v[3], tps = v[4],
        addI1 = v[5], addI2 = v[6], addI3 = v[7], addI4 = v[8], addI5 = v[9], addI6 = v[10], addI7 = v[11], addI8 = v[12],
    )
}
