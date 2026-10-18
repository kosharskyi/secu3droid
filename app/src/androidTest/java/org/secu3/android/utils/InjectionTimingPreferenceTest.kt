package org.secu3.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.secu3.android.models.InjectionTimingMode
import org.secu3.android.ui.sensors.settings.SensorsSettingsViewModel

class InjectionTimingPreferenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getDefaultSharedPreferences()

    @Test
    fun timingChoicePersistsAndExternalPreferenceChangesRefreshSettingsViewModel() {
        val key = UserPrefs.INJECTION_TIMING_MODE_KEY
        val hadValue = preferences.contains(key)
        val previous = preferences.getString(key, null)
        val viewModelStore = ViewModelStore()
        try {
            preferences.edit().remove(key).commit()
            val userPrefs = UserPrefs(context)
            assertEquals(InjectionTimingMode.Default, userPrefs.injectionTimingMode)
            assertFalse("reading the default must not create a stored preference", preferences.contains(key))

            val viewModel = SensorsSettingsViewModel(userPrefs, context)
            viewModelStore.put("sensors-settings", viewModel)
            assertEquals(InjectionTimingMode.Default, viewModel.state.value.injectionTimingMode)

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                preferences.edit().putString(key, "atdc_360").commit()
            }
            assertEquals(InjectionTimingMode.Atdc360, UserPrefs(context).injectionTimingMode)
            assertEquals(InjectionTimingMode.Atdc360, viewModel.state.value.injectionTimingMode)

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                UserPrefs(context).injectionTimingMode = InjectionTimingMode.Btdc360
            }
            assertEquals("btdc_360", preferences.getString(key, null))
            assertEquals(InjectionTimingMode.Btdc360, viewModel.state.value.injectionTimingMode)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                if (hadValue) preferences.edit().putString(key, previous).commit() else preferences.edit().remove(key).commit()
                viewModelStore.clear()
            }
        }
    }
}
