package org.secu3.android.ui.sensors

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.ui.common.SensorPacketObserver

class SensorPacketObserverTest {
    @Test
    fun startsEmptyAndEachConsumerCanChooseItsOwnSamplingInterval(): Unit = runBlocking {
        val connection = MutableStateFlow<ConnectionState>(Connected)
        val packets = MutableSharedFlow<InputPacket>()
        val observer = SensorPacketObserver(connection, packets)
        val sensors = Channel<SensorsPacket?>(Channel.UNLIMITED)
        val rawSensors = Channel<AdcRawDatPacket?>(Channel.UNLIMITED)
        val sensorCollector = launch { observer.observeSensors(updateIntervalMs = 40).collect(sensors::send) }
        val rawCollector = launch { observer.observeRawSensors(updateIntervalMs = 300).collect(rawSensors::send) }

        assertEquals(null, withTimeout(1_000) { sensors.receive() })
        assertEquals(null, withTimeout(1_000) { rawSensors.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it == 2 } }
        delay(80)

        val sensorPacket = SensorsPacket(rpm = 2100)
        val rawPacket = AdcRawDatPacket(addI1 = 2.5f)
        packets.emit(sensorPacket)
        packets.emit(rawPacket)
        assertEquals(sensorPacket, withTimeout(150) { sensors.receive() })
        assertEquals(null, withTimeoutOrNull(80) { rawSensors.receive() })
        assertEquals(rawPacket, withTimeout(350) { rawSensors.receive() })

        sensorCollector.cancel()
        rawCollector.cancel()
        sensors.close()
        rawSensors.close()
    }

    @Test
    fun disconnectImmediatelyClearsAndCancelsPendingSampleThenReconnectWaitsForFreshPacket(): Unit = runBlocking {
        val connection = MutableStateFlow<ConnectionState>(Connected)
        val packets = MutableSharedFlow<InputPacket>()
        val observer = SensorPacketObserver(connection, packets)
        val updates = Channel<SensorsPacket?>(Channel.UNLIMITED)
        val rawUpdates = Channel<AdcRawDatPacket?>(Channel.UNLIMITED)
        val collector = launch { observer.observeSensors(updateIntervalMs = 300).collect(updates::send) }
        val rawCollector = launch { observer.observeRawSensors(updateIntervalMs = 300).collect(rawUpdates::send) }

        assertEquals(null, withTimeout(1_000) { updates.receive() })
        assertEquals(null, withTimeout(1_000) { rawUpdates.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it == 2 } }
        packets.emit(SensorsPacket(rpm = 2400))
        packets.emit(AdcRawDatPacket(addI1 = 3.5f))
        connection.value = Disconnected
        assertEquals(null, withTimeout(1_000) { updates.receive() })
        assertEquals(null, withTimeout(1_000) { rawUpdates.receive() })
        assertEquals(null, withTimeoutOrNull(350) { updates.receive() })
        assertEquals(null, withTimeoutOrNull(350) { rawUpdates.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it == 0 } }

        connection.value = Connected
        assertEquals(null, withTimeout(1_000) { updates.receive() })
        assertEquals(null, withTimeout(1_000) { rawUpdates.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        assertEquals(null, withTimeoutOrNull(50) { updates.receive() })
        val freshPacket = SensorsPacket(rpm = 900)
        packets.emit(freshPacket)
        assertEquals(freshPacket, withTimeout(1_000) { updates.receive() })

        val freshRawPacket = AdcRawDatPacket(addI1 = 4.5f)
        packets.emit(freshRawPacket)
        assertEquals(freshRawPacket, withTimeout(1_000) { rawUpdates.receive() })

        collector.cancel()
        rawCollector.cancel()
        updates.close()
        rawUpdates.close()
    }

    @Test
    fun rapidDisconnectAndReconnectDoesNotLeakPendingOldSample(): Unit = runBlocking {
        val connection = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 8)
        connection.emit(Connected)
        val packets = MutableSharedFlow<InputPacket>()
        val observer = SensorPacketObserver(connection, packets)
        val updates = Channel<SensorsPacket?>(Channel.UNLIMITED)
        val collector = launch { observer.observeSensors(updateIntervalMs = 250).collect(updates::send) }

        assertEquals(null, withTimeout(1_000) { updates.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        packets.emit(SensorsPacket(rpm = 3100))
        connection.emit(Disconnected)
        connection.emit(Connected)

        assertEquals(null, withTimeout(1_000) { updates.receive() })
        assertEquals(null, withTimeoutOrNull(300) { updates.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        val freshPacket = SensorsPacket(rpm = 1200)
        packets.emit(freshPacket)
        assertEquals(freshPacket, withTimeout(1_000) { updates.receive() })

        collector.cancel()
        updates.close()
    }
}
