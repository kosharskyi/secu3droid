package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.domain.parameters.StarterValues

class StarterPresentationMappingTest {
    @Test
    fun numericRowsMapEveryNamedDomainValueAndKeepDesktopEditorMetadata() {
        val values = StarterValues(101, 202, 1.23f, 4.56f, 7, 8, 9.1f, 2.3f, 4.5f, 67.8f, 90, true, false)

        assertEquals(
            listOf(101f, 202f, 1.23f, 4.56f, 7f, 8f, 9.1f, 2.3f, 4.5f, 67.8f, 90f),
            StarterNumber.entries.map { it.value(values) },
        )
        assertEquals(listOf(0f, 0f, .1f, .1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), StarterNumber.entries.map { it.min })
        assertEquals(listOf(6000f, 6000f, 99f, 99f, 1020f, 1020f, 100f, 100f, 25f, 100f, 255f), StarterNumber.entries.map { it.max })
        assertEquals(listOf(10f, 10f, .01f, .01f, 4f, 4f, .1f, .1f, .1f, .5f, 1f), StarterNumber.entries.map { it.step })
        assertEquals(listOf(0, 0, 2, 2, 0, 0, 1, 1, 1, 1, 0), StarterNumber.entries.map { it.precision })
        assertEquals(R.string.units_rpm, StarterNumber.STARTER_OFF.unit.resourceId)
        assertEquals(R.string.units_ms, StarterNumber.PRIME_COLD.unit.resourceId)
        assertEquals("num", StarterNumber.AFTERSTART_PETROL.unit.text)
        assertEquals("str", StarterNumber.STBL_COUNTDOWN.unit.text)
        assertTrue(StarterNumber.AFTERSTART_PETROL.integer)
        assertFalse(StarterNumber.CRANK_TO_RUN_COLD.integer)
    }
}
