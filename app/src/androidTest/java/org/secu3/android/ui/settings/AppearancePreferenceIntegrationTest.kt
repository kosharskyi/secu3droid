package org.secu3.android.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.utils.AppearanceMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppearancePreferenceIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun choosingDarkUpdatesRootBarsImmediatelyAndSurvivesActivityRecreation() {
        val previous = runBlocking { compose.activity.userPrefs.settings.first().appearanceMode }
        try {
            compose.onNodeWithContentDescription("Settings icon").performClick()
            compose.waitForIdle()
            compose.onNodeWithText(compose.activity.getString(R.string.appearance_title)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.appearance_dark)).performClick()
            compose.waitForIdle()

            compose.waitUntil(5_000) {
                runBlocking { compose.activity.userPrefs.settings.first().appearanceMode == AppearanceMode.Dark }
            }
            assertEquals(AppearanceMode.Dark, runBlocking { compose.activity.userPrefs.settings.first().appearanceMode })
            compose.activityRule.scenario.onActivity { activity ->
                assertEquals(
                    false,
                    WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightStatusBars,
                )
            }
            compose.onNodeWithText(compose.activity.getString(R.string.appearance_dark)).assertIsDisplayed()

            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            assertEquals(AppearanceMode.Dark, runBlocking { compose.activity.userPrefs.settings.first().appearanceMode })
            compose.onNodeWithText(compose.activity.getString(R.string.appearance_dark)).assertIsDisplayed()
            compose.activityRule.scenario.onActivity { activity ->
                assertEquals(
                    false,
                    WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightStatusBars,
                )
            }
        } finally {
            compose.activityRule.scenario.onActivity { activity ->
                runBlocking { activity.userPrefs.setAppearanceMode(previous) }
            }
            compose.waitForIdle()
        }
    }
}
