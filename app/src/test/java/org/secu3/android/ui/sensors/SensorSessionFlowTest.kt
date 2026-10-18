package org.secu3.android.ui.sensors

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.SensorsPacket

class SensorSessionFlowTest {
    @Test
    fun clearsPacketsOnDisconnectAndStartsReconnectWithoutPreviousSessionValues(): Unit = runBlocking {
        val connection = MutableStateFlow<org.secu3.android.connection.ConnectionState>(Disconnected)
        val packets = MutableSharedFlow<InputPacket>()
        val snapshots = Channel<SensorSessionSnapshot>(Channel.UNLIMITED)
        val collector = launch { sensorSessionFlow(connection, packets).collect(snapshots::send) }

        connection.value = Connected
        assertEquals(SensorSessionSnapshot(), withTimeout(1_000) { snapshots.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        val firstRawPacket = AdcRawDatPacket(addI1 = 2.5f)
        packets.emit(firstRawPacket)
        assertEquals(firstRawPacket, withTimeout(1_000) { snapshots.receive() }.rawSensors)
        val firstSessionPacket = SensorsPacket(rpm = 2400)
        packets.emit(firstSessionPacket)
        assertEquals(firstSessionPacket, withTimeout(1_000) { snapshots.receive() }.sensors)

        connection.value = Disconnected
        assertEquals(SensorSessionSnapshot(), withTimeout(1_000) { snapshots.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it == 0 } }
        connection.value = Connected
        assertEquals(SensorSessionSnapshot(), withTimeout(1_000) { snapshots.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }

        val nextSessionPacket = SensorsPacket(rpm = 900)
        packets.emit(nextSessionPacket)
        val reconnectedSnapshot = withTimeout(1_000) { snapshots.receive() }
        assertEquals(nextSessionPacket, reconnectedSnapshot.sensors)
        assertEquals(null, reconnectedSnapshot.rawSensors)
        collector.cancel()
        snapshots.close()
        Unit
    }

    @Test
    fun cachedFirmwareSeedsOnlyAnAlreadyConnectedFirstSession(): Unit = runBlocking {
        val connection = MutableStateFlow<org.secu3.android.connection.ConnectionState>(Connected)
        val packets = MutableSharedFlow<InputPacket>()
        val cachedFirmware = FirmwareInfoPacket(tag = "cached")
        val snapshots = Channel<SensorSessionSnapshot>(Channel.UNLIMITED)
        val collector = launch {
            sensorSessionFlow(connection, packets, initialFirmware = cachedFirmware).collect(snapshots::send)
        }

        assertEquals(cachedFirmware, withTimeout(1_000) { snapshots.receive() }.firmware)
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        connection.value = Disconnected
        assertEquals(SensorSessionSnapshot(), withTimeout(1_000) { snapshots.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it == 0 } }
        connection.value = Connected
        assertEquals(SensorSessionSnapshot(), withTimeout(1_000) { snapshots.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }

        collector.cancel()
        snapshots.close()
        Unit
    }

    @Test
    fun disconnectCancelsAnUnsampledPacketAndClearsRawData(): Unit = runBlocking {
        val connection = MutableStateFlow<org.secu3.android.connection.ConnectionState>(Connected)
        val packets = MutableSharedFlow<InputPacket>()
        val snapshots = Channel<SensorSessionSnapshot>(Channel.UNLIMITED)
        val collector = launch { sensorSessionFlow(connection, packets).collect(snapshots::send) }

        assertEquals(SensorSessionSnapshot(), withTimeout(1_000) { snapshots.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        packets.emit(AdcRawDatPacket(addI1 = 3f))
        connection.value = Disconnected
        assertEquals(SensorSessionSnapshot(), withTimeout(1_000) { snapshots.receive() })
        assertEquals(null, withTimeoutOrNull(250) { snapshots.receive() })
        collector.cancel()
        snapshots.close()
        Unit
    }

    @Test
    fun disconnectedStateDoesNotCollectOrExposePackets() = runBlocking {
        val connection = MutableStateFlow<org.secu3.android.connection.ConnectionState>(Disconnected)
        val packets = MutableSharedFlow<InputPacket>()
        val snapshots = sensorSessionFlow(connection, packets).take(1).toList()
        assertEquals(listOf(SensorSessionSnapshot()), snapshots)
    }

    @Test
    fun cachedFirmwareIsNotSeededAfterAnInitialDisconnectedState() = runBlocking {
        val cachedFirmware = FirmwareInfoPacket(tag = "stale")
        val snapshots = sensorSessionFlow(
            connectionStates = flow {
                emit(Disconnected)
                delay(1)
                emit(Connected)
            },
            packets = emptyFlow(),
            initialFirmware = cachedFirmware,
        ).take(2).toList()
        assertEquals(listOf(SensorSessionSnapshot(), SensorSessionSnapshot()), snapshots)
    }

    @Test
    fun firmwareSeedDecisionIsFreshForEachCollection() = runBlocking {
        val cachedFirmware = FirmwareInfoPacket(tag = "cached")
        val session = sensorSessionFlow(
            connectionStates = flowOf(Connected),
            packets = emptyFlow(),
            initialFirmware = cachedFirmware,
        )

        repeat(2) {
            assertEquals(cachedFirmware, session.first().firmware)
        }
    }
}
