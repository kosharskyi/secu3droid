package org.secu3.android.ui.ltft

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.domain.ltft.LtftMapsState
import org.secu3.android.domain.ltft.LtftVeImage
import org.secu3.android.domain.ltft.LtftWriteState
import org.secu3.android.ui.compose.SecuComposeTheme
import java.io.File

class LtftVeFileActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun keepsTheOriginalLtftSnapshotAndChoiceAcrossPickersAndRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val registry = LtftTestPickerRegistry(context)
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        val restoration = StateRestorationTester(compose)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val messages = mutableListOf<Int>()
        val workflow = LtftVeFileWorkflow(LtftVeImage(), scope) { messages.add(it) }
        val input = File.createTempFile("ltft-ve-input-", ".bin", context.cacheDir)
        val output = File.createTempFile("ltft-ve-output-", ".bin", context.cacheDir)
        val original = eepromFixture()
        var tables by mutableStateOf(listOf(List(256) { 10f }, List(256) { -20f }))
        input.writeBytes(original)

        try {
            restoration.setContent {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    SecuComposeTheme {
                        val applyToVe = rememberLtftVeFileAction(workflow, context.contentResolver)
                        LtftMapsScreen(
                            state = LtftMapsState.Ready(
                                tables = tables,
                                rpmAxis = List(16) { 500 * (it + 1) },
                                loadAxis = List(16) { 20f + 5 * it },
                                isLoadPressure = true,
                            ),
                            connectionState = Connected,
                            writeState = LtftWriteState.Idle,
                            onNavigateUp = {},
                            onRefresh = {},
                            onReset = {},
                            onSave = {},
                            onCopy = { _, _, _ -> },
                            onExportCsv = { _, _, _, _ -> },
                            onApplyToVe = applyToVe,
                        )
                    }
                }
            }

            openFromMenu()
            compose.runOnIdle {
                assertEquals(Intent.ACTION_OPEN_DOCUMENT, registry.intent.action)
                registry.respond(null)
                assertEquals(LtftVeFileWorkflow.State.Idle, workflow.state.value)
            }

            openFromMenu()
            compose.runOnIdle { tables = List(2) { List(256) { 0f } } }
            restoration.emulateSavedInstanceStateRestore()
            compose.runOnIdle { registry.respond(Uri.fromFile(input)) }
            compose.waitUntil(5_000) { workflow.state.value is LtftVeFileWorkflow.State.Choosing }
            text(R.string.ltft_ve_eeprom_set).assertExists()
            text(R.string.ltft_ve_average).performClick()
            text(R.string.ltft_ve_save_copy).performClick()
            compose.runOnIdle {
                assertEquals(Intent.ACTION_CREATE_DOCUMENT, registry.intent.action)
                assertEquals("LTFT-VE-EEPROM.bin", registry.intent.getStringExtra(Intent.EXTRA_TITLE))
                registry.respond(null)
            }

            text(R.string.ltft_ve_average).assertIsSelected()
            text(R.string.ltft_ve_save_copy).performClick()
            restoration.emulateSavedInstanceStateRestore()
            compose.runOnIdle { registry.respond(Uri.fromFile(output)) }
            compose.waitUntil(5_000) { workflow.state.value == LtftVeFileWorkflow.State.Idle }

            compose.runOnIdle { assertEquals(listOf(R.string.ltft_ve_saved), messages) }
            assertArrayEquals(original, input.readBytes())
            assertArrayEquals(eepromFixture(corrected = true), output.readBytes())
        } finally {
            scope.cancel()
            input.delete()
            output.delete()
        }
    }

    private fun openFromMenu() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithContentDescription(context.getString(R.string.ltft_maps_actions)).performClick()
        text(R.string.ltft_ve_apply).performClick()
    }

    private fun text(id: Int) = compose.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id),
    )

    private fun eepromFixture(corrected: Boolean = false): ByteArray = ByteArray(4096).apply {
        "1284".toByteArray().copyInto(this, 4092)
        "Moto test".toByteArray().copyInto(this, 587)
        val pair = if (corrected) byteArrayOf(0xcd.toByte(), 0xd3.toByte(), 0x3c) else byteArrayOf(0, 4, 0x40)
        repeat(128) { pair.copyInto(this, 907 + 3 * it) }
        // Params CRC and table CRC for the Manager layout and the two fixed payloads.
        this[567] = 0xcb.toByte()
        this[568] = 0xa7.toByte()
        this[3418] = if (corrected) 0x78 else 0x1f
        this[3419] = if (corrected) 0xb7.toByte() else 0x5b
    }
}
