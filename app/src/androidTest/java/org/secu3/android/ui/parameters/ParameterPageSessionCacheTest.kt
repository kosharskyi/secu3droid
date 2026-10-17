package org.secu3.android.ui.parameters

import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.R
import org.secu3.android.connection.Connection
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.connection.UsbConnection
import org.secu3.android.models.RawPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.parameters.pages.compose.AnglesUiState
import org.secu3.android.ui.parameters.pages.compose.AnglesNumber
import org.secu3.android.ui.parameters.pages.compose.StarterUiState
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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

            compose.runOnIdle { vm.refreshPage(R.string.params_tab_angles_title) }
            transport.awaitReadRequest('m', 3)
            assertTrue(vm.anglesStateHolder.state is AnglesUiState.Loading)
            transport.emit(AnglesParamPacket(maxAngle = 7f))
            await { vm.anglesStateHolder.state is AnglesUiState.Data }
            delay(150)

            compose.runOnIdle {
                vm.invalidatePageCache()
                vm.activatePage(R.string.params_tab_angles_title)
            }
            transport.awaitReadRequest('m', 4)
            assertTrue(vm.anglesStateHolder.state is AnglesUiState.Loading)
            transport.emit(AnglesParamPacket(maxAngle = 7.5f))
            await { vm.anglesStateHolder.state is AnglesUiState.Data }
            delay(150)

            compose.runOnIdle {
                vm.clearPageState()
                vm.activatePage(R.string.params_tab_angles_title)
            }
            transport.awaitReadRequest('m', 5)
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

    private fun paramsViewModel(): ParamsViewModel {
        lateinit var viewModel: ParamsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[ParamsViewModel::class.java]
        }
        return viewModel
    }

    private fun activate(vm: ParamsViewModel, titleRes: Int) {
        compose.runOnIdle { vm.activatePage(titleRes) }
    }

    private suspend fun await(predicate: () -> Boolean) {
        withTimeout(3_000) {
            while (!predicate()) delay(10)
        }
    }

    private suspend fun installFakeUsb(vm: ParamsViewModel): FakeUsbTransport {
        val managerField = ParamsViewModel::class.java.getDeclaredField("secu3ConnectionManager").apply { isAccessible = true }
        val manager = managerField.get(vm) as Secu3ConnectionManager
        val usbField = Secu3ConnectionManager::class.java.declaredFields.first { UsbConnection::class.java.isAssignableFrom(it.type) }
            .apply { isAccessible = true }
        val connection = usbField.get(manager) as UsbConnection
        val portField = UsbConnection::class.java.getDeclaredField("usbSerialPort").apply { isAccessible = true }
        val requests = ConcurrentHashMap<Char, AtomicInteger>()
        val fakePort = Proxy.newProxyInstance(UsbSerialPort::class.java.classLoader, arrayOf(UsbSerialPort::class.java)) { _, method, args ->
            when (method.name) {
                "isOpen" -> true
                "write" -> {
                    val bytes = args?.get(0) as? ByteArray
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
        val stateFlowField = Connection::class.java.getDeclaredField("mConnectionStateFlow").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateFlowField.get(connection) as MutableSharedFlow<ConnectionState>
        delay(100)
        stateFlow.emit(Connected)
        return FakeUsbTransport(connection, portField, requests)
    }

    private class FakeUsbTransport(
        private val connection: UsbConnection,
        private val portField: java.lang.reflect.Field,
        private val requests: ConcurrentHashMap<Char, AtomicInteger>,
    ) {
        fun readRequestCount(descriptor: Char): Int = requests[descriptor]?.get() ?: 0

        suspend fun awaitReadRequest(descriptor: Char, expected: Int) {
            withTimeout(3_000) {
                while (readRequestCount(descriptor) < expected) delay(10)
            }
        }

        suspend fun emit(packet: OutputPacket) {
            val input = intArrayOf('@'.code) + packet.pack() + intArrayOf(0, 0)
            val rawFlowField = Connection::class.java.getDeclaredField("mReceivedPacketFlow").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val rawFlow = rawFlowField.get(connection) as MutableSharedFlow<RawPacket>
            rawFlow.emit(RawPacket(input))
        }

        fun close() {
            portField.set(connection, null)
            Connection::class.java.getDeclaredField("isRunning").apply { isAccessible = true }.setBoolean(connection, false)
        }
    }
}
