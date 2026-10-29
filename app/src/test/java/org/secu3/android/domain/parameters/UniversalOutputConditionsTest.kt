package org.secu3.android.domain.parameters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalOutputConditionsTest {
    @Test fun allFortyTwoCatalogEntriesRetainDesktopBoundsPrecisionAndDefaults() {
        val expected = listOf(
            UniOutConditionFormat(-40f,300f,.25f,2,55f,50f),
            UniOutConditionFormat(50f,30000f,10f,0,1500f,1200f),
            UniOutConditionFormat(.25f,500f,.25f,2,95f,90f),
            UniOutConditionFormat(5f,16f,.1f,1,10f,10.5f),
            UniOutConditionFormat(0f,1f,1f,0,0f,1f),
            UniOutConditionFormat(5f,250f,.1f,1,70f,65f),
            UniOutConditionFormat(0f,16f,1f,0,13f,12f),
            UniOutConditionFormat(0f,600f,.1f,1,0f,5f),
            UniOutConditionFormat(0f,600f,.1f,1,0f,5f),
            UniOutConditionFormat(0f,600f,.1f,1,0f,5f),
            UniOutConditionFormat(0f,100f,.5f,1,60f,55f),
            UniOutConditionFormat(-15f,65f,.1f,1,55f,53f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.45f),
            UniOutConditionFormat(0f,100f,.5f,1,30f,29f),
            UniOutConditionFormat(-40f,300f,.25f,2,55f,50f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.48f),
            UniOutConditionFormat(0f,600f,.1f,2,2.5f,2.48f),
            UniOutConditionFormat(0f,1f,1f,0,0f,1f),
            UniOutConditionFormat(.01f,200f,.01f,2,20f,19.9f),
            UniOutConditionFormat(0f,1f,1f,0,0f,1f),
            UniOutConditionFormat(0f,600f,.1f,1,0f,5f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.48f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.48f),
            UniOutConditionFormat(0f,600f,.01f,2,1f,5f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.48f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.48f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.48f),
            UniOutConditionFormat(0f,5f,.01f,2,2.5f,2.48f),
            UniOutConditionFormat(-40f,300f,.25f,2,55f,50f),
            UniOutConditionFormat(.25f,500f,.25f,2,95f,90f),
            UniOutConditionFormat(-40f,300f,.25f,2,55f,50f),
            UniOutConditionFormat(0f,1f,1f,0,0f,1f),
            UniOutConditionFormat(0f,1f,1f,0,0f,1f),
            UniOutConditionFormat(.02f,650f,.1f,2,95f,90f),
            UniOutConditionFormat(-1000f,1000f,1f,0,55f,50f),
            UniOutConditionFormat(.25f,500f,.25f,2,95f,90f),
            UniOutConditionFormat(0f,700f,.25f,2,320f,300f),
            UniOutConditionFormat(0f,25f,.05f,3,4.1f,4f),
            UniOutConditionFormat(0f,2000f,.25f,1,1000f,990f),
            UniOutConditionFormat(-40f,250f,.25f,1,100f,98f),
            UniOutConditionFormat(-40f,250f,.25f,1,100f,98f),
            UniOutConditionFormat(.25f,500f,.25f,2,95f,90f),
        )
        val catalog = UniversalOutputConditions()
        assertEquals(42, expected.size)
        expected.forEachIndexed { id, format -> assertEquals("condition $id", format, catalog.get(id)) }
        assertEquals(null, catalog.get(-1))
        assertEquals(null, catalog.get(42))
    }

    @Test fun onlyTimerConditionCodesAreExcludedFromSlotOne() {
        val catalog = UniversalOutputConditions()
        (0..41).forEach { id -> assertEquals("condition $id", id !in setOf(7,23), catalog.availableInSlot(id,1)) }
        (0..41).forEach { id -> assertTrue("condition $id", catalog.availableInSlot(id,2)) }
        assertFalse(catalog.availableInSlot(0,0))
        assertFalse(catalog.availableInSlot(0,3))
        assertFalse(catalog.availableInSlot(99,2))
    }
}
