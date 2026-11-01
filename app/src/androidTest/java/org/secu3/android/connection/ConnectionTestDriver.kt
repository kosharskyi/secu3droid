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
    val packets = field(Connection::class.java, "_packets").get(connection) as MutableSharedFlow<ConnectionPacket>

    fun select(transport: String): Job = synchronized(manager) {
        require(transport == if (connection is UsbConnection) "USB" else "BLUETOOTH")
        Secu3ConnectionManager::class.java.getDeclaredMethod("disconnectForTransportChange")
            .apply { isAccessible = true }.invoke(manager)
        (field(Connection::class.java, "connectionJob").get(connection) as? Job)?.cancel()
        clearFirmware()
        val runJob = Job()
        set("connectionJob", runJob)
        set("isRunning", true)
        set("nextAttemptId", 1L)
        set("activeAttemptId", 1L)
        set("currentConnectionState", Connecting)
        Secu3ConnectionManager::class.java.getDeclaredMethod("selectConnection", Connection::class.java)
            .apply { isAccessible = true }.invoke(manager, connection)
        if (connection is BtConnection) publish(Connected)
        return runJob
    }

    fun installEndpoint(endpoint: Any) { set("endpoint", endpoint); set("isRunning", true) }
    fun setAttempt(attempt: Long) {
        set("activeAttemptId", attempt)
        set("nextAttemptId", attempt)
        clearFirmware()
        if (connection.currentConnectionState != UsbAwaitingEcu &&
            connection.currentConnectionState != UsbReconnecting &&
            connection.currentConnectionState != UsbReconnectingAwaitingEcu
        ) {
            set("currentConnectionState", Connecting)
        }
    }
    suspend fun emit(raw: RawPacket, attempt: Long = connection.activeAttemptId ?: 1L) = connection.receivePacket(attempt, raw)

    fun publish(state: ConnectionState, end: ConnectionEvent.End? = null) = synchronized(manager) {
        synchronized(field(Connection::class.java, "connectionLock").get(connection)) {
            if (state != Connected || end != null) clearFirmware()
            set("currentConnectionState", state)
            if (end != null) {
                set("activeAttemptId", null)
                if (end == ConnectionEvent.End.STOPPED) set("isRunning", false)
            }
            enqueueEventLocked(ConnectionEvent(state, firmware(), end))
        }
        drainPendingEvents()
    }
    fun fail(state: ConnectionState, end: ConnectionEvent.End? = null): Unit = synchronized(manager) {
        val lock = field(Connection::class.java, "connectionLock").get(connection)
        val endpoint: Any?
        synchronized(lock) {
            if (connection.activeAttemptId == null) return
            set("activeAttemptId", null)
            clearFirmware()
            set("currentConnectionState", state)
            endpoint = Connection::class.java.getDeclaredMethod("detachCurrentEndpoint")
                .apply { isAccessible = true }.invoke(connection)
            if (end == ConnectionEvent.End.STOPPED) set("isRunning", false)
            enqueueEventLocked(ConnectionEvent(state, firmware(), end))
        }
        if (endpoint != null) {
            Connection::class.java.getDeclaredMethod("closeEndpoint", Any::class.java)
                .apply { isAccessible = true }.invoke(connection, endpoint)
        }
        drainPendingEvents()
    }
    suspend fun emitEvent(event: ConnectionEvent) {
        publish(event.state, event.end)
    }

    private fun enqueueEventLocked(event: ConnectionEvent) {
        Connection::class.java.getDeclaredMethod("enqueueEventLocked", ConnectionEvent::class.java)
            .apply { isAccessible = true }.invoke(connection, event)
    }

    private fun drainPendingEvents() {
        Secu3ConnectionManager::class.java.getDeclaredMethod("drainPendingEvents", Connection::class.java)
            .apply { isAccessible = true }.invoke(manager, connection)
    }

    private fun clearFirmware() { set("firmware", null) }
    private fun firmware() = field(Connection::class.java, "firmware").get(connection) as FirmwareInfoPacket?

    fun shutdown() {
        (field(Connection::class.java, "scope").get(connection) as CoroutineScope).cancel()
        (field(Connection::class.java, "backgroundScope").get(connection) as CoroutineScope).cancel()
    }

    private fun set(name: String, value: Any?) = field(Connection::class.java, name).set(connection, value)
    private fun field(type: Class<*>, name: String) = type.getDeclaredField(name).apply { isAccessible = true }
}
