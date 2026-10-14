package org.secu3.android.ui.logger

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class SecuLogsPendingExportTest {
    @Test
    fun permissionResultConsumesRestoredExportOnlyOnce() {
        val path = "/tmp/log.csv"
        val restoredState = SavedStateHandle(mapOf(SecuLogsViewModel.KEY_PENDING_EXPORT to path))
        val exported = mutableListOf<File>()
        var denied = 0

        consumePendingExportPath(restoredState, granted = true, onGranted = exported::add, onDenied = { denied++ })
        consumePendingExportPath(restoredState, granted = true, onGranted = exported::add, onDenied = { denied++ })

        assertEquals(listOf(File(path)), exported)
        assertEquals(0, denied)
        assertNull(restoredState.get<String>(SecuLogsViewModel.KEY_PENDING_EXPORT))
    }

    @Test
    fun deniedPermissionConsumesPendingExportWithoutSaving() {
        val path = "/tmp/log.csv"
        val state = SavedStateHandle(mapOf(SecuLogsViewModel.KEY_PENDING_EXPORT to path))
        var exported = false
        var denied = 0

        consumePendingExportPath(state, granted = false, onGranted = { exported = true }, onDenied = { denied++ })

        assertEquals(false, exported)
        assertEquals(1, denied)
        assertNull(state.get<String>(SecuLogsViewModel.KEY_PENDING_EXPORT))
    }
}
