package org.secu3.android.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {

    @Test
    fun parse_rejectsVersionWithoutTagPrefix() {
        try {
            Version.parse("0.18.0")
        } catch (e: IllegalArgumentException) {
            return
        }

        throw AssertionError("Expected version without v prefix to be rejected")
    }

    @Test
    fun parse_acceptsTaggedReleaseVersion() {
        assertEquals(Version(0, 18, 0), Version.parse("v0.18.0"))
    }

    @Test
    fun parse_acceptsUppercaseTaggedReleaseVersion() {
        assertEquals(Version(1, 2, 3), Version.parse("V1.2.3"))
    }

    @Test
    fun parse_rejectsVersionSuffix() {
        try {
            Version.parse("v0.18.0-debug")
        } catch (e: NumberFormatException) {
            return
        }

        throw AssertionError("Expected version suffix to be rejected")
    }

    @Test
    fun compareTo_ordersVersionsByMajorMinorPatch() {
        assertTrue(Version.parse("v0.19.0") > Version.parse("v0.18.9"))
        assertTrue(Version.parse("v1.0.0") > Version.parse("v0.99.99"))
        assertTrue(Version.parse("v0.18.1") > Version.parse("v0.18.0"))
    }
}
