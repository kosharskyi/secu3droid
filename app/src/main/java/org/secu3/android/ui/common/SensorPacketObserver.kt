package org.secu3.android.ui.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.AdcRawDatPacket
import org.secu3.android.models.packets.input.SensorsPacket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorPacketObserver internal constructor(
    private val connectionStates: Flow<ConnectionState>,
    private val packets: Flow<InputPacket>,
) {
    @Inject
    constructor(manager: Secu3ConnectionManager) : this(
        connectionStates = manager.connectionStateFlow,
        packets = manager.receivedPacketFlow,
    )

    fun observeSensors(updateIntervalMs: Long = DEFAULT_UPDATE_INTERVAL_MS): Flow<SensorsPacket?> =
        observePackets<SensorsPacket>(updateIntervalMs)

    fun observeRawSensors(updateIntervalMs: Long = DEFAULT_UPDATE_INTERVAL_MS): Flow<AdcRawDatPacket?> =
        observePackets<AdcRawDatPacket>(updateIntervalMs)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private inline fun <reified T : InputPacket> observePackets(updateIntervalMs: Long): Flow<T?> {
        require(updateIntervalMs > 0) { "Sensor packet update interval must be positive" }
        return connectionStates.flatMapLatest { state ->
            if (state != Connected) {
                flowOf(null)
            } else {
                packets
                    .filterIsInstance<T>()
                    .sample(updateIntervalMs)
                    .map<T, T?> { it }
                    .onStart { emit(null) }
            }
        }
    }

    companion object {
        const val DEFAULT_UPDATE_INTERVAL_MS = 100L
    }
}
