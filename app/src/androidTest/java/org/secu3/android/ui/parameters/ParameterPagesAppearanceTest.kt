package org.secu3.android.ui.parameters

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.secu3.android.ui.compose.SecuComposeTheme

internal data class ParameterAppearanceFixture(
    val name: String,
    val content: @Composable () -> Unit,
)

class ParameterPagesAppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allNineteenParameterPagesRenderInUkrainianLightAndDarkThemes() {
        val fixtures = basicParameterAppearanceFixtures() + advancedParameterAppearanceFixtures()
        assertTrue("All 19 parameter pages have appearance fixtures", fixtures.size == 19)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ukrainianConfiguration = Configuration(context.resources.configuration).apply {
            setLocale(Locale("uk"))
        }
        val ukrainianResources = context.createConfigurationContext(ukrainianConfiguration).resources
        val ukrainianContext = context.createConfigurationContext(ukrainianConfiguration)
        val fixtureIndex = mutableIntStateOf(0)
        val darkTheme = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides ukrainianContext,
                LocalConfiguration provides ukrainianConfiguration,
                LocalResources provides ukrainianResources,
            ) {
                SecuComposeTheme(darkTheme = darkTheme.value) {
                    fixtures[fixtureIndex.intValue].content()
                }
            }
        }

        fixtures.forEachIndexed { index, fixture ->
            listOf(false, true).forEach { dark ->
                compose.runOnIdle {
                    fixtureIndex.intValue = index
                    darkTheme.value = dark
                }
                compose.waitForIdle()
                val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
                assertTrue("${fixture.name} renders in ${if (dark) "dark" else "light"} theme", bitmap.width > 0 && bitmap.height > 0)
                saveDesignScreenshotIfRequested(bitmap, fixture.name, dark)
                try {
                    compose.onNodeWithTag("parameter-page-root").assertIsDisplayed()
                } catch (failure: AssertionError) {
                    throw AssertionError(
                        "${fixture.name} page root should render in ${if (dark) "dark" else "light"} theme; screenshot was captured",
                        failure,
                    )
                }
            }
        }
    }

    private fun saveDesignScreenshotIfRequested(bitmap: Bitmap, name: String, dark: Boolean) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val safeName = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDirectory = context.getExternalFilesDir(null) ?: context.cacheDir
        File(outputDirectory, "parameters-$safeName-${if (dark) "dark" else "light"}.png")
            .outputStream()
            .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
