package org.secu3.android.connection

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs

internal data class ConnectionPacket(val attemptId: Long, val packet: InputPacket)

abstract class Connection<E : Any> {
    protected val OUTPUT_PACKET_SYMBOL = 0x21
    protected val END_PACKET_SYMBOL = 0x0D
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Preference observation, endpoint cleanup and writes survive individual stop/start cycles.
    protected val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val connectionLock = Any()
    @Volatile protected var connectionJob: Job? = null
    @Volatile var isRunning = false
        protected set
    @Volatile var generation = 0L
        protected set
    private var nextAttemptId = 0L
    private var nextEventSequence = 0L
    @Volatile internal var latestEvent = ConnectionEvent(0, Disconnected)
        private set
    @Volatile internal var activeAttemptId: Long? = null
        private set
    internal val readyAttemptId: Long? get() = activeAttemptId.takeIf { currentConnectionState == Connected }
    @Volatile var currentConnectionState: ConnectionState = Disconnected
        protected set
    @Volatile private var currentFuelFlowPulsesPerLitre: Int? = null
    @Volatile private var firmware: FirmwareInfoPacket? = null
    protected val _sessionEvents = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)
    val sessionEvents = _sessionEvents.asSharedFlow()
    private val _packets = MutableSharedFlow<ConnectionPacket>()
    val receivedPacketFlow: Flow<InputPacket> = _packets
        .filter { it.attemptId == activeAttemptId }.map { it.packet }

    private val endpointLock = Any()
    private var endpoint: E? = null
    private val outboundWrites by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SerialWriteQueue(
            scope = backgroundScope,
            isCurrentEndpoint = ::isCurrentEndpoint,
            write = ::writeEndpoint,
            afterWrite = { afterEndpointWrite() },
            onFailure = ::onOutboundWriteFailure,
        )
    }
    val isConnected: Boolean
        get() = synchronized(endpointLock) { endpoint?.let(::isEndpointOpen) == true }
    val isReady: Boolean get() = synchronized(connectionLock) { readyAttemptId != null && isConnected }
    open val allowsManualDisconnect: Boolean get() = true

    /** A one-shot response cannot cross a reconnect, even on the same transport. */
    fun receivedPacketFlow(onSubscribed: () -> Unit): Flow<InputPacket> = flow {
        val attempt = activeAttemptId ?: return@flow awaitCancellation()
        emitAll(_packets.onSubscription {
            if (activeAttemptId == attempt) onSubscribed()
        }.filter { it.attemptId == attempt && activeAttemptId == attempt }.map { it.packet })
    }

    protected fun observePacketCalibration(prefs: UserPrefs) {
        backgroundScope.launch {
            prefs.observeSettings { Log.e(javaClass.simpleName, "Could not read packet calibration", it) }
                .collect { currentFuelFlowPulsesPerLitre = it.fuelFlowPulsesPerLitre }
        }
    }

    protected fun launchConnection(generation: Long, run: suspend (Job) -> Unit) {
        stopConnection()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val owner = currentCoroutineContext()[Job]!!
            publishState(owner, InProgress)
            try { run(owner) } finally {
                synchronized(connectionLock) { if (connectionJob === owner) connectionJob = null }
            }
        }
        synchronized(connectionLock) {
            this.generation = generation
            connectionJob = job
            isRunning = true
        }
        job.start()
    }

    protected fun isCurrent(job: Job): Boolean = synchronized(connectionLock) {
        connectionJob === job && job.isActive && isRunning
    }

    protected fun beginAttempt(job: Job): Long? = synchronized(connectionLock) {
        if (!isCurrent(job)) return null
        (++nextAttemptId).also {
            activeAttemptId = it
            firmware = null
        }
    }

    protected fun isCurrentAttempt(attempt: Long): Boolean = isRunning && activeAttemptId == attempt

    protected fun publishState(job: Job, state: ConnectionState, end: ConnectionEvent.End? = null) {
        synchronized(connectionLock) {
            if (connectionJob !== job || !job.isActive) return
            currentConnectionState = state
            latestEvent = ConnectionEvent(generation, state, firmware, end, ++nextEventSequence)
            _sessionEvents.tryEmit(latestEvent)
        }
    }

    protected fun markConnected(job: Job, attempt: Long) {
        synchronized(connectionLock) {
            if (!isCurrent(job) || !isCurrentAttempt(attempt)) return
            publishState(job, Connected)
        }
    }

    /** Invalidating before cleanup prevents a late parsed reply from reopening the attempt. */
    protected fun finishAttempt(job: Job, state: ConnectionState, end: ConnectionEvent.End? = null) {
        invalidateAttempt(job, activeAttemptId, state, end)?.let(::closeEndpoint)
    }

    /** State invalidation is atomic; closing the captured resource happens outside the lock. */
    protected fun invalidateAttempt(job: Job, attempt: Long?, state: ConnectionState, end: ConnectionEvent.End? = null): E? = synchronized(connectionLock) {
        if (!isCurrent(job) || activeAttemptId != attempt) return null
        activeAttemptId = null
        firmware = null
        val target = detachCurrentEndpoint()
        publishState(job, state, end)
        if (end == ConnectionEvent.End.STOPPED) isRunning = false
        target
    }

    internal suspend fun receivePacket(attempt: Long, raw: RawPacket) {
        val job = connectionJob ?: return
        if (!isCurrentAttempt(attempt) || !isCurrent(job)) return
        val packet = raw.parse(firmware, currentFuelFlowPulsesPerLitre) ?: return
        synchronized(connectionLock) {
            if (!isCurrentAttempt(attempt) || !isCurrent(job)) return
            if (packet is FirmwareInfoPacket) {
                firmware = packet
                onFirmwarePacket(job, attempt)
            }
        }
        if (isCurrentAttempt(attempt) && isCurrent(job)) _packets.emit(ConnectionPacket(attempt, packet))
    }

    protected open fun onFirmwarePacket(job: Job, attempt: Long) {
        if (readyAttemptId == attempt) publishState(job, Connected)
    }

    protected fun requestFirmware() = sendData(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))

    open fun stopConnection() {
        val wasRunning: Boolean
        val job: Job?
        synchronized(connectionLock) {
            wasRunning = isRunning
            isRunning = false
            job = connectionJob
            connectionJob = null
            activeAttemptId = null
            firmware = null
            currentConnectionState = Disconnected
        }
        job?.cancel()
        scope.coroutineContext.cancelChildren()
        if (wasRunning) disconnect()
    }

    protected open fun disconnect() {
        val target = detachCurrentEndpoint()
        val event = synchronized(connectionLock) {
            ConnectionEvent(generation, Disconnected, end = ConnectionEvent.End.STOPPED, sequence = ++nextEventSequence)
                .also { latestEvent = it }
        }
        backgroundScope.launch {
            target?.let(::closeEndpoint)
            _sessionEvents.emit(event)
        }
    }

    final fun sendData(packet: OutputPacket) {
        val target = currentEndpoint() ?: return
        outboundWrites.enqueue(target, encode(packet))
    }
    protected abstract fun isEndpointOpen(endpoint: E): Boolean
    protected abstract fun writeEndpoint(endpoint: E, bytes: ByteArray)
    protected abstract fun closeEndpoint(endpoint: E)
    protected open suspend fun afterEndpointWrite() = Unit
    protected open fun onOutboundWriteFailure(failure: Exception) {
        Log.e(javaClass.simpleName, "Error while sending data: ${failure.message}")
    }

    protected fun installEndpoint(endpoint: E): Boolean = synchronized(endpointLock) {
        if (!isRunning || this.endpoint != null) false
        else {
            this.endpoint = endpoint
            true
        }
    }

    protected fun currentEndpoint(): E? = synchronized(endpointLock) {
        endpoint?.takeIf { isRunning && isEndpointOpen(it) }
    }

    protected fun isCurrentEndpoint(endpoint: E): Boolean = synchronized(endpointLock) {
        isRunning && this.endpoint === endpoint && isEndpointOpen(endpoint)
    }

    protected fun detachCurrentEndpoint(): E? = synchronized(endpointLock) {
        endpoint.also { endpoint = null }
    }

    protected fun detachEndpoint(endpoint: E): Boolean = synchronized(endpointLock) {
        if (this.endpoint !== endpoint) false
        else {
            this.endpoint = null
            true
        }
    }

    protected fun discardEndpoint(endpoint: E) {
        detachEndpoint(endpoint)
        closeEndpoint(endpoint)
    }

    private fun encode(packet: OutputPacket): ByteArray {
        val frame = intArrayOf(OUTPUT_PACKET_SYMBOL) + packet.pack()
        val checksum = PacketReceiver.calculateChecksum(frame.copyOfRange(2, frame.size))
        val checksummed = frame + checksum[1] + checksum[0]
        return (PacketReceiver.escapeTx(checksummed) + END_PACKET_SYMBOL)
            .map { it.toByte() }
            .toByteArray()
    }

    companion object {
        internal const val MAX_PACKET_SIZE = PacketReceiver.MAX_PACKET_SIZE
    }

}
