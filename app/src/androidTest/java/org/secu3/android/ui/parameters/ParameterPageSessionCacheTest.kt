package org.secu3.android.ui.parameters

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
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
import org.secu3.android.connection.PacketReceiver
import org.secu3.android.models.RawPacket
import org.secu3.android.domain.parameters.ParameterPage
import org.secu3.android.domain.parameters.ParameterCapabilities
import org.secu3.android.domain.parameters.LtftValues
import org.secu3.android.domain.parameters.DbwValues
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.AnglesParamPacket
import org.secu3.android.models.packets.out.params.GasDoseParamPacket
import org.secu3.android.models.packets.out.params.ChokeControlParPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.CkpsParamPacket
import org.secu3.android.models.packets.out.params.MiscellaneousParamPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import org.secu3.android.models.packets.out.params.StarterParamPacket
import org.secu3.android.models.packets.out.params.TemperatureParamPacket
import org.secu3.android.models.packets.out.params.CarburParamPacket
import org.secu3.android.models.packets.out.params.IdlingParamPacket
import org.secu3.android.models.packets.out.params.KnockParamPacket
import org.secu3.android.models.packets.out.params.AccelerationParamPacket
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.models.packets.out.params.DbwParamPacket
import org.secu3.android.models.packets.out.params.FunSetParamPacket
import org.secu3.android.models.packets.out.params.AdcCorrectionsParamPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.ui.parameters.pages.compose.AdcNumber
import org.secu3.android.models.packets.out.ChangeModePacket
import org.secu3.android.utils.UserPrefs
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.PARAMETERS_ROUTE
import org.secu3.android.ui.parameters.pages.compose.AnglesNumber
import org.secu3.android.ui.parameters.pages.compose.StarterNumber
import org.secu3.android.ui.parameters.pages.compose.TemperatureFlag
import org.secu3.android.ui.parameters.pages.compose.TemperatureNumber
import org.secu3.android.ui.parameters.pages.compose.KnockNumber
import org.secu3.android.ui.parameters.pages.compose.KnockSelector
import org.secu3.android.ui.parameters.pages.compose.AccelerationNumber
import org.secu3.android.ui.parameters.pages.compose.LtftNumber
import org.secu3.android.ui.parameters.pages.compose.DbwNumber
import org.secu3.android.ui.parameters.pages.compose.FunctionsNumber
import org.secu3.android.ui.parameters.pages.compose.FunctionsChoices
import org.secu3.android.ui.parameters.pages.compose.ChokeNumber
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

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
            assertTrue(vm.anglesControl.values.value == null)
            transport.emit(AnglesParamPacket(maxAngle = 4f))
            await { vm.anglesControl.values.value != null }
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
            assertTrue(vm.anglesControl.values.value == null)
            transport.emit(AnglesParamPacket(maxAngle = 6f))
            await { vm.anglesControl.values.value != null }
            delay(150)

            compose.runOnIdle { vm.refreshPage(ParameterPage.ANGLES) }
            transport.awaitReadRequest('m', 3)
            assertTrue(vm.anglesControl.values.value == null)
            transport.emit(AnglesParamPacket(maxAngle = 7f))
            await { vm.anglesControl.values.value != null }
            delay(150)

            compose.runOnIdle {
                vm.clearPageState()
                vm.activatePage(ParameterPage.ANGLES)
            }
            transport.awaitReadRequest('m', 4)
            assertTrue(vm.anglesControl.values.value == null)
            transport.emit(AnglesParamPacket(maxAngle = 8f))
            await { vm.anglesControl.values.value != null }
        } finally {
            transport.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    @Test fun universalOutputsViewModelMapsAllSixOutputsAndPreservesSessionAndProtocolContracts() = runBlocking {
        val vm = paramsViewModel()
        val usb = installFakeUsb(vm)
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        try {
            activate(vm, R.string.params_tab_universal_outputs_title)
            usb.awaitReadRequest('&', 1)
            val inboundPayload = UniOutParamPacket().pack().drop(1).toIntArray() + intArrayOf(0xCA, 0xFE)
            val inbound = UniOutParamPacket().parse(intArrayOf('@'.code, '&'.code) + inboundPayload) as UniOutParamPacket
            usb.emit(inbound)
            await { vm.universalOutputsControl.values.value != null }
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.suppressAttachmentRescan()
            vm.isSendAllowed = false

            val expected = IntArray(67)
            val expectedTail = intArrayOf(0xCA, 0xFE)
            var writes = usb.writes.size
            suspend fun assertPacketSent() {
                usb.awaitWriteCount(++writes)
                val frame = usb.writes[writes - 1]
                assertEquals('!', frame[0].toInt().toChar())
                assertEquals('&', frame[1].toInt().toChar())
                val decoded = unescapeUniversalOutputWrite(frame)
                assertArrayEquals(expected + expectedTail, decoded.sliceArray(0 until 69))
                assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
                queue.pending.value.forEach { queue.acknowledge(it.id) }
            }
            suspend fun command(action: () -> Boolean, updateExpected: () -> Unit) {
                compose.runOnIdle { assertTrue(action()) }
                updateExpected()
                assertPacketSent()
            }

            for (output in 1..6) {
                command({ vm.setUniversalOutputEnabled(output, true) }, { setUniOutFlag(expected, output, bit = 2, value = true) })
                command({ vm.selectUniversalOutputCondition(output, 1, 0) }, { setUniOutCondition(expected, output, 1, 0); setUniOutThreshold(expected, output, 1, true, 55f, 4f); setUniOutThreshold(expected, output, 1, false, 50f, 4f) })
                command({ vm.setUniversalOutputConditionInverted(output, 1, true) }, { setUniOutFlag(expected, output, bit = 0, value = true) })
                command({ vm.setUniversalOutputConditionInverted(output, 2, true) }, { setUniOutFlag(expected, output, bit = 1, value = true) })
                command({ vm.selectUniversalOutputCondition(output, 2, 1) }, { setUniOutCondition(expected, output, 2, 1); setUniOutThreshold(expected, output, 2, true, 1500f, 1f); setUniOutThreshold(expected, output, 2, false, 1200f, 1f) })
                command({ vm.editUniversalOutputThreshold(output, 1, true, 0, 60f + output) }, { setUniOutThreshold(expected, output, 1, true, 60f + output, 4f) })
                command({ vm.editUniversalOutputThreshold(output, 1, false, 0, 45f + output) }, { setUniOutThreshold(expected, output, 1, false, 45f + output, 4f) })
                command({ vm.editUniversalOutputThreshold(output, 2, true, 1, 2000f + output) }, { setUniOutThreshold(expected, output, 2, true, 2000f + output, 1f) })
                command({ vm.editUniversalOutputThreshold(output, 2, false, 1, 1800f + output) }, { setUniOutThreshold(expected, output, 2, false, 1800f + output, 1f) })
                command({ vm.setUniversalOutputResultInverted(output, true) }, { setUniOutFlag(expected, output, bit = 3, value = true) })
                val logic = listOf(0, 1, 2, 3, 15, 0)[output - 1]
                command({ vm.selectUniversalOutputLogicFunction(output, logic) }, { setUniOutLogic(expected, output, logic) })
            }
            for (code in listOf(0, 1, 2, 3, 15)) {
                command({ vm.selectUniversalOutputsPairFunction(code) }, { expected[66] = code })
            }

            command({ vm.selectUniversalOutputCondition(1, 1, 14) }, { setUniOutCondition(expected, 1, 1, 14); setUniOutThreshold(expected, 1, 1, true, 55f, 4f); setUniOutThreshold(expected, 1, 1, false, 50f, 4f) })
            val stableState = vm.universalOutputsControl.values.value
            val noWriteCount = usb.writes.size
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle { assertFalse(vm.editUniversalOutputThreshold(1, 1, true, 0, 80f)) }
            assertEquals(noWriteCount, usb.writes.size)
            assertEquals(stableState, vm.universalOutputsControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            command({ vm.setUniversalOutputEnabled(2, false) }, { setUniOutFlag(expected, 2, bit = 2, value = false) })
            val pairRefusedCount = usb.writes.size
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle { assertFalse(vm.selectUniversalOutputsPairFunction(2)) }
            assertEquals(pairRefusedCount, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            compose.runOnIdle { vm.pausePageSession() }
            val pausedState = vm.universalOutputsControl.values.value
            val pausedWrites = usb.writes.size
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle { assertFalse(vm.setUniversalOutputEnabled(6, false)) }
            assertEquals(pausedWrites, usb.writes.size)
            assertEquals(pausedState, vm.universalOutputsControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertFalse(vm.isSendAllowed)
        } finally {
            usb.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    @Test fun adcPageAdmissionWritesCapturedPacketEvenWhenLegacySendFlagIsClosed() = runBlocking {
        val vm = paramsViewModel()
        val usb = installFakeUsb(vm)
        try {
            activate(vm, R.string.params_tab_adc_errors_corr_title)
            usb.awaitReadRequest('r', 1)
            assertEquals(null, vm.adcCorrectionsControl.values.value)

            val frame = intArrayOf(64, 'r'.code, 0xA1) + (0 until 12).flatMap { channel ->
                listOf(0, 0) + listOf(0x12, 0x34, 0x56, 0x70 + channel)
            }.toIntArray() + intArrayOf(0xCA, 0xFE)
            val packet = AdcCorrectionsParamPacket().parse(frame) as AdcCorrectionsParamPacket
            usb.emit(packet)
            await { vm.adcCorrectionsControl.values.value != null }
            delay(150)
            assertTrue(vm.adcCorrectionsControl.values.value!!.adcCompMode)
            assertEquals(0f, vm.adcCorrectionsControl.values.value!!.mapCorrection)

            val baseline = usb.writes.size
            usb.suppressAttachmentRescan()
            vm.isSendAllowed = false
            assertTrue(vm.editAdc(org.secu3.android.ui.parameters.pages.compose.AdcNumber.MAP_CORRECTION, 0f))
            assertTrue(vm.setAdcCompensationMode(false))
            usb.awaitWriteCount(baseline + 2)
            val mapEdit = usb.writes[baseline]
            assertEquals('r', mapEdit[1].toInt().toChar())
            assertEquals(0xA1, mapEdit[2].toInt() and 0xFF)
            assertArrayEquals(byteArrayOf(0, 0, 0x20, 0), mapEdit.sliceArray(5..8))
            for (channel in 1 until 12) {
                val offset = 5 + channel * 6
                assertArrayEquals(byteArrayOf(0x12, 0x34, 0x56, (0x70 + channel).toByte()), mapEdit.sliceArray(offset..offset + 3))
            }
            assertArrayEquals(byteArrayOf(0xCA.toByte(), 0xFE.toByte()), mapEdit.sliceArray(mapEdit.size - 5..mapEdit.size - 4))
            assertEquals('r', usb.writes[baseline + 1][1].toInt().toChar())
            assertEquals(0xA0, usb.writes[baseline + 1][2].toInt() and 0xFF)
            assertFalse(vm.isSendAllowed)
            assertEquals(false, vm.adcCorrectionsControl.values.value!!.adcCompMode)
        } finally {
            usb.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    @Test fun adcViewModelRoutesAllTwentyFourCommandsWithGoldenWordsAndLiveSessionGates() = runBlocking {
        val vm = paramsViewModel()
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        var transport: FakeUsbTransport? = null
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 1 shl 5).also { transport = it }
            activate(vm, R.string.params_tab_adc_errors_corr_title)
            usb.awaitReadRequest('r', 1)
            usb.emit(AdcCorrectionsParamPacket())
            await { vm.adcCorrectionsControl.values.value != null }
            delay(150)

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.suppressAttachmentRescan()
            vm.isSendAllowed = false

            val expectedValues = MutableList(24) { 0f }
            val factorWords = IntArray(12)
            val correctionWords = IntArray(12) { 0x2000 }
            val baseline = usb.writes.size
            AdcNumber.entries.forEachIndexed { index, number ->
                val channel = index / 2
                val value = if (index % 2 == 0) 1f + (channel + 1) / 8f else (channel + 1) * .125f
                expectedValues[index] = value
                if (index % 2 == 0) factorWords[channel] = 0x4000 + (channel + 1) * 0x800
                else correctionWords[channel] = 0x2000 + (channel + 1) * 0xC8000
                compose.runOnIdle { assertTrue("${number.name}", vm.editAdc(number, value)) }
                usb.awaitWriteCount(baseline + index + 1)
                val write = usb.writes[baseline + index]
                assertEquals('r', write[1].toInt().toChar())
                assertEquals(0, write[2].toInt() and 1)
                for (wireChannel in 0 until 12) {
                    val offset = 3 + wireChannel * 6
                    assertEquals(factorWords[wireChannel] ushr 8 and 0xFF, write[offset].toInt() and 0xFF)
                    assertEquals(factorWords[wireChannel] and 0xFF, write[offset + 1].toInt() and 0xFF)
                    val correction = correctionWords[wireChannel]
                    assertEquals(correction ushr 24 and 0xFF, write[offset + 2].toInt() and 0xFF)
                    assertEquals(correction ushr 16 and 0xFF, write[offset + 3].toInt() and 0xFF)
                    assertEquals(correction ushr 8 and 0xFF, write[offset + 4].toInt() and 0xFF)
                    assertEquals(correction and 0xFF, write[offset + 5].toInt() and 0xFF)
                }
                assertEquals(expectedValues, AdcNumber.entries.map { it.value(requireNotNull(vm.adcCorrectionsControl.values.value)) })
            }
            compose.runOnIdle { assertTrue(vm.setAdcCompensationMode(true)) }
            usb.awaitWriteCount(baseline + 25)
            assertEquals(1, usb.writes[baseline + 24][2].toInt() and 1)
            assertEquals(0x00, usb.writes[baseline + 24][5].toInt() and 0xFF)
            assertEquals(0x0E, usb.writes[baseline + 24][6].toInt() and 0xFF)
            assertEquals(0x30, usb.writes[baseline + 24][7].toInt() and 0xFF)
            assertEquals(0x00, usb.writes[baseline + 24][8].toInt() and 0xFF)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertEquals(false, vm.isSendAllowed)
            assertEquals(true, vm.adcCorrectionsControl.values.value?.adcCompMode)

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.emitFirmware(options = 1 shl 17) // SECU3T without TPIC8101: ADD_I3 and ADD_I4 unavailable.
            await { vm.capabilities.value.known && vm.capabilities.value.secu3T && !vm.capabilities.value.extraAnalogInputs }
            val unsupportedValues = vm.adcCorrectionsControl.values.value
            val unsupportedBaseline = usb.writes.size
            compose.runOnIdle {
                assertFalse(vm.editAdc(AdcNumber.ADD_I3_FACTOR, 2f))
                assertFalse(vm.editAdc(AdcNumber.ADD_I4_FACTOR, 2f))
            }
            delay(100)
            assertEquals(unsupportedBaseline, usb.writes.size)
            assertEquals(unsupportedValues, vm.adcCorrectionsControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            usb.emitFirmware(options = 1 shl 5)
            await { vm.capabilities.value.extraAnalogInputs && !vm.capabilities.value.secu3T }
            compose.runOnIdle { vm.pausePageSession() }
            val pausedValues = vm.adcCorrectionsControl.values.value
            val pausedBaseline = usb.writes.size
            compose.runOnIdle { assertFalse(vm.editAdc(AdcNumber.MAP_FACTOR, 2.25f)) }
            delay(100)
            assertEquals(pausedBaseline, usb.writes.size)
            assertEquals(pausedValues, vm.adcCorrectionsControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            val rawWords = IntArray(12) { 0x12345670 + it }
            val rawFrame = intArrayOf(64, 'r'.code, 0xA1) + (0 until 12).flatMap { channel ->
                listOf(0, 0, rawWords[channel] ushr 24 and 0xFF, rawWords[channel] ushr 16 and 0xFF,
                    rawWords[channel] ushr 8 and 0xFF, rawWords[channel] and 0xFF)
            }.toIntArray() + intArrayOf(0xCA, 0xFE)
            val rawPacket = AdcCorrectionsParamPacket().parse(rawFrame) as AdcCorrectionsParamPacket
            vm.adcCorrectionsControl.receive(rawPacket)
            assertFalse(vm.editAdc(AdcNumber.MAP_CORRECTION, 0f))
            assertArrayEquals(rawFrame.drop(1).toIntArray(), rawPacket.pack())
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

    @Test fun functionsReceiveAllNameFragmentsBeforeSettingsAndOfferMafEditor() = runBlocking {
        val vm = paramsViewModel()
        mountFunctionsPage(R.string.params_tab_functions_title, vm)
        val usb = installFakeUsb(vm)
        try {
            activate(vm, R.string.params_tab_functions_title)
            usb.awaitReadRequest('p', 1)
            assertEquals(null, vm.functionsControl.values.value)
            usb.emitFunctionName(1, "Alpha-N")
            delay(50)
            assertEquals(null, vm.functionsControl.values.value)
            usb.emitFunctionName(0, "Speed density")
            usb.awaitReadRequest('n', 1)
            val received = FunSetParamPacket(loadSrcCfg = 0, ckpsEngineCyl = 4, injCylDisp = .5f)
            usb.emitFunctions(received)
            await { vm.functionsControl.values.value?.functionNames == listOf("Speed density", "Alpha-N") }
            delay(150)

            vm.isSendAllowed = false // Functions writes follow actual session admission.
            val baseline = usb.writes.size
            usb.suppressAttachmentRescan()
            compose.onNodeWithTag("functions_load_source").performScrollTo().performClick()
            compose.onNodeWithTag("functions_option_4").performClick() // MAF is protocol code 4.
            val cancel = requireNotNull(vm.parameterEditSession)
            val request = cancel.request as ParameterEditRequest.FloatValue
            assertEquals(2f, request.value)
            assertEquals(.01f, request.min)
            assertEquals(8f, request.max)
            assertEquals(.0001f, request.step)
            assertEquals(4, request.precision)
            assertEquals(4, vm.functionsControl.values.value?.loadSource)
            compose.runOnIdle { vm.dismissParameterEditor(cancel.id) }
            usb.awaitWriteCount(baseline + 1)
            assertEquals("MAF source selection was sent", 4, usb.writes[baseline][20].toInt() and 0xFF)
            assertFalse(vm.isSendAllowed)
        } finally {
            usb.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    @Test fun functionsMafEditorConfirmationWritesTheDerivedPairThroughThePageRoute() = runBlocking {
        val vm = paramsViewModel()
        mountFunctionsPage(R.string.params_tab_functions_title, vm)
        val usb = installFakeUsb(vm)
        try {
            activate(vm, R.string.params_tab_functions_title)
            usb.awaitReadRequest('p', 1)
            usb.emitFunctionName(1, "Alpha-N")
            usb.emitFunctionName(0, "Speed density")
            usb.awaitReadRequest('n', 1)
            usb.emitFunctions(FunSetParamPacket(loadSrcCfg = 0, ckpsEngineCyl = 4, injCylDisp = .5f))
            await { vm.functionsControl.values.value != null }
            delay(150)

            val baseline = usb.writes.size
            usb.suppressAttachmentRescan()
            compose.onNodeWithTag("functions_load_source").performScrollTo().performClick()
            compose.onNodeWithTag("functions_option_4").performClick()
            val editor = requireNotNull(vm.parameterEditSession)
            assertEquals(4, vm.functionsControl.values.value?.loadSource)
            compose.runOnIdle { vm.acceptParameterEditor(editor.id, 3f) }
            delay(200)
            assertEquals(
                "writes after confirm; source=${vm.functionsControl.values.value?.loadSource}, " +
                    "session=${vm.connectionSession}, editor=${vm.parameterEditSession}, " +
                    "usbConnected=${usb.isConnected}",
                baseline + 2,
                usb.writes.size,
            )
            val command = usb.writes.last().map { it.toInt() and 0xFF }.toIntArray()
            val expected = FunSetParamPacket(
                loadSrcCfg = 4, ckpsEngineCyl = 4, injCylDisp = .75f, mafload_const = 430954.25f,
            ).pack()
            assertArrayEquals(expected, command.copyOfRange(1, 1 + expected.size))
        } finally {
            usb.close()
            compose.runOnIdle { vm.clearPageState() }
        }
    }

    @Test fun functionsNamedCommandsUseSessionAdmissionAndLiveCapabilityGates() = runBlocking {
        val vm = paramsViewModel()
        val usb = installFakeUsb(vm, firmwareOptions = 1 shl 28)
        try {
            activate(vm, R.string.params_tab_functions_title)
            usb.awaitReadRequest('p', 1)
            usb.emitFunctionName(0, "Alpha")
            usb.emitFunctionName(1, "Beta")
            usb.awaitReadRequest('n', 1)
            usb.emitFunctions(FunSetParamPacket(loadSrcCfg = 0, fnGas = 1, ckpsEngineCyl = 4, injCylDisp = .5f))
            await { vm.functionsControl.values.value != null }
            val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
            val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            vm.isSendAllowed = false

            var expectedWrites = usb.writes.size
            FunctionsNumber.entries.forEach { number ->
                val value = when (number) {
                    FunctionsNumber.LOWER -> 20f
                    FunctionsNumber.UPPER -> 120f
                    else -> number.min
                }
                compose.runOnIdle { assertTrue(vm.editFunctions(number, value)) }
                usb.awaitWriteCount(++expectedWrites)
                assertEquals('n'.code, usb.writes.last()[1].toInt() and 0xFF)
                assertEquals(value, number.value(requireNotNull(vm.functionsControl.values.value)), 0f)
            }

            val selectors = listOf(
                FunctionsChoices.Selector.GASOLINE_MAP to 1,
                FunctionsChoices.Selector.GAS_MAP to 0,
                FunctionsChoices.Selector.LOAD_SOURCE to 2,
                FunctionsChoices.Selector.MAPSEL_PETROL to 3,
                FunctionsChoices.Selector.MAPSEL_GAS to 4,
                FunctionsChoices.Selector.BAROCORR to 3,
                FunctionsChoices.Selector.VE2 to 2,
                FunctionsChoices.Selector.GAS_V_UNI to 5,
            )
            selectors.forEach { (selector, code) ->
                compose.runOnIdle { assertTrue(vm.selectFunctionsChoice(selector, code)) }
                usb.awaitWriteCount(++expectedWrites)
                assertEquals('n'.code, usb.writes.last()[1].toInt() and 0xFF)
            }
            assertEquals(3, usb.writes[expectedWrites - 4][21].toInt() and 0x0F)
            assertEquals(0x43, usb.writes[expectedWrites - 3][21].toInt() and 0xFF)
            assertEquals(5, usb.writes.last()[25].toInt() and 0xFF)
            val selected = requireNotNull(vm.functionsControl.values.value)
            assertEquals(1, selected.fnGasoline)
            assertEquals(0, selected.fnGas)
            assertEquals(2, selected.loadSource)
            assertEquals(3, selected.mapselPetrol)
            assertEquals(4, selected.mapselGas)
            assertEquals(3, selected.barocorrType)
            assertEquals(2, selected.ve2MapFunc)
            assertEquals(5, selected.gasVUni)
            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.fuelInjection }
            compose.runOnIdle {
                assertTrue(vm.selectFunctionsChoice(FunctionsChoices.Selector.LOAD_SOURCE, 4))
            }
            usb.awaitWriteCount(++expectedWrites)
            assertTrue(vm.updateFunctionsMafEngineDisplacement(3f))
            usb.awaitWriteCount(++expectedWrites)
            val expectedMaf = FunSetParamPacket(loadSrcCfg = 4, fnGasoline = 1, fnGas = 0, ckpsEngineCyl = 4,
                injCylDisp = .75f, mafload_const = 430954.25f, loadLower = 20f, loadUpper = 120f,
                mapCurveOffset = -5f, mapCurveGradient = -150f, tpsCurveOffset = -5f,
                tpsCurveGradient = -100f, map2CurveOffset = -5f, map2CurveGradient = -150f,
                gpsCurveOffset = -5f, gpsCurveGradient = -150f, fpsCurveOffset = -5f,
                fpsCurveGradient = -500f, apps1CurveOffset = -5f, apps1CurveGradient = -500f,
                mapselUni = 0x43, barocorrType = 3, ve2MapFunc = 2, gasVUni = 5).pack()
            val mafCommand = usb.writes.last().map { it.toInt() and 0xFF }.toIntArray()
            assertArrayEquals(expectedMaf, mafCommand.copyOfRange(1, 1 + expectedMaf.size))
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            queue.pending.value.forEach { queue.acknowledge(it.id) }

            val committed = requireNotNull(vm.functionsControl.values.value)
            usb.emitFirmware(options = (1 shl 28) or (1 shl 17))
            await { vm.capabilities.value.secu3T }
            val beforeUnsupported = usb.writes.size
            FunctionsNumber.entries.filter { it in setOf(
                FunctionsNumber.GPS_OFFSET, FunctionsNumber.GPS_GRADIENT,
                FunctionsNumber.FPS_OFFSET, FunctionsNumber.FPS_GRADIENT,
                FunctionsNumber.APPS_OFFSET, FunctionsNumber.APPS_GRADIENT,
            ) }.forEach { number -> compose.runOnIdle { assertFalse(vm.editFunctions(number, number.min)) } }
            delay(100)
            assertEquals(beforeUnsupported, usb.writes.size)
            assertEquals(committed, vm.functionsControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            compose.runOnIdle { vm.pausePageSession() }
            compose.runOnIdle {
                assertFalse(vm.editFunctions(FunctionsNumber.MAP_OFFSET, 1f))
                assertFalse(vm.selectFunctionsChoice(FunctionsChoices.Selector.MAPSEL_PETROL, 2))
            }
            assertEquals(beforeUnsupported, usb.writes.size)
            assertEquals(committed, vm.functionsControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertFalse(vm.isSendAllowed)
        } finally {
            usb.close()
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

    @Test fun accelerationVmRoutesTypedEditsWithoutLegacyGateAndHonorsFirmwareAndPause() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 1 shl 28).also { transport = it }
            activate(vm, R.string.params_tab_acceleration_title)
            usb.awaitReadRequest('|', 1)
            usb.emit(AccelerationParamPacket(
                injAeTpsdotThrd = 42, injAeColdaccMult = 150f, injAeDecayTime = 7,
                injAeType = 1, injAeTime = 12, injAeBallance = 50f,
                injAeMapdotThrd = 13, injXtauSThrd = -25f, injXtauFThrd = -8f,
                wallwetModel = 1,
            ))
            await { vm.accelerationControl.values.value?.tpsThreshold == 42 }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = usb.writes.size
            vm.isSendAllowed = false

            compose.runOnIdle {
                assertTrue(vm.editAcceleration(AccelerationNumber.TPS_THRESHOLD, 73))
                assertTrue(vm.editAcceleration(AccelerationNumber.COLD_MULTIPLIER, 250))
            }
            usb.awaitWriteCount(baseline + 2)
            assertEquals('|', usb.writes[baseline][1].toInt().toChar())
            assertEquals(73, usb.writes[baseline][2].toInt() and 0xff)
            assertEquals(73, usb.writes[baseline + 1][2].toInt() and 0xff)
            assertEquals(0, usb.writes[baseline + 1][3].toInt() and 0xff)
            assertEquals(0xC0, usb.writes[baseline + 1][4].toInt() and 0xff)
            assertEquals(250f, vm.accelerationControl.values.value?.coldMultiplier)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.fuelInjection }
            val beforeUnsupportedEdit = usb.writes.size
            compose.runOnIdle { assertFalse(vm.editAcceleration(AccelerationNumber.TPS_THRESHOLD, 74)) }
            delay(100)
            assertEquals(beforeUnsupportedEdit, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            usb.emitFirmware(options = 1 shl 28)
            await { vm.capabilities.value.fuelInjection }
            val committed = vm.accelerationControl.values.value
            compose.runOnIdle {
                vm.pausePageSession()
                assertFalse(vm.editAcceleration(AccelerationNumber.TPS_THRESHOLD, 75))
            }
            assertEquals(beforeUnsupportedEdit, usb.writes.size)
            assertEquals(committed, vm.accelerationControl.values.value)
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

    @Test fun ltftCommandsRouteEveryFieldWithFiGateAndSessionAdmission() = runBlocking {
        val vm = paramsViewModel()
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        var transport: FakeUsbTransport? = null
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 1 shl 28).also { transport = it }
            activate(vm, R.string.params_tab_ltft_title)
            usb.awaitReadRequest('O', 1)
            usb.emit(LtftParamPacket(mode = 0, learnClt = 12.5f, learnRpm0 = 800))
            await { vm.ltftControl.values.value != null }
            delay(150)
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            vm.isSendAllowed = false
            val baseline = usb.writes.size
            compose.runOnIdle {
                assertTrue(vm.selectLtftMode(1))
                assertTrue(vm.editLtftFloat(LtftNumber.LOWER_CLT, 12.5f))
                assertTrue(vm.editLtftFloat(LtftNumber.UPPER_CLT, 80.25f))
                assertTrue(vm.editLtftFloat(LtftNumber.UPPER_IAT, 100.25f))
                assertTrue(vm.editLtftFloat(LtftNumber.LEARNING_GRADIENT, .25f))
                assertTrue(vm.editLtftFloat(LtftNumber.ABS_GAS_PRESSURE, 100f))
                assertTrue(vm.editLtftFloat(LtftNumber.DIFF_GAS_PRESSURE, 12f))
                assertTrue(vm.editLtftFloat(LtftNumber.BOTTOM_LIMIT, -10f))
                assertTrue(vm.editLtftFloat(LtftNumber.TOP_LIMIT, 15f))
                assertTrue(vm.editLtftInt(LtftNumber.LOWER_RPM, 1234))
                assertTrue(vm.editLtftInt(LtftNumber.UPPER_RPM, 5432))
                assertTrue(vm.editLtftFloat(LtftNumber.LOWER_LOAD, 20f))
                assertTrue(vm.editLtftFloat(LtftNumber.UPPER_LOAD, 300f))
                assertTrue(vm.editLtftFloat(LtftNumber.NEGATIVE_INSENSITIVITY, 1.2f))
                assertTrue(vm.editLtftFloat(LtftNumber.POSITIVE_INSENSITIVITY, 3.4f))
                assertFalse(vm.editLtftInt(LtftNumber.LOWER_CLT, 5))
                assertFalse(vm.editLtftFloat(LtftNumber.LOWER_RPM, 5f))
                assertFalse(vm.selectLtftMode(4))
            }
            usb.awaitWriteCount(baseline + 15)
            val writes = usb.writes.drop(baseline)
            assertEquals(15, writes.size)
            // The legacy descriptor, packet offsets, signedness and fixed-point scaling stay intact.
            assertEquals('O', writes[0][1].toInt().toChar())
            assertEquals(1, writes[0][2].toInt() and 0xff)
            assertEquals(0, writes[1][3].toInt() and 0xff) // CLT 12.5 C -> 0x0032 quarter-degrees.
            assertEquals(50, writes[1][4].toInt() and 0xff)
            assertEquals(1, writes[2][5].toInt() and 0xff) // Upper CLT 80.25 C -> 0x0141.
            assertEquals(0x41, writes[2][6].toInt() and 0xff)
            assertEquals(0x01, writes[3][7].toInt() and 0xff) // Upper IAT 100.25 C -> 0x0191.
            assertEquals(0x91, writes[3][8].toInt() and 0xff)
            assertEquals(64, writes[4][9].toInt() and 0xff) // Learning gradient .25 -> 64/256.
            assertEquals(0x19, writes[5][10].toInt() and 0xff) // Absolute pressure 100 -> 0x1900 / 64.
            assertEquals(0, writes[5][11].toInt() and 0xff)
            assertEquals(0x03, writes[6][12].toInt() and 0xff) // Differential pressure 12 -> 0x0300 / 64.
            assertEquals(0, writes[6][13].toInt() and 0xff)
            assertEquals(0xCD, writes[7][14].toInt() and 0xff) // Signed bottom limit -10%.
            assertEquals(0x4D, writes[8][15].toInt() and 0xff) // Top limit 15%.
            assertEquals(0x04, writes[9][16].toInt() and 0xff) // Lower RPM 1234 -> 0x04D2.
            assertEquals(0xD2, writes[9][17].toInt() and 0xff)
            assertEquals(0x15, writes[10][18].toInt() and 0xff) // Upper RPM 5432 -> 0x1538.
            assertEquals(0x38, writes[10][19].toInt() and 0xff)
            assertEquals(0x05, writes[11][20].toInt() and 0xff) // Lower load 20 -> 1280/64.
            assertEquals(0, writes[11][21].toInt() and 0xff)
            assertEquals(0x4B, writes[12][22].toInt() and 0xff) // Upper load 300 -> 19200/64.
            assertEquals(0, writes[12][23].toInt() and 0xff)
            assertEquals(6, writes[13][24].toInt() and 0xff) // Negative deadband 1.2% -> 6/512.
            assertEquals(17, writes[14][25].toInt() and 0xff) // Positive deadband 3.4% -> 17/512.
            assertEquals(1, vm.ltftControl.values.value?.mode)
            assertEquals(LtftValues(1, 12.5f, 80.25f, 100.25f, .25f, 100f, 12f,
                -10f, 15f, 1234, 5432, 20f, 300f, 1.2f, 3.4f), vm.ltftControl.values.value)
            assertEquals(false, vm.isSendAllowed)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            queue.pending.value.forEach { queue.acknowledge(it.id) }

            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && !vm.capabilities.value.fuelInjection }
            val unsupportedBaseline = usb.writes.size
            compose.runOnIdle {
                assertFalse(vm.selectLtftMode(0))
                assertFalse(vm.editLtftFloat(LtftNumber.LOWER_CLT, 20f))
            }
            delay(100)
            assertEquals(unsupportedBaseline, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            usb.emitFirmware(options = 1 shl 28)
            await { vm.capabilities.value.fuelInjection }
            compose.runOnIdle { vm.pausePageSession() }
            val pausedBaseline = usb.writes.size
            compose.runOnIdle { assertFalse(vm.editLtftFloat(LtftNumber.LOWER_CLT, 20f)) }
            delay(100)
            assertEquals(pausedBaseline, usb.writes.size)
            assertEquals(12.5f, vm.ltftControl.values.value?.lowerClt)
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

    @Test fun dbwCommandsWriteTenFieldsWithFamilyGateAndSessionAdmission() = runBlocking {
        val vm = paramsViewModel()
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        var transport: FakeUsbTransport? = null
        var dbwStep = "install fake USB"
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 0).also { transport = it }
            dbwStep = "wait for initial DBW read"
            activate(vm, R.string.params_tab_el_throttle_title)
            usb.awaitReadRequest('P', 1)
            dbwStep = "receive initial DBW response"
            usb.emitDbw(DbwParamPacket(.1f, .2f, .3f, 40f, 50f, .12f, 6f, 7f, 8f, 9f), 47.5f)
            await { vm.dbwControl.values.value?.homePosition == 47.5f }
            delay(150)
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            vm.isSendAllowed = false
            val baseline = usb.writes.size

            dbwStep = "send all ten DBW edits"
            compose.runOnIdle {
                assertTrue(vm.editDbw(DbwNumber.ETC_P, .125f))
                assertTrue(vm.editDbw(DbwNumber.ETC_I, .25f))
                assertTrue(vm.editDbw(DbwNumber.ETC_D, .5f))
                assertTrue(vm.editDbw(DbwNumber.NMAX_DUTY, 45.5f))
                assertTrue(vm.editDbw(DbwNumber.PMAX_DUTY, 55.5f))
                assertTrue(vm.editDbw(DbwNumber.PID_PERIOD, .15f))
                assertTrue(vm.editDbw(DbwNumber.FRICTION_TORQUE_OPEN, 6.4f))
                assertTrue(vm.editDbw(DbwNumber.FRICTION_TORQUE_CLOSE, 7.5f))
                assertTrue(vm.editDbw(DbwNumber.FRICTION_THRD, 12f))
                assertTrue(vm.editDbw(DbwNumber.FRICTORQ_IDLE_ADD_MAX, 10.5f))
                assertFalse(vm.editDbw(DbwNumber.ETC_P, Float.NaN))
            }
            dbwStep = "verify the DBW writes"
            usb.awaitWriteCount(baseline + 10)
            val writes = usb.writes.drop(baseline)
            assertEquals(10, writes.size)
            writes.forEach { assertEquals('P', it[1].toInt().toChar()) }
            // Each immutable frame carries exactly the 14 writable bytes; Home position stays receive-only.
            assertArrayEquals(intArrayOf(0x02, 0, 0x04, 0, 0x08, 0, 91, 111, 15, 102, 120, 12, 0x02, 0xA0), writes.last().sliceArray(2..15).map { it.toInt() and 0xff }.toIntArray())
            assertEquals(DbwValues(.125f, .25f, .5f, 45.5f, 55.5f, .15f, 6.4f, 7.5f, 12f, 10.5f, 47.5f), vm.dbwControl.values.value)
            assertFalse(vm.isSendAllowed)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            queue.pending.value.forEach { queue.acknowledge(it.id) }

            dbwStep = "refresh DBW values"
            val writesBeforeRefresh = usb.writes.size
            val dbwWritesBeforeRefresh = usb.writes.count { it.size > 1 && it[1].toInt().toChar() == 'P' }
            compose.runOnIdle { vm.refreshPage(ParameterPage.EL_THROTTLE) }
            usb.awaitReadRequest('P', 2)
            await { vm.dbwControl.values.value == null }
            usb.emitDbw(DbwParamPacket(.125f, .25f, .5f, 45.5f, 55.5f, .15f, 6.4f, 7.5f, 12f, 10.5f), 48.5f)
            await { vm.dbwControl.values.value?.homePosition == 48.5f }
            assertTrue(usb.writes.size > writesBeforeRefresh) // Refresh sends the request packet.
            assertEquals(dbwWritesBeforeRefresh, usb.writes.count { it.size > 1 && it[1].toInt().toChar() == 'P' })

            dbwStep = "reject edits after SECU3T firmware update"
            usb.emitFirmware(options = 1 shl 17)
            await { vm.capabilities.value.known && vm.capabilities.value.secu3T }
            val unsupportedBaseline = usb.writes.size
            compose.runOnIdle { assertFalse(vm.editDbw(DbwNumber.ETC_I, 1f)) }
            delay(100)
            assertEquals(unsupportedBaseline, usb.writes.size)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            dbwStep = "reject edits after page session pause"
            usb.emitFirmware(options = 0)
            await { vm.capabilities.value.known && vm.capabilities.value.dbw }
            compose.runOnIdle { vm.pausePageSession() }
            val pausedBaseline = usb.writes.size
            val pausedValues = vm.dbwControl.values.value
            compose.runOnIdle { assertFalse(vm.editDbw(DbwNumber.ETC_I, 1f)) }
            delay(100)
            assertEquals(pausedBaseline, usb.writes.size)
            assertEquals(pausedValues, vm.dbwControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
        } catch (failure: TimeoutCancellationException) {
            throw AssertionError("DBW fake-USB test timed out while trying to $dbwStep", failure)
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

    @Test fun idlingCommandsPreservePositiveAndNegativeWireSlotsAndHonorPause() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 1 shl 28).also { transport = it }
            activate(vm, R.string.params_tab_idling_title)
            usb.awaitReadRequest('l', 1)
            usb.emit(IdlingParamPacket(
                idlFlags = 0x80,
                idlRegP0 = .125f,
                idlRegP1 = .25f,
                idlRegI0 = .375f,
                idlRegI1 = .5f,
                coefThrd1 = 1f,
                coefThrd2 = 1f,
            ))
            await { vm.idlingControl.values.value != null }
            assertEquals(.125f, vm.idlingControl.values.value?.proportionalNegative)
            assertEquals(.25f, vm.idlingControl.values.value?.proportionalPositive)
            assertEquals(.375f, vm.idlingControl.values.value?.integralNegative)
            assertEquals(.5f, vm.idlingControl.values.value?.integralPositive)

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = usb.writes.size
            vm.isSendAllowed = false

            assertTrue(vm.editIdlingFloat(org.secu3.android.ui.parameters.pages.compose.IdlingNumber.P0, 1.25f))
            assertTrue(vm.editIdlingFloat(org.secu3.android.ui.parameters.pages.compose.IdlingNumber.P1, .75f))
            assertTrue(vm.editIdlingFloat(org.secu3.android.ui.parameters.pages.compose.IdlingNumber.I0, 1.5f))
            assertTrue(vm.editIdlingFloat(org.secu3.android.ui.parameters.pages.compose.IdlingNumber.I1, 2f))
            usb.awaitWriteCount(baseline + 4)
            assertEquals(1, usb.writes[baseline][21].toInt() and 0xff) // P(+) -> IDLREG P1.
            assertEquals(64, usb.writes[baseline][22].toInt() and 0xff)
            assertEquals(0, usb.writes[baseline + 1][19].toInt() and 0xff) // P(-) -> IDLREG P0.
            assertEquals(192, usb.writes[baseline + 1][20].toInt() and 0xff)
            assertEquals(1, usb.writes[baseline + 2][25].toInt() and 0xff) // I(+) -> IDLREG I1.
            assertEquals(128, usb.writes[baseline + 2][26].toInt() and 0xff)
            assertEquals(2, usb.writes[baseline + 3][23].toInt() and 0xff) // I(-) -> IDLREG I0.
            assertEquals(0, usb.writes[baseline + 3][24].toInt() and 0xff)
            assertEquals(1.25f, vm.idlingControl.values.value?.proportionalPositive)
            assertEquals(.75f, vm.idlingControl.values.value?.proportionalNegative)

            val numbers = org.secu3.android.ui.parameters.pages.compose.IdlingNumber.entries
            val expectedNumbers = listOf(
                2.5f, 3.5f, 12f, -4f, 1500f, 150f, 90.25f, 40f, 150f, 1.25f, 1.5f, .75f,
                2f, 1.5f, 1.75f, 550f, 50f, 10f, 90f, 45f, 2.25f, 2.5f, 3.5f, 55f,
            )
            val numericStart = usb.writes.size
            numbers.zip(expectedNumbers).forEach { (number, value) ->
                compose.runOnIdle {
                    if (number.integer) vm.editIdlingInt(number, value.toInt())
                    else vm.editIdlingFloat(number, value)
                }
            }
            usb.awaitWriteCount(numericStart + numbers.size)
            assertEquals(expectedNumbers, numbers.map { it.value(requireNotNull(vm.idlingControl.values.value)) })

            val flags = org.secu3.android.ui.parameters.pages.compose.IdlingFlag.entries
            val flagStart = usb.writes.size
            flags.forEach { flag -> compose.runOnIdle { vm.setIdlingFlag(flag, true) } }
            usb.awaitWriteCount(flagStart + flags.size)
            assertEquals(List(flags.size) { true }, flags.map { it.value(requireNotNull(vm.idlingControl.values.value)) })
            assertEquals(0x80, usb.writes.last()[2].toInt() and 0x80)

            val beforeStaleIacAdd = usb.writes.size
            assertFalse(vm.editIdlingFloat(org.secu3.android.ui.parameters.pages.compose.IdlingNumber.IAC_ADD, 35f))
            assertEquals(beforeStaleIacAdd, usb.writes.size)
            assertTrue(vm.setIdlingFlag(org.secu3.android.ui.parameters.pages.compose.IdlingFlag.THRASSMAP, false))
            usb.awaitWriteCount(beforeStaleIacAdd + 1)
            assertTrue(vm.editIdlingFloat(org.secu3.android.ui.parameters.pages.compose.IdlingNumber.IAC_ADD, 35f))
            usb.awaitWriteCount(beforeStaleIacAdd + 2)
            assertEquals(35f, vm.idlingControl.values.value?.iacAddAfterExit)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val afterWrites = usb.writes.size
            val committed = vm.idlingControl.values.value
            compose.runOnIdle {
                vm.pausePageSession()
                vm.editIdlingFloat(org.secu3.android.ui.parameters.pages.compose.IdlingNumber.P0, 1.75f)
            }
            delay(100)
            assertEquals(afterWrites, usb.writes.size)
            assertEquals(committed, vm.idlingControl.values.value)
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

    @Test fun knockCommandsUseSessionAdmissionDespiteLegacyGateAndRejectAfterPause() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm).also { transport = it }
            activate(vm, R.string.params_tab_knock_title)
            usb.awaitReadRequest('w', 1)
            usb.emit(KnockParamPacket(
                useKnockChannel = 2, bpfFrequency = 63, kWndBeginAngle = -3.75f,
                intTimeCost = 31, recoveryDelay = 12, selectedChanels = 0x05,
            ))
            await { vm.knockControl.values.value?.useKnockChannel == 2 }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            vm.isSendAllowed = false
            val baseline = usb.writes.size

            assertTrue(vm.editKnockDelay(18))
            assertTrue(vm.selectKnock(KnockSelector.BPF, 1))
            assertTrue(vm.selectKnockChannel(7, true))
            usb.awaitWriteCount(baseline + 3)
            assertEquals('w', usb.writes[baseline][1].toInt().toChar())
            assertEquals('w', usb.writes[baseline + 1][1].toInt().toChar())
            assertEquals('w', usb.writes[baseline + 2][1].toInt().toChar())
            assertEquals(18, vm.knockControl.values.value?.recoveryDelay)
            assertEquals(1, vm.knockControl.values.value?.bpfFrequencyCode)
            assertTrue(vm.knockControl.values.value!!.isChannelSelected(7))
            assertEquals(2, vm.knockControl.values.value?.useKnockChannel)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val committed = vm.knockControl.values.value
            val writesBeforePause = usb.writes.size
            compose.runOnIdle {
                vm.pausePageSession()
                assertFalse(vm.editKnockFloat(KnockNumber.BEGIN, 4f))
            }
            assertEquals(writesBeforePause, usb.writes.size)
            assertEquals(committed, vm.knockControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertFalse(vm.isSendAllowed)
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

    @Test fun anglesCommandsUseLiveSessionSnapshotsAndRejectStaleOrPausedEdits() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm).also { transport = it }
            activate(vm, R.string.params_tab_angles_title)
            usb.awaitReadRequest('m', 1)
            usb.emit(AnglesParamPacket(maxAngle = 4f))
            await { vm.anglesControl.values.value?.maxAngle == 4f }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = usb.writes.size
            vm.isSendAllowed = false

            compose.runOnIdle { vm.editAngles(AnglesNumber.MAX, 5.5f) }
            usb.awaitWriteCount(baseline + 1)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle { vm.setAnglesFlag(org.secu3.android.ui.parameters.pages.compose.AnglesFlag.WORKING_MAP, true) }
            usb.awaitWriteCount(baseline + 2)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle { vm.setAnglesFlag(org.secu3.android.ui.parameters.pages.compose.AnglesFlag.ZERO, true) }
            usb.awaitWriteCount(baseline + 3)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertArrayEquals(
                byteArrayOf(0x21, 'm'.code.toByte(), 0, 0xB0.toByte()) + ByteArray(12) + byteArrayOf(0xAE.toByte(), 0xBE.toByte(), 0x0D),
                usb.writes[baseline],
            )
            assertArrayEquals(
                byteArrayOf(0x21, 'm'.code.toByte(), 0, 0xB0.toByte()) + ByteArray(8) + byteArrayOf(0, 1, 0, 0, 0xB2.toByte(), 0xBF.toByte(), 0x0D),
                usb.writes[baseline + 1],
            )
            assertArrayEquals(
                byteArrayOf(0x21, 'm'.code.toByte(), 0, 0xB0.toByte()) + ByteArray(8) + byteArrayOf(1, 1, 0, 0, 0xB7.toByte(), 0xC0.toByte(), 0x0D),
                usb.writes[baseline + 2],
            )
            assertEquals(5.5f, vm.anglesControl.values.value?.maxAngle)
            assertEquals(true, vm.anglesControl.values.value?.alwaysUseIgnitionMap)
            assertEquals(1, vm.anglesControl.values.value?.zeroAdvAngle)
            val afterAcceptedWrites = usb.writes.size
            val committed = vm.anglesControl.values.value
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            compose.runOnIdle { vm.editAngles(AnglesNumber.OCTANE, 4f) }
            assertEquals(afterAcceptedWrites, usb.writes.size)
            assertEquals(committed, vm.anglesControl.values.value)
            assertFalse(queue.pending.value.any { it.textRes == R.string.packet_sent })

            compose.runOnIdle {
                vm.pausePageSession()
                vm.editAngles(AnglesNumber.MAX, 6f)
                vm.setAnglesFlag(org.secu3.android.ui.parameters.pages.compose.AnglesFlag.MANUAL_CORRECTION, true)
            }
            delay(100)
            assertEquals(afterAcceptedWrites, usb.writes.size)
            assertEquals(committed, vm.anglesControl.values.value)
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

    @Test fun carburSelectorCommandsWriteExactFuelAndIgnitionWireCodes() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm, firmwareOptions = 1 shl 28).also { transport = it }
            activate(vm, R.string.params_tab_fuel_cutoff_title)
            usb.awaitReadRequest('k', 1)
            usb.emit(CarburParamPacket(fuelcut_uni = 6, igncut_uni = 41, carbInvers = 2))
            await { vm.carburControl.values.value?.fuelCutOutput == 6 }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = usb.writes.size
            vm.isSendAllowed = false

            val cases = listOf(
                Triple(false, 0, 0),
                Triple(false, 5, 5),
                Triple(false, 6, 15),
                Triple(true, 0, 0),
                Triple(true, 5, 5),
                Triple(true, 6, 15),
            )
            cases.forEachIndexed { index, (ignition, selection, expected) ->
                compose.runOnIdle { vm.selectCarburOutput(ignition, selection) }
                usb.awaitWriteCount(baseline + index + 1)
                val frame = usb.writes[baseline + index]
                assertEquals('k', frame[1].toInt().toChar())
                assertEquals(expected, frame[if (ignition) 25 else 24].toInt() and 0xff)
            }
            assertEquals(15, vm.carburControl.values.value?.fuelCutOutput)
            assertEquals(15, vm.carburControl.values.value?.ignitionCutOutput)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })

            val numericEdits = listOf(
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.IDLE_LOW, 1001, intArrayOf(0x03, 0xE9)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.IDLE_HIGH, 1002, intArrayOf(0x03, 0xEA)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.IDLE_LOW_GAS, 1003, intArrayOf(0x03, 0xEB)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.IDLE_HIGH_GAS, 1004, intArrayOf(0x03, 0xEC)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.REV_LOW, 1005, intArrayOf(0x03, 0xED)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.REV_HIGH, 1006, intArrayOf(0x03, 0xEE)),
            )
            val numericStart = usb.writes.size
            numericEdits.forEach { (number, value, _) -> compose.runOnIdle { vm.editCarburInt(number, value) } }
            usb.awaitWriteCount(numericStart + numericEdits.size)
            val integerOffsets = intArrayOf(1, 3, 8, 10, 19, 21)
            numericEdits.forEachIndexed { index, (_, _, expected) ->
                val frame = usb.writes[numericStart + index]
                assertEquals('k', frame[1].toInt().toChar())
                assertEquals(expected[0], frame[integerOffsets[index] + 1].toInt() and 0xff)
                assertEquals(expected[1], frame[integerOffsets[index] + 2].toInt() and 0xff)
            }
            val floatEdits = listOf(
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.CUTOFF_DELAY, .75f, 12 to intArrayOf(75)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.VALVE_PRESSURE, 6.25f, 6 to intArrayOf(1, 144)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.TPS, 31.5f, 13 to intArrayOf(7, 224)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.MAP, 88.5f, 15 to intArrayOf(22, 32)),
                Triple(org.secu3.android.ui.parameters.pages.compose.CarburNumber.CTS, -10.5f, 17 to intArrayOf(255, 214)),
            )
            val floatStart = usb.writes.size
            floatEdits.forEach { (number, value, _) -> compose.runOnIdle { vm.editCarburFloat(number, value) } }
            usb.awaitWriteCount(floatStart + floatEdits.size)
            floatEdits.forEachIndexed { index, (_, _, expected) ->
                val frame = usb.writes[floatStart + index]
                assertEquals('k', frame[1].toInt().toChar())
                expected.second.forEachIndexed { byte, value ->
                    assertEquals(value, frame[expected.first + byte + 1].toInt() and 0xff)
                }
            }

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val beforePause = usb.writes.size
            val committedIdle = vm.carburControl.values.value?.idleLowRpm
            compose.runOnIdle {
                vm.pausePageSession()
                vm.editCarburInt(org.secu3.android.ui.parameters.pages.compose.CarburNumber.IDLE_LOW, 2000)
            }
            assertEquals(beforePause, usb.writes.size)
            assertEquals(committedIdle, vm.carburControl.values.value?.idleLowRpm)
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

    @Test fun temperatureCommandsUseCurrentSessionAndPreserveRawFieldsAcrossWrites() = runBlocking {
        val vm = paramsViewModel()
        var transport: FakeUsbTransport? = null
        val queueField = ParamsViewModel::class.java.getDeclaredField("uiMessageQueue").apply { isAccessible = true }
        val queue = queueField.get(vm) as org.secu3.android.ui.messages.UiMessageQueue
        val originalActiveScope = queue.activeScope.value
        try {
            val usb = installFakeUsb(vm, firmwareOptions = (1 shl 28) or (1 shl 8) or (1 shl 16)).also { transport = it }
            activate(vm, R.string.params_tab_temperature_title)
            usb.awaitReadRequest('j', 1)
            usb.emit(TemperatureParamPacket(tmpFlags = 0x80, ventOn = 95f, ventOff = 90f, ventPwmFrq = 100, ventTmr = 1.27f))
            await { vm.temperatureControl.values.value?.fanOn == 95f }

            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.waitForIdle()
            queue.onActiveScopeChanged(PARAMETERS_ROUTE)
            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val baseline = usb.writes.size
            vm.isSendAllowed = false

            assertTrue(vm.editTemperatureFloat(TemperatureNumber.FAN_ON, 100f))
            usb.awaitWriteCount(baseline + 1)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertArrayEquals(
                temperatureFrame(intArrayOf('j'.code, 0x80, 0x01, 0x90, 0x01, 0x68, 0x14, 0x7B, 0, 0, 0, 0, 0, 0, 0, 0x7F)),
                usb.writes[baseline],
            )

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            assertTrue(vm.setTemperatureFlag(TemperatureFlag.COOLANT_SENSOR, true))
            usb.awaitWriteCount(baseline + 2)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertArrayEquals(
                temperatureFrame(intArrayOf('j'.code, 0x81, 0x01, 0x90, 0x01, 0x68, 0x14, 0x7B, 0, 0, 0, 0, 0, 0, 0, 0x7F)),
                usb.writes[baseline + 1],
            )

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            assertTrue(vm.editTemperatureInt(TemperatureNumber.FAN_TIMER, 30))
            usb.awaitWriteCount(baseline + 3)
            assertTrue(queue.pending.value.any { it.textRes == R.string.packet_sent })
            assertArrayEquals(
                temperatureFrame(intArrayOf('j'.code, 0x81, 0x01, 0x90, 0x01, 0x68, 0x14, 0x7B, 0, 0, 0, 0, 0, 0, 0x0B, 0xB8)),
                usb.writes[baseline + 2],
            )
            assertEquals(100f, vm.temperatureControl.values.value?.fanOn)
            assertEquals(90f, vm.temperatureControl.values.value?.fanOff)
            assertEquals(30f, vm.temperatureControl.values.value?.fanTimer)
            assertEquals(false, vm.isSendAllowed)

            queue.pending.value.forEach { queue.acknowledge(it.id) }
            val committed = vm.temperatureControl.values.value
            compose.runOnIdle {
                vm.pausePageSession()
                assertFalse(vm.editTemperatureFloat(TemperatureNumber.FAN_ON, 105f))
                assertFalse(vm.setTemperatureFlag(TemperatureFlag.COOLANT_SENSOR, false))
            }
            delay(100)
            assertEquals(baseline + 3, usb.writes.size)
            assertEquals(committed, vm.temperatureControl.values.value)
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

    private fun temperatureFrame(packet: IntArray): ByteArray {
        val frame = intArrayOf(0x21) + packet
        val checksum = PacketReceiver.calculateChecksum(frame.copyOfRange(2, frame.size))
        return (PacketReceiver.escapeTx(frame + checksum[1] + checksum[0]) + 0x0D)
            .map(Int::toByte)
            .toByteArray()
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
            await { vm.anglesControl.values.value != null }
            delay(150)

            compose.runOnIdle {
                vm.editAngles(AnglesNumber.MAX, 6f)
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

    private fun setUniOutFlag(bytes: IntArray, output: Int, bit: Int, value: Boolean) {
        val index = (output - 1) * 11
        val mask = 1 shl bit
        bytes[index] = if (value) bytes[index] or mask else bytes[index] and mask.inv()
    }
    private fun setUniOutCondition(bytes: IntArray, output: Int, slot: Int, code: Int) {
        bytes[(output - 1) * 11 + slot] = code
    }
    private fun setUniOutThreshold(bytes: IntArray, output: Int, slot: Int, on: Boolean, value: Float, scale: Float) {
        val word = (value * scale).roundToInt() and 0xFFFF
        val index = (output - 1) * 11 + if (slot == 1) if (on) 3 else 5 else if (on) 7 else 9
        bytes[index] = word ushr 8
        bytes[index + 1] = word and 0xFF
    }
    private fun setUniOutLogic(bytes: IntArray, output: Int, code: Int) {
        val index = (output - 1) * 11
        bytes[index] = bytes[index] and 0x0F or (code shl 4)
    }
    private fun unescapeUniversalOutputWrite(frame: ByteArray): IntArray {
        val decoded = ArrayList<Int>(frame.size)
        var index = 2
        while (index < frame.lastIndex) {
            val byte = frame[index++].toInt() and 0xFF
            if (byte == 0x0A && index < frame.lastIndex) {
                val escape = frame[index++].toInt() and 0xFF
                decoded += when (escape) { 0x83 -> 0x0D; 0x84 -> 0x0A; else -> escape }
            } else decoded += byte
        }
        return decoded.toIntArray()
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

    private fun mountFunctionsPage(titleRes: Int, vm: ParamsViewModel) {
        // Replace NavHost so the test owns one page root; the fake USB helper suppresses physical-device rescan.
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContentView(ComposeView(activity).apply {
                setContent {
                    val capabilities by vm.capabilities.collectAsStateWithLifecycle()
                    SecuComposeTheme {
                        ParameterPageContent(
                            requireNotNull(ParameterPageCatalog().pageForTitle(titleRes)),
                            titleRes, vm, capabilities, true, { true }, vm::openParameterEditor,
                        )
                    }
                        }
            })
        }
        compose.waitForIdle()
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
        val isConnected get() = manager.isUsbConnected

        fun suppressAttachmentRescan() {
            Secu3ConnectionManager::class.java.getDeclaredField("activeUsbAttachment")
                .apply { isAccessible = true }.set(manager, null)
        }

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

        suspend fun emitFunctionName(index: Int, name: String) {
            val encoded = name.toByteArray(java.nio.charset.Charset.forName("IBM866"))
                .map { it.toInt() and 0xFF }.toIntArray()
            require(encoded.size <= 16)
            val input = intArrayOf('@'.code, 'p'.code, 2, index) + encoded +
                IntArray(16 - encoded.size) + intArrayOf(0, 0)
            attemptPackets.emit(UsbAttemptPacket(1L, RawPacket(input)))
        }

        suspend fun emitFunctions(packet: FunSetParamPacket) {
            val writePayload = packet.pack()
            val input = (intArrayOf('@'.code) + writePayload.copyOfRange(0, 32) + intArrayOf(0x01, 0x90) +
                writePayload.copyOfRange(32, writePayload.size) + intArrayOf(0x01, 0x90) + intArrayOf(0, 0)).also {
                it[26] = packet.ckpsEngineCyl
            }
            attemptPackets.emit(UsbAttemptPacket(1L, RawPacket(input)))
        }

        suspend fun emitDbw(packet: DbwParamPacket, homePosition: Float, tail: IntArray = intArrayOf()) {
            val homeWord = (homePosition * 64).roundToInt()
            val input = intArrayOf('@'.code) + packet.pack() + intArrayOf(homeWord shr 8, homeWord and 0xff) + tail + intArrayOf(0, 0)
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
