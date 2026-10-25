package org.secu3.android.ui.parameters

import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.R
import org.secu3.android.connection.Connection
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.connection.UsbConnection
import org.secu3.android.connection.UsbAttemptPacket
import org.secu3.android.connection.UsbSessionFence
import org.secu3.android.models.RawPacket
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.UserPrefs
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.parameters.pages.compose.AnglesUiState
import org.secu3.android.ui.parameters.pages.compose.AnglesNumber
import org.secu3.android.ui.parameters.pages.compose.StarterUiState
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ParameterPageSessionCacheTest {
    @get:Rule val compose = androidx.compose.ui.test.junit4.v2.createAndroidComposeRule<MainActivity>()

    @Test fun pageReturnUsesSessionCacheAndWritesRefreshOtherPages() = runBlocking {
        val vm = paramsViewModel()
        val transport = installFakeUsb(vm)
        try {
            activate(vm, R.string.params_tab_start_title)
            transport.awaitReadRequest('o', 1)
            assertTrue(vm.starterStateHolder.state is StarterUiState.Loading)
            transport.emit(StarterParamPacket(starterOff = 120))
            await { vm.starterStateHolder.state is StarterUiState.Data }
            delay(150)

            activate(vm, R.string.params_tab_angles_title)
            transport.awaitReadRequest('m', 1)
            assertTrue(vm.anglesStateHolder.state is AnglesUiState.Loading)
            transport.emit(AnglesParamPacket(maxAngle = 4f))
            await { vm.anglesStateHolder.state is AnglesUiState.Data }
            delay(150)

            activate(vm, R.string.params_tab_start_title)
            delay(300)
            assertEquals(1, transport.readRequestCount('o'))
            assertEquals(1, transport.readRequestCount('m'))
            assertTrue(vm.starterStateHolder.state is StarterUiState.Data)

            compose.runOnIdle {
                vm.starterStateHolder.updateValue(StarterNumber.STARTER_OFF, 140f)
            }

            activate(vm, R.string.params_tab_angles_title)
            transport.awaitReadRequest('m', 2)
            assertTrue(vm.anglesStateHolder.state is AnglesUiState.Loading)
            transport.emit(AnglesParamPacket(maxAngle = 6f))
            await { vm.anglesStateHolder.state is AnglesUiState.Data }
            delay(150)

            compose.runOnIdle { vm.refreshPage(ParameterPage.ANGLES) }
            transport.awaitReadRequest('m', 3)
            assertTrue(vm.anglesStateHolder.state is AnglesUiState.Loading)
            transport.emit(AnglesParamPacket(maxAngle = 7f))
            await { vm.anglesStateHolder.state is AnglesUiState.Data }
            delay(150)

            compose.runOnIdle {
                vm.clearPageState()
                vm.activatePage(ParameterPage.ANGLES)
            }
            transport.awaitReadRequest('m', 4)
            assertTrue(vm.anglesStateHolder.state is AnglesUiState.Loading)
            transport.emit(AnglesParamPacket(maxAngle = 8f))
            await { vm.anglesStateHolder.state is AnglesUiState.Data }
        } finally {
            transport.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    @Test fun pageWaitingBehindAnotherTabUsesFreshRevisionAfterWrite() = runBlocking {
        val vm = paramsViewModel()
        val transport = installFakeUsb(vm)
        try {
            activate(vm, R.string.params_tab_start_title)
            transport.awaitReadRequest('o', 1)
            activate(vm, R.string.params_tab_angles_title)
            transport.awaitReadRequest('m', 1)
            transport.emit(AnglesParamPacket(maxAngle = 4f))
            await { vm.anglesStateHolder.state is AnglesUiState.Data }
            delay(150)

            compose.runOnIdle {
                vm.anglesStateHolder.updateValue(AnglesNumber.MAX, 6f)
            }

            activate(vm, R.string.params_tab_start_title)
            transport.awaitReadRequest('o', 2)
            assertTrue(vm.starterStateHolder.state is StarterUiState.Loading)
            transport.emit(StarterParamPacket(starterOff = 120))
            await { vm.starterStateHolder.state is StarterUiState.Data }
            delay(150)

            val afterPendingRead = transport.readRequestCount('o')
            activate(vm, R.string.params_tab_angles_title)
            activate(vm, R.string.params_tab_start_title)
            delay(300)
            assertEquals(afterPendingRead, transport.readRequestCount('o'))
            assertEquals(1, transport.readRequestCount('m'))
            assertTrue(vm.starterStateHolder.state is StarterUiState.Data)
        } finally {
            transport.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    @Test fun sendDataWritesCallTimeSnapshotInArrivalOrder() = runBlocking {
        val vm = paramsViewModel()
        val transport = installFakeUsb(vm)
        try {
            val barrier = transport.blockNextWrite()
            transport.send(ChangeModePacket('w'))
            assertTrue(barrier.entered.await(3, TimeUnit.SECONDS))

            val payload = intArrayOf('x'.code, 1)
            val packet = object : OutputPacket {
                override fun pack(): IntArray = payload
            }
            transport.send(packet)
            payload[1] = 2
            transport.send(packet)
            transport.send(ChangeModePacket('s'))
            assertEquals(0, transport.writes.size)
            barrier.release.countDown()
            transport.awaitWriteCount(4)

            assertArrayEquals(byteArrayOf(0x21, 0x68, 0x77, 0, 0x67, 0x79, 0x0D), transport.writes[0])
            assertArrayEquals(byteArrayOf(0x21, 0x78, 1, 3, 2, 0x0D), transport.writes[1])
            assertArrayEquals(byteArrayOf(0x21, 0x78, 2, 5, 3, 0x0D), transport.writes[2])
            assertArrayEquals(byteArrayOf(0x21, 0x68, 0x73, 0, 0x5B, 0x75, 0x0D), transport.writes[3])
        } finally {
            transport.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    private fun paramsViewModel(): ParamsViewModel {
        lateinit var viewModel: ParamsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[ParamsViewModel::class.java]
        }
        return viewModel
    }

    private fun activate(vm: ParamsViewModel, titleRes: Int) {
        compose.runOnIdle { vm.activatePage(parameterPageForTitle(titleRes)!!) }
    }

    private suspend fun await(predicate: () -> Boolean) {
        withTimeout(3_000) {
            while (!predicate()) delay(10)
        }
    }

    private suspend fun installFakeUsb(vm: ParamsViewModel): FakeUsbTransport {
        val managerField = ParamsViewModel::class.java.getDeclaredField("secu3ConnectionManager").apply { isAccessible = true }
        val manager = managerField.get(vm) as Secu3ConnectionManager
        val prefs = UserPrefs(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
        val originalWakeLockPreference = prefs.isWakeLockEnabled
        prefs.isWakeLockEnabled = false
        try {
            val usbField = Secu3ConnectionManager::class.java.declaredFields.first { UsbConnection::class.java.isAssignableFrom(it.type) }
                .apply { isAccessible = true }
            val connection = usbField.get(manager) as UsbConnection
            val portField = Connection::class.java.getDeclaredField("endpoint").apply { isAccessible = true }
            val requests = ConcurrentHashMap<Char, AtomicInteger>()
            val writes = CopyOnWriteArrayList<ByteArray>()
            val nextWriteBarrier = AtomicReference<WriteBarrier?>(null)
            val fakePort = Proxy.newProxyInstance(UsbSerialPort::class.java.classLoader, arrayOf(UsbSerialPort::class.java)) { _, method, args ->
                when (method.name) {
                    "isOpen" -> true
                    "write" -> {
                        val bytes = args?.get(0) as? ByteArray
                        nextWriteBarrier.getAndSet(null)?.let { barrier ->
                            barrier.entered.countDown()
                            if (!barrier.release.await(3, TimeUnit.SECONDS)) throw AssertionError("write barrier timed out")
                        }
                        if (bytes != null) writes += bytes.copyOf()
                        if (bytes != null && bytes.size >= 3 && bytes[0].toInt() == '!'.code && bytes[1].toInt() == 'h'.code) {
                            requests.computeIfAbsent(bytes[2].toInt().toChar()) { AtomicInteger() }.incrementAndGet()
                        }
                        null
                    }
                    "read" -> 0
                    "toString" -> "test-usb-port"
                    "hashCode" -> System.identityHashCode(this)
                    "equals" -> false
                    else -> null
                }
            } as UsbSerialPort
            portField.set(connection, fakePort)
            Connection::class.java.getDeclaredField("isRunning").apply { isAccessible = true }.setBoolean(connection, true)
            val attemptPacketsField = UsbConnection::class.java.getDeclaredField("_attemptPackets").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val attemptPackets = attemptPacketsField.get(connection) as MutableSharedFlow<UsbAttemptPacket>
            val fenceField = Secu3ConnectionManager::class.java.getDeclaredField("usbSessionFence").apply { isAccessible = true }
            val fence = fenceField.get(manager) as UsbSessionFence
            val generation = fence.begin(1)
            Secu3ConnectionManager::class.java.getDeclaredField("activeUsbAttachment").apply { isAccessible = true }.set(manager, null)
            val selectionClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Selection" }
            val transportClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Transport" }
            val usbTransport = transportClass.enumConstants!!.first { (it as Enum<*>).name == "USB" }
            val selection = selectionClass.declaredConstructors.first().apply { isAccessible = true }.newInstance(usbTransport, generation)
            val subscribed = CompletableDeferred<Unit>()
            @Suppress("UNCHECKED_CAST")
            val barriers = Secu3ConnectionManager::class.java.getDeclaredField("usbSelectionReady").apply { isAccessible = true }
                .get(manager) as MutableMap<Long, CompletableDeferred<Unit>>
            synchronized(barriers) { barriers[generation] = subscribed }
            @Suppress("UNCHECKED_CAST")
            val selected = Secu3ConnectionManager::class.java.getDeclaredField("selectedTransport").apply { isAccessible = true }
                .get(manager) as MutableStateFlow<Any>
            selected.value = selection
            Secu3ConnectionManager::class.java.getDeclaredField("activeUsbAttemptId").apply { isAccessible = true }.set(manager, 1L)
            withTimeout(3_000) { subscribed.await() }
            synchronized(barriers) { barriers.remove(generation) }
            attemptPackets.emit(UsbAttemptPacket(1L, firmwarePacket()))
            withTimeout(3_000) { while (!manager.isUsbConnected || !manager.usbSessionLocked.value) delay(10) }
            return FakeUsbTransport(manager, connection, portField, attemptPackets, requests, writes, nextWriteBarrier, prefs, originalWakeLockPreference)
        } catch (failure: Throwable) {
            try {
                manager.onUsbDeviceDetached(1)
            } finally {
                prefs.isWakeLockEnabled = originalWakeLockPreference
            }
            throw failure
        }
    }

    private class FakeUsbTransport(
        private val manager: Secu3ConnectionManager,
        private val connection: UsbConnection,
        private val portField: java.lang.reflect.Field,
        private val attemptPackets: MutableSharedFlow<UsbAttemptPacket>,
        private val requests: ConcurrentHashMap<Char, AtomicInteger>,
        val writes: CopyOnWriteArrayList<ByteArray>,
        private val nextWriteBarrier: AtomicReference<WriteBarrier?>,
        private val prefs: UserPrefs,
        private val originalWakeLockPreference: Boolean,
    ) {
        fun send(packet: OutputPacket) = connection.sendData(packet)

        fun blockNextWrite(): WriteBarrier = WriteBarrier().also { nextWriteBarrier.set(it) }

        fun readRequestCount(descriptor: Char): Int = requests[descriptor]?.get() ?: 0

        suspend fun awaitWriteCount(expected: Int) {
            withTimeout(3_000) {
                while (writes.size < expected) delay(10)
            }
        }

        suspend fun awaitReadRequest(descriptor: Char, expected: Int) {
            withTimeout(3_000) {
                while (readRequestCount(descriptor) < expected) delay(10)
            }
        }

        suspend fun emit(packet: OutputPacket) {
            val input = intArrayOf('@'.code) + packet.pack() + intArrayOf(0, 0)
            attemptPackets.emit(UsbAttemptPacket(1L, RawPacket(input)))
        }

        fun close() {
            try {
                manager.onUsbDeviceDetached(1)
            } finally {
                try {
                    portField.set(connection, null)
                    Connection::class.java.getDeclaredField("isRunning").apply { isAccessible = true }.setBoolean(connection, false)
                } finally {
                    prefs.isWakeLockEnabled = originalWakeLockPreference
                }
            }
        }
    }

    private class WriteBarrier {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
    }

    private fun firmwarePacket() = RawPacket(
        intArrayOf('@'.code, 'y'.code) + "SECU-3 test firmware".padEnd(52).map(Char::code).toIntArray() +
            intArrayOf(0, 0, 0, 0, 0x10, 0, 0),
    )
}
