package org.secu3.android.ui.dashboard

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
import org.secu3.android.models.packets.input.SensorsPacket

class DashboardPacketFlowTest {
    @Test
    fun freshCollectionWhileConnectedStartsWithoutPacket() = runBlocking {
        assertEquals(null, dashboardPacketFlow(flowOf(Connected), emptyFlow()).first())
    }

    @Test
    fun clearsLastPacketOnDisconnectAndWaitsForFreshPacketAfterReconnect() = runBlocking {
        val connection = MutableStateFlow<ConnectionState>(Connected)
        val packets = MutableSharedFlow<InputPacket>()
        val updates = Channel<DashboardViewData?>(Channel.UNLIMITED)
        val collector = launch { dashboardPacketFlow(connection, packets).collect(updates::send) }

        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        assertEquals(null, withTimeout(1_000) { updates.receive() })
        val oldPacket = SensorsPacket(rpm = 3000)
        packets.emit(oldPacket)
        assertEquals(3000f, withTimeout(1_000) { updates.receive() }?.rpm)

        connection.value = Disconnected
        assertEquals(null, withTimeout(1_000) { updates.receive() })
        withTimeout(1_000) { packets.subscriptionCount.first { it == 0 } }

        connection.value = Connected
        withTimeout(1_000) { packets.subscriptionCount.first { it > 0 } }
        assertEquals(null, withTimeout(1_000) { updates.receive() })
        assertEquals(null, withTimeoutOrNull(350) { updates.receive() })
        val freshPacket = SensorsPacket(rpm = 900)
        packets.emit(freshPacket)
        assertEquals(900f, withTimeout(1_000) { updates.receive() }?.rpm)

        collector.cancel()
        updates.close()
        Unit
    }
}
