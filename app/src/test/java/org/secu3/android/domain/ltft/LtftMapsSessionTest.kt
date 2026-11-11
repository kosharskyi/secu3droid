package org.secu3.android.domain.ltft

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.domain.ecu.EcuExchange
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.LoadGridPacket
import org.secu3.android.models.packets.input.LtftMapChunkPacket
import org.secu3.android.models.packets.input.RpmGridPacket
import org.secu3.android.models.packets.input.SensorsPacket
import org.secu3.android.models.packets.out.OpCompNc
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.utils.Task

class LtftMapsSessionTest {

    @Test
    fun publishesBothMapsOnlyAfterAllAddressesAreReceived() = testSession {
        connect()
        beginReadingMaps(FunSetParamPacket(loadSrcCfg = 1, funcFlags = 1), loadGrid)

        val chunks = mapChunks()
        val finalChunk = chunks.last()
        val partial = chunks.dropLast(1).reversed()
        emit(LtftMapChunkPacket(tableIndex = 1, address = 255))
        partial.forEach { emit(it) }
        emit(partial.first())
        assertEquals(LtftMapsState.Reading, session.state.value)

        emit(finalChunk)
        expect(Task.Secu3ReadSensors)
        val maps = awaitReady()
        assertEquals(rpmGrid, maps.rpmAxis)
        assertEquals(loadGrid, maps.loadAxis)
        assertEquals(2, maps.tables.size)
        assertEquals(List(256) { it / 10f }, maps.tables[0])
        assertEquals(List(256) { -it / 10f }, maps.tables[1])
        emit(SensorsPacket(rpm = 750, load = 35f, baroPress = 125f))
        assertEquals(loadGrid, awaitReady { it.workingPoint != null }.loadAxis)
    }

    @Test
    fun buildsStaticLoadAxisFromFunctionLimitsWithoutRequestingLoadGrid() = testSession {
        connect()
        beginReadingMaps(FunSetParamPacket(loadSrcCfg = 2, loadLower = 10f, loadUpper = 85f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)

        val maps = awaitReady()
        assertFalse(maps.isLoadPressure)
        assertEquals(List(16) { 10f + 5f * it }, maps.loadAxis)
        assertNull(maps.workingPoint)
        emit(SensorsPacket(rpm = 400, load = 120f))
        assertEquals(LtftWorkingPoint(400, 120f, 0f, 15f), awaitReady { it.workingPoint != null }.workingPoint)
    }

    @Test
    fun rebuildsBarometricAxisAndHidesOperatingPointDuringCranking() = testSession {
        connect()
        beginReadingMaps(FunSetParamPacket(loadSrcCfg = 1, loadLower = 20f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        assertNull(awaitReady().loadAxis)

        emit(SensorsPacket(rpm = 750, load = 27.5f, baroPress = 95f))
        val first = awaitReady { it.workingPoint != null }
        assertEquals(List(16) { 20f + 5f * it }, first.loadAxis)
        assertEquals(LtftWorkingPoint(750, 27.5f, 0.5f, 1.5f), first.workingPoint)

        emit(SensorsPacket(rpm = 750, load = 27.5f, baroPress = 170f))
        val changed = awaitReady { it.loadAxis?.last() == 170f }
        assertEquals(LtftWorkingPoint(750, 27.5f, 0.5f, 0.75f), changed.workingPoint)

        emit(SensorsPacket(rpm = 250, load = 30f, baroPress = 170f).apply { strtUse = true })
        awaitReady { it.workingPoint == null && it.loadAxis != null }
    }

    @Test
    fun incompleteReadTimesOutRestoresSensorsAndStartsWithFreshFragmentsOnRetry() = testSession(timeoutMillis = 500) {
        connect()
        val functions = FunSetParamPacket(loadLower = 20f, loadUpper = 80f)
        beginReadingMaps(functions)
        val chunks = mapChunks()
        emit(chunks.first())
        coroutineScope {
            val duplicates = launch {
                while (true) {
                    delay(50)
                    emit(chunks.first())
                }
            }
            try {
                expect(Task.Secu3ReadSensors)
            } finally {
                duplicates.cancelAndJoin()
            }
        }
        awaitState(LtftMapsState.TimedOut)

        session.refresh()
        beginReadingMaps(functions)
        chunks.drop(1).forEach { emit(it) }
        assertEquals(LtftMapsState.Reading, session.state.value)
        emit(chunks.first().copy(values = List(16) { 2f }))
        expect(Task.Secu3ReadSensors)
        assertEquals(List(16) { 2f }, awaitReady().tables.first().take(16))
    }

    @Test
    fun uniqueProgressKeepsReadingAfterTheOnePacketDeadline() = testSession(timeoutMillis = 500) {
        connect()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        val chunks = mapChunks()
        for (chunk in chunks.take(4)) {
            emit(chunk)
            delay(180)
        }
        chunks.drop(4).forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        assertEquals(2, awaitReady().tables.size)
    }

    @Test
    fun readsMissingFirmwareInformationAndRejectsFirmwareWithoutInjection() = testSession {
        firmware = null
        connect()
        expect(Task.Secu3ReadFirmwareInfo)
        emit(FirmwareInfoPacket())
        expect(Task.Secu3ReadSensors)
        awaitState(LtftMapsState.Unsupported)
        session.refresh()
        assertNoCommands()
    }

    @Test
    fun saveWaitsForMatchingAcknowledgementAndKeepsMapReady() = testSession {
        connect()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()

        session.saveMaps()
        expect(Task.Secu3OpComSaveLtft)
        session.saveMaps()
        session.refresh()
        assertNoCommands()
        emit(OpCompNc(opData = 0, opCode = 2))
        assertEquals(LtftWriteState.AwaitingAcknowledgement(LtftWriteOperation.Save), session.writeState.value)
        emit(OpCompNc(opData = 0, opCode = 9))

        awaitWriteState(LtftWriteState.Succeeded(LtftWriteOperation.Save))
        assertEquals(2, (session.state.value as LtftMapsState.Ready).tables.size)
        assertNoCommands()
    }

    @Test
    fun resetWaitsForAcknowledgementThenRereadsBothMaps() = testSession {
        connect()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()

        session.resetMaps()
        expect(Task.Secu3OpComResetLtft)
        emit(OpCompNc(opData = 0xBB, opCode = 0xCA))
        assertEquals(LtftWriteState.AwaitingAcknowledgement(LtftWriteOperation.Reset), session.writeState.value)
        emit(OpCompNc(opData = 0, opCode = 0xCA))
        assertEquals(LtftWriteState.ReadingAfterReset, session.writeState.value)

        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        val resetMaps = mapChunks().map { it.copy(values = List(it.values.size) { 0f }) }
        resetMaps.forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        val maps = awaitReady()
        assertEquals(List(256) { 0f }, maps.tables[0])
        assertEquals(List(256) { 0f }, maps.tables[1])
        awaitWriteState(LtftWriteState.Succeeded(LtftWriteOperation.Reset))
    }

    @Test
    fun resetAcknowledgementWithReadbackTimeoutCanBeRefreshed() = testSession(timeoutMillis = 500) {
        connect()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()

        session.resetMaps()
        expect(Task.Secu3OpComResetLtft)
        emit(OpCompNc(opData = 0, opCode = 0xCA))
        expect(Task.Secu3ReadRpmGrid)
        awaitWriteState(LtftWriteState.ResetReadbackFailed)
        awaitState(LtftMapsState.TimedOut)
        expect(Task.Secu3ReadSensors)

        session.refresh()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()
        awaitWriteState(LtftWriteState.Succeeded(LtftWriteOperation.Reset))
    }

    @Test
    fun writeTimeoutIsUnconfirmedAndCanBeRetriedByExplicitAction() = testSession(timeoutMillis = 500) {
        connect()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()

        session.saveMaps()
        expect(Task.Secu3OpComSaveLtft)
        awaitWriteState(LtftWriteState.Unconfirmed(LtftWriteOperation.Save))
        assertEquals(2, (session.state.value as LtftMapsState.Ready).tables.size)

        session.saveMaps()
        expect(Task.Secu3OpComSaveLtft)
        emit(OpCompNc(opData = 0, opCode = 9))
        awaitWriteState(LtftWriteState.Succeeded(LtftWriteOperation.Save))
    }

    @Test
    fun resetAcknowledgementTimeoutIgnoresLaterSensorPacketsAndAllowsRefresh() = testSession(timeoutMillis = 500) {
        connect()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()

        session.resetMaps()
        expect(Task.Secu3OpComResetLtft)
        awaitWriteState(LtftWriteState.Unconfirmed(LtftWriteOperation.Reset))
        assertEquals(LtftMapsState.TimedOut, session.state.value)
        emit(SensorsPacket(rpm = 900, load = 40f))
        assertEquals(LtftMapsState.TimedOut, session.state.value)

        session.refresh()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()
    }

    @Test
    fun disconnectClearsWriteResultAndLateAcknowledgementIsIgnoredAfterReconnect() = testSession {
        connect()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()

        session.saveMaps()
        expect(Task.Secu3OpComSaveLtft)
        setConnection(Disconnected)
        awaitState(LtftMapsState.Disconnected)
        awaitWriteState(LtftWriteState.Idle)

        setConnection(Connected)
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        mapChunks().forEach { emit(it) }
        expect(Task.Secu3ReadSensors)
        awaitReady()
        emit(OpCompNc(opData = 0, opCode = 9))
        assertEquals(LtftWriteState.Idle, session.writeState.value)
    }

    @Test
    fun pauseRestoresSensorsAndReconnectDropsPartialDataWithoutSendingIntoNewConnection() = testSession {
        connect()
        expect(Task.Secu3ReadRpmGrid)
        session.onRoutePaused()
        expect(Task.Secu3ReadSensors)
        awaitState(LtftMapsState.Disconnected)
        awaitSubscriptions(0)
        emit(RpmGridPacket(values = List(16) { 1 }))

        session.onRouteResumed()
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        val chunks = mapChunks()
        emit(chunks.first())
        setConnection(Disconnected)
        awaitState(LtftMapsState.Disconnected)
        awaitSubscriptions(0)
        assertNoCommands()

        setConnection(Connected)
        beginReadingMaps(FunSetParamPacket(loadLower = 20f, loadUpper = 80f))
        chunks.drop(1).forEach { emit(it) }
        assertEquals(LtftMapsState.Reading, session.state.value)
        emit(chunks.first().copy(values = List(16) { 3f }))
        expect(Task.Secu3ReadSensors)
        assertEquals(List(16) { 3f }, awaitReady().tables.first().take(16))
    }

    private fun testSession(timeoutMillis: Long = 2_000, test: suspend Fixture.() -> Unit) = runBlocking {
        val fixture = Fixture(this, timeoutMillis)
        try {
            fixture.test()
        } finally {
            fixture.session.close()
        }
    }

    private class Fixture(scope: CoroutineScope, timeoutMillis: Long) {
        private val connections = MutableSharedFlow<ConnectionState>(replay = 1).also { it.tryEmit(Disconnected) }
        private val packets = MutableSharedFlow<InputPacket>()
        private val commands = Channel<Task>(Channel.UNLIMITED)
        private var transportConnected = false
        var firmware: FirmwareInfoPacket? = FirmwareInfoPacket(options = 1 shl 28)
        val rpmGrid = List(16) { 500 * (it + 1) }
        val loadGrid = List(16) { 30f + 4f * it }

        private fun packetFlow(onSubscribed: () -> Unit) = packets.onSubscription { onSubscribed() }

        val session = LtftMapsSession(
            connectionStates = connections,
            firmwareInfo = { firmware },
            isTransportConnected = { transportConnected },
            ecuExchange = EcuExchange(::packetFlow),
            packets = ::packetFlow,
            sendTask = { commands.trySend(it).getOrThrow() },
            timeoutMillis = timeoutMillis,
        ).also { it.start(scope) }

        suspend fun connect() {
            session.onRouteResumed()
            setConnection(Connected)
        }

        suspend fun setConnection(state: ConnectionState) {
            transportConnected = state == Connected
            connections.emit(state)
        }

        suspend fun beginReadingMaps(functions: FunSetParamPacket, grid: List<Float>? = null) {
            expect(Task.Secu3ReadRpmGrid)
            emit(RpmGridPacket(values = rpmGrid))
            expect(Task.Secu3ReadFunsetParam)
            emit(functions)
            if (grid != null) {
                expect(Task.Secu3ReadLoadGrid)
                emit(LoadGridPacket(values = grid))
            }
            expect(Task.Secu3ReadLtftMap)
        }

        fun mapChunks(): List<LtftMapChunkPacket> = List(32) { fragment ->
            val table = fragment % 2
            val address = fragment / 2 * 16
            val sign = if (table == 0) 1 else -1
            LtftMapChunkPacket(table, address, List(16) { offset -> (address + offset) * sign / 10f })
        }

        suspend fun emit(packet: InputPacket) = packets.emit(packet)

        suspend fun expect(task: Task) {
            assertEquals(task, withTimeout(3_000) { commands.receive() })
        }

        fun assertNoCommands() {
            assertNull(commands.tryReceive().getOrNull())
        }

        suspend fun awaitSubscriptions(count: Int) {
            withTimeout(3_000) { packets.subscriptionCount.first { it == count } }
        }

        suspend fun awaitState(state: LtftMapsState) {
            withTimeout(3_000) { session.state.first { it == state } }
        }

        suspend fun awaitWriteState(state: LtftWriteState) {
            withTimeout(3_000) { session.writeState.first { it == state } }
        }

        suspend fun awaitReady(predicate: (LtftMapsState.Ready) -> Boolean = { true }): LtftMapsState.Ready =
            withTimeout(3_000) {
                session.state.first { it is LtftMapsState.Ready && predicate(it) } as LtftMapsState.Ready
            }
    }
}
