package org.secu3.android.ui

import android.view.KeyEvent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.secu3.android.connection.BluetoothConnectionEvent
import org.secu3.android.connection.BtConnection
import org.secu3.android.connection.Connection
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.RawPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.domain.diagnostics.DiagnosticOutput
import org.secu3.android.ui.diagnostics.DiagnosticsViewModel
import java.util.concurrent.CopyOnWriteArrayList

class DiagnosticsNavigationIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val fakeTransports = mutableListOf<FakeBluetoothTransport>()

    @After
    fun closeFakeTransports() {
        fakeTransports.toList().forEach(FakeBluetoothTransport::disconnect)
        fakeTransports.clear()
    }

    @Test
    fun diagnosticsGraphSharesViewModelAcrossDialogAndRecreationThenClosesSessionOnExit() = runBlocking {
        val nativeHost = navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        assertDiagnosticsHost(nativeHost)

        val firstVm = diagnosticsViewModel()
        val transport = connectFakeBluetooth(connectionManager())
        await { transport.enterCount == 1 }
        assertFalse(firstVm.state.value.controlsEnabled)
        transport.emitDiagnosticInput(flags = 0)
        await { firstVm.state.value.controlsEnabled }
        compose.activityRule.scenario.onActivity {
            assertEquals(50f, firstVm.state.value.duty)
            firstVm.selectChannel(3)
            assertEquals(50f, firstVm.state.value.duty)
            firstVm.editFrequency(321f)
            firstVm.editDuty(37.5f)
        }
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ADDITIONAL_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertSame(firstVm, diagnosticsViewModel())

        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val restoredHost = nativeHost()
        val restoredVm = diagnosticsViewModel()
        assertSame(firstVm, restoredVm)
        assertEquals(3, restoredVm.state.value.channel)
        assertEquals(321f, restoredVm.state.value.frequency)
        assertEquals(37.5f, restoredVm.state.value.duty)
        compose.onNodeWithTag("enable_bl_de_checkbox").assertIsOn()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertSame(restoredVm, diagnosticsViewModel())
        assertEquals(false, restoredVm.state.value.enableBlDe)
        compose.onNodeWithText("IGN_OUT3").assertExists()
        compose.onNodeWithText("321.0 Hz").assertExists()
        compose.onNodeWithText("37.5 %").assertExists()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertNativeHome(restoredHost)
        compose.onAllNodesWithText(compose.activity.getString(R.string.disconnect_device)).assertCountEquals(0)

        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val reopenedVm = diagnosticsViewModel()
        assertNotSame(restoredVm, reopenedVm)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertNativeHome(restoredHost)
    }

    @Test
    fun confirmingAdditionalOptionsCommitsInViewModelAndDoesNotCreateAnotherGraphEntry() = runBlocking {
        val host = navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val vm = diagnosticsViewModel()
        val transport = connectFakeBluetooth(connectionManager())
        await { transport.enterCount == 1 }
        assertFalse(vm.state.value.controlsEnabled)
        transport.emitDiagnosticInput(flags = 0)
        await { vm.state.value.controlsEnabled }
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("enable_bl_de_checkbox").performClick()
        compose.onNodeWithTag("diagnostic_ok").performClick()
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        assertEquals(true, vm.state.value.enableBlDe)

        compose.activityRule.scenario.onActivity {
            vm.setOutput(DiagnosticOutput.BL, true)
            vm.setOutput(DiagnosticOutput.DE, true)
        }
        val blDeHigh = (1 shl 11) or (1 shl 12) or (1 shl 13) or (1 shl 14)
        assertEquals(blDeHigh, vm.state.value.outputBits)

        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("diagnostic_ok").performClick()
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(true, vm.state.value.enableBlDe)
        assertEquals(blDeHigh, vm.state.value.outputBits)

        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        assertSame(vm, diagnosticsViewModel())
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
    }

    @Test
    fun linkLossClearsReadingsAndBlocksCommandsUntilFreshDiagnosticInput() = runBlocking {
        navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val vm = diagnosticsViewModel()
        assertFalse(vm.state.value.controlsEnabled)
        val transport = connectFakeBluetooth(connectionManager())
        await { transport.enterCount == 1 }
        assertFalse(vm.state.value.controlsEnabled)
        transport.emitDiagnosticInput(flags = 1, voltageRaw = 500)
        await { vm.state.value.controlsEnabled }
        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("1.250 V")

        compose.activityRule.scenario.onActivity {
            vm.selectChannel(3)
            vm.editFrequency(321f)
            vm.editDuty(37.5f)
            vm.setOutput(DiagnosticOutput.IGN_OUT4, true)
            vm.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
            vm.setOutput(DiagnosticOutput.BL, true)
            vm.setOutput(DiagnosticOutput.DE, true)
            vm.setOutput(DiagnosticOutput.TACH_O, true)
        }
        val activeFrequency = vm.state.value.frequency
        val activeDuty = vm.state.value.duty
        compose.waitForIdle()
        compose.onNodeWithTag("diagnostics-settings-icon").performClick()
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ADDITIONAL_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)

        transport.disconnect()
        await { !vm.state.value.controlsEnabled && vm.state.value.input == null }
        compose.waitForIdle()
        assertEquals(DIAGNOSTICS_ROUTE, compose.activity.rootNavigationController?.currentDestination?.route)
        compose.onNodeWithTag("analog_Voltage-value").assertTextEquals("— V")
        assertEquals(0, vm.state.value.channel)
        assertEquals(0b11, vm.state.value.outputBits)
        assertEquals(activeFrequency, vm.state.value.frequency)
        assertEquals(activeDuty, vm.state.value.duty)

        val bitsAfterLoss = vm.state.value.outputBits
        val writesAfterLoss = transport.writes.size
        compose.activityRule.scenario.onActivity {
            vm.selectChannel(5)
            vm.editFrequency(999f)
            vm.editDuty(99f)
            vm.setOutput(DiagnosticOutput.IGN_OUT1, false)
            vm.applyAdditionalOptions(enableBlDe = true, enableTachO = true)
        }
        assertEquals(0, vm.state.value.channel)
        assertEquals(activeFrequency, vm.state.value.frequency)
        assertEquals(activeDuty, vm.state.value.duty)
        assertEquals(bitsAfterLoss, vm.state.value.outputBits)
        assertEquals(writesAfterLoss, transport.writes.size)

        val reconnected = connectFakeBluetooth(connectionManager())
        await { transport.enterCount == 1 }
        await { reconnected.enterCount == 1 }
        assertFalse(vm.state.value.controlsEnabled)
        val writesBeforeFreshInput = reconnected.writes.size
        compose.activityRule.scenario.onActivity {
            vm.selectChannel(2)
            vm.editFrequency(999f)
            vm.editDuty(99f)
        }
        assertEquals(0, vm.state.value.channel)
        assertEquals(activeFrequency, vm.state.value.frequency)
        assertEquals(activeDuty, vm.state.value.duty)
        assertEquals(writesBeforeFreshInput, reconnected.writes.size)
        reconnected.emitDiagnosticInput(flags = 0)
        await { vm.state.value.controlsEnabled }
        assertEquals(0, vm.state.value.outputBits)
        assertEquals(0, vm.state.value.channel)
        assertEquals(activeFrequency, vm.state.value.frequency)
        assertEquals(activeDuty, vm.state.value.duty)
    }

    @Test
    fun enterSubscriptionIsReadyForImmediateAckAndInitialInputIsSentOnce() = runBlocking {
        navigateHome()
        compose.activityRule.scenario.onActivity { it.openDiagnostics() }
        compose.waitForIdle()
        val vm = diagnosticsViewModel()
        val transport = connectFakeBluetooth(connectionManager(), acknowledgeEnterImmediately = true)

        await { transport.enterCount == 1 }
        await { transport.enterCount == 1 && transport.diagInputRequestCount == 1 }
        assertFalse(vm.state.value.controlsEnabled)
        transport.emitDiagnosticInput(flags = 1)
        await { vm.state.value.controlsEnabled && transport.outputCount == 1 }
        assertEquals(0b11, vm.state.value.outputBits)

        transport.emitOpcode(6)
        transport.emitDiagnosticInput(flags = 1, voltageRaw = 600)
        await { vm.state.value.input?.voltage == 1.5f }
        compose.activityRule.scenario.onActivity { vm.selectChannel(1) }
        await { transport.outputCount == 2 }
        assertEquals(1, transport.diagInputRequestCount)
        assertEquals(2, transport.outputCount)
    }

    private fun navigateHome(): NavHostController {
        compose.waitUntil(10_000) { compose.activity.rootNavigationController != null }
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
            host.navigate(HOME_ROUTE)
        }
        compose.waitForIdle()
        return host
    }

    private fun nativeHost(): NavHostController {
        lateinit var host: NavHostController
        compose.activityRule.scenario.onActivity { activity ->
            host = requireNotNull(activity.rootNavigationController)
        }
        return host
    }

    private fun diagnosticsViewModel(): DiagnosticsViewModel {
        lateinit var vm: DiagnosticsViewModel
        compose.activityRule.scenario.onActivity { activity ->
            val controller = requireNotNull(activity.rootNavigationController)
            val graphEntry = controller.getBackStackEntry(DIAGNOSTICS_GRAPH_ROUTE)
            vm = ViewModelProvider(graphEntry.viewModelStore, activity.defaultViewModelProviderFactory, graphEntry.defaultViewModelCreationExtras)[DiagnosticsViewModel::class.java]
        }
        return vm
    }

    private fun assertDiagnosticsHost(host: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(DIAGNOSTICS_ROUTE, activity.rootNavigationController?.currentDestination?.route)
            assertEquals(Lifecycle.State.CREATED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
            assertEquals(Lifecycle.State.RESUMED, host.getBackStackEntry(DIAGNOSTICS_ROUTE).lifecycle.currentState)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.title_activity_diagnostics)).assertExists()
    }

    private fun assertNativeHome(host: NavHostController) {
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(HOME_ROUTE, host.currentDestination?.route)
            assertEquals(Lifecycle.State.RESUMED, host.getBackStackEntry(HOME_ROUTE).lifecycle.currentState)
            assertEquals(HOME_ROUTE, activity.rootNavigationController?.currentDestination?.route)
        }
        compose.onNodeWithTag("home-card-diagnostics").assertExists()
    }

    private fun connectionManager(): Secu3ConnectionManager =
        ViewModelProvider(compose.activity)[MainActivityViewModel::class.java].connectionManager

    private fun connectFakeBluetooth(
        manager: Secu3ConnectionManager,
        acknowledgeEnterImmediately: Boolean = false,
    ): FakeBluetoothTransport = runBlocking {
        val btField = Secu3ConnectionManager::class.java.declaredFields.first { BtConnection::class.java.isAssignableFrom(it.type) }
            .apply { isAccessible = true }
        val connection = btField.get(manager) as BtConnection
        val rawFlow = Connection::class.java.getDeclaredField("mReceivedPacketFlow").apply { isAccessible = true }
            .get(connection) as MutableSharedFlow<RawPacket>
        val writes = CopyOnWriteArrayList<ByteArray>()
        val adapter = requireNotNull(BluetoothAdapter.getDefaultAdapter()) { "Bluetooth adapter is unavailable" }
        val socket = adapter.getRemoteDevice("00:11:22:33:44:55").createRfcommSocketToServiceRecord(UUID.randomUUID())
        val socketState = runCatching { BluetoothSocket::class.java.getDeclaredField("mSocketState").apply { isAccessible = true } }.getOrNull()
        assumeTrue("Run DiagnosticsNavigationIntegrationTest with --no-hidden-api-checks", socketState != null)
        val socketStateField = socketState!!
        socketStateField.set(socket, socketStateField.type.enumConstants!!.first { (it as Enum<*>).name == "CONNECTED" })
        val socketOutput = object : OutputStream() {
            override fun write(value: Int) { writes += byteArrayOf(value.toByte()) }
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                val copy = bytes.copyOfRange(offset, offset + length)
                writes += copy
                if (acknowledgeEnterImmediately && copy.size > 3 && copy[1] == 'u'.code.toByte() && copy[3] == 6.toByte()) {
                    runBlocking {
                        withTimeout(3_000) {
                            rawFlow.emit(RawPacket(intArrayOf('@'.code, 'u'.code, 0, 6, 0, 0)))
                        }
                    }
                }
            }
        }
        val socketOutputField = runCatching { BluetoothSocket::class.java.getDeclaredField("mSocketOS").apply { isAccessible = true } }.getOrNull()
        assumeTrue("Run DiagnosticsNavigationIntegrationTest with --no-hidden-api-checks", socketOutputField != null)
        socketOutputField!!.set(socket, socketOutput)
        Connection::class.java.getDeclaredField("isRunning").apply { isAccessible = true }.setBoolean(connection, true)
        check(
            Connection::class.java.getDeclaredMethod("installEndpoint", Any::class.java).apply { isAccessible = true }
                .invoke(connection, socket) == true,
        )

        val fence = Secu3ConnectionManager::class.java.getDeclaredField("usbSessionFence")
            .apply { isAccessible = true }.get(manager) as org.secu3.android.connection.UsbSessionFence
        val generation = fence.advance()
        val selectionClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Selection" }
        val transportClass = Secu3ConnectionManager::class.java.declaredClasses.first { it.simpleName == "Transport" }
        val bluetoothTransport = transportClass.enumConstants!!.first { (it as Enum<*>).name == "BLUETOOTH" }
        val selection = selectionClass.declaredConstructors.first().apply { isAccessible = true }
            .newInstance(bluetoothTransport, generation)
        @Suppress("UNCHECKED_CAST")
        val selected = Secu3ConnectionManager::class.java.getDeclaredField("selectedTransport").apply { isAccessible = true }
            .get(manager) as kotlinx.coroutines.flow.MutableStateFlow<Any>
        selected.value = selection
        Secu3ConnectionManager::class.java.getDeclaredField("activeUsbAttachment").apply { isAccessible = true }
            .set(manager, null)

        val btEvents = BtConnection::class.java.getDeclaredField("_sessionEvents").apply { isAccessible = true }
            .get(connection) as MutableSharedFlow<BluetoothConnectionEvent>
        await { rawFlow.subscriptionCount.value > 0 && btEvents.subscriptionCount.value > 0 }
        rawFlow.emit(firmwarePacket())
        await { manager.firmwareInfo.value != null }
        btEvents.emit(BluetoothConnectionEvent(generation, Connected))
        await { manager.currentConnectionState == Connected }
        FakeBluetoothTransport(manager, generation, socket, rawFlow, writes).also(fakeTransports::add)
    }

    private suspend fun await(predicate: () -> Boolean) {
        withTimeout(4_000) { while (!predicate()) delay(10) }
    }

    private class FakeBluetoothTransport(
        private val manager: Secu3ConnectionManager,
        private val generation: Long,
        private val socket: BluetoothSocket,
        private val rawFlow: MutableSharedFlow<RawPacket>,
        val writes: CopyOnWriteArrayList<ByteArray>,
    ) {
        val enterCount: Int get() = writes.count { it.size > 3 && it[1] == 'u'.code.toByte() && it[3] == 6.toByte() }
        val diagInputRequestCount: Int get() = writes.count { it.size > 2 && it[1] == 'h'.code.toByte() && it[2] == '='.code.toByte() }
        val outputCount: Int get() = writes.count { it.size > 1 && it[1] == '^'.code.toByte() }

        suspend fun emitDiagnosticInput(flags: Int, voltageRaw: Int = 0) {
            rawFlow.emit(
                RawPacket(
                    intArrayOf('@'.code, '='.code, flags, voltageRaw shr 8, voltageRaw and 0xFF) +
                        IntArray(28) + intArrayOf(0, 0),
                ),
            )
        }

        suspend fun emitOpcode(opCode: Int) {
            rawFlow.emit(RawPacket(intArrayOf('@'.code, 'u'.code, 0, opCode, 0, 0)))
        }

        fun disconnect() = runBlocking {
            val selectedField = Secu3ConnectionManager::class.java.getDeclaredField("selectedTransport").apply { isAccessible = true }
            val selected = selectedField.get(manager) as kotlinx.coroutines.flow.StateFlow<*>
            val currentGeneration = selected.value?.let { selection ->
                selection.javaClass.getDeclaredField("generation").apply { isAccessible = true }.get(selection) as Long
            }
            if (currentGeneration == generation) manager.stopConnection()
            runCatching { socket.close() }
        }
    }

    private fun firmwarePacket() = RawPacket(
        intArrayOf('@'.code, 'y'.code) + "SECU-3 navigation test firmware".padEnd(52).map { it.code }.toIntArray() +
            intArrayOf(0, 0, 0, 0, 0x10, 0, 0),
    )

}
