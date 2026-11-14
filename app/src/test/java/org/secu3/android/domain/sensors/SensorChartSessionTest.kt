package org.secu3.android.domain.sensors

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.sensors.GaugeType

class SensorChartSessionTest {
    @Test fun keepsPacketBurstsInOneSharedTenSecondWindowAndCapsDenseHistory() = runBlocking {
        val values = listOf(1_200, 1_200, 6_400, 1_200)
        val connections = MutableStateFlow<ConnectionState>(Connected)
        val packets = MutableSharedFlow<InputPacket>()
        var nowNanos = 100_000_000L
        val session = SensorChartSession(
            connections,
            { onSubscribed -> packets.onSubscription { onSubscribed() } },
            { nowNanos },
        )
        val snapshots = Channel<SensorChartSnapshot>(Channel.UNLIMITED)
        val collector = launch { session.observe().collect(snapshots::send) }
        try {
            assertEquals(SensorChartSnapshot(Connected), withTimeout(1_000) { snapshots.receive() })
            packets.emit(AdcRawDatPacket())
            values.forEachIndexed { index, rpm ->
                nowNanos += 2_000_000L
                packets.emit(SensorsPacket(rpm = rpm))
            }
            val full = withTimeout(1_000) {
                var snapshot = snapshots.receive()
                while (snapshot.packetHistory.size < values.size) snapshot = snapshots.receive()
                snapshot
            }
            assertEquals(values, full.packetHistory.map { it.packet.rpm })
            assertEquals(listOf(2L, 4L, 6L, 8L), full.packetHistory.map { it.elapsedMillis })

            connections.value = Disconnected
            assertEquals(emptyList<SensorChartPacketSample>(), withTimeout(1_000) { snapshots.receive().packetHistory })
        } finally {
            collector.cancel()
        }

        val burst = (0..SensorChartSession.MAX_SAMPLES).toList()
        var now = 0L
        val bounded = SensorChartSession(
            MutableStateFlow<ConnectionState>(Connected),
            { onSubscribed -> flow {
                onSubscribed()
                burst.forEach { rpm -> now += 1_000_000L; emit(SensorsPacket(rpm = rpm)) }
                emit(SensorsPacket(rpm = burst.last()))
            } },
            { now },
        )
        val final = bounded.observe().first { it.packetHistory.size == SensorChartSession.MAX_SAMPLES }
        assertEquals(SensorChartSession.MAX_SAMPLES, final.packetHistory.size)
        assertTrue(final.packetHistory.last().packet.rpm > final.packetHistory.first().packet.rpm)
    }

    @Test fun restartingCollectionStartsFreshAndReconnectFencesOldSamples() = runBlocking {
        val connections = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 8)
        val packets = MutableSharedFlow<InputPacket>()
        val snapshots = Channel<SensorChartSnapshot>(Channel.UNLIMITED)
        val session = SensorChartSession(
            connections,
            { onSubscribed -> packets.onSubscription { onSubscribed() } },
            System::nanoTime,
        )
        val collector = launch { session.observe().collect(snapshots::send) }
        try {
            connections.emit(Connected)
            assertEquals(SensorChartSnapshot(Connected), withTimeout(1_000) { snapshots.receive() })
            packets.emit(SensorsPacket(rpm = 3_100))
            assertEquals(3_100, withTimeout(1_000) { snapshots.receive().packetHistory.single().packet.rpm })

            connections.emit(Disconnected)
            connections.emit(Connected)
            val reconnectedState = withTimeout(1_000) {
                var snapshot = snapshots.receive()
                while (snapshot.connectionState != Connected) snapshot = snapshots.receive()
                snapshot
            }
            assertEquals(SensorChartSnapshot(Connected), reconnectedState)
            packets.emit(SensorsPacket(rpm = 900))
            val reconnected = withTimeout(1_000) {
                var snapshot = snapshots.receive()
                while (snapshot.packetHistory.isEmpty()) snapshot = snapshots.receive()
                snapshot
            }
            assertEquals(listOf(900), reconnected.packetHistory.map { it.packet.rpm })
        } finally {
            collector.cancel()
        }
    }
}
