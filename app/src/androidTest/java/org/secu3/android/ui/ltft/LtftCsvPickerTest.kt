package org.secu3.android.ui.ltft

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.domain.ltft.LtftMapExport
import org.secu3.android.ui.compose.SecuComposeTheme
import java.io.File

class LtftCsvPickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cancellationClearsTheRequestAndRestoredPickerWritesTheOriginalTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val registry = LtftTestPickerRegistry(context)
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        val restoration = StateRestorationTester(compose)
        val export = LtftMapExport()
        val rpm = List(16) { 500 * (it + 1) }
        var corrections by mutableStateOf(List(256) { 1.5f })
        var writes = 0
        var writeResult by mutableStateOf<Boolean?>(null)
        restoration.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                SecuComposeTheme {
                    val scope = rememberCoroutineScope()
                    val launchCsv = rememberLtftCsvLauncher(
                        onDocumentCreated = { uri, csv ->
                            writes++
                            scope.launch { writeResult = export.writeCsv(context.contentResolver, uri, csv) }
                        },
                        onLaunchFailed = { error("Unexpected picker launch failure") },
                    )
                    Button(onClick = { launchCsv("LTFT-1.csv", export.csv(corrections, rpm, '.')) }) {
                        Text("Export")
                    }
                }
            }
        }

        val exportButton = compose.onNodeWithText("Export")
        exportButton.performClick()
        compose.runOnIdle {
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, registry.intent.action)
            assertEquals("text/csv", registry.intent.type)
            assertEquals("LTFT-1.csv", registry.intent.getStringExtra(Intent.EXTRA_TITLE))
            registry.respond(uri = null)
            assertEquals(0, writes)
            corrections = List(256) { 2.5f }
        }

        val expected = export.csv(List(256) { 2.5f }, rpm, '.')
        exportButton.performClick()
        compose.runOnIdle {
            assertEquals(2, registry.launches)
            corrections = List(256) { 3.5f }
        }
        restoration.emulateSavedInstanceStateRestore()

        val destination = File.createTempFile("ltft-picker-", ".csv", context.cacheDir)
        try {
            compose.runOnIdle { registry.respond(Uri.fromFile(destination)) }
            compose.waitUntil(timeoutMillis = 5_000) { writeResult != null }
            assertEquals(true, writeResult)
            assertEquals(1, writes)
            assertArrayEquals(expected.toByteArray(Charsets.UTF_8), destination.readBytes())
        } finally {
            destination.delete()
        }
    }
}

internal class LtftTestPickerRegistry(private val context: Context) : ActivityResultRegistry() {
    private var requestCode = 0
    var launches = 0
        private set
    lateinit var intent: Intent
        private set

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        this.requestCode = requestCode
        launches++
        intent = contract.createIntent(context, input)
    }

    fun respond(uri: Uri?) {
        if (uri == null) {
            dispatchResult(requestCode, Activity.RESULT_CANCELED, null)
        } else {
            dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(uri))
        }
    }
}
