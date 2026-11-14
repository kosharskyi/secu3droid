package org.secu3.android.domain.sensors

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.sensors.GaugeType
import javax.inject.Inject

data class SensorChartSample(val elapsedMillis: Long, val value: Float)

data class SensorChartPacketSample(val elapsedMillis: Long, val packet: SensorsPacket)

data class SensorChartSnapshot(
    val connectionState: ConnectionState?,
    val packetHistory: List<SensorChartPacketSample> = emptyList(),
)

class SensorChartSession internal constructor(
    private val connectionStates: Flow<ConnectionState>,
    private val packets: (() -> Unit) -> Flow<InputPacket>,
    private val monotonicNanos: () -> Long,
) {
    @Inject
    constructor(manager: Secu3ConnectionManager) : this(
        manager.connectionStateFlow,
        manager::receivedPacketFlow,
        System::nanoTime,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<SensorChartSnapshot> =
        connectionStates.flatMapLatest { state ->
            if (state == Connected) {
                channelFlow {
                    send(SensorChartSnapshot(Connected))
                    val startNanos = monotonicNanos()
                    val history = ArrayDeque<SensorChartPacketSample>()
                    val historyLock = Any()
                    launch {
                        packets {}.filterIsInstance<SensorsPacket>().collect { packet ->
                            val elapsedMillis = (monotonicNanos() - startNanos) / 1_000_000L
                            synchronized(historyLock) {
                                while (history.isNotEmpty() && history.first().elapsedMillis < elapsedMillis - WINDOW_MILLIS) {
                                    history.removeFirst()
                                }
                                if (history.size == MAX_SAMPLES) history.removeFirst()
                                history.addLast(SensorChartPacketSample(elapsedMillis, packet))
                            }
                        }
                    }
                    while (true) {
                        delay(SNAPSHOT_INTERVAL_MILLIS)
                        val snapshot = synchronized(historyLock) { history.toList() }
                        send(SensorChartSnapshot(Connected, snapshot))
                    }
                }
            } else {
                flowOf(SensorChartSnapshot(state))
            }
        }.buffer(0)

    fun readings(snapshot: SensorChartSnapshot, gaugeTypes: Set<GaugeType>): Map<GaugeType, List<SensorChartSample>> {
        if (snapshot.connectionState != Connected) return emptyMap()
        return gaugeTypes.associateWith { type ->
            snapshot.packetHistory.map { sample ->
                SensorChartSample(sample.elapsedMillis, type.getGaugeItem(sample.packet))
            }.filter { it.value.isFinite() }
        }
    }

    companion object {
        const val WINDOW_MILLIS = 10_000L
        internal const val MAX_SAMPLES = 4_096
        private const val SNAPSHOT_INTERVAL_MILLIS = 100L
    }
}
