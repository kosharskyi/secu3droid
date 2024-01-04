/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.BaseOutputPacket
import org.secu3.android.models.packets.BaseSecu3Packet
import org.secu3.android.models.packets.BaseSecu3Packet.Companion.END_PACKET_SYMBOL
import org.secu3.android.models.packets.BaseSecu3Packet.Companion.MAX_PACKET_SIZE
import org.secu3.android.models.packets.ChangeModePacket
import org.secu3.android.utils.LifeTimePrefs
import org.secu3.android.utils.PacketUtils
import org.secu3.android.utils.Task
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Secu3Manager @Inject constructor(
    private val prefs: LifeTimePrefs,
    private val bluetoothManager: BluetoothManager
    ) {

    private val bluetoothAdapter: BluetoothAdapter
        get() = bluetoothManager.adapter

    enum class SECU3_PACKET_SEARCH {
        SEARCH_START, SEARCH_END
    }

    private var mSecu3PacketSearch = SECU3_PACKET_SEARCH.SEARCH_START


    var connectedThread: ConnectedThread? = null
    private var createConnectThread: CreateConnectThread? = null

    private val mReceivedPacketFlow = MutableSharedFlow<RawPacket>()
    val receivedPacketFlow: Flow<RawPacket>
        get() = mReceivedPacketFlow


    fun start() {
        prefs.bluetoothDeviceName?.let {

            connectedThread?.mmSocket?.let { socket ->
                if (socket.isConnected) {
                    return
                }
            }

            if (bluetoothAdapter.isEnabled) {
                createConnectThread = CreateConnectThread(bluetoothAdapter, it)
                createConnectThread?.start()
            }
        }
    }

    @Synchronized
    fun addPacket(packet: BaseOutputPacket) {
        connectedThread?.sendPacket?.apply {
            if (isEmpty()) {
                add(packet)
                return@apply
            }

            try {
                if (last().javaClass != packet.javaClass) {
                    add(packet)
                }
            } catch (e: NoSuchElementException) {
                add(packet)
            }
        }
    }


    /* ============================ Thread to Create Bluetooth Connection =================================== */
    inner class CreateConnectThread(bluetoothAdapter: BluetoothAdapter, address: String) : Thread() {

        var mmSocket: BluetoothSocket? = null
        private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        init {
            val bluetoothDevice: BluetoothDevice = bluetoothAdapter.bondedDevices.first { it.name == address }
            var tmp: BluetoothSocket? = null

            try {
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e(this.javaClass.simpleName, "Socket's create() method failed", e)
            }
            mmSocket = tmp
        }




        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket?.connect()
                Log.e(this.javaClass.simpleName, "Device connected")

            } catch (connectException: IOException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket?.close()
                    Log.e(this.javaClass.simpleName, "Cannot connect to device")
                } catch (closeException: IOException) {
                    Log.e(this.javaClass.simpleName, "Could not close the client socket", closeException)
                }
                return
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = ConnectedThread(mmSocket!!)
            connectedThread?.run()
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(this.javaClass.simpleName, "Could not close the client socket", e)
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    inner class ConnectedThread(val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        val sendPacket: Queue<BaseOutputPacket> = ConcurrentLinkedDeque()

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut

            sendPacket.add(ChangeModePacket.getPacket(Task.Secu3ReadFirmwareInfo))
        }

        override fun run() {

            var idx = 0

            val packetBuffer = IntArray(MAX_PACKET_SIZE)
            Log.e(this.javaClass.simpleName, "comunicating with device")

            val charset = StandardCharsets.ISO_8859_1
            val reader = BufferedReader(InputStreamReader(mmInStream, charset))
            val writer = BufferedWriter(OutputStreamWriter(mmOutStream, charset))

            loop@ while (mmSocket.isConnected) {
                try {

                    if (sendPacket.isNotEmpty()) {
                        sendPacket.poll()?.let {
                            var packet = it.pack()
                            val checksum = PacketUtils.calculateChecksum(packet.substring(2, packet.length))

                            packet += checksum[1].toInt().toChar()
                            packet += checksum[0].toInt().toChar()

                            Log.e(this.javaClass.simpleName, packet)
                            var escaped = PacketUtils.EscTxPacket(packet)
                            escaped += END_PACKET_SYMBOL

                            writer.append(escaped)

                            sleep(20)
                            writer.flush()
                        }
                    }


                    if (reader.ready()) {

                        reader.read().takeIf { it != -1}?.toChar()?.let {

                            if (mSecu3PacketSearch == SECU3_PACKET_SEARCH.SEARCH_START) {
                                if (it == BaseSecu3Packet.INPUT_PACKET_SYMBOL) {
                                    mSecu3PacketSearch = SECU3_PACKET_SEARCH.SEARCH_END
                                    idx = 0
                                }
                            }

                            if (it != '\r') {
                                packetBuffer[idx++] = it.code
                            }

                            if (idx >= MAX_PACKET_SIZE) {
                                mSecu3PacketSearch = SECU3_PACKET_SEARCH.SEARCH_START
                                idx = 0
                            }

                            if (mSecu3PacketSearch == SECU3_PACKET_SEARCH.SEARCH_END && it == '\r') {
                                mSecu3PacketSearch = SECU3_PACKET_SEARCH.SEARCH_START
                                val escaped = PacketUtils.EscRxPacket(packetBuffer.sliceArray(IntRange(0, idx-1)))
                                val line = String(escaped, 0, escaped.size)

                                val rawPacket = RawPacket(line)
                                runBlocking {
                                    mReceivedPacketFlow.emit(rawPacket)
                                }
                            }

                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    disable()
                    break
                }
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun disable() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }
            connectedThread = null
            createConnectThread = null
        }
    }

    fun disable() {
        connectedThread?.disable()
    }

}