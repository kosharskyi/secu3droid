package org.secu3.android.utils

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.secu3.android.R

class UserPrefsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val logExportDirectoryKey = context.getString(R.string.pref_log_export_directory_key)

    @Before
    fun setUp() {
        clearLogExportDirectory()
    }

    @After
    fun tearDown() {
        clearLogExportDirectory()
    }

    @Test
    fun logExportDestinationDefaultsToDownloadsWhenNoFolderIsSelected() {
        val userPrefs = UserPrefs(context)

        assertEquals(LogExportDestination.DefaultDownloads, userPrefs.logExportDestination)
        assertNull(userPrefs.logExportDirectoryUri)
    }

    @Test
    fun logExportDestinationUsesCustomTreeWhenFolderIsSelected() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3ASecu3"
        val userPrefs = UserPrefs(context)

        userPrefs.logExportDirectoryUri = uri

        assertEquals(LogExportDestination.CustomTree(uri), userPrefs.logExportDestination)
        assertEquals(uri, userPrefs.logExportDirectoryUri)
    }

    private fun clearLogExportDirectory() {
        sharedPreferences.edit { remove(logExportDirectoryKey) }
    }
}
