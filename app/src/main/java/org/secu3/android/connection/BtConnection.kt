/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.connection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.secu3.android.models.RawPacket
import org.secu3.android.utils.UserPrefs
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BtConnection @Inject constructor(
    private val prefs: UserPrefs,
    bluetoothManager: BluetoothManager,
) : Connection<BluetoothSocket>() {
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    init { observePacketCalibration(prefs) }

    suspend fun startConnection(generation: Long) {
        val prepared = CompletableDeferred<Unit>()
        launchConnection(generation) { job ->
            try {
                val settings = prefs.settings.first()
                currentCoroutineContext().ensureActive()
                if (!isCurrent(job)) return@launchConnection
                val address = BluetoothDeviceAddress.normalize(settings.bluetoothDeviceAddress)
                if (address == null || runCatching { bluetoothAdapter?.isEnabled != true }.getOrDefault(true)) {
                    finishAttempt(job, Disconnected, ConnectionEvent.End.STOPPED)
                    prepared.complete(Unit)
                    return@launchConnection
                }
                prepared.complete(Unit)
                runConnection(job, address)
            } catch (failure: IOException) {
                currentCoroutineContext().ensureActive()
                if (isCurrent(job)) finishAttempt(job, ConnectionFailed(failure), ConnectionEvent.End.STOPPED)
                prepared.completeExceptionally(failure)
            } finally {
                if (!prepared.isCompleted) prepared.cancel()
            }
        }
        try { prepared.await() } catch (cancelled: CancellationException) {
            synchronized(connectionLock) { if (this.generation == generation) stopConnection() }
            throw cancelled
        }
    }

    private suspend fun runConnection(job: Job, address: String) {
        var attempts = 0
        while (isCurrent(job)) {
            val limit = try { prefs.settings.first().connectionRetries } catch (failure: IOException) {
                currentCoroutineContext().ensureActive()
                if (!isCurrent(job)) return
                publishState(job, ConnectionFailed(failure))
                delay(RETRY_DELAY_MS)
                continue
            }
            if (attempts >= limit) {
                publishState(job, Disconnected)
                finishAttempt(job, ConnectionTimeout, ConnectionEvent.End.STOPPED)
                return
            }
            val attempt = beginAttempt(job) ?: return
            publishState(job, InProgress)
            attempts++
            if (attempts > 1) publishState(job, ConnectionAttempt(attempts))
            val socket = try { connectToDevice(job, address) } catch (failure: IOException) {
                currentCoroutineContext().ensureActive()
                if (!isCurrent(job)) return
                finishAttempt(job, ConnectionFailed(failure))
                delay(RETRY_DELAY_MS)
                continue
            }
            if (!isCurrent(job)) return
            requestFirmware()
            markConnected(job, attempt)
            try {
                readSocket(job, socket, attempt)
                if (isCurrent(job)) throw IOException("Bluetooth socket closed")
            } catch (failure: IOException) {
                currentCoroutineContext().ensureActive()
                if (!isCurrent(job)) return
                finishAttempt(job, InProgress)
                attempts = 0
            }
        }
    }

    private fun connectToDevice(job: Job, address: String): BluetoothSocket {
        var socket: BluetoothSocket? = null
        try {
            if (!isCurrent(job)) throw IOException("Bluetooth connection was stopped")
            val device = bluetoothAdapter?.bondedDevices?.let {
                BluetoothDeviceAddress.find(address, it) { device -> device.address }
            }
                ?: throw IOException("Paired device address is unavailable")

            val newSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            socket = newSocket
            val installed = synchronized(connectionLock) {
                connectionJob === job && isRunning && installEndpoint(newSocket)
            }
            if (!installed) throw IOException("Bluetooth connection is no longer current")
            bluetoothAdapter.cancelDiscovery()
            newSocket.connect()
            if (!isCurrentEndpoint(newSocket) || !isCurrent(job)) throw IOException("Bluetooth connection was stopped while connecting")
            return newSocket
        } catch (e: SecurityException) {
            socket?.let(::discardEndpoint)
            throw IOException("Missing Bluetooth permission", e)
        } catch (e: IOException) {
            socket?.let(::discardEndpoint)
            throw e
        }
    }

    private suspend fun readSocket(job: Job, socket: BluetoothSocket, attempt: Long) {
        if (!isCurrentEndpoint(socket) || !isCurrent(job)) return
        val receiver = PacketReceiver()
        val reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.ISO_8859_1))
        while (isCurrentEndpoint(socket) && isCurrent(job)) {
            val packet = receiver.readNext(reader)
            currentCoroutineContext().ensureActive()
            if (!isCurrentEndpoint(socket) || !isCurrent(job)) return
            receivePacket(attempt, RawPacket(packet))
        }
    }

    override fun isEndpointOpen(endpoint: BluetoothSocket): Boolean = endpoint.isConnected

    override fun writeEndpoint(endpoint: BluetoothSocket, bytes: ByteArray) {
        endpoint.outputStream.write(bytes)
    }

    override fun closeEndpoint(endpoint: BluetoothSocket) {
        try {
            endpoint.close()
        } catch (failure: IOException) {
            Log.e(javaClass.simpleName, "Error while closing failed Bluetooth socket: ${failure.message}")
        }
    }

    override suspend fun afterEndpointWrite() {
        delay(20)
    }

    private companion object { const val RETRY_DELAY_MS = 2_000L }
}
