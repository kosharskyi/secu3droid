package org.secu3.android.ui.sensors

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.packets.input.SensorsPacket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.db.AppDatabase
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.models.sensors.IndicatorType
import org.secu3.android.utils.AppPrefs

@RunWith(AndroidJUnit4::class)
class SensorConfigurationObservationTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun configuredGaugeAndIndicatorFlowsReflectOfflineEditsInOrder() = runBlocking {
        val gauges = database.gaugeStateDao()
        val indicators = database.indicatorStateDao()
        val gaugeChanges = Channel<List<GaugeState>>(Channel.UNLIMITED)
        val indicatorChanges = Channel<List<IndicatorState>>(Channel.UNLIMITED)
        val gaugeObserver = launch { gauges.observeAllOrderByIdx().collect(gaugeChanges::send) }
        val indicatorObserver = launch { indicators.observeAllOrderByIdx().collect(indicatorChanges::send) }
        assertEquals(emptyList<GaugeState>(), gaugeChanges.next())
        assertEquals(emptyList<IndicatorState>(), indicatorChanges.next())

        gauges.insert(GaugeState(gaugeType = GaugeType.MAP, idx = 1))
        assertEquals(listOf(GaugeType.MAP), gaugeChanges.next().map { it.gaugeType })
        gauges.insert(GaugeState(gaugeType = GaugeType.RPM, idx = 0))
        assertEquals(listOf(GaugeType.RPM, GaugeType.MAP), gaugeChanges.next().map { it.gaugeType })
        indicators.insert(IndicatorState(indicatorType = IndicatorType.THROTTLE, idx = 1))
        assertEquals(listOf(IndicatorType.THROTTLE), indicatorChanges.next().map { it.indicatorType })
        indicators.insert(IndicatorState(indicatorType = IndicatorType.GAS_VALVE, idx = 0))
        assertEquals(listOf(IndicatorType.GAS_VALVE, IndicatorType.THROTTLE), indicatorChanges.next().map { it.indicatorType })

        gauges.deleteGauge(GaugeType.RPM)
        indicators.deleteIndicator(IndicatorType.GAS_VALVE)
        assertEquals(listOf(GaugeType.MAP), gaugeChanges.next().map { it.gaugeType })
        assertEquals(listOf(IndicatorType.THROTTLE), indicatorChanges.next().map { it.indicatorType })
        gaugeObserver.cancel()
        indicatorObserver.cancel()
    }

    @Test
    fun repositoryCombinesOfflineConfigurationPacketsAndDisplayMode(): Unit = runBlocking {
        val repository = SensorsRepository(
            AppPrefs(ApplicationProvider.getApplicationContext<Context>()),
            database,
        )
        val packet = MutableStateFlow<SensorsPacket?>(null)
        val timingMode = MutableStateFlow(InjectionTimingMode.Default)
        val gaugeChanges = Channel<List<GaugeItem>>(Channel.UNLIMITED)
        val indicatorChanges = Channel<List<IndicatorItem>>(Channel.UNLIMITED)
        val gaugeObserver = launch {
            repository.observeGaugeItems(packet, timingMode).collect(gaugeChanges::send)
        }
        val indicatorObserver = launch {
            repository.observeIndicatorItems(packet).collect(indicatorChanges::send)
        }

        try {
            assertEquals(emptyList<GaugeItem>(), gaugeChanges.next())
            assertEquals(emptyList<IndicatorItem>(), indicatorChanges.next())

            repository.addGauge(GaugeType.FUEL_CONSUMPTION)
            assertEquals(listOf(GaugeType.FUEL_CONSUMPTION), gaugeChanges.next().map { it.state.gaugeType })
            repository.addGauge(GaugeType.CONSUMED_FUEL)
            assertEquals(listOf(GaugeType.FUEL_CONSUMPTION, GaugeType.CONSUMED_FUEL), gaugeChanges.next().map { it.state.gaugeType })
            repository.addGauge(GaugeType.BEGIN_INJ_PHASE)
            val offlineGauges = gaugeChanges.next()
            assertEquals(
                listOf(GaugeType.FUEL_CONSUMPTION, GaugeType.CONSUMED_FUEL, GaugeType.BEGIN_INJ_PHASE),
                offlineGauges.map { it.state.gaugeType },
            )
            assertTrue(offlineGauges.all { it.value.isEmpty() })
            assertTrue(offlineGauges.single { it.state.gaugeType == GaugeType.CONSUMED_FUEL }.state.isNumericView)

            val reordered = listOf(
                offlineGauges.single { it.state.gaugeType == GaugeType.CONSUMED_FUEL }.state.copy(idx = 0),
                offlineGauges.single { it.state.gaugeType == GaugeType.BEGIN_INJ_PHASE }.state.copy(idx = 1),
                offlineGauges.single { it.state.gaugeType == GaugeType.FUEL_CONSUMPTION }.state.copy(idx = 2),
            )
            repository.updateGauges(reordered)
            val reorderedOffline = gaugeChanges.next()
            assertEquals(reordered.map { it.gaugeType }, reorderedOffline.map { it.state.gaugeType })
            assertTrue(reorderedOffline.all { it.value.isEmpty() })

            repository.addIndicator(IndicatorType.FI_FUEL)
            val offlineIndicators = indicatorChanges.next()
            assertEquals(listOf(IndicatorType.FI_FUEL), offlineIndicators.map { it.state.indicatorType })
            assertFalse(offlineIndicators.single().isActive)

            packet.value = SensorsPacket(
                sensorsFlags = 1 shl 2,
                injTimBegin = 420f,
                cons_fuel = 20f,
            ).apply {
                inj_ffd = 8f
            }
            val gasBitOnly = gaugeChanges.next()
            assertEquals("8.00", gasBitOnly.single { it.state.gaugeType == GaugeType.FUEL_CONSUMPTION }.value)
            assertEquals("20.000", gasBitOnly.single { it.state.gaugeType == GaugeType.CONSUMED_FUEL }.value)
            assertEquals("420.0", gasBitOnly.single { it.state.gaugeType == GaugeType.BEGIN_INJ_PHASE }.value)
            assertFalse(indicatorChanges.next().single().isActive)

            packet.value = packet.value!!.copy(sensorsFlags = (1 shl 0) or (1 shl 2)).apply {
                inj_ffd = 8f
            }
            assertTrue(indicatorChanges.next().single().isActive)
            val bothFlags = gaugeChanges.next()
            assertEquals("8.00", bothFlags.single { it.state.gaugeType == GaugeType.FUEL_CONSUMPTION }.value)
            assertEquals("20.000", bothFlags.single { it.state.gaugeType == GaugeType.CONSUMED_FUEL }.value)
            timingMode.value = InjectionTimingMode.Btdc360
            assertEquals("-300.0", gaugeChanges.next().single { it.state.gaugeType == GaugeType.BEGIN_INJ_PHASE }.value)

            packet.value = null
            val unavailableGauges = gaugeChanges.next()
            assertEquals(reordered.map { it.gaugeType }, unavailableGauges.map { it.state.gaugeType })
            assertTrue(unavailableGauges.all { it.value.isEmpty() })
            assertFalse(indicatorChanges.next().single().isActive)

            repository.deleteGauge(GaugeType.CONSUMED_FUEL)
            assertEquals(
                listOf(GaugeType.BEGIN_INJ_PHASE, GaugeType.FUEL_CONSUMPTION),
                gaugeChanges.next().map { it.state.gaugeType },
            )
            repository.deleteIndicator(IndicatorType.FI_FUEL)
            assertEquals(emptyList<IndicatorItem>(), indicatorChanges.next())
        } finally {
            gaugeObserver.cancel()
            indicatorObserver.cancel()
            gaugeChanges.close()
            indicatorChanges.close()
        }
    }
}

private suspend fun <T> Channel<T>.next(): T = withTimeout(5_000) { receive() }
