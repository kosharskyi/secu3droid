package org.secu3.android.ui.parameters

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.secu3.android.R
import org.secu3.android.connection.Connection
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.connection.UsbConnection
import org.secu3.android.connection.UsbAttemptPacket
import org.secu3.android.connection.UsbAttachmentIdentity
import org.secu3.android.connection.UsbSessionFence
import org.secu3.android.models.RawPacket
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.UserPrefs
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.PARAMETERS_ROUTE
import org.secu3.android.ui.parameters.pages.compose.AnglesUiState
import org.secu3.android.ui.parameters.pages.compose.AnglesNumber
import org.secu3.android.ui.parameters.pages.compose.StarterUiState
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import org.secu3.android.ui.parameters.pages.compose.ChokeNumber
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

    @Before fun grantBluetoothPermissions() {
        val targetPackageName = compose.activity.packageName
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        needed.forEach { permission ->
            if (compose.activity.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(targetPackageName, permission)
            }
        }
    }

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

    @Test fun gasDoseCommandsUseCurrentFirmwareAndReportSuccessfulSessionAdmission() = runBlocking {
        val vm = paramsViewModel()
        var stage = "install USB"
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 1 shl 29).also { transport = it }
            stage = "wait for Gas Dose read request"
            activate(vm, R.string.params_tab_gas_dose_title)
            usb.awaitReadRequest('*', 1)
            stage = "emit and receive Gas Dose values"
            usb.emit(GasDoseParamPacket(steps = 120, fcClosing = 10f, lambdaCorrLimitP = 10f, lambdaCorrLimitM = 10f, lambdaStoichval = 14.7f))
            await { vm.gasDoseControl.values.value?.steps == 120f }

            stage = "pause Activity at STARTED and isolate toast scope"
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            val baseline = usb.writes.size
            queue.pending.value.forEach { queue.acknowledge(it.id) }

            stage = "ordinary command write"
            compose.runOnIdle { vm.editGasDose(org.secu3.android.ui.parameters.pages.compose.GasDoseNumber.STEPS, 500f) }
            usb.awaitWriteCount(baseline + 1)
            assertEquals('!', usb.writes[baseline][0].toInt().toChar())
            assertEquals('*', usb.writes[baseline][1].toInt().toChar())
            assertEquals(0, usb.writes[baseline][5].toInt() and 0xff)

            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            compose.runOnIdle { vm.stepGasDose(1) }
            stage = "firmware support loss"
            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.gasDose }
            delay(100)
            stage = "firmware support recovery"
            usb.emitFirmware(options = 1 shl 29)
            await { vm.capabilities.value.gasDose }
            compose.runOnIdle { vm.finishGasDoseManual() }
            assertEquals(baseline + 1, usb.writes.size)

            stage = "manual command write"
            compose.runOnIdle {
                vm.stepGasDose(-1)
                vm.finishGasDoseManual()
            }
            usb.awaitWriteCount(baseline + 2)
            assertEquals(255, usb.writes[baseline + 1][5].toInt() and 0xff)

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val beforePausedFlush = usb.writes.size
            stage = "paused manual-send admission"
            compose.runOnIdle {
                vm.pausePageSession()
                vm.stepGasDose(1)
                vm.finishGasDoseManual()
            }
            assertEquals(beforePausedFlush, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
            compose.runOnIdle { vm.finishGasDoseManual() }
            assertEquals(beforePausedFlush, usb.writes.size)
        } catch (failure: Throwable) {
            val capabilities = vm.capabilities.value
            throw AssertionError(
                "Gas Dose test failed during $stage; capabilities=(known=${capabilities.known}, gasDose=${capabilities.gasDose}), " +
                    "values=${vm.gasDoseControl.values.value}, writes=${transport?.writes?.size ?: 0}",
                failure,
            )
        } finally {
            try {
                transport?.close()
            } finally {
                try {
                    compose.runOnIdle { vm.clearPageState() }
                } finally {
                    queue.onActiveScopeChanged(originalActiveScope)
                    compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
                }
            }
        }
    }

    @Test fun chokeCommandsUseCurrentFirmwareAndRespectSessionAdmission() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val options = (1 shl 22) or (1 shl 28)
            val usb = installFakeUsb(vm, firmwareOptions = options).also { transport = it }
            activate(vm, R.string.params_tab_choke_control_title)
            usb.awaitReadRequest('%', 1)
            usb.emit(ChokeControlParPacket(smSteps = 120))
            await { vm.chokeControl.values.value?.steps == 120 }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            val baseline = usb.writes.size
            queue.pending.value.forEach { queue.acknowledge(it.id) }

            compose.runOnIdle { vm.editChoke(ChokeNumber.STEPS, 500f) }
            usb.awaitWriteCount(baseline + 1)
            assertEquals('%', usb.writes[baseline][1].toInt().toChar())
            assertEquals(0, usb.writes[baseline][5].toInt() and 0xff)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            compose.runOnIdle { vm.stepChoke(1) }
            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.choke }
            delay(100)
            usb.emitFirmware(options = options)
            await { vm.capabilities.value.choke }
            compose.runOnIdle { vm.finishChokeManual() }
            assertEquals(baseline + 1, usb.writes.size)

            compose.runOnIdle {
                vm.stepChoke(-1)
                vm.finishChokeManual()
            }
            usb.awaitWriteCount(baseline + 2)
            assertEquals(255, usb.writes[baseline + 1][5].toInt() and 0xff)

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val beforePausedFlush = usb.writes.size
            compose.runOnIdle {
                vm.pausePageSession()
                vm.stepChoke(1)
                vm.finishChokeManual()
            }
            assertEquals(beforePausedFlush, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
        } finally {
            try {
                transport?.close()
            } finally {
                try {
                    compose.runOnIdle { vm.clearPageState() }
                } finally {
                    queue.onActiveScopeChanged(originalActiveScope)
                    compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
                }
            }
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
        compose.runOnIdle { vm.activatePage(requireNotNull(ParameterPageCatalog().pageForTitle(titleRes))) }
    }

    private suspend fun await(predicate: () -> Boolean) {
        withTimeout(3_000) {
            while (!predicate()) delay(10)
        }
    }

    private suspend fun installFakeUsb(vm: ParamsViewModel, firmwareOptions: Int = 0): FakeUsbTransport {
        compose.waitForIdle()
        try {
            withTimeout(3_000) { while (!compose.runOnIdle { compose.activity.hasWindowFocus() }) delay(10) }
        } catch (failure: TimeoutCancellationException) {
            throw AssertionError("MainActivity did not regain window focus before fake USB installation", failure)
        }
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
            Secu3ConnectionManager::class.java.getDeclaredField("activeUsbAttachment").apply { isAccessible = true }
                .set(manager, UsbAttachmentIdentity(1, "/test/parameter-session/1", 0, 0))
            val selectionClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Selection" }
            val transportClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Transport" }
            val usbTransport = transportClass.enumConstants!!.first { (it as Enum<*>).name == "USB" }
            val selection = selectionClass.declaredConstructors.first().apply { isAccessible = true }.newInstance(usbTransport, generation)
            val subscribed = CompletableDeferred<Unit>()
            val readyField = Secu3ConnectionManager::class.java.getDeclaredField("usbSelectionReady").apply { isAccessible = true }
            synchronized(manager) { readyField.set(manager, subscribed) }
            @Suppress("UNCHECKED_CAST")
            val selected = Secu3ConnectionManager::class.java.getDeclaredField("selectedTransport").apply { isAccessible = true }
                .get(manager) as MutableStateFlow<Any>
            selected.value = selection
            Secu3ConnectionManager::class.java.getDeclaredField("activeUsbAttemptId").apply { isAccessible = true }.set(manager, 1L)
            try {
                withTimeout(3_000) { subscribed.await() }
            } catch (failure: TimeoutCancellationException) {
                throw AssertionError(
                    "Fake USB setup timed out waiting for transport subscription; selected=$selection, " +
                        "subscriptionComplete=${subscribed.isCompleted}, running=${connection.isRunning}, connected=${connection.isConnected}",
                    failure,
                )
            }
            synchronized(manager) {
                if (readyField.get(manager) === subscribed) readyField.set(manager, null)
            }
            attemptPackets.emit(UsbAttemptPacket(1L, firmwarePacket(firmwareOptions)))
            try {
                withTimeout(3_000) { while (!manager.isUsbConnected || !manager.isUsbSessionLocked) delay(10) }
            } catch (failure: TimeoutCancellationException) {
                throw AssertionError(
                    "Fake USB setup timed out waiting for firmware confirmation; usbConnected=${manager.isUsbConnected}, " +
                        "sessionLocked=${manager.isUsbSessionLocked}, connectionState=${manager.currentConnectionState}, " +
                        "firmware=${manager.fwInfo}, running=${connection.isRunning}, connected=${connection.isConnected}, selected=$selection",
                    failure,
                )
            }
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

        suspend fun emitFirmware(options: Int) {
            val raw = intArrayOf('@'.code, 'y'.code) + "SECU-3 test firmware".padEnd(52).map(Char::code).toIntArray() +
                intArrayOf((options shr 24) and 0xff, (options shr 16) and 0xff, (options shr 8) and 0xff, options and 0xff, 0x10, 0, 0)
            attemptPackets.emit(UsbAttemptPacket(1L, RawPacket(raw)))
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

    private fun firmwarePacket(options: Int = 0) = RawPacket(
        intArrayOf('@'.code, 'y'.code) + "SECU-3 test firmware".padEnd(52).map(Char::code).toIntArray() +
            intArrayOf((options shr 24) and 0xff, (options shr 16) and 0xff, (options shr 8) and 0xff, options and 0xff, 0x10, 0, 0),
    )
}
