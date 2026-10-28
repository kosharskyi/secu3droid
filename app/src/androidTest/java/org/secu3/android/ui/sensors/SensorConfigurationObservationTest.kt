package org.secu3.android.ui.sensors

import androidx.room.Room
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.usb.UsbManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.db.AppDatabase
import org.secu3.android.db.SensorConfigurationRepository
import org.secu3.android.db.models.GaugeState
import org.secu3.android.db.models.IndicatorState
import org.secu3.android.connection.BtConnection
import org.secu3.android.connection.ConnectionNotificationPublisher
import org.secu3.android.connection.ConnectionServiceController
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.connection.UsbConnection
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.models.sensors.IndicatorType
import org.secu3.android.ui.messages.UiMessageQueue
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.IsolatedUserPrefs
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.UserPrefs
import org.secu3.android.ui.common.SensorPacketObserver
import java.util.UUID

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
        val repository = SensorConfigurationRepository(database)
        val gaugeChanges = kotlinx.coroutines.channels.Channel<List<GaugeState>>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val gaugeObserver = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.observeGaugeStates().collect(gaugeChanges::send)
        }
        val indicatorChanges = kotlinx.coroutines.channels.Channel<List<IndicatorState>>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val indicatorObserver = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.observeIndicatorStates().collect(indicatorChanges::send)
        }
        try {
            assertEquals(emptyList<GaugeState>(), gaugeChanges.next())
            assertEquals(emptyList<IndicatorState>(), indicatorChanges.next())
            assertTrue(GaugeType.DISTANCE in repository.getAvailableGaugeTypes())

            repository.addGauge(GaugeType.MAP)
            assertEquals(listOf(GaugeType.MAP), gaugeChanges.next().map { it.gaugeType })
            assertTrue(GaugeType.MAP !in repository.getAvailableGaugeTypes())
            repository.addGauge(GaugeType.DISTANCE)
            val gaugesWithDistance = gaugeChanges.next()
            assertEquals(listOf(GaugeType.MAP, GaugeType.DISTANCE), gaugesWithDistance.map { it.gaugeType })
            assertTrue(gaugesWithDistance.single { it.gaugeType == GaugeType.DISTANCE }.isNumericView)
            assertFalse(GaugeType.MAP.let { gaugesWithDistance.single { gauge -> gauge.gaugeType == it }.isNumericView })

            repository.addGauge(GaugeType.CONSUMED_FUEL)
            val gaugesWithFuel = gaugeChanges.next()
            assertTrue(gaugesWithFuel.single { it.gaugeType == GaugeType.CONSUMED_FUEL }.isNumericView)
            assertTrue(GaugeType.FUEL_CONSUMPTION_HOURLY in repository.getAvailableGaugeTypes())
            repository.addGauge(GaugeType.FUEL_CONSUMPTION_HOURLY)
            val gaugesWithHourlyFuel = gaugeChanges.next()
            assertFalse(gaugesWithHourlyFuel.single { it.gaugeType == GaugeType.FUEL_CONSUMPTION_HOURLY }.isNumericView)
            val reordered = listOf(
                gaugesWithHourlyFuel.single { it.gaugeType == GaugeType.DISTANCE }.copy(idx = 0),
                gaugesWithHourlyFuel.single { it.gaugeType == GaugeType.MAP }.copy(idx = 1),
                gaugesWithHourlyFuel.single { it.gaugeType == GaugeType.CONSUMED_FUEL }.copy(idx = 2),
                gaugesWithHourlyFuel.single { it.gaugeType == GaugeType.FUEL_CONSUMPTION_HOURLY }.copy(idx = 3),
            )
            repository.updateGauges(reordered)
            assertEquals(listOf(GaugeType.DISTANCE, GaugeType.MAP, GaugeType.CONSUMED_FUEL, GaugeType.FUEL_CONSUMPTION_HOURLY), gaugeChanges.next().map { it.gaugeType })

            repository.addIndicator(IndicatorType.THROTTLE)
            assertEquals(listOf(IndicatorType.THROTTLE), indicatorChanges.next().map { it.indicatorType })
            assertTrue(IndicatorType.THROTTLE !in repository.getAvailableIndicatorTypes())
            repository.addIndicator(IndicatorType.GAS_VALVE)
            val indicators = indicatorChanges.next()
            assertEquals(listOf(IndicatorType.THROTTLE, IndicatorType.GAS_VALVE), indicators.map { it.indicatorType })
            repository.updateIndicators(listOf(indicators[1].copy(idx = 0), indicators[0].copy(idx = 1)))
            assertEquals(listOf(IndicatorType.GAS_VALVE, IndicatorType.THROTTLE), indicatorChanges.next().map { it.indicatorType })

            repository.deleteGauge(GaugeType.MAP)
            assertEquals(listOf(GaugeType.DISTANCE, GaugeType.CONSUMED_FUEL, GaugeType.FUEL_CONSUMPTION_HOURLY), gaugeChanges.next().map { it.gaugeType })
            assertTrue(GaugeType.MAP in repository.getAvailableGaugeTypes())
            repository.deleteIndicator(IndicatorType.GAS_VALVE)
            assertEquals(listOf(IndicatorType.THROTTLE), indicatorChanges.next().map { it.indicatorType })
            assertTrue(IndicatorType.GAS_VALVE in repository.getAvailableIndicatorTypes())
        } finally {
            gaugeObserver.cancelAndJoin()
            indicatorObserver.cancelAndJoin()
            gaugeChanges.close()
            indicatorChanges.close()
        }
    }

    @Test
    fun viewModelCombinesRoomConfigurationWithPacketsAndDisplaySettings() = runBlocking {
        val fixture = Fixture(database)
        try {
            val gauges = listOf(GaugeType.FUEL_CONSUMPTION, GaugeType.CONSUMED_FUEL, GaugeType.BEGIN_INJ_PHASE)
            gauges.forEach { fixture.repository.addGauge(it) }
            fixture.repository.addIndicator(IndicatorType.GAS_VALVE)

            val offlineGauges = withTimeout(5_000) {
                fixture.viewModel.gauges.first { items ->
                    items.map { it.state.gaugeType } == gauges && items.all { it.value.isEmpty() }
                }
            }
            assertEquals(listOf(GaugeType.FUEL_CONSUMPTION, GaugeType.CONSUMED_FUEL, GaugeType.BEGIN_INJ_PHASE), offlineGauges.map { it.state.gaugeType })
            assertFalse(withTimeout(5_000) { fixture.viewModel.indicators.first().single().isActive })

            fixture.repository.addGauge(GaugeType.RPM)
            val editedOfflineGauges = withTimeout(5_000) {
                fixture.viewModel.gauges.first { items ->
                    items.size == 4 && items.all { it.value.isEmpty() }
                }
            }
            assertEquals(GaugeType.RPM, editedOfflineGauges.last().state.gaugeType)

            fixture.connectionStates.value = Connected
            withTimeout(5_000) { fixture.packetFlow.subscriptionCount.first { it >= 2 } }
            fixture.packetFlow.emit(SensorsPacket(sensorsFlags = 1 shl 2, injTimBegin = 420f, cons_fuel = 20f).apply { inj_ffd = 8f })
            val liveGauges = withTimeout(5_000) {
                fixture.viewModel.gauges.first { items -> items.single { it.state.gaugeType == GaugeType.RPM }.value == "0" }
            }
            assertEquals("8.00", liveGauges.single { it.state.gaugeType == GaugeType.FUEL_CONSUMPTION }.value)
            assertEquals("20.000", liveGauges.single { it.state.gaugeType == GaugeType.CONSUMED_FUEL }.value)
            assertEquals("420.0", liveGauges.single { it.state.gaugeType == GaugeType.BEGIN_INJ_PHASE }.value)
            assertTrue(withTimeout(5_000) { fixture.viewModel.indicators.first { it.single().isActive }.single().isActive })

            fixture.prefs.setInjectionTimingMode(InjectionTimingMode.Btdc360)
            withTimeout(5_000) { fixture.viewModel.state.first { it.injectionTimingMode == InjectionTimingMode.Btdc360 } }
            val transformedGauge = withTimeout(5_000) {
                fixture.viewModel.gauges.first { items -> items.single { it.state.gaugeType == GaugeType.BEGIN_INJ_PHASE }.value == "-300.0" }
            }
            assertEquals("-300.0", transformedGauge.single { it.state.gaugeType == GaugeType.BEGIN_INJ_PHASE }.value)

            fixture.connectionStates.value = Disconnected
            val unavailableGauges = withTimeout(5_000) {
                fixture.viewModel.gauges.first { items -> items.all { it.value.isEmpty() } }
            }
            assertEquals(4, unavailableGauges.size)
            assertFalse(withTimeout(5_000) { fixture.viewModel.indicators.first { !it.single().isActive }.single().isActive })
        } finally {
            fixture.close()
        }
    }

    private inner class Fixture(database: AppDatabase) {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val prefsFixture = IsolatedUserPrefs(context, "sensor-viewmodel-${UUID.randomUUID()}")
        val prefs: UserPrefs = prefsFixture.prefs
        private val usb = UsbConnection(context.getSystemService(Context.USB_SERVICE) as UsbManager)
        private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        private val bluetooth = BtConnection(prefs, bluetoothManager)
        private val logger = SecuLogger(prefs, FileHelper(context))
        private val serviceController = ConnectionServiceController(context, ConnectionNotificationPublisher(context))
        private val manager = Secu3ConnectionManager(usb, bluetooth, prefs, bluetoothManager, logger, serviceController)
        val connectionStates = MutableStateFlow<org.secu3.android.connection.ConnectionState>(Disconnected)
        val packetFlow = MutableSharedFlow<InputPacket>(extraBufferCapacity = 8)
        val repository = SensorConfigurationRepository(database)
        private val viewModelStore = ViewModelStore()
        val viewModel: SensorsViewModel

        init {
            val observer = SensorPacketObserver(connectionStates, packetFlow)
            lateinit var created: SensorsViewModel
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                created = SensorsViewModel(
                    manager,
                    observer,
                    prefs,
                    logger,
                    repository,
                    SavedStateHandle(),
                    UiMessageQueue(),
                )
                viewModelStore.put("sensors", created)
            }
            viewModel = created
        }

        suspend fun close() {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.clear() }
            serviceController.stop()
            (Secu3ConnectionManager::class.java.getDeclaredField("scope").apply { isAccessible = true }.get(manager) as CoroutineScope).cancel()
            usb.cancelScopes()
            bluetooth.cancelScopes()
            logger.shutdownForTests()
            (SecuLogger::class.java.getDeclaredField("loggerScope").apply { isAccessible = true }.get(logger) as CoroutineScope).cancel()
            prefsFixture.close()
        }
    }

    private suspend fun <T> kotlinx.coroutines.channels.Channel<T>.next(): T = withTimeout(5_000) { receive() }
}
