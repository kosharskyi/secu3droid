package org.secu3.android.connection

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.usb.UsbManager
import androidx.test.platform.app.InstrumentationRegistry
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.RawPacket
import org.secu3.android.utils.FileHelper
import org.secu3.android.utils.SecuLogger
import org.secu3.android.utils.Task
import org.secu3.android.utils.UserPrefs

/** Exercises the production coordinator with an isolated serial port, never a physical ECU. */
class UsbSessionIntegrationTest {
    @Test fun firmwareHandshakeGatesWritesAndOnlyMatchingDetachEndsTheLockedSession() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(71)
            fixture.manager.sendNewTask(Task.Secu3ReadSensors)
            delay(100)
            assertFalse(fixture.manager.isConnected)
            assertTrue(fixture.writes.isEmpty())

            fixture.confirmFirmware()
            assertNotNull(fixture.manager.fwInfo)
            assertFalse(fixture.manager.stopConnection())
            fixture.manager.sendNewTask(Task.Secu3ReadSensors)
            fixture.await { fixture.writes.size == 1 }

            assertFalse(fixture.manager.onUsbDeviceDetached(72))
            assertTrue(fixture.manager.isUsbConnected)
            assertTrue(fixture.manager.usbSessionLocked.value)

            assertTrue(fixture.manager.onUsbDeviceDetached(71))
            assertFalse(fixture.manager.isConnected)
            assertFalse(fixture.manager.usbSessionLocked.value)
            assertNull(fixture.manager.fwInfo)
            assertEquals(Disconnected, fixture.manager.currentConnectionState)
            fixture.manager.sendNewTask(Task.Secu3ReadSensors)
            delay(100)
            assertEquals(1, fixture.writes.size)
        } finally {
            fixture.close()
        }
    }

    @Test fun delayedBluetoothStatesCannotBreakConfirmedUsb() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(81)
            fixture.confirmFirmware()
            fixture.btStates.emit(Disconnected)
            fixture.btStates.emit(ConnectionFailed(IOException("old Bluetooth failure")))
            delay(100)

            assertEquals(Connected, fixture.manager.currentConnectionState)
            assertTrue(fixture.manager.usbSessionLocked.value)
            assertTrue(fixture.manager.isUsbConnected)
        } finally {
            fixture.close()
        }
    }

    @Test fun terminalUsbFailureRejectsLateFirmwareAndFurtherCommands() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(91)
            fixture.confirmFirmware()
            fixture.usbStates.emit(ConnectionFailed(IOException("USB read failed")))
            fixture.await { fixture.manager.currentConnectionState is ConnectionFailed }
            fixture.rawPackets.emit(firmwarePacket())
            fixture.manager.sendNewTask(Task.Secu3ReadSensors)
            delay(100)

            assertFalse(fixture.manager.usbSessionLocked.value)
            assertFalse(fixture.manager.isConnected)
            assertNull(fixture.manager.fwInfo)
            assertTrue(fixture.writes.isEmpty())
            assertTrue(fixture.manager.currentConnectionState is ConnectionFailed)
        } finally {
            fixture.close()
        }
    }

    @Test fun reattachedDeviceWithTheSameIdNeedsFreshFirmware() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.attach(101)
            fixture.confirmFirmware()
            assertTrue(fixture.manager.onUsbDeviceDetached(101))
            fixture.attach(101)
            assertFalse(fixture.manager.isConnected)
            assertNull(fixture.manager.fwInfo)

            fixture.confirmFirmware()
            assertTrue(fixture.manager.isUsbConnected)
            assertFalse(fixture.manager.stopConnection())
        } finally {
            fixture.close()
        }
    }

    private class Fixture {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val prefs = UserPrefs(context)
        private val usb = UsbConnection(context.getSystemService(Context.USB_SERVICE) as UsbManager, prefs)
        private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        private val bt = BtConnection(prefs, bluetoothManager)
        private val logger = SecuLogger(prefs, FileHelper(context))
        val manager = Secu3ConnectionManager(usb, bt, prefs, bluetoothManager, logger)
        val writes = CopyOnWriteArrayList<ByteArray>()
        val rawPackets: MutableSharedFlow<RawPacket> = connectionField("mReceivedPacketFlow").get(usb) as MutableSharedFlow<RawPacket>
        val usbStates: MutableSharedFlow<ConnectionState> = connectionField("mConnectionStateFlow").get(usb) as MutableSharedFlow<ConnectionState>
        val btStates: MutableSharedFlow<ConnectionState> = connectionField("mConnectionStateFlow").get(bt) as MutableSharedFlow<ConnectionState>

        // The endpoint injection matches existing transport integration fixtures. It bypasses only
        // Android's physical USB opening; packet parsing, session identity and send gates are real.
        suspend fun attach(deviceId: Int) {
            val open = AtomicBoolean(true)
            val port = Proxy.newProxyInstance(UsbSerialPort::class.java.classLoader, arrayOf(UsbSerialPort::class.java)) { proxy, method, args ->
                when (method.name) {
                    "isOpen" -> open.get()
                    "close" -> { open.set(false); null }
                    "write" -> { writes += (args!![0] as ByteArray).copyOf(); null }
                    "toString" -> "isolated-usb-session-test"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.get(0)
                    else -> null
                }
            } as UsbSerialPort
            connectionField("endpoint").set(usb, port)
            connectionField("isRunning").setBoolean(usb, true)
            val fence = managerField("usbSessionFence").get(manager) as UsbSessionFence
            val generation = fence.begin(deviceId)
            val selectionClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Selection" }
            val transportClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Transport" }
            val usbTransport = transportClass.enumConstants!!.first { (it as Enum<*>).name == "USB" }
            val selection = selectionClass.declaredConstructors.first().apply { isAccessible = true }.newInstance(usbTransport, generation)
            val subscribed = CompletableDeferred<Unit>()
            @Suppress("UNCHECKED_CAST")
            val barriers = managerField("usbSelectionReady").get(manager) as MutableMap<Long, CompletableDeferred<Unit>>
            synchronized(barriers) { barriers[generation] = subscribed }
            @Suppress("UNCHECKED_CAST")
            val selected = managerField("selectedTransport").get(manager) as MutableStateFlow<Any>
            selected.value = selection
            withTimeout(3_000) { subscribed.await() }
            synchronized(barriers) { barriers.remove(generation) }
            await { usbStates.subscriptionCount.value > 0 && btStates.subscriptionCount.value > 0 }
        }

        suspend fun confirmFirmware() {
            rawPackets.emit(firmwarePacket())
            await { manager.isUsbConnected && manager.usbSessionLocked.value }
        }

        suspend fun await(predicate: () -> Boolean) {
            withTimeout(3_000) { while (!predicate()) delay(10) }
        }

        fun close() {
            manager.activeUsbDeviceIdOrNull?.let(manager::onUsbDeviceDetached)
            (managerField("scope").get(manager) as CoroutineScope).cancel()
            usb.cancelScopes()
            bt.cancelScopes()
            (SecuLogger::class.java.getDeclaredField("loggerScope").apply { isAccessible = true }.get(logger) as CoroutineScope).cancel()
        }

        private fun managerField(name: String) = Secu3ConnectionManager::class.java.getDeclaredField(name).apply { isAccessible = true }
        private fun connectionField(name: String) = Connection::class.java.getDeclaredField(name).apply { isAccessible = true }
    }

    companion object {
        private fun firmwarePacket() = RawPacket(
            intArrayOf('@'.code, 'y'.code) + "SECU-3 test firmware".padEnd(52).map { it.code }.toIntArray() +
                intArrayOf(0, 0, 0, 0, 0x10, 0, 0),
        )
    }
}
