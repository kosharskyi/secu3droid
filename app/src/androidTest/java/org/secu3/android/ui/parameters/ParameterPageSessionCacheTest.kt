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
import kotlinx.coroutines.flow.first
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
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.UserPrefs
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.PARAMETERS_ROUTE
import org.secu3.android.ui.parameters.pages.compose.AnglesUiState
import org.secu3.android.ui.parameters.pages.compose.AnglesNumber
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
            assertEquals(null, vm.starterControl.values.value)
            transport.emit(StarterParamPacket(starterOff = 120))
            await { vm.starterControl.values.value != null }
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
            assertTrue(vm.starterControl.values.value != null)

            compose.runOnIdle {
                vm.editStarterInt(StarterNumber.STARTER_OFF, 140)
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

    @Test fun starterCommandsUseLiveFirmwareAndReportOnlyAdmittedSnapshots() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 1 shl 28).also { transport = it }
            activate(vm, R.string.params_tab_start_title)
            usb.awaitReadRequest('o', 1)
            usb.emit(StarterParamPacket(starterOff = 120, crankToRunTime = 1.2345f))
            await { vm.starterControl.values.value?.starterOff == 120 }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = usb.writes.size

            compose.runOnIdle {
                vm.editStarterInt(StarterNumber.STARTER_OFF, 140)
                vm.editStarterInt(StarterNumber.AFTERSTART_PETROL, 2)
            }
            usb.awaitWriteCount(baseline + 2)
            assertEquals('o', usb.writes[baseline][1].toInt().toChar())
            assertEquals(140, usb.writes[baseline][3].toInt() and 0xff)
            assertEquals(1, usb.writes[baseline + 1][8].toInt() and 0xff)
            assertEquals(2, vm.starterControl.values.value?.afterstartPetrol)
            assertEquals(1.23f, vm.starterControl.values.value?.crankToRunCold)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.fuelInjection }
            val beforeUnsupportedEdit = usb.writes.size
            compose.runOnIdle { vm.editStarterFloat(StarterNumber.PRIME_COLD, 2f) }
            delay(100)
            assertEquals(beforeUnsupportedEdit, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            val committed = vm.starterControl.values.value
            compose.runOnIdle {
                vm.pausePageSession()
                vm.editStarterInt(StarterNumber.STARTER_OFF, 160)
            }
            assertEquals(beforeUnsupportedEdit, usb.writes.size)
            assertEquals(committed, vm.starterControl.values.value)
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
            assertEquals(null, vm.starterControl.values.value)
            transport.emit(StarterParamPacket(starterOff = 120))
            await { vm.starterControl.values.value != null }
            delay(150)

            val afterPendingRead = transport.readRequestCount('o')
            activate(vm, R.string.params_tab_angles_title)
            activate(vm, R.string.params_tab_start_title)
            delay(300)
            assertEquals(afterPendingRead, transport.readRequestCount('o'))
            assertEquals(1, transport.readRequestCount('m'))
            assertTrue(vm.starterControl.values.value != null)
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

    @Test fun securityCommandsUseLiveSessionAdmissionAndRetainWriteOnlyBuffersOnRefusal() = runBlocking {
        val vm = paramsViewModel()
        val transport = installFakeUsb(vm)
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            activate(vm, R.string.params_tab_security_title)
            transport.awaitReadRequest('#', 1)
            transport.emit(SecurityParamPacket(btFlags = 0xA0, iButton0 = listOf(1, 2, 3, 4, 5, 6), iButton1 = listOf(10, 11, 12, 13, 14, 15)))
            await { vm.securityControl.values.value != null }
            assertFalse(requireNotNull(vm.securityControl.values.value).useImmobilizer)

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = transport.writes.size

            compose.runOnIdle { vm.changeSecurityKey(0, "112233") }
            assertEquals(baseline, transport.writes.size)
            compose.runOnIdle { vm.changeSecurityKey(0, "11223344556G") }
            assertEquals(baseline, transport.writes.size)

            compose.runOnIdle { vm.changeSecurityKey(0, "112233445566") }
            transport.awaitWriteCount(baseline + 1)
            assertEquals('#', transport.writes[baseline][1].toInt().toChar())
            assertEquals(0xA0, transport.writes[baseline][4].toInt() and 0xff)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertEquals("112233445566", vm.securityKey0)

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle {
                vm.changeSecurityName("ECU")
                vm.changeSecurityPassword("12")
                vm.applySecurityCredentials()
            }
            assertEquals(baseline + 1, transport.writes.size)
            assertEquals("ECU", vm.securityName)
            assertEquals("12", vm.securityPassword)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            compose.runOnIdle {
                vm.changeSecurityPassword("1234")
                vm.applySecurityCredentials()
            }
            assertEquals(baseline + 1, transport.writes.size) // useBt=false refuses credentials.
            assertEquals("ECU", vm.securityName)
            assertEquals("1234", vm.securityPassword)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            compose.runOnIdle { vm.changeSecurityBluetooth(true) }
            transport.awaitWriteCount(baseline + 2)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle { vm.applySecurityCredentials() }
            transport.awaitWriteCount(baseline + 3)
            assertEquals('#', transport.writes[baseline + 2][1].toInt().toChar())
            assertEquals(3, transport.writes[baseline + 2][2].toInt() and 0xff) // OEM name length.
            assertEquals(4, transport.writes[baseline + 2][3].toInt() and 0xff) // Password length.
            assertEquals("", vm.securityName)
            assertEquals("", vm.securityPassword)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle {
                vm.pausePageSession()
                vm.changeSecurityName("held")
                vm.changeSecurityPassword("5678")
                vm.applySecurityCredentials()
            }
            assertEquals(baseline + 3, transport.writes.size)
            assertEquals("held", vm.securityName)
            assertEquals("5678", vm.securityPassword)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
            compose.runOnIdle { vm.changeSecurityKey(0, "AABBCCDDEEFF") }
            assertEquals(baseline + 3, transport.writes.size)
            assertEquals("112233445566", vm.securityControl.values.value?.key0)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
        } finally {
            try {
                transport.close()
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

    @Test fun miscellaneousCommandsUseCurrentFirmwareAndReportSessionAdmission() = runBlocking {
        val vm = paramsViewModel()
        val transport = installFakeUsb(vm, firmwareOptions = (1 shl 28) or (1 shl 19))
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            activate(vm, R.string.params_tab_miscellaneous_title)
            transport.awaitReadRequest('z', 1)
            transport.emit(MiscellaneousParamPacket(
                ignCutoff = 1, evapAfbegin = 5000, evapAfslope = 32f / 170000f,
                pwmFrq0 = 100, pwmFrq1 = 200, vssPeriodDist = 1000f,
            ))
            await { vm.miscellaneousControl.values.value != null }
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = transport.writes.size

            compose.runOnIdle { vm.editMiscellaneous(org.secu3.android.ui.parameters.pages.MiscField.PERIOD, 120) }
            transport.awaitWriteCount(baseline + 1)
            assertEquals('z', transport.writes[baseline][1].toInt().toChar())
            assertTrue(transport.writes[baseline].size > 6)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            transport.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.fuelInjection }
            compose.runOnIdle { vm.toggleMiscellaneous(org.secu3.android.ui.parameters.pages.MiscToggle.GAS, true) }
            assertEquals(baseline + 1, transport.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle {
                vm.pausePageSession()
                vm.editMiscellaneous(org.secu3.android.ui.parameters.pages.MiscField.PERIOD, 130)
            }
            assertEquals(baseline + 1, transport.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
        } finally {
            try {
                transport.close()
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

    @Test fun fuelInjectionCommandsUseCurrentFirmwareAndSnapshotRealCylinderMetadata() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val options = (1 shl 17) or (1 shl 28)
            val usb = installFakeUsb(vm, firmwareOptions = options).also { transport = it }
            activate(vm, R.string.params_tab_fuel_injection_title)
            usb.awaitReadRequest(';', 1)
            val received = InjctrParPacket(
                config = intArrayOf(0x11, 0x11),
                flowRate = floatArrayOf(300f, 400f),
                ckpsEngineCyl = 4,
                injMaxPw = floatArrayOf(10f, 11f),
            ).also { it.engineDisp = 2f }
            usb.emitFuelInjection(received)
            await { vm.fuelInjectionControl.values.value?.banks?.getOrNull(0)?.flow == 300f }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            val baseline = usb.writes.size
            queue.pending.value.forEach { queue.acknowledge(it.id) }

            compose.runOnIdle {
                vm.editFuelInjection(org.secu3.android.ui.parameters.pages.compose.FuelInjectionNumber.FLOW_0, 321f)
            }
            usb.awaitWriteCount(baseline + 1)
            assertEquals(';', usb.writes[baseline][1].toInt().toChar())
            assertEquals(0x11, usb.writes[baseline][3].toInt() and 0xff)
            assertEquals(0x50, usb.writes[baseline][5].toInt() and 0xff)
            assertEquals(0x40, usb.writes[baseline][6].toInt() and 0xff)
            assertEquals(0, usb.writes[baseline][19].toInt() and 0xff) // Outgoing packet keeps the ECU's zero RX-only cylinder stub.
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.fuelInjection }
            val beforeUnsupportedEdit = usb.writes.size
            compose.runOnIdle {
                vm.editFuelInjection(org.secu3.android.ui.parameters.pages.compose.FuelInjectionNumber.FLOW_0, 500f)
            }
            delay(100)
            assertEquals(beforeUnsupportedEdit, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            usb.emitFirmware(options = options)
            await { vm.capabilities.value.fuelInjection }
            val beforeRestrictedFlag = usb.writes.size
            compose.runOnIdle { vm.changeFuelInjectionFlag(org.secu3.android.ui.parameters.pages.compose.FuelInjectionFlag.SWITCH_ROWS, true) }
            delay(100)
            assertEquals(beforeRestrictedFlag, usb.writes.size)

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val beforePausedEdit = usb.writes.size
            compose.runOnIdle {
                vm.pausePageSession()
                vm.editFuelInjection(org.secu3.android.ui.parameters.pages.compose.FuelInjectionNumber.FLOW_0, 600f)
            }
            assertEquals(beforePausedEdit, usb.writes.size)
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

    @Test fun ckpsCommandsUseCurrentFirmwareAndReportOnlyAdmittedSessionWrites() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 0).also { transport = it }
            activate(vm, R.string.params_tab_ckps_title)
            usb.awaitReadRequest('t', 1)
            usb.emit(CkpsParamPacket(ckpsCogsBtdc = 20, ckpsIgnitCogs = 5, ckpsEngineCyl = 4, ckpsCogsNum = 60, ckpsMissNum = 2, hallFlags = 0xE0, hallWndWidth = -12.3f, hallDegreesBtdc = -45.6f, mttf = 2.5f))
            await { vm.ckpsControl.values.value?.wheel == 60 }
            // Hall angles arrive quantized to the protocol's 1/32-degree words.
            assertEquals(-12.3125f, vm.ckpsControl.values.value?.hallWindowWidth)
            assertEquals(-45.59375f, vm.ckpsControl.values.value?.hallDegreesBeforeTdc)

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            val baseline = usb.writes.size
            queue.pending.value.forEach { queue.acknowledge(it.id) }

            compose.runOnIdle { vm.editCkps(org.secu3.android.ui.parameters.pages.compose.CkpsNumber.WHEEL, 80f) }
            usb.awaitWriteCount(baseline + 1)
            assertEquals('t', usb.writes[baseline][1].toInt().toChar())
            assertEquals(80, usb.writes[baseline][5].toInt() and 0xff)
            assertEquals(0xE0, usb.writes[baseline][7].toInt() and 0xff)
            assertEquals(-12.3125f, vm.ckpsControl.values.value?.hallWindowWidth)
            assertEquals(-45.59375f, vm.ckpsControl.values.value?.hallDegreesBeforeTdc)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.emitFirmware(options = 1 shl 2)
            await { vm.capabilities.value.known && !vm.capabilities.value.cogsBtdc }
            val beforeRestrictedChoice = usb.writes.size
            compose.runOnIdle { vm.selectCkpsChoice(true, 21) }
            delay(100)
            assertEquals(beforeRestrictedChoice, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            val beforePausedEdit = usb.writes.size
            compose.runOnIdle {
                vm.pausePageSession()
                vm.editCkps(org.secu3.android.ui.parameters.pages.compose.CkpsNumber.WHEEL, 90f)
            }
            assertEquals(beforePausedEdit, usb.writes.size)
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
        val prefs = Secu3ConnectionManager::class.java.getDeclaredField("mPrefs").apply { isAccessible = true }.get(manager) as UserPrefs
        val originalWakeLockPreference = prefs.settings.first().isWakeLockEnabled
        manager.onBackgroundWorkPreferenceChanged(false)
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
            return FakeUsbTransport(manager, connection, portField, attemptPackets, requests, writes, nextWriteBarrier, originalWakeLockPreference)
        } catch (failure: Throwable) {
            try {
                manager.onUsbDeviceDetached(1)
            } finally {
                manager.onBackgroundWorkPreferenceChanged(originalWakeLockPreference)
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

        suspend fun emitFuelInjection(packet: InjctrParPacket) {
            val input = intArrayOf('@'.code) + packet.pack() + intArrayOf(0, 0)
            input[19] = packet.ckpsEngineCyl
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
                    kotlinx.coroutines.runBlocking { manager.onBackgroundWorkPreferenceChanged(originalWakeLockPreference) }
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
