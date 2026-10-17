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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.secu3.android.R
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterTextValueRow
import java.util.Locale

@Composable
internal fun RawSensorsScreen(packet: AdcRawDatPacket?, modifier: Modifier = Modifier) {
    val rows = packet?.let {
        listOf(
            R.string.raw_sensors_frag_map_sensor_label to it.map,
            R.string.raw_sensors_frag_voltage_label to it.voltage,
            R.string.raw_sensors_frag_coolant_sensor_label to it.temperature,
            R.string.raw_sensors_frag_knock_signal_lavel_label to it.knockValue,
            R.string.raw_sensors_frag_throttle_position_sensor_label to it.tps,
            R.string.raw_sensors_frag_add_io1_input_label to it.addI1,
            R.string.raw_sensors_frag_add_io2_input_label to it.addI2,
            R.string.raw_sensors_frag_add_io3_input_label to it.addI3,
            R.string.raw_sensors_frag_add_io4_input_label to it.addI4,
            R.string.raw_sensors_frag_add_io5_input_label to it.addI5,
            R.string.raw_sensors_frag_add_io6_input_label to it.addI6,
            R.string.raw_sensors_frag_add_io7_input_label to it.addI7,
            R.string.raw_sensors_frag_add_io8_input_label to it.addI8,
        )
    }

    ParameterPage(modifier = modifier.testTag("raw-sensors-list")) {
        if (rows == null) {
            Text(
                text = stringResource(R.string.dashboard_status_no_data),
                modifier = Modifier.padding(16.dp).testTag("raw-sensors-no-data"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        rows?.let { sensorRows ->
            ParameterSection(modifier = Modifier.fillMaxWidth().testTag("raw-sensors-card")) {
                sensorRows.forEach { (label, value) ->
                    val rowTag = "raw-sensor-row-$label"
                    val numberText = String.format(Locale.US, "%.3f", value)
                    val units = stringResource(R.string.units_volts)
                    ParameterTextValueRow(
                        title = stringResource(label),
                        value = "$numberText $units",
                        numberText = numberText,
                        units = units,
                        testTag = rowTag,
                    )
                }
            }
        }
    }
}
