package org.secu3.android.ui.compose

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SecuTabRowMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pillTracksHeldPagerDragContinuouslyAndReversesBeforeSettle() {
        var pager: PagerState? = null
        var surface = Color.Unspecified
        var primaryContainer = Color.Unspecified
        compose.setContent {
            SecuComposeTheme(darkTheme = false) {
                val state = rememberPagerState(pageCount = { 3 })
                val scheme = MaterialTheme.colorScheme
                SideEffect {
                    pager = state
                    surface = scheme.surface
                    primaryContainer = scheme.primaryContainer
                }
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    SecuTabRow(
                        titles = listOf("Start", "Карбюратор", "Універсальні виходи"),
                        selectedIndex = state.currentPage,
                        selectionPosition = { state.currentPage + state.currentPageOffsetFraction },
                        onSelected = { },
                        scrollable = true,
                        testTag = "motion-tabs",
                    )
                    HorizontalPager(state = state, modifier = Modifier.weight(1f).testTag("motion-pager")) { page ->
                        androidx.compose.material3.Text("Page $page")
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Start").assertIsSelected()
        val widthPx = compose.onNodeWithTag("motion-pager").fetchSemanticsNode().boundsInRoot.width
        val state = requireNotNull(pager)

        // Advance through many fractional updates without releasing or changing the settled page.
        var heldPosition = 0f
        repeat(15) {
            compose.runOnIdle { state.dispatchRawDelta(widthPx * 0.02f) }
            compose.waitForIdle()
        }
        compose.runOnIdle {
            heldPosition = state.currentPage + state.currentPageOffsetFraction
            assertTrue("Pager should remain between pages while the drag is held", heldPosition in 0.25f..0.35f)
            assertEquals(0, state.currentPage)
            assertEquals(0, state.settledPage)
        }
        compose.waitForIdle()
        compose.onNodeWithText("Start").assertIsSelected()
        assertPillBlend(0, expectedFraction = 1f - heldPosition, surface, primaryContainer)
        assertPillBlend(1, expectedFraction = heldPosition, surface, primaryContainer)
        assertPillBlend(2, expectedFraction = 0f, surface, primaryContainer)
        captureIfRequested("design-tabs-held-mid-swipe.png")

        var reversedPosition = 0f
        compose.runOnIdle {
            state.dispatchRawDelta(-widthPx * 0.12f)
            reversedPosition = state.currentPage + state.currentPageOffsetFraction
            assertTrue("Reverse drag should move the live selection toward the first page", reversedPosition in 0.10f..0.30f)
            assertEquals(0, state.settledPage)
        }
        compose.waitForIdle()
        assertPillBlend(0, expectedFraction = 1f - reversedPosition, surface, primaryContainer)
        assertPillBlend(1, expectedFraction = reversedPosition, surface, primaryContainer)
        compose.onNodeWithText("Start").assertIsSelected()

        compose.runOnIdle { state.dispatchRawDelta(widthPx * 0.80f) }
        compose.waitForIdle()
        compose.onNodeWithText("Карбюратор").assertIsSelected()
        assertPillBlend(1, expectedFraction = 1f, surface, primaryContainer)
        captureIfRequested("design-tabs-settled.png")
    }

    @Test fun fractionalUpdatesDoNotRerunTheTabListAndStillUpdatePills() {
        val titleReads = AtomicInteger()
        val titles = object : AbstractList<String>() {
            override val size: Int = 3
            override fun get(index: Int): String {
                titleReads.incrementAndGet()
                return listOf("Start", "Angles", "Diagnostics")[index]
            }
        }
        val position = mutableFloatStateOf(0f)
        var surface = Color.Unspecified
        var primaryContainer = Color.Unspecified
        compose.setContent {
            SecuComposeTheme(darkTheme = false) {
                val scheme = MaterialTheme.colorScheme
                SideEffect {
                    surface = scheme.surface
                    primaryContainer = scheme.primaryContainer
                }
                SecuTabRow(
                    titles = titles,
                    selectedIndex = 0,
                    selectionPosition = { position.floatValue },
                    onSelected = { },
                    scrollable = true,
                    testTag = "fractional-tabs",
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Start").assertIsSelected()
        titleReads.set(0)

        repeat(20) { step ->
            compose.runOnIdle { position.floatValue = (step + 1) * 0.01f }
            compose.waitForIdle()
        }
        assertEquals("Fractional progress should not re-run the row's title traversal", 0, titleReads.get())
        assertPillBlend(0, expectedFraction = 0.80f, surface, primaryContainer, tagPrefix = "fractional-tabs")
        assertPillBlend(1, expectedFraction = 0.20f, surface, primaryContainer, tagPrefix = "fractional-tabs")
        assertPillBlend(2, expectedFraction = 0f, surface, primaryContainer, tagPrefix = "fractional-tabs")

        repeat(10) { step ->
            compose.runOnIdle { position.floatValue = 0.20f - (step + 1) * 0.01f }
            compose.waitForIdle()
        }
        assertEquals("Reversing fractional progress should still leave the tab list stable", 0, titleReads.get())
        assertPillBlend(0, expectedFraction = 0.90f, surface, primaryContainer, tagPrefix = "fractional-tabs")
        assertPillBlend(1, expectedFraction = 0.10f, surface, primaryContainer, tagPrefix = "fractional-tabs")
        compose.onNodeWithText("Start").assertIsSelected()
    }

    private fun assertPillBlend(index: Int, expectedFraction: Float, from: Color, to: Color, tagPrefix: String = "motion-tabs") {
        val bounds = compose.onNodeWithTag("$tagPrefix-pill-$index", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val image = compose.onRoot().captureToImage().toPixelMap()
        val x = bounds.center.x.toInt().coerceIn(0, image.width - 1)
        val y = with(compose.density) { (bounds.top + 2.dp.toPx()).toInt().coerceIn(0, image.height - 1) }
        val actual = image[x, y]
        val expected = androidx.compose.ui.graphics.lerp(from, to, expectedFraction)
        assertColorNear(expected, actual)
    }

    private fun assertColorNear(expected: Color, actual: Color) {
        val maxChannelError = maxOf(
            abs(expected.red - actual.red), abs(expected.green - actual.green), abs(expected.blue - actual.blue),
        )
        assertTrue("Expected interpolated pill color $expected but found $actual", maxChannelError < 0.02f)
    }

    private fun captureIfRequested(fileName: String) {
        if (InstrumentationRegistry.getArguments().getString("captureDesignScreenshots") != "true") return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null) ?: context.cacheDir, fileName)
        file.outputStream().use { output ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
