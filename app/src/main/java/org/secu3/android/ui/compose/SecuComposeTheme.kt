package org.secu3.android.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import org.secu3.android.R

/** Shared Material 3 defaults for screens migrated to Compose. */
@Composable
fun SecuComposeTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = colorResource(R.color.app_theme_primary),
        onPrimary = colorResource(R.color.app_theme_on_primary),
        secondary = colorResource(R.color.gauge_blue),
        background = colorResource(R.color.colorPrimary),
        surface = colorResource(R.color.white),
        onSurface = colorResource(R.color.black),
        error = colorResource(R.color.gauge_dark_red),
    )
    MaterialTheme(colorScheme = colors, content = content)
}
