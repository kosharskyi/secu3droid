package org.secu3.android.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket

/** Replaces physical I/O while exercising the real transport parser and manager event contract. */
internal class ConnectionTestDriver(
    private val manager: Secu3ConnectionManager,
    val connection: Connection<*>,
) {
    @Suppress("UNCHECKED_CAST")
    val events = field(Connection::class.java, "_sessionEvents").get(connection) as MutableSharedFlow<ConnectionEvent>
    @Suppress("UNCHECKED_CAST")
    val packets = field(Connection::class.java, "_packets").get(connection) as MutableSharedFlow<ConnectionPacket>
    val generation get() = connection.generation

    fun select(transport: String): Long = synchronized(manager) {
        require(transport == if (connection is UsbConnection) "USB" else "BLUETOOTH")
        val counter = field(Secu3ConnectionManager::class.java, "nextGeneration")
        val generation = counter.getLong(manager) + 1
        counter.setLong(manager, generation)
        (field(Connection::class.java, "connectionJob").get(connection) as? Job)?.cancel()
        clearFirmware()
        set("generation", generation)
        set("connectionJob", Job())
        set("isRunning", true)
        set("nextAttemptId", 1L)
        set("activeAttemptId", 1L)
        set("currentConnectionState", Connecting)
        Secu3ConnectionManager::class.java.getDeclaredMethod("selectConnection", Connection::class.java, Long::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(manager, connection, generation)
        if (connection is BtConnection) publish(Connected)
        return generation
    }

    fun installEndpoint(endpoint: Any) { set("endpoint", endpoint); set("isRunning", true) }
    fun setAttempt(attempt: Long) {
        set("activeAttemptId", attempt)
        set("nextAttemptId", attempt)
        clearFirmware()
        if (connection.currentConnectionState != UsbAwaitingEcu) set("currentConnectionState", Connecting)
    }
    suspend fun emit(raw: RawPacket, attempt: Long = connection.activeAttemptId ?: 1L) = connection.receivePacket(attempt, raw)

    fun publish(state: ConnectionState, end: ConnectionEvent.End? = null) {
        Connection::class.java.getDeclaredMethod("publishState", Job::class.java, ConnectionState::class.java, ConnectionEvent.End::class.java)
            .apply { isAccessible = true }.invoke(connection, job(), state, end)
        applyLatest()
    }
    fun fail(state: ConnectionState, end: ConnectionEvent.End? = null) {
        Connection::class.java.getDeclaredMethod("finishAttempt", Job::class.java, ConnectionState::class.java, ConnectionEvent.End::class.java)
            .apply { isAccessible = true }.invoke(connection, job(), state, end)
        applyLatest()
    }
    suspend fun emitEvent(event: ConnectionEvent) {
        if (event.generation == connection.generation) {
            if (event.state != Connected) clearFirmware()
            set("currentConnectionState", event.state)
            if (event.end == ConnectionEvent.End.STOPPED) set("isRunning", false)
        }
        val sequence = field(Connection::class.java, "nextEventSequence")
        val next = sequence.getLong(connection) + 1
        sequence.setLong(connection, next)
        events.emit(event.copy(firmware = firmware(), sequence = next))
    }

    private fun clearFirmware() { set("firmware", null) }
    private fun firmware() = field(Connection::class.java, "firmware").get(connection) as FirmwareInfoPacket?

    fun shutdown() {
        (field(Connection::class.java, "scope").get(connection) as CoroutineScope).cancel()
        (field(Connection::class.java, "backgroundScope").get(connection) as CoroutineScope).cancel()
    }

    private fun applyLatest() {
        Secu3ConnectionManager::class.java.getDeclaredMethod("handleConnectionEvent", Connection::class.java, ConnectionEvent::class.java)
            .apply { isAccessible = true }.invoke(manager, connection, connection.latestEvent)
    }
    private fun job() = field(Connection::class.java, "connectionJob").get(connection) as Job
    private fun set(name: String, value: Any?) = field(Connection::class.java, name).set(connection, value)
    private fun field(type: Class<*>, name: String) = type.getDeclaredField(name).apply { isAccessible = true }
}
