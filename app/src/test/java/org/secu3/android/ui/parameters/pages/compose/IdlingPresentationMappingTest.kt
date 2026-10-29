package org.secu3.android.ui.parameters.pages.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class IdlingPresentationMappingTest {
    @Test
    fun metadataMatchesExistingEditorLimitsAndIntegerFields() {
        assertEquals(24, IdlingNumber.entries.size)
        val integerFields = listOf(
            IdlingNumber.RPM, IdlingNumber.DEAD_BAND, IdlingNumber.RPM_ADD,
            IdlingNumber.INTEGRATOR_RPM, IdlingNumber.IAC_DB,
        )
        assertEquals(integerFields, IdlingNumber.entries.filter { it.integer })
        assertEquals(
            listOf(2, 2, 3, 3, 0, 0, 2, 1, 0, 3, 3, 3, 3, 2, 2, 0, 1, 1, 1, 0, 3, 2, 2, 1),
            IdlingNumber.entries.map { it.precision },
        )
        assertEquals(
            listOf(.01f, .01f, .025f, .025f, 5f, 1f, .2f, .5f, 10f, .005f, .005f, .005f, .005f, .01f, .01f, 10f, .1f, .5f, .5f, 1f, .005f, .05f, .05f, .1f),
            IdlingNumber.entries.map { it.step },
        )
        assertEquals(
            listOf(0f, 0f, -15f, -15f, 0f, 0f, -40f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            IdlingNumber.entries.map { it.min },
        )
        assertEquals(
            listOf(10f, 10f, 30f, 30f, 1800f, 500f, 300f, 100f, 2000f, 5f, 5f, 5f, 5f, 2.99f, 2.99f, 1200f, 100f, 100f, 100f, 500f, 5f, 48f, 48f, 500f),
            IdlingNumber.entries.map { it.max },
        )
        assertEquals(7, IdlingFlag.entries.size)
    }
}
