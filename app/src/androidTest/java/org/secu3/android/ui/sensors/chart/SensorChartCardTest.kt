package org.secu3.android.ui.sensors.chart

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.db.models.GaugeState
import org.secu3.android.domain.sensors.SensorChartSample
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.SensorPresentationCatalog

class SensorChartCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rendersCompactHistoryReformatsInjectionPhaseAndClearsOnLoss() {
        val catalog = SensorPresentationCatalog()
        val type = GaugeType.BEGIN_INJ_PHASE
        val timing = mutableStateOf(InjectionTimingMode.Btdc720)
        val connection = mutableStateOf<ConnectionState?>(Connected)
        val samples = mutableStateOf(emptyList<SensorChartSample>())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val title = context.getString(catalog.gauge(type).title)
        val unit = context.getString(catalog.gauge(type).units).trim()
        val item = GaugeItem(GaugeState(gaugeType = type, idx = 0, isChart = true), "")

        compose.setContent {
            SecuComposeTheme {
                SensorChartCard(item, samples.value, connection.value, timing.value, catalog, onMenu = {})
            }
        }
        compose.onNodeWithTag("sensor-chart-latest-${type.name}", useUnmergedTree = true).assertTextEquals("— $unit")
        compose.onNodeWithText(context.getString(R.string.sensor_chart_waiting), useUnmergedTree = true).assertIsDisplayed()

        compose.runOnIdle { samples.value = listOf(SensorChartSample(100L, 500f), SensorChartSample(220L, 420f)) }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.sensor_chart_description, title), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("sensor-chart-latest-${type.name}", useUnmergedTree = true).assertTextEquals("420.0 $unit")
        compose.onNodeWithTag("sensor-chart-plot-${type.name}", useUnmergedTree = true).assertIsDisplayed()

        compose.runOnIdle { timing.value = InjectionTimingMode.Btdc360 }
        compose.onNodeWithTag("sensor-chart-latest-${type.name}", useUnmergedTree = true).assertTextEquals("-300.0 $unit")

        compose.runOnIdle { connection.value = Disconnected }
        compose.onNodeWithText(context.getString(R.string.dashboard_status_no_data), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("sensor-chart-latest-${type.name}", useUnmergedTree = true).assertTextEquals("— $unit")
        org.junit.Assert.assertEquals(0, compose.onAllNodesWithTag("sensor-chart-plot-${type.name}").fetchSemanticsNodes().size)
    }
}
